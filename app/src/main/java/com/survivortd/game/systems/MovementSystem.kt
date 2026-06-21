package com.survivortd.game.systems

import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import com.survivortd.game.core.InputType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Movement system — processes player and enemy movement.
 *
 * Player: follows virtual joystick input (normalized vector * speed).
 * Enemies: chase the player (seek steering behavior).
 */
class MovementSystem(
    private val state: GameState
) {
    /**
     * Called every physics tick.
     */
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        // Update player movement from joystick
        if (state.playerIndex >= 0 && state.playerIndex < state.positions.size) {
            updatePlayer(dt)
        }

        // Update enemy movement (chase player)
        updateEnemies(dt)

        // Update elapsed time
        state.elapsedSeconds += dt
        state.currentTick++
    }

    /**
     * Player movement: joystick vector * speed, clamped to world bounds.
     */
    private fun updatePlayer(dt: Float) {
        val pos = state.positions[state.playerIndex]
        val player = state.players.getOrElse(state.playerIndex) { return }

        val jx = state.joystickX
        val jy = state.joystickY

        // Normalize joystick vector
        val mag = sqrt(jx * jx + jy * jy)
        val nx = if (mag > 0.01f) jx / mag else 0f
        val ny = if (mag > 0.01f) jy / mag else 0f

        val speed = if (player.isDashing) GameConfig.PLAYER_DASH_SPEED else player.moveSpeed
        pos.x += nx * speed * dt
        pos.y += ny * speed * dt

        // Clamp to world bounds
        pos.x = pos.x.coerceIn(0f, GameConfig.WORLD_WIDTH)
        pos.y = pos.y.coerceIn(0f, GameConfig.WORLD_HEIGHT)

        // Update camera to follow player
        state.cameraX = pos.x
        state.cameraY = pos.y

        // Update dash state
        if (player.isDashing) {
            player.dashTimer -= dt
            if (player.dashTimer <= 0f) {
                player.isDashing = false
            }
        } else {
            player.dashCooldown = (player.dashCooldown - dt).coerceAtLeast(0f)
        }

        // Update health percent for HUD
        if (state.playerIndex < state.healths.size) {
            state.healthPercent = state.healths[state.playerIndex].hpPercent
        }
    }

    /**
     * Enemy movement: steer towards player.
     */
    private fun updateEnemies(dt: Float) {
        if (state.playerIndex < 0 || state.playerIndex >= state.positions.size) return
        val playerPos = state.positions[state.playerIndex]

        for (i in state.enemies.indices) {
            if (i >= state.positions.size) break
            val pos = state.positions[i]
            val vel = state.velocities.getOrElse(i) { continue }
            val enemy = state.enemies[i]

            // Direction towards player
            val dx = playerPos.x - pos.x
            val dy = playerPos.y - pos.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 1f) {
                val speed = when (enemy.type) {
                    EnemyData.ZOMBIE -> 80f
                    EnemyData.RUNNER -> 160f
                    EnemyData.BRUTE -> 60f
                    EnemyData.SPIDER -> 50f
                    EnemyData.BOMBER -> 100f
                    EnemyData.HEALER -> 70f
                    EnemyData.SHIELDER -> 80f
                    EnemyData.FLYER -> 120f
                    EnemyData.ELITE -> 96f
                    EnemyData.BOSS -> 50f
                }
                pos.x += (dx / dist) * speed * dt
                pos.y += (dy / dist) * speed * dt
            }
        }
    }

    // Re-import the enum for convenience
    private typealias EnemyData = com.survivortd.game.components.EnemyComponent.EnemyData
}
