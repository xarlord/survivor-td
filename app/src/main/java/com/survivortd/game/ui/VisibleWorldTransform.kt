package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig

/**
 * Immutable projection between world coordinates and the visible canvas.
 *
 * The canvas height always represents [worldHeight] world units. [cameraX] and
 * [cameraY] are the player-follow target (the camera anchor); the anchor is
 * clamped to the fixed arena before camera shake is applied. Every world-space
 * consumer — entities, projectiles, tower placement, minimap, touch conversion,
 * and environment detail — must use this immutable projection.
 */
data class VisibleWorldTransform(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val worldHeight: Float,
    val worldWidth: Float = GameConfig.WORLD_WIDTH,
    val arenaHeight: Float = GameConfig.WORLD_HEIGHT,
    val cameraX: Float,
    val cameraY: Float,
    val shakeX: Float,
    val shakeY: Float
) {
    /** Screen pixels per world unit. */
    val scale: Float = canvasHeight / worldHeight

    /** Width of the canvas viewport in world units. */
    val visibleWorldWidth: Float = canvasWidth / scale

    /** Height of the canvas viewport in world units. */
    val visibleWorldHeight: Float = canvasHeight / scale

    /** Bounded follow target; the unshaken viewport never exposes space beyond the arena. */
    val cameraAnchorX: Float = boundedCameraCenter(cameraX, visibleWorldWidth, worldWidth)

    /** Bounded follow target; axes whose viewport spans the world remain centered. */
    val cameraAnchorY: Float = boundedCameraCenter(cameraY, visibleWorldHeight, arenaHeight)

    /** World-space x-coordinate displayed at the horizontal canvas center. */
    val visibleCenterX: Float = cameraAnchorX - shakeX

    /** World-space y-coordinate displayed at the vertical canvas center. */
    val visibleCenterY: Float = cameraAnchorY - shakeY

    /** Projects a world-space x-coordinate into canvas coordinates. */
    fun worldToScreenX(worldX: Float): Float =
        (worldX - visibleCenterX) * scale + canvasWidth / 2f

    /** Projects a world-space y-coordinate into canvas coordinates. */
    fun worldToScreenY(worldY: Float): Float =
        (worldY - visibleCenterY) * scale + canvasHeight / 2f

    /** Projects a world-space point into canvas coordinates. */
    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> =
        worldToScreenX(worldX) to worldToScreenY(worldY)

    /** Unprojects a canvas point into world coordinates. */
    fun screenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        val worldX = (screenX - canvasWidth / 2f) / scale + visibleCenterX
        val worldY = (screenY - canvasHeight / 2f) / scale + visibleCenterY
        return worldX to worldY
    }

    /** Returns the world-space bounds currently projected onto the canvas. */
    fun visibleWorldBounds(): VisibleWorldBounds {
        val halfWidth = visibleWorldWidth / 2f
        val halfHeight = visibleWorldHeight / 2f
        return VisibleWorldBounds(
            left = visibleCenterX - halfWidth,
            top = visibleCenterY - halfHeight,
            right = visibleCenterX + halfWidth,
            bottom = visibleCenterY + halfHeight
        )
    }
}

private fun boundedCameraCenter(target: Float, viewportExtent: Float, worldExtent: Float): Float {
    require(target.isFinite() && viewportExtent.isFinite() && worldExtent.isFinite())
    require(viewportExtent > 0f && worldExtent > 0f)
    if (viewportExtent >= worldExtent) return worldExtent / 2f
    val halfViewport = viewportExtent / 2f
    return target.coerceIn(halfViewport, worldExtent - halfViewport)
}

/** Axis-aligned world-space viewport bounds. */
data class VisibleWorldBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float = right - left
    val height: Float = bottom - top
}
