package com.survivortd.game.ui

import com.survivortd.game.config.TowerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildPhaseTowerIconTest {
    @Test
    fun `every tower type has a distinct stable typed icon`() {
        val expected = mapOf(
            TowerType.GUN_TURRET to AppIcon.TOWER_GUN,
            TowerType.CANNON to AppIcon.TOWER_CANNON,
            TowerType.FROST_TOWER to AppIcon.TOWER_FROST,
            TowerType.TESLA_COIL to AppIcon.TOWER_TESLA,
            TowerType.POISON_TOWER to AppIcon.TOWER_POISON,
            TowerType.ROCKET_POD to AppIcon.TOWER_ROCKET
        )

        assertEquals(TowerType.entries.toSet(), expected.keys)
        assertEquals(TowerType.entries.size, expected.values.toSet().size)
        TowerType.entries.forEach { type ->
            assertEquals(expected.getValue(type), towerIcon(type))
        }
    }
}
