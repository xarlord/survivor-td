package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.PassiveType
import com.survivortd.game.config.WeaponType
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WeaponSystemTest {

    private lateinit var state: GameState
    private lateinit var weaponSys: WeaponSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        weaponSys = WeaponSystem(state)
    }

    // ================================================================
    // ADD/UPGRADE WEAPONS
    // ================================================================
    @Nested
    @DisplayName("Weapon management")
    inner class WeaponManagement {
        @Test
        @DisplayName("Add weapon succeeds")
        fun addWeapon() {
            assertTrue(weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE))
            assertEquals(1, weaponSys.weapons.size)
        }

        @Test
        @DisplayName("Cannot add duplicate weapon")
        fun noDuplicate() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            assertFalse(weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE))
        }

        @Test
        @DisplayName("Max 6 weapon slots, replaces weakest when full")
        fun maxSlotsReplaceWeakest() {
            for (i in 0 until 6) {
                weaponSys.addWeapon(WeaponType.entries[i])
            }
            assertEquals(6, weaponSys.weapons.size)
            // Upgrade first weapon to level 3, then add a 7th
            repeat(2) { weaponSys.upgradeWeapon(WeaponType.entries[0]) }
            weaponSys.addWeapon(WeaponType.LASER_BEAM)
            assertEquals(6, weaponSys.weapons.size)
            // First weapon should still be there (it's strongest)
            assertTrue(weaponSys.weapons.any { it.type == WeaponType.entries[0] })
        }

        @Test
        @DisplayName("Upgrade weapon increases level")
        fun upgradeWeapon() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            assertEquals(1, weaponSys.weapons[0].level)
            weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE)
            assertEquals(2, weaponSys.weapons[0].level)
        }

        @Test
        @DisplayName("Weapon evolves at level 5 with catalyst")
        fun weaponEvolution() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            weaponSys.addPassive(PassiveType.POWER_CORE)  // Catalyst for AR
            // Upgrade to level 5
            repeat(4) { weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE) }
            assertEquals(5, weaponSys.weapons[0].level)
            assertFalse(weaponSys.weapons[0].isEvolved)
            // Next upgrade should evolve it
            assertTrue(weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE))
            assertTrue(weaponSys.weapons[0].isEvolved)
            assertEquals(6, weaponSys.weapons[0].level)
        }

        @Test
        @DisplayName("Weapon does NOT evolve without catalyst")
        fun noEvolutionWithoutCatalyst() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            // No passive catalyst
            repeat(4) { weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE) }
            assertFalse(weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE),
                "Should fail to evolve without catalyst")
        }
    }

    // ================================================================
    // PASSIVE ITEMS
    // ================================================================
    @Nested
    @DisplayName("Passive items")
    inner class PassiveTests {
        @Test
        @DisplayName("Add passive succeeds")
        fun addPassive() {
            assertTrue(weaponSys.addPassive(PassiveType.POWER_CORE))
            assertEquals(1, weaponSys.passives.size)
        }

        @Test
        @DisplayName("Passive stacks up to 5")
        fun passiveStacks() {
            for (i in 0 until 5) {
                assertTrue(weaponSys.addPassive(PassiveType.POWER_CORE))
            }
            assertEquals(5, weaponSys.passives[0].stacks)
            assertFalse(weaponSys.addPassive(PassiveType.POWER_CORE),
                "Should not stack beyond 5")
        }
    }

    // ================================================================
    // FIRING TESTS
    // ================================================================
    @Nested
    @DisplayName("Weapon firing")
    inner class FiringTests {
        @Test
        @DisplayName("Assault Rifle fires at nearest enemy")
        fun assaultRifleFires() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            val enemyId = state.spawnEnemy(
                x = state.positions[state.playerIndex].x + 100f,
                y = state.positions[state.playerIndex].y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(0.5f)  // Trigger fire (cooldown = 0.5s)
            assertTrue(state.healths[enemyId].currentHp < hpBefore,
                "Enemy HP should decrease after AR fire")
        }

        @Test
        @DisplayName("Spread Gun creates multiple projectiles")
        fun spreadGunFires() {
            weaponSys.addWeapon(WeaponType.SPREAD_GUN)
            state.spawnEnemy(
                x = state.positions[state.playerIndex].x + 100f,
                y = state.positions[state.playerIndex].y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val projBefore = state.projectiles.count { p -> p.damage > 5f }
            weaponSys.update(1f)  // Trigger fire
            // Should have spawned multiple projectiles
            val projAfter = state.projectiles.count { p -> p.damage > 5f }
            assertTrue(projAfter > projBefore, "Spread Gun should spawn projectiles")
        }

        @T...[truncated]