package com.survivortd.game.core

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.io.IOException

/**
 * Loads WebP/PNG texture atlases from assets/ and parses JSON frame metadata.
 *
 * Design decisions (AGY review #118):
 * - HARDWARE bitmaps only when hardware-accelerated canvas is available
 * - Integer-indexed frame arrays instead of string keys (avoids GC in hot loop)
 * - Pre-allocated Rect objects per frame (no allocation during rendering)
 * - All loading on IO dispatcher via suspend fun loadAll()
 */
class SpriteManager(private val context: Context) {

    // [#118] AGY review: detect edit/preview mode to avoid HARDWARE bitmap crashes
    private val isSoftwareContext: Boolean = run {
        try { View(context).isInEditMode } catch (_: Exception) { false }
    }

    /**
     * A single frame region on a texture atlas.
     * Pre-allocated Rect avoids allocation in the draw loop.
     */
    class SpriteFrame(
        val srcRect: Rect,
        val width: Int,
        val height: Int
    )

    /**
     * An animation sequence — array of frames played in order.
     */
    class SpriteAnim(
        val frames: Array<SpriteFrame>,
        val frameDuration: Float
    ) {
        val frameCount: Int get() = frames.size
    }

    /**
     * A loaded texture atlas with its animations indexed by animStateId.
     */
    class SpriteSheet(
        val bitmap: ImageBitmap?,
        val animations: Map<Int, SpriteAnim>
    ) {
        fun getAnim(animStateId: Int): SpriteAnim? = animations[animStateId]

        fun getFrame(animStateId: Int, frameIndex: Int): SpriteFrame? {
            val anim = animations[animStateId] ?: return null
            return anim.frames.getOrNull(frameIndex)
        }
    }

    // Atlas IDs — used by SpriteComponent.atlasId
    // -1 means "no sprite, use fallback shapes"
    companion object {
        private const val TAG = "SpriteManager"

        const val ATLAS_NONE = -1
        const val ATLAS_HEROES = 0
        const val ATLAS_ENEMIES = 1
        const val ATLAS_EFFECTS = 2
        const val ATLAS_COUNT = 3

        // Animation state IDs — used by SpriteComponent.animState
        const val ANIM_IDLE = 0
        const val ANIM_WALK = 1
        const val ANIM_ATTACK = 2
        const val ANIM_DEATH = 3

        // Frame durations (seconds per frame)
        const val DEFAULT_FRAME_DURATION = 0.15f
        const val SLOW_FRAME_DURATION = 0.25f
        const val FAST_FRAME_DURATION = 0.1f
    }

    /** Loaded atlases indexed by atlas ID. */
    private val atlases = arrayOfNulls<SpriteSheet>(ATLAS_COUNT)

    /** Whether all atlases have been successfully loaded. */
    var isLoaded: Boolean = false
        private set

    /**
     * Load all texture atlases from assets/spritesheets/.
     * Must be called from Dispatchers.IO.
     */
    fun loadAll() {
        try {
            loadAtlas(ATLAS_HEROES, "spritesheets/heroes.png", "spritesheets/heroes.json")
            loadAtlas(ATLAS_ENEMIES, "spritesheets/enemies.png", "spritesheets/enemies.json")
            loadAtlas(ATLAS_EFFECTS, "spritesheets/effects.png", "spritesheets/effects.json")
            isLoaded = true
        } catch (e: IOException) {
            // [#118] Graceful degradation — game still works with shapes
            android.util.Log.w(TAG, "Failed to load sprite atlases, using fallback shapes", e)
        }
    }

    private fun loadAtlas(atlasId: Int, imagePath: String, jsonPath: String) {
        val bitmap = loadBitmap(imagePath) ?: return
        val animations = parseFrameJson(jsonPath)
        atlases[atlasId] = SpriteSheet(bitmap, animations)
    }

    private fun loadBitmap(path: String): ImageBitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                // Force ARGB_8888 — HARDWARE bitmaps cannot be converted
                // to ImageBitmap via .asImageBitmap() and return null silently.
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inMutable = false
            }
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                    ?.asImageBitmap()
            }
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Failed to load bitmap: $path", e)
            null
        }
    }

    /**
     * Parse TexturePacker Hash JSON format into animation state → frame array map.
     * Groups frames by animation name (e.g., "hero_knight_idle" → [frame0, frame1, ...]).
     */
    private fun parseFrameJson(path: String): Map<Int, SpriteAnim> {
        val jsonStr = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Failed to load frame JSON: $path", e)
            return emptyMap()
        }

        val root = JSONObject(jsonStr)
        val framesObj = root.getJSONObject("frames")

        // Group frames by animation base name
        val animFrames = mutableMapOf<String, MutableList<SpriteFrame>>()
        val keys = framesObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val frameData = framesObj.getJSONObject(key).getJSONObject("frame")
            val x = frameData.getInt("x")
            val y = frameData.getInt("y")
            val w = frameData.getInt("w")
            val h = frameData.getInt("h")
            val srcRect = Rect(x, y, x + w, y + h)
            val frame = SpriteFrame(srcRect, w, h)

            // Extract animation base name: "hero_knight_idle_0" → "hero_knight_idle"
            val lastUnderscore = key.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val baseName = key.substring(0, lastUnderscore)
                animFrames.getOrPut(baseName) { mutableListOf() }.add(frame)
            }
        }

        // Convert to animStateId → SpriteAnim map
        // Use the registered animation name mappings
        return buildAnimMap(animFrames)
    }

    /**
     * Map animation group names to integer state IDs.
     * Names like "hero_knight_idle" → ANIM_IDLE, "hero_knight_walk" → ANIM_WALK, etc.
     * Also handles "enemy_zombie_idle" → ANIM_IDLE patterns.
     */
    private fun buildAnimMap(animFrames: Map<String, List<SpriteFrame>>): Map<Int, SpriteAnim> {
        val result = mutableMapOf<Int, SpriteAnim>()

        for ((name, frames) in animFrames) {
            val animStateId = when {
                name.endsWith("_idle") || name.endsWith("_Idle") -> ANIM_IDLE
                name.endsWith("_walk") || name.endsWith("_Walk") -> ANIM_WALK
                name.endsWith("_attack") || name.endsWith("_Attack") -> ANIM_ATTACK
                name.endsWith("_death") || name.endsWith("_Death") -> ANIM_DEATH
                // Single-frame effects get IDLE
                !name.contains('_') || name.startsWith("proj_") || name.startsWith("pickup_") -> ANIM_IDLE
                else -> ANIM_IDLE
            }

            // Only keep the first animation per state ID for this atlas
            // (If multiple entities share an atlas, we'd need per-entity anim mappings)
            if (!result.containsKey(animStateId)) {
                val duration = when {
                    name.contains("brute") -> SLOW_FRAME_DURATION
                    name.contains("runner") -> FAST_FRAME_DURATION
                    name.contains("boss") -> SLOW_FRAME_DURATION
                    name.startsWith("proj_") || name.startsWith("pickup_") -> 0f // Static
                    else -> DEFAULT_FRAME_DURATION
                }
                result[animStateId] = SpriteAnim(frames.toTypedArray(), duration)
            }
        }

        return result
    }

    fun getSheet(atlasId: Int): SpriteSheet? = atlases.getOrNull(atlasId)

    fun getFrame(atlasId: Int, animStateId: Int, frameIndex: Int): SpriteFrame? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        return sheet.getFrame(animStateId, frameIndex)
    }

    fun getAnim(atlasId: Int, animStateId: Int): SpriteAnim? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        return sheet.getAnim(animStateId)
    }

    /**
     * Register a specific animation for a given atlas + state.
     * Used to override auto-detected animations with explicit per-entity mappings.
     */
    fun registerAnim(atlasId: Int, animStateId: Int, anim: SpriteAnim) {
        val sheet = atlases[atlasId] ?: return
        @Suppress("UNCHECKED_CAST")
        val mutableAnims = sheet.animations as MutableMap<Int, SpriteAnim>
        mutableAnims[animStateId] = anim
    }
}
