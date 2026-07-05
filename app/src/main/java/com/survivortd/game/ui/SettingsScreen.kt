package com.survivortd.game.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.data.SaveManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var sfxVolume by rememberSaveable { mutableFloatStateOf(1f) }
    var bgmVolume by rememberSaveable { mutableFloatStateOf(1f) }
    var hapticsEnabled by rememberSaveable { mutableStateOf(true) }
    var minimapEnabled by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    // Load persisted settings on first composition
    LaunchedEffect(Unit) {
        val settings = SaveManager.loadSettings(context).first()
        sfxVolume = settings.sfxVolume
        bgmVolume = settings.bgmVolume
        hapticsEnabled = settings.hapticsEnabled
        minimapEnabled = settings.minimapVisible
    }

    // Save settings whenever they change
    LaunchedEffect(sfxVolume, bgmVolume, hapticsEnabled, minimapEnabled) {
        SaveManager.saveSettings(
            context,
            SaveManager.GameSettings(
                sfxVolume = sfxVolume,
                bgmVolume = bgmVolume,
                minimapVisible = minimapEnabled,
                hapticsEnabled = hapticsEnabled,
                isFirstRun = false
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333A4D))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "<- BACK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SettingsSliderRow(
                label = "SFX Volume",
                value = sfxVolume,
                onValueChange = { sfxVolume = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSliderRow(
                label = "BGM Volume",
                value = bgmVolume,
                onValueChange = { bgmVolume = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsToggleRow(
                label = "Haptics",
                checked = hapticsEnabled,
                onCheckedChange = { hapticsEnabled = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggleRow(
                label = "Minimap",
                checked = minimapEnabled,
                onCheckedChange = { minimapEnabled = it }
            )
        }
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color(0xFF00E676),
                activeTrackColor = Color(0xFF00E676),
                inactiveTrackColor = Color(0xFF333A4D)
            )
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E676),
                checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFF333A4D)
            )
        )
    }
}
