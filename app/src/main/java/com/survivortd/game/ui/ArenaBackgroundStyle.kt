package com.survivortd.game.ui

/**
 * Tunable visual constants for arena background polish.
 * Premium chapter tints (#160) — warmer / more saturated bases.
 */
object ArenaBackgroundStyle {
    const val CHAPTER_BITMAP_ALPHA = 1.0f
    const val GRID_ALPHA_PRIMARY = 0.06f
    const val GRID_ALPHA_SECONDARY = 0.03f
    const val VIGNETTE_ALPHA = 0.28f
    const val TARGET_MIN_AVG_LUMA = 45f

    fun chapterBaseColorArgb(minute: Float): Long = when {
        minute < 3f -> 0xFF4A2E1C // wasteland
        minute < 6f -> 0xFF163A28 // swamp
        minute < 9f -> 0xFF152040 // city
        minute < 12f -> 0xFF241436 // lab
        else -> 0xFF3A1418 // bunker
    }

    fun chapterGridColorArgb(minute: Float): Long = when {
        minute < 3f -> 0xFFFFB070
        minute < 6f -> 0xFF70FFB0
        minute < 9f -> 0xFF80C0FF
        minute < 12f -> 0xFFD0A0FF
        else -> 0xFFFF8080
    }
}
