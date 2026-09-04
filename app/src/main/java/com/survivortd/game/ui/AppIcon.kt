package com.survivortd.game.ui

import com.survivortd.game.systems.UpgradeType

/** Typed icon vocabulary for stable application presentation. */
enum class AppIcon {
    MENU_HEROES,
    MENU_UPGRADES,
    MENU_SETTINGS,
    MOVEMENT,
    AUTO_ATTACK,
    LEVEL_UP,
    UPGRADES,
    TOWERS,
    PAUSE,
    TOWER_GUN,
    TOWER_CANNON,
    TOWER_FROST,
    TOWER_TESLA,
    TOWER_POISON,
    TOWER_ROCKET,
    HERO_COMMANDER,
    HERO_BERSERKER,
    HERO_ENGINEER,
    HERO_MEDIC,
    HERO_SCOUT,
    HERO_SHIELDER,
    LOCKED,
    COINS,
    UPGRADE_NEW_WEAPON,
    UPGRADE_WEAPON,
    UPGRADE_NEW_PASSIVE,
    UPGRADE_PASSIVE,
    UPGRADE_HEAL,
    UPGRADE_STAT_BOOST,
    RESULT_TIME,
    RESULT_LEVEL,
    RESULT_KILLS,
    RESULT_BONUS,
    RESULT_WEAPONS
}

/** Stable visual and accessibility contract for the gameplay HUD pause control. */
object HudPausePresentation {
    val icon: AppIcon = AppIcon.PAUSE
    const val contentDescription: String = "Pause game"
}

fun UpgradeType.appIcon(): AppIcon = when (this) {
    UpgradeType.NEW_WEAPON -> AppIcon.UPGRADE_NEW_WEAPON
    UpgradeType.UPGRADE_WEAPON -> AppIcon.UPGRADE_WEAPON
    UpgradeType.NEW_PASSIVE -> AppIcon.UPGRADE_NEW_PASSIVE
    UpgradeType.UPGRADE_PASSIVE -> AppIcon.UPGRADE_PASSIVE
    UpgradeType.HEAL -> AppIcon.UPGRADE_HEAL
    UpgradeType.STAT_BOOST -> AppIcon.UPGRADE_STAT_BOOST
}

enum class ResultMetric(val label: String) {
    TIME("Time"),
    LEVEL("Level"),
    KILLS("Kills"),
    GOLD("Gold"),
    BONUS("Bonus"),
    WEAPONS("Weapons")
}

fun ResultMetric.appIcon(): AppIcon = when (this) {
    ResultMetric.TIME -> AppIcon.RESULT_TIME
    ResultMetric.LEVEL -> AppIcon.RESULT_LEVEL
    ResultMetric.KILLS -> AppIcon.RESULT_KILLS
    ResultMetric.GOLD -> AppIcon.COINS
    ResultMetric.BONUS -> AppIcon.RESULT_BONUS
    ResultMetric.WEAPONS -> AppIcon.RESULT_WEAPONS
}

fun coinLabel(amount: Int): String = "$amount coins"
