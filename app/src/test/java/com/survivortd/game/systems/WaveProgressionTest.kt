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
 * Wave progression tests — GDD §8 continuous spawn + timed bosses.
 * Issue #145: restore GDD-aligned wave model.
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
    @DisplayName("Continuous combat start")
    inner class CombatStart {

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
        @DisplayName("Continuous mode spawns enemies over time")
        fun continuousSpawns() {
            waveSys.update(0.016f)
            // ~3s of continuous combat
            state.elapsedSeconds = 1f
            repeat(200) {
                state.elapsedSeconds += 0.016f
                waveSys.update(0.016f)
            }
            assertTrue(waveSys.totalSpawned > 0, "Should spawn enemies continuously")
        }
    }

    @Nested
    @DisplayName("GDD timed bosses (§8.3)")
    inner class TimedBosses {

        @Test
        @DisplayName("Boss minutes are 5, 10, 15")
        fun bossMinutesConfig() {
            assertEquals(listOf(5, 10, 15), GameConfig.BOSS_TIMES_MINUTES)
        }

        @Test
        @DisplayName("Boss spawns at minute 5")
        fun bossAtMinute5() {
            waveSys.update(0.016f)
            state.elapsedSeconds = 5f * 60f
            waveSys.update(0.016f)
            assertTrue(state.isBossWave, "Minute 5 should trigger boss")
            assertTrue(
                state.enemies.any { it.type == ChapterConfig.WASTELAND.bossType },
                "Boss entity should spawn"
            )
        }

        @Test
        @DisplayName("Normal spawns pause while boss is alive")
        fun pauseDuringBoss() {
            waveSys.update(0.016f)
            state.elapsedSeconds = 5f * 60f
            waveSys.update(0.016f)
            assertTrue(state.isBossWave)
            val before = waveSys.totalSpawned
            state.elapsedSeconds += 2f
            repeat(120) { waveSys.update(0.016f) }
            assertEquals(before, waveSys.totalSpawned, "No normal spawns during boss")
        }

        @Test
        @DisplayName("Build phase starts after boss death")
        fun buildPhaseAfterBoss() {
            waveSys.update(0.016f)
            state.elapsedSeconds = 5f * 60f
            waveSys.update(0.016f)
            assertTrue(state.isBossWave)
            // Kill all bosses
            for (i in state.enemies.indices) {
                if (state.tags[i].tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY) {
                    state.healths[i].currentHp = 0f
                }
            }
            waveSys.update(0.016f)
            assertTrue(waveSys.isBuildPhase, "Build phase should start after boss death")
        }

        @Test
        @DisplayName("startNextWave: wave multiples of 5 are boss (compat)")
        fun startNextWaveBossCompat() {
            state.currentWave = 4
            waveSys.startNextWave()
            // After increment: wave 5
            assertEquals(5, state.currentWave)
            assertTrue(state.isBossWave, "Wave 5 should be boss")
            assertEquals("⚠ BOSS INCOMING ⚠", state.waveAnnouncementText)
        }
    }

    @Nested
    @DisplayName("Compat wave quota helpers")
    inner class CompatQuota {

        @Test
        @DisplayName("onEnemyKilled decrements remaining enemies")
        fun onEnemyKilledDecrements() {
            waveSys.startNextWave()
            val before = state.waveEnemiesRemaining
            waveSys.onEnemyKilled()
            assertEquals(before - 1, state.waveEnemiesRemaining)
        }

        @Test
        @DisplayName("Wave enemy quota scales with wave index (display counter)")
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
            state.healths[e1].currentHp = 0f
            state.healths[e2].currentHp = 0f
            val killed = state.cleanupDeadEntities()
            assertEquals(2, killed)
            assertEquals(2, state.totalKills)
        }
    }

    @Nested
    @DisplayName("GameConfig wave constants (GDD)")
    inner class WaveConstants {

        @Test
        @DisplayName("Wave pause is 5 seconds")
        fun wavePause() {
            assertEquals(5f, GameConfig.WAVE_PAUSE_SECONDS)
        }

        @Test
        @DisplayName("Build phase duration is 10 seconds")
        fun buildDuration() {
            assertEquals(10f, GameConfig.BUILD_PHASE_DURATION)
        }

        @Test
        @DisplayName("Boss wave interval compat is 5")
        fun bossInterval() {
            assertEquals(5, GameConfig.BOSS_WAVE_INTERVAL)
        }

        @Test
        @DisplayName("Base spawn interval is 1.0s")
        fun baseSpawn() {
            assertEquals(1.0f, GameConfig.BASE_SPAWN_INTERVAL)
        }

        @Test
        @DisplayName("Min spawn interval is 0.3s")
        fun minSpawn() {
            assertEquals(0.3f, GameConfig.MIN_SPAWN_INTERVAL)
        }
    }
}
