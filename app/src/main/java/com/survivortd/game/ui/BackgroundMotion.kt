package com.survivortd.game.ui

import kotlin.math.floor

/** Screen-space offset of a world-space background origin. */
data class BackgroundOffsetPx(
    val x: Float,
    val y: Float
)

/** Independent horizontal and vertical parallax multipliers for one layer. */
data class BackgroundParallax2D(
    val x: Float,
    val y: Float
)

/** Stable depth settings for the arena's world-space environment layers. */
object ArenaParallaxLayers {
    val HORIZON = BackgroundParallax2D(x = 0.15f, y = 0.12f)
    val BITMAP = BackgroundParallax2D(x = 0.08f, y = 0.08f)
    val SECONDARY_GRID = BackgroundParallax2D(x = 0.35f, y = 0.20f)
    val WORLD_GRID = BackgroundParallax2D(x = 1f, y = 1f)
}

/**
 * Applies a layer's independent parallax multipliers to a projected origin.
 * The projected origin comes from [VisibleWorldTransform], so camera motion is
 * never re-derived by individual render layers.
 */
fun calculateBackgroundOffset(
    projectedWorldOriginPx: BackgroundOffsetPx,
    parallax: BackgroundParallax2D
): BackgroundOffsetPx = BackgroundOffsetPx(
    x = projectedWorldOriginPx.x * parallax.x,
    y = projectedWorldOriginPx.y * parallax.y
)

/** Returns a non-negative phase for a repeating screen-space background tile. */
fun wrapBackgroundPhasePx(offset: Float, period: Float): Float {
    require(offset.isFinite()) { "offset must be finite" }
    require(period.isFinite() && period > 0f) { "period must be finite and greater than zero" }
    return offset - floor(offset / period) * period
}

/** Projects world origin once through the shared entity/touch transform. */
fun backgroundOriginPx(transform: VisibleWorldTransform): BackgroundOffsetPx {
    val origin = transform.worldToScreen(0f, 0f)
    return BackgroundOffsetPx(origin.first, origin.second)
}
