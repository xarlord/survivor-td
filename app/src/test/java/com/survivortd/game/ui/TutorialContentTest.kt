package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TutorialContentTest {
    @Test
    fun `tutorial uses six stable typed steps without emoji`() {
        assertEquals(6, TutorialContent.steps.size)
        assertEquals(6, TutorialContent.steps.map { it.id }.distinct().size)

        val visibleCopy = TutorialContent.steps.joinToString(" ") { "${it.title} ${it.instruction}" }
        listOf("🕹", "⚔", "⬆", "💰", "🛡", "⏸").forEach { emoji ->
            assertFalse(visibleCopy.contains(emoji), "Tutorial must not contain $emoji")
        }
    }

    @Test
    fun `movement teaches vertical control and double tap dash`() {
        val movement = TutorialContent.steps.single { it.id == TutorialStepId.MOVEMENT }
        assertTrue(movement.instruction.contains("up and down", ignoreCase = true))
        assertTrue(movement.instruction.contains("double-tap", ignoreCase = true))
        assertTrue(movement.instruction.contains("dash", ignoreCase = true))
    }

    @Test
    fun `progression terminology matches Upgrades menu`() {
        val visibleCopy = TutorialContent.steps.joinToString(" ") { "${it.title} ${it.instruction}" }
        assertTrue(visibleCopy.contains("Upgrades"))
        assertFalse(visibleCopy.contains("Shop", ignoreCase = true))
    }
}
