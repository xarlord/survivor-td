package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.GameConfig
import com.survivortd.game.core.GameState
import kotlin.math.sqrt

/**
 * Movement system — player (analog joystick) + enemies (AI velocity integrate).
 *
 * Player speed scales with stick magnitude (0..1). Dash is a one-shot from
 * [VirtualJoystick.consumeDashRequest], never polled every frame incorrectly.
 */
class MovementSystem(
    private val state: GameState,
    private val joystick: VirtualJoystick? = null
) {
    fun update(dt: Float) {
        if (state.isPaused || state.isGameOver) return

        if (state.playerIndex >= 0 && state.playerIndex < state.positions.size) {
            updatePlayer(dt)
        }
        updateEnemies(dt)
    }

    private fun updatePlayer(dt: Float) {
        val pos = state.positions[state.playerIndex]
        val player = state.players.getOrElse(state.playerIndex) { return }

        // One-shot dash from double-tap. The request carries the last valid
        // stick direction because a second touch-down starts at the center.
        val dashRequest = joystick?.consumeDashRequest()
        if (dashRequest != null &&
            !player.isDashing &&
            player.dashCooldownTimer <= 0f
        ) {
            player.isDashing = true
            player.dashTimer = GameConfig.PLAYER_DASH_DURATION
            player.dashCooldownTimer = GameConfig.PLAYER_DASH_COOLDOWN
            player.dashDirectionX = dashRequest.directionX
            player.dashDirectionY = dashRequest.directionY
        }

        val jx = state.joystickX
        val jy = state.joystickY
        val mag = sqrt(jx * jx + jy * jy).coerceIn(0f, 1f)

        val (nx, ny, speed) = if (player.isDashing) {
            // A dash stays on its initial vector for its full duration.
            Triple(player.dashDirectionX, player.dashDirectionY, GameConfig.PLAYER_DASH_SPEED)
        } else if (mag > 0.001f) {
            val dirX = jx / mag
            val dirY = jy / mag
            Triple(dirX, dirY, player.moveSpeed * mag)
        } else {
            Triple(0f, 0f, 0f)
        }

        pos.x += nx * speed * dt
        pos.y += ny * speed * dt

        pos.x = pos.x.coerceIn(0f, GameConfig.WORLD_WIDTH)
        pos.y = pos.y.coerceIn(0f, GameConfig.WORLD_HEIGHT)

        state.cameraX = pos.x
        state.cameraY = pos.y

        if (player.isDashing) {
            player.dashTimer -= dt
            if (player.dashTimer <= 0f) {
                player.isDashing = false
            }
        } else {
            player.dashCooldownTimer = (player.dashCooldownTimer - dt).coerceAtLeast(0f)
        }

        if (state.playerIndex < state.healths.size) {
            state.healthPercent = state.healths[state.playerIndex].hpPercent
        }
    }

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

            val speedMult = if (enemy.slowTimer > 0f) {
                enemy.slowTimer -= dt
                (1f - enemy.slowMagnitude).coerceAtLeast(0.1f)
            } else {
                1f
            }

            pos.x += vel.x * speedMult * dt
            pos.y += vel.y * speedMult * dt

            pos.x = pos.x.coerceIn(-100f, GameConfig.WORLD_WIDTH + 100f)
            pos.y = pos.y.coerceIn(-100f, GameConfig.WORLD_HEIGHT + 100f)
        }
    }
}
