package com.survivortd.game.ui

import com.survivortd.game.utils.FrustumCuller

/** Creates the canvas culler from the exact world bounds represented by [transform]. */
internal fun createGameCanvasFrustumCuller(
    transform: VisibleWorldTransform,
    screenMargin: Float
): FrustumCuller {
    val bounds = transform.visibleWorldBounds()
    return FrustumCuller().apply {
        margin = screenMargin / transform.scale
        update(
            camX = bounds.left,
            camY = bounds.top,
            viewWidth = bounds.width,
            viewHeight = bounds.height
        )
    }
}
