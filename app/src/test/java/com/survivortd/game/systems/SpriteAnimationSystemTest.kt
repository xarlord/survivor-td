package com.survivortd.game.systems

import com.survivortd.game.components.SpriteComponent
import com.survivortd.game.components.VelocityComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.core.SpriteManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for SpriteAnimationSystem — frame advancement, state transitions,
 * facing direction, and dead entity skipping. [#118]
 */
class SpriteAnimationSystemTest {

    private lateinit var gameState: GameState
    private lateinit var system: SpriteAnimationSystem

    @BeforeEach
    fun setUp() {
        gameState = GameState()
        system = SpriteAnimationSystem(gameState)
    }

    @Test
    @DisplayName("should advance frame when timer exceeds duration")
    fun shouldAdvanceFrameWhenTimerExceedsDuration() {
        val id = gameState.spawnPlayer()

        // Verify initial state
        val sprite = gameState.sprites[id]
        assertNotNull(sprite)
        assertEquals(0, sprite.frameIndex)
        assertEquals(0f, sprite.animTimer)

        // Advance by one frame duration
        system.update(sprite.frameDuration)

        assertEquals(1, sprite.frameIndex)
        assertTrue(sprite.animTimer < sprite.frameDuration)
    }

    @Test
    @DisplayName("should wrap frame index when exceeding frame count")
    fun shouldWrapFrameIndex() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        val totalFrames = 4

        // Advance through all frames
        for (frame in 0 until totalFrames) {
            system.update(sprite.frameDuration)
        }

        // Should wrap back to 0
        assertEquals(0, sprite.frameIndex)
    }

    @Test
    @DisplayName("should skip entities without sprites (atlasId == -1)")
    fun shouldSkipEntitiesWithoutSprites() {
        // Manually create an entity without adding a sprite
        val id = gameState.nextEntityId()
        gameState.positions.add(com.survivortd.game.components.PositionComponent())
        gameState.velocities.add(VelocityComponent())
        gameState.renders.add(com.survivortd.game.components.RenderComponent())
        gameState.healths.add(com.survivortd.game.components.HealthComponent())
        gameState.enemies.add(com.survivortd.game.components.EnemyComponent())
        gameState.players.add(com.survivortd.game.components.PlayerComponent())
        gameState.projectiles.add(com.survivortd.game.components.ProjectileComponent())
        gameState.pickups.add(com.survivortd.game.components.PickupComponent())
        gameState.towers.add(com.survivortd.game.components.TowerComponent())
        gameState.statusEffects.add(com.survivortd.game.components.StatusEffectsComponent())
        gameState.tags.add(com.survivortd.game.components.TagComponent(
            com.survivortd.game.components.TagComponent.EntityTag.ENEMY
        ))
        // Add sprite with no atlas — should be skipped
        gameState.sprites.add(SpriteComponent(atlasId = -1))

        // Should not crash
        system.update(1f)

        assertEquals(-1, gameState.sprites[id].atlasId)
        assertEquals(0, gameState.sprites[id].frameIndex)
    }

    @Test
    @DisplayName("should skip static frames (frameDuration == 0)")
    fun shouldSkipStaticFrames() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        sprite.frameDuration = 0f

        // Advance time — should not change frame
        system.update(1f)

        assertEquals(0, sprite.frameIndex)
    }

    @Test
    @DisplayName("should skip single-frame animations")
    fun shouldSkipSingleFrameAnimations() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        sprite.frameCount = 1

        // Advance time — should not change frame
        system.update(10f)

        assertEquals(0, sprite.frameIndex)
    }

    @Test
    @DisplayName("should skip dead entities")
    fun shouldSkipDeadEntities() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        val initialFrame = sprite.frameIndex

        // Kill the entity
        gameState.healths[id].currentHp = 0f

        system.update(1f)

        assertEquals(initialFrame, sprite.frameIndex)
    }

    @Test
    @DisplayName("should set facingLeft when velocity is negative")
    fun shouldSetFacingLeftWhenVelocityNegative() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]

        assertFalse(sprite.facingLeft)

        // Set velocity to the left
        gameState.velocities[id].x = -10f
        system.update(0f)

        assertTrue(sprite.facingLeft)
    }

    @Test
    @DisplayName("should clear facingLeft when velocity is positive")
    fun shouldClearFacingLeftWhenVelocityPositive() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]

        sprite.facingLeft = true
        gameState.velocities[id].x = 10f
        system.update(0f)

        assertFalse(sprite.facingLeft)
    }

    @Test
    @DisplayName("should not change facing for near-zero velocity")
    fun shouldNotChangeFacingForNearZeroVelocity() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        sprite.facingLeft = true

        gameState.velocities[id].x = 0.5f
        system.update(0f)

        assertTrue(sprite.facingLeft)
    }

    @Test
    @DisplayName("should transition to walk anim when player moves")
    fun shouldTransitionToWalkWhenPlayerMoves() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]

        assertEquals(SpriteManager.ANIM_IDLE, sprite.animState)

        // Set velocity to trigger movement
        gameState.velocities[id].x = 10f
        system.update(0f)

        assertEquals(SpriteManager.ANIM_WALK, sprite.animState)
        assertEquals(0, sprite.frameIndex) // Reset on state change
    }

    @Test
    @DisplayName("should transition back to idle when player stops")
    fun shouldTransitionToIdleWhenPlayerStops() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]

        // First move
        gameState.velocities[id].x = 100f
        system.update(0f)
        assertEquals(SpriteManager.ANIM_WALK, sprite.animState)

        // Then stop
        gameState.velocities[id].x = 0f
        gameState.velocities[id].y = 0f
        system.update(0f)
        assertEquals(SpriteManager.ANIM_IDLE, sprite.animState)
    }

    @Test
    @DisplayName("should update multiple entities in parallel")
    fun shouldUpdateMultipleEntities() {
        val id1 = gameState.spawnPlayer()
        val id2 = gameState.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)

        system.update(0.2f)

        // Both should have advanced
        assertTrue(gameState.sprites[id1].frameIndex >= 1)
        assertTrue(gameState.sprites[id2].frameIndex >= 1)
    }

    @Test
    @DisplayName("should correctly handle partial timer accumulation")
    fun shouldHandlePartialTimerAccumulation() {
        val id = gameState.spawnPlayer()
        val sprite = gameState.sprites[id]
        val duration = sprite.frameDuration

        // Two partial updates that together exceed duration
        system.update(duration * 0.6f) // Timer = 0.6 * duration
        assertEquals(0, sprite.frameIndex)
        assertTrue(sprite.animTimer > 0f)

        system.update(duration * 0.6f) // Timer = 1.2 * duration → should advance
        assertEquals(1, sprite.frameIndex)
    }
}
