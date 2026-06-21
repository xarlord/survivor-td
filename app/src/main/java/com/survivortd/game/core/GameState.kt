package com.survivortd.game.core

import com.survivortd.game.components.*
import com.survivortd.game.config.GameConfig

/**
 * Mutable game state — read directly inside drawBehind lambdas for rendering.
 * NOT a Compose State object (would cause recomposition cascades).
 *
 * All entity data lives here as flat arrays/lists for cache-friendly iteration.
 */
class GameState {

    // === ENTITIES (flat arrays for performance) ===
    val positions = mutableListOf<PositionComponent>()
    val velocities = mutableListOf<VelocityComponent>()
    val renders = mutableListOf<RenderComponent>()
    val healths = mutableListOf<HealthComponent>()
    val enemies = mutableListOf<EnemyComponent>()
    val players = mutableListOf<PlayerComponent>()
    val projectiles = mutableListOf<ProjectileComponent>()
    val pickups = mutableListOf<PickupComponent>()
    val towers = mutableListOf<TowerComponent>()
    val tags = mutableListOf<TagComponent>()

    // === NEXT ENTITY ID ===
    private var nextId = 0
    fun nextEntityId(): Int = nextId++

    // === GAME STATS ===
    var score: Long = 0
    var goldCollected: Int = 0
    var currentTick: Long = 0
    var elapsedSeconds: Float = 0f
    var healthPercent: Float = 1f
    var lives: Int = 1
    var isGameOver: Boolean = false
    var isPaused: Boolean = false
    var isVictory: Boolean = false

    // === LEVEL UP QUEUE ===
    var pendingLevelUps: Int = 0

    // === PLAYER INDEX (quick lookup) ===
    var playerIndex: Int = -1

    // === CAMERA ===
    var cameraX: Float = 0f
    var cameraY: Float = 0f

    // === INPUT ===
    var joystickX: Float = 0f  // -1..1
    var joystickY: Float = 0f  // -1..1

    /**
     * Simple renderable entity for the Canvas draw loop.
     */
    data class RenderableEntity(
        val x: Float,
        val y: Float,
        val radius: Float,
        val color: Int,
        val shape: RenderComponent.RenderShape
    )

    /**
     * Returns all entities that should be rendered this frame.
     */
    val renderableEntities: List<RenderableEntity>
        get() {
            val result = mutableListOf<RenderableEntity>()
            for (i in positions.indices) {
                if (i < renders.size) {
                    result.add(
                        RenderableEntity(
                            x = positions[i].x,
                            y = positions[i].y,
                            radius = renders[i].radius,
                            color = renders[i].color,
                            shape = renders[i].shape
                        )
                    )
                }
            }
            return result
        }

    fun hasActiveParticles(): Boolean = false  // TODO: Particle system

    // === ENTITY FACTORY METHODS ===

    fun spawnPlayer(): Int {
        val id = nextEntityId()
        positions.add(PositionComponent(
            x = GameConfig.WORLD_WIDTH / 2f,
            y = GameConfig.WORLD_HEIGHT / 2f
        ))
        velocities.add(VelocityComponent(maxSpeed = GameConfig.PLAYER_BASE_SPEED))
        renders.add(RenderComponent(
            radius = GameConfig.PLAYER_HITBOX_RADIUS,
            color = 0xFF00E676.toInt(),
            shape = RenderComponent.RenderShape.CIRCLE
        ))
        healths.add(HealthComponent(
            maxHp = GameConfig.PLAYER_BASE_HP,
            currentHp = GameConfig.PLAYER_BASE_HP
        ))
        players.add(PlayerComponent().apply {
            regen = GameConfig.PLAYER_BASE_REGEN
        })
        enemies.add(EnemyComponent())  // Placeholder — keeps arrays aligned
        projectiles.add(ProjectileComponent())  // Placeholder
        pickups.add(PickupComponent())  // Placeholder
        towers.add(TowerComponent())  // Placeholder
        tags.add(TagComponent(TagComponent.EntityTag.PLAYER))
        playerIndex = id
        return id
    }

    fun spawnEnemy(
        x: Float,
        y: Float,
        enemyType: EnemyComponent.EnemyData,
        hpScale: Float = 1f,
        damageScale: Float = 1f
    ): Int {
        val id = nextEntityId()
        positions.add(PositionComponent(x = x, y = y))
        velocities.add(VelocityComponent(maxSpeed = 80f))
        renders.add(RenderComponent(
            radius = 12f,
            color = when (enemyType) {
                EnemyComponent.EnemyData.ZOMBIE -> 0xFF888888.toInt()
                EnemyComponent.EnemyData.RUNNER -> 0xFFFF1744.toInt()
                EnemyComponent.EnemyData.BRUTE -> 0xFF4CAF50.toInt()
                EnemyComponent.EnemyData.SPITTER -> 0xFF6A1B9A.toInt()
                EnemyComponent.EnemyData.BOMBER -> 0xFFFF6F00.toInt()
                EnemyComponent.EnemyData.HEALER -> 0xFF66BB6A.toInt()
                EnemyComponent.EnemyData.SHIELDER -> 0xFF2196F3.toInt()
                EnemyComponent.EnemyData.FLYER -> 0xFFE0E0E0.toInt()
                EnemyComponent.EnemyData.ELITE -> 0xFFFFD700.toInt()
                EnemyComponent.EnemyData.BOSS -> 0xFFFF1744.toInt()
            },
            shape = RenderComponent.RenderShape.CIRCLE
        ))
        val baseHp = when (enemyType) {
            EnemyComponent.EnemyData.ZOMBIE -> 20f
            EnemyComponent.EnemyData.RUNNER -> 15f
            EnemyComponent.EnemyData.BRUTE -> 100f
            EnemyComponent.EnemyData.SPITTER -> 40f
            EnemyComponent.EnemyData.BOMBER -> 30f
            EnemyComponent.EnemyData.HEALER -> 50f
            EnemyComponent.EnemyData.SHIELDER -> 60f
            EnemyComponent.EnemyData.FLYER -> 25f
            EnemyComponent.EnemyData.ELITE -> 60f
            EnemyComponent.EnemyData.BOSS -> 4000f
        }
        healths.add(HealthComponent(
            maxHp = baseHp * hpScale,
            currentHp = baseHp * hpScale
        ))
        players.add(PlayerComponent())  // Placeholder
        enemies.add(EnemyComponent(type = enemyType))
        projectiles.add(ProjectileComponent())  // Placeholder
        pickups.add(PickupComponent())  // Placeholder
        towers.add(TowerComponent())  // Placeholder
        tags.add(TagComponent(TagComponent.EntityTag.ENEMY))
        return id
    }

    /**
     * Spawn a pickup (XP gem, gold, health) at the given position.
     */
    fun spawnPickup(
        x: Float,
        y: Float,
        xpValue: Int = 0,
        goldValue: Int = 0,
        healAmount: Float = 0f,
        color: Int = 0xFF42A5F5.toInt(),
        radius: Float = 5f
    ): Int {
        val id = nextEntityId()
        positions.add(PositionComponent(x = x, y = y))
        velocities.add(VelocityComponent())
        renders.add(RenderComponent(
            radius = radius,
            color = color,
            shape = RenderComponent.RenderShape.DIAMOND
        ))
        healths.add(HealthComponent())  // Placeholder
        players.add(PlayerComponent())  // Placeholder
        enemies.add(EnemyComponent())   // Placeholder
        projectiles.add(ProjectileComponent())  // Placeholder
        pickups.add(PickupComponent(
            xpValue = xpValue,
            goldValue = goldValue,
            healAmount = healAmount,
            lifetime = GameConfig.GEM_LIFETIME
        ))
        towers.add(TowerComponent())    // Placeholder
        tags.add(TagComponent(TagComponent.EntityTag.PICKUP))
        return id
    }

    /**
     * Remove all entities marked as dead.
     */
    fun cleanupDeadEntities() {
        var i = 0
        while (i < healths.size) {
            if (healths[i].isDead) {
                positions.removeAt(i)
                if (i < velocities.size) velocities.removeAt(i)
                if (i < renders.size) renders.removeAt(i)
                healths.removeAt(i)
                if (i < enemies.size) enemies.removeAt(i)
                if (i < players.size) players.removeAt(i)
                if (i < projectiles.size) projectiles.removeAt(i)
                if (i < pickups.size) pickups.removeAt(i)
                if (i < towers.size) towers.removeAt(i)
                if (i < tags.size) tags.removeAt(i)
            } else {
                i++
            }
        }
    }

    /**
     * Reset game state for a new match.
     */
    fun reset() {
        positions.clear()
        velocities.clear()
        renders.clear()
        healths.clear()
        enemies.clear()
        players.clear()
        projectiles.clear()
        pickups.clear()
        towers.clear()
        tags.clear()
        nextId = 0
        score = 0
        goldCollected = 0
        currentTick = 0
        elapsedSeconds = 0f
        healthPercent = 1f
        isGameOver = false
        isPaused = false
        isVictory = false
        pendingLevelUps = 0
        playerIndex = -1
        cameraX = 0f
        cameraY = 0f
        joystickX = 0f
        joystickY = 0f
    }
}
