package com.survivortd.game.ui

/**
 * Immutable projection between world coordinates and the visible canvas.
 *
 * The canvas height always represents [worldHeight] world units. Camera shake is
 * expressed in world units and moves rendered content in the shake direction,
 * so the world position at the canvas center is the camera position minus shake.
 */
data class VisibleWorldTransform(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val worldHeight: Float,
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

    /** World-space x-coordinate displayed at the horizontal canvas center. */
    val visibleCenterX: Float = cameraX - shakeX

    /** World-space y-coordinate displayed at the vertical canvas center. */
    val visibleCenterY: Float = cameraY - shakeY

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
