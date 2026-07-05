package com.survivortd.game.systems

import com.survivortd.game.components.DamageNumberComponent
import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.ProjectileComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState

/**
 * Projectile System — moves projectiles, handles enemy collisions,
 * pierce counting, AoE explosions, boomerang return, mine detonation.
 */
class ProjectileSystem(
    private val state: GameState,
    private val particleSystem: ParticleSystem? = null,
    private val gameFeelSystem: GameFeelSystem? = null
) {
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        if (state.playerIndex < 0) return

        var i = 0
        while (i < state.projectiles.size) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.PROJECTILE) {
                i++
                continue
            }
            if (i >= state.projectiles.size) break

            val proj = state.projectiles[i]
            val pos = state.positions[i]
            val vel = state.velocities[i]

            // Lifetime countdown
            proj.lifetime -= dt
            if (proj.lifetime <= 0f) {
                // Boomerang: reverse direction and re-check lifetime
                if (proj.isBoomerang && vel.x != 0f) {
                    // Reverse velocity toward player
                    val playerPos = state.positions[state.playerIndex]
                    val dx = playerPos.x - pos.x
                    val dy = playerPos.y - pos.y
                    val distSq = dx * dx + dy * dy
                    // (#115) Use squared distance comparison instead of sqrt
                    if (distSq < 900f) { // 30^2 = 900
                        // Reached player — expire
                        state.healths[i].currentHp = 0f
                        i++
                        continue
                    }
                    val dist = kotlin.math.sqrt(distSq)
                    vel.x = (dx / dist) * kotlin.math.abs(vel.x).coerceAtLeast(200f)
                    vel.y = (dy / dist) * kotlin.math.abs(vel.y).coerceAtLeast(200f)
                    proj.lifetime = 1.5f
                } else {
                    state.healths[i].currentHp = 0f
                    i++
                    continue
                }
            }

            // Move projectile with sub-stepped collision detection
            // (prevents fast projectiles from tunneling through enemies)
            val moveDist = kotlin.math.sqrt(vel.x * vel.x + vel.y * vel.y) * dt
            val subSteps = kotlin.math.ceil(moveDist / 20f).toInt().coerceIn(1, 10)
            val subDt = dt / subSteps
            var hitSomething = false
            for (step in 0 until subSteps) {
                pos.x += vel.x * subDt
                pos.y += vel.y * subDt

                // Mine: check for nearby enemies → detonate
                if (proj.isMine) {
                    val detonated = checkMineDetonation(i, proj, subDt)
                    if (detonated) {
                        state.healths[i].currentHp = 0f
                        hitSomething = true
                        break
                    }
                } else {
                    // Standard projectile: check enemy collisions
                    checkProjectileCollisions(i, proj, pos.x, pos.y)
                    if (state.healths[i].isDead) {
                        hitSomething = true
                        break
                    }
                }
            }
            if (hitSomething) {
                i++
                continue
            }

            // Remove if out of world bounds
            if (pos.x < -200f || pos.x > 2000f || pos.y < -200f || pos.y > 2000f) {
                state.healths[i].currentHp = 0f
            }

            i++
        }
    }

    /**
     * Check collision between projectile and all enemies.
     * (#115) Optimized: uses squared distances, pre-computed hit radius squared,
     * and early exit if projectile is moving away from all remaining enemies.
     */
    private fun checkProjectileCollisions(
        projIndex: Int,
        proj: ProjectileComponent,
        checkX: Float,
        checkY: Float
    ) {
        // (#115) Pre-compute hit radius squared
        val projRadius = if (projIndex < state.renders.size) state.renders[projIndex].radius else 4f
        val hitRadius = 20f + projRadius
        val hitRadiusSq = hitRadius * hitRadius

        // (#115) Get projectile velocity direction for early exit optimization
        val vel = if (projIndex < state.velocities.size) state.velocities[projIndex] else null
        val hasVelocity = vel != null && (vel.x != 0f || vel.y != 0f)

        for (j in state.enemies.indices) {
            if (j >= state.tags.size) break
            if (state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[j].isDead) continue
            if (projIndex == j) continue

            // Skip already-hit enemies (for pierce)
            if (proj.hitEntityIds.contains(j)) continue

            val enemyPos = state.positions[j]
            val dx = enemyPos.x - checkX
            val dy = enemyPos.y - checkY
            val distSq = dx * dx + dy * dy

            // (#115) Early exit: if projectile moving in a direction and enemy is behind it
            // + beyond hit radius, we can stop checking if distance is increasing
            if (hasVelocity && distSq > hitRadiusSq * 16f) {
                val velRef = vel!!
                // Enemy is behind the projectile direction
                if (dx * velRef.x + dy * velRef.y < 0f) continue
            }

            if (distSq <= hitRadiusSq) {
                // Hit!
                dealProjectileDamage(j, proj, projIndex)

                // Apply on-hit status effect if set
                proj.onHitEffect?.let { statusType ->
                    applyStatusEffect(j, statusType, proj.onHitEffectDuration, proj.onHitEffectMagnitude)
                }

                // VFX: hit spark particles + light screen shake
                particleSystem?.onHit(checkX, checkY)
                gameFeelSystem?.onLightHit()

                // AoE explosion
                if (proj.aoeRadius > 0f) {
                    explodeAoE(checkX, checkY, proj.aoeRadius, proj.damage, j)
                    // VFX: explosion particles + heavy shake + hit-stop
                    particleSystem?.onExplosion(checkX, checkY)
                    gameFeelSystem?.onExplosion()
                    // [#113] SFX: explosion
                    AudioManager.getInstance().playSfx(AudioManager.SfxType.EXPLOSION)
                    // AoE projectiles die on first hit
                    state.healths[projIndex].currentHp = 0f
                    return
                }

                // Track hit for pierce
                proj.hitEntityIds.add(j)

                // Pierce: if out of pierce charges, projectile dies
                if (proj.pierceCount <= 0 && !proj.isBoomerang) {
                    state.healths[projIndex].currentHp = 0f
                    return
                } else if (!proj.isBoomerang) {
                    proj.pierceCount--
                }
                // Boomerangs pierce infinitely, don't die on hit
            }
        }
    }

    /**
     * Check if a mine should detonate (enemy within proximity radius).
     */
    private fun checkMineDetonation(
        mineIndex: Int,
        proj: ProjectileComponent,
        dt: Float
    ): Boolean {
        val minePos = state.positions[mineIndex]
        val triggerRadius = 40f

        for (j in state.enemies.indices) {
            if (j >= state.tags.size) break
            if (state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[j].isDead) continue

            val enemyPos = state.positions[j]
            val dx = enemyPos.x - minePos.x
            val dy = enemyPos.y - minePos.y
            if (dx * dx + dy * dy <= triggerRadius * triggerRadius) {
                // Detonate!
                explodeAoE(minePos.x, minePos.y, proj.aoeRadius, proj.damage, j)
                // VFX: explosion at mine position
                particleSystem?.onExplosion(minePos.x, minePos.y)
                gameFeelSystem?.onHeavyHit()
                // [#113] SFX: explosion
                AudioManager.getInstance().playSfx(AudioManager.SfxType.EXPLOSION)
                return true
            }
        }
        return false
    }

    /**
     * Explode in an AoE radius, damaging all enemies within.
     */
    private fun explodeAoE(
        centerX: Float, centerY: Float,
        radius: Float, damage: Float,
        sourceEnemyIndex: Int
    ) {
        for (j in state.enemies.indices) {
            if (j >= state.tags.size) break
            if (state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[j].isDead) continue

            val pos = state.positions[j]
            val dx = pos.x - centerX
            val dy = pos.y - centerY
            if (dx * dx + dy * dy <= radius * radius) {
                // Apply full damage to source, 70% to others (splash falloff)
                val dmg = if (j == sourceEnemyIndex) damage else damage * 0.7f
                dealDirectDamage(j, dmg)
            }
        }
    }

    /**
     * Deal projectile damage to an enemy (respects shielder reduction).
     * (#111: Added crit logic matching WeaponSystem)
     */
    private fun dealProjectileDamage(
        enemyIndex: Int,
        proj: ProjectileComponent,
        projIndex: Int
    ) {
        val health = state.healths[enemyIndex]
        var damage = proj.damage

        // Shielder damage reduction
        if (enemyIndex < state.enemies.size && state.enemies[enemyIndex].knockbackResist >= 2f) {
            damage *= 0.5f
        }
        // Armor (#108: flat subtraction per GDD §3.2)
        damage = GameConfig.armorReduction(damage, health.armor)

        // (#111) Crit check — matches WeaponSystem behavior
        val playerComp = state.players.getOrNull(minOf(state.playerIndex, state.players.size - 1))
        val crit = playerComp != null && kotlin.random.Random.nextFloat() < playerComp.critChance
        val critMult = if (playerComp != null) playerComp.critDamage else 1.5f
        val finalDamage = if (crit) damage * critMult else damage

        health.currentHp -= finalDamage

        // [#114] VFX: damage number + hit flash
        val enemyPos = state.positions[enemyIndex]
        state.damageNumbers.add(DamageNumberComponent(x = enemyPos.x, y = enemyPos.y - 10f, value = finalDamage, isCrit = crit))
        if (enemyIndex < state.renders.size) { state.renders[enemyIndex].hitFlashTimer = 0.1f }
    }

    /**
     * Deal direct damage (no shielder check — used by AoE).
     */
    private fun dealDirectDamage(enemyIndex: Int, damage: Float) {
        val health = state.healths[enemyIndex]
        var dmg = damage
        if (enemyIndex < state.enemies.size && state.enemies[enemyIndex].knockbackResist >= 2f) {
            dmg *= 0.5f
        }
        // Armor (#108: flat subtraction per GDD §3.2)
        dmg = GameConfig.armorReduction(dmg, health.armor)
        health.currentHp -= dmg
        val ePos = state.positions[enemyIndex]
        state.damageNumbers.add(DamageNumberComponent(x = ePos.x, y = ePos.y - 10f, value = dmg))
        if (enemyIndex < state.renders.size) { state.renders[enemyIndex].hitFlashTimer = 0.1f }
    }

    /**
     * Apply a status effect to an enemy.
     */
    private fun applyStatusEffect(
        enemyIndex: Int,
        type: com.survivortd.game.config.StatusEffectType,
        duration: Float,
        magnitude: Float
    ) {
        if (enemyIndex >= state.statusEffects.size) return
        val se = state.statusEffects[enemyIndex]
        val existing = se.effects.find { it.type == type }
        if (existing != null) {
            existing.duration = maxOf(existing.duration, duration)
        } else {
            se.effects.add(
                com.survivortd.game.components.StatusEffectsComponent.ActiveStatus(
                    type = type, duration = duration, magnitude = magnitude
                )
            )
        }
    }
}
