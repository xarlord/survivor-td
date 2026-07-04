package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Meta-progression — permanent upgrades that persist across matches.
 *
 * Purchased with gold earned from matches.
 * Stored as JSON on disk.
 */
@Serializable
data class MetaProgression(
    var gold: Int = 0,
    var maxHpLevel: Int = 0,           // +20 HP per level, max 10
    var moveSpeedLevel: Int = 0,       // +10 px/s per level, max 5
    var damageLevel: Int = 0,          // +5% damage per level, max 10
    var pickupRangeLevel: Int = 0,     // +10px per level, max 5
    var extraLifeLevel: Int = 0,       // +1 revive per level, max 3
    var xpGainLevel: Int = 0,          // +5% XP per level, max 10
    var goldFindLevel: Int = 0,        // +10% gold per level, max 5
    var towerDiscountLevel: Int = 0,   // -10% tower cost per level, max 3
    var startingWeaponLevel: Int = 0,  // Start match with weapon at Lv.2, max 3
    var chaptersUnlocked: Set<String> = setOf("ch1_wasteland")
) {
    companion object {
        private const val MAX_HP_MAX = 10
        private const val SPEED_MAX = 5
        private const val DAMAGE_MAX = 10
        private const val PICKUP_MAX = 5
        private const val LIFE_MAX = 3
        private const val XP_MAX = 10
        private const val GOLD_MAX = 5
        private const val TOWER_MAX = 3
        private const val WEAPON_MAX = 3

        /** Upgrade cost: doubles each level, starts at baseCost */
        fun upgradeCost(baseCost: Int, currentLevel: Int): Int {
            return baseCost * (1 shl currentLevel)  // baseCost * 2^level
        }

        /**
         * Apply meta-progression bonuses to a new match's GameState.
         * Call this when starting a match.
         */
        fun applyToGameState(progression: MetaProgression, state: GameState) {
            if (state.playerIndex < 0) return

            val hp = state.healths.getOrNull(state.playerIndex) ?: return
            val player = state.players.getOrNull(state.playerIndex) ?: return

            // Max HP bonus
            val hpBonus = progression.maxHpLevel * 20f
            hp.maxHp += hpBonus
            hp.currentHp += hpBonus

            // Move speed
            player.moveSpeed += progression.moveSpeedLevel * 10f

            // Damage multiplier
            player.damageMult += progression.damageLevel * 0.05f

            // Pickup range
            player.pickupRange += progression.pickupRangeLevel * 10f

            // XP gain
            // Stored in player for PickupSystem to use (future)
        }

        /**
         * Load progression from disk.
         */
        fun load(path: String): MetaProgression {
            return try {
                val file = File(path)
                if (file.exists()) {
                    Json.decodeFromString(file.readText())
                } else {
                    MetaProgression()
                }
            } catch (e: Exception) {
                MetaProgression()
            }
        }
    }

    /**
     * Save progression to disk.
     */
    fun save(path: String) {
        try {
            File(path).writeText(Json.encodeToString(MetaProgression.serializer(), this))
        } catch (e: Exception) {
            // Ignore save errors
        }
    }

    /**
     * Add gold from a match result.
     */
    fun addGold(amount: Int) {
        gold = (gold + amount).coerceAtMost(99999)
    }

    // === Upgrade purchases ===

    fun buyMaxHp(): Boolean {
        if (maxHpLevel >= MAX_HP_MAX) return false
        val cost = upgradeCost(500, maxHpLevel)
        if (gold < cost) return false
        gold -= cost
        maxHpLevel++
        return true
    }

    fun buyMoveSpeed(): Boolean {
        if (moveSpeedLevel >= SPEED_MAX) return false
        val cost = upgradeCost(500, moveSpeedLevel)
        if (gold < cost) return false
        gold -= cost
        moveSpeedLevel++
        return true
    }

    fun buyDamage(): Boolean {
        if (damageLevel >= DAMAGE_MAX) return false
        val cost = upgradeCost(800, damageLevel)
        if (gold < cost) return false
        gold -= cost
        damageLevel++
        return true
    }

    fun buyPickupRange(): Boolean {
        if (pickupRangeLevel >= PICKUP_MAX) return false
        val cost = upgradeCost(400, pickupRangeLevel)
        if (gold < cost) return false
        gold -= cost
        pickupRangeLevel++
        return true
    }

    fun buyExtraLife(): Boolean {
        if (extraLifeLevel >= LIFE_MAX) return false
        val cost = upgradeCost(5000, extraLifeLevel)
        if (gold < cost) return false
        gold -= cost
        extraLifeLevel++
        return true
    }

    fun buyXpGain(): Boolean {
        if (xpGainLevel >= XP_MAX) return false
        val cost = upgradeCost(600, xpGainLevel)
        if (gold < cost) return false
        gold -= cost
        xpGainLevel++
        return true
    }

    fun buyGoldFind(): Boolean {
        if (goldFindLevel >= GOLD_MAX) return false
        val cost = upgradeCost(1000, goldFindLevel)
        if (gold < cost) return false
        gold -= cost
        goldFindLevel++
        return true
    }

    fun buyTowerDiscount(): Boolean {
        if (towerDiscountLevel >= TOWER_MAX) return false
        val cost = upgradeCost(2000, towerDiscountLevel)
        if (gold < cost) return false
        gold -= cost
        towerDiscountLevel++
        return true
    }

    fun buyStartingWeapon(): Boolean {
        if (startingWeaponLevel >= WEAPON_MAX) return false
        val cost = upgradeCost(3000, startingWeaponLevel)
        if (gold < cost) return false
        gold -= cost
        startingWeaponLevel++
        return true
    }

    /**
     * Unlock a new chapter.
     */
    fun unlockChapter(chapterId: String, cost: Int): Boolean {
        if (chapterId in chaptersUnlocked) return false
        if (gold < cost) return false
        gold -= cost
        chaptersUnlocked = chaptersUnlocked + chapterId
        return true
    }

    /**
     * Shop display item — maps an upgrade to its UI representation.
     */
    enum class UpgradeItem(
        val displayName: String,
        val description: String,
        val baseCost: Int,
        val maxLevel: Int,
        val levelGetter: (MetaProgression) -> Int
    ) {
        MAX_HP(
            "Vitality",
            "+20 Max HP per level",
            500, MAX_HP_MAX, { it.maxHpLevel }
        ),
        MOVE_SPEED(
            "Agility",
            "+10 px/s per level",
            500, SPEED_MAX, { it.moveSpeedLevel }
        ),
        DAMAGE(
            "Power",
            "+5% damage per level",
            800, DAMAGE_MAX, { it.damageLevel }
        ),
        PICKUP_RANGE(
            "Magnetism",
            "+10px pickup range per level",
            400, PICKUP_MAX, { it.pickupRangeLevel }
        ),
        EXTRA_LIFE(
            "Revival",
            "+1 revive per level",
            5000, LIFE_MAX, { it.extraLifeLevel }
        ),
        XP_GAIN(
            "Wisdom",
            "+5% XP per level",
            600, XP_MAX, { it.xpGainLevel }
        ),
        GOLD_FIND(
            "Fortune",
            "+10% gold per level",
            1000, GOLD_MAX, { it.goldFindLevel }
        ),
        TOWER_DISCOUNT(
            "Bargain",
            "-10% tower cost per level",
            2000, TOWER_MAX, { it.towerDiscountLevel }
        ),
        STARTING_WEAPON(
            "Head Start",
            "Start with weapon at Lv.2",
            3000, WEAPON_MAX, { it.startingWeaponLevel }
        );

        fun currentLevel(meta: MetaProgression): Int = levelGetter(meta)

        fun isMaxed(meta: MetaProgression): Boolean = currentLevel(meta) >= maxLevel

        fun cost(meta: MetaProgression): Int = upgradeCost(baseCost, currentLevel(meta))
    }

    /**
     * Generic buy method for any UpgradeItem.
     */
    fun buy(item: UpgradeItem): Boolean {
        return when (item) {
            UpgradeItem.MAX_HP -> buyMaxHp()
            UpgradeItem.MOVE_SPEED -> buyMoveSpeed()
            UpgradeItem.DAMAGE -> buyDamage()
            UpgradeItem.PICKUP_RANGE -> buyPickupRange()
            UpgradeItem.EXTRA_LIFE -> buyExtraLife()
            UpgradeItem.XP_GAIN -> buyXpGain()
            UpgradeItem.GOLD_FIND -> buyGoldFind()
            UpgradeItem.TOWER_DISCOUNT -> buyTowerDiscount()
            UpgradeItem.STARTING_WEAPON -> buyStartingWeapon()
        }
    }
}
