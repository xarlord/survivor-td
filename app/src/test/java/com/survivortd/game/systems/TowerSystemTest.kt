package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.config.TowerType
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TowerSystemTest {

    private lateinit var state: GameState
    private lateinit var towerSys: TowerSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        towerSys = TowerSystem(state)
    }

    @Nested
    @DisplayName("Tower placement")
    inner class Placement {

        @Test
        @DisplayName("Place tower when enough scrap")
        fun placeTowerWithScrap() {
            state.players[state.playerIndex].scrap = 100
            val placed = towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            assertTrue(placed, "Tower should be placed with enough scrap")
            assertEquals(1, towerSys.towers.size)
            assertEquals(50, state.players[state.playerIndex].scrap, "Scrap should be deducted")
        }

        @Test
        @DisplayName("Cannot place tower without enough scrap")
        fun placeTowerNoScrap() {
            state.players[state.playerIndex].scrap = 30
            val placed = towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            assertFalse(placed, "Should not place tower without enough scrap")
            assertEquals(0, towerSys.towers.size)
        }

        @Test
        @DisplayName("Cannot place more than 8 towers")
        fun maxTowers() {
            state.players[state.playerIndex].scrap = 99999
            for (i in 0 until 8) {
                assertTrue(towerSys.placeTower(TowerType.GUN_TURRET, 100f + i * 60f, 100f))
            }
            assertEquals(8, towerSys.towers.size)
            assertFalse(towerSys.placeTower(TowerType.GUN_TURRET, 999f, 999f), "9th tower should fail")
        }

        @Test
        @DisplayName("Cannot place tower too close to existing one")
        fun noOverlap() {
            state.players[state.playerIndex].scrap = 99999
            assertTrue(towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f))
            assertFalse(towerSys.placeTower(TowerType.GUN_TURRET, 210f, 210f), "Should not overlap")
        }

        @Test
        @DisplayName("Different tower types cost different scrap")
        fun differentCosts() {
            state.players[state.playerIndex].scrap = 100
            assertTrue(towerSys.placeTower(TowerType.CANNON, 200f, 200f))
            assertEquals(0, state.players[state.playerIndex].scrap, "Cannon costs 100 scrap")
        }
    }

    @Nested
    @DisplayName("Tower upgrades")
    inner class Upgrades {

        @Test
        @DisplayName("Upgrade tower increases level")
        fun upgradeLevel() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            val upgraded = towerSys.upgradeTower(0)
            assertTrue(upgraded, "Tower should upgrade")
            assertEquals(2, towerSys.towers[0].level)
        }

        @Test
        @DisplayName("Tower max level is 3")
        fun maxLevel() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            towerSys.upgradeTower(0) // → 2
            towerSys.upgradeTower(0) // → 3
            assertFalse(towerSys.upgradeTower(0), "Should not upgrade past level 3")
            assertEquals(3, towerSys.towers[0].level)
        }

        @Test
        @DisplayName("Upgrade costs scrap")
        fun upgradeCostsScrap() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f) // cost 50
            val scrapAfterPlace = state.players[state.playerIndex].scrap
            // Level 2 costs 2x base = 100
            towerSys.upgradeTower(0)
            assertEquals(scrapAfterPlace - 100, state.players[state.playerIndex].scrap)
        }
    }

    @Nested
    @DisplayName("Tower selling")
    inner class Selling {

        @Test
        @DisplayName("Sell tower returns 50% scrap")
        fun sellReturnsScrap() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f) // cost 50
            val scrapBefore = state.players[state.playerIndex].scrap
            towerSys.sellTower(0)
            assertEquals(scrapBefore + 25, state.players[state.playerIndex].scrap, "Should get 50% of 50 = 25")
            assertEquals(0, towerSys.towers.size)
        }

        @Test
        @DisplayName("Sell upgraded tower includes upgrade costs")
        fun sellUpgradedTower() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f) // cost 50
            towerSys.upgradeTower(0) // cost 100
            val scrapBefore = state.players[state.playerIndex].scrap
            towerSys.sellTower(0)
            // Total invested = 50 + 100 = 150, refund 75
            assertEquals(scrapBefore + 75, state.players[state.playerIndex].scrap)
        }
    }

    @Nested
    @DisplayName("Tower targeting & firing")
    inner class Targeting {

        @Test
        @DisplayName("Gun turret damages nearest enemy")
        fun gunTurretFires() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            val enemyId = state.spawnEnemy(x = 250f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val hpBefore = state.healths[enemyId].currentHp

            // Simulate 2 seconds of tower fire (1 shot/s)
            repeat(120) { towerSys.update(0.016f) }

            assertTrue(state.healths[enemyId].currentHp < hpBefore,
                "Enemy HP should decrease from gun turret fire")
        }

        @Test
        @DisplayName("Cannon damages multiple enemies in AoE")
        fun cannonAoEDamage() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.CANNON, 200f, 200f)
            val e1 = state.spawnEnemy(x = 250f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val e2 = state.spawnEnemy(x = 260f, y = 205f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val hp1Before = state.healths[e1].currentHp
            val hp2Before = state.healths[e2].currentHp

            // Cannon fires at 0.5/s → fire after 2s
            repeat(120) { towerSys.update(0.016f) }

            assertTrue(state.healths[e1].currentHp < hp1Before, "Primary target should be damaged")
            assertTrue(state.healths[e2].currentHp < hp2Before, "AoE enemy should be damaged")
        }

        @Test
        @DisplayName("Tower does not fire when no enemies in range")
        fun noFireWithoutTarget() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            // Enemy far away (500px)
            val enemyId = state.spawnEnemy(x = 700f, y = 700f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val hpBefore = state.healths[enemyId].currentHp

            repeat(120) { towerSys.update(0.016f) }

            assertEquals(hpBefore, state.healths[enemyId].currentHp, "Enemy should not be damaged")
        }

        @Test
        @DisplayName("Frost tower applies slow to nearby enemies")
        fun frostAppliesSlow() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.FROST_TOWER, 200f, 200f)
            val enemyId = state.spawnEnemy(x = 250f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)

            repeat(60) { towerSys.update(0.016f) }

            assertTrue(state.enemies[enemyId].slowTimer > 0f, "Enemy should be slowed")
            assertTrue(state.enemies[enemyId].slowMagnitude > 0f, "Slow magnitude should be set")
        }

        @Test
        @DisplayName("Tesla coil chains to multiple enemies")
        fun teslaChain() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.TESLA_COIL, 200f, 200f)
            val e1 = state.spawnEnemy(x = 250f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val e2 = state.spawnEnemy(x = 280f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val e3 = state.spawnEnemy(x = 310f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val hp1Before = state.healths[e1].currentHp
            val hp2Before = state.healths[e2].currentHp
            val hp3Before = state.healths[e3].currentHp

            repeat(60) { towerSys.update(0.016f) }

            assertTrue(state.healths[e1].currentHp < hp1Before, "Chain target 1 should be hit")
            assertTrue(state.healths[e2].currentHp < hp2Before, "Chain target 2 should be hit")
        }

        @Test
        @DisplayName("Tower range increases with level")
        fun rangeIncreasesWithLevel() {
            state.players[state.playerIndex].scrap = 99999
            towerSys.placeTower(TowerType.GUN_TURRET, 200f, 200f)
            towerSys.upgradeTower(0) // Level 2

            // Level 2 has +20% range = 180px instead of 150px
            // Enemy at 170px — was out of range at level 1, in range at level 2
            val enemyId = state.spawnEnemy(x = 370f, y = 200f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
            val hpBefore = state.healths[enemyId].currentHp

            repeat(120) { towerSys.update(0.016f) }

            assertTrue(state.healths[enemyId].currentHp < hpBefore,
                "Level 2 tower should hit enemy at 170px (out of level 1 range)")
        }
    }
}
