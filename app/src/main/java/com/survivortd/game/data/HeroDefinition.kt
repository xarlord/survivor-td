package com.survivortd.game.data

import com.survivortd.game.config.WeaponType

/**
 * Hero identifiers with display metadata and per-hero configuration.
 * GDD §3.3 — 6 heroes with unique passive abilities and starting weapons.
 */
enum class HeroId(
    val displayName: String,
    val description: String,
    val startingWeapon: WeaponType
) {
    COMMANDER(
        "Commander",
        "Military veteran. +10% tower damage & range.",
        WeaponType.ASSAULT_RIFLE
    ),
    BERSERKER(
        "Berserker",
        "Fierce warrior. +15% damage when HP below 30%.",
        WeaponType.KATANA
    ),
    ENGINEER(
        "Engineer",
        "Tech specialist. +1 tower slot, towers 20% cheaper.",
        WeaponType.DRONE
    ),
    MEDIC(
        "Medic",
        "Field medic. +2 HP/s regen, healing items 50% stronger.",
        WeaponType.HEALING_PULSE
    ),
    SCOUT(
        "Scout",
        "Speed specialist. +25% move speed, +30% pickup range.",
        WeaponType.BOOMERANG
    ),
    SHIELDER(
        "Shielder",
        "Defender. Takes 50% less damage for 2s every 10s.",
        WeaponType.FORCE_FIELD
    );

    companion object {
        /** Default hero (always unlocked). */
        val DEFAULT = COMMANDER
    }
}

/**
 * Unlock requirements for a hero.
 * @param heroId The hero to unlock.
 * @param unlockCost Gold cost (0 = free).
 * @param unlockCondition Human-readable condition (e.g. "Complete Ch.1"). Blank = gold-only.
 */
data class HeroUnlock(
    val heroId: HeroId,
    val unlockCost: Int,
    val unlockCondition: String = ""
) {
    val isFree: Boolean get() = unlockCost == 0 && unlockCondition.isBlank()

    companion object {
        /** All hero unlock definitions in GDD §3.3 order. */
        val ALL = listOf(
            HeroUnlock(HeroId.COMMANDER, 0),
            HeroUnlock(HeroId.BERSERKER, 5_000),
            HeroUnlock(HeroId.ENGINEER, 10_000),
            HeroUnlock(HeroId.MEDIC, 15_000),
            HeroUnlock(HeroId.SCOUT, 20_000),
            HeroUnlock(HeroId.SHIELDER, 0, "Complete Ch.1")
        )

        fun forHero(heroId: HeroId): HeroUnlock =
            ALL.first { it.heroId == heroId }
    }
}
