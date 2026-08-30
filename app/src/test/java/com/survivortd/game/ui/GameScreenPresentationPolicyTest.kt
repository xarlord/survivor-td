package com.survivortd.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameScreenPresentationPolicyTest {

    @Test
    fun `no modal exposes eligible gameplay chrome without diagnostics`() {
        val presentation = resolveGameScreenPresentation(
            tutorialVisible = false,
            runSummaryVisible = false,
            pauseVisible = false,
            levelUpVisible = false,
            waveAnnouncementEligible = true,
            buildControlsEligible = true,
            minimapEligible = false,
            diagnosticsEnabled = false
        )

        assertNull(presentation.topmostModal)
        assertTrue(presentation.showHud)
        assertTrue(presentation.showWaveAnnouncement)
        assertTrue(presentation.showBuildControls)
        assertFalse(presentation.showMinimap)
        assertFalse(presentation.showFpsTelemetry)
    }

    @Test
    fun `explicit diagnostics shows fps only during unobstructed gameplay`() {
        val unobstructed = resolveGameScreenPresentation(diagnosticsEnabled = true)
        val paused = resolveGameScreenPresentation(pauseVisible = true, diagnosticsEnabled = true)

        assertTrue(unobstructed.showFpsTelemetry)
        assertFalse(paused.showFpsTelemetry)
    }

    @Test
    fun `every modal suppresses all gameplay chrome`() {
        val presentations = listOf(
            resolveGameScreenPresentation(tutorialVisible = true, diagnosticsEnabled = true),
            resolveGameScreenPresentation(runSummaryVisible = true, diagnosticsEnabled = true),
            resolveGameScreenPresentation(pauseVisible = true, diagnosticsEnabled = true),
            resolveGameScreenPresentation(levelUpVisible = true, diagnosticsEnabled = true)
        )

        presentations.forEach { presentation ->
            assertFalse(presentation.showHud)
            assertFalse(presentation.showWaveAnnouncement)
            assertFalse(presentation.showBuildControls)
            assertFalse(presentation.showMinimap)
            assertFalse(presentation.showFpsTelemetry)
        }
    }

    @Test
    fun `modal priority matches the existing topmost presentation order`() {
        assertEquals(
            GameModalLayer.TUTORIAL,
            resolveGameScreenPresentation(
                tutorialVisible = true,
                runSummaryVisible = true,
                pauseVisible = true,
                levelUpVisible = true
            ).topmostModal
        )
        assertEquals(
            GameModalLayer.RUN_SUMMARY,
            resolveGameScreenPresentation(
                runSummaryVisible = true,
                pauseVisible = true,
                levelUpVisible = true
            ).topmostModal
        )
        assertEquals(
            GameModalLayer.PAUSE,
            resolveGameScreenPresentation(
                pauseVisible = true,
                levelUpVisible = true
            ).topmostModal
        )
        assertEquals(
            GameModalLayer.LEVEL_UP,
            resolveGameScreenPresentation(levelUpVisible = true).topmostModal
        )
    }

    @Test
    fun `clearing a higher priority modal reveals pending lower priority modal`() {
        val withTutorial = resolveGameScreenPresentation(
            tutorialVisible = true,
            runSummaryVisible = true,
            pauseVisible = true,
            levelUpVisible = true
        )
        val afterTutorial = resolveGameScreenPresentation(
            tutorialVisible = false,
            runSummaryVisible = true,
            pauseVisible = true,
            levelUpVisible = true
        )

        assertEquals(GameModalLayer.TUTORIAL, withTutorial.topmostModal)
        assertEquals(GameModalLayer.RUN_SUMMARY, afterTutorial.topmostModal)
    }
}
