package com.survivortd.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.config.TowerType

/**
 * Build Phase UI (GDD §7 / §12.3) — issue #147.
 *
 * Shown while WaveSystem.isBuildPhase is true after a boss death.
 * Player picks a tower type, then taps the map to place it with scrap.
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
            .background(Color(0xF00A0E1A), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.55f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .testTag("build_phase_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BUILD PHASE  ·  ${remainingSeconds.toInt().coerceAtLeast(0)}s",
            color = Color(0xFFFFE082),
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.testTag("build_phase_timer")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Scrap: $scrap  ·  Towers: $towersPlaced/$maxTowers",
            color = Color(0xFFECEFF1),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("build_phase_scrap")
        )
        if (selected != null) {
            Text(
                text = "Tap map to place ${selected.displayName}",
                color = Color(0xFF69F0AE),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TowerType.entries.forEach { type ->
                val canAfford = scrap >= type.baseCost
                val isSelected = selected == type
                val borderColor = when {
                    isSelected -> Color(0xFFFFD700)
                    canAfford -> Color(0xFF69F0AE)
                    else -> Color(0xFF546E7A)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, borderColor, RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF1B5E20) else Color(0xFF263238)
                        )
                        .clickable(enabled = canAfford) {
                            onSelect(if (isSelected) null else type)
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("tower_btn_${type.name}")
                ) {
                    Text(
                        text = towerEmoji(type),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = type.baseCost.toString(),
                        color = if (canAfford) Color.White else Color(0xFF90A4AE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select a tower, then tap the battlefield",
            color = Color(0xFFCFD8DC),
            fontSize = 12.sp
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

/**
 * Pure helper: convert screen (canvas) coords → world coords given camera center.
 * Used by placement + unit tests.
 */
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
