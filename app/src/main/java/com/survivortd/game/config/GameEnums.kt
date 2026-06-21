package com.survivortd.game.config

import kotlinx.serialization.Serializable

/**
 * All weapon types in Survivor TD.
 * Each weapon has an evolved form unlocked via weapon Level 5 + matching passive.
 */
@Serializable
enum class WeaponType(val displayName: String, val evolved: String?) {
    ASSAULT_RIFLE("Assault Rifle", "Minigun"),
    SPREAD_GUN("Spread Gun", "Plasma Cannon"),
    KATANA("Katana Slash", "Whirlwind Blade"),
    LIGHTNING_ORB("Lightning Orb", "Thunder Storm"),
    ROCKET_LAUNCHER("Rocket Launcher", "Missile Barrage"),
    FORCE_FIELD("Force Field", "Plasma Shield"),
    DRONE("Drone", "Drone Swarm"),
    FROST_NOVA("Frost Nova", "Absolute Zero"),
    BOOMERANG("Boomerang", "Razor Edge"),
    LANDMINE("Landmine", "Minefield"),
    HEALING_PULSE("Healing Pulse", "Regen Aura"),
    LASER_BEAM("Laser Beam", "Death Ray");
}

/**
 * All passive item types. Each is a catalyst for a weapon evolution.
 */
@Serializable
enum class PassiveType(val displayName: String, val catalystFor: WeaponType) {
    POWER_CORE("Power Core", WeaponType.ASSAULT_RIFLE),
    RAPID_LOADER("Rapid Loader", WeaponType.SPREAD_GUN),
    ENERGY_DRINK("Energy Drink", WeaponType.KATANA),
    HIGH_VOLTAGE("High-Voltage", WeaponType.LIGHTNING_ORB),
    HEAVY_CALIBER("Heavy Caliber", WeaponType.ROCKET_LAUNCHER),
    REINFORCED_PLATING("Reinforced Plating", WeaponType.FORCE_FIELD),
    CPU_UPGRADE("CPU Upgrade", WeaponType.DRONE),
    CRYO_MODULE("Cryo Module", WeaponType.FROST_NOVA),
    SHARP_EDGE("Sharp Edge", WeaponType.BOOMERANG),
    EXPANDED_MAGAZINE("Expanded Magazine", WeaponType.LANDMINE),
    MED_KIT("Med Kit", WeaponType.HEALING_PULSE),
    BATTERY("Battery", WeaponType.LASER_BEAM);
}

/**
 * Enemy archetypes with base stats.
 * Actual stats at runtime = base * scaling factor per minute elapsed.
 */
@Serializable
enum class EnemyType(
    val displayName: String,
    val baseHp: Float,
    val baseSpeed: Float,
    val baseDamage: Float,
    val xpValue: Int,
    val goldValue: Int = 2,
    val knockbackResist: Float = 1.0f
) {
    ZOMBIE("Zombie", 20f, 80f, 10f, 1),
    RUNNER("Runner", 15f, 160f, 8f, 1),
    BRUTE("Brute", 100f, 60f, 25f, 5, knockbackResist = 0.3f),
    SPITTER("Spitter", 40f, 50f, 5f, 3),
    BOMBER("Bomber", 30f, 100f, 40f, 3),
    HEALER("Healer", 50f, 70f, 0f, 5),
    SHIELDER("Shielder", 60f, 80f, 10f, 5),
    FLYER("Flyer", 25f, 120f, 12f, 2),
    ELITE("Elite", 1f, 1f, 1f, 3, goldValue = 5), // Multiplied by base type at runtime
    BOSS("Boss", 4000f, 50f, 50f, 100, goldValue = 50, knockbackResist = 0.0f);
}

/**
 * Tower types for the TD layer.
 */
@Serializable
enum class TowerType(
    val displayName: String,
    val cost: Int,
    val baseDamage: Float,
    val baseRange: Float,
    val baseFireRate: Float,
    val special: String
) {
    GUN_TURRET("Gun Turret", 50, 15f, 150f, 1.0f, "Single-target DPS"),
    CANNON("Cannon", 100, 40f, 120f, 0.5f, "AoE splash"),
    FROST_TOWER("Frost Tower", 75, 5f, 130f, 0.8f, "Slows 40% for 2s"),
    TESLA_COIL("Tesla Coil", 120, 25f, 110f, 0.7f, "Chain lightning x3"),
    POISON_TOWER("Poison Tower", 80, 3f, 140f, 1.0f, "Poison cloud, ignores armor"),
    ROCKET_POD("Rocket Pod", 150, 60f, 200f, 0.3f, "Long range, AoE");
}

/**
 * Status effects applicable to enemies.
 */
@Serializable
enum class StatusEffectType {
    BURN,       // DoT fire
    POISON,     // DoT, ignores armor
    FREEZE,     // Hard stop — enemy can't move
    SLOW,       // Movement speed reduction
    SLOW_ATTACK,// Attack speed reduction
    STUN,       // Can't act
    BLEED,      // DoT physical
}

/**
 * Pickup types that drop from enemies or appear on the map.
 */
@Serializable
enum class PickupType {
    XP_GEM_SMALL,
    XP_GEM_MEDIUM,
    XP_GEM_LARGE,
    XP_GEM_BOSS,
    HEALTH_PACK,
    MAGNET,      // Pulls all gems to player
    BOMB,        // Clears all enemies on screen
    TREASURE_CHEST,
    SCRAP,       // Tower-building currency
}
