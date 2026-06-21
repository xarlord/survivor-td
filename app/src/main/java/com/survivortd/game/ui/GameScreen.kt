package com.survivortd.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.components.RenderComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.core.GameLoop
import com.survivortd.game.systems.VirtualJoystick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Root game screen. Manages the strict rendering layer hierarchy:
 *   1. Game Canvas (background + ECS entities via drawBehind)
 *   2. HUD Overlay (Compose composables — HP, XP, timer)
 *   3. Virtual Joystick (floating, left-half of screen)
 *
 * CRITICAL: Game state is read inside drawBehind lambdas, NOT as Compose
 * State objects. This prevents recomposition cascades that destroy FPS.
 */
@Composable
fun GameScreen(
    gameState: GameState,
    gameLoop: GameLoop,
    onPause: () -> Unit,
    onGameOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only these trigger recomposition for UI — NOT game positions
    var hudHp by remember { mutableFloatStateOf(1f) }
    var hudXp by remember { mutableFloatStateOf(0f) }
    var hudLevel by remember { mutableIntStateOf(1) }
    var hudTime by remember { mutableLongStateOf(0L) }
    var hudScore by remember { mutableLongStateOf(0L) }
    var hudGold by remember { mutableIntStateOf(0) }

    // Joystick visual state — triggers redraw at ~60Hz
    var joystickActive by remember { mutableStateOf(false) }
    var joystickAnchor by remember { mutableStateOf(Offset.Zero) }
    var joystickKnob by remember { mutableStateOf(Offset.Zero) }

    val joystick = remember { VirtualJoystick(gameState) }

    DisposableEffect(Unit) {
        onDispose {
            gameLoop.stop()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // === LAYER 1: Game Canvas + Touch Input ===
        GameCanvasView(
            gameState = gameState,
            joystick = joystick,
            joystickActive = joystickActive,
            joystickAnchor = joystickAnchor,
            joystickKnob = joystickKnob,
            onJoystickActiveChange = { joystickActive = it },
            onJoystickAnchorChange = { joystickAnchor = it },
            onJoystickKnobChange = { joystickKnob = it },
            modifier = Modifier.fillMaxSize()
        )

        // === LAYER 2: HUD Overlay ===
        GameHUD(
            hp = hudHp,
            xp = hudXp,
            level = hudLevel,
            secondsRemaining = hudTime,
            score = hudScore,
            gold = hudGold,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Update HUD values at 10Hz (every 100ms) — NOT every frame
    LaunchedEffect(Unit) {
        while (true) {
            hudHp = gameState.healthPercent
            if (gameState.players.isNotEmpty() && gameState.playerIndex >= 0) {
                val player = gameState.players[minOf(gameState.playerIndex, gameState.players.size - 1)]
                hudLevel = player.level
                hudXp = if (player.xpToNext > 0)
                    player.currentXp.toFloat() / player.xpToNext.toFloat() else 0f
                hudGold = player.gold
            }
            hudTime = (900 - gameState.elapsedSeconds).toLong().coerceAtLeast(0)
            hudScore = gameState.score
            if (gameState.isGameOver) {
                onGameOver()
                break
            }
            delay(100)
        }
    }
}

/**
 * Game canvas — renders all entities + joystick via drawBehind.
 */
@Composable
private fun GameCanvasView(
    gameState: GameState,
    joystick: VirtualJoystick,
    joystickActive: Boolean,
    joystickAnchor: Offset,
    joystickKnob: Offset,
    onJoystickActiveChange: (Boolean) -> Unit,
    onJoystickAnchorChange: (Offset) -> Unit,
    onJoystickKnobChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val pos = change.position

                        when {
                            change.pressed && !joystick.active() -> {
                                joystick.onTouchDown(pos.x, pos.y)
                                onJoystickActiveChange(true)
                                onJoystickAnchorChange(Offset(pos.x, pos.y))
                                onJoystickKnobChange(Offset(pos.x, pos.y))
                            }
                            change.pressed && joystick.active() -> {
                                joystick.onTouchMove(pos.x, pos.y)
                                val (kx, ky) = joystick.knobPosition()
                                onJoystickKnobChange(Offset(kx, ky))
                            }
                            !change.pressed && joystick.active() -> {
                                joystick.onTouchUp()
                                onJoystickActiveChange(false)
                            }
                        }
                    }
                }
            }
    ) {
        drawGameBackground()
        drawEntities(gameState)
        if (joystickActive) {
            drawJoystick(joystickAnchor, joystickKnob)
        }
    }
}

private fun DrawScope.drawGameBackground() {
    drawRect(color = Color(0xFF0F1320))

    // Grid lines for movement reference
    val gridColor = Color(0xFF1A1F33)
    val gridSize = 80f
    var x = 0f
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = 0f
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

/**
 * Draws all renderable entities from game state.
 */
private fun DrawScope.drawEntities(state: GameState) {
    for (entity in state.renderableEntities) {
        val color = Color(entity.color.toLong())
        val center = Offset(entity.x, entity.y)
        when (entity.shape) {
            RenderComponent.RenderShape.CIRCLE -> {
                drawCircle(color = color, radius = entity.radius, center = center)
            }
            RenderComponent.RenderShape.RECT -> {
                drawRect(
                    color = color,
                    topLeft = Offset(entity.x - entity.radius, entity.y - entity.radius),
                    size = Size(entity.radius * 2, entity.radius * 2)
                )
            }
            RenderComponent.RenderShape.TRIANGLE -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(entity.x, entity.y - entity.radius)
                    lineTo(entity.x - entity.radius, entity.y + entity.radius)
                    lineTo(entity.x + entity.radius, entity.y + entity.radius)
                    close()
                }
                drawPath(path, color = color)
            }
            RenderComponent.RenderShape.DIAMOND -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(entity.x, entity.y - entity.radius)
                    lineTo(entity.x + entity.radius, entity.y)
                    lineTo(entity.x, entity.y + entity.radius)
                    lineTo(entity.x - entity.radius, entity.y)
                    close()
                }
                drawPath(path, color = color)
            }
        }
    }
}

/**
 * Draws the virtual joystick base + knob.
 */
private fun DrawScope.drawJoystick(anchor: Offset, knob: Offset) {
    val baseRadius = 120f
    val knobRadius = 50f

    // Base circle (semi-transparent)
    drawCircle(
        color = Color(0xFF1A1F33).copy(alpha = 0.6f),
        radius = baseRadius,
        center = anchor
    )
    // Base outline
    drawCircle(
        color = Color(0xFF333A4D),
        radius = baseRadius,
        center = anchor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
    // Knob
    drawCircle(
        color = Color(0xFF00E676).copy(alpha = 0.8f),
        radius = knobRadius,
        center = knob
    )
    // Knob outline
    drawCircle(
        color = Color(0xFF00E676),
        radius = knobRadius,
        center = knob,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
}

// === HUD Components ===

@Composable
private fun GameHUD(
    hp: Float,
    xp: Float,
    level: Int,
    secondsRemaining: Long,
    score: Long,
    gold: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF00E676)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$level",
                    color = Color(0xFF0A0E1A),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // HP bar
            Column(modifier = Modifier.weight(1f)) {
                Text("HP", color = Color(0xFFFF1744), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { hp },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFF1744),
                    trackColor = Color(0xFF333A4D)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Gold
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GOLD", color = Color(0xFFFFD700), fontSize = 9.sp)
                Text(
                    text = "$gold",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", color = Color(0xFF9E9E9E), fontSize = 9.sp)
                val mins = secondsRemaining / 60
                val secs = secondsRemaining % 60
                Text(
                    text = "%d:%02d".format(mins, secs),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // XP bar
        LinearProgressIndicator(
            progress = { xp },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF2979FF),
            trackColor = Color(0xFF333A4D)
        )
    }
}
