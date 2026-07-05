package com.survivortd.game.config

/**
 * Central game constants. All tunable values in one place.
 * Mirrors the values from GAME_SPEC.md.
 */
object GameConfig {

    // === WORLD & RENDERING ===
    const val WORLD_WIDTH = 1280f      // Arena width (logical units)
    const val WORLD_HEIGHT = 720f      // Arena height
    const val TARGET_FPS = 60
    const val FIXED_TIMESTEP = 1f / TARGET_FPS
    const val MAX_ACCUMULATOR = 0.25f  // Cap to prevent spiral of death
    const val MAX_ENTITIES = 1000      // Hard cap for ECS entities
    const val MAX_ENEMIES = 300        // Soft cap for simultaneous enemies
    const val MAX_PROJECTILES = 200    // Soft cap for simultaneous projectiles
    const val MAX_PICKUPS = 150         // Soft cap for simultaneous pickups
    const val MAX_SPAWN_PER_FRAME = 3  // Frame-budget limit for spawning

    // === PLAYER ===
    const val PLAYER_BASE_HP = 100f
    const val PLAYER_BASE_SPEED = 440f  // px/s (2x — fast responsive feel)
    const val PLAYER_BASE_PICKUP_RANGE = 60f
    const val PLAYER_BASE_DAMAGE_MULT = 1.0f
    const val PLAYER_BASE_ATTACK_SPEED_MULT = 1.0f
    const val PLAYER_BASE_CRIT_CHANCE = 0.05f
    const val PLAYER_BASE_ARMOR = 0f
    const val PLAYER_BASE_REGEN = 0.5f  // HP/s
    const val PLAYER_BASE_DODGE = 0f
    const val PLAYER_HITBOX_RADIUS = 20f
    const val PLAYER_DASH_SPEED = 800f   // 2x base for dash
    const val PLAYER_DASH_DURATION = 0.12f  // seconds
    const val PLAYER_DASH_COOLDOWN = 3f     // seconds

    // === XP & LEVELING ===
    fun xpForLevel(level: Int): Int = 5 + level * 3  // Linear: Lv1→2 needs 8, Lv10→11 needs 35
    const val MAX_LEVEL = 30
    const val MAX_WEAPON_SLOTS = 6
    const val MAX_PASSIVE_SLOTS = 6
    const val MAX_WEAPON_LEVEL = 5  // Before evolution

    // === ENEMY SCALING (per minute) ===
    const val ENEMY_HP_SCALE = 0.15f      // +15% HP per minute
    const val ENEMY_DAMAGE_SCALE = 0.08f  // +8% damage per minute
    const val ENEMY_SPAWN_RATE_SCALE = 0.20f // +20% spawn rate per minute
    const val BASE_SPAWN_INTERVAL = 1.0f   // seconds between spawns at minute 0
    const val MIN_SPAWN_INTERVAL = 0.3f    // floor on spawn interval
    const val ELITE_BASE_CHANCE = 0.05f    // 5% at minute 0
    const val ELITE_SCALE = 0.005f         // +0.5% per minute

    // === PHYSICS ===
    const val ENEMY_KNOCKBACK = 150f       // px/s impulse on hit
    const val PLAYER_KNOCKBACK = 80f       // px/s on taking damage
    const val PROJECTILE_LIFETIME = 2.0f   // seconds
    const val GEM_MAGNET_SPEED = 600f      // px/s when in pickup range (2x for faster feel)
    const val GEM_LIFETIME = 30f           // seconds before despawn

    // === PICKUP VISUALS (#91) ===
    const val HEALTH_DROP_CHANCE = 0.05f   // 5% health drop from any enemy
    const val PICKUP_PULSE_SPEED = 4f       // Hz for pulse animation

    // === MATCH ===
    const val MATCH_DURATION_SECONDS = 900 // 15 minutes
    val BOSS_TIMES_MINUTES = listOf(5, 10, 15)
    const val BUILD_PHASE_DURATION = 10f   // seconds

    // === WAVE PROGRESSION (#97) ===
    const val WAVE_DURATION_SECONDS = 45f   // Time per wave
    const val WAVE_PAUSE_SECONDS = 5f      // Brief rest between waves
    const val BOSS_WAVE_INTERVAL = 5        // Boss every N waves
    const val WAVE_ENEMY_BASE_COUNT = 10    // Base enemies per wave
    const val WAVE_ENEMY_SCALE_PER_WAVE = 3 // +N enemies per wave
    const val WAVE_HP_SCALE_PER_WAVE = 0.08f // +8% HP per wave
    const val WAVE_ANNOUNCEMENT_DURATION = 2.5f // seconds

    // === TOWERS ===
    const val MAX_TOWERS = 8
    const val TOWER_UPGRADE_LEVELS = 3

    // === GEM VALUES ===
    const val GEM_SMALL_XP = 1
    const val GEM_MEDIUM_XP = 5
    const val GEM_LARGE_XP = 20
    const val GEM_BOSS_XP = 100

    // === GOLD ===
    const val GOLD_PER_KILL = 2
    const val GOLD_CHEST_MIN = 50
    const val GOLD_CHEST_MAX = 200
    const val GOLD_COMPLETION_BONUS = 200

    // === SPECIAL PICKUPS ===
    const val MAGNET_PICKUP_RANGE = 500f
    const val MAGNET_DURATION = 5f
    const val BOMB_DAMAGE = 200f
    const val MAGNET_DROP_CHANCE = 0.02f
    const val BOMB_DROP_CHANCE = 0.01f
    const val TREASURE_CHEST_DROP_CHANCE = 0.005f
    const val DASH_DOUBLE_TAP_MS = 300L

    // === CAMERA ===
    const val CAMERA_WIDTH = 720f
    const val CAMERA_HEIGHT = 1280f
    const val CAMERA_LERP = 8f             // smoothing factor for camera follow

    // === ARMOR (#108) ===
    /** Flat armor reduction per GDD §3.2: armor subtracts flat from incoming damage, floored at 0. */
    fun armorReduction(damage: Float, armor: Float): Float {
        return (damage - armor).coerceAtLeast(0f)
    }

    // === MINIMAP (#98) ===
    const val MINIMAP_SIZE_DP = 80f
    const val MINIMAP_MARGIN_DP = 12f
    const val MINIMAP_ALPHA = 0.6f
    const val MINIMAP_DOT_RADIUS = 2.5f
    const val MINIMAP_VIEWPORT_ALPHA = 0.4f
    const val OFFSCREEN_INDICATOR_SIZE = 8f
    const val OFFSCREEN_INDICATOR_RANGE = 2f
}
