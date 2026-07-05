package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.PositionComponent
import com.survivortd.game.components.ProjectileComponent
import com.survivortd.game.components.StatusEffectsComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.config.GameBalance
import com.survivortd.game.config.WeaponType
import com.survivortd.game.config.PassiveType
import com.survivortd.game.config.StatusEffectType
import com.survivortd.game.core.GameState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tracks a weapon instance owned by the player.
 */
data class WeaponInstance(
    val type: WeaponType,
    var level: Int = 1,
    var cooldownTimer: Float = 0f,
    var isEvolved: Boolean = false,
    // Weapon-specific runtime data
    var orbAngle: Float = 0f,       // For Lightning Orb orbital position
    var droneTargetId: Int = -1,    // For Drone targeting
    var boomReturnPhase: Boolean = false  // For Boomerang
)

/**
 * Tracks passive items owned by the player.
 */
data class PassiveInstance(
    val type: PassiveType,
    var stacks: Int = 1
)

/**
 * Weapon System — auto-fires all equipped weapons every tick.
 *
 * 12 weapons, each with unique fire patterns:
 *  1. ASSAULT_RIFLE — Single bullet to nearest enemy
 *  2. SPREAD_GUN    — Cone of bullets toward nearest enemy
 *  3. KATANA        — Melee arc in front of player (hits all enemies in arc)
 *  4. LIGHTNING_ORB — Orbital orb that damages enemies on contact
 *  5. ROCKET        — Slow projectile to densest cluster, AoE on impact
 *  6. FORCE_FIELD   — PBAoE bubble around player, damages on contact
 *  7. DRONE         — Stationary near player, fires at nearest enemy
 *  8. FROST_NOVA    — Periodic burst, slows all enemies in range
 *  9. BOOMERANG     — Throws outward then returns, pierces all
 * 10. LANDMINE      — Drops mine at player position, detonates on proximity
 * 11. HEALING_PULSE — Heals player periodically
 * 12. LASER_BEAM    — Instant hit-scan to nearest enemy (beam effect)
 */
class WeaponSystem(
    private val state: GameState
) {
    val weapons = mutableListOf<WeaponInstance>()
    val passives = mutableListOf<PassiveInstance>()

    // Limit max weapon/passive slots
    val maxWeaponSlots = 6
    val maxPassiveSlots = 6

    /**
     * Add a weapon to the player's arsenal (if not already owned).
     */
    fun addWeapon(type: WeaponType): Boolean {
        if (weapons.any { it.type == type }) return false
        if (weapons.size >= maxWeaponSlots) {
            // Replace weakest (lowest level)
            val weakest = weapons.minByOrNull { it.level } ?: return false
            weapons.remove(weakest)
        }
        weapons.add(WeaponInstance(type = type, level = 1))
        return true
    }

    /**
     * Upgrade an existing weapon by 1 level (max 5, then evolve if catalyst owned).
     * Returns true if upgrade succeeded.
     */
    fun upgradeWeapon(type: WeaponType): Boolean {
        val weapon = weapons.find { it.type == type } ?: return false
        if (weapon.level >= 5 && !weapon.isEvolved) {
            // Check if catalyst passive is owned → evolve
            val catalyst = PassiveType.entries.find { it.catalystFor == type }
            if (catalyst != null && passives.any { it.type == catalyst }) {
                weapon.isEvolved = true
                weapon.level = 6
                return true
            }
            return false  // Max level, no catalyst
        }
        if (weapon.isEvolved && weapon.level >= 6) return false  // Fully maxed
        weapon.level++
        return true
    }

    /**
     * Add a passive item (stacks if already owned).
     */
    fun addPassive(type: PassiveType): Boolean {
        val existing = passives.find { it.type == type }
        if (existing != null) {
            if (existing.stacks >= 5) return false
            existing.stacks++
            return true
        }
        if (passives.size >= maxPassiveSlots) return false
        passives.add(PassiveInstance(type = type, stacks = 1))
        return true
    }

    // ================================================================
    // MAIN UPDATE
    // ================================================================
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        if (state.playerIndex < 0) return

        for (weapon in weapons) {
            weapon.cooldownTimer -= dt * getAttackSpeedMult()
            if (weapon.cooldownTimer <= 0f) {
                fireWeapon(weapon)
                val stats = GameBalance.getWeaponStats(weapon.type)
                val lvl = weapon.level.coerceIn(1, stats.levels.size) - 1
                weapon.cooldownTimer = stats.levels[lvl].cooldown
            }
        }

        // Update orbital weapons (Lightning Orb) every tick regardless of cooldown
        updateOrbital(dt)
        // Update force field damage (continuous)
        updateForceField(dt)
    }

    // ================================================================
    // FIRE WEAPON DISPATCHER
    // ================================================================
    private fun fireWeapon(w: WeaponInstance) {
        when (w.type) {
            WeaponType.ASSAULT_RIFLE -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.GUN_SHOT)
                fireAssaultRifle(w)
            }
            WeaponType.SPREAD_GUN -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.GUN_SHOT)
                fireSpreadGun(w)
            }
            WeaponType.KATANA -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.MAGIC_BLAST)
                fireKatana(w)
            }
            WeaponType.LIGHTNING_ORB -> { /* Orbital — handled in updateOrbital */ }
            WeaponType.ROCKET_LAUNCHER -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.EXPLOSION)
                fireRocket(w)
            }
            WeaponType.FORCE_FIELD -> { /* Continuous — handled in updateForceField */ }
            WeaponType.DRONE -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.GUN_SHOT)
                fireDrone(w)
            }
            WeaponType.FROST_NOVA -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.EXPLOSION)
                fireFrostNova(w)
            }
            WeaponType.BOOMERANG -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.GUN_SHOT)
                fireBoomerang(w)
            }
            WeaponType.LANDMINE -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.GUN_SHOT)
                fireLandmine(w)
            }
            WeaponType.HEALING_PULSE -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.HEAL)
                fireHealingPulse(w)
            }
            WeaponType.LASER_BEAM -> {
                AudioManager.getInstance().playSfx(AudioManager.SfxType.LASER_HUM)
                fireLaserBeam(w)
            }
        }
    }

    // ================================================================
    // TARGETING HELPERS
    // ================================================================
    private fun findNearestEnemy(maxRange: Float = Float.MAX_VALUE): Int {
        val playerPos = state.positions[state.playerIndex]
        var nearestId = -1
        var nearestDistSq = maxRange * maxRange

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue
            val pos = state.positions[i]
            val dx = pos.x - playerPos.x
            val dy = pos.y - playerPos.y
            val distSq = dx * dx + dy * dy
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq
                nearestId = i
            }
        }
        return nearestId
    }

    private fun findDensestCluster(): Int {
        val playerPos = state.positions[state.playerIndex]
        var bestId = -1
        var bestCount = 0

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue
            val pos = state.positions[i]
            var count = 0
            for (j in state.enemies.indices) {
                if (j == i || j >= state.tags.size) continue
                if (state.tags[j].tag != TagComponent.EntityTag.ENEMY) continue
                val dx = state.positions[j].x - pos.x
                val dy = state.positions[j].y - pos.y
                if (dx * dx + dy * dy < 80f * 80f) count++
            }
            // Only target clusters within range
            val pdx = pos.x - playerPos.x
            val pdy = pos.y - playerPos.y
            if (pdx * pdx + pdy * pdy > 400f * 400f) continue

            if (count > bestCount) {
                bestCount = count
                bestId = i
            }
        }
        return if (bestId >= 0) bestId else findNearestEnemy(400f)
    }

    // ================================================================
    // WEAPON: ASSAULT RIFLE
    // ================================================================
    private fun fireAssaultRifle(w: WeaponInstance) {
        val target = findNearestEnemy(500f)
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val lvl = (w.level - 1).coerceIn(0, stats.levels.lastIndex)
        val s = stats.levels[lvl]
        val playerPos = state.positions[state.playerIndex]
        val targetPos = state.positions[target]

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dist = sqrt(dx * dx + dy * dy)
        val dirX = if (dist > 0.1f) dx / dist else 1f
        val dirY = if (dist > 0.1f) dy / dist else 0f

        val count = if (w.isEvolved) {
            // Evolved Minigun: fires 6 bullets in tight spread
            6 + getExtraProjectiles()
        } else {
            s.projectileCount + getExtraProjectiles()
        }
        for (i in 0 until count) {
            val spread = if (count > 1) (i - count / 2f) * 0.08f else 0f
            val angle = atan2(dirY, dirX) + spread
            spawnProjectile(
                x = playerPos.x, y = playerPos.y,
                vx = cos(angle) * s.projectileSpeed,
                vy = sin(angle) * s.projectileSpeed,
                damage = s.damage * getDamageMult(),
                pierce = s.pierce,
                lifetime = s.range / s.projectileSpeed,
                ownerWeapon = w.type,
                isEvolved = w.isEvolved
            )
        }
    }

    // ================================================================
    // WEAPON: SPREAD GUN
    // ================================================================
    private fun fireSpreadGun(w: WeaponInstance) {
        val target = findNearestEnemy(safeStat(w, "range"))
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val lvl = (w.level - 1).coerceIn(0, stats.levels.lastIndex)
        val s = stats.levels[lvl]
        val playerPos = state.positions[state.playerIndex]
        val targetPos = state.positions[target]

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val baseAngle = atan2(dy, dx)
        val count = if (w.isEvolved) {
            // Evolved Plasma Cannon: wider cone, more projectiles
            8 + getExtraProjectiles()
        } else {
            s.projectileCount + getExtraProjectiles()
        }
        val spreadAngle = if (w.isEvolved) 1.2f else 0.6f

        for (i in 0 until count) {
            val offset = if (count > 1) (i.toFloat() / (count - 1) - 0.5f) * spreadAngle else 0f
            val angle = baseAngle + offset
            spawnProjectile(
                x = playerPos.x, y = playerPos.y,
                vx = cos(angle) * s.projectileSpeed,
                vy = sin(angle) * s.projectileSpeed,
                damage = s.damage * getDamageMult(),
                pierce = if (w.isEvolved) 2 else s.pierce,
                lifetime = s.range / s.projectileSpeed,
                ownerWeapon = w.type,
                isEvolved = w.isEvolved
            )
        }
    }

    // ================================================================
    // WEAPON: KATANA (Melee Arc)
    // ================================================================
    private fun fireKatana(w: WeaponInstance) {
        val stats = GameBalance.getWeaponStats(w.type)
        val lvl = (w.level - 1).coerceIn(0, stats.levels.lastIndex)
        val s = stats.levels[lvl]
        val playerPos = state.positions[state.playerIndex]
        val dmg = s.damage * getDamageMult()
        val range = s.range
        var killed = 0

        // Evolved Whirlwind Blade: 360° full circle instead of frontal arc
        val arcHalfWidth = if (w.isEvolved) PI else 1.57f

        // Find nearest enemy to determine facing (irrelevant for 360°)
        val nearest = findNearestEnemy(range)
        val facingAngle = if (nearest >= 0) {
            val tp = state.positions[nearest]
            atan2(tp.y - playerPos.y, tp.x - playerPos.x)
        } else 0f

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue
            val pos = state.positions[i]
            val dx = pos.x - playerPos.x
            val dy = pos.y - playerPos.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > range) continue
            if (w.isEvolved) {
                // 360° — no angle check needed
                dealDamage(i, dmg, w.type)
                applyStatus(i, StatusEffectType.BLEED, 4f, dmg * 0.15f)
                if (state.healths[i].isDead) killed++
            } else {
                val angle = atan2(dy, dx)
                val angleDiff = abs(normalizeAngle(angle - facingAngle))
                if (angleDiff <= arcHalfWidth) {
                    dealDamage(i, dmg, w.type)
                    applyStatus(i, StatusEffectType.BLEED, 4f, dmg * 0.15f)
                    if (state.healths[i].isDead) killed++
                }
            }
        }
    }

    // ================================================================
    // WEAPON: ROCKET LAUNCHER (AoE projectile)
    // ================================================================
    private fun fireRocket(w: WeaponInstance) {
        val target = findDensestCluster()
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val targetPos = state.positions[target]

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dist = sqrt(dx * dx + dy * dy)
        val dirX = if (dist > 0.1f) dx / dist else 1f
        val dirY = if (dist > 0.1f) dy / dist else 0f

        val rocketCount = if (w.isEvolved) {
            // Evolved Missile Barrage: 3 rockets in a fan
            3
        } else 1

        for (r in 0 until rocketCount) {
            val spread = if (rocketCount > 1) (r - rocketCount / 2f) * 0.25f else 0f
            val angle = atan2(dirY, dirX) + spread
            spawnProjectile(
                x = playerPos.x, y = playerPos.y,
                vx = cos(angle) * s.projectileSpeed,
                vy = sin(angle) * s.projectileSpeed,
                damage = s.damage * getDamageMult(),
                pierce = 0,
                lifetime = 3f,
                aoeRadius = s.aoeRadius * getAoEMult(),
                onHitEffect = StatusEffectType.BURN,
                onHitEffectDuration = 3f,
                onHitEffectMagnitude = 8f * getDamageMult() * 0.2f,
                ownerWeapon = w.type,
                isEvolved = w.isEvolved
            )
        }
    }

    // ================================================================
    // WEAPON: DRONE
    // ================================================================
    private fun fireDrone(w: WeaponInstance) {
        val target = findNearestEnemy(350f)
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val targetPos = state.positions[target]

        // Drone hovers at offset from player, fires at target
        val droneX = playerPos.x + 40f
        val droneY = playerPos.y - 40f
        val dx = targetPos.x - droneX
        val dy = targetPos.y - droneY
        val dist = sqrt(dx * dx + dy * dy)
        val dirX = if (dist > 0.1f) dx / dist else 1f
        val dirY = if (dist > 0.1f) dy / dist else 0f

        spawnProjectile(
            x = droneX, y = droneY,
            vx = dirX * s.projectileSpeed,
            vy = dirY * s.projectileSpeed,
            damage = s.damage * getDamageMult(),
            pierce = if (w.isEvolved) 2 else 0,
            lifetime = 1.5f,
            ownerWeapon = w.type,
            isEvolved = w.isEvolved
        )
    }

    // ================================================================
    // WEAPON: FROST NOVA (PBAoE slow)
    // ================================================================
    private fun fireFrostNova(w: WeaponInstance) {
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val dmg = s.damage * getDamageMult()
        val range = s.range
        val slowDuration = s.duration * getSlowDurationMult()
        val slowMag = 0.5f * getSlowMagnitudeMult()

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue
            val pos = state.positions[i]
            val dx = pos.x - playerPos.x
            val dy = pos.y - playerPos.y
            if (dx * dx + dy * dy <= range * range) {
                dealDamage(i, dmg, w.type)
                applyStatus(i, StatusEffectType.SLOW, slowDuration, slowMag)
                if (w.isEvolved) {
                    applyStatus(i, StatusEffectType.FREEZE, 1f, 1f)
                }
            }
        }
    }

    // ================================================================
    // WEAPON: BOOMERANG (out and back, pierces all)
    // ================================================================
    private fun fireBoomerang(w: WeaponInstance) {
        val target = findNearestEnemy(400f)
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val targetPos = state.positions[target]

        val dx = targetPos.x - playerPos.x
        val dy = targetPos.y - playerPos.y
        val dist = sqrt(dx * dx + dy * dy)
        val dirX = if (dist > 0.1f) dx / dist else 1f
        val dirY = if (dist > 0.1f) dy / dist else 0f

        val boomCount = if (w.isEvolved) {
            // Evolved Razor Edge: 3 boomerangs in a fan
            3
        } else 1

        for (b in 0 until boomCount) {
            val spread = if (boomCount > 1) (b - boomCount / 2f) * 0.5f else 0f
            val angle = atan2(dirY, dirX) + spread
            spawnProjectile(
                x = playerPos.x, y = playerPos.y,
                vx = cos(angle) * s.projectileSpeed,
                vy = sin(angle) * s.projectileSpeed,
                damage = s.damage * getDamageMult(),
                pierce = 999,
                lifetime = 1.5f,
                isBoomerang = true,
                ownerWeapon = w.type,
                isEvolved = w.isEvolved
            )
        }
    }

    // ================================================================
    // WEAPON: LANDMINE
    // ================================================================
    private fun fireLandmine(w: WeaponInstance) {
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]

        val mineCount = if (w.isEvolved) {
            // Evolved Minefield: drops 3 mines in a spread
            3
        } else 1

        for (m in 0 until mineCount) {
            val offsetX = if (mineCount > 1) (m - mineCount / 2f) * 30f else 0f
            spawnProjectile(
                x = playerPos.x + offsetX, y = playerPos.y,
                vx = 0f, vy = 0f,
                damage = s.damage * getDamageMult(),
                pierce = 0,
                lifetime = 10f,
                aoeRadius = s.aoeRadius * getAoEMult(),
                isMine = true,
                ownerWeapon = w.type,
                isEvolved = w.isEvolved
            )
        }
    }

    // ================================================================
    // WEAPON: HEALING PULSE
    // ================================================================
    private fun fireHealingPulse(w: WeaponInstance) {
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerHealth = state.healths[state.playerIndex]
        val heal = s.damage * (1f + getRegenMult())

        if (w.isEvolved) {
            // Evolved Regen Aura: heals player AND nearby allies within range
            playerHealth.currentHp = (playerHealth.currentHp + heal).coerceAtMost(playerHealth.maxHp)
            val playerPos = state.positions[state.playerIndex]
            val healRange = s.range
            for (i in state.enemies.indices) {
                // Regen Aura also grants player a brief damage shield — applied as self-buff
                // For now: just increased self-heal (already handled by stats L6)
            }
            // Evolved bonus: heal is 50% stronger
            playerHealth.currentHp = (playerHealth.currentHp + heal * 0.5f).coerceAtMost(playerHealth.maxHp)
        } else {
            playerHealth.currentHp = (playerHealth.currentHp + heal).coerceAtMost(playerHealth.maxHp)
        }
    }

    // ================================================================
    // WEAPON: LASER BEAM (instant hitscan)
    // ================================================================
    private fun fireLaserBeam(w: WeaponInstance) {
        val target = findNearestEnemy(safeStat(w, "range"))
        if (target < 0) return
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val dmg = s.damage * getDamageMult()

        if (w.isEvolved) {
            // Death Ray: pierces through to up to 5 enemies in a line
            val playerPos = state.positions[state.playerIndex]
            val targetPos = state.positions[target]
            val dx = targetPos.x - playerPos.x
            val dy = targetPos.y - playerPos.y
            val baseAngle = atan2(dy, dx)

            val hitList = mutableListOf<Int>()
            for (i in state.enemies.indices) {
                if (i >= state.tags.size) break
                if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
                if (state.healths[i].isDead) continue
                val pos = state.positions[i]
                val edx = pos.x - playerPos.x
                val edy = pos.y - playerPos.y
                val angle = atan2(edy, edx)
                val angleDiff = abs(normalizeAngle(angle - baseAngle))
                val dist = sqrt(edx * edx + edy * edy)
                if (angleDiff < 0.15f && dist < s.range) {
                    hitList.add(i)
                    if (hitList.size >= 5) break
                }
            }
            for (id in hitList) dealDamage(id, dmg, w.type)
        } else {
            dealDamage(target, dmg, w.type)
        }
    }

    // ================================================================
    // CONTINUOUS: LIGHTNING ORB (orbital)
    // ================================================================
    private fun updateOrbital(dt: Float) {
        val orb = weapons.find { it.type == WeaponType.LIGHTNING_ORB } ?: return
        val stats = GameBalance.getWeaponStats(orb.type)
        val s = stats.levels[(orb.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val dmg = s.damage * getDamageMult() * dt * 5f  // Continuous DPS
        val orbRadius = s.range
        val orbSpeed = 2f * (1f + getOrbSpeedMult())
        val orbCount = if (orb.isEvolved) 2 else 1

        orb.orbAngle += orbSpeed * dt

        for (o in 0 until orbCount) {
            val angle = orb.orbAngle + (o * PI / orbCount).toFloat()
            val orbX = playerPos.x + cos(angle) * 70f
            val orbY = playerPos.y + sin(angle) * 70f

            // Damage enemies touching the orb
            for (i in state.enemies.indices) {
                if (i >= state.tags.size) break
                if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
                if (state.healths[i].isDead) continue
                val pos = state.positions[i]
                val dx = pos.x - orbX
                val dy = pos.y - orbY
                if (dx * dx + dy * dy <= (orbRadius + 15f) * (orbRadius + 15f)) {
                    dealDamage(i, dmg, orb.type)
                    // Evolved Thunder Storm: static discharge slows enemy attacks (GDD §3.3, issue #52)
                    if (orb.isEvolved) {
                        applyStatus(i, StatusEffectType.SLOW_ATTACK, 2f, 0.35f)
                    }
                }
            }
        }
    }

    // ================================================================
    // CONTINUOUS: FORCE FIELD (PBAoE)
    // ================================================================
    private fun updateForceField(dt: Float) {
        val ff = weapons.find { it.type == WeaponType.FORCE_FIELD } ?: return
        val stats = GameBalance.getWeaponStats(ff.type)
        val s = stats.levels[(ff.level - 1).coerceIn(0, stats.levels.lastIndex)]
        val playerPos = state.positions[state.playerIndex]
        val dmg = s.damage * getDamageMult() * dt * 3f
        val radius = s.range * getAoEMult()

        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue
            val pos = state.positions[i]
            val dx = pos.x - playerPos.x
            val dy = pos.y - playerPos.y
            if (dx * dx + dy * dy <= radius * radius) {
                dealDamage(i, dmg, ff.type)
                // Evolved Plasma Shield: slows enemies that touch the shield
                if (ff.isEvolved) {
                    applyStatus(i, StatusEffectType.SLOW, 1.5f, 0.4f)
                }
            }
        }
    }

    // ================================================================
    // DAMAGE APPLICATION HELPER
    // ================================================================
    private fun dealDamage(enemyIndex: Int, baseDamage: Float, weaponType: WeaponType) {
        if (enemyIndex < 0 || enemyIndex >= state.healths.size) return
        val health = state.healths[enemyIndex]
        if (health.isDead) return

        var damage = baseDamage
        // Apply shielder damage reduction
        if (enemyIndex < state.enemies.size && state.enemies[enemyIndex].knockbackResist >= 2f) {
            damage *= 0.5f  // Shielded enemies take 50% damage
        }
        // Apply armor
        damage = (damage - health.armor).coerceAtLeast(damage * 0.1f)

        // Crit check
        val playerComp = state.players.getOrNull(state.playerIndex)
        val isCrit = playerComp != null && kotlin.random.Random.nextFloat() < playerComp.critChance
        if (isCrit) damage *= 1.5f

        health.currentHp -= damage
    }

    // ================================================================
    // STATUS EFFECT APPLICATION
    // ================================================================
    private fun applyStatus(enemyIndex: Int, type: StatusEffectType, duration: Float, magnitude: Float) {
        if (enemyIndex < 0 || enemyIndex >= state.statusEffects.size) return
        val se = state.statusEffects[enemyIndex]
        // Add or refresh status
        val existing = se.effects.find { it.type == type }
        if (existing != null) {
            existing.duration = maxOf(existing.duration, duration)
        } else {
            se.effects.add(
                com.survivortd.game.components.StatusEffectsComponent.ActiveStatus(
                    type = type, duration = duration, magnitude = magnitude
                )
            )
        }
    }

    // ================================================================
    // PROJECTILE SPAWN HELPER
    // ================================================================
    private fun spawnProjectile(
        x: Float, y: Float, vx: Float, vy: Float,
        damage: Float, pierce: Int, lifetime: Float,
        aoeRadius: Float = 0f, isBoomerang: Boolean = false,
        isMine: Boolean = false, ownerWeapon: WeaponType,
        isEvolved: Boolean = false,
        onHitEffect: StatusEffectType? = null,
        onHitEffectDuration: Float = 0f,
        onHitEffectMagnitude: Float = 0f
    ) {
        val id = state.spawnProjectile(x = x, y = y)
        state.velocities[id].x = vx
        state.velocities[id].y = vy
        state.projectiles[id].damage = damage
        state.projectiles[id].pierceCount = pierce
        state.projectiles[id].lifetime = lifetime
        if (aoeRadius > 0f) state.projectiles[id].aoeRadius = aoeRadius
        state.projectiles[id].isBoomerang = isBoomerang
        state.projectiles[id].isMine = isMine
        state.projectiles[id].ownerWeapon = ownerWeapon
        state.projectiles[id].onHitEffect = onHitEffect
        state.projectiles[id].onHitEffectDuration = onHitEffectDuration
        state.projectiles[id].onHitEffectMagnitude = onHitEffectMagnitude
        applyProjectileVisuals(id, ownerWeapon, isEvolved)
    }

    /**
     * Override projectile render properties based on weapon type.
     * Each weapon gets a unique color, size, and shape per GDD §20.
     * Evolved forms get distinct visuals (brighter colors, different shapes).
     */
    private fun applyProjectileVisuals(id: Int, weapon: WeaponType, isEvolved: Boolean = false) {
        if (id >= state.renders.size) return
        val render = state.renders[id]
        when (weapon) {
            WeaponType.ASSAULT_RIFLE -> {
                if (isEvolved) {
                    render.color = 0xFFFFD600.toInt()  // Gold Minigun
                    render.radius = 5f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                } else {
                    render.color = 0xFFFFFF00.toInt()  // Yellow (physical)
                    render.radius = 4f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.SPREAD_GUN -> {
                if (isEvolved) {
                    render.color = 0xFFE040FB.toInt()  // Purple Plasma Cannon
                    render.radius = 5f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                } else {
                    render.color = 0xFFFF6F00.toInt()  // Orange (fire)
                    render.radius = 3f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.KATANA -> {
                if (isEvolved) {
                    render.color = 0xFF76FF03.toInt()  // Lime Whirlwind Blade
                    render.radius = 22f  // Larger 360° arc
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                } else {
                    render.color = 0xFFFFFFFF.toInt()  // White (melee)
                    render.radius = 18f  // Large arc
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.TRIANGLE
                }
            }
            WeaponType.LIGHTNING_ORB -> {
                if (isEvolved) {
                    render.color = 0xFF2979FF.toInt()  // Electric blue Thunder Storm
                    render.radius = 10f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                } else {
                    render.color = 0xFFFFF700.toInt()  // Bright yellow (lightning)
                    render.radius = 8f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.ROCKET_LAUNCHER -> {
                if (isEvolved) {
                    render.color = 0xFFFF1744.toInt()  // Red Missile Barrage
                    render.radius = 9f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                } else {
                    render.color = 0xFFFF4500.toInt()  // Orange-red (fire)
                    render.radius = 7f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                }
            }
            WeaponType.FORCE_FIELD -> {
                if (isEvolved) {
                    render.color = 0xB029B6F6.toInt()  // Blue-tinted Plasma Shield
                    render.radius = 28f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                } else {
                    render.color = 0x8000FFFF.toInt()  // Cyan semi-transparent
                    render.radius = 24f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.DRONE -> {
                if (isEvolved) {
                    render.color = 0xFFFF4081.toInt()  // Pink Drone Swarm
                    render.radius = 6f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                } else {
                    render.color = 0xFF42A5F5.toInt()  // Blue
                    render.radius = 5f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                }
            }
            WeaponType.FROST_NOVA -> {
                if (isEvolved) {
                    render.color = 0xE040C4FF.toInt()  // Bright blue Absolute Zero
                    render.radius = 36f  // Larger AoE
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                } else {
                    render.color = 0xFF00FFFF.toInt()  // Cyan (ice)
                    render.radius = 32f  // Large AoE circle
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.BOOMERANG -> {
                if (isEvolved) {
                    render.color = 0xFFFFC400.toInt()  // Gold Razor Edge
                    render.radius = 7f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                } else {
                    render.color = 0xFFB0BEC5.toInt()  // Silver
                    render.radius = 6f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.DIAMOND
                }
            }
            WeaponType.LANDMINE -> {
                if (isEvolved) {
                    render.color = 0xFFFF6D00.toInt()  // Orange Minefield
                    render.radius = 7f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                } else {
                    render.color = 0xFF795548.toInt()  // Brown
                    render.radius = 6f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                }
            }
            WeaponType.HEALING_PULSE -> {
                if (isEvolved) {
                    render.color = 0xFF00E676.toInt()  // Bright green Regen Aura
                    render.radius = 20f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                } else {
                    render.color = 0xFF66BB6A.toInt()  // Green
                    render.radius = 16f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.CIRCLE
                }
            }
            WeaponType.LASER_BEAM -> {
                if (isEvolved) {
                    render.color = 0xFFFF0000.toInt()  // Pure red Death Ray
                    render.radius = 5f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                } else {
                    render.color = 0xFFFF1744.toInt()  // Red
                    render.radius = 3f
                    render.shape = com.survivortd.game.components.RenderComponent.RenderShape.RECT
                }
            }
        }
    }

    // ================================================================
    // PASSIVE MULTIPLIERS
    // ================================================================
    private fun getDamageMult(): Float {
        var mult = 1f
        passives.find { it.type == PassiveType.POWER_CORE }?.let {
            mult += 0.15f * it.stacks
        }
        return mult
    }

    private fun getAttackSpeedMult(): Float {
        var mult = 1f
        passives.find { it.type == PassiveType.RAPID_LOADER }?.let {
            mult += 0.10f * it.stacks
        }
        passives.find { it.type == PassiveType.CPU_UPGRADE }?.let {
            mult += 0.10f * it.stacks
        }
        return mult
    }

    private fun getExtraProjectiles(): Int {
        return passives.find { it.type == PassiveType.EXPANDED_MAGAZINE }?.stacks ?: 0
    }

    private fun getAoEMult(): Float {
        var mult = 1f
        passives.find { it.type == PassiveType.HEAVY_CALIBER }?.let {
            mult += 0.20f * it.stacks
        }
        passives.find { it.type == PassiveType.BATTERY }?.let {
            mult += 0.10f * it.stacks
        }
        return mult
    }

    private fun getSlowDurationMult(): Float {
        return 1f + (passives.find { it.type == PassiveType.CRYO_MODULE }?.let { 0.15f * it.stacks } ?: 0f)
    }

    private fun getSlowMagnitudeMult(): Float {
        return 1f + (passives.find { it.type == PassiveType.CRYO_MODULE }?.let { 0.15f * it.stacks } ?: 0f)
    }

    private fun getOrbSpeedMult(): Float {
        return passives.find { it.type == PassiveType.HIGH_VOLTAGE }?.let { 0.15f * it.stacks } ?: 0f
    }

    private fun getRegenMult(): Float {
        return passives.find { it.type == PassiveType.MED_KIT }?.let { 0.1f * it.stacks } ?: 0f
    }

    // ================================================================
    // UTILITIES
    // ================================================================
    private fun normalizeAngle(a: Float): Float {
        var result = a
        while (result > PI) result -= 2f * PI
        while (result < -PI) result += 2f * PI
        return result
    }

    private fun safeStat(w: WeaponInstance, field: String): Float {
        val stats = GameBalance.getWeaponStats(w.type)
        val s = stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)]
        return when (field) {
            "range" -> s.range
            "damage" -> s.damage
            else -> 300f
        }
    }

    companion object {
        private const val PI = 3.14159265f
    }
}
