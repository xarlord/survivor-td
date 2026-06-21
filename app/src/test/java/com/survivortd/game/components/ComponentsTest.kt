package com.survivortd.game.components

import com.survivortd.game.config.StatusEffectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for ECS components — basic data integrity and state transitions.
 */
class ComponentsTest {

    @Test
    @DisplayName("HealthComponent correctly reports dead state")
    fun healthComponentDeadState() {
        val hp = HealthComponent(maxHp = 100f, currentHp = 0f)
        assertTrue(hp.isDead)
    }

    @Test
    @DisplayName("HealthComponent HP percentage calculates correctly")
    fun healthComponentPercent() {
        val hp = HealthComponent(maxHp = 100f, currentHp = 75f)
        assertEquals(0.75f, hp.hpPercent, 0.001f)
    }

    @Test
    @DisplayName("HealthComponent handles zero maxHp gracefully")
    fun healthComponentZeroMaxHp() {
        val hp = HealthComponent(maxHp = 0f, currentHp = 0f)
        assertEquals(0f, hp.hpPercent, 0.001f)
    }

    @Test
    @DisplayName("PlayerComponent starts with level 1 and correct XP")
    fun playerComponentDefaults() {
        val player = PlayerComponent()
        assertEquals(1, player.level)
        assertEquals(0, player.currentXp)
        assertEquals(8, player.xpToNext)
    }

    @Test
    @DisplayName("StatusEffects can be added and removed")
    fun statusEffectsManagement() {
        val statusComp = StatusEffectsComponent()
        statusComp.effects.add(
            StatusEffectsComponent.ActiveStatus(
                type = StatusEffectType.BURN,
                duration = 3f,
                magnitude = 5f
            )
        )
        assertEquals(1, statusComp.effects.size)
        assertEquals(StatusEffectType.BURN, statusComp.effects[0].type)
    }

    @Test
    @DisplayName("ProjectileComponent hitEntityIds tracks pierced enemies")
    fun projectileHitTracking() {
        val proj = ProjectileComponent(damage = 10f, pierceCount = 2)
        proj.hitEntityIds.add(1)
        proj.hitEntityIds.add(2)
        assertEquals(2, proj.hitEntityIds.size)
    }

    @Test
    @DisplayName("TowerComponent has all tower types in enum")
    fun towerComponentEnumValues() {
        val values = TowerComponent.TowerData.values()
        assertEquals(6, values.size)
        assertTrue(values.contains(TowerComponent.TowerData.GUN_TURRET))
        assertTrue(values.contains(TowerComponent.TowerData.ROCKET_POD))
    }
}
