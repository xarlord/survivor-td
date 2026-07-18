package com.survivortd.game.testing

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.systems.WeaponSystem

/**
 * Test bridge — allows E2E instrumentation tests to inspect the live [GameState]
 * during gameplay. This is ONLY registered in debug builds.
 *
 * ## Why this exists
 *
 * Compose UI tests use `Thread.sleep()` to wait for async operations. However,
 * the game loop runs on a background thread and mutates [GameState] directly.
 * Without a bridge, tests have no way to verify game mechanics (enemy spawning,
 * combat, XP, etc.) — they can only check for crashes.
 *
 * ## Usage in GameScreen
 * ```kotlin
 * TestGameBridge.register(gameState, weaponSystem)
 * ```
 *
 * ## Usage in E2E tests
 * ```kotlin
 * val snapshot = TestGameBridge.snapshot()
 * assertTrue(snapshot.enemyCount > 0)
 * ```
 *
 * [#26][#35]
 */
object TestGameBridge {

    @Volatile
    private var _gameState: GameState? = null

    @Volatile
    private var _weaponSystem: WeaponSystem? = null

    /**
     * Whether the bridge is active. Only true in debug builds after [register] is called.
     */
    val isActive: Boolean get() = _gameState != null

    /**
     * Register the live game state. Called from GameScreen on debug builds.
     */
    fun register(gameState: GameState, weaponSystem: WeaponSystem? = null) {
        _gameState = gameState
        _weaponSystem = weaponSystem
    }

    /**
     * Unregister. Called when GameScreen is disposed.
     */
    fun unregister() {
        _gameState = null
        _weaponSystem = null
    }

    /**
     * Take a thread-safe snapshot of the current game state.
     * Returns null if the bridge is not active.
     */
    fun snapshot(): GameSnapshot? {
        val state = _gameState ?: return null
        // The game loop uses the same dedicated lock for the complete update tick.
        // Locking on GameState itself was not enough after the engine refactor,
        // because the loop no longer synchronized on that monitor.
        return state.withSynchronizedAccess {
            val playerIdx = state.playerIndex
            val player = state.players.getOrNull(playerIdx)
            // Count live enemies (tag == ENEMY and not dead)
            var enemyCount = 0
            for (i in state.tags.indices) {
                if (state.tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY) {
                    val hp = state.healths.getOrNull(i)
                    if (hp != null && !hp.isDead) enemyCount++
                }
            }
            GameSnapshot(
                playerCount = if (playerIdx >= 0) 1 else 0,
                playerHp = player?.let {
                    state.healths.getOrNull(playerIdx)?.currentHp ?: 0f
                } ?: 0f,
                playerMaxHp = player?.let {
                    state.healths.getOrNull(playerIdx)?.maxHp ?: 0f
                } ?: 0f,
                playerIsDead = state.healths.getOrNull(playerIdx)?.isDead ?: false,
                playerLevel = player?.level ?: 0,
                playerPositionX = state.positions.getOrNull(playerIdx)?.x ?: 0f,
                playerPositionY = state.positions.getOrNull(playerIdx)?.y ?: 0f,
                enemyCount = enemyCount,
                elapsedTime = state.elapsedSeconds,
                score = state.score,
                weaponCount = _weaponSystem?.weapons?.size ?: 0
            )
        }
    }

    /**
     * Get the raw game state for advanced assertions. Use with caution —
     * the state is mutated on a background thread.
     */
    fun rawState(): GameState? = _gameState

    /**
     * Immutable snapshot of game state at a point in time.
     */
    data class GameSnapshot(
        val playerCount: Int,
        val playerHp: Float,
        val playerMaxHp: Float,
        val playerIsDead: Boolean,
        val playerLevel: Int,
        val playerPositionX: Float,
        val playerPositionY: Float,
        val enemyCount: Int,
        val elapsedTime: Float,
        val score: Long,
        val weaponCount: Int
    )
}
