package com.survivortd.game.ui

internal data class DamageNumberScreenPosition(
    val x: Float,
    val baselineY: Float
)

/** Keeps a centered damage label's complete glyph bounds inside the canvas. */
internal fun fitDamageNumberToCanvas(
    anchorX: Float,
    baselineY: Float,
    textWidth: Float,
    fontTop: Float,
    fontBottom: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    edgePadding: Float
): DamageNumberScreenPosition? {
    val halfWidth = textWidth / 2f
    val minX = edgePadding + halfWidth
    val maxX = canvasWidth - edgePadding - halfWidth
    val minBaseline = edgePadding - fontTop
    val maxBaseline = canvasHeight - edgePadding - fontBottom

    if (minX > maxX || minBaseline > maxBaseline) return null

    return DamageNumberScreenPosition(
        x = anchorX.coerceIn(minX, maxX),
        baselineY = baselineY.coerceIn(minBaseline, maxBaseline)
    )
}
