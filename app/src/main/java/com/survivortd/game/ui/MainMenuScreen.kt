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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag

@Composable
fun MainMenuScreen(
    gold: Int = 0,
    onPlay: () -> Unit = {},
    onHeroes: () -> Unit = {},
    onShop: () -> Unit = {},
    onSettings: () -> Unit = {}
)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .testTag("main_menu")
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333A4D))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("G", fontSize = 16.sp, color = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$gold",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.testTag("gold_display")
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = "SURVIVOR TD",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E676),
                modifier = Modifier.testTag("title")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Survive. Build. Evolve.",
                fontSize = 16.sp,
                color = Color(0xFF9E9E9E)
            )
            Spacer(modifier = Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676))
                    .clickable(onClick = onPlay)
                    .testTag("play_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0E1A)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuButton(
                    label = "Heroes", icon = "SWORD", onClick = onHeroes,
                    modifier = Modifier.testTag("heroes_button")
                )
                MenuButton(
                    label = "Upgrades", icon = "SHOP", onClick = onShop,
                    modifier = Modifier.testTag("shop_button")
                )
                MenuButton(
                    label = "Settings", icon = "GEAR", onClick = onSettings,
                    modifier = Modifier.testTag("settings_button")
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E2E))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .size(width = 100.dp, height = 80.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 14.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
