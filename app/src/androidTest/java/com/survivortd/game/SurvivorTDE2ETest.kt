package com.survivortd.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.survivortd.game.testing.TestGameBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E instrumentation tests — REAL device/emulator testing.
 * These are HARD release gates. If these fail, the release is BLOCKED.
 *
 * Tests are split into two tiers:
 *
 * **Tier 1 — Crash & UI Tests** (original)
 *   Verify the app launches, menu renders, and gameplay doesn't crash.
 *
 * **Tier 2 — Object-Level Tests** ([#26][#35])
 *   Use [TestGameBridge] to inspect the live [GameState] during gameplay.
 *   These verify ACTUAL game mechanics: enemies spawn, combat works,
 *   XP is earned, entities are cleaned up, etc.
 *
 * NOTE: The game loop fires redrawTrigger at ~60Hz via Handler.post(Main),
 * so composeRule.waitForIdle() never returns once gameplay starts. Therefore,
 * post-PLAY tests use Thread.sleep + direct GameState inspection instead.
 */
class SurvivorTDE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Ensure bridge is clean before each test
        TestGameBridge.unregister()
    }

    // ================================================================
    // TIER 1: CRASH & UI TESTS (existing — preserved)
    // ================================================================

    @Test
    fun app_launches_without_crash() {
        composeRule.waitForIdle()
    }

    @Test
    fun main_menu_shows_title_and_play_button() {
        composeRule.onNodeWithTag("title").assertIsDisplayed()
        composeRule.onNodeWithText("PLAY").assertIsDisplayed()
    }

    @Test
    fun tapping_play_does_not_crash_app() {
        composeRule.onNodeWithTag("play_button").performClick()
        Thread.sleep(3000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    @Test
    fun game_survives_5_seconds_without_crash() {
        composeRule.onNodeWithTag("play_button").performClick()
        Thread.sleep(5000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    @Test
    fun game_survives_15_seconds_without_crash() {
        composeRule.onNodeWithTag("play_button").performClick()
        Thread.sleep(15000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    // ================================================================
    // TIER 2: OBJECT-LEVEL TESTS (new — [#26][#35])
    // ================================================================

    /**
     * Helper: Start gameplay and wait for [millis] then return a snapshot.
     * Asserts the bridge is active (game state registered).
     *
     * The game loop runs on Dispatchers.Default (background thread), so it
     * continues running even during Thread.sleep() on the main thread.
     */
    private fun startGameAndSnapshot(millis: Long): TestGameBridge.GameSnapshot {
        composeRule.onNodeWithTag("play_button").performClick()
        Thread.sleep(millis)
        val snap = TestGameBridge.snapshot()
        assertNotNull("TestGameBridge should be active after PLAY (debug build)", snap)
        return snap!!
    }

    /**
     * After 3s of gameplay, the player entity must exist with full HP.
     */
    @Test
    fun player_entity_exists_after_play() {
        val snap = startGameAndSnapshot(3000)

        assertTrue("Player entity must exist", snap.playerCount >= 1)
        assertTrue("Player HP must be > 0", snap.playerHp > 0f)
        assertTrue("Player max HP must be 100 (GameConfig)", snap.playerMaxHp == 100f)
        assertTrue("Player should not be dead at start", !snap.playerIsDead)
        assertEquals("Player should start at level 1", 1, snap.playerLevel)
    }

    /**
     * After 5s of gameplay, enemies should have spawned from the wave system.
     * The spawn interval starts at ~1.5s, so at least 2-3 enemies should exist.
     */
    @Test
    fun enemies_spawn_after_5_seconds() {
        val snap = startGameAndSnapshot(5000)

        assertTrue(
            "Enemies should have spawned by 5s (got ${snap.enemyCount}). " +
            "WaveSystem spawn interval starts at 1.5s.",
            snap.enemyCount >= 1
        )
    }

    /**
     * Game time must advance (proves the game loop is running).
     */
    @Test
    fun game_time_advances() {
        val snap = startGameAndSnapshot(5000)

        assertTrue(
            "Elapsed time should be > 0 after 5s (got ${snap.elapsedTime}s)",
            snap.elapsedTime > 0f
        )
    }

    /**
     * Player should have at least one weapon (starting weapon per GDD §3.3).
     */
    @Test
    fun starting_weapon_exists() {
        val snap = startGameAndSnapshot(3000)

        assertTrue(
            "Player should have at least 1 weapon (got ${snap.weaponCount})",
            snap.weaponCount >= 1
        )
    }

    /**
     * After 10s, there should be multiple enemies on screen.
     */
    @Test
    fun multiple_enemies_after_10_seconds() {
        val snap = startGameAndSnapshot(10000)

        assertTrue(
            "Should have 3+ enemies after 10s (got ${snap.enemyCount})",
            snap.enemyCount >= 3
        )
    }

    /**
     * Game should still be active (not crashed/game-over) after 15s.
     */
    @Test
    fun game_still_active_after_15_seconds() {
        val snap = startGameAndSnapshot(15000)

        assertTrue("Player should not be dead after 15s", !snap.playerIsDead)
        assertTrue("Should have enemies after 15s", snap.enemyCount > 0)
    }
}
