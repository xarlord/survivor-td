package com.survivortd.game.systems

import com.survivortd.game.config.ChapterConfig
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for #97 Wave Progression system.
 */
class WaveProgressionTest {

    private lateinit var state: GameState
    private lateinit var waveSys: WaveSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        waveSys = WaveSystem(state, ChapterConfig.WASTELAND)
    }

    @Nested
    @DisplayName("Wave state tracking")
    inner class WaveStateTracking {

        @Test
        @DisplayName("Game starts at wave 0 before first update")
        fun startsAtWaveZero() {
            assertEquals(0, state.currentWave)
        }

        @Test
        @DisplayName("First update starts wave 1")
        fun firstUpdateStartsWave1() {
            waveSys.update(0.016f)
            assertEquals(1, state.currentWave)
        }

        @Test
        @DisplayName("Wave announcement displays on start")
        fun announcementOnWaveStart() {
            waveSys.update(0.016f)
            assertEquals("WAVE 1", state.waveAnnouncementText)
            assertTrue(state.waveAnnouncementTimer > 0f)
        }

        @Test
        @DisplayName("Boss wave announcement shows warning")
        fun bossWaveAnnouncement() {
            state.currentWave = 4
            waveSys.startNextWave()
            assertEquals("⚠ BOSS INCOMING ⚠", state.waveAnnouncementText)
            assertTrue(state.isBossWave)
        }
    }

    @Nested
    @DisplayName("Wave progression")
    inner class WaveProgressionLogic {

        @Test
        @DisplayName("onEnemyKilled decrements remaining enemies")
        fun onEnemyKilledDecrements() {
            waveSys.startNextWave()
            val before = state.waveEnemiesRemaining
            waveSys.onEnemyKilled()
            assertEquals(before - 1, state.waveEnemiesRemaining)
        }

        @Test
        @DisplayName("Wave intermission triggers after all spawned+killed")
        fun intermissionAfterClear() {
            // Start wave 1
            waveSys.update(0.016f)
            assertEquals(1, state.currentWave)

            // Kill enemies as they spawn by ticking and calling onEnemyKilled
            // until the wave completes
            var maxIterations = 3000
            while (!state.wavePaused && maxIterations-- > 0) {
                // Count enemies before tick
                val enemyCount = countEnemies()
                waveSys.update(0.016f)
                val newEnemyCount = countEnemies()
                // Kill newly spawned enemies
                val newKills = newEnemyCount - enemyCount
                repeat(maxOf(0, newKills)) { waveSys.onEnemyKilled() }
            }
            assertTrue(state.wavePaused,
                "Should enter intermission after clearing enemies (wave=${state.currentWave})")
        }

        @Test
        @DisplayName("cleanupDeadEntities returns killed enemy count")
        fun cleanupReturnsKilledCount() {
            val playerPos = state.positions[state.playerIndex]
            val e1 = state.spawnEnemy(
                x = playerPos.x + 100f, y = playerPos.y,
                enemyType = com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE
            )
            val e2 = state.spawnEnemy(
                x = playerPos.x + 200f, y = playerPos.y,
                enemyType = com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE
            )
            // Kill both
            state.healths[e1].currentHp = 0f
            state.healths[e2].currentHp = 0f
            val killed = state.cleanupDeadEntities()
            assertEquals(2, killed)
            assertEquals(2, state.totalKills)
        }

        @Test
        @DisplayName("Wave enemy quota scales per wave")
        fun waveQuotaScales() {
            waveSys.startNextWave()
            val wave1 = state.waveEnemiesRemaining

            state.currentWave = 1
            waveSys.startNextWave()
            val wave2 = state.waveEnemiesRemaining

            state.currentWave = 2
            waveSys.startNextWave()
            val wave3 = state.waveEnemiesRemaining

            assertTrue(wave2 > wave1, "Wave 2 ($wave2) > Wave 1 ($wave1)")
            assertTrue(wave3 > wave2, "Wave 3 ($wave3) > Wave 2 ($wave2)")
        }

        @Test
        @DisplayName("Boss wave every N waves")
        fun bossWaveInterval() {
            // Wave 5: boss
            state.currentWave = 4
            waveSys.startNextWave()
            assertTrue(state.isBossWave, "Wave 5 should be boss")

            // Wave 6: not boss
            state.currentWave = 5
            waveSys.startNextWave()
            assertFalse(state.isBossWave, "Wave 6 should not be boss")

            // Wave 10: boss
            state.currentWave = 9
            waveSys.startNextWave()
            assertTrue(state.isBossWave, "Wave 10 should be boss")
        }
    }

    @Nested
    @DisplayName("GameConfig wave constants")
    inner class WaveConstants {

        @Test
        @DisplayName("Wave duration is 45 seconds")
        fun waveDuration() {
            assertEquals(45f, GameConfig.WAVE_DURATION_SECONDS)
        }

        @Test
        @DisplayName("Wave pause is 5 seconds")
        fun wavePause() {
            assertEquals(5f, GameConfig.WAVE_PAUSE_SECONDS)
        }

        @Test
        @DisplayName("Boss wave interval is 5")
        fun bossInterval() {
            assertEquals(5, GameConfig.BOSS_WAVE_INTERVAL)
        }

        @Test
        @DisplayName("Base enemy count is 10")
        fun baseEnemyCount() {
            assertEquals(10, GameConfig.WAVE_ENEMY_BASE_COUNT)
        }

        @Test
        @DisplayName("Enemy scale per wave is 3")
        fun enemyScale() {
            assertEquals(3, GameConfig.WAVE_ENEMY_SCALE_PER_WAVE)
        }
    }

    private fun countEnemies(): Int {
        var count = 0
        for (i in state.tags.indices) {
            if (state.tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY
                && !state.healths[i].isDead
            ) {
                count++
            }
        }
        return count
    }
}
