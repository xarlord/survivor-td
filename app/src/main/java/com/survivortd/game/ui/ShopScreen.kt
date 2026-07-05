package com.survivortd.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.systems.MetaProgression

/**
 * Shop screen — displays all meta-progression upgrades.
 * Player spends gold earned from matches to buy permanent upgrades.
 */
@Composable
fun ShopScreen(
    meta: MetaProgression,
    onBuy: (MetaProgression.UpgradeItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPGRADE SHOP",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "🪙 ${meta.gold}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gold persists across runs. Upgrade to get stronger!",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Upgrade grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(MetaProgression.UpgradeItem.entries) { item ->
                    UpgradeCard(
                        item = item,
                        meta = meta,
                        onBuy = { onBuy(item) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF333A4D))
                    .clickable(onClick = onBack)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "← BACK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    item: MetaProgression.UpgradeItem,
    meta: MetaProgression,
    onBuy: () -> Unit
) {
    val currentLevel = item.currentLevel(meta)
    val isMaxed = item.isMaxed(meta)
    val cost = item.cost(meta)
    val canAfford = meta.gold >= cost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .then(
                if (!isMaxed && canAfford) {
                    Modifier.clickable(onClick = onBuy)
                } else {
                    Modifier
                }
            )
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = item.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMaxed) Color(0xFF9E9E9E) else Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Level bar
                Text(
                    text = "Lv. $currentLevel/${item.maxLevel}",
                    fontSize = 11.sp,
                    color = if (isMaxed) Color(0xFF00E676) else Color(0xFF42A5F5),
                    fontWeight = FontWeight.Bold
                )
                // Cost
                if (!isMaxed) {
                    Text(
                        text = "🪙 $cost",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF1744)
                    )
                } else {
                    Text(
                        text = "MAX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}
