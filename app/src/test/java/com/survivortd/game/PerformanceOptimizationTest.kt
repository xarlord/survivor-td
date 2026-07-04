package com.survivortd.game

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import com.survivortd.game.systems.WaveSystem
import com.survivortd.game.utils.FrustumCuller
import com.survivortd.game.utils.ObjectPool
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for #115 performance optimizations:
 * - Entity cap enforcement
 * - Object pooling
 * - Frustum culling
 * - Spawn rate throttling
 */
class PerformanceOptimizationTest {

    private lateinit var state: GameState

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
    }

    @Nested
    @DisplayName("Entity Cap Enforcement")
    inner class EntityCapTest {

        @Test
        fun `enemy cap is enforced at MAX_ENEMIES`() {
            val cap = GameConfig.MAX_ENEMIES
            var spawned = 0
            for (i in 0 until cap + 10) {
                val id = state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.ZOMBIE)
                if (id >= 0) spawned++
            }
            assertEquals(cap, spawned, "Should not spawn more than MAX_ENEMIES")
            assertEquals(cap, state.activeEnemyCount, "Active enemy count should match cap")
        }

        @Test
        fun `projectile cap is enforced at MAX_PROJECTILES`() {
            val cap = GameConfig.MAX_PROJECTILES
            var spawned = 0
            for (i in 0 until cap + 10) {
                val id = state.spawnProjectile(0f, 0f)
                if (id >= 0) spawned++
            }
            assertEquals(cap, spawned, "Should not spawn more than MAX_PROJECTILES")
            assertEquals(cap, state.activeProjectileCount, "Active projectile count should match cap")
        }

        @Test
        fun `pickup cap is enforced at MAX_PICKUPS`() {
            val cap = GameConfig.MAX_PICKUPS
            var spawned = 0
            for (i in 0 until cap + 10) {
                val id = state.spawnPickup(0f, 0f, xpValue = 1)
                if (id >= 0) spawned++
            }
            assertEquals(cap, spawned, "Should not spawn more than MAX_PICKUPS")
            assertEquals(cap, state.activePickupCount, "Active pickup count should match cap")
        }

        @Test
        fun `entity counters decrement on cleanup`() {
            // Spawn some enemies
            repeat(5) { state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.ZOMBIE) }
            assertEquals(5, state.activeEnemyCount)

            // Kill them
            for (i in state.enemies.indices) {
                if (i < state.tags.size &&
                    state.tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY
                ) {
                    state.healths[i].currentHp = 0f
                }
            }
            state.cleanupDeadEntities()
            assertEquals(0, state.activeEnemyCount, "Enemy counter should be 0 after cleanup")
        }

        @Test
        fun `counters reset on game state reset`() {
            state.spawnEnemy(0f, 0f, EnemyComponent.EnemyData.ZOMBIE)
            state.spawnProjectile(0f, 0f)
            state.spawnPickup(0f, 0f, xpValue = 1)
            assertTrue(state.activeEnemyCount > 0)
            assertTrue(state.activeProjectileCount > 0)
            assertTrue(state.activePickupCount > 0)

            state.reset()
            assertEquals(0, state.activeEnemyCount)
            assertEquals(0, state.activeProjectileCount)
            assertEquals(0, state.activePickupCount)
        }

        @Test
        fun `MAX_ENTITIES is at least 1000`() {
            assertTrue(
                GameConfig.MAX_ENTITIES >= 1000,
                "MAX_ENTITIES should be at least 1000"
            )
        }

        @Test
        fun `MAX_ENEMIES is at least 300`() {
            assertTrue(
                GameConfig.MAX_ENEMIES >= 300,
                "MAX_ENEMIES should be at least 300"
            )
        }

        @Test
        fun `MAX_PROJECTILES is at least 200`() {
            assertTrue(
                GameConfig.MAX_PROJECTILES >= 200,
                "MAX_PROJECTILES should be at least 200"
            )
        }

        @Test
        fun `MAX_PICKUPS is at least 150`() {
            assertTrue(
                GameConfig.MAX_PICKUPS >= 150,
                "MAX_PICKUPS should be at least 150"
            )
        }
    }

    @Nested
    @DisplayName("Spawn Rate Throttling")
    inner class SpawnThrottlingTest {

        @Test
        fun `MAX_SPAWN_PER_FRAME is a positive number`() {
            assertTrue(
                GameConfig.MAX_SPAWN_PER_FRAME > 0,
                "MAX_SPAWN_PER_FRAME should be positive"
            )
        }

        @Test
        fun `MAX_SPAWN_PER_FRAME is at most 5`() {
            assertTrue(
                GameConfig.MAX_SPAWN_PER_FRAME <= 5,
                "MAX_SPAWN_PER_FRAME should be reasonable (<= 5)"
            )
        }

        @Test
        fun `wave system does not spawn more than MAX_SPAWN_PER_FRAME per tick`() {
            // Create a WaveSystem and force it to want to spawn many enemies
            val waveSystem = WaveSystem(state)
            // Manually set state to trigger spawning
            state.currentWave = 1
            state.waveEnemiesRemaining = 100
            state.isPaused = false

            // The wave system should cap spawning at MAX_SPAWN_PER_FRAME per tick
            // We can verify this indirectly: after one update with a very short interval,
            // at most MAX_SPAWN_PER_FRAME enemies should have been spawned
            val before = state.activeEnemyCount
            waveSystem.update(0.001f)
            val spawned = state.activeEnemyCount - before
            assertTrue(
                spawned <= GameConfig.MAX_SPAWN_PER_FRAME,
                "Wave system should not spawn more than $GameConfig.MAX_SPAWN_PER_FRAME per tick, got $spawned"
            )
        }
    }

    @Nested
    @DisplayName("Frustum Culling Bounds")
    inner class FrustumCullingBoundsTest {

        @Test
        fun `culler correctly identifies visible area`() {
            val culler = FrustumCuller()
            culler.margin = 50f
            culler.update(camX = 0f, camY = 0f, viewWidth = 1280f, viewHeight = 720f)

            // Inside viewport
            assertTrue(culler.isVisible(640f, 360f))
            assertTrue(culler.isVisible(0f, 0f))
            assertTrue(culler.isVisible(1280f, 720f))

            // Within margin
            assertTrue(culler.isVisible(-30f, 360f))
            assertTrue(culler.isVisible(1310f, 360f))

            // Outside viewport + margin
            assertFalse(culler.isVisible(-100f, 360f))
            assertFalse(culler.isVisible(1400f, 360f))
        }
    }

    @Nested
    @DisplayName("Object Pool Integration")
    inner class ObjectPoolIntegrationTest {

        @Test
        fun `game state has projectile pool`() {
            assertNotNull(state.projectilePool)
            assertTrue(state.projectilePool.size > 0, "Projectile pool should be pre-populated")
        }

        @Test
        fun `game state has pickup pool`() {
            assertNotNull(state.pickupPool)
            assertTrue(state.pickupPool.size > 0, "Pickup pool should be pre-populated")
        }

        @Test
        fun `game state has status effect pool`() {
            assertNotNull(state.statusEffectPool)
            assertTrue(state.statusEffectPool.size > 0, "StatusEffect pool should be pre-populated")
        }
    }
}
