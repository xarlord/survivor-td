package com.survivortd.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.config.TowerType
import com.survivortd.game.ui.theme.StdColors

/**
 * Build Phase UI (GDD §7 / §12.3) — premium chrome (#160).
 */
@Composable
fun BuildPhaseOverlay(
    scrap: Int,
    towersPlaced: Int,
    maxTowers: Int,
    remainingSeconds: Float,
    selected: TowerType?,
    onSelect: (TowerType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(StdColors.SurfaceGlass)
            .border(
                1.dp,
                StdColors.BorderStrong,
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("build_phase_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BUILD PHASE  ·  ${remainingSeconds.toInt().coerceAtLeast(0)}s",
            color = StdColors.AmberSoft,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.testTag("build_phase_timer")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scrap $scrap  ·  Towers $towersPlaced/$maxTowers",
            color = StdColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("build_phase_scrap")
        )
        if (selected != null) {
            Text(
                text = "Tap map to place ${selected.displayName}",
                color = StdColors.CyanBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TowerType.entries.forEach { type ->
                val canAfford = scrap >= type.baseCost
                val isSelected = selected == type
                val borderColor = when {
                    isSelected -> StdColors.Amber
                    canAfford -> StdColors.Cyan
                    else -> StdColors.Border
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) StdColors.SurfaceHigh else StdColors.Surface
                        )
                        .clickable(enabled = canAfford) {
                            onSelect(if (isSelected) null else type)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("tower_btn_${type.name}")
                ) {
                    Text(
                        text = towerEmoji(type),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = type.baseCost.toString(),
                        color = if (canAfford) StdColors.TextPrimary else StdColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a tower, then tap the battlefield",
            color = StdColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

private fun towerEmoji(type: TowerType): String = when (type) {
    TowerType.GUN_TURRET -> "🔫"
    TowerType.CANNON -> "💣"
    TowerType.FROST_TOWER -> "❄️"
    TowerType.TESLA_COIL -> "⚡"
    TowerType.POISON_TOWER -> "☠️"
    TowerType.ROCKET_POD -> "🚀"
}

object BuildPlacement {
    fun screenToWorld(
        screenX: Float,
        screenY: Float,
        canvasW: Float,
        canvasH: Float,
        cameraX: Float,
        cameraY: Float,
        cameraW: Float = com.survivortd.game.config.GameConfig.CAMERA_WIDTH,
        cameraH: Float = com.survivortd.game.config.GameConfig.CAMERA_HEIGHT
    ): Pair<Float, Float> {
        if (canvasW <= 0f || canvasH <= 0f) return cameraX to cameraY
        val worldX = cameraX - cameraW / 2f + (screenX / canvasW) * cameraW
        val worldY = cameraY - cameraH / 2f + (screenY / canvasH) * cameraH
        return worldX to worldY
    }
}
