package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BuildPlacementTest {

    @Test
    @DisplayName("portrait screen center maps to the visible world center")
    fun centerMapsToCamera() {
        val transform = portraitTransform()

        val actual = BuildPlacement.screenToWorld(
            screenX = 720f,
            screenY = 1560f,
            transform = transform
        )

        assertPointEquals(transform.screenToWorld(720f, 1560f), actual)
    }

    @Test
    @DisplayName("portrait top-left maps to the visible world top-left")
    fun topLeftMaps() {
        val transform = portraitTransform()

        val actual = BuildPlacement.screenToWorld(
            screenX = 0f,
            screenY = 0f,
            transform = transform
        )

        assertPointEquals(transform.screenToWorld(0f, 0f), actual)
    }

    private fun portraitTransform() = VisibleWorldTransform(
        canvasWidth = 1440f,
        canvasHeight = 3120f,
        worldHeight = GameConfig.WORLD_HEIGHT,
        cameraX = 640f,
        cameraY = 360f,
        shakeX = 0f,
        shakeY = 0f
    )

    private fun assertPointEquals(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, EPSILON)
        assertEquals(expected.second, actual.second, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
