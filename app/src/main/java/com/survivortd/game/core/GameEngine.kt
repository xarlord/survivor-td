package com.survivortd.game.core

import com.survivortd.game.config.WeaponType
import com.survivortd.game.systems.CombatSystem
import com.survivortd.game.systems.EnemyAISystem
import com.survivortd.game.systems.LevelUpSystem
import com.survivortd.game.systems.MovementSystem
import com.survivortd.game.systems.PickupSystem
import com.survivortd.game.systems.ProjectileSystem
import com.survivortd.game.systems.TowerSystem
import com.survivortd.game.systems.VirtualJoystick
import com.survivortd.game.systems.WaveSystem
import com.survivortd.game.systems.WeaponSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Central game engine that owns all ECS systems and the game loop.
 *
 * [#35] This class decouples game logic initialization from Compose's
 * composition lifecycle. Previously, systems and the game loop were created
 * inside [GameScreen]'s `remember{}` blocks, which only execute during
 * composition. During E2E instrumentation tests, `Thread.sleep()` blocks the
 * main thread, preventing Compose recomposition from ever running GameScreen.
 * As a result, the game loop never started and no enemies spawned.
 *
 * By creating and starting the engine synchronously in `onPlayClick`
 * (before any composition), the game runs independently of the UI thread.
 *
 * The engine runs the game loop on [Dispatchers.Default] (background thread),
 * so it continues ticking even when the main thread is blocked.
 *
 * Rendering is handled separately by [GameScreen], which reads from [state]
 * inside a Canvas `drawBehind` lambda. The engine exposes an optional
 * [onRenderTick] callback that GameScreen can use to trigger Canvas redraws.
 */
class GameEngine(
    val state: GameState
) {
    // === ALL GAME SYSTEMS ===
    val weaponSystem = WeaponSystem(state)
    val waveSystem = WaveSystem(state)
    val movementSystem = MovementSystem(state)
    val combatSystem = CombatSystem(state)
    val enemyAiSystem = EnemyAISystem(state)
    val pickupSystem = PickupSystem(state)
    val projectileSystem = ProjectileSystem(state)
    val towerSystem = TowerSystem(state)
    val levelUpSystem = LevelUpSystem(state, weaponSystem)
    val joystick = VirtualJoystick(state)

    // === RENDER CALLBACK (set by GameScreen for Canvas redraw triggers) ===
    var onRenderTick: (() -> Unit)? = null
    private var tickCount = 0L  // [#35] diagnostics

    // === GAME LOOP ===
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var gameLoop: GameLoop? = null

    /**
     * Whether the engine is currently running (game loop active).
     */
    val isRunning: Boolean get() = gameLoop?.isRunning == true

    /**
     * Initialize and start the game loop.
     *
     * - Spawns the player if not already spawned
     * - Adds the starting weapon (ASSAULT_RIFLE)
     * - Registers with TestGameBridge (debug builds)
     * - Starts the fixed-timestep game loop on a background thread
     *
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    fun start() {
        if (gameLoop?.isRunning == true) return

        // Spawn player if needed
        if (state.playerIndex < 0) {
            state.spawnPlayer()
        }

        // [#35] Ensure the player always starts with the ASSAULT_RIFLE weapon.
        if (weaponSystem.weapons.none { it.type == WeaponType.ASSAULT_RIFLE }) {
            weaponSystem.addWeapon(WeaponType.ASSAULT_RIFLE)
        }

        // Register with TestGameBridge (debug-only, no-op in release if guarded)
        com.survivortd.game.testing.TestGameBridge.register(state, weaponSystem)

        android.util.Log.i("GameEngine", "start() called — player=${state.playerIndex}, weapons=${weaponSystem.weapons.size}")

        // Create and start the game loop
        gameLoop = GameLoop(
            onUpdate = { dt ->
                if (state.isPaused || state.isGameOver) return@GameLoop
                try {
                    waveSystem.update(dt)
                    enemyAiSystem.update(dt)
                    movementSystem.update(dt)
                    combatSystem.update(dt)
                    towerSystem.update(dt)
                    weaponSystem.update(dt)
                    projectileSystem.update(dt)
                    pickupSystem.update(dt)
                    state.elapsedSeconds += dt
                    state.cleanupDeadEntities()
                    // [#35] Periodic diagnostics (every ~5 seconds at 60fps = 300 ticks)
                    if (tickCount++ % 300L == 0L) {
                        android.util.Log.i("GameEngine", "tick=$tickCount elapsed=${state.elapsedSeconds}s enemies=${state.enemies.size} isGameOver=${state.isGameOver} isPaused=${state.isPaused}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GameEngine", "Update tick exception", e)
                }
            },
            onRender = {
                onRenderTick?.invoke()
            }
        )
        gameLoop?.start(scope)
        android.util.Log.i("GameEngine", "gameLoop.start() returned, isRunning=${gameLoop?.isRunning}")
    }

    /**
     * Stop the game loop and release resources.
     * Unregisters from TestGameBridge ONLY if this engine is the active one.
     *
     * [#35] During E2E tests, multiple engines are created sequentially
     * (one per test). If engine A's dispose() runs after engine B has already
     * registered, it would clear B's registration, breaking B's snapshot.
     * We guard against this by comparing the GameState reference.
     */
    fun stop() {
        gameLoop?.stop()
        gameLoop = null
        // Only unregister if WE are still the active engine
        if (com.survivortd.game.testing.TestGameBridge.rawState() === state) {
            com.survivortd.game.testing.TestGameBridge.unregister()
        }
    }

    /**
     * Cancel the coroutine scope. Called when GameScreen is disposed.
     */
    fun dispose() {
        stop()
        scope.cancel()
    }
}
