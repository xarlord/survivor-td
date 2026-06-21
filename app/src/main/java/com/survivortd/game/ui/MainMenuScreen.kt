package com.survivortd.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

/**
 * Main menu screen. Entry point for the game.
 *
 * TODO: Add chapter select, hero select, upgrades, settings navigation.
 */
@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .testTag("main_menu"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "SURVIVOR TD",
                fontSize = 42.sp,
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
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0A0E1A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("play_button")
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
