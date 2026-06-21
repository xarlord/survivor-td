package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameBalance
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Spawn director — controls enemy spawn rate, type distribution, and boss timing.
 *
 * Spawn rate scales linearly with elapsed time:
 *   interval = max(MIN_INTERVAL, BASE - minute * scale)
 *
 * Enemy types are weighted by minute:
 *   Minutes 0-3: Mostly zombies (80%) + runners (20%)
 *   Minutes 3-6: + brutes (15%), spitters (10%)
 *   Minutes 6-10: + bombers (10%), healers (5%)
 *   Minutes 10-15: All types + elite chance increases
 */
class SpawnDirector(
    private val state: GameState,
    private val random: Random = Random.Default
) {
    private var spawnTimer = 0f
    private var totalSpawned = 0

    /**
     * Called every physics tick.
     * @param dt Delta time in seconds
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        val minute = (state.elapsedSeconds / 60f).toInt()

        // Check boss spawns
        for (bossMinute in GameConfig.BOSS_TIMES_MINUTES) {
            val bossTime = bossMinute * 60f
            // Spawn boss at the exact second (within 1 tick)
            if (state.elapsedSeconds >= bossTime && state.elapsedSeconds - dt < bossTime) {
                spawnBoss(bossMinute)
            }
        }

        // Regular enemy spawning
        spawnTimer += dt
        val interval = GameBalance.spawnIntervalAtMinute(minute)

        if (spawnTimer >= interval) {
            spawnTimer = 0f
            spawnEnemy(minute)
            totalSpawned++
        }
    }

    /**
     * Spawn a regular enemy at a random edge of the screen.
     */
    private fun spawnEnemy(minute: Int) {
        val type = rollEnemyType(minute)
        val (x, y) = randomEdgePosition()

        val hpScale = 1f + GameConfig.ENEMY_HP_SCALE * minute
        val dmgScale = 1f + GameConfig.ENEMY_DAMAGE_SCALE * minute

        state.spawnEnemy(x, y, type, hpScale, dmgScale)
    }

    /**
     * Spawn a boss at the top of the arena.
     */
    private fun spawnBoss(bossMinute: Int) {
        val (x, y) = randomEdgePosition()
        val bossHp = when (bossMinute) {
            5 -> 4000f
            10 -> 10000f
            15 -> 25000f
            else -> 4000f
        }
        state.spawnEnemy(
            x = x,
            y = y,
            enemyType = EnemyComponent.EnemyData.BOSS,
            hpScale = bossHp / 4000f,  // Base boss HP in spawnEnemy is 4000
            damageScale = 1f + GameConfig.ENEMY_DAMAGE_SCALE * bossMinute
        )
    }

    /**
     * Weighted random selection of enemy type based on elapsed minutes.
     */
    private fun rollEnemyType(minute: Int): EnemyComponent.EnemyData {
        val r = random.nextFloat()
        return when {
            minute < 3 -> {
                if (r < 0.80f) EnemyComponent.EnemyData.ZOMBIE
                else EnemyComponent.EnemyData.RUNNER
            }
            minute < 6 -> when {
                r < 0.55f -> EnemyComponent.EnemyData.ZOMBIE
                r < 0.75f -> EnemyComponent.EnemyData.RUNNER
                r < 0.88f -> EnemyComponent.EnemyData.BRUTE
                else -> EnemyComponent.EnemyData.SPIDER
            }
            minute < 10 -> when {
                r < 0.40f -> EnemyComponent.EnemyData.ZOMBIE
                r < 0.55f -> EnemyComponent.EnemyData.RUNNER
                r < 0.68f -> EnemyComponent.EnemyData.BRUTE
                r < 0.78f -> EnemyComponent.EnemyData.BOMBER
                r < 0.88f -> EnemyComponent.EnemyData.HEALER
                else -> EnemyComponent.EnemyData.SHIELDER
            }
            else -> when {
                r < 0.30f -> EnemyComponent.EnemyData.ZOMBIE
                r < 0.42f -> EnemyComponent.EnemyData.RUNNER
                r < 0.54f -> EnemyComponent.EnemyData.BRUTE
                r < 0.64f -> EnemyComponent.EnemyData.BOMBER
                r < 0.74f -> EnemyComponent.EnemyData.HEALER
                r < 0.84f -> EnemyComponent.EnemyData.SHIELDER
                else -> EnemyComponent.EnemyData.FLYER
            }
        }
    }

    /**
     * Random position just outside screen edges.
     * Enemies spawn off-screen and walk towards the player.
     */
    private fun randomEdgePosition(): Pair<Float, Float> {
        val margin = 60f
        return when (random.nextInt(4)) {
            0 -> Pair(random.nextFloat() * GameConfig.WORLD_WIDTH, -margin)              // Top
            1 -> Pair(random.nextFloat() * GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT + margin) // Bottom
            2 -> Pair(-margin, random.nextFloat() * GameConfig.WORLD_HEIGHT)              // Left
            else -> Pair(GameConfig.WORLD_WIDTH + margin, random.nextFloat() * GameConfig.WORLD_HEIGHT) // Right
        }
    }
}
