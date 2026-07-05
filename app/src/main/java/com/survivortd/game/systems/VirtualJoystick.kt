package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Virtual joystick system — converts touch input into a normalized movement vector.
 *
 * Uses a floating joystick pattern:
 * - Touch down anywhere on the left half of the screen → joystick anchor appears
 * - Drag away from anchor → movement vector is computed
 * - Touch up → joystick released, movement stops
 *
 * The joystick vector is stored in GameState (joystickX, joystickY) as a
 * normalized direction vector * magnitude (0..1).
 */
class VirtualJoystick(
    private val state: GameState,
    private val deadzone: Float = 0.12f,
    private val maxRadius: Float = 120f  // max drag distance in pixels
) {
    // Joystick anchor position (where the finger first touched)
    private var anchorX: Float = -1f
    private var anchorY: Float = -1f
    private var isActive: Boolean = false

    // Dash double-tap detection
    private var lastTapTime = 0L
    private val DASH_DOUBLE_TAP_MS = 300L

    fun checkDash(): Boolean {
        val now = System.currentTimeMillis()
        if (lastTapTime > 0L && now - lastTapTime < DASH_DOUBLE_TAP_MS) {
            lastTapTime = 0L
            return true
        }
        lastTapTime = now
        return false
    }

    /**
     * Process a touch input event.
     * Call from GameLoop's handleInput callback.
     */
    fun onTouchDown(x: Float, y: Float) {
        anchorX = x
        anchorY = y
        isActive = true
    }

    fun onTouchMove(x: Float, y: Float) {
        if (!isActive) return

        val dx = x - anchorX
        val dy = y - anchorY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < deadzone * maxRadius) {
            state.joystickX = 0f
            state.joystickY = 0f
            return
        }

        // Normalize and scale by drag distance / maxRadius
        val normalizedMag = (dist / maxRadius).coerceIn(0f, 1f)
        state.joystickX = (dx / dist) * normalizedMag
        state.joystickY = (dy / dist) * normalizedMag
    }

    fun onTouchUp() {
        isActive = false
        anchorX = -1f
        anchorY = -1f
        state.joystickX = 0f
        state.joystickY = 0f
    }

    /**
     * Whether the joystick is currently active (finger down).
     */
    fun active(): Boolean = isActive

    /**
     * Current anchor position for rendering the joystick base.
     */
    fun anchor(): Pair<Float, Float> = Pair(anchorX, anchorY)

    /**
     * Current knob position for rendering.
     */
    fun knobPosition(): Pair<Float, Float> {
        if (!isActive) return Pair(anchorX, anchorY)
        val dx = state.joystickX * maxRadius
        val dy = state.joystickY * maxRadius
        return Pair(anchorX + dx, anchorY + dy)
    }
}
