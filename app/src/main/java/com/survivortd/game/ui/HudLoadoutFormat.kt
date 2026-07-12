package com.survivortd.game.ui

/** Pure formatter for HUD loadout line (issue #149). */
object HudLoadoutFormat {
    fun format(wave: Int, weapons: Int, towers: Int): String {
        val parts = mutableListOf<String>()
        if (wave > 0) parts.add("Wave $wave")
        if (weapons > 0) parts.add("Wpn $weapons")
        if (towers > 0) parts.add("Towers $towers")
        return parts.joinToString("  ·  ")
    }
}
