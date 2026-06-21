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
    private lateinit var projSys: ProjectileSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        weaponSys = WeaponSystem(state)
        projSys = ProjectileSystem(state)
    }

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
            repeat(2) { weaponSys.upgradeWeapon(WeaponType.entries[0]) }
            weaponSys.addWeapon(WeaponType.LASER_BEAM)
            assertEquals(6, weaponSys.weapons.size)
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
            weaponSys.addPassive(PassiveType.POWER_CORE)
            repeat(4) { weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE) }
            assertEquals(5, weaponSys.weapons[0].level)
            assertFalse(weaponSys.weapons[0].isEvolved)
            assertTrue(weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE))
            assertTrue(weaponSys.weapons[0].isEvolved)
            assertEquals(6, weaponSys.weapons[0].level)
        }

        @Test
        @DisplayName("Weapon does NOT evolve without catalyst")
        fun noEvolutionWithoutCatalyst() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            repeat(4) { weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE) }
            assertFalse(weaponSys.upgradeWeapon(WeaponType.ASSAULT_RIFLE))
        }
    }

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
            assertFalse(weaponSys.addPassive(PassiveType.POWER_CORE))
        }
    }

    @Nested
    @DisplayName("Weapon firing")
    inner class FiringTests {
        @Test
        @DisplayName("Assault Rifle fires at nearest enemy")
        fun assaultRifleFires() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(0.6f)
            // Run projectile system to move bullet into enemy
            repeat(5) { projSys.update(0.016f) }
            assertTrue(state.healths[enemyId].currentHp < hpBefore,
                "Enemy HP should decrease: before=$hpBefore after=${state.healths[enemyId].currentHp}")
        }

        @Test
        @DisplayName("Spread Gun creates multiple projectiles")
        fun spreadGunFires() {
            weaponSys.addWeapon(WeaponType.SPREAD_GUN)
            val playerPos = state.positions[state.playerIndex]
            state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val projBefore = countActiveProjectiles()
            weaponSys.update(1.0f)
            val projAfter = countActiveProjectiles()
            assertTrue(projAfter > projBefore)
        }

        @Test
        @DisplayName("Katana melee hits enemy in front")
        fun katanaHitsEnemy() {
            weaponSys.addWeapon(WeaponType.KATANA)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 60f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(1.0f)
            assertTrue(state.healths[enemyId].currentHp < hpBefore)
        }

        @Test
        @DisplayName("Force Field damages nearby enemies continuously")
        fun forceFieldDamages() {
            weaponSys.addWeapon(WeaponType.FORCE_FIELD)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 40f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(1.0f)
            assertTrue(state.healths[enemyId].currentHp < hpBefore)
        }

        @Test
        @DisplayName("Frost Nova damages and slows enemies")
        fun frostNovaDamages() {
            weaponSys.addWeapon(WeaponType.FROST_NOVA)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 80f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(1.0f)
            assertTrue(state.healths[enemyId].currentHp < hpBefore)
            // Check slow status applied
            if (enemyId < state.statusEffects.size) {
                val hasSlow = state.statusEffects[enemyId].effects.any {
                    it.type == com.survivortd.game.config.StatusEffectType.SLOW
                }
                assertTrue(hasSlow, "Enemy should have slow effect")
            }
        }

        @Test
        @DisplayName("Healing Pulse heals player")
        fun healingPulseHeals() {
            weaponSys.addWeapon(WeaponType.HEALING_PULSE)
            val playerHealth = state.healths[state.playerIndex]
            playerHealth.currentHp = 20f
            weaponSys.update(5.0f)
            assertTrue(playerHealth.currentHp > 20f, "Player should be healed")
        }

        @Test
        @DisplayName("Laser Beam instantly damages nearest enemy")
        fun laserBeamDamages() {
            weaponSys.addWeapon(WeaponType.LASER_BEAM)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(2.0f)
            assertTrue(state.healths[enemyId].currentHp < hpBefore)
        }

        @Test
        @DisplayName("Rocket Launcher fires AoE projectile")
        fun rocketFires() {
            weaponSys.addWeapon(WeaponType.ROCKET_LAUNCHER)
            val playerPos = state.positions[state.playerIndex]
            state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val projBefore = countActiveProjectiles()
            weaponSys.update(2.0f)
            val projAfter = countActiveProjectiles()
            assertTrue(projAfter > projBefore)
        }

        @Test
        @DisplayName("Boomerang fires piercing projectile")
        fun boomerangFires() {
            weaponSys.addWeapon(WeaponType.BOOMERANG)
            val playerPos = state.positions[state.playerIndex]
            state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val projBefore = countActiveProjectiles()
            weaponSys.update(2.0f)
            val projAfter = countActiveProjectiles()
            assertTrue(projAfter > projBefore)
        }

        @Test
        @DisplayName("Landmine drops at player position")
        fun landmineDrops() {
            weaponSys.addWeapon(WeaponType.LANDMINE)
            val projBefore = countActiveProjectiles()
            weaponSys.update(2.0f)
            val projAfter = countActiveProjectiles()
            assertTrue(projAfter > projBefore)
        }

        @Test
        @DisplayName("Drone fires at nearest enemy")
        fun droneFires() {
            weaponSys.addWeapon(WeaponType.DRONE)
            val playerPos = state.positions[state.playerIndex]
            state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val projBefore = countActiveProjectiles()
            weaponSys.update(2.0f)
            val projAfter = countActiveProjectiles()
            assertTrue(projAfter > projBefore)
        }
    }

    @Nested
    @DisplayName("Passive multipliers")
    inner class PassiveMultiplierTests {
        @Test
        @DisplayName("Power Core increases damage")
        fun powerCoreDamageBoost() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            weaponSys.addPassive(PassiveType.POWER_CORE)
            val playerPos = state.positions[state.playerIndex]
            val enemyId = state.spawnEnemy(
                x = playerPos.x + 100f,
                y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
            val hpBefore = state.healths[enemyId].currentHp
            weaponSys.update(0.6f)
            repeat(5) { projSys.update(0.016f) }
            val damage = hpBefore - state.healths[enemyId].currentHp
            // Base damage is 8 at level 1, with 15% Power Core boost = ~9.2
            assertTrue(damage > 8f,
                "Damage ($damage) should exceed base (8.0) with Power Core passive")
        }

        @Test
        @DisplayName("Weapon does not fire without enemies")
        fun noFireWithoutEnemies() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            val projBefore = countActiveProjectiles()
            weaponSys.update(1.0f)
            val projAfter = countActiveProjectiles()
            assertEquals(projBefore, projAfter, "Should not fire with no enemies")
        }
    }

    private fun countActiveProjectiles(): Int {
        var count = 0
        for (i in state.projectiles.indices) {
            if (i < state.tags.size && state.tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.PROJECTILE) {
                if (!state.healths[i].isDead) count++
            }
        }
        return count
    }
}
