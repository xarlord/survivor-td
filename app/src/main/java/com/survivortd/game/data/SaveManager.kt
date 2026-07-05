package com.survivortd.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.survivortd.game.systems.MetaProgression
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persistent storage for meta-progression and game settings.
 * Uses DataStore Preferences (safe on Main thread via coroutine).
 */
object SaveManager {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "survivor_td"
    )

    // === Meta-progression keys ===
    private val KEY_GOLD = intPreferencesKey("meta_gold")
    private val KEY_MAX_HP = intPreferencesKey("meta_max_hp")
    private val KEY_SPEED = intPreferencesKey("meta_speed")
    private val KEY_DAMAGE = intPreferencesKey("meta_damage")
    private val KEY_PICKUP = intPreferencesKey("meta_pickup")
    private val KEY_LIFE = intPreferencesKey("meta_life")
    private val KEY_XP = intPreferencesKey("meta_xp")
    private val KEY_GOLD_FIND = intPreferencesKey("meta_gold_find")
    private val KEY_TOWER = intPreferencesKey("meta_tower")
    private val KEY_WEAPON = intPreferencesKey("meta_weapon")

    // === Settings keys ===
    private val KEY_SFX_VOLUME = floatPreferencesKey("sfx_volume")
    private val KEY_BGM_VOLUME = floatPreferencesKey("bgm_volume")
    private val KEY_MINIMAP = booleanPreferencesKey("minimap_visible")
    private val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
    private val KEY_FIRST_RUN = booleanPreferencesKey("first_run")

    data class GameSettings(
        val sfxVolume: Float = 0.8f,
        val bgmVolume: Float = 0.6f,
        val minimapVisible: Boolean = true,
        val hapticsEnabled: Boolean = true,
        val isFirstRun: Boolean = true
    )

    fun loadMeta(context: Context): Flow<MetaProgression> {
        return context.dataStore.data.map { prefs ->
            MetaProgression(
                gold = prefs[KEY_GOLD] ?: 0,
                maxHpLevel = prefs[KEY_MAX_HP] ?: 0,
                moveSpeedLevel = prefs[KEY_SPEED] ?: 0,
                damageLevel = prefs[KEY_DAMAGE] ?: 0,
                pickupRangeLevel = prefs[KEY_PICKUP] ?: 0,
                extraLifeLevel = prefs[KEY_LIFE] ?: 0,
                xpGainLevel = prefs[KEY_XP] ?: 0,
                goldFindLevel = prefs[KEY_GOLD_FIND] ?: 0,
                towerDiscountLevel = prefs[KEY_TOWER] ?: 0,
                startingWeaponLevel = prefs[KEY_WEAPON] ?: 0
            )
        }
    }

    suspend fun saveMeta(context: Context, meta: MetaProgression) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GOLD] = meta.gold
            prefs[KEY_MAX_HP] = meta.maxHpLevel
            prefs[KEY_SPEED] = meta.moveSpeedLevel
            prefs[KEY_DAMAGE] = meta.damageLevel
            prefs[KEY_PICKUP] = meta.pickupRangeLevel
            prefs[KEY_LIFE] = meta.extraLifeLevel
            prefs[KEY_XP] = meta.xpGainLevel
            prefs[KEY_GOLD_FIND] = meta.goldFindLevel
            prefs[KEY_TOWER] = meta.towerDiscountLevel
            prefs[KEY_WEAPON] = meta.startingWeaponLevel
        }
    }

    fun loadSettings(context: Context): Flow<GameSettings> {
        return context.dataStore.data.map { prefs ->
            GameSettings(
                sfxVolume = prefs[KEY_SFX_VOLUME] ?: 0.8f,
                bgmVolume = prefs[KEY_BGM_VOLUME] ?: 0.6f,
                minimapVisible = prefs[KEY_MINIMAP] ?: true,
                hapticsEnabled = prefs[KEY_HAPTICS] ?: true,
                isFirstRun = prefs[KEY_FIRST_RUN] ?: true
            )
        }
    }

    suspend fun saveSettings(context: Context, settings: GameSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SFX_VOLUME] = settings.sfxVolume
            prefs[KEY_BGM_VOLUME] = settings.bgmVolume
            prefs[KEY_MINIMAP] = settings.minimapVisible
            prefs[KEY_HAPTICS] = settings.hapticsEnabled
            prefs[KEY_FIRST_RUN] = settings.isFirstRun
        }
    }

    suspend fun markFirstRunComplete(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_RUN] = false
        }
    }
}
