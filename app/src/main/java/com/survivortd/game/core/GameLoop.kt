package com.survivortd.game.core

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Deterministic fixed-timestep game loop.
 *
 * - Physics/logic runs at exactly 60Hz regardless of display FPS
 * - Rendering interpolates between physics states for smooth visuals
 * - Accumulator capped to prevent "spiral of death"
 * - Input events are queued and processed at the next physics tick
 */
class GameLoop(
    private val onUpdate: (Float) -> Unit,
    private val onRender: (Float) -> Unit,
    private val targetTps: Int = 60,
    private val maxFrameSkip: Int = 5
) {
    private var prevTimeNanos: Long = 0L
    private var accumulator: Double = 0.0
    private var running: Boolean = false
    private val inputQueue = ConcurrentLinkedQueue<GameInput>()

    val dtSeconds: Double get() = 1.0 / targetTps
    val dtNanos: Double get() = dtSeconds * 1_000_000_000.0
    var currentTick: Long = 0
        private set
    var isRunning: Boolean = false
        private set

    fun start(scope: CoroutineScope) {
        running = true
        isRunning = true
        prevTimeNanos = System.nanoTime()
        currentTick = 0
        scope.launch(Dispatchers.Default) {
            while (running && isActive) {
                val now = System.nanoTime()
                var elapsed = (now - prevTimeNanos).toDouble()
                prevTimeNanos = now

                // Cap elapsed time to prevent spiral of death
                val maxElapsed = dtNanos * maxFrameSkip
                if (elapsed > maxElapsed) {
                    elapsed = maxElapsed
                }

                accumulator += elapsed

                var updates = 0
                while (accumulator >= dtNanos && updates < maxFrameSkip) {
                    processInputs()
                    onUpdate(dtSeconds.toFloat())
                    accumulator -= dtNanos
                    updates++
                    currentTick++
                }

                val alpha = (accumulator / dtNanos).toFloat()
                onRender(alpha)
                Thread.sleep(1)
            }
        }
    }

    fun stop() {
        running = false
        isRunning = false
    }

    fun queueInput(input: GameInput) {
        inputQueue.add(input)
    }

    private fun processInputs() {
        while (true) {
            val input = inputQueue.poll() ?: break
            handleInput(input)
        }
    }

    var handleInput: (GameInput) -> Unit = {}
}

/**
 * Input event tied to a specific physics tick.
 */
data class GameInput(
    val tick: Long,
    val type: InputType,
    val x: Float,
    val y: Float,
    val extra: Map<String, Float> = emptyMap()
)

enum class InputType {
    TAP_DOWN, TAP_UP, DRAG, SWIPE, KEY_PRESS, JOYSTICK_MOVE, JOYSTICK_RELEASE
}
