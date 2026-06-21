package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
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
     * Enemy movement: integrate velocity → position.
     * Velocity is set by EnemyAISystem; this system just applies it.
     */
    private fun updateEnemies(dt: Float) {
        if (state.playerIndex < 0 || state.playerIndex >= state.positions.size) return

        for (i in state.enemies.indices) {
            if (i >= state.positions.size) break
            if (i >= state.velocities.size) break
            if (i >= state.tags.size) break
            if (state.tags[i].tag != com.survivortd.game.components.TagComponent.EntityTag.ENEMY) continue
            if (state.healths[i].isDead) continue

            val pos = state.positions[i]
            val vel = state.velocities[i]
            val enemy = state.enemies[i]

            // Apply slow effect from frost towers
            val speedMult = if (enemy.slowTimer > 0f) {
                enemy.slowTimer -= dt
                (1f - enemy.slowMagnitude).coerceAtLeast(0.1f)
            } else {
                1f
            }

            pos.x += vel.x * speedMult * dt
            pos.y += vel.y * speedMult * dt

            // Clamp to world bounds (enemies can't leave the arena)
            pos.x = pos.x.coerceIn(-100f, GameConfig.WORLD_WIDTH + 100f)
            pos.y = pos.y.coerceIn(-100f, GameConfig.WORLD_HEIGHT + 100f)
        }
    }
}
