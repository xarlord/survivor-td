package com.survivortd.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.survivortd.game.ui.GameScreen
import com.survivortd.game.ui.MainMenuScreen

class MainActivity : ComponentActivity() {
    companion object {
        @Volatile
        var skipMenuRequested = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        skipMenuRequested = intent.getBooleanExtra("SKIP_MENU", false)
        setContent {
            SurvivorTDApp()
        }
    }
}

@Composable
fun SurvivorTDApp() {
    var showGame by remember { mutableStateOf(MainActivity.skipMenuRequested) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E1A)
    ) {
        if (showGame) {
            GameScreen(onExit = { showGame = false })
        } else {
            MainMenuScreen(onPlayClick = { showGame = true })
        }
    }
}
