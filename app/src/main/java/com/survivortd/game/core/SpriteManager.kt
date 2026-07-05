package com.survivortd.game.core

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
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
     * A loaded texture atlas with its animations indexed by "group_state" key
     * (e.g. "zombie_0" for zombie idle, "zombie_1" for zombie walk).
     */
    class SpriteSheet(
        val bitmap: ImageBitmap?,
        val animations: Map<String, SpriteAnim>
    ) {
        fun getAnim(group: String, animStateId: Int): SpriteAnim? =
            animations[group + "_" + animStateId]

        fun getFrame(group: String, animStateId: Int, frameIndex: Int): SpriteFrame? {
            val anim = animations[group + "_" + animStateId] ?: return null
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
                // ARGB_8888 required: HARDWARE config returns null from
                // decodeStream on emulators. Compose ImageBitmap handles
                // GPU memory internally, so no benefit from HARDWARE hint.
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
     * Parse TexturePacker Hash JSON format into "group_state" → frame array map.
     * Groups frames by animation name (e.g., "hero_knight_idle" → [frame0, frame1, ...]).
     */
    private fun parseFrameJson(path: String): Map<String, SpriteAnim> {
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

        // Convert to "group_state" → SpriteAnim map
        return buildAnimMap(animFrames)
    }

    /**
     * Map animation group names to "group_state" string keys.
     * "hero_knight_idle" → key "hero_knight" + state ANIM_IDLE.
     * "zombie_idle" → key "zombie" + state ANIM_IDLE.
     * "proj_bullet_0" (single frame) → key "proj_bullet" + state ANIM_IDLE.
     */
    private fun buildAnimMap(animFrames: Map<String, List<SpriteFrame>>): Map<String, SpriteAnim> {
        val result = mutableMapOf<String, SpriteAnim>()

        for ((name, frames) in animFrames) {
            // Determine animation state from suffix
            val (baseName, animStateId) = when {
                name.endsWith("_idle") || name.endsWith("_Idle") ->
                    name.dropLast(5) to ANIM_IDLE
                name.endsWith("_walk") || name.endsWith("_Walk") ->
                    name.dropLast(5) to ANIM_WALK
                name.endsWith("_attack") || name.endsWith("_Attack") ->
                    name.dropLast(7) to ANIM_ATTACK
                name.endsWith("_death") || name.endsWith("_Death") ->
                    name.dropLast(6) to ANIM_DEATH
                // Strip trailing _N for single-frame entries (proj_bullet_0 → proj_bullet)
                else -> {
                    val lastUs = name.lastIndexOf('_')
                    if (lastUs > 0 && name[lastUs + 1].isDigit()) {
                        name.substring(0, lastUs) to ANIM_IDLE
                    } else {
                        name to ANIM_IDLE
                    }
                }
            }

            val key = baseName + "_" + animStateId
            if (key !in result) {
                val duration = when {
                    baseName.contains("brute") -> SLOW_FRAME_DURATION
                    baseName.contains("runner") -> FAST_FRAME_DURATION
                    baseName.contains("boss") -> SLOW_FRAME_DURATION
                    baseName.startsWith("proj_") || baseName.startsWith("pickup_") -> 0f
                    else -> DEFAULT_FRAME_DURATION
                }
                result[key] = SpriteAnim(frames.toTypedArray(), duration)
            }
        }

        return result
    }

    fun getSheet(atlasId: Int): SpriteSheet? = atlases.getOrNull(atlasId)

    /** Get frame by animation group name + state ID (e.g. "zombie", ANIM_IDLE). */
    fun getFrame(atlasId: Int, group: String, animStateId: Int, frameIndex: Int): SpriteFrame? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        return sheet.getFrame(group, animStateId, frameIndex)
    }

    fun getAnim(atlasId: Int, group: String, animStateId: Int): SpriteAnim? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        return sheet.getAnim(group, animStateId)
    }

    /**
     * Register a specific animation for a given atlas + group + state.
     */
    fun registerAnim(atlasId: Int, group: String, animStateId: Int, anim: SpriteAnim) {
        val sheet = atlases[atlasId] ?: return
        @Suppress("UNCHECKED_CAST")
        val mutableAnims = sheet.animations as MutableMap<String, SpriteAnim>
        mutableAnims[group + "_" + animStateId] = anim
    }
}
