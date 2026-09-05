package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArenaWorldEnvironmentTest {

    @Test
    fun parallaxLayers_haveNonZeroIndependentAxesAndDistinctDepths() {
        val layers = listOf(
            ArenaParallaxLayers.HORIZON,
            ArenaParallaxLayers.BITMAP,
            ArenaParallaxLayers.SECONDARY_GRID,
            ArenaParallaxLayers.WORLD_GRID
        )

        assertTrue(layers.all { it.x > 0f && it.y > 0f })
        assertTrue(layers.any { it.x != it.y })
        assertEquals(1f, ArenaParallaxLayers.WORLD_GRID.x)
        assertEquals(1f, ArenaParallaxLayers.WORLD_GRID.y)
        assertNotEquals(ArenaParallaxLayers.HORIZON, ArenaParallaxLayers.BITMAP)
        assertNotEquals(ArenaParallaxLayers.BITMAP, ArenaParallaxLayers.SECONDARY_GRID)
    }

    @Test
    fun landmarks_areDeterministicAndInsideFixedArena() {
        val landmarks = ArenaWorldEnvironment.LANDMARKS

        assertTrue(landmarks.size >= 4)
        assertEquals(landmarks.map { it.id }.toSet().size, landmarks.size)
        assertTrue(landmarks.all { it.x in 0f..GameConfig.WORLD_WIDTH })
        assertTrue(landmarks.all { it.y in 0f..GameConfig.WORLD_HEIGHT })
        assertTrue(landmarks.all { it.width > 0f && it.height > 0f })
    }

    @Test
    fun arenaBoundary_usesGddWorldDimensions() {
        assertEquals(GameConfig.WORLD_WIDTH, ArenaWorldEnvironment.WORLD_WIDTH)
        assertEquals(GameConfig.WORLD_HEIGHT, ArenaWorldEnvironment.WORLD_HEIGHT)
    }

    @Test
    fun cameraViewport_isSmallerThanArenaToAllowTwoAxisFollow() {
        assertTrue(ArenaCameraStyle.VISIBLE_WORLD_HEIGHT < GameConfig.WORLD_HEIGHT)
        assertTrue(ArenaCameraStyle.VISIBLE_WORLD_HEIGHT > GameConfig.WORLD_HEIGHT / 2f)
    }
}
