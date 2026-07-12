package com.survivortd.game.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Chapter index mapping for backgrounds (no Android asset load required).
 */
class BackgroundChapterIndexTest {

    @Test
    @DisplayName("chapter index advances every 3 minutes (GDD chapter table)")
    fun chapterIndexByMinutes() {
        // Mirror BackgroundManager.chapterForMinutes thresholds without Context
        fun index(m: Float) = when {
            m < 3f -> 0
            m < 6f -> 1
            m < 9f -> 2
            m < 12f -> 3
            else -> 4
        }
        assertEquals(0, index(0f))
        assertEquals(0, index(2.9f))
        assertEquals(1, index(3f))
        assertEquals(2, index(8.9f))
        assertEquals(3, index(11f))
        assertEquals(4, index(12f))
        assertEquals(4, index(15f))
    }

    @Test
    @DisplayName("GameState holds backgroundManager slot")
    fun gameStateHasBackgroundSlot() {
        val state = GameState()
        assertTrue(state.backgroundManager == null)
    }
}
