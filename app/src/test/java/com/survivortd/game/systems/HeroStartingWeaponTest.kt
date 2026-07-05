package com.survivortd.game.systems

import com.survivortd.game.data.HeroId
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for starting weapon assignment per hero (GDD §3.3).
 */
class HeroStartingWeaponTest {

    @Test
    @DisplayName("WeaponSystem adds correct starting weapon for each hero")
    fun startingWeaponAssignment() {
        for (hero in HeroId.entries) {
            val state = GameState()
            state.spawnPlayer()
            val weaponSystem = WeaponSystem(state)

            weaponSystem.addWeapon(hero.startingWeapon)

            assertEquals(1, weaponSystem.weapons.size, "${hero.name} should have 1 weapon")
            assertEquals(hero.startingWeapon, weaponSystem.weapons[0].type,
                "${hero.name} should start with ${hero.startingWeapon.displayName}")
        }
    }

    @Test
    @DisplayName("WeaponSystem does not duplicate starting weapon")
    fun noDuplicateWeapon() {
        val state = GameState()
        state.spawnPlayer()
        val weaponSystem = WeaponSystem(state)

        val result1 = weaponSystem.addWeapon(HeroId.COMMANDER.startingWeapon)
        val result2 = weaponSystem.addWeapon(HeroId.COMMANDER.startingWeapon)

        assertTrue(result1, "First add should succeed")
        assertFalse(result2, "Second add of same weapon should fail")
        assertEquals(1, weaponSystem.weapons.size)
    }

    @Test
    @DisplayName("GameState tracks hero ID after initialization")
    fun gameStateHeroIdTracking() {
        for (hero in HeroId.entries) {
            val state = GameState()
            state.spawnPlayer()
            state.heroId = hero.name
            assertEquals(hero.name, state.heroId)
        }
    }
}
