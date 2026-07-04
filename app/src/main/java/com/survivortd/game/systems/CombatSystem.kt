package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Combat system — handles enemy contact damage to player, HP regen,
 * invincibility frames, and death detection.
 *
 * Called every physics tick after movement.
 */
class CombatSystem(
    private val state: GameState,
    private val gameFeelSystem: GameFeelSystem? = null
) {
    /**
     * Process combat for this tick.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        if (state.playerIndex < 0 || state.playerIndex >= state.positions.size) return

        updatePlayerInvincibility(dt)
        updatePlayerRegen(dt)
        processEnemyContact(dt)
    }

    /**
     * Tick down invincibility timer.
     */
    private fun updatePlayerInvincibility(dt: Float) {
        val health = state.healths.getOrNull(state.playerIndex) ?: return
        if (health.invincible) {
            health.invincibleTimer -= dt
            if (health.invincibleTimer <= 0f) {
                health.invincible = false
                health.invincibleTimer = 0f
            }
        }
    }

    /**
     * HP regeneration per second.
     */
    private fun updatePlayerRegen(dt: Float) {
        val health = state.healths.getOrNull(state.playerIndex) ?: return
        val player = state.players.getOrNull(state.playerIndex) ?: return
        if (health.isDead) return
        if (health.currentHp < health.maxHp) {
            health.currentHp = (health.currentHp + player.regen * dt).coerceAtMost(health.maxHp)
        }
        state.healthPercent = health.hpPercent
    }

    /**
     * Check all enemies for contact with player and apply damage.
     */
    private fun processEnemyContact(dt: Float) {
        val playerPos = state.positions[state.playerIndex]
        val health = state.healths.getOrNull(state.playerIndex) ?: return
        if (health.isDead || health.invincible) return

        val playerRadius = if (state.playerIndex < state.renders.size)
            state.renders[state.playerIndex].radius else GameConfig.PLAYER_HITBOX_RADIUS

        for (i in state.enemies.indices) {
            // Skip player entity and non-enemy placeholders
            if (i == state.playerIndex) continue
            if (i >= state.tags.size) break
            if (state.tags[i].tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (i >= state.positions.size) break
            val enemyPos = state.positions[i]
            val enemyRadius = if (i < state.renders.size) state.renders[i].radius else 12f

            val dx = enemyPos.x - playerPos.x
            val dy = enemyPos.y - playerPos.y
            val distSq = dx * dx + dy * dy
            val touchDist = playerRadius + enemyRadius

            if (distSq <= touchDist * touchDist) {
                // Contact damage scaled by elapsed time
                val minute = state.elapsedSeconds / 60f
                val damageScale = 1f + minute * GameConfig.ENEMY_DAMAGE_SCALE
                val baseDamage = getEnemyContactDamage(i)
                val finalDamage = baseDamage * damageScale

                // Apply armor reduction
                val armorReduction = health.armor / (health.armor + 10f) // diminishing returns
                val mitigatedDamage = finalDamage * (1f - armorReduction)

                // Dodge check
                if (kotlin.random.Random.nextFloat() < health.dodge) continue

                health.currentHp -= mitigatedDamage
                health.invincible = true
                health.invincibleTimer = INVINCIBILITY_FRAMES_SECONDS

                // VFX: player hit screen shake + damage flash
                gameFeelSystem?.onPlayerHit()

                // Knockback player away from enemy
                val dist = sqrt(distSq.coerceAtLeast(0.01f))
                val knockback = GameConfig.PLAYER_KNOCKBACK
                playerPos.x -= (dx / dist) * knockback * dt * 10f
                playerPos.y -= (dy / dist) * knockback * dt * 10f

                state.healthPercent = health.hpPercent

                if (health.isDead) {
                    state.isGameOver = true
                }
                break // Only one enemy hits per tick
            }
        }
    }

    /**
     * Get contact damage for an enemy at a given index.
     */
    private fun getEnemyContactDamage(index: Int): Float {
        if (index >= state.enemies.size) return DEFAULT_CONTACT_DAMAGE
        return when (state.enemies[index].type) {
            com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE -> 8f
            com.survivortd.game.components.EnemyComponent.EnemyData.RUNNER -> 6f
            com.survivortd.game.components.EnemyComponent.EnemyData.BRUTE -> 20f
            com.survivortd.game.components.EnemyComponent.EnemyData.SPITTER -> 10f
            com.survivortd.game.components.EnemyComponent.EnemyData.BOMBER -> 15f
            com.survivortd.game.components.EnemyComponent.EnemyData.HEALER -> 5f
            com.survivortd.game.components.EnemyComponent.EnemyData.SHIELDER -> 12f
            com.survivortd.game.components.EnemyComponent.EnemyData.FLYER -> 7f
            com.survivortd.game.components.EnemyComponent.EnemyData.ELITE -> 25f
            com.survivortd.game.components.EnemyComponent.EnemyData.BOSS -> 40f
        }
    }

    companion object {
        const val INVINCIBILITY_FRAMES_SECONDS = 0.5f
        private const val DEFAULT_CONTACT_DAMAGE = 10f
    }
}
