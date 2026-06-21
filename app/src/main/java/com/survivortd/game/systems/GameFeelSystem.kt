package com.survivortd.game.systems

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Game Feel System — screen shake, hit-stop, and camera effects.
 *
 * Screen shake decays exponentially. Hit-stop briefly freezes the game
 * for impact emphasis on heavy hits/boss attacks.
 */
class GameFeelSystem {

    data class ShakeState(
        var intensity: Float = 0f,
        var duration: Float = 0f,
        var maxDuration: Float = 0f
    )

    private val shake = ShakeState()

    /** Current shake offset (updated each frame) */
    var shakeOffsetX: Float = 0f
        private set
    var shakeOffsetY: Float = 0f
        private set

    /** Hit-stop remaining time (game freezes when > 0) */
    var hitStopTimer: Float = 0f
        private set

    val isHitStopActive: Boolean
        get() = hitStopTimer > 0f

    /** Damage flash overlay alpha (0-1) */
    var damageFlash: Float = 0f
        private set

    /**
     * Trigger a screen shake.
     * @param intensity Max pixel offset (e.g. 8f for normal, 20f for boss)
     * @param duration Duration in seconds (e.g. 0.2f normal, 0.5f boss)
     */
    fun shake(intensity: Float, duration: Float) {
        // Only upgrade if new shake is stronger than current
        if (intensity > shake.intensity) {
            shake.intensity = intensity
            shake.maxDuration = duration
            shake.duration = duration
        }
    }

    /**
     * Trigger hit-stop — briefly freezes the game for impact emphasis.
     * @param duration Seconds to freeze (typically 0.03-0.08s)
     */
    fun hitStop(duration: Float) {
        hitStopTimer = maxOf(hitStopTimer, duration)
    }

    /**
     * Trigger a damage flash overlay.
     * @param intensity Alpha 0-1 (typically 0.3-0.5)
     */
    fun flashDamage(intensity: Float = 0.4f) {
        damageFlash = maxOf(damageFlash, intensity)
    }

    /**
     * Convenience: light hit (bullet hit, small enemy death)
     */
    fun onLightHit() {
        shake(intensity = 3f, duration = 0.1f)
    }

    /**
     * Convenience: heavy hit (brute attack, cannon fire)
     */
    fun onHeavyHit() {
        shake(intensity = 8f, duration = 0.2f)
        hitStop(0.03f)
    }

    /**
     * Convenience: explosion (bomber death, rocket hit)
     */
    fun onExplosion() {
        shake(intensity = 12f, duration = 0.3f)
        hitStop(0.05f)
    }

    /**
     * Convenience: boss attack
     */
    fun onBossAttack() {
        shake(intensity = 20f, duration = 0.5f)
        hitStop(0.08f)
        flashDamage(0.6f)
    }

    /**
     * Convenience: player takes damage
     */
    fun onPlayerHit() {
        shake(intensity = 5f, duration = 0.15f)
        flashDamage(0.4f)
    }

    /**
     * Update shake and hit-stop timers.
     * @param dt Delta time in seconds
     * @return Effective delta time (0 if hit-stopped, dt otherwise)
     */
    fun update(dt: Float): Float {
        // Damage flash decays
        if (damageFlash > 0f) {
            damageFlash = (damageFlash - dt * 2f).coerceAtLeast(0f)
        }

        // Hit-stop
        if (hitStopTimer > 0f) {
            hitStopTimer -= dt
            if (hitStopTimer > 0f) {
                shakeOffsetX = 0f
                shakeOffsetY = 0f
                return 0f  // Freeze game
            }
        }

        // Screen shake
        if (shake.duration > 0f) {
            shake.duration -= dt
            val progress = shake.duration / shake.maxDuration
            val currentIntensity = shake.intensity * progress * progress // Quadratic decay
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            shakeOffsetX = cos(angle) * currentIntensity
            shakeOffsetY = sin(angle) * currentIntensity

            if (shake.duration <= 0f) {
                shake.intensity = 0f
                shakeOffsetX = 0f
                shakeOffsetY = 0f
            }
        } else {
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        return dt
    }

    /** Reset all effects */
    fun reset() {
        shake.intensity = 0f
        shake.duration = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f
        hitStopTimer = 0f
        damageFlash = 0f
    }
}
