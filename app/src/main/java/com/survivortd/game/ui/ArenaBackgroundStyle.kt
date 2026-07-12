package com.survivortd.game.ui

/**
 * Tunable visual constants for arena background polish (#157).
 * Kept pure so unit tests can lock readability targets.
 */
object ArenaBackgroundStyle {
    /** Chapter bitmap draw alpha (1 = full opacity). */
    const val CHAPTER_BITMAP_ALPHA = 1.0f

    /** Primary orientation grid alpha (was ~0.35 — too strong over dark art). */
    const val GRID_ALPHA_PRIMARY = 0.08f

    /** Secondary coarser grid (very subtle). */
    const val GRID_ALPHA_SECONDARY = 0.04f

    /** Edge vignette max alpha for focus on playfield. */
    const val VIGNETTE_ALPHA = 0.35f

    /** Minimum average luma target for packed chapter PNGs (0-255). */
    const val TARGET_MIN_AVG_LUMA = 45f

    fun chapterBaseColorArgb(minute: Float): Long = when {
        minute < 3f -> 0xFF3A2A1A // wasteland warm earth
        minute < 6f -> 0xFF1A3A22 // swamp green
        minute < 9f -> 0xFF1A2238 // city night blue
        minute < 12f -> 0xFF1E1A32 // lab purple
        else -> 0xFF3A1A1A // bunker red-brown
    }

    fun chapterGridColorArgb(minute: Float): Long = when {
        minute < 3f -> 0xFFE8C080
        minute < 6f -> 0xFF80E8A0
        minute < 9f -> 0xFF80B0FF
        minute < 12f -> 0xFFC080FF
        else -> 0xFFFF8080
    }
}
