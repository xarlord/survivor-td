package com.survivortd.game.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun AppIconView(
    icon: AppIcon,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val imageVector = when (icon) {
        AppIcon.MENU_HEROES -> Icons.Rounded.Person
        AppIcon.MENU_UPGRADES -> Icons.Rounded.Star
        AppIcon.MENU_SETTINGS -> Icons.Rounded.Settings
        AppIcon.MOVEMENT -> Icons.Rounded.Games
        AppIcon.AUTO_ATTACK -> Icons.Rounded.FlashOn
        AppIcon.LEVEL_UP -> Icons.Rounded.ArrowUpward
        AppIcon.UPGRADES -> Icons.Rounded.Paid
        AppIcon.TOWERS -> Icons.Rounded.Shield
        AppIcon.PAUSE -> Icons.Rounded.PauseCircle
        AppIcon.TOWER_GUN -> Icons.Rounded.GpsFixed
        AppIcon.TOWER_CANNON -> Icons.Rounded.Adjust
        AppIcon.TOWER_FROST -> Icons.Rounded.AcUnit
        AppIcon.TOWER_TESLA -> Icons.Rounded.Bolt
        AppIcon.TOWER_POISON -> Icons.Rounded.Science
        AppIcon.TOWER_ROCKET -> Icons.Rounded.RocketLaunch
        AppIcon.HERO_COMMANDER -> Icons.Rounded.MilitaryTech
        AppIcon.HERO_BERSERKER -> Icons.Rounded.FitnessCenter
        AppIcon.HERO_ENGINEER -> Icons.Rounded.Build
        AppIcon.HERO_MEDIC -> Icons.Rounded.MedicalServices
        AppIcon.HERO_SCOUT -> Icons.Rounded.Explore
        AppIcon.HERO_SHIELDER -> Icons.Rounded.Shield
        AppIcon.LOCKED -> Icons.Rounded.Lock
        AppIcon.COINS -> Icons.Rounded.Paid
        AppIcon.UPGRADE_NEW_WEAPON -> Icons.Rounded.FlashOn
        AppIcon.UPGRADE_WEAPON -> Icons.Rounded.ArrowUpward
        AppIcon.UPGRADE_NEW_PASSIVE -> Icons.Rounded.Star
        AppIcon.UPGRADE_PASSIVE -> Icons.Rounded.ArrowUpward
        AppIcon.UPGRADE_HEAL -> Icons.Rounded.MedicalServices
        AppIcon.UPGRADE_STAT_BOOST -> Icons.Rounded.Bolt
        AppIcon.RESULT_TIME -> Icons.Rounded.PauseCircle
        AppIcon.RESULT_LEVEL -> Icons.Rounded.MilitaryTech
        AppIcon.RESULT_KILLS -> Icons.Rounded.GpsFixed
        AppIcon.RESULT_BONUS -> Icons.Rounded.Star
        AppIcon.RESULT_WEAPONS -> Icons.Rounded.FitnessCenter
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}
