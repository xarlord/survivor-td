package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MenuCardLayoutTest {
    @Test
    @DisplayName("Secondary navigation uses stable labels and typed distinct icons")
    fun secondaryNavigationContract() {
        val cards = MenuCardLayout.secondaryCards

        assertEquals(listOf("Heroes", "Upgrades", "Settings"), cards.map { it.label })
        assertEquals(3, cards.map { it.icon }.distinct().size)
        assertEquals(
            listOf(AppIcon.MENU_HEROES, AppIcon.MENU_UPGRADES, AppIcon.MENU_SETTINGS),
            cards.map { it.icon }
        )
    }

    @Test
    @DisplayName("Secondary navigation exposes no platform-dependent emoji glyphs")
    fun noEmojiDisplayFields() {
        val visibleCopy = MenuCardLayout.secondaryCards.joinToString(" ") { it.label }
        listOf("⚔️", "🛒", "⚙️").forEach { emoji ->
            assertFalse(visibleCopy.contains(emoji))
        }
    }

    @Test
    @DisplayName("Settings label fits single line at 10-char budget")
    fun settingsLabelFits() {
        assertTrue(MenuCardLayout.labelFitsSingleLine("Settings", 10))
        assertEquals(1, MenuCardLayout.MAX_LABEL_LINES)
    }
}
