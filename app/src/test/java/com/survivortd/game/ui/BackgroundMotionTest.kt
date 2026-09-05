package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BackgroundMotionTest {

    @Test
    fun calculateBackgroundOffset_scalesBothAxesIndependently() {
        val actual = calculateBackgroundOffset(
            projectedWorldOriginPx = BackgroundOffsetPx(120f, -80f),
            parallax = BackgroundParallax2D(0.5f, 0.25f)
        )

        assertOffsetEquals(60f, -20f, actual)
    }

    @Test
    fun calculateBackgroundOffset_preservesVerticalCameraMovement() {
        val first = calculateBackgroundOffset(
            projectedWorldOriginPx = BackgroundOffsetPx(24f, 90f),
            parallax = BackgroundParallax2D(0.4f, 0.4f)
        )
        val second = calculateBackgroundOffset(
            projectedWorldOriginPx = BackgroundOffsetPx(24f, 30f),
            parallax = BackgroundParallax2D(0.4f, 0.4f)
        )

        assertOffsetEquals(9.6f, 36f, first)
        assertOffsetEquals(9.6f, 12f, second)
        assertEquals(-24f, second.y - first.y, EPSILON)
    }

    @Test
    fun calculateBackgroundOffset_handlesDiagonalMovement() {
        val actual = calculateBackgroundOffset(
            projectedWorldOriginPx = BackgroundOffsetPx(-70f, 50f),
            parallax = BackgroundParallax2D(0.5f, 0.5f)
        )

        assertOffsetEquals(-35f, 25f, actual)
    }

    @Test
    fun wrapBackgroundPhasePx_wrapsPositiveOffsets() {
        assertEquals(1f, wrapBackgroundPhasePx(65f, 32f), EPSILON)
        assertEquals(0f, wrapBackgroundPhasePx(64f, 32f), EPSILON)
        assertEquals(31.5f, wrapBackgroundPhasePx(31.5f, 32f), EPSILON)
    }

    @Test
    fun wrapBackgroundPhasePx_wrapsNegativeOffsets() {
        assertEquals(31f, wrapBackgroundPhasePx(-1f, 32f), EPSILON)
        assertEquals(31f, wrapBackgroundPhasePx(-33f, 32f), EPSILON)
        assertEquals(0f, wrapBackgroundPhasePx(-64f, 32f), EPSILON)
    }

    @Test
    fun wrapBackgroundPhasePx_rejectsInvalidPeriod() {
        assertThrows(IllegalArgumentException::class.java) { wrapBackgroundPhasePx(1f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { wrapBackgroundPhasePx(1f, -1f) }
        assertThrows(IllegalArgumentException::class.java) { wrapBackgroundPhasePx(1f, Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { wrapBackgroundPhasePx(1f, Float.POSITIVE_INFINITY) }
    }

    @Test
    fun wrapBackgroundPhasePx_rejectsNonFiniteOffset() {
        assertThrows(IllegalArgumentException::class.java) {
            wrapBackgroundPhasePx(Float.NaN, 32f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            wrapBackgroundPhasePx(Float.POSITIVE_INFINITY, 32f)
        }
    }

    @Test
    fun backgroundOriginPx_projectsBothCoordinatesThroughVisibleWorldTransform() {
        val transform = VisibleWorldTransform(
            canvasWidth = 900f,
            canvasHeight = 600f,
            worldHeight = 720f,
            worldWidth = 1280f,
            cameraX = 640f,
            cameraY = 330f,
            shakeX = 4f,
            shakeY = -6f
        )

        val expected = transform.worldToScreen(0f, 0f)
        val actual = backgroundOriginPx(transform)

        assertOffsetEquals(expected.first, expected.second, actual)
    }

    @Test
    fun backgroundOriginPx_movesOppositeCameraOnBothAxes() {
        val first = VisibleWorldTransform(
            canvasWidth = 500f,
            canvasHeight = 500f,
            worldHeight = 500f,
            worldWidth = 1280f,
            arenaHeight = 720f,
            cameraX = 520f,
            cameraY = 280f,
            shakeX = 0f,
            shakeY = 0f
        )
        val second = first.copy(cameraX = 680f, cameraY = 400f)

        val firstOrigin = backgroundOriginPx(first)
        val secondOrigin = backgroundOriginPx(second)
        val expectedDelta = first.worldToScreen(0f, 0f).let { firstPoint ->
            val secondPoint = second.worldToScreen(0f, 0f)
            secondPoint.first - firstPoint.first to secondPoint.second - firstPoint.second
        }

        assertEquals(expectedDelta.first, secondOrigin.x - firstOrigin.x, EPSILON)
        assertEquals(expectedDelta.second, secondOrigin.y - firstOrigin.y, EPSILON)
        assertEquals(true, secondOrigin.x < firstOrigin.x)
        assertEquals(true, secondOrigin.y < firstOrigin.y)
    }

    private fun assertOffsetEquals(expectedX: Float, expectedY: Float, actual: BackgroundOffsetPx) {
        assertEquals(expectedX, actual.x, EPSILON)
        assertEquals(expectedY, actual.y, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
