package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

class VirtualJoystickTest {

    private lateinit var state: GameState
    private lateinit var joystick: VirtualJoystick

    @BeforeEach
    fun setup() {
        state = GameState()
        joystick = VirtualJoystick(state, deadzone = 0.15f, maxRadius = 100f)
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
        joystick.onTouchMove(280f, 400f) // 80px right of 100 radius
        assertTrue(state.joystickX > 0.3f, "Joystick X should be solidly positive, got ${state.joystickX}")
        assertTrue(abs(state.joystickY) < 0.1f, "Joystick Y should be ~0")
    }

    @Test
    @DisplayName("Drag within deadzone produces no movement")
    fun deadzoneSuppressesSmallDrag() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(205f, 405f) // ~7px < 15 deadzone
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
        joystick.onTouchMove(500f, 400f)
        val mag = sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)
        assertTrue(mag <= 1.01f, "Magnitude should be clamped to 1.0, got $mag")
    }

    @Test
    @DisplayName("Diagonal drag produces proportional X and Y")
    fun diagonalDrag() {
        joystick.onTouchDown(200f, 400f)
        joystick.onTouchMove(280f, 480f)
        val mag = sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)
        assertTrue(mag > 0.4f, "Diagonal should produce significant magnitude")
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

    @Test
    @DisplayName("Analog: half-radius drag yields partial magnitude")
    fun analogPartialMagnitude() {
        joystick.setMaxRadius(100f)
        joystick.onTouchDown(0f, 0f)
        // deadzone 15px; usable span 85px; drag to 15+42.5=57.5 → ~0.5 mag
        joystick.onTouchMove(58f, 0f)
        val mag = sqrt(state.joystickX * state.joystickX + state.joystickY * state.joystickY)
        assertTrue(mag in 0.35f..0.75f, "Expected mid magnitude, got $mag")
    }

    @Test
    @DisplayName("Double-tap touch-down requests dash once")
    fun doubleTapDash() {
        val t0 = 1_000_000L
        joystick.onTouchDown(10f, 10f, pointerId = 1, nowMs = t0)
        joystick.onTouchMove(10f, -90f, pointerId = 1)
        assertFalse(joystick.isDashRequested())
        joystick.onTouchUp(1)
        joystick.onTouchDown(12f, 12f, pointerId = 1, nowMs = t0 + 120)
        val request = requireNotNull(joystick.consumeDashRequest()) { "double-tap should request a dash" }
        assertEquals(0f, request.directionX, 0.01f)
        assertEquals(-1f, request.directionY, 0.01f)
        assertNull(joystick.consumeDashRequest(), "dash is one-shot")
    }

    @Test
    @DisplayName("Ignores move from a different pointer id")
    fun ignoresOtherPointer() {
        joystick.onTouchDown(200f, 400f, pointerId = 7)
        joystick.onTouchMove(300f, 400f, pointerId = 99)
        assertEquals(0f, state.joystickX, 0.01f)
        joystick.onTouchMove(300f, 400f, pointerId = 7)
        assertTrue(state.joystickX > 0f)
    }
}
