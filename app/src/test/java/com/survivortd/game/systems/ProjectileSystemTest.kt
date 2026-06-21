package com.survivortd.game.systems

import com.survivortd.game.components.EnemyComponent
import com.survivortd.game.components.ProjectileComponent
import com.survivortd.game.components.TagComponent
import com.survivortd.game.core.GameState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ProjectileSystemTest {

    private lateinit var state: GameState
    private lateinit var projSys: ProjectileSystem

    @BeforeEach
    fun setup() {
        state = GameState()
        state.spawnPlayer()
        projSys = ProjectileSystem(state)
    }

    @Test
    @DisplayName("Projectile moves according to velocity")
    fun projectileMoves() {
        val id = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[id].x = 100f
        state.velocities[id].y = 0f
        state.projectiles[id].lifetime = 5f
        projSys.update(1f)
        assertEquals(100f, state.positions[id].x, 5f, "Projectile should move 100px right")
    }

    @Test
    @DisplayName("Projectile expires after lifetime")
    fun projectileExpires() {
        val id = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[id].x = 100f
        state.projectiles[id].lifetime = 0.5f
        projSys.update(0.6f)  // Longer than lifetime
        assertTrue(state.healths[id].isDead, "Projectile should be dead after lifetime expires")
    }

    @Test
    @DisplayName("Projectile damages enemy on collision")
    fun projectileDamagesEnemy() {
        val projId = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[projId].x = 100f
        state.projectiles[projId].damage = 15f
        state.projectiles[projId].lifetime = 5f

        val enemyId = state.spawnEnemy(x = 30f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val hpBefore = state.healths[enemyId].currentHp

        // Projectile at x=0 moves 100px/sec, enemy at x=30, dt=0.5 → proj reaches x=50
        // Enemy at x=30 is within hitRadius (24) of projectile at x=50? 50-30=20 ≤ 24 → HIT
        projSys.update(0.5f)

        assertTrue(state.healths[enemyId].currentHp < hpBefore,
            "Enemy should take damage: before=$hpBefore, after=${state.healths[enemyId].currentHp}")
    }

    @Test
    @DisplayName("Piercing projectile hits multiple enemies")
    fun pierceHitsMultiple() {
        val projId = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[projId].x = 1000f
        state.projectiles[projId].damage = 10f
        state.projectiles[projId].pierceCount = 2
        state.projectiles[projId].lifetime = 5f

        val e1 = state.spawnEnemy(x = 30f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val e2 = state.spawnEnemy(x = 60f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val hp1Before = state.healths[e1].currentHp
        val hp2Before = state.healths[e2].currentHp

        projSys.update(0.1f)  // Projectile passes through both
        assertTrue(state.healths[e1].currentHp < hp1Before, "Enemy 1 should be hit")
        assertTrue(state.healths[e2].currentHp < hp2Before, "Enemy 2 should be hit")
    }

    @Test
    @DisplayName("Non-piercing projectile dies after first hit")
    fun nonPierceDiesOnHit() {
        val projId = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[projId].x = 1000f
        state.projectiles[projId].damage = 10f
        state.projectiles[projId].pierceCount = 0
        state.projectiles[projId].lifetime = 5f

        state.spawnEnemy(x = 30f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)

        projSys.update(0.1f)
        assertTrue(state.healths[projId].isDead, "Non-piercing projectile should die after first hit")
    }

    @Test
    @DisplayName("AoE projectile damages all enemies in radius")
    fun aoeDamagesMultiple() {
        val projId = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[projId].x = 1000f
        state.projectiles[projId].damage = 30f
        state.projectiles[projId].pierceCount = 0
        state.projectiles[projId].lifetime = 5f
        state.projectiles[projId].aoeRadius = 80f

        val e1 = state.spawnEnemy(x = 30f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val e2 = state.spawnEnemy(x = 40f, y = 10f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val e3 = state.spawnEnemy(x = 50f, y = -5f, enemyType = EnemyComponent.EnemyData.ZOMBIE)

        val hp1Before = state.healths[e1].currentHp
        val hp2Before = state.healths[e2].currentHp
        val hp3Before = state.healths[e3].currentHp

        projSys.update(0.1f)  // Projectile hits e1, AoE catches e2 and e3

        assertTrue(state.healths[e1].currentHp < hp1Before, "Direct hit enemy should be damaged")
        assertTrue(state.healths[e2].currentHp < hp2Before, "AoE enemy 2 should be damaged")
        assertTrue(state.healths[e3].currentHp < hp3Before, "AoE enemy 3 should be damaged")
    }

    @Test
    @DisplayName("Boomerang pierces all enemies without dying")
    fun boomerangPiercesAll() {
        val projId = state.spawnProjectile(x = 0f, y = 0f)
        state.velocities[projId].x = 800f
        state.projectiles[projId].damage = 20f
        state.projectiles[projId].pierceCount = 999
        state.projectiles[projId].isBoomerang = true
        state.projectiles[projId].lifetime = 5f

        for (i in 0 until 5) {
            state.spawnEnemy(x = 20f + i * 30f, y = 0f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        }

        projSys.update(0.1f)
        assertFalse(state.healths[projId].isDead, "Boomerang should NOT die on hit")
    }

    @Test
    @DisplayName("Mine detonates when enemy approaches")
    fun mineDetonates() {
        val mineId = state.spawnProjectile(x = 100f, y = 100f)
        state.projectiles[mineId].damage = 50f
        state.projectiles[mineId].isMine = true
        state.projectiles[mineId].aoeRadius = 80f
        state.projectiles[mineId].lifetime = 20f

        // Place enemy just outside trigger range
        val enemyId = state.spawnEnemy(x = 200f, y = 100f, enemyType = EnemyComponent.EnemyData.ZOMBIE)
        val hpBefore = state.healths[enemyId].currentHp

        projSys.update(0.016f)  // No detonation (too far)
        assertEquals(hpBefore, state.healths[enemyId].currentHp, "Enemy too far, no damage yet")

        // Move enemy close
        state.positions[enemyId].x = 120f
        projSys.update(0.016f)
        assertTrue(state.healths[enemyId].currentHp < hpBefore, "Enemy should be damaged by mine detonation")
        assertTrue(state.healths[mineId].isDead, "Mine should be consumed after detonation")
    }
}
