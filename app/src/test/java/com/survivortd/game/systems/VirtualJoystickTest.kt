package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for VirtualJoystick — touch-to-vector conversion, deadzone, release.
 */
class VirtualJoystickTest {

    private lateinit var state: GameState
    private lateinit var joystick: VirtualJoystick

    @BeforeEach
    fun setup() {
        state = GameState()
        joystick = VirtualJoystick(state, deadzone = 0.12f, maxRadius = 120f)
    }

    @Test
    @DisplayName("Joystick starts inactive")
    fun startsInactive() {
        assertFalse(joystick.active())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }

    @Test
    @DisplayName("Touch down activates joystick")
    fun touchDownActivates() {
        joystick.onTouchDown(200f, 400f)
        assertTrue(joystick.active())
    }

    @Test
    @DisplayName("Drag beyond deadzone produces movement vector")
    fun dragProducesVector() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(260f, 400f)  // 60px right = 0.5 magnitude
        assertTrue(state.joystickX > 0f, "Joystick X should be positive (dragging right)")
        assertTrue(abs(state.joystickY) < 0.1f, "Joystick Y should be ~0")
    }

    @Test
    @DisplayName("Drag within deadzone produces no movement")
    fun deadzoneSuppressesSmallDrag() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(205f, 405f)  // 7px = under deadzone (0.12 * 120 = 14.4)
        assertEquals(0f, state.joystickX, 0.01f)
        assertEquals(0f, state.joystickY, 0.01f)
    }

    @Test
    @DisplayName("Release zeroes movement vector")
    fun releaseZeroesVector() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(300f, 400f)
        assertTrue(state.joystickX > 0f)
        joystick.onTouchUp()
        assertFalse(joystick.active())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }

    @Test
    @DisplayName("Vector magnitude is clamped to 1.0")
    fun magnitudeClamped() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(500f, 400f)  // 300px drag, much larger than maxRadius (120)
        val mag = kotlin.math.sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)
        assertTrue(mag <= 1.01f, "Magnitude should be clamped to 1.0, got $mag")
    }

    @Test
    @DisplayName("Diagonal drag produces proportional X and Y")
    fun diagonalDrag() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(260f, 460f)  // 60px right, 60px down
        val mag = kotlin.math.sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)
        assertTrue(mag > 0.5f, "Diagonal should produce significant magnitude")
        assertTrue(abs(state.joystickX - state.joystickY) < 0.15f,
            "X and Y should be roughly equal for 45-degree drag")
    }

    @Test
    @DisplayName("Touch move without touch down is ignored")
    fun moveWithoutDownIgnored() {
        joystick.onTouchMove(300f, 400f)
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }

    private fun abs(f: Float) = kotlin.math.abs(f)
}
