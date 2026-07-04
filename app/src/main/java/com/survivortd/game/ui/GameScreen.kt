package com.survivortd.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.components.RenderComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.WeaponType
import com.survivortd.game.core.GameState
import com.survivortd.game.core.GameLoop
import com.survivortd.game.systems.LevelUpSystem
import com.survivortd.game.systems.UpgradeChoice
import com.survivortd.game.systems.VirtualJoystick
import com.survivortd.game.systems.WeaponSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
    onExit: () -> Unit
) {
    val gameState = remember { GameState() }
    GameScreen(gameState = gameState, onGameOver = onExit, onExit = onExit)
}

@Composable
fun GameScreen(
    gameState: GameState,
    onGameOver: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only these trigger recomposition for UI — NOT game positions
    var hudHp by remember { mutableFloatStateOf(1f) }
    var hudXp by remember { mutableFloatStateOf(0f) }
    var hudLevel by remember { mutableIntStateOf(1) }
    var hudTime by remember { mutableLongStateOf(0L) }
    var hudScore by remember { mutableLongStateOf(0L) }
    var hudGold by remember { mutableIntStateOf(0) }
    var hudFps by remember { mutableIntStateOf(0) }

    // [#23] Redraw trigger — increments every frame to force Canvas recomposition.
    // Game entity positions are plain Kotlin objects (NOT Compose State), so the
    // Canvas would never redraw without this. The game loop calls onRender on a
    // background thread; we bump this on the Main dispatcher to trigger redraw.
    // Using Choreographer for VSYNC-aligned updates (no flooding main thread).
    var redrawTrigger by remember { mutableIntStateOf(0) }
    val renderHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val renderPending = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // FPS counter: count actual Canvas redraws per second via redrawTrigger changes.
    // We sample every second from the main coroutine — avoids off-thread complexity.
    LaunchedEffect(Unit) {
        var prevTrigger = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            val current = redrawTrigger
            hudFps = (current - prevTrigger).coerceAtLeast(0)
            prevTrigger = current
            if (hudFps > 0) {
                android.util.Log.d("SurvivorTD-FPS", "FPS=$hudFps")
            }
        }
    }

    // Joystick visual state — triggers redraw at ~60Hz
    var joystickActive by remember { mutableStateOf(false) }
    var joystickAnchor by remember { mutableStateOf(Offset.Zero) }
    var joystickKnob by remember { mutableStateOf(Offset.Zero) }

    val joystick = remember { VirtualJoystick(gameState) }

    // Level-up system state
    val weaponSystem = remember { WeaponSystem(gameState) }
    val levelUpSystem = remember { LevelUpSystem(gameState, weaponSystem) }
    var levelUpChoices by remember { mutableStateOf<List<UpgradeChoice>>(emptyList()) }

    // Create ALL game systems
    val particleSystem = remember { com.survivortd.game.systems.ParticleSystem(gameState) }
    val gameFeelSystem = remember { com.survivortd.game.systems.GameFeelSystem() }
    val movementSystem = remember { com.survivortd.game.systems.MovementSystem(gameState) }
    val combatSystem = remember { com.survivortd.game.systems.CombatSystem(gameState, gameFeelSystem) }
    val enemyAiSystem = remember { com.survivortd.game.systems.EnemyAISystem(gameState) }
    val pickupSystem = remember { com.survivortd.game.systems.PickupSystem(gameState, particleSystem) }
    val projectileSystem = remember {
        com.survivortd.game.systems.ProjectileSystem(gameState, particleSystem, gameFeelSystem)
    }
    // [#21] WaveSystem — spawns enemies continuously. Was completely missing.
    val waveSystem = remember { com.survivortd.game.systems.WaveSystem(gameState) }
    // [#22] TowerSystem — manages placed towers. Was completely missing.
    val towerSystem = remember { com.survivortd.game.systems.TowerSystem(gameState) }
    // [#32] StatusEffectSystem — processes DoTs and CC. Was completely missing.
    val statusEffectSystem = remember { com.survivortd.game.systems.StatusEffectSystem(gameState) }

    // [#20][#35] Spawn player and start game loop SYNCHRONOUSLY (not in LaunchedEffect).
    //
    // CRITICAL: LaunchedEffect(Unit) runs on the Main dispatcher. During E2E
    // instrumentation tests, Thread.sleep() on the test thread (which IS the
    // main thread) blocks the dispatcher, so the LaunchedEffect coroutine never
    // executes — player never spawns, game loop never starts, enemies never
    // appear. By doing this in remember{}, it executes during composition.
    remember {
        if (gameState.playerIndex < 0) {
            gameState.spawnPlayer()
        }
        // [#35] Player starts with ASSAULT_RIFLE per GDD §3.3 (Commander hero).
        if (weaponSystem.weapons.isEmpty()) {
            weaponSystem.addWeapon(WeaponType.ASSAULT_RIFLE)
        }
        true
    }

    // [#35] Register TestGameBridge for E2E tests (debug builds only).
    remember {
        com.survivortd.game.testing.TestGameBridge.register(gameState, weaponSystem)
        true
    }

    val gameLoop = remember(gameState) {
        GameLoop(
            onUpdate = { dt ->
                if (gameState.isPaused || gameState.isGameOver) return@GameLoop
                // Game feel first — returns effective dt (0 during hit-stop)
                val effectiveDt = gameFeelSystem.update(dt)
                if (effectiveDt <= 0f) return@GameLoop  // Hit-stop freeze

                // [#21] Wave system FIRST — spawns enemies for other systems to process
                waveSystem.update(effectiveDt)
                // System update order: AI → Movement → Status → Combat → Towers → Weapons → Projectiles → Pickups
                enemyAiSystem.update(effectiveDt)
                movementSystem.update(effectiveDt)
                statusEffectSystem.update(effectiveDt)
                combatSystem.update(effectiveDt)
                // [#22] Tower system — auto-targets and fires at enemies
                towerSystem.update(effectiveDt)
                weaponSystem.update(effectiveDt)
                projectileSystem.update(effectiveDt)
                pickupSystem.update(effectiveDt)
                // Particles always update (even during hit-stop for visual continuity)
                particleSystem.update(dt)
                gameState.elapsedSeconds += effectiveDt
                gameState.cleanupDeadEntities()
            },
            onRender = {
                // [#23] Trigger Canvas redraw by bumping state on Main thread.
                // Guard with AtomicBoolean to prevent flooding Main thread —
                // only one render callback queued at a time.
                if (renderPending.compareAndSet(false, true)) {
                    renderHandler.post {
                        renderPending.set(false)
                        redrawTrigger = (redrawTrigger + 1) % 1_000_000
                    }
                }
            }
        )
    }

    // [#35] Start game loop SYNCHRONOUSLY using a standalone CoroutineScope
    // on Dispatchers.Default (background thread). This ensures the loop runs
    // even when the Main thread is blocked by Thread.sleep() in E2E tests.
    val gameLoopScope = remember { CoroutineScope(Dispatchers.Default) }
    remember(gameLoop) {
        gameLoop.start(gameLoopScope)
        true
    }

    DisposableEffect(Unit) {
        onDispose {
            gameLoop.stop()
            gameLoopScope.cancel()
            // [#26] Unregister from TestGameBridge (debug only)
            com.survivortd.game.testing.TestGameBridge.unregister()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("game_screen")) {
        // === LAYER 1: Game Canvas + Touch Input ===
        GameCanvasView(
            gameState = gameState,
            particleSystem = particleSystem,
            gameFeelSystem = gameFeelSystem,
            towerSystem = towerSystem,
            joystick = joystick,
            joystickActive = joystickActive,
            joystickAnchor = joystickAnchor,
            joystickKnob = joystickKnob,
            onJoystickActiveChange = { joystickActive = it },
            onJoystickAnchorChange = { joystickAnchor = it },
            onJoystickKnobChange = { joystickKnob = it },
            redrawTrigger = redrawTrigger,
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
            fps = hudFps,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // === LAYER 3: Level-Up Dialog ===
        if (levelUpChoices.isNotEmpty()) {
            LevelUpDialog(
                level = hudLevel,
                choices = levelUpChoices,
                onChoiceSelected = { choice ->
                    levelUpSystem.applyChoice(choice)
                    if (gameState.pendingLevelUps > 0) {
                        // Another level-up queued — generate new choices
                        levelUpChoices = levelUpSystem.generateChoices()
                    } else {
                        levelUpChoices = emptyList()
                        gameState.isPaused = false
                    }
                }
            )
        }
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

            // Check for level-up → generate choices and pause game
            if (gameState.pendingLevelUps > 0 && levelUpChoices.isEmpty()) {
                gameState.isPaused = true
                levelUpChoices = levelUpSystem.generateChoices()
            }

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
    particleSystem: com.survivortd.game.systems.ParticleSystem,
    gameFeelSystem: com.survivortd.game.systems.GameFeelSystem,
    towerSystem: com.survivortd.game.systems.TowerSystem,
    joystick: VirtualJoystick,
    joystickActive: Boolean,
    joystickAnchor: Offset,
    joystickKnob: Offset,
    onJoystickActiveChange: (Boolean) -> Unit,
    onJoystickAnchorChange: (Offset) -> Unit,
    onJoystickKnobChange: (Offset) -> Unit,
    redrawTrigger: Int,
    modifier: Modifier = Modifier
) {
    // [#23] Read redrawTrigger so Compose knows to recompose when it changes
    @Suppress("UNUSED_VARIABLE")
    val trigger = redrawTrigger

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
        // Viewport: scale world to fill full screen height, center on player
        val scale = size.height / GameConfig.WORLD_HEIGHT
        // Camera offset: center the player on screen + screen shake
        val camX = gameState.cameraX * scale - size.width / 2f + gameFeelSystem.shakeOffsetX * scale
        val camY = gameState.cameraY * scale - size.height / 2f + gameFeelSystem.shakeOffsetY * scale

        withTransform({
            translate(left = camX, top = camY)
            scale(scale, scale)
        }) {
            drawGameBackground(gameState)
            drawEntities(gameState)
            drawParticles(particleSystem)
            drawTowers(towerSystem)
            drawPlayerGlow(gameState)
        }

        // Damage flash overlay (screen-space, not world-space)
        if (gameFeelSystem.damageFlash > 0f) {
            drawRect(
                color = Color.Red.copy(alpha = gameFeelSystem.damageFlash * 0.3f)
            )
        }
        if (joystickActive) {
            drawJoystick(joystickAnchor, joystickKnob)
        }
    }
}

/**
 * Draws the game background with parallax grid layers and theme-based colors.
 * Background changes based on elapsed game time (5 chapters).
 */
private fun DrawScope.drawGameBackground(state: GameState) {
    // Theme based on game time — 5 chapters across 15 minutes
    val minute = state.elapsedSeconds / 60f
    val (baseColor, gridColor, _) = when {
        minute < 3f -> Triple(0xFF1A150F, 0xFF2A2018, "Wasteland")
        minute < 6f -> Triple(0xFF0F1A0F, 0xFF182A18, "Toxic Swamp")
        minute < 9f -> Triple(0xFF0F0F1A, 0xFF18182A, "Abandoned City")
        minute < 12f -> Triple(0xFF0F0F1A, 0xFF1A1A28, "Underground Lab")
        else -> Triple(0xFF1A0F0F, 0xFF2A1818, "Final Bunker")
    }

    drawRect(color = Color(baseColor))

    // Parallax grid: 3 layers at different scroll speeds relative to camera
    val scale = size.height / GameConfig.WORLD_HEIGHT
    val camX = state.cameraX * scale - size.width / 2f
    val camY = state.cameraY * scale - size.height / 2f

    // Layer 0 (distant): large features, slowest scroll (0.2x camera)
    drawParallaxGrid(camX * 0.2f, camY * 0.2f, 200f, Color(0xFF1A1F22).copy(alpha = 0.4f))

    // Layer 1 (mid): medium detail (0.5x camera)
    drawParallaxGrid(camX * 0.5f, camY * 0.5f, 120f, Color(gridColor.toLong()).copy(alpha = 0.5f))

    // Layer 2 (foreground): current grid, 1:1 with camera
    drawParallaxGrid(camX, camY, 80f, Color(gridColor.toLong()).copy(alpha = 0.7f))
}

/**
 * Draws a parallax grid layer offset by camera position.
 */
private fun DrawScope.drawParallaxGrid(offsetX: Float, offsetY: Float, gridSize: Float, color: Color) {
    val startX = -offsetX % gridSize - gridSize
    val startY = -offsetY % gridSize - gridSize
    var x = startX
    while (x < size.width + gridSize) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = startY
    while (y < size.height + gridSize) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

/**
 * Draws all renderable entities from game state.
 * Iterates arrays directly — no intermediate object allocation per frame.
 */
private fun DrawScope.drawEntities(state: GameState) {
    val positions = state.positions
    val healths = state.healths
    val renders = state.renders
    val tags = state.tags
    val statusEffects = state.statusEffects
    val triPath = androidx.compose.ui.graphics.Path()
    val diaPath = androidx.compose.ui.graphics.Path()

    for (i in positions.indices) {
        if (i >= healths.size || healths[i].isDead) continue
        if (i >= renders.size) continue

        val pos = positions[i]
        val render = renders[i]
        val colorInt = render.color
        val radius = render.radius
        val center = Offset(pos.x, pos.y)

        // [#85] Draw status effect glow behind enemies
        if (i < tags.size && tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY
            && i < statusEffects.size && statusEffects[i].effects.isNotEmpty()
        ) {
            drawStatusEffectGlow(center, radius, statusEffects[i].effects)
        }

        when (render.shape) {
            RenderComponent.RenderShape.CIRCLE -> {
                drawCircle(
                    color = Color(colorInt.toLong()),
                    radius = radius,
                    center = Offset(pos.x, pos.y)
                )
            }
            RenderComponent.RenderShape.RECT -> {
                drawRect(
                    color = Color(colorInt.toLong()),
                    topLeft = Offset(pos.x - radius, pos.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
            RenderComponent.RenderShape.TRIANGLE -> {
                triPath.reset()
                triPath.moveTo(pos.x, pos.y - radius)
                triPath.lineTo(pos.x - radius, pos.y + radius)
                triPath.lineTo(pos.x + radius, pos.y + radius)
                triPath.close()
                drawPath(triPath, color = Color(colorInt.toLong()))
            }
            RenderComponent.RenderShape.DIAMOND -> {
                diaPath.reset()
                diaPath.moveTo(pos.x, pos.y - radius)
                diaPath.lineTo(pos.x + radius, pos.y)
                diaPath.lineTo(pos.x, pos.y + radius)
                diaPath.lineTo(pos.x - radius, pos.y)
                diaPath.close()
                drawPath(diaPath, color = Color(colorInt.toLong()))
            }
        }
    }
}

/**
 * Draws a glow outline behind enemies with active status effects.
 * Dominant effect gets the full glow; secondary effects get a thinner ring.
 */
private fun DrawScope.drawStatusEffectGlow(
    center: Offset,
    radius: Float,
    effects: List<com.survivortd.game.components.StatusEffectsComponent.ActiveStatus>
) {
    val colorMap = mapOf(
        com.survivortd.game.config.StatusEffectType.BURN to 0xFFFF1744.toInt(),
        com.survivortd.game.config.StatusEffectType.POISON to 0xFF76FF03.toInt(),
        com.survivortd.game.config.StatusEffectType.FREEZE to 0xFF81D4FA.toInt(),
        com.survivortd.game.config.StatusEffectType.STUN to 0xFFFFFF00.toInt(),
        com.survivortd.game.config.StatusEffectType.SLOW to 0xFFCE93D8.toInt(),
        com.survivortd.game.config.StatusEffectType.BLEED to 0xFFFF1744.toInt(),
        com.survivortd.game.config.StatusEffectType.SLOW_ATTACK to 0xFFCE93D8.toInt()
    )

    val sorted = effects.sortedByDescending { it.duration }
    for ((ringIndex, effect) in sorted.withIndex()) {
        if (ringIndex >= 2) break
        val colorInt = colorMap[effect.type] ?: continue
        val glowRadius = radius + 4f + ringIndex * 3f
        val alpha = if (ringIndex == 0) 0.5f else 0.3f
        drawCircle(
            color = Color(colorInt.toLong()).copy(alpha = alpha),
            radius = glowRadius,
            center = center
        )
    }
}

/**
 * Draws all active particles from the particle system.
 * Particles fade out based on remaining lifetime.
 */
private fun DrawScope.drawParticles(
    particleSystem: com.survivortd.game.systems.ParticleSystem
) {
    val particles = particleSystem.particles
    for (i in particles.indices) {
        val p = particles[i]
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color(p.color.toLong()).copy(alpha = alpha),
            radius = p.size * alpha, // Shrink as they fade
            center = Offset(p.x, p.y)
        )
    }
}

/**
 * Draws placed towers from TowerSystem's local list.
 * Each tower type has a unique color and shape.
 */
private fun DrawScope.drawTowers(
    towerSystem: com.survivortd.game.systems.TowerSystem
) {
    for (tower in towerSystem.towers) {
        val color = when (tower.type) {
            com.survivortd.game.config.TowerType.GUN_TURRET -> Color(0xFF42A5F5)
            com.survivortd.game.config.TowerType.CANNON -> Color(0xFFFF6F00)
            com.survivortd.game.config.TowerType.FROST_TOWER -> Color(0xFF00FFFF)
            com.survivortd.game.config.TowerType.TESLA_COIL -> Color(0xFFFFF700)
            com.survivortd.game.config.TowerType.POISON_TOWER -> Color(0xFF76FF03)
            com.survivortd.game.config.TowerType.ROCKET_POD -> Color(0xFFFF4500)
        }
        val center = Offset(tower.x, tower.y)
        // Base platform (dark circle)
        drawCircle(color = Color(0xFF1A1F33), radius = 22f, center = center)
        // Tower body
        drawCircle(color = color, radius = 14f, center = center)
        // Level indicator (small ring)
        if (tower.level >= 2) {
            drawCircle(
                color = color,
                radius = 18f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        if (tower.level >= 3) {
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = 22f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }
    }
}

/**
 * Draws a pulsing glow around the player for visibility.
 * Pulse rate ~2Hz based on elapsed seconds.
 */
private fun DrawScope.drawPlayerGlow(state: GameState) {
    if (state.playerIndex < 0) return
    val pos = state.positions[state.playerIndex]
    val pulse = (kotlin.math.sin(state.elapsedSeconds * 4f) + 1f) * 0.5f // 0..1
    val glowRadius = 24f + pulse * 6f
    drawCircle(
        color = Color(0xFF00E676).copy(alpha = 0.15f + pulse * 0.1f),
        radius = glowRadius,
        center = Offset(pos.x, pos.y)
    )
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
    fps: Int = 0,
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

            // FPS counter (debug)
            Text(
                text = "${fps}fps",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )

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
