package com.survivortd.game.systems

import com.survivortd.game.config.GameBalance
import com.survivortd.game.config.PassiveType
import com.survivortd.game.config.WeaponType
import com.survivortd.game.core.GameState
import kotlin.random.Random

/**
 * Represents a single upgrade choice presented to the player on level-up.
 */
data class UpgradeChoice(
    val type: UpgradeType,
    val title: String,
    val description: String,
    val weaponType: WeaponType? = null,
    val passiveType: PassiveType? = null,
    val iconColor: Long = 0xFF42A5F5,
    val rarity: Rarity = Rarity.COMMON
)

enum class UpgradeType {
    NEW_WEAPON,
    UPGRADE_WEAPON,
    NEW_PASSIVE,
    UPGRADE_PASSIVE,
    HEAL,
    STAT_BOOST
}

enum class Rarity(val color: Long, val weight: Int) {
    COMMON(0xFF9E9E9E, 60),
    RARE(0xFF42A5F5, 30),
    EPIC(0xFFAB47BC, 10)
}

/**
 * Level-Up System — generates random upgrade choices when the player levels up.
 *
 * On each level-up, presents 3-4 choices:
 * - New weapon (if < 6 slots)
 * - Upgrade existing weapon (if any at < max level)
 * - New passive (if < 6 passives)
 * - Upgrade existing passive (if any at < max stacks)
 * - Heal (always available as fallback)
 *
 * Choice generation respects rarity weighting and avoids duplicates.
 */
class LevelUpSystem(
    private val state: GameState,
    private val weaponSystem: WeaponSystem
) {
    /**
     * Generate 3-4 upgrade choices for the current level-up.
     * Called when state.pendingLevelUps > 0.
     */
    fun generateChoices(): List<UpgradeChoice> {
        val pool = mutableListOf<UpgradeChoice>()

        // 1. New weapons (if slots available)
        if (weaponSystem.weapons.size < 6) {
            val ownedTypes = weaponSystem.weapons.map { it.type }.toSet()
            WeaponType.entries.forEach { wt ->
                if (wt !in ownedTypes) {
                    pool.add(UpgradeChoice(
                        type = UpgradeType.NEW_WEAPON,
                        title = "New: ${wt.displayName}",
                        description = "Add ${wt.displayName} to your arsenal",
                        weaponType = wt,
                        iconColor = 0xFFFFD700,
                        rarity = Rarity.RARE
                    ))
                }
            }
        }

        // 2. Upgrade existing weapons
        for (w in weaponSystem.weapons) {
            val stats = GameBalance.getWeaponStats(w.type)
            val maxLevel = if (w.isEvolved) stats.levels.size else stats.levels.size
            if (w.level < maxLevel) {
                val isEvolve = w.level == 5 && hasCatalyst(w.type)
                val title = if (isEvolve) "EVOLVE: ${stats.displayName}!"
                else "Upgrade: ${stats.displayName} Lv.${w.level + 1}"

                val desc = if (isEvolve) {
                    "Transform into ${wt_evolvedName(w.type)}"
                } else {
                    val nextLvl = stats.levels[w.level.coerceIn(0, stats.levels.lastIndex)]
                    "+${(nextLvl.damage - stats.levels[(w.level - 1).coerceIn(0, stats.levels.lastIndex)].damage).toInt()} DMG"
                }

                pool.add(UpgradeChoice(
                    type = UpgradeType.UPGRADE_WEAPON,
                    title = title,
                    description = desc,
                    weaponType = w.type,
                    iconColor = if (isEvolve) 0xFFFF1744 else 0xFF42A5F5,
                    rarity = if (isEvolve) Rarity.EPIC else Rarity.COMMON
                ))
            }
        }

        // 3. New passives (if < 6)
        if (weaponSystem.passives.size < 6) {
            val ownedPassives = weaponSystem.passives.map { it.type }.toSet()
            PassiveType.entries.forEach { pt ->
                if (pt !in ownedPassives) {
                    pool.add(UpgradeChoice(
                        type = UpgradeType.NEW_PASSIVE,
                        title = "New: ${pt.displayName}",
                        description = passiveDescription(pt),
                        passiveType = pt,
                        iconColor = 0xFF66BB6A,
                        rarity = Rarity.RARE
                    ))
                }
            }
        }

        // 4. Upgrade existing passives
        for (p in weaponSystem.passives) {
            if (p.stacks < 5) {
                pool.add(UpgradeChoice(
                    type = UpgradeType.UPGRADE_PASSIVE,
                    title = "Stack: ${p.type.displayName} (${p.stacks + 1}/5)",
                    description = passiveDescription(p.type),
                    passiveType = p.type,
                    iconColor = 0xFF66BB6A,
                    rarity = Rarity.COMMON
                ))
            }
        }

        // 5. Heal (always available)
        pool.add(UpgradeChoice(
            type = UpgradeType.HEAL,
            title = "Heal 30% HP",
            description = "Restore health instantly",
            iconColor = 0xFFEF5350,
            rarity = Rarity.COMMON
        ))

        // 6. Stat boosts (always available)
        pool.add(UpgradeChoice(
            type = UpgradeType.STAT_BOOST,
            title = "Speed Boost +10%",
            description = "Move 10% faster permanently",
            iconColor = 0xFFFFEB3B,
            rarity = Rarity.COMMON
        ))

        // Smart choice generation:
        // 1. Guarantee at least one weapon option (new or upgrade) if any exist
        // 2. Guarantee at least one passive option if any exist
        // 3. Always include HEAL and STAT_BOOST as fallbacks
        // 4. Fill remaining slots with random picks
        val weaponOptions = pool.filter {
            it.type == UpgradeType.NEW_WEAPON || it.type == UpgradeType.UPGRADE_WEAPON
        }
        val passiveOptions = pool.filter {
            it.type == UpgradeType.NEW_PASSIVE || it.type == UpgradeType.UPGRADE_PASSIVE
        }
        val fallbacks = pool.filter {
            it.type == UpgradeType.HEAL || it.type == UpgradeType.STAT_BOOST
        }

        val result = mutableListOf<UpgradeChoice>()

        // Pick one random weapon option
        if (weaponOptions.isNotEmpty()) {
            result.add(weaponOptions[Random.nextInt(weaponOptions.size)])
        }
        // Pick one random passive option
        if (passiveOptions.isNotEmpty()) {
            result.add(passiveOptions[Random.nextInt(passiveOptions.size)])
        }
        // Add fallbacks
        result.addAll(fallbacks)

        // If we still need more, pull from remaining pool
        val remaining = pool.filter { it !in result }.toMutableList()
        while (result.size < 4 && remaining.isNotEmpty()) {
            val idx = Random.nextInt(remaining.size)
            result.add(remaining.removeAt(idx))
        }

        return result.take(4)
    }

    /**
     * Apply the selected upgrade choice to the game state.
     */
    fun applyChoice(choice: UpgradeChoice) {
        when (choice.type) {
            UpgradeType.NEW_WEAPON -> {
                choice.weaponType?.let { weaponSystem.addWeapon(it) }
            }
            UpgradeType.UPGRADE_WEAPON -> {
                choice.weaponType?.let { weaponSystem.upgradeWeapon(it) }
            }
            UpgradeType.NEW_PASSIVE -> {
                choice.passiveType?.let {
                    weaponSystem.addPassive(it)
                    applyPassiveStatEffect(it)
                }
            }
            UpgradeType.UPGRADE_PASSIVE -> {
                choice.passiveType?.let {
                    weaponSystem.addPassive(it)
                    applyPassiveStatEffect(it)
                }
            }
            UpgradeType.HEAL -> {
                if (state.playerIndex >= 0 && state.playerIndex < state.healths.size) {
                    val hp = state.healths[state.playerIndex]
                    hp.currentHp = (hp.currentHp + hp.maxHp * 0.3f).coerceAtMost(hp.maxHp)
                }
            }
            UpgradeType.STAT_BOOST -> {
                if (state.playerIndex >= 0 && state.playerIndex < state.players.size) {
                    state.players[state.playerIndex].moveSpeed *= 1.1f
                }
            }
        }

        // Consume one pending level-up
        state.pendingLevelUps = (state.pendingLevelUps - 1).coerceAtLeast(0)
    }

    /**
     * Apply immediate stat effects when a passive is picked up.
     * Passives that modify multipliers (damage, AoE, etc.) are handled
     * by WeaponSystem's getter methods at fire time.
     */
    private fun applyPassiveStatEffect(pt: PassiveType) {
        if (state.playerIndex < 0) return
        val pi = state.playerIndex
        when (pt) {
            PassiveType.ENERGY_DRINK -> {
                // +10% move speed per stack
                if (pi < state.players.size) {
                    state.players[pi].moveSpeed *= 1.1f
                }
            }
            PassiveType.REINFORCED_PLATING -> {
                // +15 max HP per stack (also heals the bonus amount)
                if (pi < state.healths.size) {
                    val hp = state.healths[pi]
                    hp.maxHp += 15f
                    hp.currentHp = (hp.currentHp + 15f).coerceAtMost(hp.maxHp)
                }
            }
            PassiveType.SHARP_EDGE -> {
                // +15% crit damage per stack
                if (pi < state.players.size) {
                    val p = state.players[pi]
                    p.critDamage += 0.15f
                }
            }
            else -> { /* no immediate stat effect */ }
        }
    }

    private fun hasCatalyst(weaponType: WeaponType): Boolean {
        val catalystPassive = PassiveType.entries.find { it.catalystFor == weaponType }
            ?: return false
        return weaponSystem.passives.any { it.type == catalystPassive && it.stacks > 0 }
    }

    private fun wt_evolvedName(weaponType: WeaponType): String {
        return weaponType.evolved ?: "Evolved Form"
    }

    private fun passiveDescription(pt: PassiveType): String {
        return when (pt) {
            PassiveType.POWER_CORE -> "+15% damage per stack"
            PassiveType.RAPID_LOADER -> "+10% attack speed per stack"
            PassiveType.ENERGY_DRINK -> "+10% move speed per stack"
            PassiveType.HIGH_VOLTAGE -> "+15% orb speed per stack"
            PassiveType.HEAVY_CALIBER -> "+20% AoE per stack"
            PassiveType.REINFORCED_PLATING -> "+15 max HP per stack"
            PassiveType.CPU_UPGRADE -> "+10% attack speed per stack"
            PassiveType.CRYO_MODULE -> "+15% slow duration per stack"
            PassiveType.SHARP_EDGE -> "+15% crit damage per stack"
            PassiveType.EXPANDED_MAGAZINE -> "+1 projectile per stack"
            PassiveType.MED_KIT -> "+2 HP/sec regen per stack"
            PassiveType.BATTERY -> "+10% AoE per stack"
        }
    }

    /**
     * Pick N unique choices from the pool, weighted by rarity.
     */
    private fun pickWeighted(pool: List<UpgradeChoice>, n: Int): List<UpgradeChoice> {
        if (pool.size <= n) return pool.sortedByDescending { it.rarity.weight }

        val result = mutableListOf<UpgradeChoice>()
        val available = pool.toMutableList()

        while (result.size < n && available.isNotEmpty()) {
            val totalWeight = available.sumOf { it.rarity.weight }
            var roll = Random.nextInt(totalWeight)
            var pickedIndex = 0
            for (i in available.indices) {
                roll -= available[i].rarity.weight
                if (roll < 0) {
                    pickedIndex = i
                    break
                }
            }
            result.add(available.removeAt(pickedIndex))
        }

        return result
    }
}
