package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameCanvasProjectionTest {

    @Test
    fun `frustum follows shaken visible bounds and converts pixel margin to world units`() {
        val transform = VisibleWorldTransform(
            canvasWidth = 1440f,
            canvasHeight = 3120f,
            worldHeight = 720f,
            cameraX = 640f,
            cameraY = 360f,
            shakeX = 12f,
            shakeY = -6f
        )
        val bounds = transform.visibleWorldBounds()

        val culler = createGameCanvasFrustumCuller(
            transform = transform,
            screenMargin = 100f
        )

        assertEquals(bounds.left, culler.camX, EPSILON)
        assertEquals(bounds.top, culler.camY, EPSILON)
        assertEquals(bounds.width, culler.viewWidth, EPSILON)
        assertEquals(bounds.height, culler.viewHeight, EPSILON)
        assertEquals(100f / transform.scale, culler.margin, EPSILON)
        assertTrue(culler.isVisible(bounds.left - culler.margin, bounds.top))
        assertFalse(culler.isVisible(bounds.left - culler.margin - 0.01f, bounds.top))
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
