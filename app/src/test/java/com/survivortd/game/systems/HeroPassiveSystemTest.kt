package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import com.survivortd.game.data.HeroId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for HeroPassiveSystem — per-hero passive effects applied each tick.
 * GDD §3.3
 */
class HeroPassiveSystemTest {

    private lateinit var state: GameState
    private lateinit var heroPassive: HeroPassiveSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        heroPassive = HeroPassiveSystem()
    }

    // === Commander ===

    @Test
    @DisplayName("Commander sets commanderBonus flag")
    fun commanderSetsBonusFlag() {
        heroPassive.initHero(HeroId.COMMANDER, state)
        val player = state.players[state.playerIndex]
        assertTrue(player.commanderBonus, "Commander should set commanderBonus flag")
    }

    // === Berserker ===

    @Test
    @DisplayName("Berserker activates +15% damage when HP below 30%")
    fun berserkerLowHpDamageBoost() {
        heroPassive.initHero(HeroId.BERSERKER, state)
        val pid = state.playerIndex
        val player = state.players[pid]
        val hp = state.healths[pid]

        // Set HP below 30%
        hp.currentHp = hp.maxHp * 0.2f
        heroPassive.applyPassive(HeroId.BERSERKER, state, 0.016f)

        assertTrue(player.berserkerActive, "Berserker should be active at low HP")
        assertEquals(player.baseDamageMultiplier * 1.15f, player.damageMult, 0.001f,
            "Damage should be 115% at low HP")
    }

    @Test
    @DisplayName("Berserker has no bonus at full HP")
    fun berserkerFullHpNoBonus() {
        heroPassive.initHero(HeroId.BERSERKER, state)
        val pid = state.playerIndex
        val player = state.players[pid]

        // HP is full (100/100) — well above 30%
        heroPassive.applyPassive(HeroId.BERSERKER, state, 0.016f)

        assertFalse(player.berserkerActive, "Berserker should not be active at full HP")
        assertEquals(player.baseDamageMultiplier, player.damageMult, 0.001f)
    }

    // === Engineer ===

    @Test
    @DisplayName("Engineer gets +1 tower slot (9 total)")
    fun engineerExtraTowerSlot() {
        heroPassive.initHero(HeroId.ENGINEER, state)
        val player = state.players[state.playerIndex]
        assertEquals(GameConfig.MAX_TOWERS + 1, player.maxTowers,
            "Engineer should have 9 tower slots")
    }

    @Test
    @DisplayName("Engineer gets 20% tower cost reduction")
    fun engineerTowerCostReduction() {
        heroPassive.initHero(HeroId.ENGINEER, state)
        val player = state.players[state.playerIndex]
        assertEquals(0.8f, player.towerCostMultiplier, 0.001f,
            "Engineer tower cost should be 80% of normal")
    }

    // === Medic ===

    @Test
    @DisplayName("Medic gets +2 HP/s regen")
    fun medicRegenBonus() {
        heroPassive.initHero(HeroId.MEDIC, state)
        val player = state.players[state.playerIndex]
        assertEquals(GameConfig.PLAYER_BASE_REGEN + 2f, player.regen, 0.001f,
            "Medic should have base regen + 2 HP/s")
    }

    @Test
    @DisplayName("Medic gets 50% healing bonus")
    fun medicHealingBonus() {
        heroPassive.initHero(HeroId.MEDIC, state)
        val player = state.players[state.playerIndex]
        assertEquals(1.5f, player.healingBonus, 0.001f,
            "Medic healing bonus should be 1.5x")
    }

    // === Scout ===

    @Test
    @DisplayName("Scout gets +25% move speed")
    fun scoutMoveSpeed() {
        heroPassive.initHero(HeroId.SCOUT, state)
        val player = state.players[state.playerIndex]
        assertEquals(player.baseMoveSpeed * 1.25f, player.moveSpeed, 0.001f,
            "Scout should have 125% base move speed")
    }

    @Test
    @DisplayName("Scout gets +30% pickup range")
    fun scoutPickupRange() {
        heroPassive.initHero(HeroId.SCOUT, state)
        val player = state.players[state.playerIndex]
        assertEquals(player.basePickupRange * 1.3f, player.pickupRange, 0.001f,
            "Scout should have 130% base pickup range")
    }

    // === Shielder ===

    @Test
    @DisplayName("Shielder activates damage reduction after cooldown expires")
    fun shielderDamageReduction() {
        heroPassive.initHero(HeroId.SHIELDER, state)
        val player = state.players[state.playerIndex]

        // Shielder starts with shieldCooldownTimer = 10f
        // Advance time just past the cooldown (10s) with a small dt
        heroPassive.applyPassive(HeroId.SHIELDER, state, 10.1f)

        // Shield was triggered: cooldown reset to 10f, damageReduction set to 2f
        // Then dt subtracted: 2f - 10.1f = negative. armorReduction was set to 0.5f.
        assertEquals(0.5f, player.armorReduction, 0.001f,
            "Armor reduction should be 50% while shield is active")
    }

    @Test
    @DisplayName("Shielder damage reduction expires after 2 seconds")
    fun shielderDamageReductionExpires() {
        heroPassive.initHero(HeroId.SHIELDER, state)
        val player = state.players[state.playerIndex]

        // Trigger shield, then let it expire
        heroPassive.applyPassive(HeroId.SHIELDER, state, 10.5f)
        assertTrue(player.armorReduction > 0f, "Shield should be active")

        // Advance past the 2s reduction duration
        heroPassive.applyPassive(HeroId.SHIELDER, state, 3f)
        assertEquals(0f, player.armorReduction, 0.001f,
            "Armor reduction should be 0 when shield expires")
    }

    // === Base value snapshots ===

    @Test
    @DisplayName("initHero snapshots base values before modifications")
    fun baseValueSnapshot() {
        // Set custom values before initHero
        val player = state.players[state.playerIndex]
        player.moveSpeed = 300f
        player.pickupRange = 80f
        player.damageMult = 1.5f

        heroPassive.initHero(HeroId.COMMANDER, state)

        assertEquals(1.5f, player.baseDamageMultiplier, 0.001f,
            "baseDamageMultiplier should snapshot pre-hero value")
        assertEquals(300f, player.baseMoveSpeed, 0.001f,
            "baseMoveSpeed should snapshot pre-hero value")
        assertEquals(80f, player.basePickupRange, 0.001f,
            "basePickupRange should snapshot pre-hero value")
    }

    @Test
    @DisplayName("GameState stores hero ID when set externally")
    fun gameStateStoresHeroId() {
        state.heroId = HeroId.BERSERKER.name
        assertEquals("BERSERKER", state.heroId)
    }
}
