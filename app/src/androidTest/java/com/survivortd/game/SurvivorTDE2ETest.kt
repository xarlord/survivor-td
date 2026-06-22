package com.survivortd.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * E2E instrumentation tests — REAL device/emulator testing.
 * These are HARD release gates. If these fail, the release is BLOCKED.
 *
 * NOTE: The game loop fires redrawTrigger at ~60Hz via Handler.post(Main),
 * so composeRule.waitForIdle() never returns once gameplay starts. Therefore,
 * post-PLAY tests use Thread.sleep + process-alive checks instead of
 * assertIsDisplayed (which implicitly waits for idle).
 */
class SurvivorTDE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
        // If we get here without crash, the game launched successfully.
        // The game loop makes waitForIdle() unreliable, so we verify via
        // the Compose view being attached to the window.
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
}
