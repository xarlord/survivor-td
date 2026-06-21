package com.survivortd.game.components

import com.survivortd.game.config.StatusEffectType

/**
 * Position component — where an entity is in the world.
 */
data class PositionComponent(
    var x: Float = 0f,
    var y: Float = 0f
)

/**
 * Velocity component — movement vector in px/s.
 */
data class VelocityComponent(
    var x: Float = 0f,
    var y: Float = 0f,
    var maxSpeed: Float = 0f
)

/**
 * Render component — what to draw on Canvas.
 */
data class RenderComponent(
    var radius: Float = 10f,
    var color: Int = 0xFFFFFFFF.toInt(),
    var shape: RenderShape = RenderShape.CIRCLE
) {
    enum class RenderShape { CIRCLE, RECT, TRIANGLE, DIAMOND }
}

/**
 * Health component — HP and armor.
 */
data class HealthComponent(
    var maxHp: Float = 100f,
    var currentHp: Float = 100f,
    var armor: Float = 0f,
    var dodge: Float = 0f,
    var invincible: Boolean = false,
    var invincibleTimer: Float = 0f
) {
    val isDead: Boolean get() = currentHp <= 0f
    val hpPercent: Float get() = if (maxHp > 0) (currentHp / maxHp).coerceIn(0f, 1f) else 0f
}

/**
 * Damage component — contact damage on touch.
 */
data class DamageComponent(
    var contactDamage: Float = 10f,
    var attackCooldown: Float = 1.0f,
    var attackTimer: Float = 0f
)

/**
 * Tags an entity as a specific game archetype.
 */
data class TagComponent(
    val tag: EntityTag
) {
    enum class EntityTag {
        PLAYER, ENEMY, PROJECTILE, PICKUP, TOWER,
        BOSS, ELITE, OBSTACLE, EFFECT, UI_ELEMENT
    }
}

/**
 * Enemy-specific data.
 */
data class EnemyComponent(
    var type: EnemyData = EnemyData.ZOMBIE,
    var knockbackResist: Float = 1f,
    var aiState: AiState = AiState.CHASE,
    var aiTimer: Float = 0f,          // General-purpose attack/cooldown timer
    var specialTimer: Float = 0f,     // Charge/special ability timer
    var phase: Int = 0,               // Boss phase (0,1,2,3)
    var targetId: Int = -1,           // For healer/shielder targeting
    var spawnX: Float = 0f,          // Remember spawn position for spitter kiting
    var spawnY: Float = 0f,
    var zigzagPhase: Float = 0f      // For runner/flyer erratic movement
) {
    enum class EnemyData { ZOMBIE, RUNNER, BRUTE, SPITTER, BOMBER, HEALER, SHIELDER, FLYER, ELITE, BOSS }
    enum class AiState { CHASE, ATTACK, FLEE, SUMMON, SPECIAL, CHARGE, KITE, SUPPORT }
}

/**
 * Status effects collection for an entity.
 */
data class StatusEffectsComponent(
    val effects: MutableList<ActiveStatus> = mutableListOf()
) {
    data class ActiveStatus(
        val type: StatusEffectType,
        var duration: Float,
        val magnitude: Float,
        val tickInterval: Float = 0.5f,
        var tickTimer: Float = 0f
    )
}

/**
 * Projectile data for weapons.
 */
data class ProjectileComponent(
    var damage: Float = 10f,
    var pierceCount: Int = 0,
    var lifetime: Float = 2f,
    var homing: Boolean = false,
    var onHitEffect: StatusEffectType? = null,
    var onHitEffectDuration: Float = 0f,
    var onHitEffectMagnitude: Float = 0f,
    var hitEntityIds: MutableSet<Int> = mutableSetOf()
)

/**
 * Pickup data — XP gems, health packs, etc.
 */
data class PickupComponent(
    var xpValue: Int = 0,
    var goldValue: Int = 0,
    var healAmount: Float = 0f,
    var isMagnetized: Boolean = false,
    var lifetime: Float = 30f
)

/**
 * Player-specific data.
 */
data class PlayerComponent(
    var level: Int = 1,
    var currentXp: Int = 0,
    var xpToNext: Int = 8,
    var gold: Int = 0,
    var pickupRange: Float = 60f,
    var damageMult: Float = 1f,
    var attackSpeedMult: Float = 1f,
    var critChance: Float = 0.05f,
    var regen: Float = 0.5f,
    var moveSpeed: Float = 220f,
    var dashTimer: Float = 0f,
    var dashCooldown: Float = 3f,
    var isDashing: Boolean = false
)

/**
 * Tower component for TD buildings.
 */
data class TowerComponent(
    var type: TowerData = TowerData.GUN_TURRET,
    var level: Int = 1,
    var fireTimer: Float = 0f,
    var targetId: Int = -1,
    var totalKills: Int = 0
) {
    enum class TowerData { GUN_TURRET, CANNON, FROST_TOWER, TESLA_COIL, POISON_TOWER, ROCKET_POD }
}
