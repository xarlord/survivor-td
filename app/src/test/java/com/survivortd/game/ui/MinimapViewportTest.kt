package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinimapViewportTest {

    @Test
    fun `viewport maps the transform visible world bounds into minimap coordinates`() {
        val transform = VisibleWorldTransform(
            canvasWidth = 1080f,
            canvasHeight = 2400f,
            worldHeight = WORLD_HEIGHT,
            cameraX = 640f,
            cameraY = 360f,
            shakeX = 10f,
            shakeY = -20f
        )
        val projection = MinimapProjection(
            minimapSize = MINIMAP_SIZE,
            worldWidth = WORLD_WIDTH,
            worldHeight = WORLD_HEIGHT
        )

        val viewport = projection.map(transform.visibleWorldBounds())

        assertEquals(58.5f, viewport.left, EPSILON)
        assertEquals(37.5f, viewport.top, EPSILON)
        assertEquals(99f, viewport.right, EPSILON)
        assertEquals(127.5f, viewport.bottom, EPSILON)
    }

    @Test
    fun `portrait and landscape transforms produce aspect sensitive viewport dimensions`() {
        val projection = MinimapProjection(
            minimapSize = MINIMAP_SIZE,
            worldWidth = WORLD_WIDTH,
            worldHeight = WORLD_HEIGHT
        )
        val portrait = projection.map(
            transform(canvasWidth = 1080f, canvasHeight = 2400f).visibleWorldBounds()
        )
        val landscape = projection.map(
            transform(canvasWidth = 2400f, canvasHeight = 1080f).visibleWorldBounds()
        )

        assertEquals(40.5f, portrait.width, EPSILON)
        assertEquals(90f, portrait.height, EPSILON)
        assertTrue(portrait.width < portrait.height)
        assertEquals(200f, landscape.width, EPSILON)
        assertEquals(90f, landscape.height, EPSILON)
        assertTrue(landscape.width > landscape.height)
    }

    @Test
    fun `circular clip contains center and edge but rejects square corners`() {
        val projection = MinimapProjection(
            minimapSize = MINIMAP_SIZE,
            worldWidth = WORLD_WIDTH,
            worldHeight = WORLD_HEIGHT
        )

        assertTrue(projection.circularClip.contains(80f, 80f))
        assertTrue(projection.circularClip.contains(160f, 80f))
        assertFalse(projection.circularClip.contains(160f, 160f))
        assertFalse(projection.circularClip.contains(-0.1f, 80f))
    }

    private fun transform(canvasWidth: Float, canvasHeight: Float) = VisibleWorldTransform(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        worldHeight = WORLD_HEIGHT,
        cameraX = 640f,
        cameraY = 360f,
        shakeX = 0f,
        shakeY = 0f
    )

    private companion object {
        const val WORLD_WIDTH = 1280f
        const val WORLD_HEIGHT = 720f
        const val MINIMAP_SIZE = 160f
        const val EPSILON = 0.001f
    }
}
