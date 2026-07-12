package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BuildPlacementTest {

    @Test
    @DisplayName("screen center maps to camera center")
    fun centerMapsToCamera() {
        val (wx, wy) = BuildPlacement.screenToWorld(
            screenX = 360f,
            screenY = 640f,
            canvasW = 720f,
            canvasH = 1280f,
            cameraX = 640f,
            cameraY = 360f
        )
        assertEquals(640f, wx, 0.5f)
        assertEquals(360f, wy, 0.5f)
    }

    @Test
    @DisplayName("top-left screen maps to camera top-left world")
    fun topLeftMaps() {
        val camX = 640f
        val camY = 360f
        val (wx, wy) = BuildPlacement.screenToWorld(
            screenX = 0f,
            screenY = 0f,
            canvasW = 720f,
            canvasH = 1280f,
            cameraX = camX,
            cameraY = camY
        )
        assertEquals(camX - GameConfig.CAMERA_WIDTH / 2f, wx, 0.5f)
        assertEquals(camY - GameConfig.CAMERA_HEIGHT / 2f, wy, 0.5f)
    }
}
