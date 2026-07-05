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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survivortd.game.data.HeroId
import com.survivortd.game.data.HeroUnlock
import com.survivortd.game.systems.MetaProgression
import com.survivortd.game.systems.SaveManager
import com.survivortd.game.ui.GameScreen
import com.survivortd.game.ui.HeroSelectScreen
import com.survivortd.game.ui.MainMenuScreen
import com.survivortd.game.ui.SettingsScreen
import com.survivortd.game.ui.ShopScreen
import com.survivortd.game.ui.theme.SurvivorTDTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

enum class Screen { MENU, HERO_SELECT, SHOP, SETTINGS, GAME }

@Composable
fun SurvivorTDApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var unlockedHeroes by remember { mutableStateOf(setOf(HeroId.DEFAULT.name)) }
    var selectedHero by remember { mutableStateOf(HeroId.DEFAULT.name) }
    var playerGold by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Load saved state on first composition
    LaunchedEffect(Unit) {
        unlockedHeroes = SaveManager.loadUnlockedHeroes(context)
        selectedHero = SaveManager.loadSelectedHero(context)
        val meta = MetaProgression.load(getMetaProgressionPath(context))
        playerGold = meta.gold
    }

    // Reload meta-progression whenever returning to a screen (re-reads JSON)
    fun refreshMeta() {
        val meta = MetaProgression.load(getMetaProgressionPath(context))
        playerGold = meta.gold
    }

    SurvivorTDTheme {
        NavHost(navController = navController, startDestination = Screen.MENU.name) {
            composable(Screen.MENU.name) {
                // Refresh gold from JSON every time menu is composed
                LaunchedEffect(Unit) { refreshMeta() }
                MainMenuScreen(
                    gold = playerGold,
                    onPlay = { navController.navigate(Screen.GAME.name) },
                    onHeroes = { navController.navigate(Screen.HERO_SELECT.name) },
                    onShop = { navController.navigate(Screen.SHOP.name) },
                    onSettings = { navController.navigate(Screen.SETTINGS.name) }
                )
            }
            composable(Screen.HERO_SELECT.name) {
                HeroSelectScreen(
                    unlockedHeroes = unlockedHeroes,
                    selectedHero = selectedHero,
                    playerGold = playerGold,
                    onHeroSelected = { heroName ->
                        selectedHero = heroName
                        scope.launch { SaveManager.saveSelectedHero(context, heroName) }
                        navController.popBackStack()
                    },
                    onUnlockHero = { heroId ->
                        unlockedHeroes = unlockedHeroes + heroId.name
                        val meta = MetaProgression.load(getMetaProgressionPath(context))
                        val unlockCost = HeroUnlock.forHero(heroId).unlockCost
                        meta.gold -= unlockCost
                        meta.save(getMetaProgressionPath(context))
                        playerGold = meta.gold
                        scope.launch {
                            SaveManager.saveUnlockedHeroes(context, unlockedHeroes)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SHOP.name) {
                val shopMeta = remember { mutableStateOf(MetaProgression.load(getMetaProgressionPath(context))) }
                LaunchedEffect(Unit) {
                    refreshMeta()
                    shopMeta.value = MetaProgression.load(getMetaProgressionPath(context))
                }
                LaunchedEffect(shopMeta.value) {
                    playerGold = shopMeta.value.gold
                }
                ShopScreen(
                    meta = shopMeta.value,
                    onBuy = { item ->
                        if (shopMeta.value.buy(item)) {
                            shopMeta.value.save(getMetaProgressionPath(context))
                            playerGold = shopMeta.value.gold
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SETTINGS.name) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.GAME.name) {
                GameScreen(heroId = selectedHero, onExit = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun rememberLoadMetaProgression(): MetaProgression {
    val context = LocalContext.current
    var meta by remember { mutableStateOf(MetaProgression()) }

    LaunchedEffect(Unit) {
        val loaded = kotlinx.coroutines.flow.flow {
            emit(MetaProgression.load(context.filesDir.absolutePath + "/meta_progression.json"))
        }.first()
        meta = loaded
    }

    return meta
}

/** Path for MetaProgression JSON save file. */
internal fun getMetaProgressionPath(context: android.content.Context): String {
    return context.filesDir.resolve("meta_progression.json").absolutePath
}
