package com.survivortd.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.systems.UpgradeChoice
import kotlinx.coroutines.delay

/**
 * Level-Up Dialog — shows upgrade choices when the player levels up.
 * [#88] Translucent overlay — game continues behind it (Vampire Survivors style).
 * 10-second timer auto-selects a random card if player doesn't choose.
 */
@Composable
fun LevelUpDialog(
    level: Int,
    choices: List<UpgradeChoice>,
    onChoiceSelected: (UpgradeChoice) -> Unit,
    modifier: Modifier = Modifier,
    onTimeout: (UpgradeChoice) -> Unit = onChoiceSelected
) {
    var timeRemaining by remember { mutableIntStateOf(10) }

    // 10-second countdown with auto-select
    LaunchedEffect(choices) {
        timeRemaining = 10
        for (i in 10 downTo 1) {
            delay(1000)
            timeRemaining = i - 1
        }
        // Auto-select first choice on timeout
        if (choices.isNotEmpty()) {
            onTimeout(choices.first())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            // Level-up is a true blocking modal. Consume the full scrim
            // gesture, including drags that start outside a card.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            // Title + Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LEVEL UP!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${timeRemaining}s",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timeRemaining <= 3) Color(0xFFFF1744) else Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Level $level — Choose an Upgrade",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Upgrade choices (max 3 per GDD §6)
            choices.take(3).forEach { choice ->
                UpgradeCard(choice = choice) { onChoiceSelected(choice) }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Single upgrade choice card with rarity-colored border.
 */
@Composable
private fun UpgradeCard(
    choice: UpgradeChoice,
    onClick: () -> Unit
) {
    val borderColor = Color(choice.rarity.color)
    val iconColor = Color(choice.iconColor)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.3f))
                .border(1.dp, iconColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = choiceIcon(choice),
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = choice.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = choice.description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Rarity tag
        Text(
            text = choice.rarity.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = borderColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun choiceIcon(choice: UpgradeChoice): String {
    return when (choice.type) {
        com.survivortd.game.systems.UpgradeType.NEW_WEAPON -> "⚔"
        com.survivortd.game.systems.UpgradeType.UPGRADE_WEAPON -> "↑"
        com.survivortd.game.systems.UpgradeType.NEW_PASSIVE -> "✦"
        com.survivortd.game.systems.UpgradeType.UPGRADE_PASSIVE -> "★"
        com.survivortd.game.systems.UpgradeType.HEAL -> "♥"
        com.survivortd.game.systems.UpgradeType.STAT_BOOST -> "⚡"
    }
}
