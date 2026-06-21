package com.survivortd.game.config

import com.survivortd.game.components.EnemyComponent
import kotlinx.serialization.Serializable

/**
 * Chapter configuration — defines the map, enemies, boss, and economy for a stage.
 *
 * 5 chapters planned, MVP focuses on Chapter 1 (Wasteland).
 */
@Serializable
data class ChapterConfig(
    val chapterId: String,
    val name: String,
    val durationSeconds: Int,        // Match length (900 = 15 min)
    val worldWidth: Float = 1280f,
    val worldHeight: Float = 720f,
    val bossTimeMinutes: List<Int> = listOf(5, 10, 15),
    val enemyPool: Map<Int, Map<EnemyComponent.EnemyData, Int>> = defaultEnemyPool(),
    val bossType: EnemyComponent.EnemyData = EnemyComponent.EnemyData.BOSS,
    val bossHp: Float = 4000f,
    val backgroundTint: Int = 0xFF1A1A2E.toInt()
) {
    companion object {
        /**
         * Default weighted enemy pool by minute threshold.
         * Key = minute mark, Value = enemy type → weight.
         * The spawner picks the highest applicable minute threshold.
         */
        fun defaultEnemyPool(): Map<Int, Map<EnemyComponent.EnemyData, Int>> = mapOf(
            0 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 70,
                EnemyComponent.EnemyData.RUNNER to 30
            ),
            3 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 60,
                EnemyComponent.EnemyData.RUNNER to 25,
                EnemyComponent.EnemyData.BRUTE to 15
            ),
            5 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 40,
                EnemyComponent.EnemyData.RUNNER to 30,
                EnemyComponent.EnemyData.BRUTE to 15,
                EnemyComponent.EnemyData.SPITTER to 15
            ),
            8 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 35,
                EnemyComponent.EnemyData.RUNNER to 25,
                EnemyComponent.EnemyData.BRUTE to 15,
                EnemyComponent.EnemyData.SPITTER to 10,
                EnemyComponent.EnemyData.BOMBER to 10,
                EnemyComponent.EnemyData.HEALER to 5
            ),
            10 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 30,
                EnemyComponent.EnemyData.RUNNER to 25,
                EnemyComponent.EnemyData.BRUTE to 20,
                EnemyComponent.EnemyData.SPITTER to 10,
                EnemyComponent.EnemyData.BOMBER to 10,
                EnemyComponent.EnemyData.HEALER to 5
            ),
            13 to mapOf(
                EnemyComponent.EnemyData.ZOMBIE to 20,
                EnemyComponent.EnemyData.RUNNER to 20,
                EnemyComponent.EnemyData.BRUTE to 20,
                EnemyComponent.EnemyData.SPITTER to 15,
                EnemyComponent.EnemyData.BOMBER to 10,
                EnemyComponent.EnemyData.HEALER to 10,
                EnemyComponent.EnemyData.SHIELDER to 5
            )
        )

        /** Chapter definitions */
        val WASTELAND = ChapterConfig(
            chapterId = "ch1_wasteland",
            name = "Wasteland",
            durationSeconds = 900,
            bossType = EnemyComponent.EnemyData.BOSS,
            bossHp = 4000f,
            backgroundTint = 0xFF1A1A2E.toInt()
        )

        val TOXIC_SWAMP = ChapterConfig(
            chapterId = "ch2_toxic_swamp",
            name = "Toxic Swamp",
            durationSeconds = 900,
            bossHp = 10000f,
            backgroundTint = 0xFF0D2818.toInt()
        )

        val ABANDONED_CITY = ChapterConfig(
            chapterId = "ch3_abandoned_city",
            name = "Abandoned City",
            durationSeconds = 900,
            bossHp = 10000f,
            backgroundTint = 0xFF1A1A1A.toInt()
        )

        val UNDERGROUND_LAB = ChapterConfig(
            chapterId = "ch4_underground_lab",
            name = "Underground Lab",
            durationSeconds = 900,
            bossHp = 25000f,
            backgroundTint = 0xFF0A0A1A.toInt()
        )

        val FINAL_BUNKER = ChapterConfig(
            chapterId = "ch5_final_bunker",
            name = "Final Bunker",
            durationSeconds = 1200,  // 20 minutes
            bossHp = 25000f,
            backgroundTint = 0xFF1A0A0A.toInt()
        )

        val ALL_CHAPTERS = listOf(WASTELAND, TOXIC_SWAMP, ABANDONED_CITY, UNDERGROUND_LAB, FINAL_BUNKER)
    }

    /**
     * Get the active enemy pool for the current match time.
     * Returns the highest applicable minute threshold pool.
     */
    fun getActivePool(elapsedSeconds: Float): Map<EnemyComponent.EnemyData, Int> {
        val minutes = (elapsedSeconds / 60f).toInt()
        var bestKey = 0
        for (key in enemyPool.keys.sorted()) {
            if (key <= minutes) bestKey = key
        }
        return enemyPool[bestKey] ?: enemyPool[0] ?: mapOf(EnemyComponent.EnemyData.ZOMBIE to 100)
    }

    /**
     * Check if a boss should spawn at the current time.
     * Returns boss index (0, 1, 2) or -1 if no boss.
     */
    fun shouldSpawnBoss(elapsedSeconds: Float): Int {
        val minute = (elapsedSeconds / 60f).toInt()
        return bossTimeMinutes.indexOfFirst { it == minute }
            .takeIf { it >= 0 }
            ?: -1
    }
}
