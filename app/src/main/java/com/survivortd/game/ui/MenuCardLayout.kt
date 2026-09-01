package com.survivortd.game.ui

/** Layout helpers and typed presentation contracts for main-menu secondary cards. */
data class MenuCardSpec(
    val label: String,
    val icon: AppIcon
)

object MenuCardLayout {
    const val MAX_LABEL_LINES = 1
    const val ICON_SP = 22
    const val LABEL_SP = 12

    val secondaryCards = listOf(
        MenuCardSpec("Heroes", AppIcon.MENU_HEROES),
        MenuCardSpec("Upgrades", AppIcon.MENU_UPGRADES),
        MenuCardSpec("Settings", AppIcon.MENU_SETTINGS)
    )

    fun labelFitsSingleLine(label: String, maxChars: Int = 10): Boolean =
        label.length <= maxChars
}
