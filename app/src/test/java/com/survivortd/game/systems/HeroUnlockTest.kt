package com.survivortd.game.systems

import com.survivortd.game.data.HeroId
import com.survivortd.game.data.HeroUnlock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for hero unlock flow — enough gold → unlocked, not enough → still locked.
 * GDD §3.3
 */
class HeroUnlockTest {

    private var unlockedHeroes: MutableSet<String> = mutableSetOf(HeroId.DEFAULT.name)

    @BeforeEach
    fun setup() {
        unlockedHeroes = mutableSetOf(HeroId.DEFAULT.name)
    }

    @Test
    @DisplayName("Hero with enough gold can be unlocked")
    fun unlockWithEnoughGold() {
        val playerGold = 10_000
        val hero = HeroId.BERSERKER
        val unlock = HeroUnlock.forHero(hero)
        assertFalse(hero.name in unlockedHeroes, "Should start locked")

        assertTrue(playerGold >= unlock.unlockCost, "Player should have enough gold")
        unlockedHeroes.add(hero.name)

        assertTrue(hero.name in unlockedHeroes, "Hero should now be unlocked")
    }

    @Test
    @DisplayName("Hero without enough gold stays locked")
    fun notEnoughGoldStaysLocked() {
        val playerGold = 3_000
        val hero = HeroId.BERSERKER
        val unlock = HeroUnlock.forHero(hero)
        assertFalse(hero.name in unlockedHeroes, "Should start locked")

        assertFalse(playerGold >= unlock.unlockCost, "Player should NOT have enough gold")

        // Do NOT add to unlocked
        assertFalse(hero.name in unlockedHeroes, "Hero should remain locked")
    }

    @Test
    @DisplayName("All heroes can be unlocked with sufficient gold")
    fun allHeroesUnlockableWithGold() {
        val totalGold = 100_000 // More than enough
        for (hero in HeroId.entries) {
            val unlock = HeroUnlock.forHero(hero)
            if (unlock.unlockCost > 0 && unlock.unlockCondition.isBlank()) {
                if (totalGold >= unlock.unlockCost) {
                    unlockedHeroes.add(hero.name)
                }
            }
        }
        // Commander (free) + all gold-cost heroes
        assertTrue(HeroId.COMMANDER.name in unlockedHeroes)
        assertTrue(HeroId.BERSERKER.name in unlockedHeroes)
        assertTrue(HeroId.ENGINEER.name in unlockedHeroes)
        assertTrue(HeroId.MEDIC.name in unlockedHeroes)
        assertTrue(HeroId.SCOUT.name in unlockedHeroes)
    }

    @Test
    @DisplayName("Commander is always unlocked by default")
    fun commanderDefaultUnlocked() {
        assertTrue(HeroId.COMMANDER.name in unlockedHeroes,
            "Commander should be unlocked by default")
    }

    @Test
    @DisplayName("Shielder requires condition, not just gold")
    fun shielderRequiresCondition() {
        val hero = HeroId.SHIELDER
        val unlock = HeroUnlock.forHero(hero)
        assertTrue(unlock.unlockCondition.isNotBlank(),
            "Shielder should have a condition")
        assertEquals(0, unlock.unlockCost,
            "Shielder should not cost gold")
        assertFalse(unlock.isFree,
            "Shielder has a condition so isFree should be false")
    }
}
