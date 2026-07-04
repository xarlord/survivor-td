package com.survivortd.game.systems

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.survivortd.game.data.HeroId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Save Manager — persists hero unlock state and selection via DataStore.
 *
 * GDD §3.3 — hero unlocks are permanent meta-progression.
 */
object SaveManager {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "survivor_td_save")

    // === Keys ===
    private val KEY_UNLOCKED_HEROES = stringPreferencesKey("unlocked_heroes")
    private val KEY_SELECTED_HERO = stringPreferencesKey("selected_hero")

    // === Default values ===
    private val DEFAULT_UNLOCKED = setOf(HeroId.DEFAULT.name)
    private const val DEFAULT_SELECTED = "COMMANDER"

    // === Public API ===

    /**
     * Load the set of unlocked hero names.
     */
    suspend fun loadUnlockedHeroes(context: Context): Set<String> {
        return try {
            val prefs = context.dataStore.data.first()
            val raw = prefs[KEY_UNLOCKED_HEROES] ?: ""
            parseHeroSet(raw).ifEmpty { DEFAULT_UNLOCKED }
        } catch (e: Exception) {
            DEFAULT_UNLOCKED
        }
    }

    /**
     * Save the set of unlocked hero names.
     */
    suspend fun saveUnlockedHeroes(context: Context, heroes: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UNLOCKED_HEROES] = heroes.joinToString(",")
        }
    }

    /**
     * Load the selected hero name.
     */
    suspend fun loadSelectedHero(context: Context): String {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[KEY_SELECTED_HERO] ?: DEFAULT_SELECTED
        } catch (e: Exception) {
            DEFAULT_SELECTED
        }
    }

    /**
     * Save the selected hero name.
     */
    suspend fun saveSelectedHero(context: Context, heroId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_HERO] = heroId
        }
    }

    /**
     * Observe unlocked heroes as a Flow.
     */
    fun observeUnlockedHeroes(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_UNLOCKED_HEROES] ?: ""
            parseHeroSet(raw).ifEmpty { DEFAULT_UNLOCKED }
        }
    }

    /**
     * Observe selected hero as a Flow.
     */
    fun observeSelectedHero(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SELECTED_HERO] ?: DEFAULT_SELECTED
        }
    }

    // === Helpers ===

    private fun parseHeroSet(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
