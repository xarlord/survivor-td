package com.survivortd.game.config

import kotlinx.serialization.Serializable

/**
 * Data classes for weapon stats at each level.
 * Loaded from JSON or defined in code for the initial implementation.
 */

@Serializable
data class WeaponStats(
    val type: WeaponType,
    val displayName: String,
    val levels: List<WeaponLevel>
)

@Serializable
data class WeaponLevel(
    val level: Int,          // 1-5, then 6 = evolved
    val damage: Float,
    val cooldown: Float,     // seconds between shots
    val projectileSpeed: Float,
    val projectileCount: Int = 1,
    val pierce: Int = 0,
    val range: Float = 300f,
    val aoeRadius: Float = 0f,
    val duration: Float = 0f,
    val special: String? = null
)

@Serializable
data class TowerStats(
    val type: TowerType,
    val cost: Int,
    val levels: List<TowerLevel>
)

@Serializable
data class TowerLevel(
    val level: Int,
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val upgradeCost: Int,
    val special: String? = null
)

/**
 * Enemy spawn configuration per chapter/stage.
 */
@Serializable
data class StageConfig(
    val chapter: Int,
    val stage: Int,
    val durationSeconds: Int = 900,
    val enemyTypes: List<EnemyType>,
    val spawnPattern: SpawnPattern,
    val bossTimes: List<Int> = listOf(5, 10, 15),
    val environment: String = "wasteland"
)

@Serializable
data class SpawnPattern(
    val initialInterval: Float = 1.0f,
    val minInterval: Float = 0.3f,
    val intervalScalePerMinute: Float = 0.20f,
    val maxConcurrent: Int = 200
)

/**
 * Complete game balance data — all weapon/tower/enemy stats.
 * This is the single source of truth for game balancing.
 */
object GameBalance {

    fun getWeaponStats(type: WeaponType): WeaponStats = when (type) {
        WeaponType.ASSAULT_RIFLE -> WeaponStats(
            type, "Assault Rifle",
            listOf(
                WeaponLevel(1, damage = 8f, cooldown = 0.5f, projectileSpeed = 600f),
                WeaponLevel(2, damage = 12f, cooldown = 0.45f, projectileSpeed = 600f),
                WeaponLevel(3, damage = 18f, cooldown = 0.4f, projectileSpeed = 650f, projectileCount = 2),
                WeaponLevel(4, damage = 24f, cooldown = 0.35f, projectileSpeed = 650f, projectileCount = 2),
                WeaponLevel(5, damage = 32f, cooldown = 0.3f, projectileSpeed = 700f, projectileCount = 3),
                WeaponLevel(6, damage = 50f, cooldown = 0.2f, projectileSpeed = 800f, projectileCount = 5, special = "evolved:Minigun")
            )
        )
        WeaponType.SPREAD_GUN -> WeaponStats(
            type, "Spread Gun",
            listOf(
                WeaponLevel(1, damage = 6f, cooldown = 0.8f, projectileSpeed = 500f, projectileCount = 3, range = 250f),
                WeaponLevel(2, damage = 9f, cooldown = 0.75f, projectileSpeed = 500f, projectileCount = 3, range = 260f),
                WeaponLevel(3, damage = 12f, cooldown = 0.7f, projectileSpeed = 550f, projectileCount = 4, range = 280f),
                WeaponLevel(4, damage = 16f, cooldown = 0.65f, projectileSpeed = 550f, projectileCount = 5, range = 300f),
                WeaponLevel(5, damage = 22f, cooldown = 0.6f, projectileSpeed = 600f, projectileCount = 6, range = 320f),
                WeaponLevel(6, damage = 35f, cooldown = 0.5f, projectileSpeed = 700f, projectileCount = 9, range = 350f, special = "evolved:Plasma Cannon")
            )
        )
        WeaponType.KATANA -> WeaponStats(
            type, "Katana Slash",
            listOf(
                WeaponLevel(1, damage = 15f, cooldown = 1.2f, projectileSpeed = 0f, range = 80f),
                WeaponLevel(2, damage = 22f, cooldown = 1.1f, projectileSpeed = 0f, range = 85f),
                WeaponLevel(5, damage = 40f, cooldown = 0.8f, projectileSpeed = 0f, range = 100f),
                WeaponLevel(6, damage = 60f, cooldown = 0.5f, projectileSpeed = 0f, range = 120f, special = "evolved:Whirlwind Blade")
            )
        )
        // TODO: Fill in remaining weapons
        else -> WeaponStats(
            type, type.displayName,
            listOf(WeaponLevel(1, damage = 10f, cooldown = 0.8f, projectileSpeed = 500f))
        )
    }

    fun getTowerStats(type: TowerType): TowerStats = when (type) {
        TowerType.GUN_TURRET -> TowerStats(
            type, 50,
            listOf(
                TowerLevel(1, damage = 15f, range = 150f, fireRate = 1.0f, upgradeCost = 75),
                TowerLevel(2, damage = 25f, range = 170f, fireRate = 1.2f, upgradeCost = 100),
                TowerLevel(3, damage = 40f, range = 190f, fireRate = 1.5f, upgradeCost = 0)
            )
        )
        TowerType.CANNON -> TowerStats(
            type, 100,
            listOf(
                TowerLevel(1, damage = 40f, range = 120f, fireRate = 0.5f, upgradeCost = 120, special = "aoe"),
                TowerLevel(2, damage = 60f, range = 135f, fireRate = 0.6f, upgradeCost = 150, special = "aoe"),
                TowerLevel(3, damage = 90f, range = 150f, fireRate = 0.7f, upgradeCost = 0, special = "aoe")
            )
        )
        // TODO: Fill in remaining towers
        else -> TowerStats(
            type, 75,
            listOf(TowerLevel(1, damage = 10f, range = 130f, fireRate = 1.0f, upgradeCost = 100))
        )
    }

    /** Enemy HP at a given minute (exponential scaling). */
    fun enemyHpAtMinute(baseHp: Float, minute: Int): Float {
        return baseHp * (1f + GameConfig.ENEMY_HP_SCALE * minute)
    }

    /** Enemy damage at a given minute. */
    fun enemyDamageAtMinute(baseDamage: Float, minute: Int): Float {
        return baseDamage * (1f + GameConfig.ENEMY_DAMAGE_SCALE * minute)
    }

    /** Spawn interval at a given minute. */
    fun spawnIntervalAtMinute(minute: Int): Float {
        val interval = GameConfig.BASE_SPAWN_INTERVAL * (1f - GameConfig.ENEMY_SPAWN_RATE_SCALE * minute / 10f)
        return interval.coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)
    }
}
