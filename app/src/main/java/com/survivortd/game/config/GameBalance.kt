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
                WeaponLevel(3, damage = 30f, cooldown = 1.0f, projectileSpeed = 0f, range = 90f),
                WeaponLevel(4, damage = 38f, cooldown = 0.9f, projectileSpeed = 0f, range = 95f),
                WeaponLevel(5, damage = 48f, cooldown = 0.8f, projectileSpeed = 0f, range = 100f),
                WeaponLevel(6, damage = 70f, cooldown = 0.5f, projectileSpeed = 0f, range = 120f, special = "evolved:Whirlwind Blade")
            )
        )
        WeaponType.LIGHTNING_ORB -> WeaponStats(
            type, "Lightning Orb",
            listOf(
                WeaponLevel(1, damage = 5f, cooldown = 0f, projectileSpeed = 0f, range = 25f),
                WeaponLevel(2, damage = 7f, cooldown = 0f, projectileSpeed = 0f, range = 27f),
                WeaponLevel(3, damage = 10f, cooldown = 0f, projectileSpeed = 0f, range = 30f),
                WeaponLevel(4, damage = 14f, cooldown = 0f, projectileSpeed = 0f, range = 33f),
                WeaponLevel(5, damage = 18f, cooldown = 0f, projectileSpeed = 0f, range = 36f),
                WeaponLevel(6, damage = 30f, cooldown = 0f, projectileSpeed = 0f, range = 40f, special = "evolved:Thunder Storm")
            )
        )
        WeaponType.ROCKET_LAUNCHER -> WeaponStats(
            type, "Rocket Launcher",
            listOf(
                WeaponLevel(1, damage = 30f, cooldown = 2.0f, projectileSpeed = 300f, aoeRadius = 60f),
                WeaponLevel(2, damage = 40f, cooldown = 1.8f, projectileSpeed = 300f, aoeRadius = 65f),
                WeaponLevel(3, damage = 55f, cooldown = 1.6f, projectileSpeed = 320f, aoeRadius = 70f),
                WeaponLevel(4, damage = 70f, cooldown = 1.4f, projectileSpeed = 320f, aoeRadius = 80f),
                WeaponLevel(5, damage = 90f, cooldown = 1.2f, projectileSpeed = 350f, aoeRadius = 90f),
                WeaponLevel(6, damage = 120f, cooldown = 0.8f, projectileSpeed = 400f, aoeRadius = 110f, special = "evolved:Missile Barrage")
            )
        )
        WeaponType.FORCE_FIELD -> WeaponStats(
            type, "Force Field",
            listOf(
                WeaponLevel(1, damage = 4f, cooldown = 0f, projectileSpeed = 0f, range = 50f),
                WeaponLevel(2, damage = 6f, cooldown = 0f, projectileSpeed = 0f, range = 55f),
                WeaponLevel(3, damage = 9f, cooldown = 0f, projectileSpeed = 0f, range = 60f),
                WeaponLevel(4, damage = 12f, cooldown = 0f, projectileSpeed = 0f, range = 65f),
                WeaponLevel(5, damage = 16f, cooldown = 0f, projectileSpeed = 0f, range = 70f),
                WeaponLevel(6, damage = 25f, cooldown = 0f, projectileSpeed = 0f, range = 90f, special = "evolved:Plasma Shield")
            )
        )
        WeaponType.DRONE -> WeaponStats(
            type, "Drone",
            listOf(
                WeaponLevel(1, damage = 8f, cooldown = 0.7f, projectileSpeed = 500f),
                WeaponLevel(2, damage = 12f, cooldown = 0.65f, projectileSpeed = 500f),
                WeaponLevel(3, damage = 16f, cooldown = 0.6f, projectileSpeed = 550f),
                WeaponLevel(4, damage = 22f, cooldown = 0.55f, projectileSpeed = 550f),
                WeaponLevel(5, damage = 28f, cooldown = 0.5f, projectileSpeed = 600f),
                WeaponLevel(6, damage = 40f, cooldown = 0.4f, projectileSpeed = 700f, special = "evolved:Drone Swarm")
            )
        )
        WeaponType.FROST_NOVA -> WeaponStats(
            type, "Frost Nova",
            listOf(
                WeaponLevel(1, damage = 6f, cooldown = 2.5f, projectileSpeed = 0f, range = 100f, duration = 2f),
                WeaponLevel(2, damage = 9f, cooldown = 2.3f, projectileSpeed = 0f, range = 110f, duration = 2.2f),
                WeaponLevel(3, damage = 13f, cooldown = 2.1f, projectileSpeed = 0f, range = 120f, duration = 2.4f),
                WeaponLevel(4, damage = 17f, cooldown = 1.9f, projectileSpeed = 0f, range = 130f, duration = 2.6f),
                WeaponLevel(5, damage = 22f, cooldown = 1.7f, projectileSpeed = 0f, range = 140f, duration = 3f),
                WeaponLevel(6, damage = 35f, cooldown = 1.2f, projectileSpeed = 0f, range = 160f, duration = 4f, special = "evolved:Absolute Zero")
            )
        )
        WeaponType.BOOMERANG -> WeaponStats(
            type, "Boomerang",
            listOf(
                WeaponLevel(1, damage = 12f, cooldown = 1.5f, projectileSpeed = 400f),
                WeaponLevel(2, damage = 17f, cooldown = 1.4f, projectileSpeed = 400f),
                WeaponLevel(3, damage = 24f, cooldown = 1.3f, projectileSpeed = 420f),
                WeaponLevel(4, damage = 32f, cooldown = 1.2f, projectileSpeed = 420f),
                WeaponLevel(5, damage = 40f, cooldown = 1.1f, projectileSpeed = 450f),
                WeaponLevel(6, damage = 55f, cooldown = 0.8f, projectileSpeed = 500f, special = "evolved:Razor Edge")
            )
        )
        WeaponType.LANDMINE -> WeaponStats(
            type, "Landmine",
            listOf(
                WeaponLevel(1, damage = 40f, cooldown = 3.0f, projectileSpeed = 0f, aoeRadius = 70f),
                WeaponLevel(2, damage = 55f, cooldown = 2.8f, projectileSpeed = 0f, aoeRadius = 75f),
                WeaponLevel(3, damage = 75f, cooldown = 2.6f, projectileSpeed = 0f, aoeRadius = 80f),
                WeaponLevel(4, damage = 95f, cooldown = 2.4f, projectileSpeed = 0f, aoeRadius = 90f),
                WeaponLevel(5, damage = 120f, cooldown = 2.2f, projectileSpeed = 0f, aoeRadius = 100f),
                WeaponLevel(6, damage = 160f, cooldown = 1.5f, projectileSpeed = 0f, aoeRadius = 130f, special = "evolved:Minefield")
            )
        )
        WeaponType.HEALING_PULSE -> WeaponStats(
            type, "Healing Pulse",
            listOf(
                WeaponLevel(1, damage = 5f, cooldown = 5.0f, projectileSpeed = 0f),
                WeaponLevel(2, damage = 8f, cooldown = 4.5f, projectileSpeed = 0f),
                WeaponLevel(3, damage = 12f, cooldown = 4.0f, projectileSpeed = 0f),
                WeaponLevel(4, damage = 16f, cooldown = 3.5f, projectileSpeed = 0f),
                WeaponLevel(5, damage = 22f, cooldown = 3.0f, projectileSpeed = 0f),
                WeaponLevel(6, damage = 30f, cooldown = 2.0f, projectileSpeed = 0f, special = "evolved:Regen Aura")
            )
        )
        WeaponType.LASER_BEAM -> WeaponStats(
            type, "Laser Beam",
            listOf(
                WeaponLevel(1, damage = 10f, cooldown = 0.5f, projectileSpeed = 0f, range = 350f),
                WeaponLevel(2, damage = 15f, cooldown = 0.45f, projectileSpeed = 0f, range = 360f),
                WeaponLevel(3, damage = 22f, cooldown = 0.4f, projectileSpeed = 0f, range = 370f),
                WeaponLevel(4, damage = 30f, cooldown = 0.35f, projectileSpeed = 0f, range = 380f),
                WeaponLevel(5, damage = 40f, cooldown = 0.3f, projectileSpeed = 0f, range = 400f),
                WeaponLevel(6, damage = 60f, cooldown = 0.2f, projectileSpeed = 0f, range = 450f, special = "evolved:Death Ray")
            )
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
        // TODO: Fill in remaining towers — done below
        TowerType.FROST_TOWER -> TowerStats(
            type, 75,
            listOf(
                TowerLevel(1, damage = 5f, range = 130f, fireRate = 0.8f, upgradeCost = 100, special = "slow"),
                TowerLevel(2, damage = 8f, range = 150f, fireRate = 0.9f, upgradeCost = 130, special = "slow"),
                TowerLevel(3, damage = 12f, range = 170f, fireRate = 1.0f, upgradeCost = 0, special = "slow")
            )
        )
        TowerType.TESLA_COIL -> TowerStats(
            type, 120,
            listOf(
                TowerLevel(1, damage = 25f, range = 110f, fireRate = 0.7f, upgradeCost = 160, special = "chain"),
                TowerLevel(2, damage = 38f, range = 125f, fireRate = 0.8f, upgradeCost = 200, special = "chain"),
                TowerLevel(3, damage = 55f, range = 140f, fireRate = 0.9f, upgradeCost = 0, special = "chain")
            )
        )
        TowerType.POISON_TOWER -> TowerStats(
            type, 80,
            listOf(
                TowerLevel(1, damage = 3f, range = 140f, fireRate = 1.0f, upgradeCost = 110, special = "poison"),
                TowerLevel(2, damage = 5f, range = 160f, fireRate = 1.0f, upgradeCost = 140, special = "poison"),
                TowerLevel(3, damage = 8f, range = 180f, fireRate = 1.0f, upgradeCost = 0, special = "poison")
            )
        )
        TowerType.ROCKET_POD -> TowerStats(
            type, 150,
            listOf(
                TowerLevel(1, damage = 60f, range = 200f, fireRate = 0.3f, upgradeCost = 200, special = "aoe"),
                TowerLevel(2, damage = 90f, range = 220f, fireRate = 0.4f, upgradeCost = 250, special = "aoe"),
                TowerLevel(3, damage = 130f, range = 240f, fireRate = 0.5f, upgradeCost = 0, special = "aoe")
            )
        )
    }

    /** Enemy HP at a given minute (linear scaling per GDD §29.1). */
    fun enemyHpAtMinute(baseHp: Float, minute: Int): Float {
        return baseHp * (1f + GameConfig.ENEMY_HP_SCALE * minute)
    }

    /** Enemy damage at a given minute. */
    fun enemyDamageAtMinute(baseDamage: Float, minute: Int): Float {
        return baseDamage * (1f + GameConfig.ENEMY_DAMAGE_SCALE * minute)
    }

    /** Spawn interval at a given minute. */
    fun spawnIntervalAtMinute(minute: Int): Float {
        // GDD §29.1: Formula is baseInterval / (1 + minutesElapsed × 0.20)
        // Example: Minute 5 = 1.0 / (1 + 0.20 * 5) = 1.0 / 2.0 = 0.5s
        val interval = GameConfig.BASE_SPAWN_INTERVAL / (1f + GameConfig.ENEMY_SPAWN_RATE_SCALE * minute)
        return interval.coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)
    }
}
