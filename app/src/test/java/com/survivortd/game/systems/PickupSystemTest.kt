package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for PickupSystem — XP gem spawning, magnetism, collection.
 */
class PickupSystemTest {

    private lateinit var state: GameState
    private lateinit var pickupSystem: PickupSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        pickupSystem = PickupSystem(state)
    }

    @Test
    @DisplayName("Dead enemy spawns XP gem")
    fun deadEnemySpawnsGem() {
        val enemyId = state.spawnEnemy(
            x = 100f, y = 100f,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        // Kill the enemy
        state.healths[enemyId].currentHp = 0f

        val pickupsBefore = state.pickups.count { it.xpValue > 0 }
        pickupSystem.update(0.016f)
        val pickupsAfter = state.pickups.count { it.xpValue > 0 }
        assertTrue(pickupsAfter > pickupsBefore,
            "A gem should spawn when enemy dies")
    }

    @Test
    @DisplayName("Boss drops large XP gem")
    fun bossDropsLargeGem() {
        val enemyId = state.spawnEnemy(
            x = 100f, y = 100f,
            enemyType = EnemyComponent.EnemyData.BOSS
        )
        state.healths[enemyId].currentHp = 0f
        pickupSystem.update(0.016f)

        val gem = state.pickups.lastOrNull { it.xpValue > 0 }
        assertNotNull(gem, "Boss should drop a gem")
        assertEquals(GameConfig.GEM_BOSS_XP, gem!!.xpValue,
            "Boss gem should have 100 XP")
    }

    @Test
    @DisplayName("XP gem magnetizes when player is in pickup range")
    fun gemMagnetizes() {
        // Spawn a gem near the player
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        state.spawnPickup(
            x = playerPos.x + 50f,  // Within pickup range (60f default)
            y = playerPos.y,
            xpValue = 5
        )

        pickupSystem.update(0.016f)
        val gemIndex = state.pickups.indexOfLast { it.xpValue > 0 }
        assertTrue(state.pickups[gemIndex].isMagnetized,
            "Gem should be magnetized when within pickup range")
    }

    @Test
    @DisplayName("Player collects XP gem and gains XP")
    fun playerCollectsXpGem() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        // Place gem right on the player, small XP so it doesn't trigger level-up
        state.spawnPickup(
            x = playerPos.x,
            y = playerPos.y,
            xpValue = 3
        )
        val xpBefore = state.players[pid].currentXp
        pickupSystem.update(0.016f)
        assertTrue(state.players[pid].currentXp > xpBefore,
            "Player XP should increase after collecting gem")
    }

    @Test
    @DisplayName("Collecting enough XP triggers level up")
    fun xpTriggersLevelUp() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        // Set current XP close to threshold
        state.players[pid].currentXp = 6  // Need 8 for level 1->2
        state.spawnPickup(
            x = playerPos.x,
            y = playerPos.y,
            xpValue = 5  // 6+5=11 > 8 → level up
        )
        pickupSystem.update(0.016f)
        assertEquals(2, state.players[pid].level,
            "Player should be level 2 after collecting enough XP")
        assertTrue(state.pendingLevelUps > 0,
            "Pending level ups should be queued")
    }

    @Test
    @DisplayName("Gem expires after lifetime")
    fun gemExpires() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        // Place gem far away from player so it doesn't get collected
        state.spawnPickup(
            x = playerPos.x + 1000f,
            y = playerPos.y + 1000f,
            xpValue = 5,
            radius = 5f
        )
        // Simulate lifetime expiring
        val gemIdx = state.pickups.lastIndex
        state.pickups[gemIdx].lifetime = 0.01f
        pickupSystem.update(0.1f)
        assertEquals(0, state.pickups[gemIdx].xpValue,
            "Gem should be cleared after lifetime expires")
    }
}
