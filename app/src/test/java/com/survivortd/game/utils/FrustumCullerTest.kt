package com.survivortd.game.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for FrustumCuller — visibility bounds checking.
 */
class FrustumCullerTest {

    @Nested
    @DisplayName("Visibility Bounds")
    inner class VisibilityTest {

        @Test
        fun `center of viewport is visible`() {
            val culler = FrustumCuller()
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertTrue(culler.isVisible(50f, 50f), "Center of viewport should be visible")
        }

        @Test
        fun `position inside viewport is visible`() {
            val culler = FrustumCuller()
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertTrue(culler.isVisible(25f, 25f), "Position inside viewport should be visible")
        }

        @Test
        fun `position just outside left edge is culled`() {
            val culler = FrustumCuller()
            culler.margin = 10f
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertFalse(culler.isVisible(-11f, 50f), "Position outside left edge should be culled")
        }

        @Test
        fun `position just outside right edge is culled`() {
            val culler = FrustumCuller()
            culler.margin = 10f
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertFalse(culler.isVisible(111f, 50f), "Position outside right edge should be culled")
        }

        @Test
        fun `position just outside top edge is culled`() {
            val culler = FrustumCuller()
            culler.margin = 10f
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertFalse(culler.isVisible(50f, -11f), "Position outside top edge should be culled")
        }

        @Test
        fun `position just outside bottom edge is culled`() {
            val culler = FrustumCuller()
            culler.margin = 10f
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertFalse(culler.isVisible(50f, 111f), "Position outside bottom edge should be culled")
        }

        @Test
        fun `position within margin is visible`() {
            val culler = FrustumCuller()
            culler.margin = 50f
            culler.update(camX = 0f, camY = 0f, viewWidth = 100f, viewHeight = 100f)
            assertTrue(culler.isVisible(-40f, 50f), "Position within margin should be visible")
            assertTrue(culler.isVisible(140f, 50f), "Position within margin on right should be visible")
            assertTrue(culler.isVisible(50f, -40f), "Position within margin on top should be visible")
            assertTrue(culler.isVisible(50f, 140f), "Position within margin on bottom should be visible")
        }
    }

    @Nested
    @DisplayName("Camera Offset")
    inner class CameraOffsetTest {

        @Test
        fun `culling works with non-zero camera position`() {
            val culler = FrustumCuller()
            culler.margin = 10f
            culler.update(camX = 500f, camY = 300f, viewWidth = 100f, viewHeight = 100f)
            // Inside
            assertTrue(culler.isVisible(550f, 350f), "Center of offset viewport should be visible")
            // Outside
            assertFalse(culler.isVisible(489f, 350f), "Left of offset viewport should be culled")
            assertFalse(culler.isVisible(611f, 350f), "Right of offset viewport should be culled")
        }
    }
}
