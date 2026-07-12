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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.survivortd.game.ui.theme.StdColors

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
                        StdColors.GradientTop,
                        StdColors.GradientMid,
                        StdColors.GradientBottom
                    )
                )
            )
            .statusBarsPadding()
            .testTag("main_menu")
    ) {
        // Ambient orbs (premium depth without heavy art)
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = StdColors.Cyan.copy(alpha = 0.07f),
                        radius = size.minDimension * 0.42f,
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * 0.15f,
                            size.height * 0.22f
                        )
                    )
                    drawCircle(
                        color = StdColors.Coral.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.38f,
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * 0.88f,
                            size.height * 0.35f
                        )
                    )
                    drawCircle(
                        color = StdColors.Violet.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * 0.55f,
                            size.height * 0.78f
                        )
                    )
                }
        )

        // Currency chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(StdColors.SurfaceGlass)
                .border(1.dp, StdColors.Border, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("◆", fontSize = 12.sp, color = StdColors.Amber)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$gold",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = StdColors.AmberSoft,
                letterSpacing = 0.5.sp,
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
                text = "SURVIVOR",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 8.sp,
                color = StdColors.CyanBright.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TD",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = StdColors.TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .testTag("title")
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = StdColors.Cyan.copy(alpha = 0.12f).toArgb()
                                setShadowLayer(
                                    36.dp.toPx(),
                                    0f,
                                    0f,
                                    StdColors.Cyan.toArgb()
                                )
                            }
                            val rect = android.graphics.RectF(
                                -8f, -4f, size.width + 8f, size.height + 8f
                            )
                            canvas.nativeCanvas.drawRoundRect(rect, 16.dp.toPx(), 16.dp.toPx(), paint)
                        }
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Survive  ·  Build  ·  Evolve",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                color = StdColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(52.dp))

            // Primary CTA — gradient coral → amber edge, premium
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(StdColors.CoralDeep, StdColors.Coral, Color(0xFFFB7185))
                        )
                    )
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                color = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    28.dp.toPx(),
                                    0f,
                                    10.dp.toPx(),
                                    StdColors.Coral.copy(alpha = 0.55f).toArgb()
                                )
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                android.graphics.RectF(0f, 0f, size.width, size.height),
                                16.dp.toPx(),
                                16.dp.toPx(),
                                paint
                            )
                        }
                    }
                    .clickable(onClick = onPlay)
                    .testTag("play_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = StdColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StdColors.SurfaceGlass)
            .border(1.dp, StdColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp)
    ) {
        Text(
            text = icon,
            fontSize = 22.sp,
            color = StdColors.CyanBright
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = StdColors.TextPrimary,
            maxLines = MenuCardLayout.MAX_LABEL_LINES,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )
    }
}
