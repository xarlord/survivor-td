package com.survivortd.game.ui

import com.survivortd.game.config.GameConfig

/** Stable world-space decoration used to make camera motion legible. */
data class ArenaLandmark(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val kind: Kind
) {
    enum class Kind {
        WATCHTOWER,
        CRATE,
        ROCK,
        BEACON
    }
}

/** Fixed, deterministic environment geometry for the 1280x720 arena. */
object ArenaWorldEnvironment {
    const val WORLD_WIDTH: Float = GameConfig.WORLD_WIDTH
    const val WORLD_HEIGHT: Float = GameConfig.WORLD_HEIGHT
    const val BOUNDARY_STROKE_WORLD: Float = 4f
    const val GROUND_DETAIL_SPACING_WORLD: Float = 64f

    val LANDMARKS: List<ArenaLandmark> = listOf(
        ArenaLandmark("north_watchtower", 548f, 118f, 46f, 62f, ArenaLandmark.Kind.WATCHTOWER),
        ArenaLandmark("east_crate", 742f, 506f, 42f, 42f, ArenaLandmark.Kind.CRATE),
        ArenaLandmark("center_rock", 630f, 606f, 54f, 30f, ArenaLandmark.Kind.ROCK),
        ArenaLandmark("west_beacon", 282f, 350f, 28f, 54f, ArenaLandmark.Kind.BEACON),
        ArenaLandmark("south_crate", 960f, 622f, 48f, 38f, ArenaLandmark.Kind.CRATE),
        ArenaLandmark("far_rock", 1110f, 184f, 68f, 34f, ArenaLandmark.Kind.ROCK)
    )
}

/** Intentional portrait framing that permits camera travel on both arena axes. */
object ArenaCameraStyle {
    const val VISIBLE_WORLD_HEIGHT: Float = 560f
}
