package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.ChapterConfig
import com.survivortd.game.config.GameBalance
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wave System — manages continuous enemy spawning and boss waves.
 *
 * Spawning logic:
 * - Continuously spawns enemies from screen edges
 * - Spawn rate increases over time
 * - Enemy type pool changes by minute threshold
 * - Boss spawns at configured times (5, 10, 15 min)
 * - Normal spawning pauses during boss fights (10s)
 * - Build phase after boss defeated (10s)
 */
class WaveSystem(
    private val state: GameState,
    private val chapter: ChapterConfig = ChapterConfig.WASTELAND
) {
    private var spawnTimer = 0f
    private var spawnInterval = GameConfig.BASE_SPAWN_INTERVAL   // [#49] Use config, was hardcoded 1.5f

    // Boss tracking
    private val bossesSpawned = mutableSetOf<Int>()
    private var bossActive = false
    private var bossPauseTimer = 0f
    private var buildPhaseTimer = 0f

    var isBuildPhase = false
        private set

    /** Total enemies spawned this match */
    var totalSpawned = 0
        private set

    /**
     * Main update — called every tick.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        val minutes = state.elapsedSeconds / 60f

        // Check for boss spawn
        val bossIndex = chapter.shouldSpawnBoss(state.elapsedSeconds)
        if (bossIndex >= 0 && bossIndex !in bossesSpawned) {
            spawnBoss(bossIndex)
            bossesSpawned.add(bossIndex)
        }

        // Handle boss active state — pause normal spawning
        if (bossActive) {
            if (isBossDead()) {
                bossActive = false
                // Start build phase
                buildPhaseTimer = 10f
                isBuildPhase = true
            }
            return
        }

        // Handle build phase
        if (isBuildPhase) {
            buildPhaseTimer -= dt
            if (buildPhaseTimer <= 0f) {
                isBuildPhase = false
            }
            return  // No spawning during build phase
        }

        // Normal spawning
        spawnTimer += dt
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            // [#49] Recompute interval from the canonical GameBalance formula
            // so runtime spawning matches the GDD validation tests exactly.
            // Was a divergent hardcoded formula (1.5 - min*0.05) ignoring config.
            spawnInterval = GameBalance.spawnIntervalAtMinute(minutes.toInt())
            spawnEnemy()
        }
    }

    /**
     * Spawn a regular enemy from a random screen edge.
     */
    private fun spawnEnemy() {
        val pool = chapter.getActivePool(state.elapsedSeconds)
        val enemyType = pickWeighted(pool)

        // Spawn at random screen edge
        val edge = Random.nextInt(4)
        val (x, y) = when (edge) {
            0 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to -30f          // Top
            1 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to GameConfig.WORLD_HEIGHT + 30f  // Bottom
            2 -> -30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT          // Left
            else -> GameConfig.WORLD_WIDTH + 30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT // Right
        }

        // HP scaling: +10% per minute
        val minutes = state.elapsedSeconds / 60f
        val hpScale = 1f + minutes * 0.1f

        state.spawnEnemy(x, y, enemyType, hpScale = hpScale)
        totalSpawned++
    }

    /**
     * Spawn a boss at center-top of screen.
     */
    private fun spawnBoss(index: Int) {
        bossActive = true
        val bossHp = chapter.bossHp * (1f + index * 0.5f)  // Each boss tougher
        state.spawnEnemy(
            x = GameConfig.WORLD_WIDTH / 2f,
            y = 100f,
            enemyType = chapter.bossType,
            hpScale = bossHp / 4000f  // Base boss HP is 4000
        )
        totalSpawned++
    }

    /**
     * Check if the boss is dead.
     */
    private fun isBossDead(): Boolean {
        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (state.enemies[i].type == chapter.bossType && !state.healths[i].isDead) {
                return false
            }
        }
        return true
    }

    /**
     * Pick a weighted random enemy type.
     */
    private fun pickWeighted(pool: Map<EnemyComponent.EnemyData, Int>): EnemyComponent.EnemyData {
        val total = pool.values.sum()
        var roll = Random.nextInt(total)
        for ((type, weight) in pool) {
            roll -= weight
            if (roll < 0) return type
        }
        return EnemyComponent.EnemyData.ZOMBIE
    }
}
