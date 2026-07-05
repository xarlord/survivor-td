package com.survivortd.game.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for ObjectPool — acquire/release cycle, sizing, and reset behavior.
 */
class ObjectPoolTest {

    data class TestItem(
        var value: Int = 0,
        var name: String = "default"
    ) {
        fun resetForPool() {
            value = 0
            name = "default"
        }
    }

    @Nested
    @DisplayName("Acquire/Release Cycle")
    inner class AcquireReleaseTest {

        @Test
        fun `pool pre-allocates initial size items`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 10
            )
            assertEquals(10, pool.size, "Pool should start with initialSize items")
        }

        @Test
        fun `acquire reduces pool size`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 10
            )
            pool.acquire()
            assertEquals(9, pool.size, "Pool should have one fewer item after acquire")
        }

        @Test
        fun `release increases pool size`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 10
            )
            val item = pool.acquire()
            assertEquals(9, pool.size)
            pool.release(item)
            assertEquals(10, pool.size, "Pool should have item back after release")
        }

        @Test
        fun `acquire resets item before returning`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 10
            )
            val item = pool.acquire()
            item.value = 42
            item.name = "modified"
            pool.release(item)
            val reacquired = pool.acquire()
            assertEquals(0, reacquired.value, "Acquired item should be reset")
            assertEquals("default", reacquired.name, "Acquired item name should be reset")
        }

        @Test
        fun `acquire from empty pool creates new item`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 2,
                maxSize = 5
            )
            pool.acquire()
            pool.acquire()
            assertEquals(0, pool.size)
            val item = pool.acquire()
            assertNotNull(item, "Should create new item when pool is empty")
            assertEquals(0, pool.size, "Pool should still be empty after creating new")
        }
    }

    @Nested
    @DisplayName("Max Size")
    inner class MaxSizeTest {

        @Test
        fun `release does not exceed max size`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 2,
                maxSize = 4
            )
            assertEquals(2, pool.size)
            // Release more items than maxSize
            repeat(10) { pool.release(TestItem(value = it)) }
            assertEquals(4, pool.size, "Pool should not exceed maxSize")
        }
    }

    @Nested
    @DisplayName("Full Cycle Stress")
    inner class StressTest {

        @Test
        fun `many acquire-release cycles maintain correctness`() {
            val pool = ObjectPool(
                factory = { TestItem() },
                reset = { it.resetForPool() },
                initialSize = 5,
                maxSize = 20
            )
            repeat(100) { i ->
                val item = pool.acquire()
                item.value = i
                // Only release odd items to test mixed acquire/create patterns
                if (i % 2 == 1) {
                    pool.release(item)
                }
            }
            assertTrue(pool.size <= 20, "Pool should never exceed maxSize")
            // 50 odd items were released back, minus any that exceeded maxSize
            assertTrue(pool.size > 0, "Pool should have some items from releases")
        }
    }
}
