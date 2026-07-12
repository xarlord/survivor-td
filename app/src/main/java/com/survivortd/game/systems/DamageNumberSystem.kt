package com.survivortd.game.systems

import com.survivortd.game.core.GameState

/**
 * Updates floating damage numbers (fade + rise) and removes expired ones.
 * Spawned by CombatSystem / ProjectileSystem; drawn by GameScreen.
 */
class DamageNumberSystem(private val state: GameState) {

    fun update(dt: Float) {
        val list = state.damageNumbers
        var i = 0
        while (i < list.size) {
            val dn = list[i]
            dn.elapsed += dt
            dn.x += dn.vx * dt
            dn.y += dn.vy * dt
            // slight gravity so they arc instead of linear rise forever
            // (vy is negative upward; reduce magnitude over time)
            if (dn.elapsed >= dn.lifetime) {
                list.removeAt(i)
            } else {
                i++
            }
        }
        // Cap to avoid unbounded growth under heavy AoE
        if (list.size > MAX_ACTIVE) {
            list.subList(0, list.size - MAX_ACTIVE).clear()
        }
    }

    companion object {
        const val MAX_ACTIVE = 120
    }
}
