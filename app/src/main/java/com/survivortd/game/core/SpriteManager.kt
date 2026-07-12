package com.survivortd.game.core

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.io.IOException

/**
 * Loads texture atlases from assets/ and parses JSON frame metadata.
 *
 * Issue #146: animations are keyed by (variantId, animState), not animState alone.
 * Previously only the first idle/walk in an atlas was kept — all enemies shared zombie frames.
 *
 * animKey = variantId * VARIANT_STRIDE + animState
 */
class SpriteManager(private val context: Context) {

    private val isSoftwareContext: Boolean = run {
        try { View(context).isInEditMode } catch (_: Exception) { false }
    }

    class SpriteFrame(
        val srcRect: Rect,
        val width: Int,
        val height: Int
    )

    class SpriteAnim(
        val frames: Array<SpriteFrame>,
        val frameDuration: Float
    ) {
        val frameCount: Int get() = frames.size
    }

    class SpriteSheet(
        val bitmap: ImageBitmap?,
        val animations: Map<Int, SpriteAnim>
    ) {
        fun getAnim(animKey: Int): SpriteAnim? = animations[animKey]

        fun getFrame(animKey: Int, frameIndex: Int): SpriteFrame? {
            val anim = animations[animKey] ?: return null
            return anim.frames.getOrNull(frameIndex)
        }
    }

    companion object {
        private const val TAG = "SpriteManager"

        const val ATLAS_NONE = -1
        const val ATLAS_HEROES = 0
        const val ATLAS_ENEMIES = 1
        const val ATLAS_EFFECTS = 2
        const val ATLAS_COUNT = 3

        const val ANIM_IDLE = 0
        const val ANIM_WALK = 1
        const val ANIM_ATTACK = 2
        const val ANIM_DEATH = 3

        /** Room for 10 anim states per variant (idle/walk/attack/death + extras). */
        const val VARIANT_STRIDE = 10

        const val DEFAULT_FRAME_DURATION = 0.15f
        const val SLOW_FRAME_DURATION = 0.25f
        const val FAST_FRAME_DURATION = 0.1f

        // Enemy variants (must match atlas frame prefixes)
        const val VARIANT_ZOMBIE = 0
        const val VARIANT_RUNNER = 1
        const val VARIANT_BRUTE = 2
        const val VARIANT_SPITTER = 3
        const val VARIANT_BOMBER = 4
        const val VARIANT_HEALER = 5
        const val VARIANT_SHIELDER = 6
        const val VARIANT_FLYER = 7
        const val VARIANT_ELITE = 8
        const val VARIANT_BOSS = 9

        // Hero variants
        const val VARIANT_HERO_DEFAULT = 0
        const val VARIANT_HERO_KNIGHT = 0
        const val VARIANT_HERO_COMMANDER = 1
        const val VARIANT_HERO_BERSERKER = 2
        const val VARIANT_HERO_ENGINEER = 3
        const val VARIANT_HERO_MEDIC = 4
        const val VARIANT_HERO_SCOUT = 5
        const val VARIANT_HERO_SHIELDER = 6

        // Effects variants
        const val VARIANT_PROJ_BULLET = 0
        const val VARIANT_PROJ_LASER = 1
        const val VARIANT_PROJ_ROCKET = 2
        const val VARIANT_PICKUP_XP_SMALL = 3
        const val VARIANT_PICKUP_XP_MEDIUM = 4
        const val VARIANT_PICKUP_XP_LARGE = 5
        const val VARIANT_PICKUP_HEALTH = 6

        fun animKey(variantId: Int, animState: Int): Int =
            variantId * VARIANT_STRIDE + animState

        fun enemyVariant(typeName: String): Int = when (typeName.lowercase()) {
            "zombie" -> VARIANT_ZOMBIE
            "runner" -> VARIANT_RUNNER
            "brute" -> VARIANT_BRUTE
            "spitter" -> VARIANT_SPITTER
            "bomber" -> VARIANT_BOMBER
            "healer" -> VARIANT_HEALER
            "shielder" -> VARIANT_SHIELDER
            "flyer" -> VARIANT_FLYER
            "elite" -> VARIANT_ELITE
            "boss" -> VARIANT_BOSS
            else -> VARIANT_ZOMBIE
        }
    }

    private val atlases = arrayOfNulls<SpriteSheet>(ATLAS_COUNT)

    var isLoaded: Boolean = false
        private set

    fun loadAll() {
        try {
            loadAtlas(ATLAS_HEROES, "spritesheets/heroes.png", "spritesheets/heroes.json")
            loadAtlas(ATLAS_ENEMIES, "spritesheets/enemies.png", "spritesheets/enemies.json")
            loadAtlas(ATLAS_EFFECTS, "spritesheets/effects.png", "spritesheets/effects.json")
            isLoaded = true
        } catch (e: IOException) {
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
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inMutable = false
            }
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Failed to load bitmap: $path", e)
            null
        }
    }

    private fun parseFrameJson(path: String): Map<Int, SpriteAnim> {
        val jsonStr = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            android.util.Log.w(TAG, "Failed to load frame JSON: $path", e)
            return emptyMap()
        }

        val root = JSONObject(jsonStr)
        val framesObj = root.getJSONObject("frames")
        val animFrames = mutableMapOf<String, MutableList<Pair<Int, SpriteFrame>>>()

        val keys = framesObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val frameData = framesObj.getJSONObject(key).getJSONObject("frame")
            val x = frameData.getInt("x")
            val y = frameData.getInt("y")
            val w = frameData.getInt("w")
            val h = frameData.getInt("h")
            val frame = SpriteFrame(Rect(x, y, x + w, y + h), w, h)

            // "zombie_idle_0" → base "zombie_idle", index 0
            val lastUnderscore = key.lastIndexOf('_')
            if (lastUnderscore <= 0) continue
            val indexPart = key.substring(lastUnderscore + 1).toIntOrNull() ?: 0
            val baseName = key.substring(0, lastUnderscore)
            animFrames.getOrPut(baseName) { mutableListOf() }.add(indexPart to frame)
        }

        return buildAnimMap(animFrames)
    }

    /**
     * Map every animation group to a unique (variant, state) key.
     * Also aliases missing WALK → IDLE for that variant so movement doesn't go blank.
     */
    private fun buildAnimMap(
        animFrames: Map<String, List<Pair<Int, SpriteFrame>>>
    ): Map<Int, SpriteAnim> {
        val result = mutableMapOf<Int, SpriteAnim>()
        val idleKeysByVariant = mutableMapOf<Int, Int>()

        for ((name, indexedFrames) in animFrames) {
            val sorted = indexedFrames.sortedBy { it.first }.map { it.second }
            if (sorted.isEmpty()) continue

            val (variantId, animState) = parseNameToVariantAndState(name)
            val key = animKey(variantId, animState)

            val duration = when {
                name.contains("brute") || name.contains("boss") -> SLOW_FRAME_DURATION
                name.contains("runner") -> FAST_FRAME_DURATION
                name.startsWith("proj_") || name.startsWith("pickup_") -> 0f
                else -> DEFAULT_FRAME_DURATION
            }
            result[key] = SpriteAnim(sorted.toTypedArray(), duration)
            if (animState == ANIM_IDLE) {
                idleKeysByVariant[variantId] = key
            }
        }

        // Alias walk→idle when walk frames missing for a variant
        for ((variant, idleKey) in idleKeysByVariant) {
            val walkKey = animKey(variant, ANIM_WALK)
            if (!result.containsKey(walkKey)) {
                result[walkKey] = result[idleKey]!!
            }
        }

        return result
    }

    /**
     * Examples:
     *  - zombie_idle → (0, IDLE)
     *  - hero_knight_walk → (0, WALK)
     *  - proj_bullet → (0, IDLE)
     *  - pickup_xp_small → (3, IDLE)
     */
    private fun parseNameToVariantAndState(name: String): Pair<Int, Int> {
        val state = when {
            name.endsWith("_idle", true) -> ANIM_IDLE
            name.endsWith("_walk", true) -> ANIM_WALK
            name.endsWith("_attack", true) -> ANIM_ATTACK
            name.endsWith("_death", true) -> ANIM_DEATH
            else -> ANIM_IDLE
        }

        val base = name
            .removeSuffix("_idle").removeSuffix("_Idle")
            .removeSuffix("_walk").removeSuffix("_Walk")
            .removeSuffix("_attack").removeSuffix("_Attack")
            .removeSuffix("_death").removeSuffix("_Death")
            .lowercase()

        val variant = when {
            base == "zombie" || base == "enemy_zombie" -> VARIANT_ZOMBIE
            base == "runner" || base == "enemy_runner" -> VARIANT_RUNNER
            base == "brute" || base == "enemy_brute" -> VARIANT_BRUTE
            base == "spitter" || base == "enemy_spitter" -> VARIANT_SPITTER
            base == "bomber" || base == "enemy_bomber" -> VARIANT_BOMBER
            base == "healer" || base == "enemy_healer" -> VARIANT_HEALER
            base == "shielder" || base == "enemy_shielder" -> VARIANT_SHIELDER
            base == "flyer" || base == "enemy_flyer" -> VARIANT_FLYER
            base == "elite" || base == "enemy_elite" -> VARIANT_ELITE
            base == "boss" || base == "enemy_boss" -> VARIANT_BOSS
            base == "hero_knight" || base == "knight" -> VARIANT_HERO_KNIGHT
            base == "hero_commander" || base == "commander" -> VARIANT_HERO_COMMANDER
            base == "hero_berserker" || base == "berserker" -> VARIANT_HERO_BERSERKER
            base == "hero_engineer" || base == "engineer" -> VARIANT_HERO_ENGINEER
            base == "hero_medic" || base == "medic" -> VARIANT_HERO_MEDIC
            base == "hero_scout" || base == "scout" -> VARIANT_HERO_SCOUT
            base == "hero_shielder" -> VARIANT_HERO_SHIELDER
            base == "proj_bullet" -> VARIANT_PROJ_BULLET
            base == "proj_laser" -> VARIANT_PROJ_LASER
            base == "proj_rocket" -> VARIANT_PROJ_ROCKET
            base == "pickup_xp_small" -> VARIANT_PICKUP_XP_SMALL
            base == "pickup_xp_medium" -> VARIANT_PICKUP_XP_MEDIUM
            base == "pickup_xp_large" -> VARIANT_PICKUP_XP_LARGE
            base == "pickup_health" -> VARIANT_PICKUP_HEALTH
            else -> VARIANT_HERO_DEFAULT
        }
        return variant to state
    }

    fun getSheet(atlasId: Int): SpriteSheet? = atlases.getOrNull(atlasId)

    fun getFrame(atlasId: Int, variantId: Int, animStateId: Int, frameIndex: Int): SpriteFrame? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        val key = animKey(variantId, animStateId)
        sheet.getFrame(key, frameIndex)?.let { return it }
        // Fallback: default variant same state, then idle
        if (variantId != 0) {
            sheet.getFrame(animKey(0, animStateId), frameIndex)?.let { return it }
        }
        return sheet.getFrame(animKey(variantId, ANIM_IDLE), frameIndex)
            ?: sheet.getFrame(animKey(0, ANIM_IDLE), frameIndex)
    }

    /** Backward-compat: treats variant as 0. Prefer variant-aware overload. */
    fun getFrame(atlasId: Int, animStateId: Int, frameIndex: Int): SpriteFrame? =
        getFrame(atlasId, 0, animStateId, frameIndex)

    fun getAnim(atlasId: Int, variantId: Int, animStateId: Int): SpriteAnim? {
        val sheet = atlases.getOrNull(atlasId) ?: return null
        sheet.getAnim(animKey(variantId, animStateId))?.let { return it }
        if (variantId != 0) {
            sheet.getAnim(animKey(0, animStateId))?.let { return it }
        }
        return sheet.getAnim(animKey(variantId, ANIM_IDLE))
            ?: sheet.getAnim(animKey(0, ANIM_IDLE))
    }

    fun getAnim(atlasId: Int, animStateId: Int): SpriteAnim? =
        getAnim(atlasId, 0, animStateId)

    fun registerAnim(atlasId: Int, animStateId: Int, anim: SpriteAnim) {
        registerAnim(atlasId, 0, animStateId, anim)
    }

    fun registerAnim(atlasId: Int, variantId: Int, animStateId: Int, anim: SpriteAnim) {
        val sheet = atlases[atlasId] ?: return
        @Suppress("UNCHECKED_CAST")
        val mutableAnims = sheet.animations as MutableMap<Int, SpriteAnim>
        mutableAnims[animKey(variantId, animStateId)] = anim
    }

    /** Test helper: inject a sheet without assets. */
    internal fun setAtlasForTest(atlasId: Int, sheet: SpriteSheet) {
        atlases[atlasId] = sheet
        isLoaded = true
    }
}
