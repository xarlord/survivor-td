package com.survivortd.game.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Validates that every value in GameConfig.kt and GameBalance.kt matches the
 * Game Design Document (GAME_SPEC.md).
 *
 * If a value in code diverges from the GDD, either:
 *  - The code is wrong → fix the code
 *  - The GDD is outdated → update the GDD
 *
 * Either way, this test surfaces the discrepancy.
 *
 * [#26]
 */
class GddConfigValidationTest {

    // ================================================================
    // WORLD & RENDERING (GDD §3.1)
    // ================================================================
    @Nested
    @DisplayName("World & Rendering Constants (GDD §3.1)")
    inner class WorldConstants {
        @Test fun world_width_is_1280() = assertEquals(1280f, GameConfig.WORLD_WIDTH)
        @Test fun world_height_is_720() = assertEquals(720f, GameConfig.WORLD_HEIGHT)
        @Test fun target_fps_is_60() = assertEquals(60, GameConfig.TARGET_FPS)
        @Test fun fixed_timestep_is_1_over_60() = assertEquals(1f / 60f, GameConfig.FIXED_TIMESTEP, 0.0001f)
        @Test fun max_entities_is_500() = assertEquals(500, GameConfig.MAX_ENTITIES)
        @Test fun max_accumulator_is_0_25() = assertEquals(0.25f, GameConfig.MAX_ACCUMULATOR)
    }

    // ================================================================
    // PLAYER STATS (GDD §4)
    // ================================================================
    @Nested
    @DisplayName("Player Base Stats (GDD §4.1)")
    inner class PlayerStats {
        @Test fun base_hp_is_100() = assertEquals(100f, GameConfig.PLAYER_BASE_HP)
        @Test fun base_speed_is_220() = assertEquals(220f, GameConfig.PLAYER_BASE_SPEED)
        @Test fun base_pickup_range_is_60() = assertEquals(60f, GameConfig.PLAYER_BASE_PICKUP_RANGE)
        @Test fun base_damage_mult_is_1() = assertEquals(1.0f, GameConfig.PLAYER_BASE_DAMAGE_MULT)
        @Test fun base_attack_speed_mult_is_1() = assertEquals(1.0f, GameConfig.PLAYER_BASE_ATTACK_SPEED_MULT)
        @Test fun base_crit_chance_is_5_percent() = assertEquals(0.05f, GameConfig.PLAYER_BASE_CRIT_CHANCE)
        @Test fun base_armor_is_0() = assertEquals(0f, GameConfig.PLAYER_BASE_ARMOR)
        @Test fun base_regen_is_0_5() = assertEquals(0.5f, GameConfig.PLAYER_BASE_REGEN)
        @Test fun base_dodge_is_0() = assertEquals(0f, GameConfig.PLAYER_BASE_DODGE)
        @Test fun hitbox_radius_is_20() = assertEquals(20f, GameConfig.PLAYER_HITBOX_RADIUS)
        @Test fun dash_speed_is_400() = assertEquals(400f, GameConfig.PLAYER_DASH_SPEED)
        @Test fun dash_duration_is_0_12() = assertEquals(0.12f, GameConfig.PLAYER_DASH_DURATION)
        @Test fun dash_cooldown_is_3() = assertEquals(3f, GameConfig.PLAYER_DASH_COOLDOWN)
    }

    // ================================================================
    // XP & LEVELING (GDD §4.3)
    // ================================================================
    @Nested
    @DisplayName("XP & Leveling (GDD §4.3)")
    inner class XpLeveling {
        @Test fun xp_for_level_1_is_8() = assertEquals(8, GameConfig.xpForLevel(1))
        @Test fun xp_for_level_2_is_11() = assertEquals(11, GameConfig.xpForLevel(2))
        @Test fun xp_for_level_5_is_20() = assertEquals(20, GameConfig.xpForLevel(5))
        @Test fun xp_for_level_10_is_35() = assertEquals(35, GameConfig.xpForLevel(10))
        @Test fun xp_for_level_15_is_50() = assertEquals(50, GameConfig.xpForLevel(15))
        @Test fun xp_for_level_20_is_65() = assertEquals(65, GameConfig.xpForLevel(20))
        @Test fun xp_formula_is_linear_5_plus_level_times_3() {
            // GDD: xpForLevel(n) = 5 + n * 3
            for (level in 1..30) {
                assertEquals(5 + level * 3, GameConfig.xpForLevel(level),
                    "XP for level $level should be ${5 + level * 3}")
            }
        }
        @Test fun max_level_is_30() = assertEquals(30, GameConfig.MAX_LEVEL)
        @Test fun max_weapon_slots_is_6() = assertEquals(6, GameConfig.MAX_WEAPON_SLOTS)
        @Test fun max_passive_slots_is_6() = assertEquals(6, GameConfig.MAX_PASSIVE_SLOTS)
        @Test fun max_weapon_level_is_5() = assertEquals(5, GameConfig.MAX_WEAPON_LEVEL)
    }

    // ================================================================
    // ENEMY SCALING (GDD §5.2)
    // ================================================================
    @Nested
    @DisplayName("Enemy Scaling (GDD §5.2)")
    inner class EnemyScaling {
        @Test fun hp_scale_is_15_percent_per_minute() = assertEquals(0.15f, GameConfig.ENEMY_HP_SCALE)
        @Test fun damage_scale_is_8_percent_per_minute() = assertEquals(0.08f, GameConfig.ENEMY_DAMAGE_SCALE)
        @Test fun spawn_rate_scale_is_20_percent() = assertEquals(0.20f, GameConfig.ENEMY_SPAWN_RATE_SCALE)
        @Test fun base_spawn_interval_is_1_second() = assertEquals(1.0f, GameConfig.BASE_SPAWN_INTERVAL)
        @Test fun min_spawn_interval_is_0_3() = assertEquals(0.3f, GameConfig.MIN_SPAWN_INTERVAL)
        @Test fun elite_base_chance_is_5_percent() = assertEquals(0.05f, GameConfig.ELITE_BASE_CHANCE)
        @Test fun elite_scale_is_0_5_percent() = assertEquals(0.005f, GameConfig.ELITE_SCALE)
    }

    // ================================================================
    // MATCH CONFIG (GDD §6)
    // ================================================================
    @Nested
    @DisplayName("Match Config (GDD §6)")
    inner class MatchConfig {
        @Test fun match_duration_is_900_seconds() = assertEquals(900, GameConfig.MATCH_DURATION_SECONDS)
        @Test fun boss_times_are_5_10_15() = assertEquals(listOf(5, 10, 15), GameConfig.BOSS_TIMES_MINUTES)
        @Test fun build_phase_duration_is_10() = assertEquals(10f, GameConfig.BUILD_PHASE_DURATION)
    }

    // ================================================================
    // TOWER CONFIG (GDD §7)
    // ================================================================
    @Nested
    @DisplayName("Tower Config (GDD §7)")
    inner class TowerConfig {
        @Test fun max_towers_is_8() = assertEquals(8, GameConfig.MAX_TOWERS)
        @Test fun tower_upgrade_levels_is_3() = assertEquals(3, GameConfig.TOWER_UPGRADE_LEVELS)
    }

    // ================================================================
    // GEM/GOLD VALUES (GDD §8 — Economy)
    // ================================================================
    @Nested
    @DisplayName("Economy Values (GDD §8)")
    inner class EconomyValues {
        @Test fun gem_small_xp_is_1() = assertEquals(1, GameConfig.GEM_SMALL_XP)
        @Test fun gem_medium_xp_is_5() = assertEquals(5, GameConfig.GEM_MEDIUM_XP)
        @Test fun gem_large_xp_is_20() = assertEquals(20, GameConfig.GEM_LARGE_XP)
        @Test fun gem_boss_xp_is_100() = assertEquals(100, GameConfig.GEM_BOSS_XP)
        @Test fun gold_per_kill_is_2() = assertEquals(2, GameConfig.GOLD_PER_KILL)
        @Test fun gold_chest_min_is_50() = assertEquals(50, GameConfig.GOLD_CHEST_MIN)
        @Test fun gold_chest_max_is_200() = assertEquals(200, GameConfig.GOLD_CHEST_MAX)
        @Test fun gold_completion_bonus_is_200() = assertEquals(200, GameConfig.GOLD_COMPLETION_BONUS)
    }

    // ================================================================
    // PHYSICS (GDD §9)
    // ================================================================
    @Nested
    @DisplayName("Physics Constants (GDD §9)")
    inner class PhysicsConstants {
        @Test fun enemy_knockback_is_150() = assertEquals(150f, GameConfig.ENEMY_KNOCKBACK)
        @Test fun player_knockback_is_80() = assertEquals(80f, GameConfig.PLAYER_KNOCKBACK)
        @Test fun projectile_lifetime_is_2s() = assertEquals(2.0f, GameConfig.PROJECTILE_LIFETIME)
        @Test fun gem_magnet_speed_is_300() = assertEquals(300f, GameConfig.GEM_MAGNET_SPEED)
        @Test fun gem_lifetime_is_30s() = assertEquals(30f, GameConfig.GEM_LIFETIME)
    }

    // ================================================================
    // CAMERA (GDD §3.2)
    // ================================================================
    @Nested
    @DisplayName("Camera Constants (GDD §3.2)")
    inner class CameraConstants {
        @Test fun camera_width_is_720() = assertEquals(720f, GameConfig.CAMERA_WIDTH)
        @Test fun camera_height_is_1280() = assertEquals(1280f, GameConfig.CAMERA_HEIGHT)
        @Test fun camera_lerp_is_8() = assertEquals(8f, GameConfig.CAMERA_LERP)
    }

    // ================================================================
    // ENEMY BASE STATS (GDD §5.1)
    // ================================================================
    @Nested
    @DisplayName("Enemy Base Stats (GDD §5.1)")
    inner class EnemyBaseStats {
        @Test fun zombie_stats() {
            val e = EnemyType.ZOMBIE
            assertEquals(20f, e.baseHp)
            assertEquals(80f, e.baseSpeed)
            assertEquals(10f, e.baseDamage)
            assertEquals(1, e.xpValue)
        }
        @Test fun runner_stats() {
            val e = EnemyType.RUNNER
            assertEquals(15f, e.baseHp)
            assertEquals(160f, e.baseSpeed)
            assertEquals(8f, e.baseDamage)
            assertEquals(1, e.xpValue)
        }
        @Test fun brute_stats() {
            val e = EnemyType.BRUTE
            assertEquals(100f, e.baseHp)
            assertEquals(60f, e.baseSpeed)
            assertEquals(25f, e.baseDamage)
            assertEquals(5, e.xpValue)
            assertEquals(0.3f, e.knockbackResist)
        }
        @Test fun spitter_stats() {
            val e = EnemyType.SPITTER
            assertEquals(40f, e.baseHp)
            assertEquals(50f, e.baseSpeed)
            assertEquals(5f, e.baseDamage)
            assertEquals(3, e.xpValue)
        }
        @Test fun bomber_stats() {
            val e = EnemyType.BOMBER
            assertEquals(30f, e.baseHp)
            assertEquals(100f, e.baseSpeed)
            assertEquals(40f, e.baseDamage)
            assertEquals(3, e.xpValue)
        }
        @Test fun healer_stats() {
            val e = EnemyType.HEALER
            assertEquals(50f, e.baseHp)
            assertEquals(70f, e.baseSpeed)
            assertEquals(0f, e.baseDamage)
            assertEquals(5, e.xpValue)
        }
        @Test fun shielder_stats() {
            val e = EnemyType.SHIELDER
            assertEquals(60f, e.baseHp)
            assertEquals(80f, e.baseSpeed)
            assertEquals(10f, e.baseDamage)
            assertEquals(5, e.xpValue)
        }
        @Test fun flyer_stats() {
            val e = EnemyType.FLYER
            assertEquals(25f, e.baseHp)
            assertEquals(120f, e.baseSpeed)
            assertEquals(12f, e.baseDamage)
            assertEquals(2, e.xpValue)
        }
        @Test fun boss_stats() {
            val e = EnemyType.BOSS
            assertEquals(4000f, e.baseHp)
            assertEquals(50f, e.baseSpeed)
            assertEquals(50f, e.baseDamage)
            assertEquals(100, e.xpValue)
            assertEquals(50, e.goldValue)
            assertEquals(0f, e.knockbackResist) // Immune to knockback
        }
    }

    // ================================================================
    // WEAPON COUNT & TYPES (GDD §10.1)
    // ================================================================
    @Nested
    @DisplayName("Weapon Types (GDD §10.1)")
    inner class WeaponTypes {
        @Test fun there_are_12_weapon_types() {
            assertEquals(12, WeaponType.entries.size,
                "GDD specifies 12 weapons, got ${WeaponType.entries.size}")
        }
        @Test fun all_weapons_have_evolved_form() {
            for (w in WeaponType.entries) {
                assertTrue(w.evolved != null,
                    "${w.name} must have an evolved form (GDD §10.3)")
            }
        }
        @Test fun weapon_names_match_gdd() {
            assertEquals("Assault Rifle", WeaponType.ASSAULT_RIFLE.displayName)
            assertEquals("Spread Gun", WeaponType.SPREAD_GUN.displayName)
            assertEquals("Katana Slash", WeaponType.KATANA.displayName)
            assertEquals("Lightning Orb", WeaponType.LIGHTNING_ORB.displayName)
            assertEquals("Rocket Launcher", WeaponType.ROCKET_LAUNCHER.displayName)
            assertEquals("Force Field", WeaponType.FORCE_FIELD.displayName)
            assertEquals("Drone", WeaponType.DRONE.displayName)
            assertEquals("Frost Nova", WeaponType.FROST_NOVA.displayName)
            assertEquals("Boomerang", WeaponType.BOOMERANG.displayName)
            assertEquals("Landmine", WeaponType.LANDMINE.displayName)
            assertEquals("Healing Pulse", WeaponType.HEALING_PULSE.displayName)
            assertEquals("Laser Beam", WeaponType.LASER_BEAM.displayName)
        }
    }

    // ================================================================
    // PASSIVE ITEMS (GDD §10.2)
    // ================================================================
    @Nested
    @DisplayName("Passive Items (GDD §10.2)")
    inner class PassiveItems {
        @Test fun there_are_12_passive_types() {
            assertEquals(12, PassiveType.entries.size,
                "GDD specifies 12 passives, got ${PassiveType.entries.size}")
        }
        @Test fun each_passive_catalyzes_a_unique_weapon() {
            val catalyzedWeapons = PassiveType.entries.map { it.catalystFor }.toSet()
            assertEquals(12, catalyzedWeapons.size,
                "Each passive should catalyze a unique weapon")
        }
    }

    // ================================================================
    // TOWER TYPES (GDD §7.1)
    // ================================================================
    @Nested
    @DisplayName("Tower Types (GDD §7.1)")
    inner class TowerTypes {
        @Test fun there_are_6_tower_types() {
            assertEquals(6, TowerType.entries.size,
                "GDD specifies 6 towers, got ${TowerType.entries.size}")
        }
        @Test fun gun_turret_stats() {
            val t = TowerType.GUN_TURRET
            assertEquals(50, t.baseCost)
            assertEquals(15f, t.baseDamage)
            assertEquals(150f, t.baseRange)
            assertEquals(1.0f, t.baseFireRate)
        }
        @Test fun cannon_stats() {
            val t = TowerType.CANNON
            assertEquals(100, t.baseCost)
            assertEquals(40f, t.baseDamage)
            assertTrue(t.isAoE, "Cannon should be AoE")
        }
        @Test fun rocket_pod_stats() {
            val t = TowerType.ROCKET_POD
            assertEquals(150, t.baseCost)
            assertEquals(60f, t.baseDamage)
            assertEquals(200f, t.baseRange)
            assertTrue(t.isAoE, "Rocket Pod should be AoE")
        }
        @Test fun tower_upgrade_cost_formula() {
            // GDD: upgradeCost(level) = baseCost * level
            assertEquals(50, TowerType.GUN_TURRET.upgradeCost(1))
            assertEquals(100, TowerType.GUN_TURRET.upgradeCost(2))
            assertEquals(150, TowerType.GUN_TURRET.upgradeCost(3))
        }
        @Test fun tower_stats_for_level_2() {
            // GDD: L2 = +50% damage, +20% range
            val s2 = TowerType.GUN_TURRET.statsForLevel(2)
            assertEquals(22.5f, s2.damage, 0.1f) // 15 * 1.5
            assertEquals(180f, s2.range, 0.1f)   // 150 * 1.2
        }
    }

    // ================================================================
    // ASSAULT RIFLE WEAPON STATS (GDD §10.1 — Table)
    // ================================================================
    @Nested
    @DisplayName("Assault Rifle Stats (GDD §10.1)")
    inner class AssaultRifleStats {
        private val stats = GameBalance.getWeaponStats(WeaponType.ASSAULT_RIFLE)

        @Test fun has_6_levels_including_evolution() {
            assertEquals(6, stats.levels.size,
                "Assault Rifle should have levels 1-5 + evolution (6 total)")
        }
        @Test fun level_1_damage_is_8() = assertEquals(8f, stats.levels[0].damage)
        @Test fun level_1_cooldown_is_0_5() = assertEquals(0.5f, stats.levels[0].cooldown)
        @Test fun level_3_has_double_projectile() {
            assertEquals(2, stats.levels[2].projectileCount,
                "Level 3 should fire 2 projectiles")
        }
        @Test fun level_5_has_triple_projectile() {
            assertEquals(3, stats.levels[4].projectileCount,
                "Level 5 should fire 3 projectiles")
        }
        @Test fun evolved_level_6_is_minigun() {
            val evolved = stats.levels[5]
            assertEquals(6, evolved.level)
            assertEquals("evolved:Minigun", evolved.special)
            assertTrue(evolved.damage >= 50f, "Evolved should be powerful (damage >= 50)")
        }
        @Test fun damage_increases_monotonically() {
            for (i in 1 until stats.levels.size) {
                assertTrue(stats.levels[i].damage >= stats.levels[i - 1].damage,
                    "Damage should not decrease at level ${i + 1}")
            }
        }
        @Test fun cooldown_decreases_monotonically() {
            for (i in 1 until stats.levels.size) {
                assertTrue(stats.levels[i].cooldown <= stats.levels[i - 1].cooldown,
                    "Cooldown should not increase at level ${i + 1}")
            }
        }
    }

    // ================================================================
    // SPREAD GUN WEAPON STATS (GDD §10.1 — Table)
    // ================================================================
    @Nested
    @DisplayName("Spread Gun Stats (GDD §10.1)")
    inner class SpreadGunStats {
        private val stats = GameBalance.getWeaponStats(WeaponType.SPREAD_GUN)

        @Test fun level_1_has_3_projectiles() = assertEquals(3, stats.levels[0].projectileCount)
        @Test fun evolved_level_6_is_plasma_cannon() {
            assertEquals("evolved:Plasma Cannon", stats.levels[5].special)
            assertTrue(stats.levels[5].projectileCount >= 9,
                "Evolved Plasma Cannon should fire 9+ projectiles")
        }
    }

    // ================================================================
    // KATANA WEAPON STATS (GDD §10.1 — Table)
    // ================================================================
    @Nested
    @DisplayName("Katana Stats (GDD §10.1)")
    inner class KatanaStats {
        private val stats = GameBalance.getWeaponStats(WeaponType.KATANA)

        @Test fun level_1_damage_is_15() = assertEquals(15f, stats.levels[0].damage)
        @Test fun evolved_level_6_is_whirlwind_blade() {
            assertEquals("evolved:Whirlwind Blade", stats.levels.last().special)
        }
    }

    // ================================================================
    // CHAPTER CONFIG (GDD §6.1)
    // ================================================================
    @Nested
    @DisplayName("Chapter Config (GDD §6.1)")
    inner class ChapterConfigTest {
        @Test fun there_are_5_chapters() {
            assertEquals(5, ChapterConfig.ALL_CHAPTERS.size,
                "GDD specifies 5 chapters")
        }
        @Test fun chapter_1_is_wasteland() {
            assertEquals("Wasteland", ChapterConfig.WASTELAND.name)
            assertEquals(900, ChapterConfig.WASTELAND.durationSeconds)
            assertEquals(4000f, ChapterConfig.WASTELAND.bossHp)
        }
        @Test fun chapter_5_is_final_bunker_20min() {
            assertEquals("Final Bunker", ChapterConfig.FINAL_BUNKER.name)
            assertEquals(1200, ChapterConfig.FINAL_BUNKER.durationSeconds,
                "Chapter 5 should be 20 minutes (1200s)")
        }
        @Test fun boss_hp_scales_across_chapters() {
            assertTrue(ChapterConfig.WASTELAND.bossHp < ChapterConfig.TOXIC_SWAMP.bossHp,
                "Boss HP should increase: Ch1 < Ch2")
            assertTrue(ChapterConfig.UNDERGROUND_LAB.bossHp >= ChapterConfig.TOXIC_SWAMP.bossHp,
                "Boss HP should increase: Ch4 >= Ch2")
        }
        @Test fun default_boss_times_are_5_10_15() {
            assertEquals(listOf(5, 10, 15), ChapterConfig.WASTELAND.bossTimeMinutes)
        }
        @Test fun enemy_pool_has_minute_thresholds() {
            val pool = ChapterConfig.defaultEnemyPool()
            assertTrue(pool.containsKey(0), "Should have minute-0 pool")
            assertTrue(pool.containsKey(5), "Should have minute-5 pool")
            assertTrue(pool.containsKey(10), "Should have minute-10 pool")
        }
        @Test fun minute_0_pool_is_zombies_and_runners() {
            val pool = ChapterConfig.defaultEnemyPool()[0]!!
            assertTrue(pool.containsKey(EnemyComponent.EnemyData.ZOMBIE))
            assertTrue(pool.containsKey(EnemyComponent.EnemyData.RUNNER))
        }
    }

    // ================================================================
    // STATUS EFFECTS (GDD §11)
    // ================================================================
    @Nested
    @DisplayName("Status Effects (GDD §11)")
    inner class StatusEffects {
        @Test fun there_are_7_status_types() {
            assertEquals(7, StatusEffectType.entries.size,
                "GDD specifies 7 status effects, got ${StatusEffectType.entries.size}")
        }
        @Test fun all_gdd_status_effects_exist() {
            val expected = setOf("BURN", "POISON", "FREEZE", "SLOW", "SLOW_ATTACK", "STUN", "BLEED")
            val actual = StatusEffectType.entries.map { it.name }.toSet()
            assertEquals(expected, actual)
        }
    }

    // ================================================================
    // PICKUP TYPES (GDD §8.2)
    // ================================================================
    @Nested
    @DisplayName("Pickup Types (GDD §8.2)")
    inner class PickupTypes {
        @Test fun there_are_9_pickup_types() {
            assertEquals(9, PickupType.entries.size,
                "GDD specifies 9 pickup types, got ${PickupType.entries.size}")
        }
        @Test fun all_expected_pickups_exist() {
            val expected = setOf(
                "XP_GEM_SMALL", "XP_GEM_MEDIUM", "XP_GEM_LARGE", "XP_GEM_BOSS",
                "HEALTH_PACK", "MAGNET", "BOMB", "TREASURE_CHEST", "SCRAP"
            )
            val actual = PickupType.entries.map { it.name }.toSet()
            assertEquals(expected, actual)
        }
    }

    // ================================================================
    // BALANCE HELPERS (GDD §5.2 formulas)
    // ================================================================
    @Nested
    @DisplayName("Balance Formulas (GDD §5.2)")
    inner class BalanceFormulas {
        @Test fun enemy_hp_scales_15_percent_per_minute() {
            // At minute 0: base HP
            assertEquals(20f, GameBalance.enemyHpAtMinute(20f, 0), 0.1f)
            // At minute 10: 20 * (1 + 0.15 * 10) = 20 * 2.5 = 50
            assertEquals(50f, GameBalance.enemyHpAtMinute(20f, 10), 0.1f)
        }
        @Test fun enemy_damage_scales_8_percent_per_minute() {
            assertEquals(10f, GameBalance.enemyDamageAtMinute(10f, 0), 0.1f)
            // At minute 10: 10 * (1 + 0.08 * 10) = 10 * 1.8 = 18
            assertEquals(18f, GameBalance.enemyDamageAtMinute(10f, 10), 0.1f)
        }
        @Test fun spawn_interval_decreases_over_time() {
            val interval0 = GameBalance.spawnIntervalAtMinute(0)
            val interval5 = GameBalance.spawnIntervalAtMinute(5)
            val interval10 = GameBalance.spawnIntervalAtMinute(10)
            assertTrue(interval0 > interval5, "Spawn interval should decrease: min 0 > min 5")
            assertTrue(interval5 > interval10, "Spawn interval should decrease: min 5 > min 10")
        }
        @Test fun spawn_interval_floors_at_0_3() {
            assertEquals(0.3f, GameBalance.spawnIntervalAtMinute(100), 0.01f,
                "Spawn interval should floor at 0.3s")
        }
    }

    // ================================================================
    // ECS COMPONENT SCHEMA (GDD §14.1)
    // ================================================================
    @Nested
    @DisplayName("ECS Component Schema (GDD §14.1)")
    inner class EcsSchema {
        @Test fun all_entity_tags_exist() {
            val expected = setOf(
                "PLAYER", "ENEMY", "PROJECTILE", "PICKUP", "TOWER",
                "BOSS", "ELITE", "OBSTACLE", "EFFECT", "UI_ELEMENT"
            )
            val actual = com.survivortd.game.components.TagComponent.EntityTag.entries
                .map { it.name }.toSet()
            assertEquals(expected, actual)
        }
        @Test fun all_enemy_types_exist() {
            val expected = setOf(
                "ZOMBIE", "RUNNER", "BRUTE", "SPITTER", "BOMBER",
                "HEALER", "SHIELDER", "FLYER", "ELITE", "BOSS"
            )
            val actual = com.survivortd.game.components.EnemyComponent.EnemyData.entries
                .map { it.name }.toSet()
            assertEquals(expected, actual)
        }
        @Test fun all_tower_data_types_exist() {
            val expected = setOf(
                "GUN_TURRET", "CANNON", "FROST_TOWER", "TESLA_COIL",
                "POISON_TOWER", "ROCKET_POD"
            )
            val actual = com.survivortd.game.components.TowerComponent.TowerData.entries
                .map { it.name }.toSet()
            assertEquals(expected, actual)
        }
        @Test fun all_render_shapes_exist() {
            val expected = setOf("CIRCLE", "RECT", "TRIANGLE", "DIAMOND")
            val actual = com.survivortd.game.components.RenderComponent.RenderShape.entries
                .map { it.name }.toSet()
            assertEquals(expected, actual)
        }
        @Test fun all_ai_states_exist() {
            val expected = setOf(
                "CHASE", "ATTACK", "FLEE", "SUMMON", "SPECIAL", "CHARGE", "KITE", "SUPPORT"
            )
            val actual = com.survivortd.game.components.EnemyComponent.AiState.entries
                .map { it.name }.toSet()
            assertEquals(expected, actual)
        }
    }

}
