package com.survivortd.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survivortd.game.systems.MetaProgression
import com.survivortd.game.ui.GameScreen
import com.survivortd.game.ui.MainMenuScreen
import com.survivortd.game.ui.SettingsScreen
import com.survivortd.game.ui.ShopScreen
import com.survivortd.game.ui.theme.SurvivorTDTheme
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

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

enum class Screen { MENU, SHOP, SETTINGS, GAME }

@Composable
fun SurvivorTDApp() {
    val navController = rememberNavController()

    SurvivorTDTheme {
        NavHost(navController = navController, startDestination = Screen.MENU.name) {
            composable(Screen.MENU.name) {
                val meta = rememberLoadMetaProgression()
                MainMenuScreen(
                    gold = meta.gold,
                    onPlay = { navController.navigate(Screen.GAME.name) },
                    onHeroes = { },
                    onShop = { navController.navigate(Screen.SHOP.name) },
                    onSettings = { navController.navigate(Screen.SETTINGS.name) }
                )
            }
            composable(Screen.SHOP.name) {
                val meta = rememberLoadMetaProgression()
                ShopScreen(
                    meta = meta,
                    onBuy = { },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SETTINGS.name) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.GAME.name) {
                GameScreen(onExit = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun rememberLoadMetaProgression(): MetaProgression {
    val context = LocalContext.current
    var meta by remember { mutableStateOf(MetaProgression()) }

    LaunchedEffect(Unit) {
        val loaded = flow {
            emit(MetaProgression.load(context.filesDir.absolutePath + "/meta_progression.json"))
        }.first()
        meta = loaded
    }

    return meta
}
