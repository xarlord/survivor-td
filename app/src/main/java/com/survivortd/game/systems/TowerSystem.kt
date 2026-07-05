package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.components.TowerComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.StatusEffectType
import com.survivortd.game.config.TowerType
import com.survivortd.game.core.GameState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Tower System — manages tower placement, targeting, firing, and upgrades.
 *
 * Towers are placed during Build Phase using Scrap currency.
 * They auto-target enemies within range, persist for the entire match.
 *
 * Tower Types:
 * - GUN_TURRET: Single-target DPS
 * - CANNON: AoE splash
 * - FROST_TOWER: Slows enemies
 * - TESLA_COIL: Chain lightning
 * - POISON_TOWER: DoT cloud
 * - ROCKET_POD: Long-range AoE
 */
class TowerSystem(
    private val state: GameState
) {
    val towers = mutableListOf<TowerInstance>()

    /** Maximum towers a player can place */
    val maxTowers = 8

    /** Whether build phase is currently active */
    var isBuildPhase = false

    data class TowerInstance(
        val type: TowerType,
        var level: Int = 1,
        var fireTimer: Float = 0f,
        var targetId: Int = -1,
        var totalKills: Int = 0,
        val x: Float,
        val y: Float
    )

    /**
     * Attempt to place a tower at the given position.
     * Returns true if placed, false if not enough scrap or max towers reached.
     */
    fun placeTower(type: TowerType, x: Float, y: Float): Boolean {
        if (towers.size >= maxTowers) return false

        val player = state.players.getOrNull(state.playerIndex) ?: return false
        if (player.scrap < type.baseCost) return false

        // Check no overlap with existing towers (min 48px apart)
        for (t in towers) {
            if (hypot(t.x - x, t.y - y) < 48f) return false
        }

        player.scrap -= type.baseCost
        towers.add(TowerInstance(
            type = type,
            x = x,
            y = y,
            fireTimer = 0f
        ))
        return true
    }

    /**
     * Upgrade an existing tower by 1 level (max 3).
     * Returns true if upgraded, false if max level or not enough scrap.
     */
    fun upgradeTower(towerIndex: Int): Boolean {
        if (towerIndex < 0 || towerIndex >= towers.size) return false
        val tower = towers[towerIndex]
        if (tower.level >= 3) return false

        val player = state.players.getOrNull(state.playerIndex) ?: return false
        val cost = tower.type.upgradeCost(tower.level + 1)
        if (player.scrap < cost) return false

        player.scrap -= cost
        tower.level++
        return true
    }

    /**
     * Sell a tower for 50% of total invested.
     */
    fun sellTower(towerIndex: Int): Boolean {
        if (towerIndex < 0 || towerIndex >= towers.size) return false
        val tower = towers[towerIndex]
        val player = state.players.getOrNull(state.playerIndex) ?: return false

        // Calculate total invested: base cost + upgrade costs
        var invested = tower.type.baseCost
        for (lvl in 2..tower.level) {
            invested += tower.type.upgradeCost(lvl)
        }
        player.scrap += invested / 2
        towers.removeAt(towerIndex)
        return true
    }

    /**
     * Give scrap to player (called when enemies die).
     */
    fun addScrap(amount: Int) {
        val player = state.players.getOrNull(state.playerIndex) ?: return
        player.scrap += amount
    }

    /**
     * Main update — target and fire at enemies.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        for (tower in towers) {
            tower.fireTimer -= dt
            val stats = tower.type.statsForLevel(tower.level)

            // Find target if current is invalid
            if (tower.targetId < 0 ||
                tower.targetId >= state.enemies.size ||
                state.tags.getOrNull(tower.targetId)?.tag != TagComponent.EntityTag.ENEMY ||
                state.healths.getOrNull(tower.targetId)?.isDead == true ||
                distToEnemy(tower, tower.targetId) > stats.range
            ) {
                tower.targetId = findTarget(tower, stats.range)
            }

            if (tower.targetId >= 0 && tower.fireTimer <= 0f) {
                fireTower(tower, stats)
                tower.fireTimer = 1f / stats.fireRate
            }
        }
    }

    private fun findTarget(tower: TowerInstance, range: Float): Int {
        var nearestId = -1
        var nearestDistSq = range * range

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue

            val dx = state.positions[i].x - tower.x
            val dy = state.positions[i].y - tower.y
            val distSq = dx * dx + dy * dy
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq
                nearestId = i
            }
        }
        return nearestId
    }

    private fun fireTower(tower: TowerInstance, stats: com.survivortd.game.config.TowerLevelStats) {
        val targetPos = state.positions.getOrNull(tower.targetId) ?: return

        when (tower.type) {
            TowerType.GUN_TURRET -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TURRET_SHOT)
                fireSingleTarget(tower, targetPos, stats.damage)
            }
            TowerType.CANNON -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TURRET_SHOT)
                fireAoE(tower, targetPos, stats.damage, stats.aoeRadius)
            }
            TowerType.FROST_TOWER -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TURRET_SHOT)
                fireFrost(tower, targetPos, stats.damage)
            }
            TowerType.TESLA_COIL -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TESLA_ZAP)
                fireChainLightning(tower, stats.damage)
            }
            TowerType.POISON_TOWER -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TURRET_SHOT)
                firePoisonCloud(tower, stats.damage)
            }
            TowerType.ROCKET_POD -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.TURRET_SHOT)
                fireRocket(tower, targetPos, stats.damage, stats.aoeRadius)
            }
        }
    }

    private fun fireSingleTarget(tower: TowerInstance, targetPos: Any, damage: Float) {
        val hp = state.healths.getOrNull(tower.targetId) ?: return
        hp.currentHp -= damage
        if (hp.isDead) tower.totalKills++
    }

    private fun fireAoE(tower: TowerInstance, targetPos: Any, damage: Float, radius: Float) {
        val center = state.positions[tower.targetId]
        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue

            val dx = state.positions[i].x - center.x
            val dy = state.positions[i].y - center.y
            if (dx * dx + dy * dy <= radius * radius) {
                state.healths[i].currentHp -= damage
                if (state.healths[i].isDead) tower.totalKills++
            }
        }
    }

    private fun fireFrost(tower: TowerInstance, targetPos: Any, damage: Float) {
        val hp = state.healths.getOrNull(tower.targetId) ?: return
        hp.currentHp -= damage

        // (#111) Apply SLOW status effect to nearby enemies (unified with StatusEffectSystem)
        val center = state.positions[tower.targetId]
        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue

            val dx = state.positions[i].x - center.x
            val dy = state.positions[i].y - center.y
            if (dx * dx + dy * dy <= 60f * 60f) {
                applySlow(i, 2f, 0.4f)
            }
        }
    }

    private fun fireChainLightning(tower: TowerInstance, damage: Float) {
        // Hit primary target, then chain to 2 nearest enemies
        val hit = mutableSetOf<Int>()
        var currentTarget = tower.targetId

        for (chain in 0 until 3) {
            if (currentTarget < 0 || currentTarget >= state.enemies.size) break
            if (currentTarget in hit) break

            hit.add(currentTarget)
            val hp = state.healths.getOrNull(currentTarget) ?: break
            hp.currentHp -= damage * (1f - chain * 0.2f)  // Each chain does 20% less
            if (hp.isDead) tower.totalKills++

            // Tesla Coil chain lightning STUNs hit enemies for 0.5s (GDD §3.3, issue #52)
            applyStun(currentTarget, 0.5f)

            // Find next chain target within 100px
            var nextTarget = -1
            var nearestDistSq = 100f * 100f
            val from = state.positions[currentTarget]
            for (i in state.enemies.indices) {
                if (i in hit) continue
                if (i >= state.tags.size) break
                if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
                if (state.healths[i].isDead) continue

                val dx = state.positions[i].x - from.x
                val dy = state.positions[i].y - from.y
                val distSq = dx * dx + dy * dy
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq
                    nextTarget = i
                }
            }
            currentTarget = nextTarget
        }
    }

    private fun firePoisonCloud(tower: TowerInstance, dps: Float) {
        // Apply poison DoT to all enemies in range
        val stats = tower.type.statsForLevel(tower.level)
        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue

            val dx = state.positions[i].x - tower.x
            val dy = state.positions[i].y - tower.y
            if (dx * dx + dy * dy <= stats.range * stats.range) {
                // Poison ignores armor
                state.healths[i].currentHp -= dps * (1f / stats.fireRate)
                if (state.healths[i].isDead) tower.totalKills++
                // Apply POISON status effect (DoT, ignores armor per GDD §3.3)
                applyStatus(i, dps, stats.fireRate)
            }
        }
    }

    private fun fireRocket(tower: TowerInstance, targetPos: Any, damage: Float, radius: Float) {
        // Rockets have travel time — spawn a projectile that explodes on arrival
        // For simplicity, instant damage + AoE (same as cannon but bigger range)
        fireAoE(tower, targetPos, damage, radius)
    }

    private fun distToEnemy(tower: TowerInstance, enemyIndex: Int): Float {
        if (enemyIndex < 0 || enemyIndex >= state.positions.size) return Float.MAX_VALUE
        val pos = state.positions[enemyIndex]
        return hypot(pos.x - tower.x, pos.y - tower.y)
    }

    /**
     * Apply a STUN status effect to an enemy (hard CC — zeroes velocity, GDD §3.3).
     * Issue #52: Tesla Coil chain lightning now STUNs hit enemies.
     */
    private fun applyStun(enemyIndex: Int, duration: Float) {
        if (enemyIndex < 0 || enemyIndex >= state.statusEffects.size) return
        val se = state.statusEffects[enemyIndex]
        val existing = se.effects.find { it.type == StatusEffectType.STUN }
        if (existing != null) {
            existing.duration = maxOf(existing.duration, duration)
        } else {
            se.effects.add(
                com.survivortd.game.components.StatusEffectsComponent.ActiveStatus(
                    type = StatusEffectType.STUN,
                    duration = duration,
                    magnitude = 1f
                )
            )
        }
    }

    /**
     * (#111) Apply a SLOW status effect to an enemy (unified with StatusEffectSystem).
     */
    private fun applySlow(enemyIndex: Int, duration: Float, magnitude: Float) {
        if (enemyIndex < 0 || enemyIndex >= state.statusEffects.size) return
        val se = state.statusEffects[enemyIndex]
        val existing = se.effects.find { it.type == StatusEffectType.SLOW }
        if (existing != null) {
            existing.duration = maxOf(existing.duration, duration)
        } else {
            se.effects.add(
                com.survivortd.game.components.StatusEffectsComponent.ActiveStatus(
                    type = StatusEffectType.SLOW,
                    duration = duration,
                    magnitude = magnitude,
                    tickInterval = 0f
                )
            )
        }
    }

    /**
     * Apply a POISON status effect to an enemy (DoT, ignores armor).
     */
    private fun applyStatus(enemyIndex: Int, dps: Float, fireRate: Float) {
        if (enemyIndex < 0 || enemyIndex >= state.statusEffects.size) return
        val se = state.statusEffects[enemyIndex]
        val poisonDmg = dps * 0.3f  // 30% of tower DPS as poison per tick
        val existing = se.effects.find { it.type == StatusEffectType.POISON }
        if (existing != null) {
            existing.duration = maxOf(existing.duration, 3f)
        } else {
            se.effects.add(
                com.survivortd.game.components.StatusEffectsComponent.ActiveStatus(
                    type = StatusEffectType.POISON,
                    duration = 3f,
                    magnitude = poisonDmg
                )
            )
        }
    }
}
