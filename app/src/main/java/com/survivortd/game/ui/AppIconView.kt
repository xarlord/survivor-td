package com.survivortd.game.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.GpsFixed
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
    }

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}
