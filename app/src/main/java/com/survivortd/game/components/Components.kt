package com.survivortd.game.components

import com.survivortd.game.config.StatusEffectType
import kotlin.random.Random

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
    var shape: RenderShape = RenderShape.CIRCLE,
    var hitFlashTimer: Float = 0f
) {
    enum class RenderShape { CIRCLE, RECT, TRIANGLE, DIAMOND, CROSS }
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
    var invincibleTimer: Float = 0f,
    var deathTimer: Float = 0f
) {
    val isDead: Boolean get() = currentHp <= 0f
    val isDying: Boolean get() = deathTimer > 0f
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
    var zigzagPhase: Float = 0f,     // For runner/flyer erratic movement
    var slowTimer: Float = 0f,       // Active slow effect duration
    var slowMagnitude: Float = 0f    // Slow amount (0=none, 0.4=40% slow)
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
    var hitEntityIds: MutableSet<Int> = mutableSetOf(),
    var aoeRadius: Float = 0f,
    var isBoomerang: Boolean = false,
    var isMine: Boolean = false,
    var ownerWeapon: com.survivortd.game.config.WeaponType = com.survivortd.game.config.WeaponType.ASSAULT_RIFLE
) {
    /** Reset to default values for pool reuse. */
    fun reset() {
        damage = 10f
        pierceCount = 0
        lifetime = 2f
        homing = false
        onHitEffect = null
        onHitEffectDuration = 0f
        onHitEffectMagnitude = 0f
        hitEntityIds.clear()
        aoeRadius = 0f
        isBoomerang = false
        isMine = false
        ownerWeapon = com.survivortd.game.config.WeaponType.ASSAULT_RIFLE
    }
}

/**
 * Pickup data — XP gems, health packs, etc.
 */
data class PickupComponent(
    var xpValue: Int = 0,
    var goldValue: Int = 0,
    var scrapValue: Int = 0,
    var healAmount: Float = 0f,
    var isMagnetized: Boolean = false,
    var lifetime: Float = 30f,
    var pickupType: com.survivortd.game.config.PickupType = com.survivortd.game.config.PickupType.XP_GEM_SMALL
) {
    /** Reset to default values for pool reuse. */
    fun reset() {
        xpValue = 0
        goldValue = 0
        scrapValue = 0
        healAmount = 0f
        isMagnetized = false
        lifetime = 30f
        pickupType = com.survivortd.game.config.PickupType.XP_GEM_SMALL
    }
}

/**
 * Player-specific data.
 */
data class PlayerComponent(
    var level: Int = 1,
    var currentXp: Int = 0,
    var xpToNext: Int = 8,
    var gold: Int = 0,
    var scrap: Int = 0,
    var pickupRange: Float = 60f,
    var damageMult: Float = 1f,
    var attackSpeedMult: Float = 1f,
    var critChance: Float = 0.05f,
    var regen: Float = 0.5f,
    var moveSpeed: Float = 220f,
    var dashTimer: Float = 0f,
    var dashCooldown: Float = 3f,
    var dashCooldownTimer: Float = 0f,
    var isDashing: Boolean = false,
    var hasRevived: Boolean = false,
    var magnetTimer: Float = 0f,

    // === Hero-specific fields (#119) ===
    /** Base values before hero passive modifications (snapshot at game start). */
    var baseDamageMultiplier: Float = 1f,
    var baseMoveSpeed: Float = 220f,
    var basePickupRange: Float = 60f,

    /** Commander: +10% tower damage & range (flag for TowerSystem). */
    var commanderBonus: Boolean = false,

    /** Berserker: extra damage multiplier applied when HP < 30%. */
    var berserkerActive: Boolean = false,

    /** Engineer: max tower slots (default 8, Engineer gets 9). */
    var maxTowers: Int = 8,

    /** Engineer: tower cost multiplier (1.0f default, 0.8f for Engineer). */
    var towerCostMultiplier: Float = 1f,

    /** Medic: healing items strength multiplier (1.5f for Medic). */
    var healingBonus: Float = 1f,

    /** Shielder: cooldown timer for damage reduction cycle (10s). */
    var shieldCooldownTimer: Float = 0f,

    /** Shielder: remaining time on damage reduction buff (2s when active). */
    var damageReductionTimer: Float = 0f,

    /** Shielder: damage reduction while active (0.5f = 50% less damage). */
    var armorReduction: Float = 0f
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

data class DamageNumberComponent(
    var x: Float = 0f,
    var y: Float = 0f,
    var value: Float = 0f,
    var isCrit: Boolean = false,
    var elementColor: Int = 0xFFFFFFFF.toInt(),
    var lifetime: Float = 0.6f,
    var elapsed: Float = 0f,
    val vx: Float = (Random.nextFloat() - 0.5f) * 20f,
    val vy: Float = -60f
) {
    val alpha: Float get() = (1f - elapsed / lifetime).coerceIn(0f, 1f)
    val fontSize: Float get() = if (isCrit) 24f else 16f
}
