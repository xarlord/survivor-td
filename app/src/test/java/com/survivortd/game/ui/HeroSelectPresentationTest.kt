package com.survivortd.game.ui

import com.survivortd.game.data.HeroId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class HeroSelectPresentationTest {
    @Test
    fun `every hero has a distinct typed icon`() {
        val icons = HeroId.entries.map { hero -> heroIcon(hero) }

        assertEquals(HeroId.entries.size, icons.size)
        assertEquals(HeroId.entries.size, icons.distinct().size)
    }

    @Test
    fun `locked hero label contains no platform emoji`() {
        val visibleCopy = lockedHeroLabel(unlockCondition = "", unlockCost = 5_000)

        listOf("🔒", "🎖️", "⚔️", "🔧", "🏥", "👁️", "🛡️").forEach { emoji ->
            assertFalse(visibleCopy.contains(emoji), "Hero-selection copy must not contain $emoji")
        }
        assertEquals("5000 Gold", visibleCopy)
    }

    @Test
    fun `unlock condition remains stable without lock prefix`() {
        assertEquals(
            "Complete Ch.1",
            lockedHeroLabel(unlockCondition = "Complete Ch.1", unlockCost = 0)
        )
    }
}
