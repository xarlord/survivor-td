package com.survivortd.game.testing

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [TestGameBridge] — the E2E bridge that lets instrumentation
 * tests inspect the live GameState.
 *
 * [#26][#35]
 */
class TestGameBridgeTest {

    private lateinit var gameState: GameState

    @BeforeEach
    fun setup() {
        gameState = GameState()
        TestGameBridge.unregister()
    }

    @Test
    @DisplayName("Bridge is inactive before register()")
    fun inactiveBeforeRegister() {
        assertFalse(TestGameBridge.isActive)
        assertNull(TestGameBridge.snapshot())
    }

    @Test
    @DisplayName("register() makes bridge active")
    fun registerActivatesBridge() {
        TestGameBridge.register(gameState)
        assertTrue(TestGameBridge.isActive)
        assertNotNull(TestGameBridge.snapshot())
    }

    @Test
    @DisplayName("unregister() deactivates bridge")
    fun unregisterDeactivates() {
        TestGameBridge.register(gameState)
        TestGameBridge.unregister()
        assertFalse(TestGameBridge.isActive)
        assertNull(TestGameBridge.snapshot())
    }

    @Test
    @DisplayName("Snapshot returns valid state for empty state (no player)")
    fun snapshotEmptyState() {
        TestGameBridge.register(gameState)
        val snap = TestGameBridge.snapshot()!!
        assertEquals(0, snap.playerCount)
        assertEquals(0, snap.enemyCount)
        assertEquals(0f, snap.elapsedTime)
    }

    @Test
    @DisplayName("Snapshot correctly counts enemies")
    fun snapshotCountsEnemies() {
        // Add 3 live enemies using spawnEnemy()
        repeat(3) {
            gameState.spawnEnemy(
                x = 100f * it,
                y = 100f * it,
                enemyType = EnemyComponent.EnemyData.ZOMBIE
            )
        }
        // Add a dead enemy (should NOT be counted)
        val deadIdx = gameState.spawnEnemy(
            x = 500f,
            y = 500f,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        gameState.healths[deadIdx].currentHp = 0f  // isDead is computed from currentHp

        TestGameBridge.register(gameState)
        val snap = TestGameBridge.snapshot()!!
        assertEquals(3, snap.enemyCount, "Should count 3 live enemies, not the dead one")
    }

    @Test
    @DisplayName("Snapshot reports player data after spawnPlayer()")
    fun snapshotPlayerData() {
        gameState.spawnPlayer()
        TestGameBridge.register(gameState)
        val snap = TestGameBridge.snapshot()!!
        assertEquals(1, snap.playerCount)
        assertEquals(100f, snap.playerMaxHp, 0.1f)
        assertEquals(100f, snap.playerHp, 0.1f)
        assertFalse(snap.playerIsDead)
    }

    @Test
    @DisplayName("rawState() returns the live GameState")
    fun rawStateReturnsGameState() {
        TestGameBridge.register(gameState)
        val state = TestGameBridge.rawState()
        assertNotNull(state)
        assertEquals(gameState, state)
    }

    @Test
    @DisplayName("Snapshot waits for an atomic game-state update")
    fun snapshotWaitsForAtomicStateUpdate() {
        TestGameBridge.register(gameState)
        val updateStarted = CountDownLatch(1)
        val snapshotEntered = CountDownLatch(1)

        val writer = Thread {
            gameState.withSynchronizedAccess {
                updateStarted.countDown()
                Thread.sleep(200)
            }
        }
        writer.start()
        assertTrue(updateStarted.await(1, TimeUnit.SECONDS))

        val reader = Thread {
            TestGameBridge.snapshot()
            snapshotEntered.countDown()
        }
        reader.start()

        assertFalse(
            snapshotEntered.await(50, TimeUnit.MILLISECONDS),
            "A bridge snapshot must not observe state while an update holds the state lock"
        )
        writer.join(1_000)
        assertTrue(snapshotEntered.await(1, TimeUnit.SECONDS))
        reader.join(1_000)
    }
}
