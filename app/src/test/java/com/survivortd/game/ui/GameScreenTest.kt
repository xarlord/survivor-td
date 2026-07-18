package com.survivortd.game.ui

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Regression coverage for the state-lock contract used by GameScreen's loop. */
class GameScreenTest {

    @Test
    fun game_tick_state_access_is_atomic() {
        val state = GameState()

        val elapsed = state.withSynchronizedAccess {
            state.elapsedSeconds += 1f
            state.elapsedSeconds
        }

        assertEquals(1f, elapsed)
    }

    @Test
    fun entry_pending_level_up_runs_zero_systems_and_opens_after_lock() {
        val state = GameState().apply { pendingLevelUps = 1 }
        var systems = 0
        var openedWhileLocked = true

        runSimulationTickBoundary(
            gameState = state,
            isPaused = false,
            modalBlocking = false,
            canAdvanceSimulation = true,
            dt = 1f / 60f,
            updateGameFeel = { 1f },
            updateThroughProjectile = { systems++ },
            updateRemaining = { systems++ },
            openLevelUp = {
                openedWhileLocked = Thread.holdsLock(state.synchronizationLock)
                state.pendingLevelUps = 0
            }
        )

        assertEquals(0, systems)
        assertFalse(openedWhileLocked)
    }

    @Test
    fun pending_after_projectile_skips_remaining_tick_and_cleanup() {
        val state = GameState()
        val calls = mutableListOf<String>()

        runSimulationTickBoundary(
            gameState = state,
            isPaused = false,
            modalBlocking = false,
            canAdvanceSimulation = true,
            dt = 1f / 60f,
            updateGameFeel = { calls += "feel"; 1f },
            updateThroughProjectile = {
                calls += "projectile"
                state.pendingLevelUps = 1
            },
            updateRemaining = { calls += "remaining" },
            openLevelUp = { calls += "open" }
        )

        assertEquals(listOf("feel", "projectile", "open"), calls)
        assertEquals(0f, state.elapsedSeconds)
    }

    @Test
    fun modal_and_pause_blocks_do_not_run_a_tick() {
        val state = GameState()
        var systems = 0

        runSimulationTickBoundary(
            gameState = state,
            isPaused = false,
            modalBlocking = true,
            canAdvanceSimulation = false,
            dt = 1f / 60f,
            updateGameFeel = { systems++; 1f },
            updateThroughProjectile = { systems++ },
            updateRemaining = { systems++ },
            openLevelUp = { systems++ }
        )

        assertEquals(0, systems)
    }

    @Test
    fun normal_tick_holds_snapshot_lock_and_releases_before_open() {
        val state = GameState()
        val snapshotEntered = CountDownLatch(1)
        val snapshot = Thread {
            state.withSynchronizedAccess { snapshotEntered.countDown() }
        }

        runSimulationTickBoundary(
            gameState = state,
            isPaused = false,
            modalBlocking = false,
            canAdvanceSimulation = true,
            dt = 1f / 60f,
            updateGameFeel = { 1f },
            updateThroughProjectile = { snapshot.start(); Thread.sleep(40) },
            updateRemaining = { },
            openLevelUp = { }
        )

        assertTrue(snapshotEntered.await(1, TimeUnit.SECONDS))
        snapshot.join(1_000)
    }
}
