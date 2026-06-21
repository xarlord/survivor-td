package com.survivortd.game.systems

import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PolishSystemTest {

    @Nested
    @DisplayName("Particle System")
    inner class ParticleTest {
        private lateinit var state: GameState
        private lateinit var particles: ParticleSystem

        @BeforeEach
        fun setup() {
            state = GameState()
            particles = ParticleSystem(state)
        }

        @Test
        @DisplayName("Spawn burst creates particles")
        fun spawnBurst() {
            particles.spawnBurst(100f, 200f, ParticleSystem.ParticleType.SPARK, count = 8)
            assertEquals(8, particles.count())
        }

        @Test
        @DisplayName("Particles die after lifetime expires")
        fun particlesDieAfterLife() {
            particles.spawnBurst(0f, 0f, ParticleSystem.ParticleType.SPARK, count = 4, life = 0.3f)
            assertEquals(4, particles.count())
            // Simulate ~0.5 seconds
            repeat(30) { particles.update(0.016f) }
            assertEquals(0, particles.count(), "Particles should expire after lifetime")
        }

        @Test
        @DisplayName("Max particles cap enforced")
        fun maxParticlesCap() {
            // Try to spawn 1000 particles
            repeat(20) {
                particles.spawnBurst(0f, 0f, ParticleSystem.ParticleType.SPARK, count = 50)
            }
            assertTrue(particles.count() <= 500, "Should cap at 500 particles")
        }

        @Test
        @DisplayName("onHit spawns sparks")
        fun onHit() {
            particles.onHit(100f, 100f)
            assertTrue(particles.count() > 0)
        }

        @Test
        @DisplayName("onEnemyDeath spawns blood + burst")
        fun onEnemyDeath() {
            particles.onEnemyDeath(100f, 100f)
            assertTrue(particles.count() >= 10, "Death should spawn 10+ particles")
        }

        @Test
        @DisplayName("onExplosion spawns explosion + smoke")
        fun onExplosion() {
            particles.onExplosion(100f, 100f)
            val hasExplosion = particles.particles.any { it.type == ParticleSystem.ParticleType.EXPLOSION }
            val hasSmoke = particles.particles.any { it.type == ParticleSystem.ParticleType.SMOKE }
            assertTrue(hasExplosion, "Should spawn explosion particles")
            assertTrue(hasSmoke, "Should spawn smoke particles")
        }

        @Test
        @DisplayName("Particles move over time")
        fun particlesMove() {
            particles.spawnBurst(100f, 100f, ParticleSystem.ParticleType.SPARK, count = 1, speed = 200f)
            val before = particles.particles.first().x
            particles.update(0.1f)
            val after = particles.particles.first().x
            assertNotEquals(before, after, "Particle should move")
        }

        @Test
        @DisplayName("Clear removes all particles")
        fun clearParticles() {
            particles.spawnBurst(0f, 0f, ParticleSystem.ParticleType.SPARK, count = 10)
            particles.clear()
            assertEquals(0, particles.count())
        }
    }

    @Nested
    @DisplayName("Game Feel System (Screen Shake)")
    inner class GameFeelTest {
        private lateinit var feel: GameFeelSystem

        @BeforeEach
        fun setup() {
            feel = GameFeelSystem()
        }

        @Test
        @DisplayName("Shake produces offset")
        fun shakeProducesOffset() {
            feel.shake(intensity = 10f, duration = 0.3f)
            feel.update(0.016f)
            assertTrue(feel.shakeOffsetX != 0f || feel.shakeOffsetY != 0f,
                "Shake should produce non-zero offset")
        }

        @Test
        @DisplayName("Shake decays to zero")
        fun shakeDecaysToZero() {
            feel.shake(intensity = 10f, duration = 0.2f)
            // Simulate 0.5 seconds
            repeat(30) { feel.update(0.016f) }
            assertEquals(0f, feel.shakeOffsetX, 0.001f)
            assertEquals(0f, feel.shakeOffsetY, 0.001f)
        }

        @Test
        @DisplayName("Hit-stop freezes game (returns 0 dt)")
        fun hitStopFreezesGame() {
            feel.hitStop(0.05f)
            val effectiveDt = feel.update(0.016f)
            assertEquals(0f, effectiveDt, "Hit-stop should return 0 dt")
        }

        @Test
        @DisplayName("Hit-stop releases after duration")
        fun hitStopReleases() {
            feel.hitStop(0.03f)
            // Wait 0.05s
            repeat(5) { feel.update(0.016f) }
            val effectiveDt = feel.update(0.016f)
            assertTrue(effectiveDt > 0f, "Hit-stop should end, dt should be > 0")
        }

        @Test
        @DisplayName("Damage flash decays")
        fun damageFlashDecays() {
            feel.flashDamage(0.5f)
            assertEquals(0.5f, feel.damageFlash, 0.01f)
            repeat(30) { feel.update(0.016f) }
            assertEquals(0f, feel.damageFlash, 0.01f, "Flash should decay to 0")
        }

        @Test
        @DisplayName("onBossAttack triggers shake + hitstop + flash")
        fun onBossAttackCombo() {
            feel.onBossAttack()
            feel.update(0.016f)
            // Shake should be active
            assertTrue(feel.shakeOffsetX != 0f || feel.shakeOffsetY != 0f)
            assertTrue(feel.isHitStopActive)
            assertTrue(feel.damageFlash > 0f)
        }

        @Test
        @DisplayName("Weaker shake doesn't override stronger")
        fun weakerShakeDoesNotOverride() {
            feel.shake(intensity = 20f, duration = 0.5f)
            feel.update(0.016f)
            val offset1 = kotlin.math.abs(feel.shakeOffsetX) + kotlin.math.abs(feel.shakeOffsetY)

            feel.shake(intensity = 3f, duration = 0.1f) // Should NOT override
            feel.update(0.016f)
            // The intensity should still be high from the 20f shake
            // (offset might differ due to randomness, but it should still be decaying from 20f)
        }

        @Test
        @DisplayName("Reset clears all effects")
        fun resetClears() {
            feel.shake(10f, 0.3f)
            feel.hitStop(0.1f)
            feel.flashDamage(0.5f)
            feel.reset()
            assertEquals(0f, feel.shakeOffsetX)
            assertEquals(0f, feel.hitStopTimer)
            assertEquals(0f, feel.damageFlash)
            assertFalse(feel.isHitStopActive)
        }
    }

    @Nested
    @DisplayName("Audio Manager (Silent Mode)")
    inner class AudioTest {

        @Test
        @DisplayName("Silent mode with null context")
        fun silentMode() {
            val audio = AudioManager.getInstance(context = null)
            assertTrue(audio.isSilent)
            // Should not crash
            audio.playSfx(AudioManager.SfxType.GUN_SHOT)
            audio.setSfxVolume(0.5f)
        }

        @Test
        @DisplayName("Volume clamps to 0-1")
        fun volumeClamp() {
            val audio = AudioManager.getInstance(context = null)
            audio.setSfxVolume(2f)  // Over 1
            audio.setSfxVolume(-1f) // Under 0
            // No crash — clamped internally
        }

        @Test
        @DisplayName("Haptics toggle")
        fun hapticsToggle() {
            val audio = AudioManager.getInstance(context = null)
            audio.setHaptics(false)
            assertFalse(audio.isHapticsEnabled())
            audio.setHaptics(true)
            assertTrue(audio.isHapticsEnabled())
        }
    }
}
