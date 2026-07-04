package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.StatusEffectType
import com.survivortd.game.core.GameState

/**
 * Status Effect System — processes all active status effects on enemies each tick.
 *
 * Handles:
 * - DoT effects (BURN, POISON, BLEED): deal damage per tick interval
 * - Hard CC (FREEZE, STUN): stop enemy movement (via setting velocity to 0)
 * - Soft CC (SLOW, SLOW_ATTACK): reduce movement/attack speed
 *
 * This system is critical — without it, status effects are applied but never
 * processed, making 5/7 status types (BURN, POISON, BLEED, STUN, SLOW_ATTACK) dead.
 *
 * Called every physics tick after MovementSystem (to override enemy velocities
 * for FREEZE/STUN) and before ProjectileSystem (so DoTs can apply kill credits).
 */
class StatusEffectSystem(
    private val state: GameState
) {
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        for (i in state.statusEffects.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (i >= state.healths.size || state.healths[i].isDead) continue

            val se = state.statusEffects[i]
            if (se.effects.isEmpty()) continue

            val enemy = state.enemies.getOrNull(i)
            val vel = state.velocities.getOrNull(i)

            val iterator = se.effects.iterator()
            while (iterator.hasNext()) {
                val effect = iterator.next()
                effect.duration -= dt

                if (effect.duration <= 0f) {
                    iterator.remove()
                    continue
                }

                when (effect.type) {
                    StatusEffectType.BURN, StatusEffectType.POISON, StatusEffectType.BLEED -> {
                        // DoT: deal damage every tick interval
                        effect.tickTimer -= dt
                        if (effect.tickTimer <= 0f) {
                            effect.tickTimer = effect.tickInterval
                            val dotDamage = effect.magnitude
                            // POISON ignores armor (per GDD §3.3)
                            if (effect.type == StatusEffectType.POISON) {
                                state.healths[i].currentHp -= dotDamage
                            } else {
                                // Armor (#108: flat subtraction per GDD §3.2)
                                val armor = if (i < state.healths.size) state.healths[i].armor else 0f
                                val mitigated = GameConfig.armorReduction(dotDamage, armor)
                                state.healths[i].currentHp -= mitigated
                            }
                        }
                    }
                    StatusEffectType.FREEZE, StatusEffectType.STUN -> {
                        // Hard CC: zero out velocity so enemy can't move
                        if (vel != null) {
                            vel.x = 0f
                            vel.y = 0f
                        }
                    }
                    StatusEffectType.SLOW, StatusEffectType.SLOW_ATTACK -> {
                        // Soft CC: reduce velocity magnitude
                        if (vel != null) {
                            val mult = (1f - effect.magnitude).coerceAtLeast(0.1f)
                            vel.x *= mult
                            vel.y *= mult
                        }
                    }
                }
            }
        }
    }
}
