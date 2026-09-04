package com.survivortd.game.ui

import com.survivortd.game.systems.UpgradeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppIconMappingTest {
    @Test
    fun `every upgrade type has an explicit typed icon`() {
        val expected = mapOf(
            UpgradeType.NEW_WEAPON to AppIcon.UPGRADE_NEW_WEAPON,
            UpgradeType.UPGRADE_WEAPON to AppIcon.UPGRADE_WEAPON,
            UpgradeType.NEW_PASSIVE to AppIcon.UPGRADE_NEW_PASSIVE,
            UpgradeType.UPGRADE_PASSIVE to AppIcon.UPGRADE_PASSIVE,
            UpgradeType.HEAL to AppIcon.UPGRADE_HEAL,
            UpgradeType.STAT_BOOST to AppIcon.UPGRADE_STAT_BOOST
        )

        assertEquals(UpgradeType.entries.toSet(), expected.keys)
        expected.forEach { (type, icon) -> assertEquals(icon, type.appIcon()) }
    }

    @Test
    fun `coin copy is meaningful without a platform glyph`() {
        assertEquals("48 coins", coinLabel(48))
        listOf("🪙", "💰").forEach { glyph ->
            check(!coinLabel(48).contains(glyph)) { "Coin copy must not contain $glyph" }
        }
    }

    @Test
    fun `every result metric has an explicit typed icon and plain label`() {
        val expected = mapOf(
            ResultMetric.TIME to AppIcon.RESULT_TIME,
            ResultMetric.LEVEL to AppIcon.RESULT_LEVEL,
            ResultMetric.KILLS to AppIcon.RESULT_KILLS,
            ResultMetric.GOLD to AppIcon.COINS,
            ResultMetric.BONUS to AppIcon.RESULT_BONUS,
            ResultMetric.WEAPONS to AppIcon.RESULT_WEAPONS
        )

        assertEquals(ResultMetric.entries.toSet(), expected.keys)
        expected.forEach { (metric, icon) -> assertEquals(icon, metric.appIcon()) }
        assertEquals(listOf("Time", "Level", "Kills", "Gold", "Bonus", "Weapons"), ResultMetric.entries.map { it.label })
    }
}
