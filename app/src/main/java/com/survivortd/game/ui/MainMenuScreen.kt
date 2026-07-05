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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B10))
            .testTag("main_menu")
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
                .background(Color(0xFF1C1921), RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFA832FF),
                    shape = RoundedCornerShape(8.dp)
                )
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
                color = Color(0xFFF3EFF7),
                modifier = Modifier
                    .testTag("title")
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = Color(0xFFFF1F44).copy(alpha = 0.15f).toArgb()
                                setShadowLayer(
                                    25.dp.toPx(),
                                    0f,
                                    0f,
                                    Color(0xFFFF1F44).toArgb()
                                )
                            }
                            val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                            canvas.nativeCanvas.drawRoundRect(
                                rect,
                                12.dp.toPx(),
                                12.dp.toPx(),
                                paint
                            )
                        }
                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Survive. Build. Evolve.",
                fontSize = 16.sp,
                color = Color(0xFFF3EFF7).copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(64.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = Color(0xFFFF1F44).toArgb()
                                setShadowLayer(
                                    20.dp.toPx(),
                                    0f,
                                    0f,
                                    Color(0xFFFF1F44).toArgb()
                                )
                            }
                            val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                            canvas.nativeCanvas.drawRoundRect(
                                rect,
                                12.dp.toPx(),
                                12.dp.toPx(),
                                paint
                            )
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlay)
                    .testTag("play_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3EFF7)
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
            .background(Color(0xFF1C1921), RoundedCornerShape(12.dp))
            .border(
                width = 1.5.dp,
                color = Color(0xFF39FF14),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .size(width = 100.dp, height = 80.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 14.sp, color = Color(0xFF39FF14), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3EFF7))
    }
}
