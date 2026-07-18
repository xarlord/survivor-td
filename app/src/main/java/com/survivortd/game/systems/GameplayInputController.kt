package com.survivortd.game.systems

import com.survivortd.game.core.GameState

/**
 * Owns gameplay input and modal transitions.
 *
 * A modal is a hard boundary: it pauses simulation, clears the active pointer,
 * and consumes any pointer sequence that reaches the gameplay layer. Gameplay
 * input is only accepted from the left half of the viewport.
 */
class GameplayInputController(
    private val state: GameState,
    private val joystick: VirtualJoystick
) {
    private enum class Modal {
        NONE,
        TUTORIAL,
        LEVEL_UP
    }

    @Volatile
    private var modal: Modal = Modal.NONE

    @Volatile
    private var disposed = false

    @Synchronized
    fun openTutorial() {
        if (modal == Modal.LEVEL_UP) return
        openModal(Modal.TUTORIAL)
    }

    @Synchronized
    fun dismissTutorial() {
        closeModal(Modal.TUTORIAL)
    }

    @Synchronized
    fun openLevelUp() {
        if (modal == Modal.TUTORIAL) return
        openModal(Modal.LEVEL_UP)
    }

    @Synchronized
    fun dismissLevelUp() {
        closeModal(Modal.LEVEL_UP)
    }

    /** True while tutorial or level-up owns the input surface. */
    fun isModalBlocking(): Boolean = modal != Modal.NONE

    /** Used by the game loop as the simulation gate. */
    fun canAdvanceSimulation(): Boolean =
        !disposed && modal == Modal.NONE && !state.isPaused && !state.isGameOver

    fun runSimulationIfAllowed(dt: Float, update: (Float) -> Unit) {
        if (canAdvanceSimulation()) update(dt)
    }

    /**
     * Returns true when the event is consumed by the gameplay surface. Events
     * blocked by a modal are consumed without ever reaching the joystick.
     */
    @Synchronized
    fun onPointerDown(x: Float, y: Float, screenWidth: Float, pointerId: Long): Boolean {
        if (disposed || isModalBlocking()) return true
        if (joystick.active()) return true
        if (x >= screenWidth * LEFT_HALF_FRACTION) return false
        joystick.onTouchDown(x, y, pointerId = pointerId, screenWidth = screenWidth)
        return true
    }

    @Synchronized
    fun onPointerMove(x: Float, y: Float, pointerId: Long): Boolean {
        if (disposed || isModalBlocking()) return true
        if (!joystick.active()) return false
        joystick.onTouchMove(x, y, pointerId = pointerId)
        return true
    }

    @Synchronized
    fun onPointerUp(pointerId: Long): Boolean {
        if (disposed || isModalBlocking()) return true
        if (!joystick.active()) return false
        joystick.onTouchUp(pointerId = pointerId)
        return true
    }

    /** A cancelled gesture invalidates the whole pointer sequence. */
    @Synchronized
    fun onPointerCancel(): Boolean {
        resetInput()
        return true
    }

    /** Used when a run is restarted or a pause/modal boundary is entered. */
    @Synchronized
    fun restart() {
        disposed = false
        modal = Modal.NONE
        state.isPaused = false
        resetInput()
    }

    /** Releases all input ownership when the composition is disposed. */
    @Synchronized
    fun dispose() {
        disposed = true
        modal = Modal.NONE
        resetInput()
    }

    private fun openModal(next: Modal) {
        disposed = false
        modal = next
        state.isPaused = true
        resetInput()
    }

    private fun closeModal(expected: Modal) {
        if (modal != expected) return
        modal = Modal.NONE
        state.isPaused = false
        resetInput()
    }

    private fun resetInput() {
        joystick.reset()
    }

    companion object {
        private const val LEFT_HALF_FRACTION = 0.5f
    }
}
