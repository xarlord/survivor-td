package com.survivortd.game.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Chapter background bitmaps (GDD themed arenas).
 * Assets live under assets/backgrounds/bg_*.png.
 */
class BackgroundManager(private val context: Context) {

    data class ChapterBg(
        val name: String,
        val assetPath: String,
        val bitmap: ImageBitmap?
    )

    private val chapters = mutableListOf<ChapterBg>()

    fun loadAll() {
        chapters.clear()
        val defs = listOf(
            "Wasteland" to "backgrounds/bg_wasteland.png",
            "Toxic Swamp" to "backgrounds/bg_swamp.png",
            "Abandoned City" to "backgrounds/bg_city.png",
            "Underground Lab" to "backgrounds/bg_lab.png",
            "Final Bunker" to "backgrounds/bg_bunker.png",
        )
        for ((name, path) in defs) {
            val bmp = loadAsset(path)
            chapters.add(ChapterBg(name, path, bmp?.asImageBitmap()))
        }
    }

    fun chapterForMinutes(minutes: Float): ChapterBg {
        val idx = when {
            minutes < 3f -> 0
            minutes < 6f -> 1
            minutes < 9f -> 2
            minutes < 12f -> 3
            else -> 4
        }
        return chapters.getOrElse(idx) {
            ChapterBg("Wasteland", "backgrounds/bg_wasteland.png", null)
        }
    }

    fun chapterCount(): Int = chapters.size

    fun hasAnyLoaded(): Boolean = chapters.any { it.bitmap != null }

    private fun loadAsset(path: String): Bitmap? = try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}
