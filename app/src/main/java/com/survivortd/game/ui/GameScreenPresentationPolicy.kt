package com.survivortd.game.ui

/** Presentation-only ordering for mutually exclusive gameplay modals. */
internal enum class GameModalLayer {
    TUTORIAL,
    RUN_SUMMARY,
    PAUSE,
    LEVEL_UP
}

/** Immutable visibility decision for gameplay chrome and the final modal host. */
internal data class GameScreenPresentation(
    val topmostModal: GameModalLayer?,
    val showHud: Boolean,
    val showWaveAnnouncement: Boolean,
    val showBuildControls: Boolean,
    val showMinimap: Boolean,
    val showFpsTelemetry: Boolean
)

/**
 * Resolves visual presence only. It deliberately does not mutate or dismiss any
 * backing modal state, so pending lower-priority modals survive hand-offs.
 */
internal fun resolveGameScreenPresentation(
    tutorialVisible: Boolean = false,
    runSummaryVisible: Boolean = false,
    pauseVisible: Boolean = false,
    levelUpVisible: Boolean = false,
    waveAnnouncementEligible: Boolean = true,
    buildControlsEligible: Boolean = true,
    minimapEligible: Boolean = true,
    diagnosticsEnabled: Boolean = false
): GameScreenPresentation {
    val topmostModal = when {
        tutorialVisible -> GameModalLayer.TUTORIAL
        runSummaryVisible -> GameModalLayer.RUN_SUMMARY
        pauseVisible -> GameModalLayer.PAUSE
        levelUpVisible -> GameModalLayer.LEVEL_UP
        else -> null
    }
    val showGameplayChrome = topmostModal == null

    return GameScreenPresentation(
        topmostModal = topmostModal,
        showHud = showGameplayChrome,
        showWaveAnnouncement = showGameplayChrome && waveAnnouncementEligible,
        showBuildControls = showGameplayChrome && buildControlsEligible,
        showMinimap = showGameplayChrome && minimapEligible,
        showFpsTelemetry = showGameplayChrome && diagnosticsEnabled
    )
}
