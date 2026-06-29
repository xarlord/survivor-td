package com.survivortd.game.config

import com.survivortd.game.components.EnemyComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [#34] Validates that all 10 enemy types (except BOSS, which spawns separately)
 * appear in at least one spawn pool across all minute thresholds.
 */
class EnemySpawnPoolTest {

    @Test
    @DisplayName("FLYER appears in at least one spawn pool")
    fun flyerAppearsInSpawnPool() {
        val allPools = ChapterConfig.defaultEnemyPool()
        val flyerFound = allPools.values.any { pool ->
            pool.containsKey(EnemyComponent.EnemyData.FLYER)
        }
        assertTrue(flyerFound, "FLYER must appear in at least one spawn pool")
    }

    @Test
    @DisplayName("ELITE appears in at least one spawn pool")
    fun eliteAppearsInSpawnPool() {
        val allPools = ChapterConfig.defaultEnemyPool()
        val eliteFound = allPools.values.any { pool ->
            pool.containsKey(EnemyComponent.EnemyData.ELITE)
        }
        assertTrue(eliteFound, "ELITE must appear in at least one spawn pool")
    }

    @Test
    @DisplayName("All 9 non-boss enemy types appear in spawn pools")
    fun allNonBossEnemiesAppearInPools() {
        val allPools = ChapterConfig.defaultEnemyPool()
        val allTypes = mutableSetOf<EnemyComponent.EnemyData>()

        for (pool in allPools.values) {
            allTypes.addAll(pool.keys)
        }

        for (type in EnemyComponent.EnemyData.entries) {
            if (type == EnemyComponent.EnemyData.BOSS) continue  // Boss spawns separately
            assertTrue(
                allTypes.contains(type),
                "${type.name} must appear in at least one spawn pool (found: ${allTypes.map { it.name }})"
            )
        }
    }

    @Test
    @DisplayName("Spawn pool weights are positive and sum to 100")
    fun spawnPoolWeightsValid() {
        val allPools = ChapterConfig.defaultEnemyPool()
        for ((minute, pool) in allPools) {
            val total = pool.values.sum()
            assertTrue(total == 100,
                "Pool at minute $minute should sum to 100 (got $total)")
            for ((type, weight) in pool) {
                assertTrue(weight > 0,
                    "Weight for $type at minute $minute must be positive (got $weight)")
            }
        }
    }

    @Test
    @DisplayName("FLYER does not appear too early (min 5+)")
    fun flyerNotInEarlyGame() {
        val allPools = ChapterConfig.defaultEnemyPool()
        for ((minute, pool) in allPools) {
            if (minute < 5) {
                assertFalse(
                    pool.containsKey(EnemyComponent.EnemyData.FLYER),
                    "FLYER should not appear before minute 5 (found at minute $minute)"
                )
            }
        }
    }

    @Test
    @DisplayName("ELITE does not appear too early (min 8+)")
    fun eliteNotInEarlyGame() {
        val allPools = ChapterConfig.defaultEnemyPool()
        for ((minute, pool) in allPools) {
            if (minute < 8) {
                assertFalse(
                    pool.containsKey(EnemyComponent.EnemyData.ELITE),
                    "ELITE should not appear before minute 8 (found at minute $minute)"
                )
            }
        }
    }
}
