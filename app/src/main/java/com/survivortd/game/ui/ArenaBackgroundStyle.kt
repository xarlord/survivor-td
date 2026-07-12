package com.survivortd.game.ui

import androidx.compose.ui.graphics.Color

/**
 * Tunable visual constants for arena background polish (#157 / #160).
 */
object ArenaBackgroundStyle {
    const val CHAPTER_BITMAP_ALPHA = 0.72f
    const val GRID_ALPHA_PRIMARY = 0.05f
    const val GRID_ALPHA_SECONDARY = 0.025f
    const val VIGNETTE_ALPHA = 0.32f
    const val TARGET_MIN_AVG_LUMA = 45f

    data class Palette(
        val skyTop: Color,
        val skyMid: Color,
        val horizon: Color,
        val ground: Color,
        val groundDeep: Color,
        val silhouette: Color,
        val haze: Color,
        val grid: Color
    )

    fun chapterPalette(minute: Float): Palette = when {
        minute < 3f -> Palette( // wasteland dusk — high drama
            skyTop = Color(0xFF0D0818),
            skyMid = Color(0xFF4A1830),
            horizon = Color(0xFFFF8A3D),
            ground = Color(0xFF3D2418),
            groundDeep = Color(0xFF140C08),
            silhouette = Color(0xFF080408),
            haze = Color(0xFFFFB070),
            grid = Color(0xFFFFC090)
        )
        minute < 6f -> Palette( // swamp
            skyTop = Color(0xFF061810),
            skyMid = Color(0xFF0E3A28),
            horizon = Color(0xFF1A6A48),
            ground = Color(0xFF143828),
            groundDeep = Color(0xFF0A1C14),
            silhouette = Color(0xFF05140C),
            haze = Color(0xFF60E0A0),
            grid = Color(0xFF80FFC0)
        )
        minute < 9f -> Palette( // city night
            skyTop = Color(0xFF080C1C),
            skyMid = Color(0xFF152048),
            horizon = Color(0xFF3A50A0),
            ground = Color(0xFF1A2238),
            groundDeep = Color(0xFF0C101C),
            silhouette = Color(0xFF060810),
            haze = Color(0xFF80B0FF),
            grid = Color(0xFF90C0FF)
        )
        minute < 12f -> Palette( // lab
            skyTop = Color(0xFF12081C),
            skyMid = Color(0xFF2A1450),
            horizon = Color(0xFF7040C0),
            ground = Color(0xFF241434),
            groundDeep = Color(0xFF10081A),
            silhouette = Color(0xFF0A0610),
            haze = Color(0xFFD0A0FF),
            grid = Color(0xFFE0B0FF)
        )
        else -> Palette( // bunker
            skyTop = Color(0xFF1A0808),
            skyMid = Color(0xFF4A1418),
            horizon = Color(0xFFA03030),
            ground = Color(0xFF3A1818),
            groundDeep = Color(0xFF140808),
            silhouette = Color(0xFF0A0404),
            haze = Color(0xFFFF7070),
            grid = Color(0xFFFF9090)
        )
    }

    /** @deprecated prefer chapterPalette */
    fun chapterBaseColorArgb(minute: Float): Long =
        chapterPalette(minute).ground.value.toLong() and 0xFFFFFFFFL

    fun chapterGridColorArgb(minute: Float): Long =
        chapterPalette(minute).grid.value.toLong() and 0xFFFFFFFFL
}
