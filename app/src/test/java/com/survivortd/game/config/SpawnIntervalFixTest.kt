package com.survivortd.game.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Regression test for issue #67 - spawn interval formula bug.
 * Confirms the corrected formula matches GDD §29 specification.
 */
class SpawnIntervalFixTest {

    @Test
    @DisplayName("Minute 0: spawn interval = 1.0s")
    fun minute0() {
        val interval = GameBalance.spawnIntervalAtMinute(0)
        assertEquals(1.0f, interval, 0.01f, "Minute 0 should have 1.0s interval")
    }

    @Test
    @DisplayName("Minute 5: spawn interval = 0.5s (2 enemies/s)")
    fun minute5() {
        val interval = GameBalance.spawnIntervalAtMinute(5)
        assertEquals(0.5f, interval, 0.01f, "Minute 5 should have 0.5s interval (2 enemies/s)")
    }

    @Test
    @DisplayName("Minute 10: spawn interval = 0.33s (3 enemies/s)")
    fun minute10() {
        val interval = GameBalance.spawnIntervalAtMinute(10)
        assertEquals(0.33f, interval, 0.01f, "Minute 10 should have ~0.33s interval (3 enemies/s)")
    }

    @Test
    @DisplayName("Minute 15: spawn interval = 0.3s (capped at minimum)")
    fun minute15() {
        val interval = GameBalance.spawnIntervalAtMinute(15)
        assertEquals(0.3f, interval, 0.01f, "Minute 15 should be capped at 0.3s minimum interval")
    }

    @Test
    @DisplayName("Spawn rate increases over time (interval decreases)")
    fun spawnRateIncreases() {
        val interval0 = GameBalance.spawnIntervalAtMinute(0)
        val interval5 = GameBalance.spawnIntervalAtMinute(5)
        val interval10 = GameBalance.spawnIntervalAtMinute(10)

        assert(interval5 < interval0) { "Spawn interval should decrease over time" }
        assert(interval10 < interval5) { "Spawn interval should keep decreasing" }
        assert(interval10 >= GameConfig.MIN_SPAWN_INTERVAL) {
            "Spawn interval should not go below minimum (${GameConfig.MIN_SPAWN_INTERVAL}s)"
        }
    }
}
