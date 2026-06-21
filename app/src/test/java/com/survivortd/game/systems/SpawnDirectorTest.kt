package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SpawnDirectorTest {

    private lateinit var state: GameState
    private lateinit var director: SpawnDirector

    @BeforeEach
    fun setUp() {
        state = GameState()
        state.spawnPlayer()
        director = SpawnDirector(state, Random(42))  // Deterministic seed
    }

    @Test
    @DisplayName("No enemies spawn at time 0 until first interval passes")
    fun noSpawnAtStart() {
        director.update(0.5f)  // Less than base interval (1.0s)
        val enemyCount = state.tags.count { it.tag.name == "ENEMY" }
        assertEquals(0, enemyCount)
    }

    @Test
    @DisplayName("First enemy spawns after 1 second")
    fun firstSpawnAfterOneSecond() {
        director.update(1.1f)  // Just over base interval
        val enemyCount = state.tags.count { it.tag.name == "ENEMY" }
        assertEquals(1, enemyCount)
    }

    @Test
    @DisplayName("Multiple enemies spawn over time")
    fun multipleSpawnsOverTime() {
        // Simulate 10 seconds of game time in 0.5s ticks
        repeat(20) { director.update(0.5f) }
        val enemyCount = state.tags.count { it.tag.name == "ENEMY" }
        assertTrue(enemyCount >= 5, "Should have spawned at least 5 enemies in 10s, got $enemyCount")
    }

    @Test
    @DisplayName("Spawn rate increases as time passes")
    fun spawnRateIncreases() {
        // Simulate 5 seconds
        repeat(10) { director.update(0.5f) }
        val earlyCount = state.tags.count { it.tag.name == "ENEMY" }

        // Reset and simulate 5 more seconds at minute 10 equivalent
        state.elapsedSeconds = 600f  // 10 minutes
        repeat(10) { director.update(0.5f) }
        val lateCount = state.tags.count { it.tag.name == "ENEMY" } - earlyCount

        // Late game should spawn faster (lower interval)
        assertTrue(lateCount >= earlyCount, "Late game spawn rate should be >= early game")
    }

    @Test
    @DisplayName("No spawning when game is paused")
    fun noSpawnWhenPaused() {
        state.isPaused = true
        director.update(5f)
        val enemyCount = state.tags.count { it.tag.name == "ENEMY" }
        assertEquals(0, enemyCount)
    }

    @Test
    @DisplayName("Boss spawns at minute 5")
    fun bossSpawnAt5Min() {
        // Set elapsed to just before 5 minutes
        state.elapsedSeconds = 299.5f
        director.update(0.6f)  // Crosses the 300s mark
        val bossCount = state.enemies.count { it.type == EnemyComponent.EnemyData.BOSS }
        assertEquals(1, bossCount, "Boss should spawn at minute 5")
    }

    @Test
    @DisplayName("Enemies spawn at world edges (off-screen)")
    fun enemiesSpawnAtEdges() {
        director.update(1.1f)
        val enemyPos = state.positions.lastOrNull { state.tags[state.positions.indexOf(it)].tag.name == "ENEMY" }
        assertNotNull(enemyPos)
        // Enemy should be outside or near the edge of the world
        val outOfBounds = enemyPos!!.x < 0 || enemyPos.x > GameConfig.WORLD_WIDTH ||
                          enemyPos.y < 0 || enemyPos.y > GameConfig.WORLD_HEIGHT
        assertTrue(outOfBounds, "Enemy should spawn at world edge, got (${enemyPos.x}, ${enemyPos.y})")
    }
}
