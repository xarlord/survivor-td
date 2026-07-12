package com.survivortd.game.systems

import com.survivortd.game.components.DamageNumberComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DamageNumberSystemTest {
    private lateinit var state: GameState
    private lateinit var system: DamageNumberSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        system = DamageNumberSystem(state)
    }

    @Test
    @DisplayName("update floats numbers upward and advances elapsed")
    fun floatsUpward() {
        state.damageNumbers.add(DamageNumberComponent(x = 100f, y = 200f, value = 25f))
        val y0 = state.damageNumbers[0].y
        system.update(0.1f)
        assertTrue(state.damageNumbers[0].y < y0)
        assertEquals(0.1f, state.damageNumbers[0].elapsed, 0.001f)
    }

    @Test
    @DisplayName("expired numbers are removed")
    fun removesExpired() {
        val dn = DamageNumberComponent(x = 0f, y = 0f, value = 10f, lifetime = 0.5f)
        dn.elapsed = 0.4f
        state.damageNumbers.add(dn)
        system.update(0.2f)
        assertTrue(state.damageNumbers.isEmpty())
    }

    @Test
    @DisplayName("cap trims oldest when over MAX_ACTIVE")
    fun capsActive() {
        repeat(DamageNumberSystem.MAX_ACTIVE + 30) {
            state.damageNumbers.add(DamageNumberComponent(x = it.toFloat(), y = 0f, value = 1f))
        }
        system.update(0.001f)
        assertEquals(DamageNumberSystem.MAX_ACTIVE, state.damageNumbers.size)
    }
}
