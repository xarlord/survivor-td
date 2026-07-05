package com.survivortd.game.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class UpgradeItem(
    val id: String,
    val name: String,
    val description: String,
    val currentLevel: Int,
    val maxLevel: Int,
    val baseCost: Int,
    val onBuy: () -> Boolean
)

@Composable
fun ShopScreen(
    meta: MetaProgression,
    onBuy: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val upgrades = listOf(
        UpgradeItem("maxHp", "Max HP", "+20 HP per level", meta.maxHpLevel, 10, 500,
            { meta.buyMaxHp().also { if (it) onBuy("maxHp") } }),
        UpgradeItem("moveSpeed", "Move Speed", "+10 px/s per level", meta.moveSpeedLevel, 5, 500,
            { meta.buyMoveSpeed().also { if (it) onBuy("moveSpeed") } }),
        UpgradeItem("damage", "Damage", "+5% damage per level", meta.damageLevel, 10, 800,
            { meta.buyDamage().also { if (it) onBuy("damage") } }),
        UpgradeItem("pickupRange", "Pickup Range", "+10px per level", meta.pickupRangeLevel, 5, 400,
            { meta.buyPickupRange().also { if (it) onBuy("pickupRange") } }),
        UpgradeItem("extraLife", "Extra Life", "+1 revive per level", meta.extraLifeLevel, 3, 5000,
            { meta.buyExtraLife().also { if (it) onBuy("extraLife") } }),
        UpgradeItem("xpGain", "XP Gain", "+5% XP per level", meta.xpGainLevel, 10, 600,
            { meta.buyXpGain().also { if (it) onBuy("xpGain") } }),
        UpgradeItem("goldFind", "Gold Find", "+10% gold per level", meta.goldFindLevel, 5, 1000,
            { meta.buyGoldFind().also { if (it) onBuy("goldFind") } }),
        UpgradeItem("towerDiscount", "Tower Discount", "-10% tower cost per level", meta.towerDiscountLevel, 3, 2000,
            { meta.buyTowerDiscount().also { if (it) onBuy("towerDiscount") } }),
        UpgradeItem("startingWeapon", "Starting Weapon", "Start match with weapon at Lv.2", meta.startingWeaponLevel, 3, 3000,
            { meta.buyStartingWeapon().also { if (it) onBuy("startingWeapon") } })
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333A4D))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("<- BACK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("UPGRADES", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333A4D))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("G ${meta.gold}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upgrades, key = { it.id }) { upgrade ->
                    UpgradeCard(item = upgrade, onBuy = { upgrade.onBuy() })
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(item: UpgradeItem, onBuy: () -> Unit) {
    val cost = MetaProgression.upgradeCost(item.baseCost, item.currentLevel)
    val maxed = item.currentLevel >= item.maxLevel

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = if (maxed) "MAX" else "Lv. ${item.currentLevel}/${item.maxLevel}",
                    fontSize = 14.sp,
                    color = if (maxed) Color(0xFF00E676) else Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, fontSize = 13.sp, color = Color(0xFF9E9E9E))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (maxed) Color(0xFF333A4D) else Color(0xFF00E676))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (maxed) "MAXED OUT" else "G $cost",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (maxed) Color(0xFF9E9E9E) else Color(0xFF0A0E1A)
                    )
                }
            }
        }
    }
}
