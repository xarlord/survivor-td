package com.survivortd.game.systems

import com.survivortd.game.components.DamageNumberComponent
import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.TagComponent
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
    private val gameFeelSystem: GameFeelSystem? = null,
    var meta: MetaProgression? = null
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
        processBomberExplosions()
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

                // Apply armor reduction (#108: flat subtraction per GDD §3.2)
                val mitigatedDamage = GameConfig.armorReduction(finalDamage, health.armor)

                // Dodge check
                if (kotlin.random.Random.nextFloat() < health.dodge) continue

                health.currentHp -= mitigatedDamage
                health.invincible = true
                health.invincibleTimer = INVINCIBILITY_FRAMES_SECONDS

                // [#114] VFX: damage number on player
                val playerPos = state.positions[state.playerIndex]
                state.damageNumbers.add(DamageNumberComponent(
                    x = playerPos.x, y = playerPos.y - 10f,
                    value = mitigatedDamage
                ))
                if (state.playerIndex < state.renders.size) {
                    state.renders[state.playerIndex].hitFlashTimer = 0.1f
                }

                // VFX: player hit screen shake + damage flash
                gameFeelSystem?.onPlayerHit()
                // [#113] SFX: player hit
                AudioManager.getInstance().playSfx(AudioManager.SfxType.PLAYER_HIT)

                // Knockback player away from enemy
                val dist = sqrt(distSq.coerceAtLeast(0.01f))
                val knockback = GameConfig.PLAYER_KNOCKBACK
                playerPos.x -= (dx / dist) * knockback * dt * 10f
                playerPos.y -= (dy / dist) * knockback * dt * 10f

                state.healthPercent = health.hpPercent

                if (health.isDead) {
                    // Check for extra lives from MetaProgression
                    val player = state.players.getOrNull(state.playerIndex)
                    val currentMeta = meta
                    if (currentMeta != null && currentMeta.extraLifeLevel > 0 && player != null && !player.hasRevived) {
                        currentMeta.extraLifeLevel--
                        health.currentHp = health.maxHp * 0.5f
                        player.hasRevived = true
                        health.invincible = true
                        health.invincibleTimer = 2f
                    } else {
                        health.currentHp = 0f
                        state.isGameOver = true
                    }
                }
                break // Only one enemy hits per tick
            }
        }
    }

    /**
     * Get contact damage for an enemy at a given index.
     * (#110: Uses baseDamage from EnemyType enum for consistency)
     */
    private fun getEnemyContactDamage(index: Int): Float {
        if (index >= state.enemies.size) return DEFAULT_CONTACT_DAMAGE
        return when (state.enemies[index].type) {
            EnemyComponent.EnemyData.ZOMBIE -> com.survivortd.game.config.EnemyType.ZOMBIE.baseDamage
            EnemyComponent.EnemyData.RUNNER -> com.survivortd.game.config.EnemyType.RUNNER.baseDamage
            EnemyComponent.EnemyData.BRUTE -> com.survivortd.game.config.EnemyType.BRUTE.baseDamage
            EnemyComponent.EnemyData.SPITTER -> com.survivortd.game.config.EnemyType.SPITTER.baseDamage
            EnemyComponent.EnemyData.BOMBER -> com.survivortd.game.config.EnemyType.BOMBER.baseDamage
            EnemyComponent.EnemyData.HEALER -> com.survivortd.game.config.EnemyType.HEALER.baseDamage
            EnemyComponent.EnemyData.SHIELDER -> com.survivortd.game.config.EnemyType.SHIELDER.baseDamage
            EnemyComponent.EnemyData.FLYER -> com.survivortd.game.config.EnemyType.FLYER.baseDamage
            EnemyComponent.EnemyData.ELITE -> com.survivortd.game.config.EnemyType.ELITE.baseDamage
            EnemyComponent.EnemyData.BOSS -> com.survivortd.game.config.EnemyType.BOSS.baseDamage
        }
    }

    /**
     * (#110) Process bomber explosions: bombers in SPECIAL state with aiTimer > 0.5s
     * deal AoE damage to all entities within 80px and then die.
     */
    private fun processBomberExplosions() {
        for (i in state.enemies.indices) {
            if (i >= state.healths.size || state.healths[i].isDead) continue
            if (i >= state.tags.size || state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            val enemy = state.enemies[i]
            if (enemy.type != EnemyComponent.EnemyData.BOMBER) continue
            if (enemy.aiState != EnemyComponent.AiState.SPECIAL) continue
            if (enemy.aiTimer <= 0.5f) continue

            // AoE damage to all entities within 80px
            val bomberPos = state.positions[i]
            for (j in state.positions.indices) {
                if (j >= state.healths.size || state.healths[j].isDead) continue
                if (j >= state.tags.size || state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
                if (j == i) continue
                val dx = state.positions[j].x - bomberPos.x
                val dy = state.positions[j].y - bomberPos.y
                if (dx * dx + dy * dy < 6400f) { // 80px radius
                    state.healths[j].currentHp -= 40f
                }
            }
            // Bomber dies
            state.healths[i].currentHp = 0f
        }
    }

    companion object {
        const val INVINCIBILITY_FRAMES_SECONDS = 0.5f
        private const val DEFAULT_CONTACT_DAMAGE = 10f
    }
}
