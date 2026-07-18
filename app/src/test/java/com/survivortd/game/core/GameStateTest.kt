package com.survivortd.game.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Regression coverage for the shared game-state synchronization contract. */
class GameStateTest {

    @Test
    fun withSynchronizedAccess_returns_the_block_result() {
        val state = GameState()

        val result = state.withSynchronizedAccess { 42 }

        assertEquals(42, result)
    }

    @Test
    fun withSynchronizedAccess_serializes_readers_and_writers() {
        val state = GameState()
        val writerStarted = CountDownLatch(1)
        val readerEntered = CountDownLatch(1)

        val writer = Thread {
            state.withSynchronizedAccess {
                writerStarted.countDown()
                Thread.sleep(150)
            }
        }
        writer.start()
        assertTrue(writerStarted.await(1, TimeUnit.SECONDS))

        val reader = Thread {
            state.withSynchronizedAccess { readerEntered.countDown() }
        }
        reader.start()

        assertFalse(readerEntered.await(25, TimeUnit.MILLISECONDS))
        writer.join(1_000)
        assertTrue(readerEntered.await(1, TimeUnit.SECONDS))
        reader.join(1_000)
    }
}
