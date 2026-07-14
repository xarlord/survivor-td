package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import kotlin.math.sqrt

/**
 * Virtual joystick — touch → normalized movement vector on [GameState].
 *
 * Floating stick:
 * - Touch down: anchor at finger
 * - Drag: vector = direction * magnitude (0..1), deadzone applied
 * - Up: zero vector
 *
 * [maxRadius] is in **the same pixel space as Compose pointer positions**.
 * Prefer [setMaxRadius] from the canvas size (density-aware).
 *
 * Dash: double-tap within [DASH_DOUBLE_TAP_MS] on touch-down sets
 * [dashRequested]; MovementSystem consumes it once.
 */
class VirtualJoystick(
    private val state: GameState,
    private val deadzone: Float = 0.15f,
    maxRadius: Float = 160f
) {
    private var maxRadius: Float = maxRadius.coerceAtLeast(48f)

    private var anchorX: Float = -1f
    private var anchorY: Float = -1f
    private var isActive: Boolean = false
    private var activePointerId: Long = -1L

    private var lastTapTimeMs: Long = 0L
    /** One-shot flag for dash; cleared by [consumeDashRequest]. */
    private var dashRequested: Boolean = false

    fun setMaxRadius(px: Float) {
        if (px > 32f) maxRadius = px
    }

    fun maxRadius(): Float = maxRadius

    fun onTouchDown(x: Float, y: Float, pointerId: Long = 0L, nowMs: Long = System.currentTimeMillis()) {
        // Double-tap dash detection (touch-down only — never per physics tick)
        if (lastTapTimeMs > 0L && nowMs - lastTapTimeMs < DASH_DOUBLE_TAP_MS) {
            dashRequested = true
            lastTapTimeMs = 0L
        } else {
            lastTapTimeMs = nowMs
        }

        anchorX = x
        anchorY = y
        isActive = true
        activePointerId = pointerId
        state.joystickX = 0f
        state.joystickY = 0f
    }

    fun onTouchMove(x: Float, y: Float, pointerId: Long = activePointerId) {
        if (!isActive) return
        if (activePointerId >= 0 && pointerId >= 0 && pointerId != activePointerId) return

        val dx = x - anchorX
        val dy = y - anchorY
        val dist = sqrt(dx * dx + dy * dy)
        val deadPx = deadzone * maxRadius

        if (dist < deadPx) {
            state.joystickX = 0f
            state.joystickY = 0f
            return
        }

        // Analog magnitude: 0 at deadzone edge → 1 at maxRadius
        val usable = (dist - deadPx) / (maxRadius - deadPx).coerceAtLeast(1f)
        val mag = usable.coerceIn(0f, 1f)
        state.joystickX = (dx / dist) * mag
        state.joystickY = (dy / dist) * mag
    }

    fun onTouchUp(pointerId: Long = activePointerId) {
        if (activePointerId >= 0 && pointerId >= 0 && pointerId != activePointerId) return
        isActive = false
        activePointerId = -1L
        anchorX = -1f
        anchorY = -1f
        state.joystickX = 0f
        state.joystickY = 0f
    }

    fun active(): Boolean = isActive

    fun activePointerId(): Long = activePointerId

    fun consumeDashRequest(): Boolean {
        if (!dashRequested) return false
        dashRequested = false
        return true
    }

    /** Test helper / debug */
    fun isDashRequested(): Boolean = dashRequested

    fun anchor(): Pair<Float, Float> = Pair(anchorX, anchorY)

    fun knobPosition(): Pair<Float, Float> {
        if (!isActive || anchorX < 0f) return Pair(anchorX, anchorY)
        val dx = state.joystickX * maxRadius
        val dy = state.joystickY * maxRadius
        return Pair(anchorX + dx, anchorY + dy)
    }

    companion object {
        const val DASH_DOUBLE_TAP_MS = 280L
        /** Fraction of min(canvas W,H) used as stick radius. */
        const val RADIUS_SCREEN_FRACTION = 0.16f
    }
}
