package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import com.survivortd.game.data.HeroId

/**
 * Hero Passive System — applies per-hero passive bonuses each tick.
 *
 * Call this once per frame after spawning the player and before
 * other systems that depend on hero stats (Movement, Combat, Towers, etc.).
 *
 * GDD §3.3 — each hero has a unique passive effect.
 */
class HeroPassiveSystem {

    /**
     * Initialize hero-specific fields on the player component.
     * Call this once when starting a new game (not every tick).
     */
    fun initHero(heroId: HeroId, state: GameState) {
        if (state.playerIndex < 0) return
        val player = state.players.getOrNull(state.playerIndex) ?: return

        // Snapshot base values before hero modifications
        player.baseDamageMultiplier = player.damageMult
        player.baseMoveSpeed = player.moveSpeed
        player.basePickupRange = player.pickupRange

        // Reset hero flags
        player.commanderBonus = false
        player.berserkerActive = false
        player.maxTowers = GameConfig.MAX_TOWERS
        player.towerCostMultiplier = 1f
        player.healingBonus = 1f
        player.shieldCooldownTimer = 0f
        player.damageReductionTimer = 0f
        player.armorReduction = 0f

        // Apply one-time hero setup
        when (heroId) {
            HeroId.COMMANDER -> {
                player.commanderBonus = true
            }
            HeroId.BERSERKER -> {
                // Handled in applyPassive each tick based on HP
            }
            HeroId.ENGINEER -> {
                player.maxTowers = GameConfig.MAX_TOWERS + 1  // 9 instead of 8
                player.towerCostMultiplier = 0.8f
            }
            HeroId.MEDIC -> {
                player.regen += 2f  // +2 HP/s on top of base
                player.healingBonus = 1.5f
            }
            HeroId.SCOUT -> {
                player.moveSpeed = player.baseMoveSpeed * 1.25f  // +25%
                player.pickupRange = player.basePickupRange * 1.3f  // +30%
            }
            HeroId.SHIELDER -> {
                // Shield cycle handled per-tick in applyPassive
                player.shieldCooldownTimer = 10f  // Start with cooldown ready to trigger
            }
        }
    }

    /**
     * Apply per-tick passive effects. Call every frame.
     */
    fun applyPassive(heroId: HeroId, state: GameState, dt: Float) {
        if (state.playerIndex < 0) return
        val player = state.players.getOrNull(state.playerIndex) ?: return

        when (heroId) {
            HeroId.COMMANDER -> {
                // Tower damage +10%, range +10% — applied in TowerSystem
                // via player.commanderBonus flag (set in initHero)
            }
            HeroId.BERSERKER -> {
                val hp = state.healths.getOrNull(state.playerIndex) ?: return
                if (hp.currentHp < hp.maxHp * 0.3f) {
                    player.damageMult = player.baseDamageMultiplier * 1.15f
                    player.berserkerActive = true
                } else {
                    player.damageMult = player.baseDamageMultiplier
                    player.berserkerActive = false
                }
            }
            HeroId.ENGINEER -> {
                // One-time setup in initHero, no per-tick logic needed
            }
            HeroId.MEDIC -> {
                // Regen bonus applied in initHero (+2 HP/s)
                // healingBonus flag used by PickupSystem
            }
            HeroId.SCOUT -> {
                // Speed and pickup range applied in initHero
            }
            HeroId.SHIELDER -> {
                player.shieldCooldownTimer -= dt
                if (player.shieldCooldownTimer <= 0f) {
                    // Activate shield: 50% damage reduction for 2s
                    player.shieldCooldownTimer = 10f
                    player.damageReductionTimer = 2f
                }
                if (player.damageReductionTimer > 0f) {
                    player.damageReductionTimer -= dt
                    player.armorReduction = 0.5f  // 50% less damage
                } else {
                    player.armorReduction = 0f
                }
            }
        }
    }
}
