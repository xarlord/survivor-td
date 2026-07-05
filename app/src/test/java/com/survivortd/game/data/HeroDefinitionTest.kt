package com.survivortd.game.data

import com.survivortd.game.config.WeaponType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for HeroDefinition — hero enums, unlock costs, starting weapons.
 * GDD §3.3
 */
class HeroDefinitionTest {

    @Test
    @DisplayName("All 6 heroes are defined")
    fun allHeroesDefined() {
        assertEquals(6, HeroId.entries.size, "Should have exactly 6 heroes")
    }

    @Test
    @DisplayName("Each hero has a unique display name")
    fun uniqueDisplayNames() {
        val names = HeroId.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size, "Display names should be unique")
    }

    @Test
    @DisplayName("Each hero has a non-blank description")
    fun nonBlankDescriptions() {
        for (hero in HeroId.entries) {
            assertTrue(hero.description.isNotBlank(), "${hero.name} should have a description")
        }
    }

    @Test
    @DisplayName("Commander is the default hero")
    fun commanderIsDefault() {
        assertEquals(HeroId.COMMANDER, HeroId.DEFAULT)
    }

    @Test
    @DisplayName("Starting weapons match GDD §3.3")
    fun startingWeapons() {
        assertEquals(WeaponType.ASSAULT_RIFLE, HeroId.COMMANDER.startingWeapon)
        assertEquals(WeaponType.KATANA, HeroId.BERSERKER.startingWeapon)
        assertEquals(WeaponType.DRONE, HeroId.ENGINEER.startingWeapon)
        assertEquals(WeaponType.HEALING_PULSE, HeroId.MEDIC.startingWeapon)
        assertEquals(WeaponType.BOOMERANG, HeroId.SCOUT.startingWeapon)
        assertEquals(WeaponType.FORCE_FIELD, HeroId.SHIELDER.startingWeapon)
    }

    @Test
    @DisplayName("All heroes have unlock definitions")
    fun allHeroesHaveUnlocks() {
        assertEquals(6, HeroUnlock.ALL.size, "Should have unlock info for all 6 heroes")
        for (hero in HeroId.entries) {
            assertNotNull(HeroUnlock.forHero(hero), "Unlock info for ${hero.name}")
        }
    }

    @Test
    @DisplayName("Unlock costs match GDD §3.3")
    fun unlockCosts() {
        assertEquals(0, HeroUnlock.forHero(HeroId.COMMANDER).unlockCost, "Commander is free")
        assertEquals(5_000, HeroUnlock.forHero(HeroId.BERSERKER).unlockCost)
        assertEquals(10_000, HeroUnlock.forHero(HeroId.ENGINEER).unlockCost)
        assertEquals(15_000, HeroUnlock.forHero(HeroId.MEDIC).unlockCost)
        assertEquals(20_000, HeroUnlock.forHero(HeroId.SCOUT).unlockCost)
        assertEquals(0, HeroUnlock.forHero(HeroId.SHIELDER).unlockCost, "Shielder costs 0 gold (condition-based)")
    }

    @Test
    @DisplayName("Shielder requires chapter completion condition")
    fun shielderCondition() {
        val unlock = HeroUnlock.forHero(HeroId.SHIELDER)
        assertEquals("Complete Ch.1", unlock.unlockCondition)
        assertFalse(unlock.isFree, "Shielder has a condition so is not free")
    }

    @Test
    @DisplayName("Commander is free (no cost, no condition)")
    fun commanderIsFree() {
        val unlock = HeroUnlock.forHero(HeroId.COMMANDER)
        assertTrue(unlock.isFree, "Commander should be free")
    }
}
