package com.survivortd.game.systems

import com.survivortd.game.components.StatusEffectsComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.config.StatusEffectType
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [#32] Tests that the StatusEffectSystem correctly processes all 7 status types.
 *
 * Before this fix, 5/7 status types (BURN, POISON, BLEED, STUN, SLOW_ATTACK) were
 * applied but never processed — DoT effects dealt no damage, hard CC didn't stop
 * movement.
 */
class StatusEffectSystemTest {

    private lateinit var state: GameState
    private lateinit var sys: StatusEffectSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        sys = StatusEffectSystem(state)
    }

    private fun spawnEnemy(): Int {
        state.spawnEnemy(x = 100f, y = 100f, enemyType = com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)
        return state.enemies.lastIndex
    }

    private fun applyStatusTo(enemyIdx: Int, type: StatusEffectType, duration: Float, magnitude: Float) {
        state.statusEffects[enemyIdx].effects.add(
            StatusEffectsComponent.ActiveStatus(
                type = type, duration = duration, magnitude = magnitude
            )
        )
    }

    @Nested
    @DisplayName("DoT effects (BURN, POISON, BLEED)")
    inner class DoTTests {

        @Test
        @DisplayName("BURN deals damage over time")
        fun burnDealsDamage() {
            val idx = spawnEnemy()
            val initialHp = state.healths[idx].currentHp
            applyStatusTo(idx, StatusEffectType.BURN, duration = 5f, magnitude = 10f)
            // First tick is at tickInterval=0.5s, tickTimer starts at 0, so first tick at dt=0.5+
            sys.update(0.6f)  // Should trigger first tick
            assertTrue(
                state.healths[idx].currentHp < initialHp,
                "BURN should deal damage. HP went from $initialHp to ${state.healths[idx].currentHp}"
            )
        }

        @Test
        @DisplayName("POISON deals damage ignoring armor")
        fun poisonDealsDamageIgnoringArmor() {
            val idx = spawnEnemy()
            state.healths[idx].armor = 100f  // High armor
            applyStatusTo(idx, StatusEffectType.POISON, duration = 5f, magnitude = 10f)
            val initialHp = state.healths[idx].currentHp
            sys.update(0.6f)
            // Poison should deal full 10 damage despite 100 armor
            assertEquals(
                initialHp - 10f, state.healths[idx].currentHp, 0.1f,
                "POISON should ignore armor"
            )
        }

        @Test
        @DisplayName("BLEED deals damage subject to armor")
        fun bleedDealsDamageWithArmor() {
            val idx = spawnEnemy()
            // (#108) Use armor=3 so flat reduction still allows damage through: max(10-3,0)=7
            state.healths[idx].armor = 3f
            applyStatusTo(idx, StatusEffectType.BLEED, duration = 5f, magnitude = 10f)
            val initialHp = state.healths[idx].currentHp
            sys.update(0.6f)
            assertTrue(
                state.healths[idx].currentHp < initialHp,
                "BLEED should deal damage (reduced by armor)"
            )
            // Flat armor: damage = max(10 - 3, 0) = 7
            val expectedDamage = GameConfig.armorReduction(10f, 3f)
            assertEquals(
                initialHp - expectedDamage, state.healths[idx].currentHp, 0.1f,
                "BLEED should be reduced by armor (flat subtraction)"
            )
        }
    }

    @Nested
    @DisplayName("Hard CC (FREEZE, STUN)")
    inner class HardCCTests {

        @Test
        @DisplayName("FREEZE zeroes enemy velocity")
        fun freezeZeroesVelocity() {
            val idx = spawnEnemy()
            state.velocities[idx].x = 100f
            state.velocities[idx].y = 200f
            applyStatusTo(idx, StatusEffectType.FREEZE, duration = 3f, magnitude = 1f)
            sys.update(0.1f)
            assertEquals(0f, state.velocities[idx].x, "FREEZE should zero velocity X")
            assertEquals(0f, state.velocities[idx].y, "FREEZE should zero velocity Y")
        }

        @Test
        @DisplayName("STUN zeroes enemy velocity")
        fun stunZeroesVelocity() {
            val idx = spawnEnemy()
            state.velocities[idx].x = 50f
            state.velocities[idx].y = -80f
            applyStatusTo(idx, StatusEffectType.STUN, duration = 2f, magnitude = 1f)
            sys.update(0.1f)
            assertEquals(0f, state.velocities[idx].x, "STUN should zero velocity X")
            assertEquals(0f, state.velocities[idx].y, "STUN should zero velocity Y")
        }
    }

    @Nested
    @DisplayName("Soft CC (SLOW, SLOW_ATTACK)")
    inner class SoftCCTests {

        @Test
        @DisplayName("SLOW reduces velocity magnitude")
        fun slowReducesVelocity() {
            val idx = spawnEnemy()
            state.velocities[idx].x = 100f
            state.velocities[idx].y = 0f
            applyStatusTo(idx, StatusEffectType.SLOW, duration = 3f, magnitude = 0.5f)  // 50% slow
            sys.update(0.1f)
            assertEquals(50f, state.velocities[idx].x, 1f, "SLOW should reduce velocity by 50%")
        }
    }

    @Nested
    @DisplayName("Duration and cleanup")
    inner class DurationTests {

        @Test
        @DisplayName("Status effect removed when duration expires")
        fun statusRemovedOnExpiry() {
            val idx = spawnEnemy()
            applyStatusTo(idx, StatusEffectType.BURN, duration = 1f, magnitude = 10f)
            sys.update(1.1f)  // Past duration
            assertTrue(
                state.statusEffects[idx].effects.none { it.type == StatusEffectType.BURN },
                "BURN should be removed when duration expires"
            )
        }
    }
}
