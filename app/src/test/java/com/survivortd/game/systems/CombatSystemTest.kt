package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for CombatSystem — enemy contact damage, regen, invincibility, death.
 */
class CombatSystemTest {

    private lateinit var state: GameState
    private lateinit var combat: CombatSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        combat = CombatSystem(state)
    }

    @Test
    @DisplayName("Player takes contact damage from enemy on touch")
    fun playerTakesContactDamage() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        val enemyId = state.spawnEnemy(
            x = playerPos.x + 10f,  // Very close — within contact range
            y = playerPos.y,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        val hpBefore = state.healths[pid].currentHp
        combat.update(0.016f)
        val hpAfter = state.healths[pid].currentHp
        assertTrue(hpAfter < hpBefore, "Player should take contact damage from zombie")
        assertTrue(hpBefore - hpAfter >= 5f, "Damage should be at least 5 HP (zombie base damage)")
    }

    @Test
    @DisplayName("Player is invincible after taking damage")
    fun invincibilityAfterHit() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        state.spawnEnemy(
            x = playerPos.x + 10f,
            y = playerPos.y,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        // First hit applies damage
        combat.update(0.016f)
        val hpAfterFirstHit = state.healths[pid].currentHp

        // Second tick should not damage (invincible)
        combat.update(0.016f)
        assertEquals(hpAfterFirstHit, state.healths[pid].currentHp, 0.1f,
            "Player should be invincible after taking damage")
    }

    @Test
    @DisplayName("Invincibility wears off after 0.5 seconds")
    fun invincibilityWearsOff() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        state.spawnEnemy(
            x = playerPos.x + 10f,
            y = playerPos.y,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        combat.update(0.016f)  // Hit
        val hpAfterFirstHit = state.healths[pid].currentHp
        assertTrue(state.healths[pid].invincible)

        // Wait for invincibility to expire
        combat.update(0.6f)
        assertFalse(state.healths[pid].invincible,
            "Invincibility should wear off after 0.5s")
    }

    @Test
    @DisplayName("Player HP regenerates over time")
    fun hpRegenerates() {
        val pid = state.playerIndex
        // Damage the player manually
        state.healths[pid].currentHp = 50f
        val hpBefore = state.healths[pid].currentHp
        combat.update(1f)  // 1 second = 0.5 regen
        assertTrue(state.healths[pid].currentHp > hpBefore,
            "Player HP should regenerate")
        // Regen is 0.5 HP/s, so after 1s should gain ~0.5 HP
        assertEquals(50.5f, state.healths[pid].currentHp, 0.2f,
            "Player should regenerate ~0.5 HP/sec")
    }

    @Test
    @DisplayName("HP does not exceed max HP during regen")
    fun hpDoesNotExceedMax() {
        val pid = state.playerIndex
        state.healths[pid].currentHp = 99.9f
        combat.update(1f)
        assertEquals(100f, state.healths[pid].currentHp, 0.01f,
            "HP should be clamped to max")
    }

    @Test
    @DisplayName("Player death triggers game over")
    fun deathTriggersGameOver() {
        val pid = state.playerIndex
        // Spawn boss adjacent — contact damage is 40 + scaling
        val playerPos = state.positions[pid]
        state.spawnEnemy(
            x = playerPos.x + 10f,
            y = playerPos.y,
            enemyType = EnemyComponent.EnemyData.BOSS
        )
        // Set HP very low
        state.healths[pid].currentHp = 1f
        combat.update(0.016f)
        assertTrue(state.healths[pid].isDead, "Player should be dead")
        assertTrue(state.isGameOver, "Game over flag should be set")
    }

    @Test
    @DisplayName("No combat when paused")
    fun noCombatWhenPaused() {
        val pid = state.playerIndex
        val playerPos = state.positions[pid]
        state.spawnEnemy(
            x = playerPos.x + 10f,
            y = playerPos.y,
            enemyType = EnemyComponent.EnemyData.ZOMBIE
        )
        val hpBefore = state.healths[pid].currentHp
        state.isPaused = true
        combat.update(0.016f)
        assertEquals(hpBefore, state.healths[pid].currentHp, 0.01f,
            "No damage should occur when paused")
    }
}
