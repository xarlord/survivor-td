package com.survivortd.game.systems

import com.survivortd.game.components.PositionComponent
import com.survivortd.game.core.GameState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Particle System — visual effects for hits, explosions, deaths, and pickups.
 *
 * Uses a flat-array ECS approach: parallel lists for particle data.
 * Particles are lightweight — just position, velocity, color, lifetime, size.
 */
class ParticleSystem(private val state: GameState) {

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float,
        var color: Int,
        var type: ParticleType,
        var gravity: Float = 0f
    )

    enum class ParticleType {
        SPARK,      // Small bright dot, fast fade
        SMOKE,      // Grey expanding circle, slow rise
        BLOOD,      // Red droplet, gravity
        EXPLOSION,  // Orange/yellow expanding ring
        FROST,      // Cyan crystal shard
        POISON,     // Green bubble
        ELECTRIC,   // Yellow jagged line
        GEM_SPARKLE,// Blue sparkle for XP pickup
        HEAL,       // Green cross sparkle
        DEATH_BURST // Multiple particles radiating outward
    }

    companion object {
        private const val MAX_PARTICLES = 500
    }

    val particles = mutableListOf<Particle>()

    /** Total particles spawned (for stats) */
    var totalSpawned = 0
        private set

    /**
     * Spawn a burst of particles at a position.
     */
    fun spawnBurst(
        x: Float,
        y: Float,
        type: ParticleType,
        count: Int = 8,
        speed: Float = 100f,
        color: Int = 0xFFFFFFFF.toInt(),
        life: Float = 0.5f,
        size: Float = 4f
    ) {
        repeat(count) {
            if (particles.size >= MAX_PARTICLES) return

            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            val spd = speed * (0.5f + Random.nextFloat() * 0.5f)
            val pColor = when (type) {
                ParticleType.SPARK -> 0xFFFFEE58.toInt()
                ParticleType.SMOKE -> 0xFF9E9E9E.toInt()
                ParticleType.BLOOD -> 0xFFD32F2F.toInt()
                ParticleType.EXPLOSION -> if (Random.nextBoolean()) 0xFFFF6F00.toInt() else 0xFFFFEB3B.toInt()
                ParticleType.FROST -> 0xFF81D4FA.toInt()
                ParticleType.POISON -> 0xFF76FF03.toInt()
                ParticleType.ELECTRIC -> 0xFFFFFF00.toInt()
                ParticleType.GEM_SPARKLE -> 0xFF42A5F5.toInt()
                ParticleType.HEAL -> 0xFF66BB6A.toInt()
                ParticleType.DEATH_BURST -> color
            }

            particles.add(Particle(
                x = x,
                y = y,
                vx = cos(angle) * spd,
                vy = sin(angle) * spd,
                life = life * (0.7f + Random.nextFloat() * 0.6f),
                maxLife = life,
                size = size * (0.7f + Random.nextFloat() * 0.6f),
                color = pColor,
                type = type,
                gravity = when (type) {
                    ParticleType.BLOOD -> 200f
                    ParticleType.SMOKE -> -30f  // Rises
                    else -> 0f
                }
            ))
            totalSpawned++
        }
    }

    /**
     * Convenience: hit spark when projectile hits enemy.
     */
    fun onHit(x: Float, y: Float) {
        spawnBurst(x, y, ParticleType.SPARK, count = 4, speed = 80f, life = 0.2f, size = 3f)
    }

    /**
     * Convenience: enemy death burst.
     */
    fun onEnemyDeath(x: Float, y: Float) {
        spawnBurst(x, y, ParticleType.BLOOD, count = 6, speed = 120f, life = 0.4f, size = 4f)
        spawnBurst(x, y, ParticleType.DEATH_BURST, count = 8, speed = 150f, color = 0xFFFF5252.toInt(), life = 0.3f, size = 3f)
    }

    /**
     * Convenience: explosion (cannon, bomber, rocket).
     */
    fun onExplosion(x: Float, y: Float) {
        spawnBurst(x, y, ParticleType.EXPLOSION, count = 16, speed = 200f, life = 0.5f, size = 6f)
        spawnBurst(x, y, ParticleType.SMOKE, count = 6, speed = 40f, life = 0.8f, size = 8f)
    }

    /**
     * Convenience: gem pickup sparkle.
     */
    fun onGemPickup(x: Float, y: Float) {
        spawnBurst(x, y, ParticleType.GEM_SPARKLE, count = 3, speed = 50f, life = 0.3f, size = 2f)
    }

    /**
     * Convenience: heal pickup.
     */
    fun onHeal(x: Float, y: Float) {
        spawnBurst(x, y, ParticleType.HEAL, count = 6, speed = 30f, life = 0.6f, size = 4f)
    }

    /**
     * Update all particles — move, apply gravity, decrease life, remove dead.
     * (#115) Uses index-based backward iteration to avoid iterator allocation.
     */
    fun update(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            // Friction
            p.vx *= 0.95f
            p.vy *= 0.95f
            p.life -= dt
            if (p.life <= 0f) {
                particles.removeAt(i)
            }
            i--
        }
    }

    /** Clear all particles */
    fun clear() {
        particles.clear()
    }

    /** Active particle count */
    fun count(): Int = particles.size
}
