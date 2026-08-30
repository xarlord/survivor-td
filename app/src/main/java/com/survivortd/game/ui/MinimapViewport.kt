package com.survivortd.game.ui

/** Pure world-to-minimap projection shared by entity dots and the camera viewport. */
internal class MinimapProjection(
    val minimapSize: Float,
    worldWidth: Float,
    worldHeight: Float
) {
    init {
        require(minimapSize.isFinite() && minimapSize > 0f) { "minimapSize must be positive" }
        require(worldWidth.isFinite() && worldWidth > 0f) { "worldWidth must be positive" }
        require(worldHeight.isFinite() && worldHeight > 0f) { "worldHeight must be positive" }
    }

    private val scale = minOf(minimapSize / worldWidth, minimapSize / worldHeight)
    private val offsetX = (minimapSize - worldWidth * scale) / 2f
    private val offsetY = (minimapSize - worldHeight * scale) / 2f

    val circularClip = MinimapCircle(
        centerX = minimapSize / 2f,
        centerY = minimapSize / 2f,
        radius = minimapSize / 2f
    )

    fun mapX(worldX: Float): Float = worldX * scale + offsetX

    fun mapY(worldY: Float): Float = worldY * scale + offsetY

    fun projectTowerMarkers(towers: List<WorldPoint>): List<MinimapPoint> =
        towers.map { tower ->
            MinimapPoint(
                x = mapX(tower.x),
                y = mapY(tower.y)
            )
        }

    /** Maps the exact world bounds represented by the game canvas transform. */
    fun map(bounds: VisibleWorldBounds): MinimapViewport = MinimapViewport(
        left = mapX(bounds.left),
        top = mapY(bounds.top),
        right = mapX(bounds.right),
        bottom = mapY(bounds.bottom)
    )
}

internal data class WorldPoint(val x: Float, val y: Float)

internal data class MinimapPoint(val x: Float, val y: Float)

/** Minimap-space camera viewport rectangle. */
internal data class MinimapViewport(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float = right - left
    val height: Float = bottom - top
}

/** Pure circular clip geometry used by the minimap canvas and JVM tests. */
internal data class MinimapCircle(
    val centerX: Float,
    val centerY: Float,
    val radius: Float
) {
    fun contains(x: Float, y: Float): Boolean {
        val deltaX = x - centerX
        val deltaY = y - centerY
        return deltaX * deltaX + deltaY * deltaY <= radius * radius
    }
}
