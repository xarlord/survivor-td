package com.survivortd.game.ui

import com.survivortd.game.components.TagComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.testing.TestGameBridge
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for the TestGameBridge registration logic used by [GameScreen].
 *
 * [GameScreen] registers/unregisters the live [GameState] with [TestGameBridge]
 * so that E2E instrumentation tests can inspect game objects. This was
 * previously done inside LaunchedEffect(Unit) (async, dispatched to Main),
 * which caused E2E tests to fail because the registration coroutine had not
 * executed by assertion time. The fix moved registration into a synchronous
 * remember{} block. These tests validate the bridge contract directly.
 *
 * [#28][#29]
 */
class GameScreenTest {

    @BeforeEach
    fun resetBridge() {
        TestGameBridge.unregister()
    }

    @AfterEach
    fun cleanup() {
        TestGameBridge.unregister()
    }

    @Test
    @DisplayName("TestGameBridge is inactive before registration")
    fun bridgeInactiveBeforeRegister() {
        assertFalse(TestGameBridge.isActive)
        assertNull(TestGameBridge.snapshot())
    }

    @Test
    @DisplayName("TestGameBridge becomes active after register() with a spawned player")
    fun bridgeActiveAfterRegisterWithPlayer() {
        val state = GameState()
        state.spawnPlayer()
        TestGameBridge.register(state)

        assertTrue(TestGameBridge.isActive)
        val snap = TestGameBridge.snapshot()
        assertNotNull(snap, "Snapshot must be non-null after registration")
    }

    @Test
    @DisplayName("Snapshot reports the spawned player entity with correct stats")
    fun snapshotReportsPlayer() {
        val state = GameState()
        state.spawnPlayer()
        TestGameBridge.register(state)

        val snap = TestGameBridge.snapshot()!!
        assertEquals(1, snap.playerCount, "Exactly one player should exist")
        assertTrue(snap.playerHp > 0f, "Player HP must be positive")
        assertFalse(snap.playerIsDead, "Player must not be dead on spawn")
        assertEquals(1, snap.playerLevel, "Player starts at level 1")
    }

    @Test
    @DisplayName("Snapshot reflects enemies after they are spawned")
    fun snapshotReportsEnemies() {
        val state = GameState()
        state.spawnPlayer()
        state.spawnEnemy(100f, 100f, com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)
        state.spawnEnemy(200f, 200f, com.survivortd.game.components.EnemyComponent.EnemyData.RUNNER)
        TestGameBridge.register(state)

        val snap = TestGameBridge.snapshot()!!
        assertEquals(2, snap.enemyCount, "Two enemies should be reported")
    }

    @Test
    @DisplayName("Snapshot reflects player position at world center on spawn")
    fun snapshotReportsPlayerCenterPosition() {
        val state = GameState()
        state.spawnPlayer()
        TestGameBridge.register(state)

        val snap = TestGameBridge.snapshot()!!
        val centerX = com.survivortd.game.config.GameConfig.WORLD_WIDTH / 2f
        val centerY = com.survivortd.game.config.GameConfig.WORLD_HEIGHT / 2f
        assertEquals(centerX, snap.playerPositionX, 0.01f)
        assertEquals(centerY, snap.playerPositionY, 0.01f)
    }

    @Test
    @DisplayName("unregister() clears the bridge and snapshots return null")
    fun unregisterClearsBridge() {
        val state = GameState()
        state.spawnPlayer()
        TestGameBridge.register(state)
        assertTrue(TestGameBridge.isActive)

        TestGameBridge.unregister()
        assertFalse(TestGameBridge.isActive)
        assertNull(TestGameBridge.snapshot())
    }

    @Test
    @DisplayName("rawState() returns the live GameState after registration")
    fun rawStateReturnsLiveState() {
        val state = GameState()
        state.spawnPlayer()
        TestGameBridge.register(state)

        val raw = TestGameBridge.rawState()
        assertNotNull(raw)
        assertTrue(raw!!.players.isNotEmpty())
    }
}
