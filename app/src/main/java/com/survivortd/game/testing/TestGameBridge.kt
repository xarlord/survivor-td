package com.survivortd.game.testing

import com.survivortd.game.components.TagComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.systems.WeaponSystem

/**
 * Debug-only bridge that exposes live [GameState] to instrumented (E2E) tests.
 *
 * In release builds, [instance] is always null and all getters return safe defaults,
 * so this class has zero overhead in production.
 *
 * Usage in GameScreen (debug only):
 * ```
 * TestGameBridge.register(gameState, weaponSystem)
 * ```
 *
 * Usage in E2E tests:
 * ```
 * val snapshot = TestGameBridge.snapshot()
 * assertTrue(snapshot.enemyCount > 0)
 * ```
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
        android.util.Log.i("TestGameBridge", "register() — state=${gameState.hashCode()} weapons=${weaponSystem?.hashCode()}")
    }

    /**
     * Clear the registration. Called from GameScreen on dispose (debug builds).
     */
    fun unregister() {
        android.util.Log.i("TestGameBridge", "unregister() — old state=${_gameState?.hashCode()}", Exception("unregister stacktrace"))
        _gameState = null
        _weaponSystem = null
    }

    /**
     * Take a point-in-time snapshot of all relevant game state for test assertions.
     * Returns null if bridge is not active (release build or game not started).
     */
    fun snapshot(): GameSnapshot? {
        val state = _gameState ?: return null
        // [#35] Synchronize on state to get a consistent snapshot while the
        // game loop may be mutating the arrays on a background thread.
        val result = synchronized(state) {
            GameSnapshot.from(state, _weaponSystem)
        }
        android.util.Log.i("TestGameBridge", "snapshot() — state=${state.hashCode()} tags=${state.tags.size} enemies=${result.enemyCount} elapsed=${result.elapsedSeconds}")
        return result
    }

    /**
     * Get the raw GameState (for advanced test scenarios).
     * Returns null if not active.
     */
    fun rawState(): GameState? = _gameState

    // ==================================================================
    // SNAPSHOT DATA CLASS
    // ==================================================================

    /**
     * Immutable point-in-time snapshot of all game state relevant to tests.
     * Captured atomically to avoid race conditions during game loop updates.
     */
    data class GameSnapshot(
        // === ENTITY COUNTS ===
        val enemyCount: Int,
        val projectileCount: Int,
        val pickupCount: Int,
        val playerCount: Int,
        val towerCount: Int,
        val totalEntities: Int,

        // === PLAYER STATS ===
        val playerHp: Float,
        val playerMaxHp: Float,
        val playerLevel: Int,
        val playerCurrentXp: Int,
        val playerXpToNext: Int,
        val playerGold: Int,
        val playerScrap: Int,
        val playerIsDead: Boolean,
        val playerPositionX: Float,
        val playerPositionY: Float,

        // === GAME STATE ===
        val elapsedSeconds: Float,
        val isGameOver: Boolean,
        val isPaused: Boolean,
        val isVictory: Boolean,
        val score: Long,
        val pendingLevelUps: Int,

        // === WEAPONS & PASSIVES ===
        val weaponCount: Int,
        val weaponTypes: List<String>,
        val weaponLevels: List<Int>,
        val passiveCount: Int,

        // === ENEMY DETAILS (first 10 for sampling) ===
        val enemyTypes: List<String>,
        val enemyHps: List<Float>,
        val enemyPositions: List<Pair<Float, Float>>,

        // === WAVE INFO ===
        val isBuildPhase: Boolean,
        val waveTimeRemaining: Float
    ) {
        companion object {
            fun from(state: GameState, weaponSystem: WeaponSystem?): GameSnapshot {
                var enemyCount = 0
                var projectileCount = 0
                var pickupCount = 0
                var playerCount = 0
                var towerCount = 0
                val enemyTypes = mutableListOf<String>()
                val enemyHps = mutableListOf<Float>()
                val enemyPositions = mutableListOf<Pair<Float, Float>>()

                for (i in state.tags.indices) {
                    val tag = state.tags[i].tag
                    when (tag) {
                        TagComponent.EntityTag.ENEMY -> {
                            enemyCount++
                            if (enemyTypes.size < 10 && i < state.enemies.size) {
                                enemyTypes.add(state.enemies[i].type.name)
                                enemyHps.add(state.healths.getOrNull(i)?.currentHp ?: 0f)
                                val pos = state.positions.getOrNull(i)
                                enemyPositions.add(Pair(pos?.x ?: 0f, pos?.y ?: 0f))
                            }
                        }
                        TagComponent.EntityTag.PROJECTILE -> projectileCount++
                        TagComponent.EntityTag.PICKUP -> pickupCount++
                        TagComponent.EntityTag.PLAYER -> playerCount++
                        TagComponent.EntityTag.TOWER -> towerCount++
                        else -> {}
                    }
                }

                // Player stats
                val pid = state.playerIndex
                val player = state.players.getOrNull(pid)
                val playerHealth = state.healths.getOrNull(pid)
                val playerPos = state.positions.getOrNull(pid)

                // Weapon info
                val weaponTypes = weaponSystem?.weapons?.map { it.type.name } ?: emptyList()
                val weaponLevels = weaponSystem?.weapons?.map { it.level } ?: emptyList()

                return GameSnapshot(
                    enemyCount = enemyCount,
                    projectileCount = projectileCount,
                    pickupCount = pickupCount,
                    playerCount = playerCount,
                    towerCount = towerCount,
                    totalEntities = state.tags.size,

                    playerHp = playerHealth?.currentHp ?: 0f,
                    playerMaxHp = playerHealth?.maxHp ?: 0f,
                    playerLevel = player?.level ?: 1,
                    playerCurrentXp = player?.currentXp ?: 0,
                    playerXpToNext = player?.xpToNext ?: 0,
                    playerGold = player?.gold ?: 0,
                    playerScrap = player?.scrap ?: 0,
                    playerIsDead = playerHealth?.isDead ?: false,
                    playerPositionX = playerPos?.x ?: 0f,
                    playerPositionY = playerPos?.y ?: 0f,

                    elapsedSeconds = state.elapsedSeconds,
                    isGameOver = state.isGameOver,
                    isPaused = state.isPaused,
                    isVictory = state.isVictory,
                    score = state.score,
                    pendingLevelUps = state.pendingLevelUps,

                    weaponCount = weaponTypes.size,
                    weaponTypes = weaponTypes,
                    weaponLevels = weaponLevels,
                    passiveCount = weaponSystem?.passives?.size ?: 0,

                    enemyTypes = enemyTypes,
                    enemyHps = enemyHps,
                    enemyPositions = enemyPositions,

                    isBuildPhase = false, // Set externally by WaveSystem if needed
                    waveTimeRemaining = (900f - state.elapsedSeconds).coerceAtLeast(0f)
                )
            }
        }
    }
}
