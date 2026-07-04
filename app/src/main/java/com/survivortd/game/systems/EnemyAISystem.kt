package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.EnemyComponent.EnemyData
import com.survivortd.game.components.EnemyComponent.AiState
import com.survivortd.game.components.PositionComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enemy AI system — drives type-specific behavior for each enemy type.
 *
 * Replaces the basic chase in MovementSystem with rich AI:
 *
 * ZOMBIE   — Slow relentless chase (dumb)
 * RUNNER   — Fast, zigzag, erratic approach
 * BRUTE    — Slow charge-up, then lunges (telegraphed)
 * SPITTER  — Ranged kiter: approaches to mid-range, then strafes
 * BOMBER   — Rushes player, explodes on contact
 * HEALER   — Stays at range, heals nearby enemies
 * SHIELDER — Projects a damage-reduction aura on nearby enemies
 * FLYER    — Erratic sine-wave flight path
 * ELITE    — Enhanced zombie: faster, tankier, mini-charge
 * BOSS     — Multi-phase: chase → charge → summon adds → enrage
 *
 * Called every physics tick BEFORE MovementSystem applies movement.
 * Sets velocity on each enemy; MovementSystem integrates velocity → position.
 */
class EnemyAISystem(
    private val state: GameState
) {
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        if (state.playerIndex < 0 || state.playerIndex >= state.positions.size) return

        val playerPos = state.positions[state.playerIndex]

        for (i in state.enemies.indices) {
            if (i >= state.positions.size) break
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue

            val pos = state.positions[i]
            val enemy = state.enemies[i]
            val vel = if (i < state.velocities.size) state.velocities[i] else null
            if (vel == null) continue

            val dx = playerPos.x - pos.x
            val dy = playerPos.y - pos.y
            val dist = sqrt(dx * dx + dy * dy)
            val dirX = if (dist > 0.1f) dx / dist else 0f
            val dirY = if (dist > 0.1f) dy / dist else 0f

            when (enemy.type) {
                EnemyData.ZOMBIE -> updateZombie(enemy, vel, dirX, dirY, dt)
                EnemyData.RUNNER -> updateRunner(enemy, vel, dirX, dirY, dist, dt)
                EnemyData.BRUTE -> updateBrute(enemy, vel, dirX, dirY, dist, dt)
                EnemyData.SPITTER -> updateSpitter(enemy, i, vel, dirX, dirY, dist, dt)
                EnemyData.BOMBER -> updateBomber(enemy, vel, dirX, dirY, dist, dt, pos, playerPos)
                EnemyData.HEALER -> updateHealer(enemy, i, vel, dirX, dirY, dist, dt)
                EnemyData.SHIELDER -> updateShielder(enemy, i, vel, dirX, dirY, dist, dt)
                EnemyData.FLYER -> updateFlyer(enemy, vel, dirX, dirY, dt)
                EnemyData.ELITE -> updateElite(enemy, vel, dirX, dirY, dist, dt)
                EnemyData.BOSS -> updateBoss(enemy, i, vel, dirX, dirY, dist, dt, pos, playerPos)
            }
        }

        // (#117) Enemy-enemy separation: push apart overlapping enemies
        applyEnemySeparation()
    }

    /**
     * (#117) Simple separation force — push overlapping enemies apart
     * to prevent stacking and improve visual clarity.
     */
    private fun applyEnemySeparation() {
        for (i in state.enemies.indices) {
            if (i >= state.healths.size || state.healths[i].isDead) continue
            if (i >= state.tags.size || state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (i >= state.positions.size || i >= state.renders.size) continue

            for (j in i + 1 until state.positions.size) {
                if (j >= state.healths.size || state.healths[j].isDead) continue
                if (j >= state.tags.size || state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
                if (j >= state.renders.size) continue

                val dx = state.positions[j].x - state.positions[i].x
                val dy = state.positions[j].y - state.positions[i].y
                val distSq = dx * dx + dy * dy
                val minDist = state.renders[i].radius + state.renders[j].radius
                if (distSq < minDist * minDist) {
                    val (nx, ny, dist) = if (distSq > 0.01f) {
                        val d = sqrt(distSq)
                        Triple(dx / d, dy / d, d)
                    } else {
                        // Enemies at exact same position — push in arbitrary direction
                        Triple(1f, 0f, 0f)
                    }
                    val overlap = (minDist - dist) * 0.3f
                    state.positions[i].x -= nx * overlap
                    state.positions[i].y -= ny * overlap
                    state.positions[j].x += nx * overlap
                    state.positions[j].y += ny * overlap
                }
            }
        }
    }

    // ================================================================
    // ZOMBIE — dumb relentless chase
    // ================================================================
    private fun updateZombie(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dt: Float
    ) {
        val speed = 80f
        vel.x = dirX * speed
        vel.y = dirY * speed
        enemy.aiState = AiState.CHASE
    }

    // ================================================================
    // RUNNER — fast zigzag approach
    // ================================================================
    private fun updateRunner(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        val speed = 160f
        enemy.zigzagPhase += dt * 8f  // Fast oscillation

        // Perpendicular vector for zigzag
        val perpX = -dirY
        val perpY = dirX
        val zigzag = sin(enemy.zigzagPhase) * 0.5f

        vel.x = (dirX + perpX * zigzag) * speed
        vel.y = (dirY + perpY * zigzag) * speed
        enemy.aiState = AiState.CHASE
    }

    // ================================================================
    // BRUTE — slow chase, telegraphed lunge when in range
    // ================================================================
    private fun updateBrute(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        enemy.aiTimer -= dt

        when (enemy.aiState) {
            AiState.CHASE -> {
                // Slow approach
                vel.x = dirX * 50f
                vel.y = dirY * 50f
                // When in lunge range, start telegraph
                if (dist < 200f && enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHARGE
                    enemy.aiTimer = 0.6f  // Telegraph duration (0.6s)
                    vel.x = 0f  // Immediately stop on transition
                    vel.y = 0f
                }
            }
            AiState.CHARGE -> {
                // Stationary during telegraph — building up
                vel.x = 0f
                vel.y = 0f
                if (enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.ATTACK
                    enemy.aiTimer = 0.3f  // Lunge duration (0.3s)
                }
            }
            AiState.ATTACK -> {
                // Lunge! Fast burst toward player
                vel.x = dirX * 350f
                vel.y = dirY * 350f
                if (enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHASE
                    enemy.aiTimer = 2f  // Cooldown before next lunge
                }
            }
            else -> enemy.aiState = AiState.CHASE
        }
    }

    // ================================================================
    // SPITTER — kiter: approach to ~250px, then strafe + keep distance
    // ================================================================
    private fun updateSpitter(
        enemy: EnemyComponent, myIndex: Int,
        vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        val idealRange = 250f
        val strafeSpeed = 70f
        val approachSpeed = 60f

        enemy.zigzagPhase += dt * 3f

        when {
            dist > idealRange + 50f -> {
                // Approach
                vel.x = dirX * approachSpeed
                vel.y = dirY * approachSpeed
                enemy.aiState = AiState.CHASE
            }
            dist < idealRange - 50f -> {
                // Back away
                vel.x = -dirX * approachSpeed
                vel.y = -dirY * approachSpeed
                enemy.aiState = AiState.FLEE
            }
            else -> {
                // Strafe around player
                val perpX = -dirY
                val perpY = dirX
                val strafeDir = sin(enemy.zigzagPhase * 0.3f).let { if (it > 0) 1f else -1f }
                vel.x = perpX * strafeSpeed * strafeDir
                vel.y = perpY * strafeSpeed * strafeDir
                enemy.aiState = AiState.KITE

                // (#110) Spitter ranged attack: fire projectile toward player every 1.5s
                enemy.aiTimer += dt
                if (enemy.aiTimer > 1.5f) {
                    enemy.aiTimer = 0f
                    val pos = state.positions[myIndex]
                    val projId = state.spawnProjectile(pos.x, pos.y)
                    if (projId >= 0) {
                        val playerPos = state.positions[state.playerIndex]
                        val dx = playerPos.x - pos.x
                        val dy = playerPos.y - pos.y
                        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        state.velocities[projId].x = (dx / d) * 200f
                        state.velocities[projId].y = (dy / d) * 200f
                        state.projectiles[projId].damage = 15f
                        state.projectiles[projId].lifetime = 3f
                    }
                }
            }
        }
    }

    // ================================================================
    // BOMBER — rushes player, explodes on contact
    // ================================================================
    private fun updateBomber(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float,
        pos: PositionComponent, playerPos: PositionComponent
    ) {
        val speed = 120f
        // When very close, stop and flash (pre-explosion)
        if (dist < 40f) {
            enemy.aiState = AiState.SPECIAL
            enemy.aiTimer += dt
            vel.x = 0f
            vel.y = 0f
            // Flash effect: change color
            // Actual explosion handled by CombatSystem checking aiState == SPECIAL && aiTimer > 0.5
        } else {
            vel.x = dirX * speed
            vel.y = dirY * speed
            enemy.aiState = AiState.CHASE
        }
    }

    // ================================================================
    // HEALER — stays at range, periodically heals nearby enemies
    // ================================================================
    private fun updateHealer(
        enemy: EnemyComponent, myIndex: Int,
        vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        val safeRange = 300f
        enemy.aiTimer -= dt

        // Stay at safe distance
        if (dist < safeRange - 50f) {
            // Back away
            vel.x = -dirX * 70f
            vel.y = -dirY * 70f
            enemy.aiState = AiState.FLEE
        } else if (dist > safeRange + 50f) {
            // Approach
            vel.x = dirX * 60f
            vel.y = dirY * 60f
            enemy.aiState = AiState.CHASE
        } else {
            // Hover
            vel.x *= 0.8f
            vel.y *= 0.8f
            enemy.aiState = AiState.SUPPORT
        }

        // Heal pulse every 3 seconds
        if (enemy.aiTimer <= 0f && enemy.aiState == AiState.SUPPORT) {
            healNearbyEnemies(myIndex, 200f, 15f)
            enemy.aiTimer = 3f
        }
    }

    // ================================================================
    // SHIELDER — stays mid-range, projects damage reduction aura
    // ================================================================
    private fun updateShielder(
        enemy: EnemyComponent, myIndex: Int,
        vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        val idealRange = 180f

        if (dist < idealRange - 30f) {
            vel.x = -dirX * 50f
            vel.y = -dirY * 50f
        } else if (dist > idealRange + 30f) {
            vel.x = dirX * 50f
            vel.y = dirY * 50f
        } else {
            // Strafe
            enemy.zigzagPhase += dt * 2f
            val perpX = -dirY
            val perpY = dirX
            val strafeDir = if (sin(enemy.zigzagPhase) > 0) 1f else -1f
            vel.x = perpX * 40f * strafeDir
            vel.y = perpY * 40f * strafeDir
        }
        enemy.aiState = AiState.SUPPORT

        // Passive aura: reduces damage nearby enemies take (checked by CombatSystem)
        // We mark nearby enemies by setting knockbackResist > 1 as a flag
        markShieldedEnemies(myIndex, 150f, dt)
    }

    // ================================================================
    // FLYER — erratic sine-wave flight path
    // ================================================================
    private fun updateFlyer(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dt: Float
    ) {
        val speed = 120f
        enemy.zigzagPhase += dt * 5f

        val perpX = -dirY
        val perpY = dirX
        val wave = sin(enemy.zigzagPhase) * 0.7f

        vel.x = (dirX * 0.6f + perpX * wave) * speed
        vel.y = (dirY * 0.6f + perpY * wave) * speed
        enemy.aiState = AiState.CHASE
    }

    // ================================================================
    // ELITE — enhanced zombie with mini-charge
    // ================================================================
    private fun updateElite(
        enemy: EnemyComponent, vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float
    ) {
        enemy.aiTimer -= dt

        when (enemy.aiState) {
            AiState.CHASE -> {
                vel.x = dirX * 100f
                vel.y = dirY * 100f
                if (dist < 150f && enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHARGE
                    enemy.aiTimer = 0.3f
                }
            }
            AiState.CHARGE -> {
                vel.x = dirX * 200f
                vel.y = dirY * 200f
                if (enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHASE
                    enemy.aiTimer = 1.5f
                }
            }
            else -> enemy.aiState = AiState.CHASE
        }
    }

    // ================================================================
    // BOSS — multi-phase encounter
    // ================================================================
    private fun updateBoss(
        enemy: EnemyComponent, myIndex: Int,
        vel: com.survivortd.game.components.VelocityComponent,
        dirX: Float, dirY: Float, dist: Float, dt: Float,
        pos: PositionComponent, playerPos: PositionComponent
    ) {
        enemy.aiTimer -= dt
        enemy.specialTimer -= dt

        // Determine phase based on HP percentage
        val hpPercent = if (myIndex < state.healths.size) state.healths[myIndex].hpPercent else 1f
        val newPhase = when {
            hpPercent > 0.66f -> 0
            hpPercent > 0.33f -> 1
            hpPercent > 0f -> 2
            else -> 3
        }
        if (newPhase != enemy.phase) {
            enemy.phase = newPhase
            enemy.aiState = AiState.SPECIAL
            enemy.aiTimer = 1f  // Phase transition pause
        }

        when (enemy.aiState) {
            AiState.SPECIAL -> {
                // Phase transition — stationary
                vel.x = 0f
                vel.y = 0f
                if (enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHASE
                }
            }
            AiState.CHASE -> {
                // Slow relentless advance, speed increases per phase
                val speed = 40f + enemy.phase * 15f
                vel.x = dirX * speed
                vel.y = dirY * speed

                // Phase 1+: charge attack every ~5s
                if (enemy.phase >= 1 && enemy.aiTimer <= 0f && dist > 100f) {
                    enemy.aiState = AiState.CHARGE
                    enemy.aiTimer = 0.8f  // Telegraph
                }
                // Phase 2+: summon adds every ~10s
                else if (enemy.phase >= 2 && enemy.specialTimer <= 0f) {
                    enemy.aiState = AiState.SUMMON
                    enemy.aiTimer = 1f
                    enemy.specialTimer = 12f
                }
            }
            AiState.CHARGE -> {
                if (enemy.aiTimer > 0.4f) {
                    // Telegraph — stationary, gathering
                    vel.x = 0f
                    vel.y = 0f
                } else {
                    // Charge! Fast burst
                    val chargeSpeed = 250f + enemy.phase * 50f
                    vel.x = dirX * chargeSpeed
                    vel.y = dirY * chargeSpeed
                }
                if (enemy.aiTimer <= 0f) {
                    enemy.aiState = AiState.CHASE
                    enemy.aiTimer = 4f
                }
            }
            AiState.SUMMON -> {
                // Stationary while summoning
                vel.x = 0f
                vel.y = 0f
                if (enemy.aiTimer <= 0f) {
                    summonAdds(pos.x, pos.y, enemy.phase + 2)
                    enemy.aiState = AiState.CHASE
                    enemy.aiTimer = 2f
                }
            }
            else -> enemy.aiState = AiState.CHASE
        }
    }

    // ================================================================
    // HELPER: Heal nearby enemies
    // ================================================================
    private fun healNearbyEnemies(healerIndex: Int, radius: Float, amount: Float) {
        val healerPos = state.positions[healerIndex]
        var healed = 0
        for (i in state.enemies.indices) {
            if (i == healerIndex) continue
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (i >= state.healths.size) continue
            if (state.healths[i].isDead) continue
            if (i >= state.positions.size) break
            if (state.enemies[i].type == EnemyData.BOSS) continue

            val pos = state.positions[i]
            val dx = pos.x - healerPos.x
            val val_ = pos.y - healerPos.y
            val distSq = dx * dx + val_ * val_
            if (distSq <= radius * radius) {
                val hp = state.healths[i]
                hp.currentHp = (hp.currentHp + amount).coerceAtMost(hp.maxHp)
                healed++
            }
        }
    }

    // ================================================================
    // HELPER: Mark nearby enemies as shielded (damage reduction)
    // ================================================================
    private fun markShieldedEnemies(shielderIndex: Int, radius: Float, dt: Float) {
        val shielderPos = state.positions[shielderIndex]
        for (i in state.enemies.indices) {
            if (i == shielderIndex) continue
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (i >= state.positions.size) break
            if (state.enemies[i].type == EnemyData.BOSS) continue

            val pos = state.positions[i]
            val dx = pos.x - shielderPos.x
            val dy = pos.y - shielderPos.y
            val distSq = dx * dx + dy * dy
            if (distSq <= radius * radius) {
                // Mark: set knockbackResist to 2f to indicate shielded
                // CombatSystem checks this flag
                state.enemies[i].knockbackResist = 2f
            }
        }
    }

    // ================================================================
    // HELPER: Summon adds around boss position
    // ================================================================
    private fun summonAdds(centerX: Float, centerY: Float, count: Int) {
        for (j in 0 until count) {
            val angle = (j.toFloat() / count) * (2f * PI_FLOAT)
            val offsetX = cos(angle) * 80f
            val offsetY = sin(angle) * 80f
            val addType = if (j % 3 == 0) EnemyData.RUNNER else EnemyData.ZOMBIE
            state.spawnEnemy(
                x = centerX + offsetX,
                y = centerY + offsetY,
                enemyType = addType,
                hpScale = 0.5f  // Adds are weaker
            )
        }
    }

    companion object {
        private const val PI_FLOAT = 3.14159265f
    }
}
