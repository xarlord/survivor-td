package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.ChapterConfig
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class ChapterAndWaveTest {

    private lateinit var state: GameState
    private lateinit var waveSys: WaveSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        waveSys = WaveSystem(state, ChapterConfig.WASTELAND)
    }

    @Nested
    @DisplayName("Chapter configuration")
    inner class ChapterConfigTest {

        @Test
        @DisplayName("Wasteland chapter has correct settings")
        fun wastelandSettings() {
            val ch = ChapterConfig.WASTELAND
            assertEquals("ch1_wasteland", ch.chapterId)
            assertEquals("Wasteland", ch.name)
            assertEquals(900, ch.durationSeconds)
            assertEquals(3, ch.bossTimeMinutes.size)
            assertTrue(ch.bossTimeMinutes.contains(5))
            assertTrue(ch.bossTimeMinutes.contains(10))
            assertTrue(ch.bossTimeMinutes.contains(15))
        }

        @Test
        @DisplayName("5 chapters defined")
        fun fiveChapters() {
            assertEquals(5, ChapterConfig.ALL_CHAPTERS.size)
        }

        @Test
        @DisplayName("Final bunker is 20 minutes")
        fun finalBunkerDuration() {
            assertEquals(1200, ChapterConfig.FINAL_BUNKER.durationSeconds)
        }

        @Test
        @DisplayName("Enemy pool changes by minute")
        fun enemyPoolChanges() {
            val ch = ChapterConfig.WASTELAND
            // Minute 0: only zombie+runner
            val pool0 = ch.getActivePool(0f)
            assertFalse(pool0.containsKey(EnemyComponent.EnemyData.BRUTE))

            // Minute 5: brute and spitter appear
            val pool5 = ch.getActivePool(301f)  // 5 min 1 sec
            assertTrue(pool5.containsKey(EnemyComponent.EnemyData.BRUTE))
        }

        @Test
        @DisplayName("Boss spawn detection")
        fun bossSpawnDetection() {
            val ch = ChapterConfig.WASTELAND
            assertEquals(0, ch.shouldSpawnBoss(300f))  // 5 min → boss 0
            assertEquals(-1, ch.shouldSpawnBoss(200f)) // 3.3 min → no boss
        }
    }

    @Nested
    @DisplayName("Wave spawning")
    inner class WaveSpawning {

        @Test
        @DisplayName("Spawns enemies over time")
        fun spawnsEnemies() {
            // Simulate 3 seconds of spawning
            repeat(180) { waveSys.update(0.016f) }
            assertTrue(waveSys.totalSpawned > 0, "Should have spawned enemies")
        }

        @Test
        @DisplayName("Spawn rate increases over time")
        fun spawnRateIncreases() {
            // Early game (minute 0): ~1 spawn per 1.5s
            val earlySpawns = countSpawns(10f)

            // Advance to mid-game (3 min — before boss time)
            state.elapsedSeconds = 180f
            waveSys = WaveSystem(state, ChapterConfig.WASTELAND) // Fresh wave system
            val midSpawns = countSpawns(10f)

            assertTrue(midSpawns > earlySpawns,
                "Spawn rate should increase over time: early=$earlySpawns, mid=$midSpawns")
        }

        @Test
        @DisplayName("Boss spawns at configured time")
        fun bossSpawns() {
            state.elapsedSeconds = 0f
            waveSys = WaveSystem(state, ChapterConfig.WASTELAND)
            // Advance to 5 minutes
            state.elapsedSeconds = 300f
            waveSys.update(0.016f)
            // Boss should be on the field
            val hasBoss = state.enemies.any { it.type == EnemyComponent.EnemyData.BOSS }
            assertTrue(hasBoss, "Boss should spawn at minute 5")
        }

        @Test
        @DisplayName("Normal spawning pauses during boss")
        fun pausesDuringBoss() {
            state.elapsedSeconds = 300f  // 5 min
            waveSys.update(0.016f)  // Spawn boss

            val spawnedBeforePause = waveSys.totalSpawned
            // Simulate 5 seconds with boss alive
            repeat(300) { waveSys.update(0.016f) }
            assertEquals(spawnedBeforePause, waveSys.totalSpawned,
                "No new enemies should spawn during boss fight")
        }

        private fun countSpawns(duration: Float): Int {
            val before = waveSys.totalSpawned
            val ticks = (duration / 0.016f).toInt()
            repeat(ticks) { waveSys.update(0.016f) }
            return waveSys.totalSpawned - before
        }
    }

    @Nested
    @DisplayName("Meta-progression economy")
    inner class MetaEconomy {

        @Test
        @DisplayName("Earn gold from kills")
        fun earnGold() {
            val meta = MetaProgression(gold = 0)
            meta.addGold(500)
            assertEquals(500, meta.gold)
        }

        @Test
        @DisplayName("Gold capped at 99999")
        fun goldCap() {
            val meta = MetaProgression(gold = 99990)
            meta.addGold(100)
            assertEquals(99999, meta.gold)
        }

        @Test
        @DisplayName("Buy Max HP upgrade")
        fun buyMaxHp() {
            val meta = MetaProgression(gold = 500)
            assertTrue(meta.buyMaxHp())
            assertEquals(1, meta.maxHpLevel)
            assertEquals(0, meta.gold)  // Cost 500
        }

        @Test
        @DisplayName("Cannot buy without gold")
        fun cannotBuyNoGold() {
            val meta = MetaProgression(gold = 100)
            assertFalse(meta.buyMaxHp())
            assertEquals(0, meta.maxHpLevel)
        }

        @Test
        @DisplayName("Max level limits")
        fun maxLevelLimits() {
            val meta = MetaProgression(gold = 999999)
            for (i in 0 until 10) {
                assertTrue(meta.buyMaxHp(), "Should buy level ${i+1}")
            }
            assertFalse(meta.buyMaxHp(), "Should not exceed max level 10")
            assertEquals(10, meta.maxHpLevel)
        }

        @Test
        @DisplayName("Upgrade cost doubles each level")
        fun costDoubles() {
            assertEquals(500, MetaProgression.upgradeCost(500, 0))   // L1: 500
            assertEquals(1000, MetaProgression.upgradeCost(500, 1))  // L2: 1000
            assertEquals(2000, MetaProgression.upgradeCost(500, 2))  // L3: 2000
        }

        @Test
        @DisplayName("Unlock chapters")
        fun unlockChapter() {
            val meta = MetaProgression(gold = 10000)
            assertTrue(meta.unlockChapter("ch2_toxic_swamp", 5000))
            assertTrue(meta.chaptersUnlocked.contains("ch2_toxic_swamp"))
            assertEquals(5000, meta.gold)
        }

        @Test
        @DisplayName("Start with Wasteland unlocked")
        fun startWithWasteland() {
            val meta = MetaProgression()
            assertTrue(meta.chaptersUnlocked.contains("ch1_wasteland"))
        }

        @Test
        @DisplayName("Apply bonuses to GameState")
        fun applyBonuses() {
            val meta = MetaProgression(
                maxHpLevel = 3,
                moveSpeedLevel = 2,
                damageLevel = 5,
                pickupRangeLevel = 1
            )
            val state = GameState()
            state.spawnPlayer()
            val hpBefore = state.healths[state.playerIndex].maxHp
            val speedBefore = state.players[state.playerIndex].moveSpeed
            val dmgBefore = state.players[state.playerIndex].damageMult

            MetaProgression.applyToGameState(meta, state)

            assertEquals(hpBefore + 60f, state.healths[state.playerIndex].maxHp)
            assertEquals(speedBefore + 20f, state.players[state.playerIndex].moveSpeed)
            assertEquals(dmgBefore + 0.25f, state.players[state.playerIndex].damageMult, 0.001f)
        }
    }
}
