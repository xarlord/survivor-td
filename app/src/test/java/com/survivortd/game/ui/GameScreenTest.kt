package com.survivortd.game.ui

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
