package com.survivortd.game.systems

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import kotlinx.serialization.Serializable

/**
 * Audio Manager — manages SFX and BGM playback.
 *
 * SFX use SoundPool for low-latency playback.
 * BGM uses MediaPlayer (handled at the UI layer).
 *
 * All SFX are synthesized programmatically (see scripts/synth_audio.py).
 * The actual audio files are loaded from assets/audio/sfx/ at runtime.
 *
 * For unit testing, AudioManager can operate in "silent mode" where
 * all calls are no-ops (when context == null).
 */
class AudioManager private constructor(
    private val context: Context?,
    private val soundPool: SoundPool?
) {
    @Serializable
    enum class SfxType(val fileName: String) {
        // Weapon fire
        GUN_SHOT("sfx/gun_shot.ogg"),
        EXPLOSION("sfx/explosion.ogg"),
        LASER_HUM("sfx/laser_hum.ogg"),
        MAGIC_BLAST("sfx/magic_blast.ogg"),

        // Enemy
        ENEMY_DEATH("sfx/enemy_death.ogg"),
        BOSS_ROAR("sfx/boss_roar.ogg"),

        // Player
        LEVEL_UP("sfx/level_up.ogg"),
        PLAYER_HIT("sfx/player_hit.ogg"),
        PLAYER_DEATH("sfx/player_death.ogg"),

        // Pickup
        GEM_COLLECT("sfx/gem_collect.ogg"),
        HEAL("sfx/heal.ogg"),

        // Tower
        TURRET_SHOT("sfx/turret_shot.ogg"),
        TESLA_ZAP("sfx/tesla_zap.ogg"),

        // UI
        BUTTON_TAP("sfx/button_tap.ogg"),
        COIN_CLINK("sfx/coin_clink.ogg")
    }

    @Serializable
    enum class BgmType(val fileName: String) {
        MENU("bgm/menu.mp3"),
        BATTLE("bgm/battle.mp3"),
        BOSS("bgm/boss.mp3"),
        BUILD_PHASE("bgm/build_phase.mp3")
    }

    private val soundIds = mutableMapOf<SfxType, Int>()
    private var sfxVolume = 0.8f
    private var bgmVolume = 0.6f
    private var hapticsEnabled = true

    val isSilent: Boolean
        get() = context == null || soundPool == null

    companion object {
        @Volatile
        private var instance: AudioManager? = null

        /**
         * Get the singleton instance. Pass null context for silent mode (unit tests).
         */
        fun getInstance(context: Context? = null): AudioManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val pool = if (context != null) {
                        SoundPool.Builder()
                            .setMaxStreams(8)
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .build()
                    } else null
                    AudioManager(context, pool).also { instance = it }
                }
            }
        }
    }

    /**
     * Preload an SFX from assets.
     */
    fun loadSfx(type: SfxType) {
        if (isSilent) return
        try {
            context!!.assets.openFd(type.fileName).use { afd ->
                val id = soundPool!!.load(afd, 1)
                soundIds[type] = id
            }
        } catch (e: Exception) {
            // File not found — silent mode for this SFX
        }
    }

    /**
     * Play an SFX. No-op in silent mode.
     */
    fun playSfx(type: SfxType, pitch: Float = 1f, volume: Float = 1f) {
        if (isSilent) return
        val id = soundIds[type] ?: return
        soundPool?.play(id, volume * sfxVolume, volume * sfxVolume, 1, 0, pitch)
    }

    /**
     * Set SFX volume (0-1).
     */
    fun setSfxVolume(vol: Float) {
        sfxVolume = vol.coerceIn(0f, 1f)
    }

    /**
     * Set BGM volume (0-1).
     */
    fun setBgmVolume(vol: Float) {
        bgmVolume = vol.coerceIn(0f, 1f)
    }

    fun setHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    fun isHapticsEnabled(): Boolean = hapticsEnabled

    /**
     * Release all resources.
     */
    fun release() {
        soundPool?.release()
        soundIds.clear()
        instance = null
    }
}
