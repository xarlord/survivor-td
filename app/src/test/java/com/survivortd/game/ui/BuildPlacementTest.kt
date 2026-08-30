package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BuildPlacementTest {

    @Test
    @DisplayName("portrait center placement uses the shaken visible world center")
    fun centerUsesCameraAndShake() {
        val transform = portraitTransform()

        val actual = BuildPlacement.screenToWorld(
            screenX = 720f,
            screenY = 1560f,
            transform = transform
        )

        assertWorldPointEquals(expectedX = 628f, expectedY = 366f, actual = actual)
    }

    @Test
    @DisplayName("portrait top-left placement uses camera, shake, and portrait scale")
    fun topLeftUsesCameraShakeAndPortraitScale() {
        val transform = portraitTransform()

        val actual = BuildPlacement.screenToWorld(
            screenX = 0f,
            screenY = 0f,
            transform = transform
        )

        assertWorldPointEquals(
            expectedX = 461.84616f,
            expectedY = 6.000013f,
            actual = actual
        )
    }

    private fun portraitTransform() = VisibleWorldTransform(
        canvasWidth = 1440f,
        canvasHeight = 3120f,
        worldHeight = GameConfig.WORLD_HEIGHT,
        cameraX = 640f,
        cameraY = 360f,
        shakeX = 12f,
        shakeY = -6f
    )

    private fun assertWorldPointEquals(
        expectedX: Float,
        expectedY: Float,
        actual: Pair<Float, Float>
    ) {
        assertEquals(expectedX, actual.first, EPSILON)
        assertEquals(expectedY, actual.second, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.001f
    }
}
