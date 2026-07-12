package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MovementSystemTest {

    private lateinit var state: GameState
    private lateinit var movement: MovementSystem

    @BeforeEach
    fun setUp() {
        state = GameState()
        state.spawnPlayer()
        movement = MovementSystem(state)
    }

    @Test
    @DisplayName("Player stays at center with no input")
    fun playerNoMovement() {
        val startX = state.positions[state.playerIndex].x
        val startY = state.positions[state.playerIndex].y
        movement.update(0.5f)
        assertEquals(startX, state.positions[state.playerIndex].x, 1f)
        assertEquals(startY, state.positions[state.playerIndex].y, 1f)
    }

    @Test
    @DisplayName("Player moves right when joystick is right")
    fun playerMovesRight() {
        state.joystickX = 1f
        state.joystickY = 0f
        val startX = state.positions[state.playerIndex].x
        movement.update(1f)
        val endX = state.positions[state.playerIndex].x
        assertTrue(endX > startX, "Player should move right: start=$startX, end=$endX")
    }

    @Test
    @DisplayName("Player moves at correct speed")
    fun playerSpeed() {
        state.joystickX = 1f
        movement.update(1f)
        val delta = state.positions[state.playerIndex].x - (GameConfig.WORLD_WIDTH / 2f)
        // Player speed is 220 px/s, should move ~220 px in 1s
        assertEquals(220f, delta, 5f, "Player should move ~220px in 1s")
    }

    @Test
    @DisplayName("Player is clamped to world bounds")
    fun playerClampedToWorld() {
        // Place player at edge
        state.positions[state.playerIndex].x = GameConfig.WORLD_WIDTH
        state.joystickX = 1f  // Push further right
        movement.update(1f)
        assertEquals(GameConfig.WORLD_WIDTH, state.positions[state.playerIndex].x, 0.1f)
    }

    @Test
    @DisplayName("Enemy moves towards player when AI + movement both run")
    fun enemyChasesPlayer() {
        val enemyId = state.spawnEnemy(
            x = 0f,
            y = 0f,
            enemyType = com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE
        )
        // In the new architecture, EnemyAISystem sets velocity, then MovementSystem applies it
        val ai = EnemyAISystem(state)
        ai.update(0.016f)      // Sets zombie velocity toward player
        val startPos = state.positions[enemyId].x
        movement.update(0.016f) // Integrates velocity → position
        val endPos = state.positions[enemyId].x
        assertTrue(endPos > startPos, "Enemy should move towards player (positive X)")
    }

    @Test
    @DisplayName("No movement when paused")
    fun noMovementWhenPaused() {
        state.isPaused = true
        state.joystickX = 1f
        val startX = state.positions[state.playerIndex].x
        movement.update(1f)
        assertEquals(startX, state.positions[state.playerIndex].x, 0.1f)
    }

    @Test
    @DisplayName("Elapsed time is NOT advanced by MovementSystem")
    fun elapsedTimeNotAdvancedByMovementSystem() {
        // Game loop (GameScreen) is the sole authority for elapsedSeconds.
        // MovementSystem must NOT increment it — double-increment was a bug (#75).
        assertEquals(0f, state.elapsedSeconds)
        movement.update(0.5f)
        assertEquals(0f, state.elapsedSeconds, 0.01f)
        movement.update(0.5f)
        assertEquals(0f, state.elapsedSeconds, 0.01f)
    }

    @Test
    @DisplayName("Camera follows player")
    fun cameraFollowsPlayer() {
        state.joystickX = 1f
        movement.update(1f)
        assertEquals(state.positions[state.playerIndex].x, state.cameraX, 1f)
    }

    @Test
    @DisplayName("Analog stick magnitude scales speed (#162)")
    fun analogScalesSpeed() {
        val start = state.positions[state.playerIndex].x
        state.joystickX = 0.5f
        state.joystickY = 0f
        movement.update(1f)
        val halfDelta = state.positions[state.playerIndex].x - start
        // ~110 px at half stick (220 * 0.5)
        assertEquals(110f, halfDelta, 8f, "half stick should move ~half speed")
    }

    @Test
    @DisplayName("Dash request from joystick applies dash speed once")
    fun dashFromJoystick() {
        val joy = VirtualJoystick(state)
        val mov = MovementSystem(state, joy)
        joy.onTouchDown(0f, 0f, nowMs = 1000L)
        joy.onTouchUp()
        joy.onTouchDown(0f, 0f, nowMs = 1100L)
        assertTrue(joy.isDashRequested())
        state.joystickX = 1f
        val start = state.positions[state.playerIndex].x
        mov.update(0.1f)
        val delta = state.positions[state.playerIndex].x - start
        // dash 400 * 0.1 = 40
        assertTrue(delta > 30f, "dash should move farther than walk: delta=$delta")
        assertFalse(joy.isDashRequested(), "dash consumed")
    }
}
