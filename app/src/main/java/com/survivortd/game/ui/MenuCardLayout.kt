package com.survivortd.game.ui

/** Layout helpers for main menu secondary cards (#158). */
object MenuCardLayout {
    const val MAX_LABEL_LINES = 1
    const val ICON_SP = 18
    const val LABEL_SP = 12

    fun shortIcon(label: String): String = when (label.lowercase()) {
        "heroes" -> "⚔️"
        "upgrades", "shop" -> "🛒"
        "settings" -> "⚙️"
        else -> label.take(2).uppercase()
    }

    fun labelFitsSingleLine(label: String, maxChars: Int = 10): Boolean =
        label.length <= maxChars
}
