package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MenuCardLayoutTest {
    @Test
    @DisplayName("Settings uses short gear icon not multi-letter GEAR word")
    fun settingsIconShort() {
        val icon = MenuCardLayout.shortIcon("Settings")
        assertTrue(icon.codePointCount(0, icon.length) <= 2)
        assertFalse(icon.equals("GEAR", ignoreCase = true))
    }

    @Test
    @DisplayName("Settings label fits single line at 10-char budget")
    fun settingsLabelFits() {
        assertTrue(MenuCardLayout.labelFitsSingleLine("Settings", 10))
        assertTrue(MenuCardLayout.MAX_LABEL_LINES == 1)
    }
}
