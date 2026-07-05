package com.survivortd.game.core

import com.survivortd.game.components.*
import com.survivortd.game.config.GameConfig
import com.survivortd.game.utils.ObjectPool

/**
 * Mutable game state — read directly inside drawBehind lambdas for rendering.
 * NOT a Compose State object (would cause recomposition cascades).
 *
 * All entity data lives here as flat arrays/lists for cache-friendly iteration.
 *
 * Performance (#115): Object pools for frequently allocated components,
 * entity cap enforcement per type, and active count tracking.
 */
class GameState {

    // === ENTITIES (flat arrays for performance) ===
    val positions = ArrayList<PositionComponent>(GameConfig.MAX_ENTITIES)
    val velocities = ArrayList<VelocityComponent>(GameConfig.MAX_ENTITIES)
    val renders = ArrayList<RenderComponent>(GameConfig.MAX_ENTITIES)
    val healths = ArrayList<HealthComponent>(GameConfig.MAX_ENTITIES)
    val enemies = ArrayList<EnemyComponent>(GameConfig.MAX_ENTITIES)
    val players = ArrayList<PlayerComponent>(GameConfig.MAX_ENTITIES)
    val projectiles = ArrayList<ProjectileComponent>(GameConfig.MAX_ENTITIES)
    val pickups = ArrayList<PickupComponent>(GameConfig.MAX_ENTITIES)
    val towers = ArrayList<TowerComponent>(GameConfig.MAX_ENTITIES)
    val statusEffects = ArrayList<StatusEffectsComponent>(GameConfig.MAX_ENTITIES)
    val tags = ArrayList<TagComponent>(GameConfig.MAX_ENTITIES)
    val damageNumbers = ArrayList<com.survivortd.game.components.DamageNumberComponent>(200)

    // === OBJECT POOLS (#115) — reduce GC pressure from frequent create/destroy ===
    val projectilePool = ObjectPool(
        factory = { ProjectileComponent() },
        reset = { it.reset() },
        initialSize = 200,
        maxSize = 500
    )
    val pickupPool = ObjectPool(
        factory = { PickupComponent() },
        reset = { it.reset() },
        initialSize = 100,
        maxSize = 300
    )
    val statusEffectPool = ObjectPool(
        factory = { StatusEffectsComponent() },
        reset = { it.effects.clear() },
        initialSize = 50,
        maxSize = 200
    )

    // === ENTITY COUNT TRACKING (#115) ===
    var activeEnemyCount: Int = 0
        private set
    var activeProjectileCount: Int = 0
        private set
    var activePickupCount: Int = 0
        private set

    // === HUD STRING BUILDER (#115) — avoid allocations in game loop ===
    val hudStringBuilder = StringBuilder(64)

    // === NEXT ENTITY ID ===
    // [#47] Entity IDs ARE array indices — all component lists are parallel.
    // The next ID is always the current list size (where .add() will place
    // the new entity). A monotonic counter would diverge after cleanupDeadEntities()
    // removes entities and shifts indices, causing IndexOutOfBoundsException.
    private var nextId = 0
    fun nextEntityId(): Int {
        // Use current list size as the ID — this is always the correct index
        // for the next entity to be added via .add(). All spawn methods add to
        // every component list (using placeholders), and cleanupDeadEntities()
        // removes from every list at the same index, keeping them parallel.
        return positions.size
    }

    // === GAME STATS ===
    var score: Long = 0
    var goldCollected: Int = 0
    var totalKills: Int = 0
    var currentTick: Long = 0
    var elapsedSeconds: Float = 0f
    var healthPercent: Float = 1f
    var lives: Int = 1
    var isGameOver: Boolean = false
    var gameOverHandled: Boolean = false
    var isPaused: Boolean = false
    var isVictory: Boolean = false

    // === WAVE PROGRESSION (#97) ===
    var currentWave: Int = 0
    var waveEnemiesRemaining: Int = 0
    var isBossWave: Boolean = false
    var waveAnnouncementText: String = ""
    var waveAnnouncementTimer: Float = 0f
    var wavePaused: Boolean = false
    var wavePauseTimer: Float = 0f

    // === LEVEL UP QUEUE ===
    var pendingLevelUps: Int = 0

    // === HERO (#119) ===
    /** Current hero ID name (maps to HeroId enum). */
    var heroId: String = "COMMANDER"

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
     * Dead entities are filtered out to prevent ghost rendering.
     */
    val renderableEntities: List<RenderableEntity>
        get() {
            val result = mutableListOf<RenderableEntity>()
            for (i in positions.indices) {
                if (i >= healths.size || healths[i].isDead) continue
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
        statusEffects.add(StatusEffectsComponent())
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
        if (positions.size >= GameConfig.MAX_ENTITIES) return -1
        if (activeEnemyCount >= GameConfig.MAX_ENEMIES) return -1
        val id = nextEntityId()
        positions.add(PositionComponent(x = x, y = y))
        velocities.add(VelocityComponent(maxSpeed = 80f))
        renders.add(RenderComponent(
            radius = when (enemyType) {
                EnemyComponent.EnemyData.ZOMBIE -> 12f
                EnemyComponent.EnemyData.RUNNER -> 8f
                EnemyComponent.EnemyData.BRUTE -> 22f
                EnemyComponent.EnemyData.SPITTER -> 14f
                EnemyComponent.EnemyData.BOMBER -> 13f
                EnemyComponent.EnemyData.HEALER -> 10f
                EnemyComponent.EnemyData.SHIELDER -> 16f
                EnemyComponent.EnemyData.FLYER -> 9f
                EnemyComponent.EnemyData.ELITE -> 18f
                EnemyComponent.EnemyData.BOSS -> 35f
            },
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
            shape = when (enemyType) {
                EnemyComponent.EnemyData.ZOMBIE -> RenderComponent.RenderShape.CIRCLE
                EnemyComponent.EnemyData.RUNNER -> RenderComponent.RenderShape.TRIANGLE
                EnemyComponent.EnemyData.BRUTE -> RenderComponent.RenderShape.RECT
                EnemyComponent.EnemyData.SPITTER -> RenderComponent.RenderShape.DIAMOND
                EnemyComponent.EnemyData.BOMBER -> RenderComponent.RenderShape.CIRCLE
                EnemyComponent.EnemyData.HEALER -> RenderComponent.RenderShape.CIRCLE
                EnemyComponent.EnemyData.SHIELDER -> RenderComponent.RenderShape.RECT
                EnemyComponent.EnemyData.FLYER -> RenderComponent.RenderShape.DIAMOND
                EnemyComponent.EnemyData.ELITE -> RenderComponent.RenderShape.TRIANGLE
                EnemyComponent.EnemyData.BOSS -> RenderComponent.RenderShape.CIRCLE
            }
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
        statusEffects.add(StatusEffectsComponent())
        tags.add(TagComponent(TagComponent.EntityTag.ENEMY))
        activeEnemyCount++
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
        scrapValue: Int = 0,
        healAmount: Float = 0f,
        color: Int = 0xFF42A5F5.toInt(),
        radius: Float = 5f,
        shape: RenderComponent.RenderShape = RenderComponent.RenderShape.DIAMOND,
        pickupType: com.survivortd.game.config.PickupType = com.survivortd.game.config.PickupType.XP_GEM_SMALL
    ): Int {
        if (positions.size >= GameConfig.MAX_ENTITIES) return -1
        if (activePickupCount >= GameConfig.MAX_PICKUPS) return -1
        val id = nextEntityId()
        positions.add(PositionComponent(x = x, y = y))
        velocities.add(VelocityComponent())
        renders.add(RenderComponent(
            radius = radius,
            color = color,
            shape = shape
        ))
        healths.add(HealthComponent())  // Placeholder
        players.add(PlayerComponent())  // Placeholder
        enemies.add(EnemyComponent())   // Placeholder
        projectiles.add(ProjectileComponent())  // Placeholder
        pickups.add(PickupComponent(
            xpValue = xpValue,
            goldValue = goldValue,
            scrapValue = scrapValue,
            healAmount = healAmount,
            lifetime = GameConfig.GEM_LIFETIME,
            pickupType = pickupType
        ))
        towers.add(TowerComponent())    // Placeholder
        statusEffects.add(StatusEffectsComponent())
        tags.add(TagComponent(TagComponent.EntityTag.PICKUP))
        activePickupCount++
        return id
    }

    /**
     * Spawn a projectile entity at the given position.
     * Velocity, damage, etc. are set by the caller after spawning.
     */
    fun spawnProjectile(
        x: Float,
        y: Float
    ): Int {
        if (positions.size >= GameConfig.MAX_ENTITIES) return -1
        if (activeProjectileCount >= GameConfig.MAX_PROJECTILES) return -1
        val id = nextEntityId()
        positions.add(PositionComponent(x = x, y = y))
        velocities.add(VelocityComponent())
        renders.add(RenderComponent(
            radius = 4f,
            color = 0xFF66BB6A.toInt(),
            shape = RenderComponent.RenderShape.CIRCLE
        ))
        healths.add(HealthComponent())  // Placeholder
        players.add(PlayerComponent())  // Placeholder
        enemies.add(EnemyComponent())   // Placeholder
        projectiles.add(ProjectileComponent(
            damage = 10f,
            pierceCount = 0,
            lifetime = 2f
        ))
        pickups.add(PickupComponent())  // Placeholder
        towers.add(TowerComponent())    // Placeholder
        statusEffects.add(StatusEffectsComponent())
        tags.add(TagComponent(TagComponent.EntityTag.PROJECTILE))
        activeProjectileCount++
        return id
    }

    /**
     * Remove all entities marked as dead.
     * Iterates backwards to avoid index shifting — each removal is O(1).
     */
    /**
     * Remove all entities marked as dead.
     * Iterates backwards to avoid index shifting — each removal is O(1).
     * Returns the count of killed enemies (for wave tracking).
     */
    fun cleanupDeadEntities(): Int {
        var killedEnemies = 0
        var i = healths.size - 1
        while (i >= 0) {
            if (i < healths.size && healths[i].isDead) {
                // Track enemy kills and decrement type counters
                if (i < tags.size) {
                    when (tags[i].tag) {
                        TagComponent.EntityTag.ENEMY -> {
                            totalKills++
                            killedEnemies++
                            activeEnemyCount--
                        }
                        TagComponent.EntityTag.PROJECTILE -> {
                            activeProjectileCount--
                        }
                        TagComponent.EntityTag.PICKUP -> {
                            activePickupCount--
                        }
                        else -> {}
                    }
                }
                positions.removeAt(i)
                if (i < velocities.size) velocities.removeAt(i)
                if (i < renders.size) renders.removeAt(i)
                healths.removeAt(i)
                if (i < enemies.size) enemies.removeAt(i)
                if (i < players.size) players.removeAt(i)
                if (i < projectiles.size) projectiles.removeAt(i)
                if (i < pickups.size) pickups.removeAt(i)
                if (i < towers.size) towers.removeAt(i)
                if (i < statusEffects.size) statusEffects.removeAt(i)
                if (i < tags.size) tags.removeAt(i)
            }
            i--
        }
        // Clamp counters to 0 (safety against edge cases)
        activeEnemyCount = activeEnemyCount.coerceAtLeast(0)
        activeProjectileCount = activeProjectileCount.coerceAtLeast(0)
        activePickupCount = activePickupCount.coerceAtLeast(0)
        return killedEnemies
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
        statusEffects.clear()  // [#47] was missing — kept arrays out of sync after reset
        tags.clear()
        damageNumbers.clear()
        nextId = 0
        activeEnemyCount = 0
        activeProjectileCount = 0
        activePickupCount = 0
        score = 0
        goldCollected = 0
        totalKills = 0
        currentTick = 0
        elapsedSeconds = 0f
        healthPercent = 1f
        isGameOver = false
        gameOverHandled = false
        isPaused = false
        isVictory = false
        currentWave = 0
        waveEnemiesRemaining = 0
        isBossWave = false
        waveAnnouncementText = ""
        waveAnnouncementTimer = 0f
        wavePaused = false
        wavePauseTimer = 0f
        pendingLevelUps = 0
        playerIndex = -1
        heroId = "COMMANDER"
        cameraX = 0f
        cameraY = 0f
        joystickX = 0f
        joystickY = 0f
    }
}
