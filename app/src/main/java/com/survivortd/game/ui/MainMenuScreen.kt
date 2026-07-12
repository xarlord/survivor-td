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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0F18),
                        Color(0xFF0D0B10),
                        Color(0xFF0A1210)
                    )
                )
            )
            .statusBarsPadding()
            .testTag("main_menu")
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .background(Color(0xEE1C1921), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFA832FF), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("💰", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
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
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Text(
                text = "SURVIVOR TD",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF3EFF7),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("title")
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = Color(0xFFFF1F44).copy(alpha = 0.18f).toArgb()
                                setShadowLayer(
                                    28.dp.toPx(),
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Survive. Build. Evolve.",
                fontSize = 15.sp,
                color = Color(0xFFF3EFF7).copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(60.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = Color(0xFFFF1F44).toArgb()
                                setShadowLayer(
                                    22.dp.toPx(),
                                    0f,
                                    0f,
                                    Color(0xFFFF1F44).toArgb()
                                )
                            }
                            val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                            canvas.nativeCanvas.drawRoundRect(
                                rect,
                                14.dp.toPx(),
                                14.dp.toPx(),
                                paint
                            )
                        }
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPlay)
                    .testTag("play_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3EFF7)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuButton(
                    label = "Heroes",
                    onClick = onHeroes,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("heroes_button")
                )
                MenuButton(
                    label = "Upgrades",
                    onClick = onShop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_button")
                )
                MenuButton(
                    label = "Settings",
                    onClick = onSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_button")
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = MenuCardLayout.shortIcon(label)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(92.dp)
            .background(Color(0xEE1C1921), RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFF39FF14), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = icon,
            fontSize = MenuCardLayout.ICON_SP.sp,
            color = Color(0xFF39FF14),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = MenuCardLayout.LABEL_SP.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF3EFF7),
            maxLines = MenuCardLayout.MAX_LABEL_LINES,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
