package com.survivortd.game.systems

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for AudioManager singleton behavior, especially the no-arg getInstance()
 * overload added in #113.
 */
class AudioManagerTest {

    @BeforeEach
    fun reset() {
        // Reset singleton before each test
        try { AudioManager.getInstance().release() } catch (_: Exception) {}
    }

    @Test
    fun `getInstance no-arg returns silent mode instance`() {
        val instance = AudioManager.getInstance()
        assertNotNull(instance)
        assertTrue(instance.isSilent)
    }

    @Test
    fun `getInstance with null context returns silent mode`() {
        val instance = AudioManager.getInstance(null)
        assertTrue(instance.isSilent)
    }

    @Test
    fun `getInstance returns same instance after initialization`() {
        val first = AudioManager.getInstance()
        val second = AudioManager.getInstance()
        assertSame(first, second)
    }

    @Test
    fun `silent mode playSfx is a no-op`() {
        val instance = AudioManager.getInstance()
        instance.playSfx(AudioManager.SfxType.GUN_SHOT)
        instance.playSfx(AudioManager.SfxType.EXPLOSION, pitch = 2f, volume = 0.5f)
        instance.loadSfx(AudioManager.SfxType.GUN_SHOT)
    }

    @Test
    fun `setSfxVolume clamps to 0-1 range`() {
        val instance = AudioManager.getInstance()
        instance.setSfxVolume(-0.5f)
        instance.setSfxVolume(1.5f)
        instance.setSfxVolume(0.5f)
    }

    @Test
    fun `SfxType has all 15 expected entries`() {
        assertEquals(15, AudioManager.SfxType.entries.size)
    }

    @Test
    fun `no-arg getInstance does not overwrite existing instance`() {
        val first = AudioManager.getInstance()
        assertTrue(first.isSilent)
        val second = AudioManager.getInstance()
        assertSame(first, second)
    }
}
