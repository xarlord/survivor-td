package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HudLoadoutFormatTest {
    @Test
    @DisplayName("formats wave + weapons + towers")
    fun fullLine() {
        assertEquals("Wave 3  ·  Wpn 2  ·  Towers 1", HudLoadoutFormat.format(3, 2, 1))
    }

    @Test
    @DisplayName("omits zero fields")
    fun omitsZero() {
        assertEquals("Wave 1", HudLoadoutFormat.format(1, 0, 0))
        assertEquals("", HudLoadoutFormat.format(0, 0, 0))
    }
}
