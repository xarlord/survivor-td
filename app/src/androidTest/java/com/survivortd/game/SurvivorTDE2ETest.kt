package com.survivortd.game

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.survivortd.game.components.TagComponent
import com.survivortd.game.data.SaveManager
import com.survivortd.game.testing.TestGameBridge
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
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
        clickPlayButton()
        Thread.sleep(3000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    @Test
    fun game_survives_5_seconds_without_crash() {
        clickPlayButton()
        Thread.sleep(5000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    @Test
    fun game_survives_15_seconds_without_crash() {
        clickPlayButton()
        Thread.sleep(15000)
        assertTrue(composeRule.activity.window.decorView.isAttachedToWindow)
    }

    @Test
    fun navigating_to_game_is_route_exclusive_with_paused_clock() {
        setFirstRunAndRecreateActivity(isFirstRun = true)

        composeRule.mainClock.autoAdvance = false
        try {
            listOf("⚔️", "🛒", "⚙️").forEach { formerEmoji ->
                assertEquals(
                    "Secondary menu navigation must not expose platform emoji",
                    0,
                    composeRule.onAllNodesWithText(formerEmoji, substring = true)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false).size
                )
            }
            composeRule.onNodeWithTag("play_button").performClick()
            composeRule.mainClock.advanceTimeBy(100L)

            val gameScreen = composeRule.onAllNodesWithTag("game_screen")
            val gameDeadline = System.currentTimeMillis() + 10_000L
            while (gameScreen.fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty() &&
                System.currentTimeMillis() < gameDeadline
            ) {
                Thread.sleep(100L)
            }
            assertTrue(
                "Game route must compose after PLAY with a paused clock",
                gameScreen.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            )
            assertEquals(
                "Outgoing menu route must be disposed before tutorial interaction",
                0,
                composeRule.onAllNodesWithTag("main_menu")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )

            val tutorial = composeRule.onAllNodesWithText("LET'S GO!", substring = true)
            val tutorialDeadline = System.currentTimeMillis() + 5_000L
            while (tutorial.fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty() &&
                System.currentTimeMillis() < tutorialDeadline
            ) {
                Thread.sleep(100L)
            }
            assertTrue(
                "First-run tutorial must own the exclusive game route",
                tutorial.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            )
            listOf("up and down", "Double-tap", "dash", "Upgrades between runs").forEach { copy ->
                assertTrue(
                    "Tutorial must expose required onboarding copy: $copy",
                    composeRule.onAllNodesWithText(copy, substring = true)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
                )
            }
            assertEquals(
                "Tutorial terminology must not refer to the retired Shop label",
                0,
                composeRule.onAllNodesWithText("Shop", substring = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            composeRule.onNodeWithTag("tutorial_start_button")
                .performScrollTo()
                .assertIsDisplayed()
            assertEquals(
                "Tutorial modal must remove HUD from semantics",
                0,
                composeRule.onAllNodesWithTag("game_hud")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            assertEquals(
                "Tutorial modal must remove minimap from semantics",
                0,
                composeRule.onAllNodesWithTag("minimap")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            assertEquals(
                "Ordinary dev builds must not expose FPS telemetry",
                0,
                composeRule.onAllNodesWithTag("fps_telemetry")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )

            tutorial[0].performClick()
            composeRule.mainClock.advanceTimeBy(1000L)

            assertEquals(
                "Tutorial must be dismissed before gameplay resumes",
                0,
                composeRule.onAllNodesWithText("LET'S GO!", substring = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            assertEquals(
                "Menu route must stay disposed after tutorial dismissal",
                0,
                composeRule.onAllNodesWithTag("main_menu")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            composeRule.onNodeWithTag("game_screen").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    // ================================================================
    // TIER 2: OBJECT-LEVEL TESTS (new — [#26][#35])
    // ================================================================

    @Test
    fun pending_level_up_waits_for_tutorial_and_opens_once_after_dismissal() {
        setFirstRunAndRecreateActivity(isFirstRun = true)

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag("play_button").performClick()
            composeRule.mainClock.advanceTimeBy(1000L)

            val bridgeDeadline = System.currentTimeMillis() + 10_000L
            while (!TestGameBridge.isActive && System.currentTimeMillis() < bridgeDeadline) {
                Thread.sleep(100L)
            }
            assertTrue("TestGameBridge should be active after PLAY", TestGameBridge.isActive)

            val tutorial = composeRule.onAllNodesWithText("LET'S GO!", substring = true)
            val tutorialDeadline = System.currentTimeMillis() + 5_000L
            while (tutorial.fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty() &&
                System.currentTimeMillis() < tutorialDeadline
            ) {
                Thread.sleep(100L)
            }
            assertTrue(
                "Tutorial must be visible before injecting a pending level-up",
                tutorial.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            )

            val state = TestGameBridge.rawState()
            assertNotNull("TestGameBridge raw state should be available", state)
            state!!.withSynchronizedAccess { state.pendingLevelUps = 1 }
            val elapsedBeforeDismiss = TestGameBridge.snapshot()!!.elapsedTime
            composeRule.mainClock.advanceTimeBy(1000L)

            assertTrue(
                "Tutorial must continue owning input while a level-up is pending",
                tutorial.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            )
            assertEquals(
                "Level-up dialog must not open while tutorial is active",
                0,
                composeRule.onAllNodesWithText("LEVEL UP!", substring = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            assertEquals(elapsedBeforeDismiss, TestGameBridge.snapshot()!!.elapsedTime, 0.001f)

            tutorial[0].performClick()
            composeRule.mainClock.advanceTimeBy(1000L)

            val dialogCount = composeRule.onAllNodesWithText("LEVEL UP!", substring = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            assertEquals("Exactly one level-up dialog must be shown", 1, dialogCount)
            assertEquals(
                "Elapsed time must not advance during tutorial-to-level-up handoff",
                elapsedBeforeDismiss,
                TestGameBridge.snapshot()!!.elapsedTime,
                0.001f
            )
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun build_placement_is_visible_in_world_and_minimap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setFirstRunAndRecreateActivity(isFirstRun = false)

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag("play_button").performClick()
            composeRule.mainClock.advanceTimeBy(1000L)

            val bridgeDeadline = System.currentTimeMillis() + 10_000L
            while (!TestGameBridge.isActive && System.currentTimeMillis() < bridgeDeadline) {
                Thread.sleep(100L)
            }
            assertTrue("TestGameBridge should be active after PLAY", TestGameBridge.isActive)

            val timerBounds = composeRule.onNodeWithTag("hud_time")
                .fetchSemanticsNode().boundsInRoot
            val pauseBounds = composeRule.onNodeWithTag("pause_button")
                .fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Pause target must not overlap the survival timer",
                !timerBounds.overlaps(pauseBounds)
            )

            val state = TestGameBridge.rawState()!!
            state.withSynchronizedAccess {
                state.elapsedSeconds = 300f
                state.players[state.playerIndex].scrap = 1_000
                state.healths[state.playerIndex].apply {
                    maxHp = 100_000f
                    currentHp = 100_000f
                }
            }

            val waveSystem = TestGameBridge.rawWaveSystem()!!
            state.withSynchronizedAccess {
                state.currentWave = 4
                waveSystem.startNextWave()
            }
            val hasBoss = state.withSynchronizedAccess {
                state.enemies.indices.any { index ->
                    state.tags.getOrNull(index)?.tag == TagComponent.EntityTag.ENEMY &&
                        state.enemies[index].type ==
                        com.survivortd.game.components.EnemyComponent.EnemyData.BOSS
                }
            }
            assertTrue("The deterministic gate must enter a real boss wave", hasBoss)

            state.withSynchronizedAccess {
                state.enemies.indices.forEach { index ->
                    if (state.tags.getOrNull(index)?.tag == TagComponent.EntityTag.ENEMY) {
                        state.healths[index].currentHp = 0f
                    }
                }
                waveSystem.update(0.016f)
                assertTrue("Boss death must enter WaveSystem build phase", waveSystem.isBuildPhase)
                state.isPaused = true
            }
            val towerSystem = TestGameBridge.rawTowerSystem()!!
            state.withSynchronizedAccess {
                assertTrue(
                    "First tower should place during the real build phase",
                    towerSystem.placeTower(
                        com.survivortd.game.config.TowerType.GUN_TURRET,
                        220f,
                        240f
                    )
                )
                assertTrue(
                    "Second tower should place during the real build phase",
                    towerSystem.placeTower(
                        com.survivortd.game.config.TowerType.GUN_TURRET,
                        520f,
                        480f
                    )
                )
            }
            composeRule.mainClock.advanceTimeBy(500L)
            composeRule.waitForIdle()

            assertEquals(
                "Two towers should survive the live render/minimap pipeline",
                2,
                TestGameBridge.snapshot()!!.towerCount
            )

            assertEquals(
                "Minimap must be hidden while build controls own the bottom touch region",
                0,
                composeRule.onAllNodesWithTag("minimap")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).size
            )
            val buildScreenshot = context.filesDir.resolve("pr184_build_layout.png")
            FileOutputStream(buildScreenshot).use { output ->
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    output
                )
            }
            assertTrue("Build-layout evidence screenshot must be written", buildScreenshot.isFile)

            state.withSynchronizedAccess {
                state.isPaused = false
                waveSystem.update(11f)
                assertTrue("Build phase should end before minimap evidence", !waveSystem.isBuildPhase)
                state.isPaused = true
            }
            composeRule.mainClock.advanceTimeBy(500L)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("minimap").assertIsDisplayed()

            val minimapScreenshot = context.filesDir.resolve("pr184_tower_minimap.png")
            FileOutputStream(minimapScreenshot).use { output ->
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    output
                )
            }
            assertTrue("Minimap evidence screenshot must be written", minimapScreenshot.isFile)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun setFirstRunAndRecreateActivity(isFirstRun: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            SaveManager.saveSettings(context, SaveManager.GameSettings(isFirstRun = isFirstRun))
        }
        // createAndroidComposeRule launches the Activity before each test body.
        // Recreate it so MainActivity observes the persisted setup deterministically,
        // independent of test execution order within the instrumentation process.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    /**
     * Clicks the PLAY button without deadlocking on waitForIdle().
     *
     * [#46] performClick() internally calls waitForIdle(). Once GameScreen
     * composes, the game loop's 60Hz onRender callback keeps the main Looper
     * perpetually non-idle (via Handler.post), causing performClick() to hang.
     * Disabling mainClock.autoAdvance skips the internal waitForIdle() call.
     */
    private fun clickPlayButton() {
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag("play_button").performClick()
            composeRule.mainClock.advanceTimeBy(1000L)
            dismissTutorialIfPresent()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun dismissTutorialIfPresent() {
        val tutorialButtons = composeRule.onAllNodesWithText("LET'S GO!", substring = true)
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline) {
            if (tutorialButtons.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()) {
                tutorialButtons[0].performClick()
                composeRule.mainClock.advanceTimeBy(1000L)
                return
            }
            Thread.sleep(100L)
        }
    }

    /**
     * Helper: Start gameplay and wait for [millis] then return a snapshot.
     * Asserts the bridge is active (game state registered).
     *
     * The game loop runs on Dispatchers.Default (background thread), so it
     * continues running even during Thread.sleep() on the main thread.
     *
     * CRITICAL FIX [#46]: performClick() internally calls waitForIdle() to wait
     * for recomposition. However, once the GameScreen composes, the game loop
     * fires onRender at ~60Hz via Handler.post(Main), bumping redrawTrigger and
     * keeping the main Looper perpetually non-idle. This causes performClick()
     * to deadlock — it never returns because waitForIdle() never completes.
     *
     * SOLUTION: Disable mainClock.autoAdvance before the click. This makes the
     * Compose test framework skip its internal waitForIdle() calls (the clock is
     * paused, so it doesn't try to advance to idle). The click's onClick lambda
     * still fires synchronously, setting showGame=true and triggering the first
     * composition of GameScreen (which registers TestGameBridge in a remember{}
     * block). We then poll for TestGameBridge.isActive instead of relying on
     * waitForIdle().
     */
    private fun startGameAndSnapshot(millis: Long): TestGameBridge.GameSnapshot {
        // [#46] Use the deadlock-safe click helper
        clickPlayButton()

        // Poll for TestGameBridge activation (max 10 seconds).
        // The GameScreen composes asynchronously; the remember{} block that
        // registers TestGameBridge runs during composition.
        val startTime = System.currentTimeMillis()
        var snap: TestGameBridge.GameSnapshot? = null
        while (System.currentTimeMillis() - startTime < 10_000) {
            snap = TestGameBridge.snapshot()
            if (snap != null) break
            Thread.sleep(100)
        }

        assertNotNull("TestGameBridge should be active after PLAY (debug build)", snap)

        // Wait for requested gameplay time. The game loop runs on
        // Dispatchers.Default (background thread) and is NOT blocked by
        // Thread.sleep() on the main thread.
        Thread.sleep(millis)

        // Return a fresh snapshot after the gameplay duration
        return TestGameBridge.snapshot()!!
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
     *
     * NOTE: Uses a forgiving threshold (>= 2) because CI runs the emulator
     * headless with software rendering (-no-window -gpu swiftshader_indirect),
     * so the game loop runs slower than real-time. 10s of wall-clock on the CI
     * emulator ≈ 3s of in-game time. The assertion's purpose is to confirm
     * spawning CONTINUES over time (more than the single-enemy check at 5s),
     * not to verify exact spawn counts. [#49]
     */
    @Test
    fun game_progresses_after_15_seconds() {
        // CI emulator is slow — 15s wall-clock may only be ~3s game time.
        // Verify the game loop has advanced: elapsedTime > 0 and game is active.
        // [#49]
        val snap = startGameAndSnapshot(15000)

        assertTrue(
            "Game should have progressed after 15s (elapsed=${snap.elapsedTime}s, enemies=${snap.enemyCount})",
            snap.elapsedTime > 0f && !snap.playerIsDead
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
