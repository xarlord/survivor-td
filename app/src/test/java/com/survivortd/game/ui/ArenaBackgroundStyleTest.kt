package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ArenaBackgroundStyleTest {
    @Test
    @DisplayName("grid alpha stays subtle so art can dominate")
    fun gridSubtle() {
        assertTrue(ArenaBackgroundStyle.GRID_ALPHA_PRIMARY < 0.12f)
        assertTrue(ArenaBackgroundStyle.GRID_ALPHA_SECONDARY < ArenaBackgroundStyle.GRID_ALPHA_PRIMARY)
        assertTrue(ArenaBackgroundStyle.CHAPTER_BITMAP_ALPHA in 0.5f..1.0f)
    }

    @Test
    @DisplayName("each chapter has distinct palette")
    fun chapterPalettesDistinct() {
        val a = ArenaBackgroundStyle.chapterPalette(0f)
        val b = ArenaBackgroundStyle.chapterPalette(4f)
        val c = ArenaBackgroundStyle.chapterPalette(10f)
        assertTrue(a.skyMid != b.skyMid)
        assertTrue(b.ground != c.ground)
        assertTrue(a.horizon != c.horizon)
    }
}
