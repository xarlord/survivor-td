package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.PickupComponent
import com.survivortd.game.components.PositionComponent
import com.survivortd.game.components.RenderComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.sqrt

/**
 * Pickup system — handles XP gem spawning from dead enemies,
 * gem magnetism toward player, and gem collection.
 *
 * Gems spawn on enemy death with a small random scatter.
 * When player is within pickupRange, gems accelerate toward the player.
 * When gems touch the player, XP/gold/heal is applied.
 */
class PickupSystem(
    private val state: GameState
) {
    /**
     * Process pickups for this tick.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        if (state.playerIndex < 0 || state.playerIndex >= state.positions.size) return

        // Spawn gems from enemies that died this tick
        spawnGemsFromDeadEnemies()

        // Update existing pickups
        updatePickups(dt)
    }

    /**
     * When an enemy's HP drops to 0, spawn a gem at its position.
     * Uses tags to avoid spawning duplicate gems for the same dead enemy.
     */
    private fun spawnGemsFromDeadEnemies() {
        if (state.playerIndex < 0) return

        for (i in state.healths.indices) {
            if (i >= state.tags.size) break
            val tag = state.tags[i].tag
            if (tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (i >= state.healths.size) continue

            val health = state.healths[i]
            if (!health.isDead) continue

            // Spawn gem at enemy position
            if (i < state.positions.size) {
                val pos = state.positions[i]
                val enemyType = if (i < state.enemies.size) state.enemies[i].type
                    else EnemyComponent.EnemyData.ZOMBIE

                spawnXpGem(pos.x, pos.y, enemyType)
            }
        }
    }

    /**
     * Spawn an XP gem at the given position with a small scatter.
     */
    private fun spawnXpGem(x: Float, y: Float, enemyType: EnemyComponent.EnemyData) {
        val scatterX = (kotlin.random.Random.nextFloat() - 0.5f) * 20f
        val scatterY = (kotlin.random.Random.nextFloat() - 0.5f) * 20f

        val (xpValue, gemColor, gemRadius) = when (enemyType) {
            EnemyComponent.EnemyData.ZOMBIE,
            EnemyComponent.EnemyData.RUNNER,
            EnemyComponent.EnemyData.FLYER -> Triple(GameConfig.GEM_SMALL_XP, 0xFF42A5F5.toInt(), 5f)
            EnemyComponent.EnemyData.BRUTE,
            EnemyComponent.EnemyData.SHIELDER,
            EnemyComponent.EnemyData.SPITTER -> Triple(GameConfig.GEM_MEDIUM_XP, 0xFF7C4DFF.toInt(), 7f)
            EnemyComponent.EnemyData.BOMBER,
            EnemyComponent.EnemyData.HEALER -> Triple(GameConfig.GEM_SMALL_XP, 0xFF42A5F5.toInt(), 5f)
            EnemyComponent.EnemyData.ELITE -> Triple(GameConfig.GEM_LARGE_XP, 0xFFFFD700.toInt(), 9f)
            EnemyComponent.EnemyData.BOSS -> Triple(GameConfig.GEM_BOSS_XP, 0xFFFFD700.toInt(), 14f)
        }

        val goldValue = if (enemyType == EnemyComponent.EnemyData.BOSS)
            GameConfig.GOLD_CHEST_MIN + kotlin.random.Random.nextInt(GameConfig.GOLD_CHEST_MAX - GameConfig.GOLD_CHEST_MIN)
        else GameConfig.GOLD_PER_KILL

        // Scrap drops for tower placement
        val scrapValue = when (enemyType) {
            EnemyComponent.EnemyData.ZOMBIE,
            EnemyComponent.EnemyData.RUNNER,
            EnemyComponent.EnemyData.FLYER -> 2
            EnemyComponent.EnemyData.BRUTE,
            EnemyComponent.EnemyData.SHIELDER,
            EnemyComponent.EnemyData.SPITTER -> 5
            EnemyComponent.EnemyData.BOMBER,
            EnemyComponent.EnemyData.HEALER -> 3
            EnemyComponent.EnemyData.ELITE -> 10
            EnemyComponent.EnemyData.BOSS -> 50
        }

        state.spawnPickup(
            x = x + scatterX,
            y = y + scatterY,
            xpValue = xpValue,
            goldValue = goldValue,
            scrapValue = scrapValue,
            color = gemColor,
            radius = gemRadius
        )
    }

    /**
     * Update all pickups: lifetime, magnetism, collection.
     */
    private fun updatePickups(dt: Float) {
        val playerPos = state.positions[state.playerIndex]
        val player = state.players.getOrNull(state.playerIndex) ?: return
        val playerRadius = if (state.playerIndex < state.renders.size)
            state.renders[state.playerIndex].radius else GameConfig.PLAYER_HITBOX_RADIUS

        for (i in state.pickups.indices) {
            if (i >= state.positions.size) break
            val pickup = state.pickups[i]
            if (pickup.xpValue == 0 && pickup.goldValue == 0 && pickup.healAmount == 0f) continue

            val pos = state.positions[i]

            // Lifetime decay
            pickup.lifetime -= dt
            if (pickup.lifetime <= 0f) {
                pickup.xpValue = 0
                pickup.goldValue = 0
                pickup.scrapValue = 0
                pickup.healAmount = 0f
                state.healths[i].currentHp = 0f
                continue
            }

            val dx = playerPos.x - pos.x
            val dy = playerPos.y - pos.y
            val distSq = dx * dx + dy * dy
            val dist = sqrt(distSq.coerceAtLeast(0.01f))

            // Check magnetism range
            val magnetRange = player.pickupRange
            if (dist < magnetRange) {
                pickup.isMagnetized = true
            }

            // Move toward player if magnetized
            if (pickup.isMagnetized) {
                val speed = GameConfig.GEM_MAGNET_SPEED
                pos.x += (dx / dist) * speed * dt
                pos.y += (dy / dist) * speed * dt
            }

            // Collection check
            val collectDist = playerRadius + 8f
            if (distSq <= collectDist * collectDist) {
                // Apply XP
                if (pickup.xpValue > 0) {
                    player.currentXp += pickup.xpValue
                    while (player.currentXp >= player.xpToNext && player.level < GameConfig.MAX_LEVEL) {
                        player.currentXp -= player.xpToNext
                        player.level++
                        player.xpToNext = GameConfig.xpForLevel(player.level)
                        state.pendingLevelUps++
                    }
                }
                // Apply gold
                if (pickup.goldValue > 0) {
                    player.gold += pickup.goldValue
                    state.goldCollected += pickup.goldValue
                }
                // Apply scrap
                if (pickup.scrapValue > 0) {
                    player.scrap += pickup.scrapValue
                }
                // Apply heal
                if (pickup.healAmount > 0f && state.playerIndex < state.healths.size) {
                    val health = state.healths[state.playerIndex]
                    health.currentHp = (health.currentHp + pickup.healAmount).coerceAtMost(health.maxHp)
                }
                // Mark for removal
                pickup.xpValue = 0
                pickup.goldValue = 0
                pickup.scrapValue = 0
                pickup.healAmount = 0f
                state.healths[i].currentHp = 0f
            }
        }
    }
}
