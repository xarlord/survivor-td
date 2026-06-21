package com.survivortd.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import org.junit.Rule
import org.junit.Test

/**
 * E2E instrumentation tests — REAL device/emulator testing.
 * These are HARD release gates. If these fail, the release is BLOCKED.
 */
class SurvivorTDE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_without_crash() {
        // If we get here, the app launched without crashing
        composeRule.waitForIdle()
    }

    @Test
    fun main_menu_shows_title_and_play_button() {
        composeRule.onNodeWithTag("title")
            .assertIsDisplayed()
        composeRule.onNodeWithText("PLAY")
            .assertIsDisplayed()
    }

    @Test
    fun tapping_play_starts_game_screen() {
        composeRule.onNodeWithTag("play_button")
            .performClick()

        // Game screen should appear
        composeRule.waitUntilAtLeastOneExists(
            timeoutMillis = 5000
        ) {
            composeRule.onAllNodesWithTag("game_screen").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("game_screen")
            .assertIsDisplayed()
    }

    @Test
    fun game_screen_remains_visible_after_5_seconds() {
        // Start the game
        composeRule.onNodeWithTag("play_button").performClick()

        composeRule.waitUntilAtLeastOneExists(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("game_screen").fetchSemanticsNodes().isNotEmpty()
        }

        // Wait 5 seconds — game should still be running (not crashed)
        Thread.sleep(5000)

        composeRule.onNodeWithTag("game_screen")
            .assertIsDisplayed()
    }

    @Test
    fun app_survives_15_seconds_of_gameplay_without_crash() {
        // Start the game
        composeRule.onNodeWithTag("play_button").performClick()

        composeRule.waitUntilAtLeastOneExists(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("game_screen").fetchSemanticsNodes().isNotEmpty()
        }

        // Simulate 15 seconds of gameplay
        Thread.sleep(15000)

        // App should still be alive — game screen still displayed
        composeRule.onNodeWithTag("game_screen")
            .assertIsDisplayed()
    }
}
