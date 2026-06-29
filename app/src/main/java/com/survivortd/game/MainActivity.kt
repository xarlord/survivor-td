package com.survivortd.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.survivortd.game.core.GameState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.survivortd.game.ui.GameScreen
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
    var showGame by remember { mutableStateOf(false) }
    // [#29] Hold GameState at the app level so we can register it with
    // TestGameBridge synchronously in onPlayClick — before GameScreen's
    // composition runs. This avoids the Compose test clock timing issue
    // where remember{} inside GameScreen never executes during Thread.sleep().
    val gameState = remember { GameState() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E1A)
    ) {
        if (showGame) {
            GameScreen(gameState = gameState, onExit = { showGame = false })
        } else {
            MainMenuScreen(onPlayClick = {
                // [#29] Register TestGameBridge SYNCHRONOUSLY on the click
                // callback — runs immediately when PLAY is tapped, before
                // the GameScreen composition is processed.
                gameState.reset()
                gameState.spawnPlayer()
                com.survivortd.game.testing.TestGameBridge.register(gameState)
                showGame = true
            })
        }
    }
}
