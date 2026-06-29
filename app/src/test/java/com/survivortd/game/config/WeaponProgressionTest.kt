package com.survivortd.game.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [#31] Validates that all 12 weapons have complete 6-level progression tables.
 *
 * Before this fix, 9 of 12 weapons fell through to a stub `else` branch with
 * only a single Level-1 entry — they could never be upgraded or evolved.
 */
class WeaponProgressionTest {

    @Test
    @DisplayName("All 12 weapons have full 6-level progression")
    fun allWeaponsHaveFullProgression() {
        for (weaponType in WeaponType.entries) {
            val stats = GameBalance.getWeaponStats(weaponType)
            assertEquals(
                6, stats.levels.size,
                "${weaponType.displayName} should have 6 levels (1-5 + evolved), got ${stats.levels.size}"
            )
        }
    }

    @Test
    @DisplayName("All weapon levels are numbered 1-6 sequentially")
    fun weaponLevelsAreSequential() {
        for (weaponType in WeaponType.entries) {
            val stats = GameBalance.getWeaponStats(weaponType)
            for (i in stats.levels.indices) {
                assertEquals(
                    i + 1, stats.levels[i].level,
                    "${weaponType.displayName} level ${i + 1} should be numbered ${i + 1}"
                )
            }
        }
    }

    @Test
    @DisplayName("All weapon level 6 has evolution special marker")
    fun evolvedLevelHasSpecial() {
        for (weaponType in WeaponType.entries) {
            val stats = GameBalance.getWeaponStats(weaponType)
            val evolvedLevel = stats.levels.last()
            assertNotNull(
                evolvedLevel.special,
                "${weaponType.displayName} evolved level (6) must have a special marker"
            )
            assertTrue(
                evolvedLevel.special!!.startsWith("evolved:"),
                "${weaponType.displayName} evolved special must start with 'evolved:', got: ${evolvedLevel.special}"
            )
            assertEquals(
                weaponType.evolved,
                evolvedLevel.special!!.removePrefix("evolved:"),
                "${weaponType.displayName} evolved name should match enum value"
            )
        }
    }

    @Test
    @DisplayName("Weapon damage increases with each level")
    fun weaponDamageIncreasesWithLevel() {
        for (weaponType in WeaponType.entries) {
            // HEALING_PULSE damage field is actually heal amount — skip DPS check
            if (weaponType == WeaponType.HEALING_PULSE) continue
            val stats = GameBalance.getWeaponStats(weaponType)
            for (i in 1 until stats.levels.size) {
                assertTrue(
                    stats.levels[i].damage >= stats.levels[i - 1].damage,
                    "${weaponType.displayName} level ${i + 1} damage (${stats.levels[i].damage}) " +
                        "should be >= level $i (${stats.levels[i - 1].damage})"
                )
            }
        }
    }

    @Test
    @DisplayName("All 6 towers have full 3-level progression")
    fun allTowersHaveFullProgression() {
        for (towerType in TowerType.entries) {
            val stats = GameBalance.getTowerStats(towerType)
            assertEquals(
                3, stats.levels.size,
                "${towerType.displayName} should have 3 levels, got ${stats.levels.size}"
            )
        }
    }

    @Test
    @DisplayName("Tower costs match GDD §7.2")
    fun towerCostsMatchGDD() {
        // From GDD §7.2: Gun Turret=50, Cannon=100, Frost Tower=75, Tesla Coil=120, Poison Tower=80, Rocket Pod=150
        assertEquals(50, GameBalance.getTowerStats(TowerType.GUN_TURRET).cost)
        assertEquals(100, GameBalance.getTowerStats(TowerType.CANNON).cost)
        assertEquals(75, GameBalance.getTowerStats(TowerType.FROST_TOWER).cost)
        assertEquals(120, GameBalance.getTowerStats(TowerType.TESLA_COIL).cost)
        assertEquals(80, GameBalance.getTowerStats(TowerType.POISON_TOWER).cost)
        assertEquals(150, GameBalance.getTowerStats(TowerType.ROCKET_POD).cost)
    }
}
