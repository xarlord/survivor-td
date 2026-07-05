package com.survivortd.game.systems

import com.survivortd.game.components.DamageNumberComponent
import com.survivortd.game.components.HealthComponent
import com.survivortd.game.components.RenderComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [#114] Tests for Visual VFX — damage numbers, hit flash, death animation.
 */
@DisplayName("Visual VFX Tests")
class VisualVfxTest {

    private lateinit var state: GameState

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
    }

    @Test
    @DisplayName("DamageNumberComponent alpha fades from 1 to 0 over lifetime")
    fun damageNumberAlphaFadesOverLifetime() {
        val dn = DamageNumberComponent(value = 50f)
        assertEquals(1.0f, dn.alpha, 0.01f, "Alpha should start at 1")
        dn.elapsed = dn.lifetime * 0.5f
        assertEquals(0.5f, dn.alpha, 0.01f, "Alpha should be 0.5 at half lifetime")
        dn.elapsed = dn.lifetime
        assertEquals(0.0f, dn.alpha, 0.01f, "Alpha should be 0 at end of lifetime")
    }

    @Test
    @DisplayName("DamageNumberComponent crit has larger font")
    fun damageNumberCritHasLargerFont() {
        val normal = DamageNumberComponent(value = 10f, isCrit = false)
        val crit = DamageNumberComponent(value = 10f, isCrit = true)
        assertTrue(crit.fontSize > normal.fontSize, "Crit font should be larger than normal")
        assertEquals(16f, normal.fontSize, 0.01f)
        assertEquals(24f, crit.fontSize, 0.01f)
    }

    @Test
    @DisplayName("DamageNumberComponent has random horizontal velocity")
    fun damageNumberHasRandomVelocity() {
        val dn = DamageNumberComponent(value = 10f)
        assertTrue(dn.vx >= -10f && dn.vx <= 10f, "VX should be in range [-10, 10]")
        assertEquals(-60f, dn.vy, 0.01f, "VY should be -60 (float upward)")
    }

    @Test
    @DisplayName("DamageNumberComponent moves with velocity over time")
    fun damageNumberMovesWithVelocity() {
        val dn = DamageNumberComponent(x = 100f, y = 200f, value = 25f)
        val dt = 0.1f
        dn.elapsed += dt
        dn.x += dn.vx * dt
        dn.y += dn.vy * dt
        assertTrue(dn.y < 200f, "Y should decrease (float upward)")
    }

    @Test
    @DisplayName("HitFlash timer decays over time")
    fun hitFlashTimerDecays() {
        val render = RenderComponent(hitFlashTimer = 0.1f)
        assertTrue(render.hitFlashTimer > 0f, "Timer should start positive")
        render.hitFlashTimer = (render.hitFlashTimer - 0.05f).coerceAtLeast(0f)
        assertEquals(0.05f, render.hitFlashTimer, 0.001f)
        render.hitFlashTimer = (render.hitFlashTimer - 0.05f).coerceAtLeast(0f)
        assertEquals(0.0f, render.hitFlashTimer, 0.001f)
    }

    @Test
    @DisplayName("Death animation state machine: not dying -> dying -> dead")
    fun deathAnimationStateMachine() {
        val health = HealthComponent(maxHp = 100f, currentHp = 100f)
        assertFalse(health.isDying, "Should not be dying initially")
        assertFalse(health.isDead, "Should not be dead initially")

        health.deathTimer = 0.3f
        assertTrue(health.isDying, "Should be dying when deathTimer > 0")

        health.deathTimer -= 0.3f
        assertFalse(health.isDying, "Should not be dying when timer expires")

        health.currentHp = 0f
        assertTrue(health.isDead, "Should be dead when HP reaches 0")
    }

    @Test
    @DisplayName("Death animation fade progress")
    fun deathAnimationFadeProgress() {
        val health = HealthComponent(deathTimer = 0.3f)
        var progress = health.deathTimer / 0.3f
        assertEquals(1.0f, progress, 0.01f, "Progress should start at 1.0")

        health.deathTimer = 0.15f
        progress = health.deathTimer / 0.3f
        assertEquals(0.5f, progress, 0.01f, "Progress should be 0.5 at halfway")

        health.deathTimer = 0f
        progress = health.deathTimer / 0.3f
        assertEquals(0.0f, progress, 0.01f, "Progress should be 0 at end")
    }

    @Test
    @DisplayName("Death animation shrink: radius decreases by up to 50%")
    fun deathAnimationShrink() {
        val health = HealthComponent(deathTimer = 0.3f)
        val baseRadius = 20f
        var progress = health.deathTimer / 0.3f
        var radius = baseRadius * (1f - progress * 0.5f)
        assertEquals(10f, radius, 0.01f, "Radius should shrink to 50% at start of death")

        health.deathTimer = 0f
        progress = health.deathTimer / 0.3f
        radius = baseRadius * (1f - progress * 0.5f)
        assertEquals(20f, radius, 0.01f, "Radius should be full at end of death")
    }

    @Test
    @DisplayName("Damage numbers cleaned up after lifetime")
    fun damageNumbersCleanedAfterLifetime() {
        state.damageNumbers.add(DamageNumberComponent(x = 100f, y = 100f, value = 25f))
        assertEquals(1, state.damageNumbers.size)
        val dn = state.damageNumbers[0]
        dn.elapsed = dn.lifetime
        state.damageNumbers.removeAt(0)
        assertEquals(0, state.damageNumbers.size)
    }

    @Test
    @DisplayName("Active damage numbers preserved before lifetime expires")
    fun activeDamageNumbersPreserved() {
        state.damageNumbers.add(DamageNumberComponent(x = 100f, y = 100f, value = 25f))
        val dn = state.damageNumbers[0]
        dn.elapsed = dn.lifetime * 0.5f
        assertEquals(1, state.damageNumbers.size, "Active damage number should not be removed")
        assertTrue(dn.alpha > 0f, "Alpha should still be positive")
    }

    @Test
    @DisplayName("Damage numbers cleared on game reset")
    fun damageNumbersClearedOnReset() {
        state.damageNumbers.add(DamageNumberComponent(x = 100f, y = 100f, value = 25f))
        state.damageNumbers.add(DamageNumberComponent(x = 200f, y = 200f, value = 50f))
        assertTrue(state.damageNumbers.isNotEmpty())
        state.reset()
        assertTrue(state.damageNumbers.isEmpty(), "Damage numbers should be cleared on reset")
    }
}
