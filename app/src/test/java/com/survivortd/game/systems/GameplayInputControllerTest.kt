package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameplayInputControllerTest {

    private lateinit var state: GameState
    private lateinit var joystick: VirtualJoystick
    private lateinit var controller: GameplayInputController

    @BeforeEach
    fun setUp() {
        state = GameState()
        joystick = VirtualJoystick(state, deadzone = 0.15f, maxRadius = 100f)
        controller = GameplayInputController(state, joystick)
    }

    @Test
    fun tutorialModalFreezesSimulationUntilExplicitDismissal() {
        controller.openTutorial()
        val before = state.elapsedSeconds

        repeat(60) { controller.runSimulationIfAllowed(1f / 60f) { state.elapsedSeconds += it } }

        assertTrue(controller.isModalBlocking())
        assertFalse(controller.canAdvanceSimulation())
        assertEquals(before, state.elapsedSeconds)

        controller.dismissTutorial()
        controller.runSimulationIfAllowed(1f / 60f) { state.elapsedSeconds += it }

        assertTrue(controller.canAdvanceSimulation())
        assertNotEquals(before, state.elapsedSeconds)
    }

    @Test
    fun levelUpModalFreezesSimulationUntilUpgradeDismissal() {
        controller.openLevelUp()
        val before = state.elapsedSeconds

        repeat(60) { controller.runSimulationIfAllowed(1f / 60f) { state.elapsedSeconds += it } }

        assertTrue(controller.isModalBlocking())
        assertFalse(controller.canAdvanceSimulation())
        assertEquals(before, state.elapsedSeconds)

        controller.dismissLevelUp()
        controller.runSimulationIfAllowed(1f / 60f) { state.elapsedSeconds += it }

        assertTrue(controller.canAdvanceSimulation())
        assertNotEquals(before, state.elapsedSeconds)
    }

    @Test
    fun blockingModalConsumesTouchAndDragWithoutReachingGameplay() {
        controller.openLevelUp()

        assertTrue(controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 11L))
        assertTrue(controller.onPointerMove(x = 300f, y = 100f, pointerId = 11L))
        assertTrue(controller.onPointerUp(pointerId = 11L))

        assertFalse(joystick.active())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }

    @Test
    fun dismissalDoesNotLeakTheModalPointerSequence() {
        controller.openTutorial()
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 7L)
        controller.dismissTutorial()

        controller.onPointerMove(x = 300f, y = 100f, pointerId = 7L)
        controller.onPointerUp(pointerId = 7L)

        assertFalse(joystick.active())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)

        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 8L)
        assertTrue(joystick.active())
        assertEquals(8L, joystick.activePointerId())
    }

    @Test
    fun joystickStartsOnlyInLeftHalfOfScreen() {
        assertFalse(controller.onPointerDown(x = 500f, y = 300f, screenWidth = 1000f, pointerId = 1L))
        assertFalse(joystick.active())

        assertTrue(controller.onPointerDown(x = 499f, y = 300f, screenWidth = 1000f, pointerId = 2L))
        assertTrue(joystick.active())
        assertEquals(2L, joystick.activePointerId())
    }

    @Test
    fun upwardAndDownwardInputProduceOppositeSignedYIntent() {
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 1L)
        controller.onPointerMove(x = 100f, y = 200f, pointerId = 1L)
        val upwardY = state.joystickY
        controller.onPointerUp(pointerId = 1L)

        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 2L)
        controller.onPointerMove(x = 100f, y = 400f, pointerId = 2L)
        val downwardY = state.joystickY

        assertTrue(upwardY < 0f, "Upward drag should produce negative Y intent, got $upwardY")
        assertTrue(downwardY > 0f, "Downward drag should produce positive Y intent, got $downwardY")
    }

    @Test
    fun pointerCancelResetsVectorAndOwner() {
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 4L)
        controller.onPointerMove(x = 250f, y = 200f, pointerId = 4L)
        assertTrue(joystick.active())
        assertNotEquals(0f, state.joystickX)

        controller.onPointerCancel()

        assertFalse(joystick.active())
        assertEquals(-1L, joystick.activePointerId())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }

    @Test
    fun restartModalEntryAndDisposalResetJoystickLifecycle() {
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 1L)
        controller.onPointerMove(x = 250f, y = 300f, pointerId = 1L)
        controller.restart()
        assertJoystickReset()

        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 2L)
        controller.onPointerMove(x = 250f, y = 300f, pointerId = 2L)
        controller.openLevelUp()
        assertJoystickReset()

        controller.dismissLevelUp()
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 3L)
        controller.onPointerMove(x = 250f, y = 300f, pointerId = 3L)
        controller.dispose()
        assertJoystickReset()
    }

    @Test
    fun foreignSecondPointerCannotTakeControlOrLatchMovement() {
        controller.onPointerDown(x = 100f, y = 300f, screenWidth = 1000f, pointerId = 10L)
        controller.onPointerMove(x = 200f, y = 300f, pointerId = 10L)
        val ownerVector = state.joystickX

        assertTrue(controller.onPointerDown(x = 110f, y = 300f, screenWidth = 1000f, pointerId = 20L))
        controller.onPointerMove(x = 400f, y = 50f, pointerId = 20L)
        controller.onPointerUp(pointerId = 20L)

        assertEquals(10L, joystick.activePointerId())
        assertEquals(ownerVector, state.joystickX)
        assertEquals(0f, state.joystickY)

        controller.onPointerMove(x = 100f, y = 200f, pointerId = 10L)
        assertTrue(state.joystickY < 0f)
    }

    private fun assertJoystickReset() {
        assertFalse(joystick.active())
        assertEquals(-1L, joystick.activePointerId())
        assertEquals(0f, state.joystickX)
        assertEquals(0f, state.joystickY)
    }
}
