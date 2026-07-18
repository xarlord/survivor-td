package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VisibleWorldTransformTest {

    @Test
    fun `portrait canvas derives scale visible dimensions center and bounds`() {
        val transform = portraitTransform()

        assertEquals(3120f / 720f, transform.scale, EPSILON)
        assertEquals(1440f / transform.scale, transform.visibleWorldWidth, EPSILON)
        assertEquals(720f, transform.visibleWorldHeight, EPSILON)
        assertEquals(628f, transform.visibleCenterX, EPSILON)
        assertEquals(366f, transform.visibleCenterY, EPSILON)

        val bounds = transform.visibleWorldBounds()
        assertEquals(transform.visibleCenterX - transform.visibleWorldWidth / 2f, bounds.left, EPSILON)
        assertEquals(transform.visibleCenterY - transform.visibleWorldHeight / 2f, bounds.top, EPSILON)
        assertEquals(transform.visibleCenterX + transform.visibleWorldWidth / 2f, bounds.right, EPSILON)
        assertEquals(transform.visibleCenterY + transform.visibleWorldHeight / 2f, bounds.bottom, EPSILON)
        assertEquals(transform.visibleWorldWidth, bounds.width, EPSILON)
        assertEquals(transform.visibleWorldHeight, bounds.height, EPSILON)
    }

    @Test
    fun `portrait visible center and corners project to canvas center and corners`() {
        val transform = portraitTransform()
        val bounds = transform.visibleWorldBounds()

        assertPointEquals(720f to 1560f, transform.worldToScreen(transform.visibleCenterX, transform.visibleCenterY))
        assertPointEquals(0f to 0f, transform.worldToScreen(bounds.left, bounds.top))
        assertPointEquals(1440f to 0f, transform.worldToScreen(bounds.right, bounds.top))
        assertPointEquals(0f to 3120f, transform.worldToScreen(bounds.left, bounds.bottom))
        assertPointEquals(1440f to 3120f, transform.worldToScreen(bounds.right, bounds.bottom))
    }

    @Test
    fun `landscape canvas exposes aspect-sensitive visible world`() {
        val transform = VisibleWorldTransform(
            canvasWidth = 3120f,
            canvasHeight = 1440f,
            worldHeight = 720f,
            cameraX = 640f,
            cameraY = 360f,
            shakeX = 10f,
            shakeY = -20f
        )

        assertEquals(2f, transform.scale, EPSILON)
        assertEquals(1560f, transform.visibleWorldWidth, EPSILON)
        assertEquals(720f, transform.visibleWorldHeight, EPSILON)
        assertEquals(630f, transform.visibleCenterX, EPSILON)
        assertEquals(380f, transform.visibleCenterY, EPSILON)

        val bounds = transform.visibleWorldBounds()
        assertEquals(-150f, bounds.left, EPSILON)
        assertEquals(20f, bounds.top, EPSILON)
        assertEquals(1410f, bounds.right, EPSILON)
        assertEquals(740f, bounds.bottom, EPSILON)
        assertPointEquals(1560f to 720f, transform.worldToScreen(630f, 380f))
    }

    @Test
    fun `nonzero shake offsets the visible center opposite the rendered shake`() {
        val transform = portraitTransform()

        assertPointEquals(
            720f to 1560f,
            transform.worldToScreen(transform.cameraX - transform.shakeX, transform.cameraY - transform.shakeY)
        )
        assertPointEquals(
            (720f + transform.shakeX * transform.scale) to
                (1560f + transform.shakeY * transform.scale),
            transform.worldToScreen(transform.cameraX, transform.cameraY)
        )
    }

    @Test
    fun `world to screen to world round trips representative points`() {
        val transform = portraitTransform()
        val worldPoints = listOf(
            -125.5f to 47.25f,
            transform.visibleCenterX to transform.visibleCenterY,
            701.25f to 412.75f,
            1600f to -900f
        )

        worldPoints.forEach { world ->
            val screen = transform.worldToScreen(world.first, world.second)
            assertPointEquals(world, transform.screenToWorld(screen.first, screen.second))
        }
    }

    @Test
    fun `screen to world to screen round trips center corners and off-canvas points`() {
        val transform = portraitTransform()
        val screenPoints = listOf(
            0f to 0f,
            720f to 1560f,
            1440f to 3120f,
            -240f to 3400f
        )

        screenPoints.forEach { screen ->
            val world = transform.screenToWorld(screen.first, screen.second)
            assertPointEquals(screen, transform.worldToScreen(world.first, world.second))
        }
    }

    private fun portraitTransform() = VisibleWorldTransform(
        canvasWidth = 1440f,
        canvasHeight = 3120f,
        worldHeight = 720f,
        cameraX = 640f,
        cameraY = 360f,
        shakeX = 12f,
        shakeY = -6f
    )

    private fun assertPointEquals(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, EPSILON)
        assertEquals(expected.second, actual.second, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
