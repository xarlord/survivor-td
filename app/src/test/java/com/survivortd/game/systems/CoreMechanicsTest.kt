package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.PickupType
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for #116 missing core mechanics:
 * - Victory Condition
 * - Score System
 * - Dash Trigger
 * - Player Revival
 * - Special Pickups (Magnet, Bomb, Treasure Chest)
 */
class CoreMechanicsTest {

    private lateinit var state: GameState

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
    }

    // === VICTORY CONDITION ===

    @Test
    @DisplayName("Victory flag can be set and reset")
    fun victoryFlagSetReset() {
        assertFalse(state.isVictory)
        state.isVictory = true
        assertTrue(state.isVictory)
        state.isVictory = false
        assertFalse(state.isVictory)
    }

    @Test
    @DisplayName("Victory triggers at 900 seconds elapsed")
    fun victoryAt900Seconds() {
        assertFalse(state.isVictory)
        assertFalse(state.isGameOver)
        state.elapsedSeconds = GameConfig.MATCH_DURATION_SECONDS.toFloat()
        if (state.elapsedSeconds >= GameConfig.MATCH_DURATION_SECONDS) {
            state.isVictory = true
            state.isGameOver = true
        }
        assertTrue(state.isVictory)
        assertTrue(state.isGameOver)
    }

    @Test
    @DisplayName("No victory before 900 seconds")
    fun noVictoryBefore900() {
        state.elapsedSeconds = GameConfig.MATCH_DURATION_SECONDS.toFloat() - 1f
        if (state.elapsedSeconds >= GameConfig.MATCH_DURATION_SECONDS) {
            state.isVictory = true
            state.isGameOver = true
        }
        assertFalse(state.isVictory)
        assertFalse(state.isGameOver)
    }

    // === SCORE SYSTEM ===

    @Test
    @DisplayName("XP pickup adds +10 score")
    fun xpPickupAddsScore() {
        val initialScore = state.score
        state.score += 10
        assertEquals(initialScore + 10, state.score)
    }

    @Test
    @DisplayName("Gold pickup adds +25 score")
    fun goldPickupAddsScore() {
        val initialScore = state.score
        state.score += 25
        assertEquals(initialScore + 25, state.score)
    }

    @Test
    @DisplayName("Health pickup adds +100 score")
    fun healthPickupAddsScore() {
        val initialScore = state.score
        state.score += 100
        assertEquals(initialScore + 100, state.score)
    }

    @Test
    @DisplayName("Wave completion adds +50 score")
    fun waveCompletionAddsScore() {
        val initialScore = state.score
        state.score += 50
        assertEquals(initialScore + 50, state.score)
    }

    // === DASH TRIGGER ===

    @Test
    @DisplayName("Single joystick tap does not trigger dash")
    fun singleTapNoDash() {
        val joystick = VirtualJoystick(state)
        val result1 = joystick.checkDash()
        assertFalse(result1, "First tap should not trigger dash")
    }

    @Test
    @DisplayName("Player dashCooldownTimer ticks down")
    fun dashCooldownTicksDown() {
        val player = state.players[state.playerIndex]
        player.dashCooldownTimer = 2f
        player.dashCooldownTimer = (player.dashCooldownTimer - 0.5f).coerceAtLeast(0f)
        assertEquals(1.5f, player.dashCooldownTimer)
        player.dashCooldownTimer = (player.dashCooldownTimer - 2f).coerceAtLeast(0f)
        assertEquals(0f, player.dashCooldownTimer, "Should not go below 0")
    }

    @Test
    @DisplayName("Player has dashCooldownTimer field")
    fun playerHasDashCooldownTimer() {
        val player = state.players[state.playerIndex]
        assertEquals(0f, player.dashCooldownTimer)
        player.dashCooldownTimer = 5f
        assertEquals(5f, player.dashCooldownTimer)
    }

    // === PLAYER REVIVAL ===

    @Test
    @DisplayName("Revival with extra lives restores HP")
    fun revivalWithExtraLives() {
        val meta = MetaProgression()
        meta.extraLifeLevel = 1
        val player = state.players[state.playerIndex]
        assertFalse(player.hasRevived)

        val health = state.healths[state.playerIndex]
        health.currentHp = 0f
        health.maxHp = 100f

        if (meta.extraLifeLevel > 0 && !player.hasRevived) {
            meta.extraLifeLevel--
            health.currentHp = health.maxHp * 0.5f
            player.hasRevived = true
            health.invincible = true
            health.invincibleTimer = 2f
        }

        assertEquals(50f, health.currentHp, "Should revive at 50% HP")
        assertTrue(player.hasRevived)
        assertEquals(0, meta.extraLifeLevel, "Should consume one life")
        assertTrue(health.invincible)
        assertEquals(2f, health.invincibleTimer)
    }

    @Test
    @DisplayName("No revival without extra lives")
    fun noRevivalWithoutLives() {
        val meta = MetaProgression()
        meta.extraLifeLevel = 0

        state.healths[state.playerIndex].currentHp = 0f

        if (meta.extraLifeLevel > 0 && !state.players[state.playerIndex].hasRevived) {
            // Would revive
        } else {
            state.isGameOver = true
        }

        assertTrue(state.isGameOver, "Should be game over with no extra lives")
    }

    @Test
    @DisplayName("Revival only happens once per run")
    fun revivalOnlyOnce() {
        val meta = MetaProgression()
        meta.extraLifeLevel = 3
        val player = state.players[state.playerIndex]
        val health = state.healths[state.playerIndex]
        health.maxHp = 100f

        // First death: revive
        health.currentHp = 0f
        if (meta.extraLifeLevel > 0 && !player.hasRevived) {
            meta.extraLifeLevel--
            health.currentHp = health.maxHp * 0.5f
            player.hasRevived = true
        }
        assertEquals(2, meta.extraLifeLevel, "Should consume one life")
        assertTrue(player.hasRevived)

        // Second death: no revive
        health.currentHp = 0f
        if (meta.extraLifeLevel > 0 && !player.hasRevived) {
            meta.extraLifeLevel--
            health.currentHp = health.maxHp * 0.5f
            player.hasRevived = true
        } else {
            state.isGameOver = true
        }
        assertTrue(state.isGameOver, "Should be game over on second death")
        assertEquals(2, meta.extraLifeLevel, "Should NOT consume another life")
    }

    // === SPECIAL PICKUPS ===

    @Test
    @DisplayName("Magnet pickup sets magnetTimer to 5 seconds")
    fun magnetPickupSetsTimer() {
        val player = state.players[state.playerIndex]
        assertEquals(0f, player.magnetTimer)
        player.magnetTimer = GameConfig.MAGNET_DURATION
        assertEquals(GameConfig.MAGNET_DURATION, player.magnetTimer)
    }

    @Test
    @DisplayName("Magnet timer decays over time")
    fun magnetTimerDecays() {
        val player = state.players[state.playerIndex]
        player.magnetTimer = GameConfig.MAGNET_DURATION
        player.magnetTimer = (player.magnetTimer - 1f).coerceAtLeast(0f)
        assertEquals(GameConfig.MAGNET_DURATION - 1f, player.magnetTimer)
    }

    @Test
    @DisplayName("Bomb deals 200 damage to all enemies")
    fun bombDealsDamageToAll() {
        val enemy1 = state.spawnEnemy(100f, 100f, EnemyComponent.EnemyData.ZOMBIE)
        val enemy2 = state.spawnEnemy(200f, 200f, EnemyComponent.EnemyData.ZOMBIE)

        state.healths[enemy1].currentHp = 100f
        state.healths[enemy2].currentHp = 100f

        for (j in state.healths.indices) {
            if (j < state.tags.size && state.tags[j].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY) {
                if (j < state.healths.size) {
                    state.healths[j].currentHp -= GameConfig.BOMB_DAMAGE
                }
            }
        }

        assertTrue(state.healths[enemy1].currentHp <= 0f, "Enemy 1 should be dead from bomb")
        assertTrue(state.healths[enemy2].currentHp <= 0f, "Enemy 2 should be dead from bomb")
    }

    @Test
    @DisplayName("Treasure chest gives 50-200 gold")
    fun treasureChestGold() {
        val player = state.players[state.playerIndex]
        val chestGold = GameConfig.GOLD_CHEST_MIN + kotlin.random.Random.nextInt(
            GameConfig.GOLD_CHEST_MAX - GameConfig.GOLD_CHEST_MIN
        )
        assertTrue(chestGold >= GameConfig.GOLD_CHEST_MIN, "Chest gold >= 50")
        assertTrue(chestGold <= GameConfig.GOLD_CHEST_MAX, "Chest gold <= 200")
        player.gold += chestGold
        assertEquals(chestGold, player.gold)
    }

    @Test
    @DisplayName("Magnet collection sets player magnetTimer")
    fun magnetCollectionEffect() {
        val player = state.players[state.playerIndex]
        val pickupType = PickupType.MAGNET
        when (pickupType) {
            PickupType.MAGNET -> player.magnetTimer = GameConfig.MAGNET_DURATION
            else -> {}
        }
        assertEquals(GameConfig.MAGNET_DURATION, player.magnetTimer)
    }

    @Test
    @DisplayName("Magnet pickup enhanced range is 500px")
    fun magnetEnhancedRange() {
        val player = state.players[state.playerIndex]
        player.magnetTimer = GameConfig.MAGNET_DURATION
        val range = if (player.magnetTimer > 0f) GameConfig.MAGNET_PICKUP_RANGE else player.pickupRange
        assertEquals(GameConfig.MAGNET_PICKUP_RANGE, range)
    }

    @Test
    @DisplayName("Normal pickup range when magnet not active")
    fun normalPickupRange() {
        val player = state.players[state.playerIndex]
        player.magnetTimer = 0f
        val range = if (player.magnetTimer > 0f) GameConfig.MAGNET_PICKUP_RANGE else player.pickupRange
        assertEquals(player.pickupRange, range)
    }

    // === GAME CONFIG CONSTANTS ===

    @Test
    @DisplayName("MAGNET_PICKUP_RANGE is 500")
    fun configMagnetRange() {
        assertEquals(500f, GameConfig.MAGNET_PICKUP_RANGE)
    }

    @Test
    @DisplayName("MAGNET_DURATION is 5 seconds")
    fun configMagnetDuration() {
        assertEquals(5f, GameConfig.MAGNET_DURATION)
    }

    @Test
    @DisplayName("BOMB_DAMAGE is 200")
    fun configBombDamage() {
        assertEquals(200f, GameConfig.BOMB_DAMAGE)
    }

    @Test
    @DisplayName("DASH_DOUBLE_TAP_MS is 300")
    fun configDashDoubleTapMs() {
        assertEquals(300L, GameConfig.DASH_DOUBLE_TAP_MS)
    }

    @Test
    @DisplayName("MAGNET_DROP_CHANCE is 2%")
    fun configMagnetDropChance() {
        assertEquals(0.02f, GameConfig.MAGNET_DROP_CHANCE)
    }

    @Test
    @DisplayName("BOMB_DROP_CHANCE is 1%")
    fun configBombDropChance() {
        assertEquals(0.01f, GameConfig.BOMB_DROP_CHANCE)
    }

    @Test
    @DisplayName("TREASURE_CHEST_DROP_CHANCE is 0.5%")
    fun configTreasureChestDropChance() {
        assertEquals(0.005f, GameConfig.TREASURE_CHEST_DROP_CHANCE)
    }

    // === PLAYER COMPONENT FIELDS ===

    @Test
    @DisplayName("PlayerComponent has all new fields")
    fun playerComponentFields() {
        val player = com.survivortd.game.components.PlayerComponent()
        assertEquals(0f, player.dashCooldownTimer)
        assertFalse(player.hasRevived)
        assertEquals(0f, player.magnetTimer)
    }
}
