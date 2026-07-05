package com.survivortd.game.utils

/**
 * Frustum culling helper — checks whether a world-space position is visible
 * within the camera viewport plus a configurable margin.
 *
 * All math uses squared distances where possible to avoid sqrt calls.
 */
class FrustumCuller {
    var camX: Float = 0f
    var camY: Float = 0f
    var viewWidth: Float = 0f
    var viewHeight: Float = 0f
    var margin: Float = 100f

    /** Update camera bounds from camera position and viewport size. */
    fun update(camX: Float, camY: Float, viewWidth: Float, viewHeight: Float) {
        this.camX = camX
        this.camY = camY
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
    }

    /** Returns true if the given world position is potentially visible. */
    fun isVisible(worldX: Float, worldY: Float): Boolean {
        return worldX >= camX - margin &&
               worldX <= camX + viewWidth + margin &&
               worldY >= camY - margin &&
               worldY <= camY + viewHeight + margin
    }
}
