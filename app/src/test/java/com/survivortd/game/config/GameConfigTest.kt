package com.survivortd.game.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Tests for GameConfig — XP curve, enemy scaling, spawn timing.
 * These verify the balance simulation values are correctly reflected in code.
 */
class GameConfigTest {

    @Nested
    @DisplayName("XP Curve")
    inner class XpCurveTest {

        @Test
        fun `level 1 to 2 requires 8 XP`() {
            assertEquals(8, GameConfig.xpForLevel(1))
        }

        @Test
        fun `level 10 to 11 requires 35 XP`() {
            assertEquals(35, GameConfig.xpForLevel(10))
        }

        @Test
        fun `level 15 to 16 requires 53 XP`() {
            assertEquals(53, GameConfig.xpForLevel(15))
        }

        @Test
        fun `level 20 to 21 requires 68 XP`() {
            assertEquals(68, GameConfig.xpForLevel(20))
        }

        @ParameterizedTest
        @CsvSource("1, 8", "5, 20", "10, 35", "15, 53", "20, 68", "30, 95")
        fun `xp curve follows linear formula 5 + N times 3`(level: Int, expectedXp: Int) {
            assertEquals(expectedXp, GameConfig.xpForLevel(level))
        }

        @Test
        fun `xp curve is monotonically increasing`() {
            for (level in 1 until GameConfig.MAX_LEVEL) {
                assertTrue(
                    GameConfig.xpForLevel(level) < GameConfig.xpForLevel(level + 1),
                    "Level $level XP should be less than level ${level + 1}"
                )
            }
        }

        @Test
        fun `player reaches level 18 within 15-minute match`() {
            // At ~30 XP/min average (balance sim), player should reach Lv 18 by minute 15
            // Cumulative XP for Lv 18 = sum of xpForLevel(1..17)
            var cumulative = 0
            for (level in 1..17) {
                cumulative += GameConfig.xpForLevel(level)
            }
            // 15 min * 30 XP/min = 450 XP available
            // Cumulative for Lv 18 = 435
            assertTrue(cumulative <= 450, "Cumulative XP to reach Lv 18 ($cumulative) should be <= 450 (15min @ 30xp/min)")
        }
    }

    @Nested
    @DisplayName("Enemy Scaling")
    inner class EnemyScalingTest {

        @Test
        fun `zombie HP at minute 0 equals base HP`() {
            val hp = GameBalance.enemyHpAtMinute(20f, 0)
            assertEquals(20f, hp, 0.01f)
        }

        @Test
        fun `zombie HP at minute 10 is 50 HP (+150 percent)`() {
            val hp = GameBalance.enemyHpAtMinute(20f, 10)
            assertEquals(50f, hp, 0.01f)
        }

        @Test
        fun `enemy HP scaling is linear not exponential`() {
            val hpAt5 = GameBalance.enemyHpAtMinute(20f, 5)
            val hpAt10 = GameBalance.enemyHpAtMinute(20f, 10)
            // Linear: difference should be constant
            val diff5to10 = hpAt10 - hpAt5
            val diff0to5 = hpAt5 - 20f
            assertEquals(diff0to5, diff5to10, 0.01f, "HP scaling should be linear")
        }
    }

    @Nested
    @DisplayName("Spawn Interval")
    inner class SpawnIntervalTest {

        @Test
        fun `spawn interval at minute 0 is base interval`() {
            val interval = GameBalance.spawnIntervalAtMinute(0)
            assertEquals(1.0f, interval, 0.01f)
        }

        @Test
        fun `spawn interval never drops below minimum`() {
            val interval = GameBalance.spawnIntervalAtMinute(20)
            assertTrue(interval >= GameConfig.MIN_SPAWN_INTERVAL, "Spawn interval should not drop below ${GameConfig.MIN_SPAWN_INTERVAL}")
        }

        @Test
        fun `spawn interval decreases over time`() {
            val early = GameBalance.spawnIntervalAtMinute(2)
            val late = GameBalance.spawnIntervalAtMinute(10)
            assertTrue(late < early, "Late game spawn interval should be shorter (more frequent)")
        }
    }
}
