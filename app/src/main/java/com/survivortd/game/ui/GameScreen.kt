package com.survivortd.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.components.RenderComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.WeaponType
import com.survivortd.game.data.HeroId
import com.survivortd.game.core.GameState
import com.survivortd.game.core.GameLoop
import androidx.compose.ui.platform.LocalContext
import com.survivortd.game.data.SaveManager
import com.survivortd.game.systems.AudioManager
import com.survivortd.game.systems.LevelUpSystem
import com.survivortd.game.systems.MetaProgression
import com.survivortd.game.systems.UpgradeChoice
import com.survivortd.game.systems.VirtualJoystick
import com.survivortd.game.systems.WeaponSystem
import com.survivortd.game.utils.FrustumCuller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    heroId: String = "COMMANDER",
    onExit: () -> Unit
) {
    val gameState = remember { GameState() }
    GameScreen(gameState = gameState, heroId = heroId, onGameOver = onExit, onExit = onExit)
}

@Composable
fun GameScreen(
    gameState: GameState,
    heroId: String = "COMMANDER",
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

    // [#94] Pause state
    var isPaused by remember { mutableStateOf(false) }

    // [#112] System back button pauses the game
    BackHandler(enabled = true) {
        isPaused = true
    }

    // [#89] Run summary (death screen) state
    var showRunSummary by remember { mutableStateOf(false) }
    var summaryKills by remember { mutableIntStateOf(0) }
    var summaryGold by remember { mutableIntStateOf(0) }
    var summaryLevel by remember { mutableIntStateOf(1) }
    var summaryTime by remember { mutableLongStateOf(0L) }
    var summaryWeapons by remember { mutableIntStateOf(0) }
    var summaryIsVictory by remember { mutableStateOf(false) }
    var hudFps by remember { mutableIntStateOf(0) }

    // [#97] Wave HUD state
    var hudWave by remember { mutableIntStateOf(0) }
    var hudWaveText by remember { mutableStateOf("") }

    // [#95] Tutorial state
    var showTutorial by remember { mutableStateOf(false) }

    // [#92] Meta-progression
    val metaProgression = remember { mutableStateOf(MetaProgression()) }
    val context = LocalContext.current

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
    val combatSystem = remember { com.survivortd.game.systems.CombatSystem(gameState, gameFeelSystem, com.survivortd.game.systems.MetaProgression()) }
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
    // [#119] HeroPassiveSystem — applies hero-specific passive bonuses.
    val heroPassiveSystem = remember { com.survivortd.game.systems.HeroPassiveSystem() }

    // Resolve HeroId from string parameter
    val hero = remember(heroId) {
        runCatching { HeroId.valueOf(heroId) }.getOrDefault(HeroId.DEFAULT)
    }

    // [#113] Initialize AudioManager with preloaded SFX
    val audioManager = remember(context) {
        AudioManager.getInstance(context).also {
            AudioManager.SfxType.entries.forEach { sfx -> it.loadSfx(sfx) }
        }
    }

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
        gameState.heroId = hero.name
        // [#119] Apply hero passives and give hero's starting weapon.
        heroPassiveSystem.initHero(hero, gameState)
        if (weaponSystem.weapons.isEmpty()) {
            weaponSystem.addWeapon(hero.startingWeapon)
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
                if (gameState.isPaused || gameState.isGameOver || isPaused) return@GameLoop
                // Game feel first — returns effective dt (0 during hit-stop)
                val effectiveDt = gameFeelSystem.update(dt)
                if (effectiveDt <= 0f) return@GameLoop  // Hit-stop freeze

                // [#21] Wave system FIRST — spawns enemies for other systems to process
                waveSystem.update(effectiveDt)
                // [#119] Hero passives (must run before movement/combat for berserker/scout/shielder)
                heroPassiveSystem.applyPassive(hero, gameState, effectiveDt)
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
                val killed = gameState.cleanupDeadEntities()
                repeat(killed) { waveSystem.onEnemyKilled() }
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

    // [#92] Load meta-progression and apply bonuses at game start
    LaunchedEffect(Unit) {
        val meta = SaveManager.loadMeta(context).first()
        metaProgression.value = meta
        MetaProgression.applyToGameState(meta, gameState)
        // [#95] Check first-run tutorial
        val settings = SaveManager.loadSettings(context).first()
        showTutorial = settings.isFirstRun
    }

    DisposableEffect(Unit) {
        onDispose {
            gameLoop.stop()
            gameLoopScope.cancel()
            // [#26] Unregister from TestGameBridge (debug only)
            com.survivortd.game.testing.TestGameBridge.unregister()
            // [#113] Release AudioManager on dispose
            audioManager.release()
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
            wave = hudWave,
            waveText = hudWaveText,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // [#97] Wave Announcement Overlay
        if (gameState.waveAnnouncementTimer > 0f && gameState.waveAnnouncementText.isNotEmpty()) {
            val isBoss = gameState.waveAnnouncementText.contains("BOSS")
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameState.waveAnnouncementText,
                    fontSize = if (isBoss) 28.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBoss) Color(0xFFFF1744) else Color(0xFFFFD700),
                    textAlign = TextAlign.Center
                )
            }
        }

        // === LAYER 3: Level-Up Dialog (non-blocking, game continues) ===
        if (levelUpChoices.isNotEmpty()) {
            LevelUpDialog(
                level = hudLevel,
                choices = levelUpChoices,
                onChoiceSelected = { choice ->
                    levelUpSystem.applyChoice(choice)
                    // [#113] Play level-up SFX
                    AudioManager.getInstance().playSfx(AudioManager.SfxType.LEVEL_UP)
                    if (gameState.pendingLevelUps > 0) {
                        levelUpChoices = levelUpSystem.generateChoices()
                    } else {
                        levelUpChoices = emptyList()
                    }
                },
                onTimeout = { choice ->
                    levelUpSystem.applyChoice(choice)
                    // [#113] Play level-up SFX
                    AudioManager.getInstance().playSfx(AudioManager.SfxType.LEVEL_UP)
                    if (gameState.pendingLevelUps > 0) {
                        levelUpChoices = levelUpSystem.generateChoices()
                    } else {
                        levelUpChoices = emptyList()
                    }
                }
            )
        }

        // === LAYER 4: Pause Button (#94) ===
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333A4D))
                .testTag("pause_button")
        ) {
            Text(
                text = "⏸",
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { isPaused = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // === LAYER 5: Pause Overlay (#94) ===
        if (isPaused && !showRunSummary) {
            PauseOverlay(
                onResume = { isPaused = false },
                onQuit = {
                    isPaused = false
                    onExit()
                }
            )
        }

        // === LAYER 6: Run Summary / Death Screen (#89) ===
        if (showRunSummary) {
            RunSummaryScreen(
                kills = summaryKills,
                gold = summaryGold,
                level = summaryLevel,
                timeSeconds = summaryTime,
                weapons = summaryWeapons,
                isVictory = summaryIsVictory,
                onPlayAgain = {
                    showRunSummary = false
                    gameState.reset()
                    if (gameState.playerIndex < 0) {
                        gameState.spawnPlayer()
                    }
                    gameState.heroId = hero.name
                    heroPassiveSystem.initHero(hero, gameState)
                    if (weaponSystem.weapons.isEmpty()) {
                        weaponSystem.addWeapon(hero.startingWeapon)
                    }
                },
                onMenu = {
                    showRunSummary = false
                    onExit()
                }
            )
        }

        // === LAYER 7: Minimap (#98) ===
        MinimapView(
            gameState = gameState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = GameConfig.MINIMAP_MARGIN_DP.dp,
                    bottom = GameConfig.MINIMAP_MARGIN_DP.dp
                )
        )

        // === LAYER 8: Tutorial Overlay (#95) ===
        if (showTutorial) {
            TutorialOverlay(onDismiss = {
                showTutorial = false
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    SaveManager.markFirstRunComplete(context)
                }
            })
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
            hudWave = gameState.currentWave
            hudWaveText = if (gameState.waveAnnouncementTimer > 0f) gameState.waveAnnouncementText else ""

            // Check for level-up → generate choices (game continues, no pause)
            if (gameState.pendingLevelUps > 0 && levelUpChoices.isEmpty()) {
                levelUpChoices = levelUpSystem.generateChoices()
            }

            // [#116] Victory condition — survived 15 minutes
            if (!gameState.isGameOver && !gameState.isVictory && gameState.elapsedSeconds >= GameConfig.MATCH_DURATION_SECONDS) {
                gameState.isVictory = true
                gameState.isGameOver = true
                gameState.gameOverHandled = true
                summaryKills = gameState.totalKills
                summaryGold = gameState.goldCollected + GameConfig.GOLD_COMPLETION_BONUS
                summaryLevel = hudLevel
                summaryTime = 0L
                summaryWeapons = weaponSystem.weapons.size
                summaryIsVictory = true
                delay(1000)
                showRunSummary = true
            }

            // [#89] Game over — capture stats and show run summary
                if (gameState.isGameOver && !gameState.gameOverHandled) {
                    gameState.gameOverHandled = true
                    // [#113] Play death SFX
                    AudioManager.getInstance().playSfx(AudioManager.SfxType.PLAYER_DEATH)
                    summaryKills = gameState.totalKills
                    summaryGold = gameState.goldCollected
                    summaryLevel = hudLevel
                    summaryTime = hudTime
                    summaryWeapons = weaponSystem.weapons.size
                    // [#92] Save gold to meta-progression
                    val currentMeta = metaProgression.value
                    currentMeta.addGold(gameState.goldCollected)
                    SaveManager.saveMeta(context, currentMeta)
                    metaProgression.value = currentMeta
                    // Delay 1.5 seconds before showing summary (death processing)
                    delay(1500)
                    showRunSummary = true
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

        // (#115) Frustum culler — compute world-space view bounds for off-screen skip
        val culler = FrustumCuller().apply {
            margin = 100f / scale  // margin in world units
            update(
                camX = gameState.cameraX - (size.width / scale) / 2f,
                camY = gameState.cameraY - (size.height / scale) / 2f,
                viewWidth = size.width / scale,
                viewHeight = size.height / scale
            )
        }

        withTransform({
            translate(left = camX, top = camY)
            scale(scale, scale)
        }) {
            drawGameBackground(gameState)
            drawEntities(gameState, culler)
            drawParticles(particleSystem, culler)
            drawTowers(towerSystem, culler)
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
 * (#115) Iterates arrays directly with frustum culling to skip off-screen entities.
 * Pre-calculates values outside the loop. Reuses Path objects to avoid allocations.
 */
private fun DrawScope.drawEntities(state: GameState, culler: FrustumCuller) {
    val positions = state.positions
    val healths = state.healths
    val renders = state.renders
    val tags = state.tags
    val statusEffects = state.statusEffects
    val triPath = androidx.compose.ui.graphics.Path()
    val diaPath = androidx.compose.ui.graphics.Path()
    val sinVal = kotlin.math.sin(state.elapsedSeconds * GameConfig.PICKUP_PULSE_SPEED)
    val pulse = (sinVal + 1f) * 0.5f

    for (i in positions.indices) {
        if (i >= healths.size || healths[i].isDead) continue
        if (i >= renders.size) continue

        val pos = positions[i]
        val px = pos.x
        val py = pos.y

        // (#115) Frustum culling — skip off-screen entities
        if (!culler.isVisible(px, py)) continue

        val render = renders[i]
        val colorInt = render.color
        var radius = render.radius

        // (#85) Draw status effect glow behind enemies
        if (i < tags.size && tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY
            && i < statusEffects.size && statusEffects[i].effects.isNotEmpty()
        ) {
            drawStatusEffectGlow(Offset(px, py), radius, statusEffects[i].effects)
        }

        // (#91) Pulse animation for pickups — pre-calculated outside loop
        val isPickup = i < tags.size &&
            tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.PICKUP
        if (isPickup) {
            radius = radius * (0.85f + pulse * 0.15f)
        }

        // Draw shape — single path to avoid code duplication
        when (render.shape) {
            RenderComponent.RenderShape.CIRCLE -> {
                drawCircle(
                    color = Color(colorInt.toLong()),
                    radius = radius,
                    center = Offset(px, py)
                )
            }
            RenderComponent.RenderShape.RECT -> {
                drawRect(
                    color = Color(colorInt.toLong()),
                    topLeft = Offset(px - radius, py - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
            RenderComponent.RenderShape.TRIANGLE -> {
                triPath.reset()
                triPath.moveTo(px, py - radius)
                triPath.lineTo(px - radius, py + radius)
                triPath.lineTo(px + radius, py + radius)
                triPath.close()
                drawPath(triPath, color = Color(colorInt.toLong()))
            }
            RenderComponent.RenderShape.DIAMOND -> {
                diaPath.reset()
                diaPath.moveTo(px, py - radius)
                diaPath.lineTo(px + radius, py)
                diaPath.lineTo(px, py + radius)
                diaPath.lineTo(px - radius, py)
                diaPath.close()
                drawPath(diaPath, color = Color(colorInt.toLong()))
            }
            // (#91) CROSS shape for health pickups
            RenderComponent.RenderShape.CROSS -> {
                val armWidth = radius * 0.35f
                val crossColor = Color(colorInt.toLong())
                drawRect(
                    color = crossColor,
                    topLeft = Offset(px - armWidth, py - radius),
                    size = androidx.compose.ui.geometry.Size(armWidth * 2, radius * 2)
                )
                drawRect(
                    color = crossColor,
                    topLeft = Offset(px - radius, py - armWidth),
                    size = androidx.compose.ui.geometry.Size(radius * 2, armWidth * 2)
                )
            }
        }
    }
}

/**
 * Draws a glow outline behind enemies with active status effects.
 * (#115) Avoids allocations: uses pre-computed color map and inline sort logic.
 */
private fun drawStatusEffectGlowColor(effectType: com.survivortd.game.config.StatusEffectType): Int? {
    return when (effectType) {
        com.survivortd.game.config.StatusEffectType.BURN -> 0xFFFF1744.toInt()
        com.survivortd.game.config.StatusEffectType.POISON -> 0xFF76FF03.toInt()
        com.survivortd.game.config.StatusEffectType.FREEZE -> 0xFF81D4FA.toInt()
        com.survivortd.game.config.StatusEffectType.STUN -> 0xFFFFFF00.toInt()
        com.survivortd.game.config.StatusEffectType.SLOW -> 0xFFCE93D8.toInt()
        com.survivortd.game.config.StatusEffectType.BLEED -> 0xFFFF1744.toInt()
        com.survivortd.game.config.StatusEffectType.SLOW_ATTACK -> 0xFFCE93D8.toInt()
    }
}

private fun DrawScope.drawStatusEffectGlow(
    center: Offset,
    radius: Float,
    effects: List<com.survivortd.game.components.StatusEffectsComponent.ActiveStatus>
) {
    // Only show top 2 effects by duration (avoid allocating sorted list)
    var maxDur1 = -1f
    var maxDur2 = -1f
    var effect1: com.survivortd.game.components.StatusEffectsComponent.ActiveStatus? = null
    var effect2: com.survivortd.game.components.StatusEffectsComponent.ActiveStatus? = null
    for (effect in effects) {
        if (effect.duration > maxDur1) {
            effect2 = effect1
            maxDur2 = maxDur1
            effect1 = effect
            maxDur1 = effect.duration
        } else if (effect.duration > maxDur2) {
            effect2 = effect
            maxDur2 = effect.duration
        }
    }

    effect1?.let { effect ->
        val colorInt = drawStatusEffectGlowColor(effect.type) ?: return@let
        drawCircle(
            color = Color(colorInt.toLong()).copy(alpha = 0.5f),
            radius = radius + 4f,
            center = center
        )
    }
    effect2?.let { effect ->
        val colorInt = drawStatusEffectGlowColor(effect.type) ?: return@let
        drawCircle(
            color = Color(colorInt.toLong()).copy(alpha = 0.3f),
            radius = radius + 7f,
            center = center
        )
    }
}

/**
 * Draws all active particles from the particle system.
 * (#115) Particles fade out based on remaining lifetime.
 * Frustum culling skips off-screen particles.
 */
private fun DrawScope.drawParticles(
    particleSystem: com.survivortd.game.systems.ParticleSystem,
    culler: FrustumCuller
) {
    val particles = particleSystem.particles
    for (i in particles.indices) {
        val p = particles[i]
        // (#115) Frustum cull particles
        if (!culler.isVisible(p.x, p.y)) continue
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
 * (#115) Frustum culling skips off-screen towers.
 */
private fun DrawScope.drawTowers(
    towerSystem: com.survivortd.game.systems.TowerSystem,
    culler: FrustumCuller
) {
    for (tower in towerSystem.towers) {
        if (!culler.isVisible(tower.x, tower.y)) continue
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
    wave: Int = 0,
    waveText: String = "",
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

            // Timer — (#115) use StringBuilder to avoid format() allocation
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", color = Color(0xFF9E9E9E), fontSize = 9.sp)
                val mins = secondsRemaining / 60
                val secs = secondsRemaining % 60
                Text(
                    text = "$mins:${secs.toString().padStart(2, '0')}",
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

        // [#97] Wave + Time info row
        if (wave > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Wave $wave",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * [#94] Pause overlay with Resume and Quit options.
 */
@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E2E))
                .padding(32.dp)
        ) {
            Text(
                text = "PAUSED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676))
                    .clickable(onClick = onResume)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  RESUME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0E1A)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF1744))
                    .clickable(onClick = onQuit)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕  QUIT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * [#89] Run Summary / Death Screen — shows stats after game over.
 */
@Composable
private fun RunSummaryScreen(
    kills: Int,
    gold: Int,
    level: Int,
    timeSeconds: Long,
    weapons: Int,
    isVictory: Boolean = false,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    val mins = (900 - timeSeconds) / 60
    val secs = (900 - timeSeconds) % 60
    val timeString = "$mins:${secs.toString().padStart(2, '0')}"

    val titleText = if (isVictory) "VICTORY!" else "RUN OVER"
    val titleColor = if (isVictory) Color(0xFF00E676) else Color(0xFFFF1744)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E2E))
                .padding(32.dp)
        ) {
            Text(
                text = titleText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(24.dp))

            StatRow("⏱ Time", timeString)
            StatRow("🏆 Level", "$level")
            StatRow("💀 Kills", "$kills")
            StatRow("🪙 Gold", "$gold")
            if (isVictory) {
                StatRow("🎁 Bonus", "+${GameConfig.GOLD_COMPLETION_BONUS} gold")
            }
            StatRow("⚔ Weapons", "$weapons")
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676))
                    .clickable(onClick = onPlayAgain)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  PLAY AGAIN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0E1A)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF333A4D))
                    .clickable(onClick = onMenu)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/**
 * [#98] Minimap — shows a scaled-down view of the world with entity dots.
 * Player = blue, enemies = red, boss = gold, towers = green.
 * Viewport rectangle shows the current camera view area.
 */
@Composable
private fun MinimapView(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(GameConfig.MINIMAP_SIZE_DP.dp)
    ) {
        val worldW = GameConfig.WORLD_WIDTH
        val worldH = GameConfig.WORLD_HEIGHT
        val mapSize = size.width
        val scaleX = mapSize / worldW
        val scaleY = mapSize / worldH
        val scale = minOf(scaleX, scaleY)
        val offsetX = (mapSize - worldW * scale) / 2f
        val offsetY = (mapSize - worldH * scale) / 2f

        // Background
        drawCircle(
            color = Color(0xFF0A0E1A).copy(alpha = GameConfig.MINIMAP_ALPHA),
            radius = mapSize / 2f,
            center = center
        )
        // Border
        drawCircle(
            color = Color(0xFF333A4D),
            radius = mapSize / 2f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )

        // Entity dots
        val positions = gameState.positions
        val healths = gameState.healths
        val tags = gameState.tags
        val renders = gameState.renders

        for (i in positions.indices) {
            if (i >= healths.size || healths[i].isDead) continue
            if (i >= tags.size) continue

            val x = positions[i].x * scale + offsetX
            val y = positions[i].y * scale + offsetY
            val color = when (tags[i].tag) {
                TagComponent.EntityTag.PLAYER -> Color(0xFF42A5F5)
                TagComponent.EntityTag.ENEMY -> {
                    // Boss check: large radius
                    val isBoss = i < renders.size && renders[i].radius > 30f
                    if (isBoss) Color(0xFFFFD700) else Color(0xFFFF1744)
                }
                TagComponent.EntityTag.TOWER -> Color(0xFF00E676)
                else -> continue
            }
            drawCircle(
                color = color,
                radius = GameConfig.MINIMAP_DOT_RADIUS,
                center = Offset(x, y)
            )
        }

        // Viewport rectangle (camera area)
        val camX = gameState.cameraX * scale + offsetX
        val camY = gameState.cameraY * scale + offsetY
        val vpW = GameConfig.CAMERA_WIDTH * scale
        val vpH = GameConfig.CAMERA_HEIGHT * scale
        drawRect(
            color = Color.White.copy(alpha = GameConfig.MINIMAP_VIEWPORT_ALPHA),
            topLeft = Offset(camX - vpW / 2f, camY - vpH / 2f),
            size = androidx.compose.ui.geometry.Size(vpW, vpH)
        )
    }
}
