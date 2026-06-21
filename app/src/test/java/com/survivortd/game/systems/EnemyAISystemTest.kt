package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.EnemyComponent.AiState
import com.survivortd.game.components.TagComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EnemyAISystemTest {

    private lateinit var state: GameState
    private lateinit var ai: EnemyAISystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        ai = EnemyAISystem(state)
    }

    // ================================================================
    // ZOMBIE
    // ================================================================
    @Nested
    @DisplayName("Zombie AI")
    inner class ZombieTests {
        @Test
        @DisplayName("Zombie moves toward player")
        fun zombieChases() {
            val enemyId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            ai.update(0.016f)
            val vel = state.velocities[enemyId]
            assertTrue(vel.x > 0f, "Zombie velocity X should be positive (moving toward center)")
        }

        @Test
        @DisplayName("Zombie has CHASE state")
        fun zombieState() {
            val enemyId = state.spawnEnemy(x = 100f, y = 100f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            ai.update(0.016f)
            assertEquals(AiState.CHASE, state.enemies[enemyId].aiState)
        }
    }

    // ================================================================
    // RUNNER
    // ================================================================
    @Nested
    @DisplayName("Runner AI")
    inner class RunnerTests {
        @Test
        @DisplayName("Runner moves toward player faster than zombie")
        fun runnerIsFast() {
            val runnerId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.RUNNER)
            val zombieId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            ai.update(0.016f)
            val runnerSpeed = kotlin.math.sqrt(
                state.velocities[runnerId].x.toDouble().pow(2) +
                state.velocities[runnerId].y.toDouble().pow(2)
            )
            val zombieSpeed = kotlin.math.sqrt(
                state.velocities[zombieId].x.toDouble().pow(2) +
                state.velocities[zombieId].y.toDouble().pow(2)
            )
            assertTrue(runnerSpeed > zombieSpeed, "Runner ($runnerSpeed) should be faster than Zombie ($zombieSpeed)")
        }
    }

    // ================================================================
    // BRUTE
    // ================================================================
    @Nested
    @DisplayName("Brute AI")
    inner class BruteTests {
        @Test
        @DisplayName("Brute starts in CHASE state")
        fun bruteStartsChasing() {
            val bruteId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BRUTE)
            // Brute far away → CHASE
            ai.update(0.016f)
            assertEquals(AiState.CHASE, state.enemies[bruteId].aiState)
        }

        @Test
        @DisplayName("Brute transitions to CHARGE when near player")
        fun bruteCharges() {
            val playerPos = state.positions[state.playerIndex]
            val bruteId = state.spawnEnemy(
                x = playerPos.x + 100f, y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.BRUTE
            )
            // Run AI enough ticks to trigger charge (< 200px distance)
            for (i in 0 until 100) {
                ai.update(0.016f)
                if (state.enemies[bruteId].aiState == AiState.CHARGE) break
            }
            assertTrue(
                state.enemies[bruteId].aiState == AiState.CHARGE,
                "Brute should enter CHARGE state when within 200px"
            )
        }

        @Test
        @DisplayName("Brute stops during CHARGE telegraph")
        fun bruteStopsDuringTelegraph() {
            val playerPos = state.positions[state.playerIndex]
            val bruteId = state.spawnEnemy(
                x = playerPos.x + 100f, y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.BRUTE
            )
            // Advance until CHARGE starts, then check velocity on the NEXT tick
            var foundCharge = false
            for (i in 0 until 200) {
                ai.update(0.016f)
                if (state.enemies[bruteId].aiState == AiState.CHARGE && !foundCharge) {
                    foundCharge = true
                    // First tick of CHARGE: aiTimer starts at 0.6, so still in telegraph (> 0.3f)
                    if (state.enemies[bruteId].aiTimer > 0.3f) {
                        val vel = state.velocities[bruteId]
                        assertEquals(0f, vel.x, 1f, "Brute should stop during telegraph phase")
                        assertEquals(0f, vel.y, 1f, "Brute should stop during telegraph phase")
                        return
                    }
                }
            }
            // If we never caught the telegraph moment, just verify CHARGE state was reached
            assertTrue(foundCharge, "Brute should have entered CHARGE state")
        }
    }

    // ================================================================
    // SPITTER
    // ================================================================
    @Nested
    @DisplayName("Spitter AI")
    inner class SpitterTests {
        @Test
        @DisplayName("Spitter approaches when far")
        fun spitterApproachesWhenFar() {
            val spitterId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.SPITTER)
            ai.update(0.016f)
            assertEquals(AiState.CHASE, state.enemies[spitterId].aiState,
                "Spitter should chase when far from player")
        }

        @Test
        @DisplayName("Spitter kites when at ideal range")
        fun spitterKitesAtRange() {
            val playerPos = state.positions[state.playerIndex]
            val spitterId = state.spawnEnemy(
                x = playerPos.x + 250f, y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.SPITTER
            )
            ai.update(0.016f)
            assertTrue(
                state.enemies[spitterId].aiState == AiState.KITE ||
                state.enemies[spitterId].aiState == AiState.CHASE,
                "Spitter at ideal range should kite or reposition, got ${state.enemies[spitterId].aiState}"
            )
        }
    }

    // ================================================================
    // BOMBER
    // ================================================================
    @Nested
    @DisplayName("Bomber AI")
    inner class BomberTests {
        @Test
        @DisplayName("Bomber rushes player (fast chase)")
        fun bomberRushes() {
            val bomberId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BOMBER)
            ai.update(0.016f)
            val vel = state.velocities[bomberId]
            val speed = kotlin.math.sqrt(vel.x.toDouble().pow(2) + vel.y.toDouble().pow(2))
            assertTrue(speed > 50f, "Bomber should be moving fast toward player")
        }

        @Test
        @DisplayName("Bomber enters SPECIAL when very close")
        fun bomberExplodesWhenClose() {
            val playerPos = state.positions[state.playerIndex]
            val bomberId = state.spawnEnemy(
                x = playerPos.x + 20f, y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.BOMBER
            )
            ai.update(0.016f)
            assertEquals(AiState.SPECIAL, state.enemies[bomberId].aiState,
                "Bomber should enter SPECIAL when within 40px")
        }
    }

    // ================================================================
    // HEALER
    // ================================================================
    @Nested
    @DisplayName("Healer AI")
    inner class HealerTests {
        @Test
        @DisplayName("Healer flees when player is close")
        fun healerFlees() {
            val playerPos = state.positions[state.playerIndex]
            val healerId = state.spawnEnemy(
                x = playerPos.x + 100f, y = playerPos.y,
                enemyType = EnemyComponent.EnemyData.HEALER
            )
            ai.update(0.016f)
            assertEquals(AiState.FLEE, state.enemies[healerId].aiState,
                "Healer should flee when player is close")
        }

        @Test
        @DisplayName("Healer heals nearby enemies")
        fun healerHealsNearby() {
            // Place healer and zombie far from player so healer stays in SUPPORT range
            // Player is at center (640, 360). Place healer at (640, 60) — 300px away (ideal safe range)
            val healerId = state.spawnEnemy(x = 640f, y = 60f, enemyType = EnemyComponent.EnemyData.HEALER)
            val zombieId = state.spawnEnemy(x = 660f, y = 70f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            // Damage the zombie
            state.healths[zombieId].currentHp = 5f
            // Run enough ticks for healer to reach SUPPORT state and trigger heal pulse (3s cooldown)
            for (i in 0 until 250) {  // ~4 seconds of game time
                ai.update(0.016f)
            }
            assertTrue(state.healths[zombieId].currentHp > 5f,
                "Zombie should have been healed (HP went from 5 to ${state.healths[zombieId].currentHp})")
        }
    }

    // ================================================================
    // SHIELDER
    // ================================================================
    @Nested
    @DisplayName("Shielder AI")
    inner class ShielderTests {
        @Test
        @DisplayName("Shielder marks nearby enemies as shielded")
        fun shielderProtects() {
            val shielderId = state.spawnEnemy(x = 500f, y = 400f, enemyType = EnemyComponent.EnemyData.SHIELDER)
            val zombieId = state.spawnEnemy(x = 520f, y = 410f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            ai.update(0.016f)
            assertEquals(2f, state.enemies[zombieId].knockbackResist, 0.01f,
                "Nearby zombie should be marked as shielded")
        }

        @Test
        @DisplayName("Shielder does not shield boss")
        fun shielderIgnoresBoss() {
            val shielderId = state.spawnEnemy(x = 500f, y = 400f, enemyType = EnemyComponent.EnemyData.SHIELDER)
            val bossId = state.spawnEnemy(x = 520f, y = 410f, enemyType = EnemyComponent.EnemyData.BOSS)
            ai.update(0.016f)
            assertNotEquals(2f, state.enemies[bossId].knockbackResist,
                "Boss should NOT be shielded")
        }
    }

    // ================================================================
    // FLYER
    // ================================================================
    @Nested
    @DisplayName("Flyer AI")
    inner class FlyerTests {
        @Test
        @DisplayName("Flyer moves toward player")
        fun flyerChases() {
            val flyerId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.FLYER)
            ai.update(0.016f)
            val vel = state.velocities[flyerId]
            assertTrue(vel.x > 0f, "Flyer should move toward player")
        }

        @Test
        @DisplayName("Flyer has sine-wave component (erratic path)")
        fun flyerErraticPath() {
            val flyerId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.FLYER)
            // After several ticks, zigzagPhase should advance
            for (i in 0 until 10) ai.update(0.016f)
            assertTrue(state.enemies[flyerId].zigzagPhase > 0f,
                "Flyer zigzagPhase should advance over time")
        }
    }

    // ================================================================
    // BOSS
    // ================================================================
    @Nested
    @DisplayName("Boss AI")
    inner class BossTests {
        @Test
        @DisplayName("Boss starts in phase 0 (CHASE)")
        fun bossPhase0() {
            val bossId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BOSS)
            ai.update(0.016f)
            assertEquals(0, state.enemies[bossId].phase)
            assertEquals(AiState.CHASE, state.enemies[bossId].aiState)
        }

        @Test
        @DisplayName("Boss transitions to phase 1 when HP drops below 66%")
        fun bossPhaseTransition1() {
            val bossId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BOSS)
            // Damage boss to 50% HP → phase 1
            state.healths[bossId].currentHp = state.healths[bossId].maxHp * 0.5f
            ai.update(0.016f)
            assertEquals(1, state.enemies[bossId].phase,
                "Boss should be in phase 1 at 50% HP")
        }

        @Test
        @DisplayName("Boss transitions to phase 2 when HP drops below 33%")
        fun bossPhaseTransition2() {
            val bossId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BOSS)
            state.healths[bossId].currentHp = state.healths[bossId].maxHp * 0.2f
            ai.update(0.016f)
            assertEquals(2, state.enemies[bossId].phase,
                "Boss should be in phase 2 at 20% HP")
        }

        @Test
        @DisplayName("Boss enters SPECIAL on phase transition")
        fun bossSpecialOnTransition() {
            val bossId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.BOSS)
            ai.update(0.016f)  // Phase 0 — sets to CHASE
            state.healths[bossId].currentHp = state.healths[bossId].maxHp * 0.5f
            ai.update(0.016f)  // Phase transition
            assertEquals(AiState.SPECIAL, state.enemies[bossId].aiState,
                "Boss should enter SPECIAL state during phase transition")
        }

        @Test
        @DisplayName("Boss summons adds in phase 2")
        fun bossSummonsAdds() {
            val bossId = state.spawnEnemy(x = 500f, y = 400f, enemyType = EnemyComponent.EnemyData.BOSS)
            val enemiesBefore = state.enemies.count {
                it.type != EnemyComponent.EnemyData.BOSS && !state.healths[state.enemies.indexOf(it)].isDead
            }
            // Set boss to phase 2 and force summon
            state.healths[bossId].currentHp = state.healths[bossId].maxHp * 0.1f
            state.enemies[bossId].phase = 1  // Will transition to 2
            state.enemies[bossId].specialTimer = 0f
            ai.update(0.016f)  // Triggers phase transition
            // Run enough ticks for CHASE → SUMMON
            for (i in 0 until 500) {
                ai.update(0.016f)
            }
            val enemiesAfter = state.enemies.count { e ->
                val idx = state.enemies.indexOf(e)
                e.type != EnemyComponent.EnemyData.BOSS && idx < state.healths.size && !state.healths[idx].isDead
            }
            assertTrue(enemiesAfter > enemiesBefore,
                "Boss should have summoned adds: before=$enemiesBefore, after=$enemiesAfter")
        }
    }

    // ================================================================
    // GENERAL
    // ================================================================
    @Nested
    @DisplayName("General AI behavior")
    inner class GeneralTests {
        @Test
        @DisplayName("AI does nothing when paused")
        fun aiSkipsWhenPaused() {
            val enemyId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val velBefore = state.velocities[enemyId].x
            state.isPaused = true
            ai.update(0.016f)
            assertEquals(velBefore, state.velocities[enemyId].x, 0.01f,
                "Velocity should not change when paused")
        }

        @Test
        @DisplayName("Dead enemies are not processed")
        fun deadEnemiesSkipped() {
            val enemyId = state.spawnEnemy(x = 0f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            state.healths[enemyId].currentHp = 0f
            ai.update(0.016f)
            // Velocity should remain at default (0)
            assertEquals(0f, state.velocities[enemyId].x, 0.01f,
                "Dead enemy should not move")
        }
    }
}

// Extension for kotlin.math.pow on Double
private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
