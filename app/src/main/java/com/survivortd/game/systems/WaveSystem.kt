package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.ChapterConfig
import com.survivortd.game.config.GameBalance
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wave System — manages discrete wave-based enemy spawning with announcements.
 *
 * Spawning logic:
 * - Discrete waves: each wave lasts WAVE_DURATION_SECONDS
 * - Enemy count scales per wave
 * - Brief intermission (WAVE_PAUSE_SECONDS) between waves
 * - Boss wave every BOSS_WAVE_INTERVAL waves
 * - Wave announcements displayed in HUD
 * - Difficulty scales per wave (HP, enemy count)
 */
class WaveSystem(
    private val state: GameState,
    private val chapter: ChapterConfig = ChapterConfig.WASTELAND
) {
    private var spawnTimer = 0f
    private var waveTimer = 0f

    // Boss tracking
    private var bossActive = false
    private var bossPauseTimer = 0f
    private var buildPhaseTimer = 0f

    var isBuildPhase = false
        private set

    /** Total enemies spawned this match */
    var totalSpawned = 0
        private set

    /** Enemies killed in current wave */
    private var waveKillCount = 0

    /** Total enemies to spawn in current wave */
    private var waveSpawnQuota = 0

    /** Enemies already spawned in current wave */
    private var waveSpawnCount = 0

    // (#115) Spawn rate throttling — don't spawn more than N enemies per tick
    private var spawnedThisTick = 0

    /**
     * Start a new wave. Call after game initialization or after intermission.
     */
    fun startNextWave() {
        state.currentWave++
        state.wavePaused = false
        state.wavePauseTimer = 0f
        waveTimer = 0f
        waveSpawnCount = 0
        waveKillCount = 0

        // Boss every N waves
        state.isBossWave = (state.currentWave % GameConfig.BOSS_WAVE_INTERVAL == 0)

        if (state.isBossWave) {
            // Boss wave: spawn boss immediately, quota is just the boss
            waveSpawnQuota = 1
            state.waveAnnouncementText = "⚠ BOSS INCOMING ⚠"
        } else {
            // Normal wave: scale enemy count
            waveSpawnQuota = GameConfig.WAVE_ENEMY_BASE_COUNT +
                (state.currentWave - 1) * GameConfig.WAVE_ENEMY_SCALE_PER_WAVE
            state.waveAnnouncementText = "WAVE ${state.currentWave}"
        }

        state.waveEnemiesRemaining = waveSpawnQuota
        state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION

        // Calculate spawn interval for this wave
        val waveInterval = GameConfig.WAVE_DURATION_SECONDS / waveSpawnQuota.coerceAtLeast(1)
        spawnTimer = 0f // Spawn first enemy immediately

        if (state.isBossWave) {
            spawnBossForWave()
            waveSpawnCount++
        }
    }

    /**
     * Main update — called every tick.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        // (#115) Reset per-tick spawn counter
        spawnedThisTick = 0

        // Tick down announcement timer
        if (state.waveAnnouncementTimer > 0f) {
            state.waveAnnouncementTimer -= dt
        }

        // Handle boss active state
        if (bossActive) {
            if (isBossDead()) {
                bossActive = false
                buildPhaseTimer = GameConfig.BUILD_PHASE_DURATION
                isBuildPhase = true
                state.waveEnemiesRemaining = 0
                state.waveAnnouncementText = "BOSS DEFEATED!"
                state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
            }
            return
        }

        // Handle build phase (post-boss)
        if (isBuildPhase) {
            buildPhaseTimer -= dt
            if (buildPhaseTimer <= 0f) {
                isBuildPhase = false
                startIntermission()
            }
            return
        }

        // Handle intermission between waves
        if (state.wavePaused) {
            state.wavePauseTimer -= dt
            if (state.wavePauseTimer <= 0f) {
                startNextWave()
            }
            return
        }

        // If no wave active yet, start wave 1
        if (state.currentWave == 0) {
            startNextWave()
            return
        }

        // Wave timer — auto-advance when wave time expires or all enemies spawned+killed
        waveTimer += dt
        val allDone = waveSpawnCount >= waveSpawnQuota && state.waveEnemiesRemaining <= 0

        if (waveTimer >= GameConfig.WAVE_DURATION_SECONDS || allDone) {
            startIntermission()
            return
        }

        // Spawn enemies at calculated interval
        if (waveSpawnCount < waveSpawnQuota) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val waveInterval = (GameConfig.WAVE_DURATION_SECONDS - waveTimer) /
                    (waveSpawnQuota - waveSpawnCount).coerceAtLeast(1)
                spawnTimer = waveInterval.coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)
                // (#115) Frame-budget limiting: don't spawn more than MAX_SPAWN_PER_FRAME per tick
                if (spawnedThisTick < GameConfig.MAX_SPAWN_PER_FRAME) {
                    spawnEnemy()
                    waveSpawnCount++
                    spawnedThisTick++
                }
            }
        }
    }

    /**
     * Called when an enemy dies — track wave progress.
     */
    fun onEnemyKilled() {
        if (!state.isBossWave && state.waveEnemiesRemaining > 0) {
            state.waveEnemiesRemaining--
        }
        waveKillCount++
    }

    /**
     * Start intermission between waves.
     */
    private fun startIntermission() {
        state.wavePaused = true
        state.wavePauseTimer = GameConfig.WAVE_PAUSE_SECONDS
        state.waveAnnouncementText = "GET READY"
        state.waveAnnouncementTimer = GameConfig.WAVE_PAUSE_SECONDS
        state.score += 50
    }

    /**
     * Spawn a regular enemy from a random screen edge.
     */
    private fun spawnEnemy() {
        val pool = chapter.getActivePool(state.elapsedSeconds)
        val enemyType = pickWeighted(pool)

        // Spawn at random screen edge
        val edge = Random.nextInt(4)
        val (x, y) = when (edge) {
            0 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to -30f
            1 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to GameConfig.WORLD_HEIGHT + 30f
            2 -> -30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT
            else -> GameConfig.WORLD_WIDTH + 30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT
        }

        // HP scaling: base time scaling + wave scaling
        val minutes = state.elapsedSeconds / 60f
        val hpScale = (1f + minutes * GameConfig.ENEMY_HP_SCALE) *
            (1f + state.currentWave * GameConfig.WAVE_HP_SCALE_PER_WAVE)

        state.spawnEnemy(x, y, enemyType, hpScale = hpScale)
        totalSpawned++
    }

    /**
     * Spawn a boss for the current boss wave.
     */
    private fun spawnBossForWave() {
        bossActive = true
        val bossIndex = (state.currentWave / GameConfig.BOSS_WAVE_INTERVAL) - 1
        val bossHp = chapter.bossHp * (1f + bossIndex * 0.5f)
        state.spawnEnemy(
            x = GameConfig.WORLD_WIDTH / 2f,
            y = 100f,
            enemyType = chapter.bossType,
            hpScale = bossHp / 4000f
        )
        totalSpawned++
    }

    /**
     * Check if the boss is dead.
     */
    private fun isBossDead(): Boolean {
        for (i in state.enemies.indices) {
            if (i >= state.tags.size) break
            if (state.tags[i].tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (state.enemies[i].type == chapter.bossType && !state.healths[i].isDead) {
                return false
            }
        }
        return true
    }

    /**
     * Pick a weighted random enemy type.
     */
    private fun pickWeighted(pool: Map<EnemyComponent.EnemyData, Int>): EnemyComponent.EnemyData {
        val total = pool.values.sum()
        var roll = Random.nextInt(total)
        for ((type, weight) in pool) {
            roll -= weight
            if (roll < 0) return type
        }
        return EnemyComponent.EnemyData.ZOMBIE
    }
}
