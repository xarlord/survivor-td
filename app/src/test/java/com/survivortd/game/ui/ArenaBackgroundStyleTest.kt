package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ArenaBackgroundStyleTest {
    @Test
    @DisplayName("grid alpha is soft enough not to dominate chapter art")
    fun softGrid() {
        assertTrue(ArenaBackgroundStyle.GRID_ALPHA_PRIMARY <= 0.12f)
        assertTrue(ArenaBackgroundStyle.GRID_ALPHA_SECONDARY < ArenaBackgroundStyle.GRID_ALPHA_PRIMARY)
        assertEquals(1.0f, ArenaBackgroundStyle.CHAPTER_BITMAP_ALPHA, 0.001f)
    }

    @Test
    @DisplayName("chapter base colors differ across minutes (themed arenas)")
    fun themedBases() {
        val c0 = ArenaBackgroundStyle.chapterBaseColorArgb(0f)
        val c1 = ArenaBackgroundStyle.chapterBaseColorArgb(4f)
        val c2 = ArenaBackgroundStyle.chapterBaseColorArgb(10f)
        val c3 = ArenaBackgroundStyle.chapterBaseColorArgb(14f)
        assertTrue(setOf(c0, c1, c2, c3).size >= 3)
    }
}
