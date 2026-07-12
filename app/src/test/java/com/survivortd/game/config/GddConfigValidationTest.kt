package com.survivortd.game.config

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Validates that every constant in [GameConfig] matches the values specified
 * in GAME_SPEC.md (the GDD). If any of these fail, either the code or the GDD
 * needs to be updated to match.
 *
 * [#26]
 */
class GddConfigValidationTest {

    @Nested
    @DisplayName("§3.2 Player Base Stats")
    inner class PlayerStatsTest {
        @Test
        @DisplayName("Player base HP = 100")
        fun playerHp() = assertEquals(100f, GameConfig.PLAYER_BASE_HP)

        @Test
        @DisplayName("Player base speed = 220 px/s (GDD §3.2)")
        fun playerSpeed() = assertEquals(220f, GameConfig.PLAYER_BASE_SPEED)

        @Test
        @DisplayName("Player pickup range = 60 px")
        fun playerPickupRange() = assertEquals(60f, GameConfig.PLAYER_BASE_PICKUP_RANGE)

        @Test
        @DisplayName("Player damage mult = 1.0")
        fun playerDamageMult() = assertEquals(1.0f, GameConfig.PLAYER_BASE_DAMAGE_MULT)

        @Test
        @DisplayName("Player attack speed mult = 1.0")
        fun playerAttackSpeedMult() = assertEquals(1.0f, GameConfig.PLAYER_BASE_ATTACK_SPEED_MULT)

        @Test
        @DisplayName("Player crit chance = 5%")
        fun playerCritChance() = assertEquals(0.05f, GameConfig.PLAYER_BASE_CRIT_CHANCE)

        @Test
        @DisplayName("Player base armor = 0")
        fun playerArmor() = assertEquals(0f, GameConfig.PLAYER_BASE_ARMOR)

        @Test
        @DisplayName("Player HP regen = 0.5/s")
        fun playerRegen() = assertEquals(0.5f, GameConfig.PLAYER_BASE_REGEN)

        @Test
        @DisplayName("Player hitbox radius = 20 px")
        fun playerHitbox() = assertEquals(20f, GameConfig.PLAYER_HITBOX_RADIUS)

        @Test
        @DisplayName("Player dash speed = 400 px/s (GDD §3.1)")
        fun playerDashSpeed() = assertEquals(400f, GameConfig.PLAYER_DASH_SPEED)

        @Test
        @DisplayName("Player dash cooldown = 3s")
        fun playerDashCooldown() = assertEquals(3f, GameConfig.PLAYER_DASH_COOLDOWN)
    }

    @Nested
    @DisplayName("§13.1 World & Rendering Constants")
    inner class WorldConstantsTest {
        @Test
        @DisplayName("World width = 1280 px")
        fun worldWidth() = assertEquals(1280f, GameConfig.WORLD_WIDTH)

        @Test
        @DisplayName("World height = 720 px")
        fun worldHeight() = assertEquals(720f, GameConfig.WORLD_HEIGHT)

        @Test
        @DisplayName("Camera width = 720 px")
        fun cameraWidth() = assertEquals(720f, GameConfig.CAMERA_WIDTH)

        @Test
        @DisplayName("Camera height = 1280 px")
        fun cameraHeight() = assertEquals(1280f, GameConfig.CAMERA_HEIGHT)

        @Test
        @DisplayName("Target FPS = 60")
        fun targetFps() = assertEquals(60, GameConfig.TARGET_FPS)

        @Test
        @DisplayName("Max entities >= 500")
        fun maxEntities() = assertTrue(GameConfig.MAX_ENTITIES >= 500, "MAX_ENTITIES should be at least 500")
    }

    @Nested
    @DisplayName("§13.2 Physics Constants")
    inner class PhysicsConstantsTest {
        @Test
        @DisplayName("Enemy knockback = 150 px/s")
        fun enemyKnockback() = assertEquals(150f, GameConfig.ENEMY_KNOCKBACK)

        @Test
        @DisplayName("Player knockback = 80 px/s")
        fun playerKnockback() = assertEquals(80f, GameConfig.PLAYER_KNOCKBACK)

        @Test
        @DisplayName("Projectile lifetime = 2.0s")
        fun projectileLifetime() = assertEquals(2.0f, GameConfig.PROJECTILE_LIFETIME)

        @Test
        @DisplayName("Gem magnet speed = 600 px/s")
        fun gemMagnetSpeed() = assertEquals(600f, GameConfig.GEM_MAGNET_SPEED)

        @Test
        @DisplayName("Gem lifetime = 30s")
        fun gemLifetime() = assertEquals(30f, GameConfig.GEM_LIFETIME)
    }

    @Nested
    @DisplayName("§8.1 Enemy Base Stats")
    inner class EnemyStatsTest {
        /**
         * Helper: spawn an enemy of the given type and return its base HP.
         */
        private fun getEnemyBaseHp(type: EnemyComponent.EnemyData): Float {
            val state = GameState()
            val idx = state.spawnEnemy(0f, 0f, type)
            return state.healths[idx].maxHp
        }

        @Test
        @DisplayName("Zombie: HP=20")
        fun zombieHp() = assertEquals(20f, getEnemyBaseHp(EnemyComponent.EnemyData.ZOMBIE))

        @Test
        @DisplayName("Runner: HP=15")
        fun runnerHp() = assertEquals(15f, getEnemyBaseHp(EnemyComponent.EnemyData.RUNNER))

        @Test
        @DisplayName("Brute: HP=100")
        fun bruteHp() = assertEquals(100f, getEnemyBaseHp(EnemyComponent.EnemyData.BRUTE))

        @Test
        @DisplayName("Spitter: HP=40")
        fun spitterHp() = assertEquals(40f, getEnemyBaseHp(EnemyComponent.EnemyData.SPITTER))

        @Test
        @DisplayName("Bomber: HP=30")
        fun bomberHp() = assertEquals(30f, getEnemyBaseHp(EnemyComponent.EnemyData.BOMBER))

        @Test
        @DisplayName("Healer: HP=50")
        fun healerHp() = assertEquals(50f, getEnemyBaseHp(EnemyComponent.EnemyData.HEALER))

        @Test
        @DisplayName("Shielder: HP=60")
        fun shielderHp() = assertEquals(60f, getEnemyBaseHp(EnemyComponent.EnemyData.SHIELDER))

        @Test
        @DisplayName("Flyer: HP=25")
        fun flyerHp() = assertEquals(25f, getEnemyBaseHp(EnemyComponent.EnemyData.FLYER))

        @Test
        @DisplayName("Boss: HP=4000+")
        fun bossHp() {
            val hp = getEnemyBaseHp(EnemyComponent.EnemyData.BOSS)
            assertTrue(hp >= 4000f, "Boss HP should be 4000+ (got $hp)")
        }
    }

    @Nested
    @DisplayName("§8.2 Enemy Scaling Per Minute")
    inner class EnemyScalingTest {
        @Test
        @DisplayName("HP scale = +15% per minute")
        fun hpScale() = assertEquals(0.15f, GameConfig.ENEMY_HP_SCALE)

        @Test
        @DisplayName("Damage scale = +8% per minute")
        fun damageScale() = assertEquals(0.08f, GameConfig.ENEMY_DAMAGE_SCALE)

        @Test
        @DisplayName("Spawn rate scale = +20% per minute")
        fun spawnRateScale() = assertEquals(0.20f, GameConfig.ENEMY_SPAWN_RATE_SCALE)

        @Test
        @DisplayName("Elite base chance = 5%")
        fun eliteBaseChance() = assertEquals(0.05f, GameConfig.ELITE_BASE_CHANCE)

        @Test
        @DisplayName("Elite scale = +0.5% per minute")
        fun eliteScale() = assertEquals(0.005f, GameConfig.ELITE_SCALE)
    }

    @Nested
    @DisplayName("§7 Tower Types")
    inner class TowerStatsTest {
        @Test
        @DisplayName("Gun Turret: Cost=50, Dmg=15, Range=150, Rate=1.0/s")
        fun gunTurret() {
            val t = TowerType.GUN_TURRET
            assertEquals(50, t.baseCost)
            assertEquals(15f, t.baseDamage)
            assertEquals(150f, t.baseRange)
            assertEquals(1.0f, t.baseFireRate)
        }

        @Test
        @DisplayName("Cannon: Cost=100, Dmg=40, Range=120, Rate=0.5/s")
        fun cannon() {
            val t = TowerType.CANNON
            assertEquals(100, t.baseCost)
            assertEquals(40f, t.baseDamage)
            assertEquals(120f, t.baseRange)
            assertEquals(0.5f, t.baseFireRate)
        }

        @Test
        @DisplayName("Frost Tower: Cost=75, Dmg=5, Range=130, Rate=0.8/s")
        fun frostTower() {
            val t = TowerType.FROST_TOWER
            assertEquals(75, t.baseCost)
            assertEquals(5f, t.baseDamage)
            assertEquals(130f, t.baseRange)
            assertEquals(0.8f, t.baseFireRate)
        }

        @Test
        @DisplayName("Tesla Coil: Cost=120, Dmg=25, Range=110, Rate=0.7/s")
        fun teslaCoil() {
            val t = TowerType.TESLA_COIL
            assertEquals(120, t.baseCost)
            assertEquals(25f, t.baseDamage)
            assertEquals(110f, t.baseRange)
            assertEquals(0.7f, t.baseFireRate)
        }

        @Test
        @DisplayName("Poison Tower: Cost=80, Dmg=3, Range=140, Rate=1.0/s")
        fun poisonTower() {
            val t = TowerType.POISON_TOWER
            assertEquals(80, t.baseCost)
            assertEquals(3f, t.baseDamage)
            assertEquals(140f, t.baseRange)
            assertEquals(1.0f, t.baseFireRate)
        }

        @Test
        @DisplayName("Rocket Pod: Cost=150, Dmg=60, Range=200, Rate=0.3/s")
        fun rocketPod() {
            val t = TowerType.ROCKET_POD
            assertEquals(150, t.baseCost)
            assertEquals(60f, t.baseDamage)
            assertEquals(200f, t.baseRange)
            assertEquals(0.3f, t.baseFireRate)
        }

        @Test
        @DisplayName("Max towers = 8")
        fun maxTowers() = assertEquals(8, GameConfig.MAX_TOWERS)
    }

    @Nested
    @DisplayName("§6.1 XP & Leveling")
    inner class XpLevelingTest {
        @Test
        @DisplayName("XP for level uses formula: 5 + level * 3")
        fun xpFormula() {
            // GDD says Level N→N+1: 5 + N * 3 XP (linear)
            assertEquals(8, GameConfig.xpForLevel(1))   // Lv1→2: 5 + 1*3 = 8
            assertEquals(11, GameConfig.xpForLevel(2))  // Lv2→3: 5 + 2*3 = 11
            assertEquals(35, GameConfig.xpForLevel(10)) // Lv10→11: 5 + 10*3 = 35
            assertEquals(53, GameConfig.xpForLevel(16)) // Lv16→17: 5 + 16*3 = 53
        }

        @Test
        @DisplayName("Max level = 30")
        fun maxLevel() = assertEquals(30, GameConfig.MAX_LEVEL)

        @Test
        @DisplayName("Max weapon slots = 6")
        fun maxWeaponSlots() = assertEquals(6, GameConfig.MAX_WEAPON_SLOTS)

        @Test
        @DisplayName("Max passive slots = 6")
        fun maxPassiveSlots() = assertEquals(6, GameConfig.MAX_PASSIVE_SLOTS)

        @Test
        @DisplayName("Max weapon level (pre-evolution) = 5")
        fun maxWeaponLevel() = assertEquals(5, GameConfig.MAX_WEAPON_LEVEL)
    }

    @Nested
    @DisplayName("§4.2 Weapon Roster (12 weapons)")
    inner class WeaponRosterTest {
        @Test
        @DisplayName("Exactly 12 weapon types")
        fun weaponCount() = assertEquals(12, WeaponType.entries.size)

        @Test
        @DisplayName("All 12 weapons have evolution names")
        fun allWeaponsHaveEvolutions() {
            for (w in WeaponType.entries) {
                assertTrue(w.evolved != null, "${w.displayName} should have an evolution name")
            }
        }
    }

    @Nested
    @DisplayName("§5.2 Passive Item Roster (12 items)")
    inner class PassiveRosterTest {
        @Test
        @DisplayName("Exactly 12 passive types")
        fun passiveCount() = assertEquals(12, PassiveType.entries.size)

        @Test
        @DisplayName("Each passive is a catalyst for exactly one weapon")
        fun allPassivesHaveCatalysts() {
            for (p in PassiveType.entries) {
                assertTrue(p.catalystFor != null, "${p.displayName} should be a catalyst for a weapon")
            }
        }
    }

    @Nested
    @DisplayName("§10 Economy")
    inner class EconomyTest {
        @Test
        @DisplayName("Gold per kill = 2")
        fun goldPerKill() = assertEquals(2, GameConfig.GOLD_PER_KILL)

        @Test
        @DisplayName("Gold chest min = 50")
        fun goldChestMin() = assertEquals(50, GameConfig.GOLD_CHEST_MIN)

        @Test
        @DisplayName("Gold chest max = 200")
        fun goldChestMax() = assertEquals(200, GameConfig.GOLD_CHEST_MAX)

        @Test
        @DisplayName("Gem values: small=1, medium=5, large=20, boss=100")
        fun gemValues() {
            assertEquals(1, GameConfig.GEM_SMALL_XP)
            assertEquals(5, GameConfig.GEM_MEDIUM_XP)
            assertEquals(20, GameConfig.GEM_LARGE_XP)
            assertEquals(100, GameConfig.GEM_BOSS_XP)
        }
    }

    @Nested
    @DisplayName("§8.3 Match Structure")
    inner class MatchStructureTest {
        @Test
        @DisplayName("Match duration = 900s (15 minutes)")
        fun matchDuration() = assertEquals(900, GameConfig.MATCH_DURATION_SECONDS)

        @Test
        @DisplayName("Boss times = minutes 5, 10, 15")
        fun bossTimes() = assertEquals(listOf(5, 10, 15), GameConfig.BOSS_TIMES_MINUTES)

        @Test
        @DisplayName("Build phase duration = 10s")
        fun buildPhaseDuration() = assertEquals(10f, GameConfig.BUILD_PHASE_DURATION)
    }

    @Nested
    @DisplayName("§4.3 Status Effects (7 types)")
    inner class StatusEffectTest {
        @Test
        @DisplayName("Exactly 7 status effect types")
        fun statusEffectCount() = assertEquals(7, StatusEffectType.entries.size)

        @Test
        @DisplayName("All expected status types exist")
        fun expectedStatusTypes() {
            val expected = setOf("BURN", "POISON", "FREEZE", "SLOW", "SLOW_ATTACK", "STUN", "BLEED")
            val actual = StatusEffectType.entries.map { it.name }.toSet()
            assertEquals(expected, actual)
        }
    }
}
