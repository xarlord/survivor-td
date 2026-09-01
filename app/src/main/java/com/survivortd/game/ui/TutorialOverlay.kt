package com.survivortd.game.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-run tutorial overlay — shown once, then disabled via SaveManager.
 * Teaches the player basic controls and game mechanics.
 */
@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAlpha by rememberInfiniteTransition(
        label = "pulse"
    ).animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E2E))
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("tutorial_content")
        ) {
            Text(
                text = "SURVIVOR TD",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "How to Play",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            TutorialContent.steps.forEach { step -> TutorialStep(step) }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676).copy(alpha = pulseAlpha))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 16.dp)
                    .testTag("tutorial_start_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LET'S GO!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0E1A)
                )
            }
        }
    }
}

@Composable
private fun TutorialStep(step: TutorialStepSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("tutorial_step_${step.id.name.lowercase()}"),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        AppIconView(
            icon = step.icon,
            tint = Color(0xFF00E676),
            size = 24.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${step.title}: ${step.instruction}",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Left,
            modifier = Modifier.weight(1f)
        )
    }
}
