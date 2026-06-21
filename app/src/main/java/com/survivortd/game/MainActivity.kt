package com.survivortd.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.survivortd.game.ui.MainMenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurvivorTDApp()
        }
    }
}

@Composable
fun SurvivorTDApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E1A)
    ) {
        MainMenuScreen()
    }
}
