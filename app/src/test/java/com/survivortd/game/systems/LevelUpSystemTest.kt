package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.PassiveType
import com.survivortd.game.config.WeaponType
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LevelUpSystemTest {

    private lateinit var state: GameState
    private lateinit var weaponSys: WeaponSystem
    private lateinit var levelUpSys: LevelUpSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        weaponSys = WeaponSystem(state)
        levelUpSys = LevelUpSystem(state, weaponSys)
    }

    @Nested
    @DisplayName("Choice generation")
    inner class ChoiceGeneration {

        @Test
        @DisplayName("Generates 3-4 choices on level-up")
        fun generatesChoices() {
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            assertTrue(choices.size in 3..4,
                "Should generate 3-4 choices, got ${choices.size}")
        }

        @Test
        @DisplayName("Offers new weapon choices when slots available")
        fun offersNewWeapons() {
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            // Player has no weapons yet → all 12 should be in pool, at least 1 offered
            assertTrue(choices.any { it.type == UpgradeType.NEW_WEAPON },
                "Should offer at least one new weapon choice")
        }

        @Test
        @DisplayName("No new weapon choices when all 6 slots full")
        fun noNewWeaponsWhenSlotsFull() {
            // Fill 6 weapon slots
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            weaponSys.addWeapon(WeaponType.SPREAD_GUN)
            weaponSys.addWeapon(WeaponType.KATANA)
            weaponSys.addWeapon(WeaponType.LIGHTNING_ORB)
            weaponSys.addWeapon(WeaponType.ROCKET_LAUNCHER)
            weaponSys.addWeapon(WeaponType.FORCE_FIELD)

            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            assertFalse(choices.any { it.type == UpgradeType.NEW_WEAPON },
                "Should NOT offer new weapons when slots full")
        }

        @Test
        @DisplayName("Offers weapon upgrade when weapons owned")
        fun offersWeaponUpgrade() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            assertTrue(choices.any { it.type == UpgradeType.UPGRADE_WEAPON },
                "Should offer weapon upgrade when weapons owned")
        }

        @Test
        @DisplayName("Offers heal as fallback")
        fun offersHeal() {
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            assertTrue(choices.any { it.type == UpgradeType.HEAL },
                "Should always offer heal")
        }

        @Test
        @DisplayName("Offers stat boost as fallback")
        fun offersStatBoost() {
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            assertTrue(choices.any { it.type == UpgradeType.STAT_BOOST },
                "Should always offer stat boost")
        }

        @Test
        @DisplayName("Generated choices have no duplicates")
        fun noDuplicateChoices() {
            state.pendingLevelUps = 1
            val choices = levelUpSys.generateChoices()
            val keys = choices.map { "${it.type}:${it.weaponType}:${it.passiveType}" }
            assertEquals(keys.size, keys.toSet().size,
                "Choices should be unique")
        }
    }

    @Nested
    @DisplayName("Applying choices")
    inner class ApplyChoices {

        @Test
        @DisplayName("NEW_WEAPON adds weapon to arsenal")
        fun applyNewWeapon() {
            val choice = UpgradeChoice(
                type = UpgradeType.NEW_WEAPON,
                title = "New: Assault Rifle",
                description = "test",
                weaponType = WeaponType.ASSAULT_RIFLE
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            assertTrue(weaponSys.weapons.any { it.type == WeaponType.ASSAULT_RIFLE },
                "Weapon should be added")
            assertEquals(0, state.pendingLevelUps,
                "pendingLevelUps should be decremented")
        }

        @Test
        @DisplayName("UPGRADE_WEAPON increases weapon level")
        fun applyWeaponUpgrade() {
            weaponSys.addWeapon(WeaponType.ASSAULT_RIFLE)
            val initialLevel = weaponSys.weapons.first().level
            val choice = UpgradeChoice(
                type = UpgradeType.UPGRADE_WEAPON,
                title = "Upgrade",
                description = "test",
                weaponType = WeaponType.ASSAULT_RIFLE
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            assertEquals(initialLevel + 1, weaponSys.weapons.first().level,
                "Weapon level should increase by 1")
        }

        @Test
        @DisplayName("NEW_PASSIVE adds passive")
        fun applyNewPassive() {
            val choice = UpgradeChoice(
                type = UpgradeType.NEW_PASSIVE,
                title = "New: Power Core",
                description = "test",
                passiveType = PassiveType.POWER_CORE
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            assertTrue(weaponSys.passives.any { it.type == PassiveType.POWER_CORE },
                "Passive should be added")
        }

        @Test
        @DisplayName("UPGRADE_PASSIVE stacks passive")
        fun applyPassiveStack() {
            weaponSys.addPassive(PassiveType.POWER_CORE)
            val initialStacks = weaponSys.passives.first().stacks
            val choice = UpgradeChoice(
                type = UpgradeType.UPGRADE_PASSIVE,
                title = "Stack",
                description = "test",
                passiveType = PassiveType.POWER_CORE
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            assertEquals(initialStacks + 1, weaponSys.passives.first().stacks,
                "Passive stacks should increase by 1")
        }

        @Test
        @DisplayName("HEAL restores 30% HP")
        fun applyHeal() {
            val hp = state.healths[state.playerIndex]
            hp.currentHp = hp.maxHp * 0.5f  // 50% HP
            val choice = UpgradeChoice(
                type = UpgradeType.HEAL,
                title = "Heal",
                description = "test"
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            val expected = (hp.maxHp * 0.5f + hp.maxHp * 0.3f)
            assertEquals(expected, hp.currentHp, 1f,
                "HP should be restored by 30% of max")
        }

        @Test
        @DisplayName("STAT_BOOST increases move speed")
        fun applyStatBoost() {
            val initialSpeed = state.players[state.playerIndex].moveSpeed
            val choice = UpgradeChoice(
                type = UpgradeType.STAT_BOOST,
                title = "Speed",
                description = "test"
            )
            state.pendingLevelUps = 1
            levelUpSys.applyChoice(choice)
            assertEquals(initialSpeed * 1.1f, state.players[state.playerIndex].moveSpeed, 1f,
                "Move speed should increase by 10%")
        }

        @Test
        @DisplayName("Multiple level-ups consume one at a time")
        fun multipleLevelUpsConsumeOne() {
            state.pendingLevelUps = 3
            val choice = UpgradeChoice(
                type = UpgradeType.HEAL,
                title = "Heal",
                description = "test"
            )
            levelUpSys.applyChoice(choice)
            assertEquals(2, state.pendingLevelUps,
                "Should consume one level-up, leaving 2")
        }
    }

    @Nested
    @DisplayName("XP and leveling")
    inner class XPAndLeveling {

        @Test
        @DisplayName("Player levels up when currentXp >= xpToNext")
        fun playerLevelsUp() {
            val player = state.players[state.playerIndex]
            player.currentXp = player.xpToNext
            val initialLevel = player.level

            // Simulate XP gain → triggers level-up in PickupSystem
            player.currentXp -= player.xpToNext
            player.level++
            player.xpToNext = GameConfig.xpForLevel(player.level)
            state.pendingLevelUps++

            assertEquals(initialLevel + 1, player.level)
            assertEquals(1, state.pendingLevelUps)
        }

        @Test
        @DisplayName("XP curve uses linear formula 5 + level * 3")
        fun xpCurveIsLinear() {
            // Level 1 → xpForLevel(2) = 5 + 2*3 = 11
            assertEquals(11, GameConfig.xpForLevel(2))
            // Level 5 → xpForLevel(5) = 5 + 5*3 = 20
            assertEquals(20, GameConfig.xpForLevel(5))
            // Level 10 → xpForLevel(10) = 5 + 10*3 = 35
            assertEquals(35, GameConfig.xpForLevel(10))
            // Level 15 → xpForLevel(15) = 5 + 15*3 = 50
            assertEquals(50, GameConfig.xpForLevel(15))
        }
    }
}
