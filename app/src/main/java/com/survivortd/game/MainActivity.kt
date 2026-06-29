package com.survivortd.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.survivortd.game.core.GameEngine
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

    // [#35] GameEngine is created and started SYNCHRONOUSLY in onPlayClick,
    // BEFORE GameScreen composes. This decouples game logic from Compose's
    // composition lifecycle. During E2E tests, Thread.sleep() blocks the main
    // thread, preventing GameScreen's remember{} blocks from executing. By
    // starting the engine in the click handler, the game loop runs on a
    // background thread independent of the UI thread.
    var gameEngine by remember { mutableStateOf<GameEngine?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E1A)
    ) {
        if (showGame && gameEngine != null) {
            GameScreen(
                engine = gameEngine!!,
                onExit = {
                    gameEngine?.dispose()
                    gameEngine = null
                    showGame = false
                }
            )
        } else {
            MainMenuScreen(onPlayClick = {
                // [#35] Create fresh state + engine and start SYNCHRONOUSLY.
                // This runs on the main thread during the click callback, so
                // it executes before Thread.sleep() in E2E tests blocks it.
                val state = GameState()
                val engine = GameEngine(state)
                engine.start()
                gameEngine = engine
                showGame = true
            })
        }
    }
}
