package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DamageNumberViewportTest {

    @Test
    fun `left and right edge labels are clamped fully inside canvas`() {
        assertEquals(
            DamageNumberScreenPosition(x = 21f, baselineY = 100f),
            fitDamageNumberToCanvas(
                anchorX = 2f,
                baselineY = 100f,
                textWidth = 40f,
                fontTop = -16f,
                fontBottom = 4f,
                canvasWidth = 200f,
                canvasHeight = 300f,
                edgePadding = 1f
            )
        )
        assertEquals(
            DamageNumberScreenPosition(x = 179f, baselineY = 100f),
            fitDamageNumberToCanvas(
                anchorX = 199f,
                baselineY = 100f,
                textWidth = 40f,
                fontTop = -16f,
                fontBottom = 4f,
                canvasWidth = 200f,
                canvasHeight = 300f,
                edgePadding = 1f
            )
        )
    }

    @Test
    fun `top and bottom edge baselines are clamped using font metrics`() {
        assertEquals(
            DamageNumberScreenPosition(x = 100f, baselineY = 17f),
            fitDamageNumberToCanvas(100f, 1f, 20f, -16f, 4f, 200f, 300f, 1f)
        )
        assertEquals(
            DamageNumberScreenPosition(x = 100f, baselineY = 295f),
            fitDamageNumberToCanvas(100f, 299f, 20f, -16f, 4f, 200f, 300f, 1f)
        )
    }

    @Test
    fun `label larger than available canvas is omitted`() {
        assertNull(
            fitDamageNumberToCanvas(100f, 100f, 220f, -16f, 4f, 200f, 300f, 1f)
        )
    }
}
