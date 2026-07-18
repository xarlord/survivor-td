package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

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
        controller.tryAcquireLevelUp()
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
    fun tutorial_active_pending_level_up_is_rejected_without_transition_or_reset() {
        controller.openTutorial()
        state.pendingLevelUps = 1
        state.joystickX = 0.6f
        state.joystickY = -0.4f

        val result = controller.tryAcquireLevelUp()

        assertEquals(GameplayInputController.LevelUpAcquisition.REJECTED_BY_TUTORIAL, result)
        assertEquals(0.6f, state.joystickX)
        assertEquals(-0.4f, state.joystickY)
        controller.dismissTutorial()
        assertTrue(controller.canAdvanceSimulation())
    }

    @Test
    fun pending_level_up_after_tutorial_dismissal_is_claimed_once_without_simulation_handoff() {
        controller.openTutorial()
        state.pendingLevelUps = 1
        val before = state.elapsedSeconds

        repeat(10) {
            controller.runSimulationIfAllowed(1f / 60f) { state.elapsedSeconds += it }
        }
        controller.dismissTutorial()
        var choicesEmpty = true
        var generatedChoices = 0
        repeat(2) {
            if (state.pendingLevelUps > 0 && choicesEmpty) {
                val acquisition = controller.tryAcquireLevelUp()
                if (acquisition != GameplayInputController.LevelUpAcquisition.REJECTED_BY_TUTORIAL) {
                    generatedChoices++
                    choicesEmpty = false
                }
            }
        }

        assertEquals(1, generatedChoices)
        assertTrue(controller.isModalBlocking())
        assertEquals(before, state.elapsedSeconds)
        assertFalse(controller.canAdvanceSimulation())
    }

    @Test
    fun existing_level_up_allows_choice_population_without_reopening_or_resetting_input() {
        val first = controller.tryAcquireLevelUp()
        state.joystickX = -0.35f
        state.joystickY = 0.2f

        val second = controller.tryAcquireLevelUp()

        assertEquals(GameplayInputController.LevelUpAcquisition.ACQUIRED, first)
        assertEquals(GameplayInputController.LevelUpAcquisition.ALREADY_ACTIVE, second)
        assertEquals(-0.35f, state.joystickX)
        assertEquals(0.2f, state.joystickY)
        assertFalse(controller.canAdvanceSimulation())
    }

    @Test
    fun concurrent_level_up_requests_have_one_acquisition_and_no_reopen_race() {
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val results = Collections.synchronizedList(
            mutableListOf<GameplayInputController.LevelUpAcquisition>()
        )
        val workers = List(8) {
            thread(start = true) {
                ready.countDown()
                assertTrue(start.await(1, TimeUnit.SECONDS))
                results += controller.tryAcquireLevelUp()
            }
        }

        assertTrue(ready.await(1, TimeUnit.SECONDS))
        start.countDown()
        workers.forEach { it.join(1_000) }

        assertEquals(1, results.count {
            it == GameplayInputController.LevelUpAcquisition.ACQUIRED
        })
        assertEquals(7, results.count {
            it == GameplayInputController.LevelUpAcquisition.ALREADY_ACTIVE
        })
        assertFalse(controller.canAdvanceSimulation())
    }

    @Test
    fun blockingModalConsumesTouchAndDragWithoutReachingGameplay() {
        controller.tryAcquireLevelUp()

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
        controller.tryAcquireLevelUp()
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
