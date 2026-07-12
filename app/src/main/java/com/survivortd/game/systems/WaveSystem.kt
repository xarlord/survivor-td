package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.ChapterConfig
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.random.Random

/**
 * Wave System — GDD §8 continuous spawning + timed bosses + build phases.
 *
 * - Continuous edge spawning (no discrete wave quotas)
 * - Spawn interval scales with minutes elapsed
 * - Bosses at minutes 5 / 10 / 15 (GameConfig.BOSS_TIMES_MINUTES)
 * - Normal spawns pause briefly while boss is active
 * - After boss death: 10s build phase (GameConfig.BUILD_PHASE_DURATION)
 *
 * [currentWave] is a display/meta counter: increments at each boss or each minute
 * for HUD compatibility (WAVE N). Starts at 1 on first update.
 */
class WaveSystem(
    private val state: GameState,
    private val chapter: ChapterConfig = ChapterConfig.WASTELAND
) {
    private var spawnTimer = 0f
    private var spawnInterval = GameConfig.BASE_SPAWN_INTERVAL

    private var bossActive = false
    private var bossPauseTimer = 0f
    private var buildPhaseTimer = 0f
    private val bossesTriggered = mutableSetOf<Int>()

    private var started = false
    private var spawnedThisTick = 0

    var isBuildPhase = false
        private set

    /** Total enemies spawned this match */
    var totalSpawned = 0
        private set

    /**
     * Compatibility: previous discrete-wave API. In continuous mode this starts
     * continuous combat if not already running and may force a boss if [state.currentWave]
     * is a multiple of BOSS_WAVE_INTERVAL (tests). Prefer update() driven flow.
     */
    /**
     * Compat API from discrete-wave era: increments [currentWave] then configures.
     * Boss if currentWave % BOSS_WAVE_INTERVAL == 0.
     */
    fun startNextWave() {
        started = true
        state.currentWave++
        state.wavePaused = false
        state.wavePauseTimer = 0f
        state.isBossWave = state.currentWave > 0 &&
            state.currentWave % GameConfig.BOSS_WAVE_INTERVAL == 0

        if (state.isBossWave) {
            state.waveAnnouncementText = "⚠ BOSS INCOMING ⚠"
            state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
            state.waveEnemiesRemaining = 1
            if (!bossActive) {
                spawnBossForMinute(state.currentWave)
            }
        } else {
            state.waveAnnouncementText = "WAVE ${state.currentWave}"
            state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
            state.waveEnemiesRemaining = GameConfig.WAVE_ENEMY_BASE_COUNT +
                (state.currentWave - 1).coerceAtLeast(0) * GameConfig.WAVE_ENEMY_SCALE_PER_WAVE
            spawnTimer = 0f
            bossActive = false
        }
    }

    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return
        spawnedThisTick = 0

        if (state.waveAnnouncementTimer > 0f) {
            state.waveAnnouncementTimer -= dt
        }

        // First tick: enter combat
        if (!started) {
            started = true
            state.currentWave = 1
            state.waveAnnouncementText = "WAVE 1"
            state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
            state.waveEnemiesRemaining = GameConfig.WAVE_ENEMY_BASE_COUNT
            spawnTimer = 0f
            return
        }

        // Build phase after boss (GDD §7)
        if (isBuildPhase) {
            buildPhaseTimer -= dt
            state.waveAnnouncementText = "BUILD PHASE"
            if (buildPhaseTimer <= 0f) {
                isBuildPhase = false
                state.waveAnnouncementText = "GET READY"
                state.waveAnnouncementTimer = GameConfig.WAVE_PAUSE_SECONDS
            }
            return
        }

        // Boss alive: pause normal spawns
        if (bossActive) {
            if (isBossDead()) {
                bossActive = false
                state.isBossWave = false
                buildPhaseTimer = GameConfig.BUILD_PHASE_DURATION
                isBuildPhase = true
                state.waveEnemiesRemaining = 0
                state.waveAnnouncementText = "BOSS DEFEATED!"
                state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
            }
            return
        }

        // Brief ready after build / intermission flag
        if (state.wavePaused) {
            state.wavePauseTimer -= dt
            if (state.wavePauseTimer <= 0f) {
                state.wavePaused = false
            }
            return
        }

        // Minute-based boss triggers (GDD §8.3)
        val minutes = state.elapsedSeconds / 60f
        for (bossMinute in GameConfig.BOSS_TIMES_MINUTES) {
            if (minutes >= bossMinute && bossesTriggered.add(bossMinute)) {
                state.currentWave = bossMinute
                state.isBossWave = true
                state.waveAnnouncementText = "⚠ BOSS INCOMING ⚠"
                state.waveAnnouncementTimer = GameConfig.WAVE_ANNOUNCEMENT_DURATION
                state.waveEnemiesRemaining = 1
                spawnBossForMinute(bossMinute)
                return
            }
        }

        // Continuous spawn (GDD §8.5)
        // spawnInterval = max(0.3, base - minutes * 0.05)  OR rate scale
        val scaled = GameConfig.BASE_SPAWN_INTERVAL /
            (1f + minutes * GameConfig.ENEMY_SPAWN_RATE_SCALE).coerceAtLeast(0.01f)
        spawnInterval = scaled.coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)

        spawnTimer += dt
        while (spawnTimer >= spawnInterval && spawnedThisTick < GameConfig.MAX_SPAWN_PER_FRAME) {
            spawnTimer -= spawnInterval
            spawnEnemy()
            spawnedThisTick++
            if (state.waveEnemiesRemaining > 0) {
                // Soft counter for HUD; continuous mode doesn't hard-stop on 0
            }
        }

        // Display wave number ≈ floor(minutes)+1 when not in boss minute
        if (!state.isBossWave) {
            val displayWave = (minutes.toInt() + 1).coerceAtLeast(1)
            if (displayWave > state.currentWave && displayWave !in GameConfig.BOSS_TIMES_MINUTES) {
                state.currentWave = displayWave
            }
        }
    }

    fun onEnemyKilled() {
        if (state.waveEnemiesRemaining > 0) {
            state.waveEnemiesRemaining--
        }
    }

    private fun spawnEnemy() {
        val pool = chapter.getActivePool(state.elapsedSeconds)
        val enemyType = pickWeighted(pool)

        val edge = Random.nextInt(4)
        val (x, y) = when (edge) {
            0 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to -30f
            1 -> Random.nextFloat() * GameConfig.WORLD_WIDTH to GameConfig.WORLD_HEIGHT + 30f
            2 -> -30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT
            else -> GameConfig.WORLD_WIDTH + 30f to Random.nextFloat() * GameConfig.WORLD_HEIGHT
        }

        val minutes = state.elapsedSeconds / 60f
        val hpScale = 1f + minutes * GameConfig.ENEMY_HP_SCALE
        // Elite chance
        var type = enemyType
        val eliteChance = GameConfig.ELITE_BASE_CHANCE + minutes * GameConfig.ELITE_SCALE
        if (Random.nextFloat() < eliteChance && type != EnemyComponent.EnemyData.BOSS) {
            type = EnemyComponent.EnemyData.ELITE
        }

        state.spawnEnemy(x, y, type, hpScale = hpScale)
        totalSpawned++
    }

    private fun spawnBossForMinute(minuteOrWave: Int) {
        bossActive = true
        val bossIndex = (GameConfig.BOSS_TIMES_MINUTES.indexOf(minuteOrWave)
            .takeIf { it >= 0 } ?: ((minuteOrWave / GameConfig.BOSS_WAVE_INTERVAL) - 1).coerceAtLeast(0))
        val bossHp = chapter.bossHp * (1f + bossIndex * 0.5f)
        state.spawnEnemy(
            x = GameConfig.WORLD_WIDTH / 2f,
            y = 100f,
            enemyType = chapter.bossType,
            hpScale = bossHp / 4000f
        )
        totalSpawned++
    }

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

    private fun pickWeighted(pool: Map<EnemyComponent.EnemyData, Int>): EnemyComponent.EnemyData {
        val total = pool.values.sum()
        if (total <= 0) return EnemyComponent.EnemyData.ZOMBIE
        var roll = Random.nextInt(total)
        for ((type, weight) in pool) {
            roll -= weight
            if (roll < 0) return type
        }
        return EnemyComponent.EnemyData.ZOMBIE
    }
}
