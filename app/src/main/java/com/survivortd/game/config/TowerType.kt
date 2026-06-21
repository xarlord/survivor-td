package com.survivortd.game.config

/**
 * Tower types — each with unique targeting, damage type, and special effects.
 *
 * Placed during Build Phase using Scrap currency.
 * Towers persist for the rest of the match.
 */
enum class TowerType(
    val displayName: String,
    val baseCost: Int,
    val baseDamage: Float,
    val baseRange: Float,
    val baseFireRate: Float,   // Shots per second
    val isAoE: Boolean = false,
    val isDoT: Boolean = false,
    val special: String = ""
) {
    GUN_TURRET(
        "Gun Turret", 50, 15f, 150f, 1.0f,
        special = "Reliable single-target DPS"
    ),
    CANNON(
        "Cannon", 100, 40f, 120f, 0.5f,
        isAoE = true,
        special = "AoE splash damage"
    ),
    FROST_TOWER(
        "Frost Tower", 75, 5f, 130f, 0.8f,
        special = "Slows enemies 40% for 2s"
    ),
    TESLA_COIL(
        "Tesla Coil", 120, 25f, 110f, 0.7f,
        special = "Chain lightning to 3 enemies"
    ),
    POISON_TOWER(
        "Poison Tower", 80, 3f, 140f, 1.0f,
        isDoT = true,
        special = "Poison cloud, ignores armor"
    ),
    ROCKET_POD(
        "Rocket Pod", 150, 60f, 200f, 0.3f,
        isAoE = true,
        special = "Long range, AoE, slow projectile"
    );

    /**
     * Calculate upgrade cost for the given level.
     * Level 2 = 2x base, Level 3 = 3x base
     */
    fun upgradeCost(level: Int): Int {
        return baseCost * level
    }

    /**
     * Get stats for a tower at the given level (1-3).
     * Level 2: +50% damage, +20% range
     * Level 3: +100% damage, +30% range
     */
    fun statsForLevel(level: Int): TowerLevelStats {
        val l = level.coerceIn(1, 3)
        val damageMult = 1f + 0.5f * (l - 1)
        val rangeMult = 1f + 0.2f * (l - 1)
        return TowerLevelStats(
            damage = baseDamage * damageMult,
            range = baseRange * rangeMult,
            fireRate = baseFireRate,
            aoeRadius = if (isAoE) baseDamage * 0.8f * damageMult else 0f
        )
    }
}

data class TowerLevelStats(
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val aoeRadius: Float = 0f
)
