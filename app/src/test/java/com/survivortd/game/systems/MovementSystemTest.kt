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
    @DisplayName("Enemy moves towards player")
    fun enemyChasesPlayer() {
        val enemyId = state.spawnEnemy(
            x = 0f,
            y = 0f,
            enemyType = com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE
        )
        val startPos = state.positions[enemyId].x
        movement.update(1f)
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
    @DisplayName("Elapsed time increases")
    fun elapsedTimeAdvances() {
        assertEquals(0f, state.elapsedSeconds)
        movement.update(0.5f)
        assertEquals(0.5f, state.elapsedSeconds, 0.01f)
        movement.update(0.5f)
        assertEquals(1f, state.elapsedSeconds, 0.01f)
    }

    @Test
    @DisplayName("Camera follows player")
    fun cameraFollowsPlayer() {
        state.joystickX = 1f
        movement.update(1f)
        assertEquals(state.positions[state.playerIndex].x, state.cameraX, 1f)
    }
}
