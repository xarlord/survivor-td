package com.survivortd.game.ui.theme

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StdColorsTest {
    @Test
    @DisplayName("primary text on void meets ~AA contrast (>=4.5)")
    fun textOnVoidReadable() {
        val ratio = StdColors.contrastRatio(StdColors.TextPrimary, StdColors.Void)
        assertTrue(ratio >= 4.5f, "text/void contrast was $ratio")
    }

    @Test
    @DisplayName("amber gold readable on surface")
    fun amberOnSurface() {
        val ratio = StdColors.contrastRatio(StdColors.Amber, StdColors.Surface)
        assertTrue(ratio >= 3.0f, "amber/surface contrast was $ratio")
    }

    @Test
    @DisplayName("accents are distinct (not monochrome soup)")
    fun accentsDistinct() {
        val cyan = StdColors.relativeLuminance(StdColors.Cyan)
        val amber = StdColors.relativeLuminance(StdColors.Amber)
        val coral = StdColors.relativeLuminance(StdColors.Coral)
        // Hues differ — luminance alone won't catch that; ensure none equal
        assertTrue(setOf(cyan, amber, coral).size == 3 ||
            (StdColors.Cyan != StdColors.Amber && StdColors.Amber != StdColors.Coral))
        assertTrue(StdColors.Cyan != StdColors.Amber)
        assertTrue(StdColors.Coral != StdColors.Cyan)
    }

    @Test
    @DisplayName("void is darker than elevated surface (depth hierarchy)")
    fun depthHierarchy() {
        assertTrue(
            StdColors.relativeLuminance(StdColors.Void) <
                StdColors.relativeLuminance(StdColors.Surface)
        )
        assertTrue(
            StdColors.relativeLuminance(StdColors.Surface) <
                StdColors.relativeLuminance(StdColors.SurfaceHigh)
        )
    }
}
