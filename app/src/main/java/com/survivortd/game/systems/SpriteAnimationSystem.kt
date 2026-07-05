package com.survivortd.game.systems

import com.survivortd.game.components.SpriteComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.core.SpriteManager

/**
 * Advances sprite animation frames based on delta time.
 *
 * Design (AGY review #118): No string allocation in hot loop.
 * Frame lookup uses integer atlasId + animState + frameIndex directly.
 * Static frames (frameDuration == 0f) are skipped immediately.
 */
class SpriteAnimationSystem(private val gameState: GameState) {

    /**
     * Update all entity animations.
     * Called once per physics tick (60Hz).
     *
     * @param dt Delta time in seconds since last tick.
     */
    fun update(dt: Float) {
        val sprites = gameState.sprites
        val velocities = gameState.velocities
        val tags = gameState.tags
        val healths = gameState.healths
        val size = sprites.size

        for (i in 0 until size) {
            if (i >= healths.size || healths[i].isDead) continue

            val sprite = sprites[i]
            // Skip entities without sprites or with static frames
            if (!sprite.hasSprite || sprite.frameDuration <= 0f) continue
            if (sprite.frameCount <= 1) continue

            // Determine facing direction from velocity.
            // Only update when magnitude exceeds dead-zone to avoid flicker.
            if (i < velocities.size) {
                val vx = velocities[i].x
                if (vx < -1f) {
                    sprite.facingLeft = true
                } else if (vx > 1f) {
                    sprite.facingLeft = false
                }
                // Within [-1, 1]: preserve current facing
            }

            // Walking detection: switch to walk anim when moving
            if (i < velocities.size && i < tags.size) {
                val vx = velocities[i].x
                val vy = velocities[i].y
                val isMoving = (vx * vx + vy * vy) > 4f // threshold: > 2 px/s
                val tag = tags[i].tag

                if (tag == com.survivortd.game.components.TagComponent.EntityTag.PLAYER ||
                    tag == com.survivortd.game.components.TagComponent.EntityTag.ENEMY
                ) {
                    val desiredAnim = if (isMoving) SpriteManager.ANIM_WALK else SpriteManager.ANIM_IDLE
                    if (sprite.animState != desiredAnim) {
                        sprite.animState = desiredAnim
                        sprite.frameIndex = 0
                        sprite.animTimer = 0f
                        // Update frame count from sprite manager if available
                        val anim = gameState.spriteManager?.getAnim(sprite.atlasId, desiredAnim)
                        if (anim != null && anim.frameCount > 0) {
                            sprite.frameCount = anim.frameCount
                            sprite.frameDuration = anim.frameDuration
                        }
                    }
                }
            }

            // Advance animation timer
            sprite.animTimer += dt

            if (sprite.animTimer >= sprite.frameDuration) {
                sprite.animTimer -= sprite.frameDuration
                sprite.frameIndex = (sprite.frameIndex + 1) % sprite.frameCount
            }
        }
    }
}
