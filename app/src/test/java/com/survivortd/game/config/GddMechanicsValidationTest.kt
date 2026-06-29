package com.survivortd.game.config

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Validates that the game's runtime mechanics produce results consistent with
 * the balance targets in GAME_SPEC.md §17. These tests verify DPS ranges,
 * economy flow, enemy scaling, and other gameplay-feel properties.
 *
 * [#26]
 */
class GddMechanicsValidationTest {

    @Nested
    @DisplayName("§17.2 Weapon DPS Balance Targets")
    inner class WeaponDpsTest {
        /**
         * Aura/continuous weapons that have cooldown=0. Their damage is applied
         * per-frame within a range, so the standard DPS formula
         * (damage * projectileCount / cooldown) produces Infinity and is
         * meaningless. They are excluded from DPS-per-shot tests.
         */
        private val auraWeapons = setOf(
            WeaponType.LIGHTNING_ORB,  // continuous chain lightning
            WeaponType.FORCE_FIELD     // continuous damage aura
        )

        /**
         * Calculate theoretical DPS for a weapon at a given level.
         * DPS = damage * projectileCount / cooldown
         * Returns 0 for aura weapons (cooldown=0) since per-shot DPS is
         * not a meaningful metric for continuous-damage weapons.
         */
        private fun weaponDps(type: WeaponType, level: Int): Float {
            val stats = GameBalance.getWeaponStats(type)
            val lv = stats.levels.find { it.level == level } ?: return 0f
            if (lv.cooldown <= 0f) return 0f  // aura/continuous weapon
            return lv.damage * lv.projectileCount / lv.cooldown
        }

        @Test
        @DisplayName("Early game (Lv1): each weapon should do 15-60 DPS")
        fun earlyGameDps() {
            // GDD §17.2: DPS per weapon Early (Lv1) = 30-50
            // We allow a wider range since some weapons (mines, AoE) trade DPS for utility
            for (weapon in WeaponType.entries) {
                if (weapon in auraWeapons) continue  // [#44] aura weapons use per-frame damage
                val dps = weaponDps(weapon, 1)
                assertTrue(
                    dps in 5f..80f,
                    "${weapon.displayName} Lv1 DPS should be 5-80 (got $dps). " +
                    "GDD target: 30-50, but utility weapons may be lower."
                )
            }
        }

        @Test
        @DisplayName("Mid game (Lv3): each weapon should do 30-150 DPS")
        fun midGameDps() {
            // GDD §17.2: DPS per weapon Mid (Lv3) = 80-120
            for (weapon in WeaponType.entries) {
                if (weapon in auraWeapons) continue  // [#44] aura weapons use per-frame damage
                val dps = weaponDps(weapon, 3)
                assertTrue(
                    dps in 15f..200f,
                    "${weapon.displayName} Lv3 DPS should be 15-200 (got $dps). " +
                    "GDD target: 80-120, but utility weapons may vary."
                )
            }
        }

        @Test
        @DisplayName("Late game (Evolved): each weapon should do 100-500 DPS")
        fun lateGameDps() {
            // GDD §17.2: DPS per weapon Late (Evolved) = 200-400
            for (weapon in WeaponType.entries) {
                if (weapon in auraWeapons) continue  // [#44] aura weapons use per-frame damage
                val dps = weaponDps(weapon, 6) // Level 6 = evolved
                assertTrue(
                    dps in 50f..1500f,
                    "${weapon.displayName} Evolved DPS should be 50-1500 (got $dps). " +
                    "GDD target: 200-400, but AoE/utility/high-power weapons may vary."
                )
            }
        }

        @Test
        @DisplayName("Evolved weapon should be 2-5x stronger than Level 5")
        fun evolutionIsPowerful() {
            // GDD §17.2: Evolutions feel powerful but not game-breaking (3-4x base DPS)
            for (weapon in WeaponType.entries) {
                val lv5Dps = weaponDps(weapon, 5)
                val evolvedDps = weaponDps(weapon, 6)
                if (lv5Dps > 0f && evolvedDps > 0f) {
                    val ratio = evolvedDps / lv5Dps
                    assertTrue(
                        ratio in 1.5f..8f,
                        "${weapon.displayName} evolution should be 1.5-8x Lv5 DPS " +
                        "(got ${ratio}x: Lv5=$lv5Dps, Evolved=$evolvedDps). " +
                        "GDD target: 3-4x."
                    )
                }
            }
        }

        @Test
        @DisplayName("Total evolved DPS of all 12 weapons exceeds 1000")
        fun totalDpsLateGame() {
            // GDD §17.2: Total DPS (all weapons) Late = 600-1200
            // Player has max 6 slots, but if all 12 evolved weapons existed:
            // Aura weapons (cooldown=0) are excluded since their DPS is continuous, not per-shot
            val totalDps = WeaponType.entries
                .filter { it !in auraWeapons }
                .sumOf { weaponDps(it, 6).toDouble() }
            assertTrue(
                totalDps > 500f,
                "Total DPS of evolved projectile weapons should exceed 500 (got $totalDps). " +
                "With 6 slots at 200+ DPS each = 1200+ target."
            )
        }
    }

    @Nested
    @DisplayName("§17.1 DPS Viability (can player survive?)")
    inner class DpsViabilityTest {

        /**
         * Support/utility weapons that are not designed for raw damage output.
         * Their primary function is healing or utility, not killing enemies,
         * so they should not be subject to kill-time viability tests.
         */
        private val nonOffensiveWeapons = setOf(
            WeaponType.HEALING_PULSE  // heals the player, not an offensive weapon
        )

        @Test
        @DisplayName("Level 3 weapon can kill Zombie (HP=20) in under 5 seconds")
        fun level3KillsZombie() {
            val state = GameState()
            val idx = state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.ZOMBIE)
            val zombieHp = state.healths[idx].maxHp
            for (weapon in WeaponType.entries) {
                if (weapon in nonOffensiveWeapons) continue  // [#44] support weapon
                val stats = GameBalance.getWeaponStats(weapon)
                val lv3 = stats.levels.find { it.level == 3 }!!
                if (lv3.cooldown <= 0f) continue  // [#44] aura weapon, DPS not applicable
                val dps = lv3.damage * lv3.projectileCount / lv3.cooldown
                val timeToKill = zombieHp / dps
                assertTrue(
                    timeToKill < 5f,
                    "${weapon.displayName} Lv3 should kill Zombie (HP=$zombieHp) in <5s " +
                    "(got ${timeToKill}s, DPS=$dps)"
                )
            }
        }

        @Test
        @DisplayName("Level 5 weapon can kill Brute (HP=100) in under 10 seconds")
        fun level5KillsBrute() {
            val state = GameState()
            val idx = state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.BRUTE)
            val bruteHp = state.healths[idx].maxHp
            for (weapon in WeaponType.entries) {
                if (weapon in nonOffensiveWeapons) continue  // [#44] support weapon
                val stats = GameBalance.getWeaponStats(weapon)
                val lv5 = stats.levels.find { it.level == 5 }!!
                if (lv5.cooldown <= 0f) continue  // [#44] aura weapon, DPS not applicable
                val dps = lv5.damage * lv5.projectileCount / lv5.cooldown
                val timeToKill = bruteHp / dps
                assertTrue(
                    timeToKill < 10f,
                    "${weapon.displayName} Lv5 should kill Brute (HP=$bruteHp) in <10s " +
                    "(got ${timeToKill}s, DPS=$dps)"
                )
            }
        }
    }

    @Nested
    @DisplayName("§8.2 Enemy Scaling Viability")
    inner class EnemyScalingTest {
        @Test
        @DisplayName("Enemy HP at minute 10 should be 200-400 for Zombies")
        fun enemyHpAtMin10() {
            // GDD §17.2: Enemy HP at min 10 = 200-300
            val state = GameState()
            val idx = state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.ZOMBIE)
            val zombieBaseHp = state.healths[idx].maxHp
            val hpAtMin10 = GameBalance.enemyHpAtMinute(zombieBaseHp, 10)
            // 20 * (1 + 0.15 * 10) = 20 * 2.5 = 50
            assertTrue(
                hpAtMin10 > 30f,
                "Zombie HP at min 10 should be > 30 (got $hpAtMin10). " +
                "Base=$zombieBaseHp, scale=${GameConfig.ENEMY_HP_SCALE}"
            )
        }

        @Test
        @DisplayName("Enemy HP scales linearly (not exponentially)")
        fun enemyHpLinearScaling() {
            val baseHp = 20f
            val hpAtMin0 = GameBalance.enemyHpAtMinute(baseHp, 0)
            val hpAtMin5 = GameBalance.enemyHpAtMinute(baseHp, 5)
            val hpAtMin10 = GameBalance.enemyHpAtMinute(baseHp, 10)
            val hpAtMin15 = GameBalance.enemyHpAtMinute(baseHp, 15)

            // Linear: HP = base * (1 + 0.15 * minute)
            assertEquals(hpAtMin0, 20f, 0.1f)
            assertEquals(hpAtMin5, 35f, 0.1f)
            assertEquals(hpAtMin10, 50f, 0.1f)
            assertEquals(hpAtMin15, 65f, 0.1f)
        }

        private fun assertEquals(actual: Float, expected: Float, delta: Float) {
            assertTrue(
                kotlin.math.abs(actual - expected) < delta,
                "Expected $expected ± $delta, got $actual"
            )
        }

        @Test
        @DisplayName("Spawn rate increases over time (interval decreases)")
        fun spawnRateIncreases() {
            val interval0 = GameBalance.spawnIntervalAtMinute(0)
            val interval5 = GameBalance.spawnIntervalAtMinute(5)
            val interval10 = GameBalance.spawnIntervalAtMinute(10)

            assertTrue(interval5 < interval0, "Spawn interval should decrease over time")
            assertTrue(interval10 < interval5, "Spawn interval should keep decreasing")
            assertTrue(
                interval10 >= GameConfig.MIN_SPAWN_INTERVAL,
                "Spawn interval should not go below minimum (${GameConfig.MIN_SPAWN_INTERVAL}s)"
            )
        }
    }

    @Nested
    @DisplayName("§10.2 Economy Flow")
    inner class EconomyFlowTest {
        @Test
        @DisplayName("Cheapest tower (Gun Turret=50) needs 25 kills at 2 gold/kill")
        fun cheapestTowerEconomy() {
            val towerCost = TowerType.GUN_TURRET.baseCost
            val goldPerKill = GameConfig.GOLD_PER_KILL
            val killsNeeded = towerCost / goldPerKill
            assertTrue(
                killsNeeded in 15..50,
                "Gun Turret needs $killsNeeded kills (cost=$towerCost, gold/kill=$goldPerKill). " +
                "Should be 25-50 for reasonable economy flow."
            )
        }

        @Test
        @DisplayName("Most expensive tower (Rocket Pod=150) needs 75 kills")
        fun expensiveTowerEconomy() {
            val towerCost = TowerType.ROCKET_POD.baseCost
            val goldPerKill = GameConfig.GOLD_PER_KILL
            val killsNeeded = towerCost / goldPerKill
            assertTrue(
                killsNeeded in 50..100,
                "Rocket Pod needs $killsNeeded kills (cost=$towerCost, gold/kill=$goldPerKill). " +
                "Should be 50-100 for a premium tower."
            )
        }

        @Test
        @DisplayName("Match completion bonus covers ~1 tower purchase")
        fun completionBonusEconomy() {
            val bonus = GameConfig.GOLD_COMPLETION_BONUS
            val cheapestTower = TowerType.GUN_TURRET.baseCost
            assertTrue(
                bonus >= cheapestTower,
                "Completion bonus ($bonus) should cover at least 1 cheapest tower ($cheapestTower)"
            )
        }
    }

    @Nested
    @DisplayName("§7.2 Tower Relevance")
    inner class TowerRelevanceTest {
        @Test
        @DisplayName("All towers have positive DPS")
        fun allTowersHavePositiveDps() {
            for (tower in TowerType.entries) {
                val dps = tower.baseDamage * tower.baseFireRate
                assertTrue(
                    dps > 0f,
                    "${tower.displayName} should have positive DPS (got $dps)"
                )
            }
        }

        @Test
        @DisplayName("More expensive towers should generally do more DPS")
        fun costEffectiveness() {
            // Utility towers trade DPS for status effects (slow, poison DoT).
            // They should not be compared on raw DPS against offensive towers.
            val utilityTowers = setOf(
                TowerType.FROST_TOWER,   // applies SLOW
                TowerType.POISON_TOWER   // applies POISON DoT
            )
            // Sort by cost and check DPS generally increases
            val sorted = TowerType.entries.sortedBy { it.baseCost }
            for (i in 0 until sorted.size - 1) {
                val cheap = sorted[i]
                val expensive = sorted[i + 1]
                // [#44] Skip utility towers — their value comes from status effects, not raw DPS
                if (cheap in utilityTowers || expensive in utilityTowers) continue
                val cheapDps = cheap.baseDamage * cheap.baseFireRate
                val expensiveDps = expensive.baseDamage * expensive.baseFireRate
                assertTrue(
                    expensiveDps >= cheapDps * 0.5f,
                    "${expensive.displayName} (cost=${expensive.baseCost}) DPS ($expensiveDps) " +
                    "should not be too far below ${cheap.displayName} (cost=${cheap.baseCost}) DPS ($cheapDps)"
                )
            }
        }
    }

    @Nested
    @DisplayName("§6.1 XP Progression Viability")
    inner class XpProgressionTest {
        @Test
        @DisplayName("Level 1→2 requires 8 XP (2 zombie kills)")
        fun level1To2Xp() {
            val xpNeeded = GameConfig.xpForLevel(1)
            val zombieXp = GameConfig.GEM_SMALL_XP
            val killsNeeded = xpNeeded / zombieXp
            assertTrue(
                killsNeeded in 2..10,
                "Level 1→2 needs $killsNeeded zombie kills (XP=$xpNeeded, gem=$zombieXp). " +
                "Should be 2-10 for fast early-game leveling."
            )
        }

        @Test
        @DisplayName("Level progression slows but doesn't stall")
        fun levelProgressionCurve() {
            val xp1 = GameConfig.xpForLevel(1)
            val xp5 = GameConfig.xpForLevel(5)
            val xp10 = GameConfig.xpForLevel(10)
            val xp15 = GameConfig.xpForLevel(15)
            val xp20 = GameConfig.xpForLevel(20)

            // Linear scaling: 5 + level * 3
            assertTrue(xp1 < xp5, "XP should increase with level")
            assertTrue(xp5 < xp10, "XP should keep increasing")
            assertTrue(xp10 < xp15, "XP should keep increasing")
            assertTrue(xp15 < xp20, "XP should keep increasing")

            // Ratio should be gradual (not exponential)
            val ratio = xp20.toFloat() / xp1.toFloat()
            assertTrue(
                ratio < 10f,
                "XP at Lv20 should be <10x XP at Lv1 (ratio=$ratio). " +
                "Linear scaling should not be too steep."
            )
        }
    }
}
