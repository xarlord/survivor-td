package com.survivortd.game.core

import com.survivortd.game.components.SpriteComponent
import com.survivortd.game.core.GameState
import com.survivortd.game.core.SpriteManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for SpriteManager — atlas IDs, frame lookups, JSON parsing,
 * and graceful handling of missing assets. [#118]
 *
 * Note: Actual bitmap loading requires Android context (Robolectric),
 * so these tests focus on the data structures and index-based lookups.
 */
class SpriteManagerTest {

    @Test
    @DisplayName("atlas ID constants should be distinct non-negative values")
    fun atlasConstantsShouldBeDistinct() {
        assertTrue(SpriteManager.ATLAS_HEROES >= 0)
        assertTrue(SpriteManager.ATLAS_ENEMIES >= 0)
        assertTrue(SpriteManager.ATLAS_EFFECTS >= 0)
        assertNotEquals(SpriteManager.ATLAS_HEROES, SpriteManager.ATLAS_ENEMIES)
        assertNotEquals(SpriteManager.ATLAS_HEROES, SpriteManager.ATLAS_EFFECTS)
        assertNotEquals(SpriteManager.ATLAS_ENEMIES, SpriteManager.ATLAS_EFFECTS)
    }

    @Test
    @DisplayName("animation state constants should be distinct non-negative values")
    fun animStateConstantsShouldBeDistinct() {
        assertTrue(SpriteManager.ANIM_IDLE >= 0)
        assertTrue(SpriteManager.ANIM_WALK >= 0)
        assertTrue(SpriteManager.ANIM_ATTACK >= 0)
        assertTrue(SpriteManager.ANIM_DEATH >= 0)
        val states = setOf(
            SpriteManager.ANIM_IDLE,
            SpriteManager.ANIM_WALK,
            SpriteManager.ANIM_ATTACK,
            SpriteManager.ANIM_DEATH
        )
        assertEquals(4, states.size)
    }

    @Test
    @DisplayName("ATLAS_NONE should be -1 indicating no sprite")
    fun atlasNoneShouldBeNegative() {
        assertEquals(-1, SpriteManager.ATLAS_NONE)
    }

    @Test
    @DisplayName("SpriteComponent.hasSprite should be false for default")
    fun spriteComponentDefaultHasNoSprite() {
        val sprite = SpriteComponent()
        assertFalse(sprite.hasSprite)
        assertEquals(-1, sprite.atlasId)
    }

    @Test
    @DisplayName("SpriteComponent.hasSprite should be true when atlasId >= 0")
    fun spriteComponentHasSpriteWhenAtlasSet() {
        val sprite = SpriteComponent(atlasId = SpriteManager.ATLAS_HEROES)
        assertTrue(sprite.hasSprite)
    }

    @Test
    @DisplayName("SpriteFrame should store correct rect and dimensions")
    fun spriteFrameShouldStoreCorrectData() {
        val rect = android.graphics.Rect(10, 20, 74, 84) // x=10, y=20, w=64, h=64
        val frame = SpriteManager.SpriteFrame(rect, 64, 64)
        // Rect.equals() uses identity, compare fields
        assertEquals(rect.left, frame.srcRect.left)
        assertEquals(rect.top, frame.srcRect.top)
        assertEquals(rect.right, frame.srcRect.right)
        assertEquals(rect.bottom, frame.srcRect.bottom)
        assertEquals(64, frame.width)
        assertEquals(64, frame.height)
    }

    @Test
    @DisplayName("SpriteAnim should report correct frame count")
    fun spriteAnimShouldReportCorrectFrameCount() {
        val frames = arrayOf(
            SpriteManager.SpriteFrame(android.graphics.Rect(0, 0, 64, 64), 64, 64),
            SpriteManager.SpriteFrame(android.graphics.Rect(64, 0, 128, 64), 64, 64),
            SpriteManager.SpriteFrame(android.graphics.Rect(128, 0, 192, 64), 64, 64),
        )
        val anim = SpriteManager.SpriteAnim(frames, 0.15f)
        assertEquals(3, anim.frameCount)
    }

    @Test
    @DisplayName("SpriteSheet should return null for unknown anim group")
    fun spriteSheetShouldReturnNullForUnknownAnim() {
        val frame = SpriteManager.SpriteFrame(android.graphics.Rect(0, 0, 64, 64), 64, 64)
        val anim = SpriteManager.SpriteAnim(arrayOf(frame), 0.15f)
        // bitmap=null: tests only exercise animation lookup, never draw
        val sheet = SpriteManager.SpriteSheet(
            bitmap = null,
            animations = mapOf("zombie_0" to anim)
        )
        assertNull(sheet.getAnim("unknown", 99)) // Unknown group+state
        assertNull(sheet.getFrame("unknown", 99, 0)) // Unknown group+state
    }

    @Test
    @DisplayName("SpriteSheet should return frame by group, state and index")
    fun spriteSheetShouldReturnFrameByStateAndIndex() {
        val frame0 = SpriteManager.SpriteFrame(android.graphics.Rect(0, 0, 64, 64), 64, 64)
        val frame1 = SpriteManager.SpriteFrame(android.graphics.Rect(64, 0, 128, 64), 64, 64)
        val anim = SpriteManager.SpriteAnim(arrayOf(frame0, frame1), 0.15f)
        val sheet = SpriteManager.SpriteSheet(
            bitmap = null,
            animations = mapOf("hero_knight_0" to anim)
        )
        val result0 = sheet.getFrame("hero_knight", SpriteManager.ANIM_IDLE, 0)
        assertNotNull(result0)
        assertSame(frame0.srcRect, result0!!.srcRect)
        val result1 = sheet.getFrame("hero_knight", SpriteManager.ANIM_IDLE, 1)
        assertNotNull(result1)
        assertSame(frame1.srcRect, result1!!.srcRect)
        assertNull(sheet.getFrame("hero_knight", SpriteManager.ANIM_IDLE, 2)) // Out of bounds
    }

    @Test
    @DisplayName("GameState should include sprites array parallel to other arrays")
    fun gameStateShouldIncludeSpritesArray() {
        val state = GameState()
        assertEquals(0, state.sprites.size)
        assertEquals(0, state.positions.size)

        // Spawn player
        val id = state.spawnPlayer()

        // Sprites array should have same length as other arrays
        assertTrue(id >= 0)
        assertEquals(state.positions.size, state.sprites.size)
        assertEquals(state.renders.size, state.sprites.size)
        assertEquals(state.tags.size, state.sprites.size)
    }

    @Test
    @DisplayName("spawned player should have hero sprite component")
    fun spawnedPlayerShouldHaveHeroSprite() {
        val state = GameState()
        val id = state.spawnPlayer()
        val sprite = state.sprites[id]
        assertEquals(SpriteManager.ATLAS_HEROES, sprite.atlasId)
        assertEquals(SpriteManager.ANIM_IDLE, sprite.animState)
        assertEquals("hero_knight", sprite.animGroup)
        assertEquals(4, sprite.frameCount)
        assertTrue(sprite.frameDuration > 0f)
    }

    @Test
    @DisplayName("spawned enemy should have enemy sprite component with group")
    fun spawnedEnemyShouldHaveEnemySprite() {
        val state = GameState()
        val id = state.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)
        val sprite = state.sprites[id]
        assertEquals(SpriteManager.ATLAS_ENEMIES, sprite.atlasId)
        assertEquals(SpriteManager.ANIM_IDLE, sprite.animState)
        assertEquals("zombie", sprite.animGroup)
        assertEquals(4, sprite.frameCount)
    }

    @Test
    @DisplayName("spawned runner should have correct anim group and fast duration")
    fun spawnedRunnerShouldHaveFastAnim() {
        val state = GameState()
        val id = state.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.RUNNER)
        val sprite = state.sprites[id]
        assertEquals("runner", sprite.animGroup)
        assertEquals(SpriteManager.FAST_FRAME_DURATION, sprite.frameDuration)
    }

    @Test
    @DisplayName("spawned pickup should have effects sprite (static)")
    fun spawnedPickupShouldHaveEffectsSprite() {
        val state = GameState()
        val id = state.spawnPickup(0f, 0f, xpValue = 1)
        val sprite = state.sprites[id]
        assertEquals(SpriteManager.ATLAS_EFFECTS, sprite.atlasId)
        assertEquals("pickup_xp_small", sprite.animGroup)
        assertEquals(0f, sprite.frameDuration) // Static
        assertEquals(1, sprite.frameCount)
    }

    @Test
    @DisplayName("spawned projectile should have effects sprite (static)")
    fun spawnedProjectileShouldHaveEffectsSprite() {
        val state = GameState()
        val id = state.spawnProjectile(0f, 0f)
        val sprite = state.sprites[id]
        assertEquals(SpriteManager.ATLAS_EFFECTS, sprite.atlasId)
        assertEquals("proj_bullet", sprite.animGroup)
        assertEquals(0f, sprite.frameDuration) // Static
        assertEquals(1, sprite.frameCount)
    }

    @Test
    @DisplayName("cleanupDeadEntities should keep sprites array aligned")
    fun cleanupShouldKeepSpritesAligned() {
        val state = GameState()
        val playerId = state.spawnPlayer()
        val enemyId1 = state.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)
        val enemyId2 = state.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.RUNNER)

        // Kill one enemy
        state.healths[enemyId1].currentHp = 0f
        state.cleanupDeadEntities()

        // All arrays should still be the same length
        assertEquals(state.positions.size, state.sprites.size)
        assertEquals(state.renders.size, state.sprites.size)
        assertEquals(state.tags.size, state.sprites.size)
    }

    @Test
    @DisplayName("reset should clear sprites array")
    fun resetShouldClearSprites() {
        val state = GameState()
        state.spawnPlayer()
        state.spawnEnemy(0f, 0f, com.survivortd.game.components.EnemyComponent.EnemyData.ZOMBIE)

        assertTrue(state.sprites.isNotEmpty())

        state.reset()

        assertTrue(state.sprites.isEmpty())
        assertEquals(0, state.sprites.size)
    }

    @Test
    @DisplayName("spriteManager should start as null")
    fun spriteManagerShouldStartNull() {
        val state = GameState()
        assertNull(state.spriteManager)
    }
}
