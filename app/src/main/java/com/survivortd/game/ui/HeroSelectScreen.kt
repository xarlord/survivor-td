package com.survivortd.game.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.data.HeroId
import com.survivortd.game.data.HeroUnlock

/**
 * Hero Select Screen — grid of 6 hero cards.
 *
 * - Unlocked heroes show a green checkmark and can be selected.
 * - Locked heroes show a lock icon + unlock cost/condition.
 * - Tapping a locked hero with enough gold shows an unlock dialog.
 * - Selected hero has a gold border.
 * - Back button returns to the previous screen.
 *
 * GDD §3.3
 */
@Composable
fun HeroSelectScreen(
    unlockedHeroes: Set<String>,
    selectedHero: String,
    playerGold: Int,
    onHeroSelected: (String) -> Unit,
    onUnlockHero: (HeroId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heroes = HeroId.entries
    var unlockDialogHero by remember { mutableStateOf<HeroUnlock?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .testTag("hero_select_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← BACK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .testTag("hero_back_button")
                        .padding(8.dp)
                )
                Text(
                    text = "SELECT HERO",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hero grid: 2 columns x 3 rows
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("hero_grid")
            ) {
                items(heroes, key = { it.name }) { hero ->
                    val isUnlocked = hero.name in unlockedHeroes
                    val isSelected = hero.name == selectedHero
                    val unlockInfo = HeroUnlock.forHero(hero)

                    HeroCard(
                        hero = hero,
                        isUnlocked = isUnlocked,
                        isSelected = isSelected,
                        unlockInfo = unlockInfo,
                        playerGold = playerGold,
                        onClick = {
                            if (isUnlocked) {
                                onHeroSelected(hero.name)
                            } else {
                                unlockDialogHero = unlockInfo
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected hero info
            val selected = heroes.find { it.name == selectedHero }
            if (selected != null && selected.name in unlockedHeroes) {
                Text(
                    text = "Starting weapon: ${selected.startingWeapon.displayName}",
                    fontSize = 14.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("hero_starting_weapon")
                )
            }
        }
    }

    // Unlock confirmation dialog
    unlockDialogHero?.let { unlock ->
        val hero = unlock.heroId
        val canAfford = playerGold >= unlock.unlockCost
        AlertDialog(
            onDismissRequest = { unlockDialogHero = null },
            containerColor = Color(0xFF1E1E2E),
            title = {
                Text(
                    text = "Unlock ${hero.displayName}?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = hero.description,
                        color = Color(0xFF9E9E9E),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (unlock.unlockCondition.isNotEmpty()) {
                        Text(
                            text = "Requirement: ${unlock.unlockCondition}",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp
                        )
                    } else if (unlock.unlockCost > 0) {
                        val goldColor = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF1744)
                        Text(
                            text = "Cost: ${unlock.unlockCost} Gold",
                            color = goldColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!canAfford) {
                            Text(
                                text = "Not enough gold!",
                                color = Color(0xFFFF1744),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (canAfford || unlock.unlockCost == 0) {
                            onUnlockHero(hero)
                        }
                        unlockDialogHero = null
                    },
                    enabled = canAfford || unlock.unlockCost == 0
                ) {
                    Text(
                        text = "UNLOCK",
                        color = if (canAfford || unlock.unlockCost == 0)
                            Color(0xFF00E676) else Color(0xFF555555),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { unlockDialogHero = null }) {
                    Text("CANCEL", color = Color(0xFF9E9E9E))
                }
            }
        )
    }
}

/**
 * Individual hero card in the selection grid.
 */
@Composable
private fun HeroCard(
    hero: HeroId,
    isUnlocked: Boolean,
    isSelected: Boolean,
    unlockInfo: HeroUnlock,
    playerGold: Int,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> Color(0xFFFFD700)  // Gold border for selected
        isUnlocked -> Color(0xFF00E676)  // Green border for unlocked
        else -> Color(0xFF333A4D)        // Gray border for locked
    }
    val borderWidth = if (isSelected) 3.dp else 2.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("hero_card_${hero.name}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero icon/emoji
        val icon = when (hero) {
            HeroId.COMMANDER -> "🎖️"
            HeroId.BERSERKER -> "⚔️"
            HeroId.ENGINEER -> "🔧"
            HeroId.MEDIC -> "🏥"
            HeroId.SCOUT -> "👁️"
            HeroId.SHIELDER -> "🛡️"
        }
        Text(text = icon, fontSize = 32.sp)

        Spacer(modifier = Modifier.height(8.dp))

        // Hero name
        Text(
            text = hero.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) Color.White else Color(0xFF9E9E9E)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = hero.description,
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Starting weapon
        Text(
            text = hero.startingWeapon.displayName,
            fontSize = 11.sp,
            color = Color(0xFF42A5F5),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status indicator
        if (isUnlocked) {
            if (isSelected) {
                Text(
                    text = "✓ SELECTED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            } else {
                Text(
                    text = "✓ UNLOCKED",
                    fontSize = 11.sp,
                    color = Color(0xFF00E676)
                )
            }
        } else {
            // Lock status
            Text(
                text = if (unlockInfo.unlockCondition.isNotEmpty()) {
                    "🔒 ${unlockInfo.unlockCondition}"
                } else {
                    "🔒 ${unlockInfo.unlockCost} Gold"
                },
                fontSize = 11.sp,
                color = if (playerGold >= unlockInfo.unlockCost)
                    Color(0xFFFFD700) else Color(0xFFFF1744)
            )
        }
    }
}
