package com.survivortd.game.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Survivor TD premium design tokens (#160).
 *
 * Aesthetic: dark AAA mobile / Linear-Raycast precision —
 * void canvas, elevated glass surfaces, cyan life, amber economy, coral danger.
 * All menu + HUD chrome should use these — no one-off neon hex soup.
 */
object StdColors {
    // Canvas
    val Void = Color(0xFF05060A)
    val VoidElevated = Color(0xFF0B0D14)
    val Surface = Color(0xFF12151F)
    val SurfaceHigh = Color(0xFF1A1F2E)
    val SurfaceGlass = Color(0xCC12151F)

    // Borders / lines
    val Border = Color(0xFF2A3142)
    val BorderStrong = Color(0xFF3D4660)
    val BorderGlow = Color(0x665EEAD4)

    // Text
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val TextInverse = Color(0xFF0B0D14)

    // Accents
    val Cyan = Color(0xFF2DD4BF)       // primary action / XP / life glow
    val CyanBright = Color(0xFF5EEAD4)
    val Amber = Color(0xFFFBBF24)      // gold / economy
    val AmberSoft = Color(0xFFFCD34D)
    val Coral = Color(0xFFF43F5E)      // HP / danger / primary CTA
    val CoralDeep = Color(0xFFE11D48)
    val Violet = Color(0xFFA78BFA)     // scrap / tech secondary
    val Scrap = Color(0xFFC4B5FD)

    // Semantic bars
    val Hp = Coral
    val HpTrack = Color(0xFF2A1520)
    val Xp = Cyan
    val XpTrack = Color(0xFF0F2430)
    val Track = Color(0xFF1E2433)

    // Atmosphere gradients (menu)
    val GradientTop = Color(0xFF101525)
    val GradientMid = Color(0xFF080A10)
    val GradientBottom = Color(0xFF0A1218)

    // Relative luminance helpers for tests (0..1, sRGB approx)
    fun relativeLuminance(c: Color): Float {
        fun lin(v: Float): Float =
            if (v <= 0.04045f) v / 12.92f else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        val r = lin(c.red)
        val g = lin(c.green)
        val b = lin(c.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /** WCAG contrast ratio between two colors. */
    fun contrastRatio(a: Color, b: Color): Float {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        val light = maxOf(l1, l2)
        val dark = minOf(l1, l2)
        return (light + 0.05f) / (dark + 0.05f)
    }
}
