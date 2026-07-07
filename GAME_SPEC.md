# GAME SPEC: Survivor TD (Working Title)

> **Genre**: Bullet-Heaven Tower Defense / Wave Survival Roguelite
> **Inspiration**: Survivor.io + Vampire Survivors + Tower Defense
> **Platform**: Android (Kotlin + Compose Canvas + Fleks ECS + dyn4j)
> **Monetization**: F2P — Rewarded Ads + IAP (no pay-to-win)
> **Target**: 60 FPS on mid-range Android, sessions 3-15 min

---

## 1. CORE CONCEPT

The player controls a single hero character that auto-attacks enemies in a top-down 2D arena. Enemies spawn in waves and rush the player. The player moves with a virtual joystick to dodge and position. On leveling up, the player chooses 1 of 3 random upgrades — weapons, passive boosts, or utility skills. Survive as long as possible (typically 15-minute matches).

**What makes it different from survivor.io**: A strategic **tower/building layer**. Between waves, the player can place defensive towers on the battlefield that auto-target enemies, creating a tower-defense meta-layer on top of the bullet-heaven core.

---

## 2. THE THREE GAME LOOPS

### Primary Loop (Instant — Every Second)
```
Move hero (joystick) → Enemies approach → Weapons auto-fire → 
Enemies die → XP gems drop → Collect gems → Fill XP bar → Level up →
Choose upgrade → Weapons get stronger → More enemies spawn
```

### Secondary Loop (Per Match — 15 Minutes)
```
Start match → Early game (weak, 1 weapon) → Mid game (3-5 weapons, upgrading) →
Boss waves (minutes 5, 10, 15) → Build towers between waves →
Survive or die → End screen: XP earned, gold earned, unlocks → Return to meta
```

### Tertiary Loop (Meta-Game — Between Sessions)
```
Earn gold from matches → Upgrade permanent hero stats → Unlock new weapons →
Unlock new heroes → Unlock new maps/chapters → Daily challenges →
Battle Pass progression → Return to matches stronger
```

---

## 3. PLAYER HERO

### 3.1 Movement
- **Control**: Single virtual joystick (left thumb). Auto-aim weapons (right side free for abilities/ultimates).
- **Speed**: Base 220 px/s (adjustable via passive upgrades)
- **Hitbox**: Circular, radius 20px (forgiving — smaller than visual sprite)
- **Dash**: Double-tap joystick = dash in direction (120ms, 400px/s, 3s cooldown)

### 3.2 Base Stats
| Stat | Base | Description |
|------|------|-------------|
| HP | 100 | Health points. 0 = death = match over |
| Move Speed | 220 px/s | Movement speed |
| Pickup Range | 60 px | Radius for auto-collecting XP gems |
| Damage Multiplier | 1.0x | Global damage multiplier |
| Attack Speed | 1.0x | Global attack frequency multiplier |
| Crit Chance | 5% | Chance for 2x damage |
| Armor | 0 | Reduces incoming damage by flat amount |
| HP Regen | 0.5/s | Health restored per second |
| Dodge | 0% | Chance to avoid all damage from a hit |

### 3.3 Heroes (Unlockable)
| Hero | Passive Ability | Starting Weapon | Unlock |
|------|----------------|------------------|--------|
| **Commander** (default) | +10% tower damage & range | Assault Rifle | Free |
| **Berserker** | +15% damage when HP < 30% | Katana Slash | 5,000 Gold |
| **Engineer** | Can place 1 extra tower, towers 20% cheaper | Drone | 10,000 Gold |
| **Medic** | +2 HP/s regen, healing items 50% stronger | Healing Pulse | 15,000 Gold |
| **Scout** | +25% move speed, +30% pickup range | Boomerang | 20,000 Gold |
| **Shielder** | Takes 50% less damage for 2s every 10s | Force Field | Complete Ch.1 |

---

## 4. WEAPONS SYSTEM (Auto-Fire, No Manual Aiming)

### 4.1 Design Philosophy
- Weapons fire **automatically** at the nearest enemy (or per their targeting pattern)
- Each weapon has: damage, cooldown, projectile count, area, duration, pierce
- Weapons **upgrade via merging**: get the same weapon upgrade 3-5 times → weapon evolves
- Max 6 weapon slots active simultaneously

### 4.2 Weapon Roster (12 Base Weapons)

| # | Weapon | Type | Description | Evolution |
|---|--------|------|-------------|-----------|
| W1 | **Assault Rifle** | Projectile | Fires bullets at nearest enemy. Reliable DPS. | → **Minigun**: 3x fire rate, spread shot |
| W2 | **Spread Gun** | Spread | Fires 3-5 bullets in a cone | → **Plasma Cannon**: Piercing plasma rounds |
| W3 | **Katana Slash** | Melee Arc | Slashes in front of player | → **Whirlwind Blade**: 360° spinning slash |
| W4 | **Lightning Orb** | Orbital | Orbits player, damages on contact | → **Thunder Storm**: 2 orbs, chain lightning |
| W5 | **Rocket Launcher** | AoE | Fires rocket at densest enemy cluster | → **Missile Barrage**: 5 rockets, cluster bombs |
| W6 | **Force Field** | Shield | Bubble around player, damages pushed enemies | → **Plasma Shield**: Larger, reflects projectiles |
| W7 | **Drone** | Summon | Autonomous drone shoots enemies | → **Drone Swarm**: 3 drones, piercing shots |
| W8 | **Frost Nova** | PBAoE | Periodic frost burst around player, slows | → **Absolute Zero**: Freezes enemies solid |
| W9 | **Boomerang** | Boomerang | Throws and returns, pierces all | → **Razor Edge**: 3 boomerangs, bleed DoT |
| W10 | **Landmine** | Trap | Drops mines behind player | → **Minefield**: Cluster drops, bigger blast |
| W11 | **Healing Pulse** | Support | Heals player periodically | → **Regen Aura**: Heals + cleanses debuffs |
| W12 | **Laser Beam** | Beam | Continuous beam to nearest enemy | → **Death Ray**: Sweeping laser, pierces all |

### 4.3 Weapon Mechanics
- **Base Level**: Each weapon starts at Level 1
- **Upgrade**: Each level-up choice can upgrade an existing weapon (+dmg, +projectiles, -cooldown)
- **Evolution**: When a weapon reaches Level 5 AND the player has the matching passive item, it evolves into its Ultimate form
- **Max Slots**: 6 weapon slots. Choosing a new weapon when full = replace weakest

### 4.4 Weapon Leveling (Per-Level Effects)

Example — Assault Rifle leveling:
| Level | Effect |
|-------|--------|
| 1 | Base: 10 dmg, 0.3s cooldown, 1 bullet |
| 2 | +5 damage (15 total) |
| 3 | +1 bullet per shot |
| 4 | -0.05s cooldown (0.25s) |
| 5 | +10 damage, bullets pierce 1 enemy |
| **Evolved** | Minigun: 30 dmg, 0.1s cooldown, 3-bullet spread, pierce 2 |

---

## 5. PASSIVE ITEMS (Upgrade Choices)

### 5.1 Design
- Passive items boost the player globally. They're upgrade choices mixed in with weapons.
- **Weapon Evolution Catalyst**: Each evolved weapon requires a specific passive item to be owned
- Max 6 passive slots

### 5.2 Passive Item Roster (12 Items)

| # | Item | Effect | Max Stacks | Evolution Catalyst For |
|---|------|--------|------------|----------------------|
| P1 | **Power Core** | +15% damage per stack | 5 | Assault Rifle → Minigun |
| P2 | **Rapid Loader** | +10% attack speed per stack | 5 | Spread Gun → Plasma Cannon |
| P3 | **Energy Drink** | +15 px/s move speed per stack | 5 | Katana → Whirlwind Blade |
| P4 | **High-Voltage** | +15% orb damage & speed | 5 | Lightning Orb → Thunder Storm |
| P5 | **Heavy Caliber** | +20% AoE/explosion size | 5 | Rocket → Missile Barrage |
| P6 | **Reinforced Plating** | +5 armor per stack | 5 | Force Field → Plasma Shield |
| P7 | **CPU Upgrade** | +20% drone damage & fire rate | 5 | Drone → Drone Swarm |
| P5b | **Cryo Module** | +15% slow duration & effect | 5 | Frost Nova → Absolute Zero |
| P8 | **Sharp Edge** | +10% crit chance per stack | 5 | Boomerang → Razor Edge |
| P9 | **Expanded Magazine** | +1 projectile to all weapons | 3 | Landmine → Minefield |
| P10 | **Med Kit** | +1 HP regen per stack | 5 | Healing Pulse → Regen Aura |
| P11 | **Battery** | +10% weapon duration & area | 5 | Laser → Death Ray |

---

## 6. LEVELING & PROGRESSION (In-Match)

### 6.1 XP Curve
- Kill enemy → XP gem drops (small=1xp, medium=5xp, large=20xp, boss=100xp)
- XP to next level scales exponentially:
  - Level 1→2: 5 XP
  - Level 2→3: 10 XP
  - Level 3→4: 20 XP
  - Level N→N+1: `5 + N * 3` XP (linear, not exponential)
  - Level 10→11: 35 XP
  - Level 15→16: 53 XP
  - Level 20→21: 68 XP
  - Max level: 30 (reachable in ~20 min endless, ~Lv 18 in 15-min match)

### 6.2 Level-Up Flow
```
XP bar fills → Game time freezes (0.2s fade) → 
Overlay shows 3 random upgrade cards →
Player taps one → Upgrade applied → Game resumes
```

### 6.3 Upgrade Card Logic
On each level-up, generate 3 cards from a weighted pool:
- **New Weapon** (if < 6 weapons): 25% chance to appear
- **Upgrade Existing Weapon** (if any weapon < Level 5): 40% chance
- **New Passive** (if < 6 passives): 20% chance
- **Upgrade Existing Passive** (if any passive < max stacks): 15% chance

**Evolution override**: If a weapon is Level 5 AND its catalyst passive is owned, the evolution card is **guaranteed** to appear.

### 6.4 Evolution System (Detailed)
```
Weapon reaches Level 5 + Catalyst Passive owned →
Next level-up: Evolution card appears (guaranteed) →
Player selects → Weapon transforms → Old passive consumed →
New evolved weapon starts at Level 1 (can level up 5 more times)
```

---

## 7. TOWER / BUILDING SYSTEM (TD Layer)

### 7.1 Design
Between waves (every 3 minutes), a **Build Phase** appears (10 seconds). The player can spend **Scrap** (dropped by enemies) to place towers on the battlefield. Towers persist for the rest of the match.

### 7.2 Tower Types (6)

| Tower | Cost | Damage | Range | Fire Rate | Special |
|-------|------|--------|-------|-----------|---------|
| **Gun Turret** | 50 | 15 | 150px | 1.0/s | Reliable single-target DPS |
| **Cannon** | 100 | 40 | 120px | 0.5/s | AoE splash damage |
| **Frost Tower** | 75 | 5 | 130px | 0.8/s | Slows enemies 40% for 2s |
| **Tesla Coil** | 120 | 25 | 110px | 0.7/s | Chain lightning to 3 enemies |
| **Poison Tower** | 80 | 3/s DoT | 140px | Applied | Poison cloud, ignores armor |
| **Rocket Pod** | 150 | 60 | 200px | 0.3/s | Long range, AoE, slow projectile |

### 7.3 Tower Upgrades
- Each tower can be upgraded 3 times (click tower during build phase):
  - Level 2 (2x cost): +50% damage, +20% range
  - Level 3 (3x cost): +100% damage, +30% range, +special effect
- Max towers: 8 (default), 9 with Engineer hero

### 7.4 Strategic Layer
- Towers create "kill zones" — player can kite enemies through tower range
- **Synergy**: Frost Tower slows → Cannon hits clustered enemies → Poison DoT finishes
- **Scrap economy**: Choosing between upgrading hero (level-up) vs placing towers

---

### 7.5 Auto-Build (Focus Mode)
- If playing offline or with "Focus Mode" enabled, auto-build places towers optimally.

---

## 8. ENEMIES

### 8.1 Enemy Types

| Type | HP | Speed | Damage | XP | Behavior |
|------|----|-------|--------|----|----------|
| **Zombie** | 20 | 80 px/s | 10 | 1 | Walks straight toward player |
| **Runner** | 15 | 160 px/s | 8 | 1 | Fast, zig-zag approach |
| **Brute** | 100 | 60 px/s | 25 | 5 | Tanky, slow, high contact damage |
| **Spitter** | 40 | 50 px/s | 5 (ranged) | 3 | Stops at range, shoots projectiles |
| **Bomber** | 30 | 100 px/s | 40 (AoE) | 3 | Explodes on death or contact |
| **Healer** | 50 | 70 px/s | 0 | 5 | Heals nearby enemies |
| **Shielder** | 60 | 80 px/s | 10 | 5 | Projects shield to nearby enemies |
| **Flyer** | 25 | 120 px/s | 12 | 2 | Ignores terrain/towers, beelines player |
| **Elite** | 3x base | 1.2x | 2x | 3x | Random type, glowing aura, drops loot |
| **Boss** | 4000+ | 50 px/s | 50+ | 100+ | Appears at min 5/10/15, multiple attacks |

### 8.2 Enemy Scaling (Per Minute)
```
HP = baseHP * (1 + minutesElapsed * 0.15)
Damage = baseDamage * (1 + minutesElapsed * 0.08)
Spawn Rate = baseRate * (1 + minutesElapsed * 0.20)
Elite Chance = 5% + minutesElapsed * 0.5%
```

### 8.3 Wave Structure
- **Continuous spawning** — no discrete "waves" during active combat
- Spawn rate increases over time (see scaling above)
- **Boss waves** at minutes 5, 10, 15 — boss spawns, normal spawns pause for 10s
- **Build Phase** after each boss wave: 10s break, player places towers

### 8.4 Enemy AI (via enemy_ai_system.kt)
- **PatrolBehavior**: Not used (enemies always chase)
- **ChaseBehavior**: Move toward player at enemy speed. On contact → deal damage + knockback
- **ShootBehavior**: For Spitters — stop at range, fire projectile at player position
- **WaveBehavior**: Continuous spawning from screen edges in weighted-random directions
 weighted by enemy type

### 8.5 Spawning Logic
```
spawnTimer += dt
if spawnTimer > spawnInterval:
    spawnTimer = 0
    spawnInterval = max(0.3, baseInterval - minutesElapsed * 0.05)
    
    # Pick enemy type from weighted pool
    pool = getWeightedPool(minutesElapsed)
    type = weightedRandom(pool)
    
    # Spawn at random screen edge
    edge = random(4)  # top, bottom, left, right
    pos = randomPointOnEdge(edge, screenBounds)
    
    # Create entity with type-specific components
    spawnEnemy(type, pos)
```

---

## 9. CHAPTERS & MAPS

### 9.1 Chapter Structure
| Chapter | Map Theme | Boss | Duration | Unlock |
|---------|-----------|------|----------|--------|
| Ch.1 | **Wasteland** | Alpha Beast | 15 min | Default |
| Ch.2 | **Toxic Swamp** | Hive Queen | 15 min | Clear Ch.1 once |
| Ch.3 | **Abandoned City** | War Machine | 15 min | Clear Ch.2 once |
| Ch.4 | **Underground Lab** | Experiment 99 | 15 min | Clear Ch.3 once |
| Ch.5 | **Final Bunker** | Overlord | 20 min | Clear Ch.4 once |

### 9.2 Map Mechanics
- **Fixed arena**: 1280x720 world space (camera follows player)
- **World bounds**: Player and enemies constrained to arena
- **Obstacles**: Destructible crates (drop loot), indestructible walls (block movement/projectiles)
- **Pickups**: Health packs, magnet (collect all gems), bomb (clear screen), treasure chest (gold)

---

## 10. META-PROGRESSION (Between Matches)

### 10.1 Permanent Upgrades (Gold)
Permanent upgrades purchased with gold earned from matches. These persist across all matches.

| Upgrade | Effect | Levels | Cost (Gold) |
|---------|--------|--------|-------------|
| **Max HP +20** | +20 HP per level | 10 | 500, 1000, 2000, ... |
| **Move Speed +10** | +10 px/s per level | 5 | 500, 1000, 2000, ... |
| **Starting Damage +5%** | +5% global damage per level | 10 | 800, 1600, ... |
| **Starting Pickup +10px** | +10px pickup range | 5 | 400, 80, 1200, ... |
| **Extra Life** | +1 revive per match | 3 | 5000, 10000, 20000 |
| **XP Gain +5%** | +5% XP from gems | 10 | 600, 1200, ... |
| **Gold Find +10%** | +10% gold from drops | 5 | 1000, 2000, ... |
| **Tower Discount** | -10% tower cost | 3 | 2000, 4000, 8000 |
| **Starting Weapon Level** | Start each match with a weapon at Lv.2 | 3 | 3000, 6000, 9000 |

### 10.2 Gold Economy
- Earn gold from: kills (1-3 gold each), treasure chests (50-200), match completion bonus (100-500)
- Spend gold on: permanent upgrades, hero unlocks, new chapter access
- Gold cap: 99,999

### 10.3 Battle Pass / Mastery (future — post-MVP)
- Daily challenges: "Kill 500 zombies", "Survive 10 minutes", "Use 3 weapons"
- Weekly: "Clear Chapter 1 in under 12 minutes"
- Mastery levels per weapon: reach 1000 kills with a weapon → unlock a cosmetic skin

---

## 11. MONETIZATION

### 11.1 Rewarded Ads (Primary Revenue)
| Trigger | Reward |
|---------|--------|
| Death → Revive | Watch ad → Revive with 50% HP (1x per match) |
| Double Gold | Watch ad → 2x gold from last match |
| Daily Free Gold | Watch ad → 100 gold (3x/day) |
| Free Spin | Watch ad → Spin wheel for random reward |

### 10.2 IAP (Secondary Revenue)
| Item | Price | Description |
|------|-------|-------------|
| **Starter Pack** | $0.99 | 5,000 gold + 2x XP boost (1h) + remove ads for 24h |
| **Gold Pouch** | $2.99 | 15,000 gold |
| **Gold Chest** | $4.99 | 30,000 gold + 500 gems (premium currency) |
| **Remove Ads** | $4.99 | Permanently remove all non-rewarded ads |
| **Battle Pass** | $4.99/season | Exclusive cosmetics, extra daily challenges |
| **Hero Bundle** | $9.99 | Unlock all current + future heroes |

### 11.3 Design Principles
- **No pay-to-win**: All power comes from playing. IAP only buys convenience/time-savers.
- **No energy system**: Play as much as you want, no stamina limits.
- **Banner ads**: NONE (per Sefa's preference — "more presence" on all screens means more rewarded ad placements, not banners)

---

## 12. CONTROLS & UI

### 12.1 In-Game HUD
```
┌────────────────────────────────────────────────────────┐
│  HP Bar (top center)    Timer (top center)    Wave (top-right)
│  ████████████░░░░░░  HP 78/100      03:42       Wave 3
│  XP Bar (bottom, full width)
│  ██████████████████░░░░░░  Lv. 7   Next: 34/90 XP
│
│                    [GAME CANVAS]
│
│  [Joystick]                              [Ultimate Button]
│  (bottom-left)                           (bottom-right)
│
│  Weapon Icons (top-left, showing levels):  │ Towers (top-right):
│  🔫 Lv3  ⚡ Lv2  💊 Lv1                    │  🏭 x3  ⚡ x1
└────────────────────────────────────────────────────────┘
+ Scrap counter (bottom-center, small): "Scrap: 125"
```

### 12.2 Level-Up Overlay
```
┌────────────────────────────────────────────────────────┐
│                     LEVEL UP!                           │
│                                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ 🔫 NEW   │  │ ⚡ UPGRADE│  │ 💊 NEW   │             │
│  │ Assault  │  │ Lightning│  │ Healing  │             │
│  │ Rifle    │  │ Orb → Lv3│  │ Pulse    │             │
│  │          │  │          │  │          │             │
│  │ +10 DMG  │  │ +15% orb │  │ +1 HP/s  │             │
│  │ every    │  │ dmg &    │  │ regen    │             │
│  │ 0.3s     │  │ speed    │  │          │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                        │
│            Tap to choose (game paused)                 │
└────────────────────────────────────────────────────────┘
```

### 12.3 Build Phase Overlay
```
┌────────────────────────────────────────────────────────┐
│  🔧 BUILD PHASE — 0:10  │  Scrap: 125                  │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐            │
│  │ 🔫 │ │ 💣 │ │ ❄️ │ │ ⚡ │ │ ☣️ │ │ 🚀 │            │
│  │Gun │ │Can-│ │Frst│ │Tsla│ │Pois│ │Rckt│            │
│  │ 50 │ │100 │ │ 75 │ │120 │ │ 80 │ │150 │            │
│  └────┘ └────┘ ┐ └────┘ └────┘ └────┘ └────┘            │
│                                                        │
│  Tap tower type → Tap map to place                     │
│  Tap existing tower to upgrade (long press to sell 50%) │
└────────────────────────────────────────────────────────┘
 Phase ends in 10s or when player taps "Ready"
```

### 12.4 Screen Hierarchy
```
Main Menu
  ├── Play → Chapter Select → Hero Select → Match
  ├── Heroes (unlock/view)
  ├── Upgrades (permanent)
  ├── Settings (audio, haptics, quality)
  └── Shop (IAP, rewarded ads)
```

---

## 13. PHYSICS & TECHNICAL CONSTANTS

### 13.1 World & Rendering
| Constant | Value | Description |
|----------|-------|-------------|
| WORLD_WIDTH | 1280 px | Arena width |
| WORLD_HEIGHT | 720 px | Arena height (camera centered on player) |
| CAMERA_WIDTH | 720 px | Visible width (camera follows player) |
| CAMERA_HEIGHT | 1280 px | Visible height (portrait) |
| TILE_SIZE | 64 px | Tile grid for obstacles/tower placement |
| TARGET_FPS | 60 | Physics tick rate (fixed timestep) |
| MAX_ENTITIES | 500 | Hard cap for ECS entities (enemies + projectiles + pickups) |

### 13.2 Physics Constants
| Constant | Value |
|----------|-------|
| ENEMY_KNOCKBACK | 150 px/s impulse on hit |
| PLAYER_KNOCKBACK | 80 px/s on taking damage |
| PROJECTILE_LIFETIME | 2.0s default |
| GEM_MAGNET_SPEED | 300 px/s when in pickup range |
| GEM_LIFETIME | 30s (disappears if not collected) |

### 13.3 Performance Targets
| Metric | Target |
|--------|--------|
| Frame time | <16.6ms (60 FPS) |
| Entity count | Up to 300 enemies + 200 projectiles simultaneously |
| Memory | <150 MB RAM |
| Battery | <15% per hour |
| Warm start | <2s |

---

## 14. ECS SCHEMA

### 14.1 Components
| Component | Fields | Used By |
|-----------|--------|---------|
| `Position(x, y)` | Float, Float | All entities |
| `Velocity(vx, vy)` | Float, Float | Moving entities |
| `Sprite(textureId, width, height, rotation)` | String, Float, Float, Float | Renderable entities |
| `Health(current, max, regen)` | Float, Float, Float | Player, enemies, towers |
| `Damage(amount, source, isCritical)` | Float, EntityRef, Boolean | Damage events |
| `Enemy(type, behavior, damage, xpValue, knockbackResist)` | Enum, Enum, Float, Int, Float | All enemies |
| `Weapon(type, level, cooldownTimer, cooldownDuration, target)` | Enum, Int, Float, Float, EntityRef? | Player |
| `Projectile(damage, pierceCount, lifetime, weaponType, ownerType)` | Float, Int, Float, Enum, Enum | Projectiles |
| `Pickup(type, value, magnetized)` | Enum(PickupType), Float, Boolean | Gems, health, etc. |
| `Tower(type, level, range, fireRate, fireTimer, cost)` | Enum, Int, Float, Float, Float, Int | Towers |
| `Passive(type, stacks)` | Enum, Int | Player |
| `PlayerStats(hp, maxHp, speed, pickupRange, dmgMult, atkSpeedMult, crit, armor, regen, dodge)` | Multiple | Player |
| `CameraFollow(target, offsetX, offsetY)` | EntityRef, Float, Float | Camera |
| `Lifetime(remaining)` | Float | Projectiles, particles, temporary entities |
| `Particle(type, preset, elapsed, duration, vx, vy)` | Enum, Enum, Float, Float, Float, Float | VFX |
| `SquashStretch(amount, duration, axis)` | Float, Float, Enum | Hit feedback |
| `StatusEffect(type, duration, magnitude)` | Enum(Burn/Poison/Freeze/Slow/SlowAttack/Stun), Float, Float | Enemies |
| `Collider(radius)` | Float | Collision detection |

### 14.2 Systems
| System | Priority | Description |
|--------|----------|-------------|
| `InputSystem` | 0 | Read joystick, update player velocity |
| `WeaponSystem` | 10 | Tick all weapon cooldowns, fire when ready |
| `ProjectileSystem` | 20 | Move projectiles, check collisions |
| `EnemyAISystem` | 30 | Update enemy behaviors (chase, shoot, etc.) |
| `MoveSystem` | 40 | Apply velocities to positions |
| `CollisionSystem` | 50 | Resolve entity-entity collisions (circle-circle) |
| `DamageSystem` | 60 | Process damage events, apply HP changes |
| `StatusEffectSystem` | 65 | Tick status durations, apply DoT/slow |
| `PickupSystem` | 70 | Check pickup-range collisions, apply effects |
| `TowerSystem` | 75 | Tower targeting & firing |
| `LifetimeSystem` | 80 | Remove expired entities |
| `ParticleUpdateSystem` | 85 | Update particle physics & lifetime |
| `XPSystem` | 90 | Check XP bar, trigger level-up if full |
| `SpawnSystem` | 95 | Spawn enemies based on elapsed time |
| `CameraSystem` | 100 | Follow player, apply shake |
| `RenderSystem` | 110 | Draw all entities with Sprite component |

### 14.3 Priority Order (Execution Order)
```
Input → Weapons → Projectiles → EnemyAI → Move → Collision → 
Damage → StatusEffects → Pickups → Towers → Lifetime → Particles → 
XP → Spawn → Camera → Render
16 systems, deterministic tick order
```

---

## 15. LEVEL DATA FORMAT (JSON)

### 15.1 Chapter Definition
```json
{
  "chapterId": "ch1_wasteland",
  "name": "Wasteland",
  "duration": 900,
  "worldWidth": 1280,
  "towerSlots": [
    {"x": 320, "y": 360},
    {"x": 640, "y": 360},
    {"x": 960, "y": 360}
  ],
  "obstacles": [
    {"type": "crate", "x": 400, "y": 300, "hp": 30, "loot": "gold_50"},
    {"type": "wall", "x": 640, "y": 100, "width": 200, "height": 20}
  ],
  "bossTimeMinutes": [5, 10, 15],
  "enemyPool": {
    "0": {"zombie": 70, "runner": 30},
    "3": {"zombie": 50, "runner": 30, "brute": 20},
    "5": {"zombie": 40, "runner": 30, "brute": 15, "spitter": 15},
    "10": {"zombie": 30, "runner": 25, "brute": 20, "spitter": 10, "bomber": 10, "healer": 5},
    "15": {"zombie": 20, "runner": 20, "brute": 20, "spitter": 15, "bomber": 10, "healer": 10, "shielder": 5}
  },
  "bossDefinition": {
    "name": "Alpha Beast",
    "hp": 4000,
    "speed": 50,
    "damage": 50,
    "attacks": [
      {"type": "charge", "cooldown": 8.0, "damage": 60},
      {"type": "summon", "cooldown": 12.0, "count": 5, "summonType": "zombie"},
      {"type": "aoe_slam", "cooldown": 10.0, "radius": 150, "damage": 40}
    ]
  }
}
```

---

## 16. AUDIO DESIGN

### 16.1 Sound Effects (SoundPool)
| Event | SFX | Count |
|-------|-----|-------|
| Weapon fire | gun_shot, slash, explosion, laser_hum, magic_blast | 15 |
| Enemy death | squish, crunch, pop, zombie_groan | 8 |
| Level up | level_up_chime, card_select | 3 |
| XP gem collect | gem_ping (pitch varies by value) | 2 |
| Player hit | hurt_grunt, shield_block | 4 |
| Tower fire | turret_shot, cannon_boom, frost_freeze, tesla_zap | 8 |
| Pickup | health_pack, magnet_pulse, bomb_explosion | 5 |
| UI | button_tap, menu_open, menu_close, coin_clink | 6 |
| Boss | boss_roar, boss_slam, boss_death | 6 |
| Build phase | build_place, upgrade_hum, sell | 4 |
| Player death | death_sting, game_over_sting | 2 |

**Total**: ~63 SFX, all synthesized via pydub

### 16.2 Music
- **Menu music**: Ambient/calm loop (2-3 min loop)
- **Battle music**: Intense, driving beat (3-4 min loop, layers intensify as match progresses)
- **Boss music**: Heavy, dramatic (1 min loop)
- **Build phase**: Brief tension builder (10s)

> **Design note:** In Survivor TD, the "pickup magnet" ability pulls all gems toward the player.

### 16.3 Audio Synthesis Pipeline
All audio synthesized programmatically via `scripts/synth_audio.py`:
- SFX: Square/sawtooth waves with envelopes, noise bursts for explosions
- Music: Layered synth (bass + drums + lead), procedural loops
- All files: Mono 22050Hz 16-bit OGG (SoundPool) or MP3 (BGM)
- Each SFX < 50KB, total audio budget < 3MB

---

## 17. VISUAL ART STYLE

**Art Direction:** Retro pixel art inspired by 16-bit era games (Vampire Survivors, Brotato reference). Minimalist but readable at all zoom levels.

### Character Design
- **Player Hero:** Humanoid survivor, 32x48 pixels base sprite
  - Commander: Military armor, blue/cyan color scheme, distinctive helmet
  - Berserker: Red bandana, muscular build, aggressive posture
  - Engineer: Goggles, tool belt, tech-orange accents
  - Medic: White cross symbol, green accents, staff
  - Scout: Lean silhouette, parkour-ready pose, yellow accents
  - Shielder: Heavy shield, defensive stance, purple energy field
- **Silhouette:** Each hero has unique silhouette for instant recognition
- **Animation:** Idle (2 frames), Run (4 frames loop), Attack (3 frames), Death (4 frames)

### Enemy Design (Visual Distinction)
- **Color coding by threat level:**
  - Common (Zombie, Runner): Green/yellow tones, standard size
  - Tank (Brute): Red, 1.5x larger, heavy frame
  - Ranged (Spitter): Purple, distinct projectile visible
  - Special (Bomber): Orange, glowing red fuse visual
  - Support (Healer): Pink/white, healing aura particles
  - Elite: Gold border, 1.2x size, particle trail
  - Boss: 3x size, multi-part sprite, unique attack animations

### Background Style
- **Parallax:** 3-layer parallax (distant, mid, foreground)
- **Themes:**
  - Wasteland: Desert tones, rubble, debris, muted browns/oranges
  - Toxic Swamp: Green fog overlay, bubbling pools, decayed structures
  - Abandoned City: Crumbling buildings, neon remnants, gray/blue tones
  - Underground Lab: Clean metal, flickering lights, computer screens
  - Final Bunker: Industrial red/black, ominous lighting
- **Obstacles:** Pixel-perfect crates (destructible), walls (indestructible)

### Color Palette
- **Background:** Dark desaturated (#3a3a4a base) - allows projectiles to pop
- **Player:** Blue/cyan tones (#00bfff) - friendly identification
- **Enemies:** Red/orange/yellow gradient - threat indication
  - Low threat: Yellow (#ffd700)
  - Medium threat: Orange (#ff8c00)
  - High threat: Red (#ff0000)
- **Projectiles:** Match weapon element color
  - Physical (bullet): Yellow (#ffff00)
  - Fire: Orange-red (#ff4500)
  - Ice: Cyan (#00ffff)
  - Lightning: Bright yellow (#fff700)
  - Poison: Green (#00ff00)
  - Healing: White (#ffffff)

### Rarity Color Scheme
- Common: White text
- Rare: Blue text
- Epic: Purple text
- Legendary: Gold text
- Mythic (evolved weapons): Rainbow gradient

---

## 18. UI/UX DESIGN

### HUD Overlay (During Gameplay)
```
┌────────────────────────────────────────────────────────┐
│  [♥] HP: 78/100    [⏱] 03:42    [🌊] Wave 3           │ <- Top bar
│  ████████████░░░░░░                                        │
│                                                          │
│  [⭐] Lv.7       XP: 34/90                               │ <- Below HP bar
│  ██████████████████░░░░░░                                │
│                                                          │
│                    [GAME CANVAS]                         │
│                                                          │
│  Weapon Slots:       Towers:        Scrap:              │
│  🔫 Lv3  ⚡ Lv2       🏭 x3  ⚡ x1    💠 125              │ <- Bottom-left
│  💊 Lv1                                                    │
│                                                          │
│  [Joystick] (dynamic)                        [Ult]       │ <- Bottom area
│  (appears on touch)                                     │
└────────────────────────────────────────────────────────┘
```

### UI Element Specifications
- **Health Bar:** Top-center, 300x20px, red gradient (#ff0000 to #ff4500), white border
- **XP Bar:** Below health bar, 300x15px, green gradient (#00ff00 to #32cd32), fills 0-100%
- **Level Badge:** Circular, 32x32px, top-left of XP bar, gold border, pulsing on level-up
- **Timer:** Top-center, white text, monospace font, format MM:SS
- **Wave Indicator:** Top-right, wave icon + number, updates every boss wave
- **Weapon Slots:** Bottom-left, 6 slots horizontal, 64x64px each
  - Shows: Icon, level badge, damage number (bottom-right of icon)
  - Highlight: Golden border for evolved weapons
- **Tower Counter:** Bottom-center, shows placed towers: 🏭 x3
- **Scrap Counter:** Bottom-right, large text: 💠 125
- **Joystick:** Bottom-left, appears on touch, 150x150px maximum, opacity 0.5
- **Ultimate Button:** Bottom-right, 80x80px, shows cooldown circular progress

### Damage Numbers
- **Font:** Bold, white with black outline, 16px base
- **Color:**
  - Normal damage: White
  - Critical damage: Yellow, larger font (24px)
  - Elemental damage: Element color (fire=red, ice=cyan)
- **Animation:** Float upward from hit point, 60px/s vertical speed, fade over 0.5s
- **Stacking:** If multiple hits same location, offset slightly to prevent overlap

### Main Menu
```
┌────────────────────────────────────────────────────────┐
│                   SURVIVOR TD                            │
│                                                        │
│                   [▶ PLAY]                              │ <- Center, largest (120x60px)
│                                                        │
│  [Heroes]  [Upgrades]  [Settings]  [Shop]              │ <- Horizontal row
│                                                        │
│  Gold: 5420 💰                                          │ <- Top-right
└────────────────────────────────────────────────────────┘
```
- **PLAY Button:** Center-screen, 120x60px, green gradient, pulse animation
- **Settings Button:** Top-right corner, 48x48px gear icon
- **Hero Select:** Horizontal carousel, swipe to navigate, shows locked heroes

### Level-Up Overlay
```
┌────────────────────────────────────────────────────────┐
│                    LEVEL UP!                            │ <- Top-center, gold, pulsing
│                                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ 🔫 NEW   │  │ ⚡ UPGRADE│  │ 💊 NEW   │             │ <- 3 cards horizontal
│  │ Assault  │  │ Lightning│  │ Healing  │             │
│  │ Rifle    │  │ Orb → Lv3│  │ Pulse    │             │
│  │          │  │          │  │          │             │
│  │ +10 DMG  │  │ +15% orb │  │ +1 HP/s  │             │
│  │ 0.3s cd  │  │ dmg/speed│  │ regen    │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                        │
│  [Cards: 160x120px each, white background,            │
│   rounded corners 8px, shadow]                       │
│                                                        │
│  Game paused - Tap to choose                           │ <- Bottom hint
└────────────────────────────────────────────────────────┘
```
- **Card specs:** 160x120px, white background, rounded corners (8px radius), drop shadow
- **Icon:** 48x48px, top-center of card
- **Text:** Bold, 14px, black
- **Highlight:** Cards hover-grow on touch (1.1x scale)

### Build Phase Overlay
```
┌────────────────────────────────────────────────────────┐
│  🔧 BUILD PHASE — 0:10  │  Scrap: 125                  │ <- Top bar
│                                                        │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐            │ <- Tower selection
│  │ 🔫 │ │ 💣 │ │ ❄️ │ │ ⚡ │ │ ☣️ │ │ 🚀 │            │ <- 6 tower icons
│  │Gun │ │Can │ │Frs │ │Tsl │ │Poi │ │Rkt │            │ <- 48x48px each
│  │ 50 │ │100 │ │ 75 │ │120 │ │ 80 │ │150 │            │ <- Cost below icon
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘            │
│                                                        │
│  [GAME CANVAS - show placement grid]                  │ <- Placement mode
│  [Highlight valid tiles in green, invalid in red]      │
│                                                        │
│  Tap tower → Tap map to place                           │ <- Bottom hint
│  Long press tower to sell (50% refund)                  │
│  [READY] button (skip timer)                           │ <- Bottom-right
└────────────────────────────────────────────────────────┘
```
- **Tower icons:** 48x48px, grayed out if not enough scrap
- **Placement grid:** 64x64px tiles, semi-transparent overlay
- **Valid placement:** Green highlight (#00ff00, 50% opacity)
- **Invalid placement:** Red highlight (#ff0000, 50% opacity)
- **Preview:** Ghost tower sprite at touch position
- **Timer:** Countdown in red when < 3 seconds remaining

### Pause Menu
```
┌────────────────────────────────────────────────────────┐
│                     ⏸ PAUSED                            │ <- Top-center
│                                                        │
│  ┌────────────────────────────────────────┐           │
│  │              RESUME                     │           │ <- Top button, largest
│  └────────────────────────────────────────┘           │
│                                                        │
│  ┌────────────────────────────────────────┐           │
│  │            SETTINGS                     │           │
│  └────────────────────────────────────────┘           │
│                                                        │
│  ┌────────────────────────────────────────┐           │
│  │            QUIT MATCH                   │           │ <- Bottom button, red
│  └────────────────────────────────────────┘           │
│                                                        │
│  Buttons: 240x60px, centered, 20px vertical spacing  │
└────────────────────────────────────────────────────────┘
```

### Settings Menu
```
┌────────────────────────────────────────────────────────┐
│  ⚙️ SETTINGS                                            │
│                                                        │
│  Music Volume: [▓▓▓▓▓▓▓░░] 70%                         │ <- Slider
│  SFX Volume:   [▓▓▓▓▓▓▓▓░] 80%                         │ <- Slider
│  [✓] Haptics                                           │ <- Toggle
│  Quality: [Auto ▼]                                     │ <- Dropdown
│                                                        │
│  [← BACK]                                              │ <- Bottom-left
└────────────────────────────────────────────────────────┘
```
- **Sliders:** 200px width, drag to adjust
- **Toggles:** Checkboxes, green when enabled
- **Dropdown:** 200px width, tap to expand

---

## 19. VISUAL EFFECTS (VFX)

### Combat Feedback
- **Hit Flash:** Enemy sprite flashes white for 0.1s on damage
  - Implementation: Tint sprite to #ffffff, fade back over 0.1s
- **Damage Number:** Floats upward from hit point, white text, fades over 0.5s
  - Vertical speed: 60px/s
  - Spawn offset: ±10px random horizontal, -20px vertical from center
- **Screen Shake:** Mild shake on player damage
  - Offset: ±5px random X/Y
  - Duration: 0.2s
  - Decay: Linear interpolation to zero
- **Knockback:** Visual sprite slide (no interpolation, instant position snap)
  - Enemy pushed back 20px on hit
  - Recovery: 0.3s before resuming chase

### Death Effects
- **Enemy Death:** Sprite fades out + shrinks over 0.3s
  - Opacity: 1.0 → 0.0
  - Scale: 1.0 → 0.5
  - Easing: Linear
- **Boss Death:** Larger explosion effect
  - Particle burst: 20 particles, 8 directions, 100px/s radial velocity
  - Flash: Full screen white flash 0.3s
  - Sound: boss_death_sting
- **Elite Death:** Gold particle burst
  - 10 gold particles, rainbow colors
  - Last 0.5s before normal death

### Level Up
- **Flash:** Full screen white flash 0.2s
  - Opacity: 0 → 1.0 → 0 (triangle wave)
- **Badge:** Level badge pulses for 1s
  - Scale: 1.0 → 1.3 → 1.0 (sine wave, 2 cycles)
  - Color: Gold glow
- **Sound:** level_up_chime
- **XP Bar:** Turns gold for 1s, then green

### Weapon Evolution
- **Visual Cue:** Weapon icon glows with golden border
  - Border color: #ffd700 (gold)
  - Pulse: 2 cycles over 1s
- **Transform:** Old sprite fades out, new sprite fades in (0.3s crossfade)
- **Particles:** 10 golden particles emanate from weapon slot
- **Sound:** evolution_chime (ascending pitch, 0.5s)

### Projectile Trails
- **Bullet:** Yellow trail, 2px width, 0.2s lifetime
- **Fireball:** Orange particle trail, 5 particles per frame, fade over 0.5s
- **Ice Shard:** No trail, blue sparkles on hit
- **Lightning:** Jagged line, random jitter, 0.1s lifetime
- **Poison:** Green drip particles, fall down 30px/s
- **Laser:** Continuous beam, no trail (beam is persistent)

### Explosion Effects
- **Rocket/Explosion:** Radial particle burst
  - 15 particles, 360° spread
  - Speed: 80-120px/s random
  - Color: Orange → Red gradient
  - Lifetime: 0.4s
- **Minefield:** Cluster explosions
  - 5 smaller explosions, 0.1s delay between each
- **Nuclear (evolved rocket):** Screen flash + large particle burst
  - 30 particles, 200px/s max speed
  - Full screen shake (10px offset, 0.4s)

### Pickup Effects
- **XP Gem:**
  - Magnet: Gem accelerates toward player at 300px/s when in range
  - Collect: Scale 1.0 → 0 (over 0.1s), fade out
  - Sound: gem_ping (pitch varies by value: small=high, medium=mid, large=low)
- **Gold:**
  - Visual: Gold coin sprite, spin animation
  - Collect: +1 UI counter animates (scale +50% then shrink back)
  - Sound: coin_clink
- **Health Pack:**
  - Visual: Green cross icon, pulse animation
  - Collect: Player sprite flashes green 0.2s
  - Sound: health_pickup

### Status Effects Visuals
- **BURN:** Enemy sprite tinted red, flame particles (2 per frame)
  - Damage tick: Red number every 0.5s
- **POISON:** Green tint, green drip particles
  - Damage tick: Green number every 0.5s
- **FREEZE:** Blue tint, ice overlay (semi-transparent white)
  - Visual: Enemy stops animating, shakes slightly
- **STUN:** Star effect above head (star sprite, 16x16px, rotates)
- **SLOW:** Purple tint, movement animates at 50% speed

### Tower Effects
- **Placement:** Building animation (construct sprite 0 → 100% opacity over 0.5s)
- **Upgrade:** Golden ring expands from tower center (0.5s duration)
- **Sell:** Fade out + shrink (0.3s)
- **Firing:**
  - Gun Turret: Muzzle flash (white dot, 0.1s)
  - Cannon: Explosion at tower (smoke particles)
  - Frost Tower: Blue ripple effect (expanding circle, 0.3s)
  - Tesla Coil: Lightning arc to target
  - Poison Tower: Green cloud at impact
  - Rocket Pod: Rocket trail particles

---

## 20. ASSET CREATION CHECKLIST

### Sprite Sheets (16x16 to 64x64 sprites, 4-color palette per sprite)
**Format:** PNG with transparency, power-of-2 dimensions (e.g., 512x512 sheet)

#### Player Heroes
- [ ] **Commander:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px per frame
- [ ] **Berserker:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px
- [ ] **Engineer:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px
- [ ] **Medic:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px
- [ ] **Scout:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px
- [ ] **Shielder:** Idle (2f), Run (4f), Attack (3f), Death (4f) - 32x48px

#### Enemies
- [ ] **Zombie:** Idle (1f), Walk (4f), Attack (2f), Death (3f) - 24x32px
- [ ] **Runner:** Idle (1f), Run (6f), Attack (2f), Death (3f) - 20x28px
- [ ] **Brute:** Idle (1f), Walk (2f), Attack (3f), Death (4f) - 32x40px
- [ ] **Spitter:** Idle (1f), Walk (4f), Spit (2f), Death (3f) - 24x32px
- [ ] **Bomber:** Idle (1f), Walk (4f), Explode (3f), Death (3f) - 24x32px
- [ ] **Healer:** Idle (1f), Walk (4f), Heal (2f), Death (3f) - 24x32px
- [ ] **Shielder:** Idle (1f), Walk (4f), Shield (2f), Death (3f) - 24x32px
- [ ] **Flyer:** Idle (1f), Fly (4f), Attack (2f), Death (3f) - 20x28px
- [ ] **Elite:** Recolor of any type + gold border + particle trail (10 frames)
- [ ] **Boss:** Alpha Beast, Hive Queen, War Machine, Experiment 99, Overlord
  - Each: Idle (2f), Move (4f), Attack1 (3f), Attack2 (3f), Attack3 (3f), Death (6f)
  - Size: 64x80px per frame

#### Weapon Projectiles
- [ ] **Bullet:** 8x8px, yellow, horizontal trail
- [ ] **Spread Gun bullet:** 6x6px, orange, cone pattern
- [ ] **Katana slash:** 32x32px, white arc sprite, transparent edges
- [ ] **Lightning Orb:** 16x16px, yellow/white, pulsing
- [ ] **Rocket:** 12x12px, orange, flame trail particle
- [ ] **Force Field bubble:** 48x48px, cyan, semi-transparent
- [ ] **Drone:** 16x16px, blue, hovering animation
- [ ] **Frost Nova burst:** 64x64px, blue, expanding circle
- [ ] **Boomerang:** 16x8px, silver, spinning animation
- [ ] **Landmine:** 12x12px, brown, blinking red light
- [ ] **Healing Pulse:** 32x32px, white/green, expanding ring
- [ ] **Laser Beam:** 4x64px, red, vertical gradient

#### Towers
- [ ] **Gun Turret:** 32x32px, idle, firing (2f)
- [ ] **Cannon:** 40x40px, idle, firing (3f, recoil animation)
- [ ] **Frost Tower:** 32x32px, idle, firing (2f, blue pulse)
- [ ] **Tesla Coil:** 32x32px, idle, firing (2f, lightning)
- [ ] **Poison Tower:** 32x32px, idle, firing (2f, green cloud)
- [ ] **Rocket Pod:** 48x48px, idle, firing (4f, missile launch)

#### UI Icons
- [ ] **Weapon icons:** 48x48px each (12 weapons)
- [ ] **Passive icons:** 48x48px each (12 passives)
- [ ] **Tower icons:** 48x48px each (6 towers)
- [ ] **Hero portraits:** 64x64px each (6 heroes)
- [ ] **UI elements:** Hearts, coins, gems, scrap, XP bar, level badge, etc.

#### Pickups
- [ ] **XP Gem (small):** 8x8px, blue
- [ ] **XP Gem (medium):** 12x12px, green
- [ ] **XP Gem (large):** 16x16px, gold
- [ ] **Gold coin:** 12x12px, gold, spinning animation
- [ ] **Health pack:** 16x16px, white with red cross
- [ ] **Magnet:** 16x16px, blue magnet icon
- [ ] **Bomb:** 16x16px, black bomb with fuse
- [ ] **Treasure chest:** 24x24px, brown/gold

#### Backgrounds
- [ ] **Wasteland:** 1280x720px base, parallax layers (3 layers)
- [ ] **Toxic Swamp:** 1280x720px base, green fog overlay
- [ ] **Abandoned City:** 1280x720px base, crumbling buildings
- [ ] **Underground Lab:** 1280x720px base, clean metal, screens
- [ ] **Final Bunker:** 1280x720px base, industrial red/black

#### Obstacles
- [ ] **Crate:** 32x32px, destructible, 3 damage states (100%, 50%, 0%)
- [ ] **Wall:** Various sizes (64x32, 32x64, 64x64), indestructible
- [ ] **Rock:** 24x24px, indestructible decoration

### Sound Effects (mono 16-bit WAV, <50KB each)
**Synthesis:** All sounds synthesized via `scripts/synth_audio.py`

#### Combat Sounds
- [ ] **Hit (enemy):** Short impact, 0.1s, crunch
- [ ] **Hit (player):** Slightly deeper, 0.15s, grunt
- [ ] **Critical hit:** Higher pitch, 0.1s, sharp
- [ ] **Enemy death:** Quick crunch, 0.2s, squish
- [ ] **Player death:** Dramatic sting, 0.5s, descending

#### Weapon Sounds
- [ ] **Assault Rifle:** Burst fire, 0.1s per shot, mechanical
- [ ] **Spread Gun:** Shotgun blast, 0.15s, boom
- [ ] **Katana:** Slash, 0.1s, swoosh
- [ ] **Lightning Orb:** Zap, 0.1s, electric crackle
- [ ] **Rocket Launcher:** Thud + explosion, 0.3s, rumble
- [ ] **Force Field:** Hum, 0.2s, shield activation
- [ ] **Drone:** Whirring, 0.1s per shot, mechanical
- [ ] **Frost Nova:** Burst, 0.2s, ice shatter
- [ ] **Boomerang:** Swoosh-return, 0.3s, aerodynamic
- [ ] **Landmine:** Click + boom, 0.4s, delay then explosion
- [ ] **Healing Pulse:** Chime, 0.3s, ascending
- [ ] **Laser Beam:** Hum, continuous, while firing

#### Tower Sounds
- [ ] **Gun Turret:** Machine gun, 0.1s per shot
- [ ] **Cannon:** Boom, 0.2s, deep explosion
- [ ] **Frost Tower:** Freeze, 0.2s, ice crack
- [ ] **Tesla Coil:** Zap, 0.1s, electric arc
- [ ] **Poison Tower:** Hiss, 0.2s, gas release
- [ ] **Rocket Pod:** Launch, 0.3s, rocket whoosh

#### Pickup Sounds
- [ ] **XP Gem (small):** High ping, 0.05s, chime
- [ ] **XP Gem (medium):** Mid ping, 0.08s, chime
- [ ] **XP Gem (large):** Low ping, 0.1s, chime
- [ ] **Gold pickup:** Coin jingle, 0.3s, register
- [ ] **Health pickup:** Heal sound, 0.3s, ascending

#### UI Sounds
- [ ] **Button tap:** Click, 0.05s, sharp
- [ ] **Menu open:** Whoosh, 0.2s, slide
- [ ] **Menu close:** Reverse whoosh, 0.2s
- [ ] **Level up:** Ascending chime, 0.5s, triumphant
- [ ] **Upgrade selected:** Confirm, 0.2s, positive
- [ ] **Evolution:** Epic chord, 0.6s, powerful
- [ ] **Build phase start:** Bell, 0.3s, alert
- [ ] **Tower placed:** Construct, 0.2s, mechanical
- [ ] **Tower upgrade:** Power up, 0.3s, ascending
- [ ] **Tower sell:** Cash register, 0.2s, coin
- [ ] **Pause:** Whoosh, 0.2s
- [ ] **Resume:** Un-pause, 0.2s

#### Boss Sounds
- [ ] **Boss roar:** Roar, 1.0s, menacing
- [ ] **Boss slam:** Impact, 0.4s, heavy
- [ ] **Boss death:** Epic sting, 1.5s, dramatic

### Music Tracks (looping OGG, ~2MB each)
**Synthesis:** Procedural loops via `scripts/synth_audio.py`

- [ ] **Menu theme:** Ambient, calm, 60s loop, layer: bass + pad
- [ ] **Gameplay theme:** Intense, driving, 90s loop, layers: drums + bass + lead, intensity increases at minutes 5, 10, 15
- [ ] **Boss theme:** Dramatic, heavy, 45s loop, layers: heavy drums + orchestral hits
- [ ] **Build phase theme:** Brief tension, 10s loop, layer: ambient pulse
- [ ] **Victory theme:** Triumphant, 8s, ascending melody
- [ ] **Game over theme:** Somber, 6s, descending melody

### Animation Timelines
**Frame rate:** 12 FPS for all sprites (retro feel)

#### Player (Commander example)
- Idle: Frame 0 (0.5s) → Frame 1 (0.5s) → loop (1s total)
- Run: Frame 0→1→2→3 (0.083s each) → loop (0.33s total)
- Attack: Frame 0→1→2 (0.1s each) → back to Idle (0.3s total)
- Death: Frame 0→1→2→3 (0.1s each) → fade (0.4s total)

#### Zombie example
- Idle: Single frame (static)
- Walk: Frame 0→1→2→3 (0.083s each) → loop (0.33s total)
- Attack: Frame 0→1 (0.15s each) → back to Walk (0.3s total)
- Death: Frame 0→1→2 (0.1s each) → fade (0.3s total)

---

## 21. STATUS EFFECT SYSTEM

### Effect Types & Mechanics

#### BURN (Damage Over Time)
- **Damage:** 5 HP per second (0.5s ticks = 2.5 damage)
- **Duration:** 3 seconds (6 ticks)
- **Total Damage:** 15 HP
- **Visual:** Red tint, flame particles (2 per frame), red damage numbers
- **Source:** Fire weapons (Rocket Launcher, Fireball)
- **Stacking:** No (refreshes duration)
- **Purge:** No (cannot be cleansed early)

#### POISON (Slow + DoT)
- **Damage:** 3 HP per second (0.5s ticks = 1.5 damage)
- **Slow:** 30% movement speed reduction
- **Duration:** 4 seconds (8 ticks)
- **Total Damage:** 12 HP
- **Visual:** Green tint, green drip particles, green damage numbers
- **Source:** Poison weapons (Poison Tower)
- **Stacking:** No (refreshes duration)
- **Purge:** Healing Pulse cleanses poison

#### FREEZE (Stop Movement)
- **Effect:** Complete movement stop (0 px/s)
- **Duration:** 2 seconds
- **Visual:** Blue tint, ice overlay (semi-transparent white), shakes slightly
- **Source:** Ice weapons (Frost Nova, Frost Tower)
- **Stacking:** No (refreshes duration)
- **Purge:** No
- **Damage:** Frozen enemies take +20% damage from all sources

#### STUN (Prevent Actions)
- **Effect:** Prevents attacks and special abilities
- **Duration:** 1.5 seconds
- **Visual:** Star effect above head (16x16px star sprite, rotates)
- **Source:** Lightning weapons (Lightning Orb, Tesla Coil)
- **Stacking:** No (refreshes duration)
- **Purge:** No
- **Note:** Stunned enemies can still move (unless also Frozen)

#### SLOW ATTACK (Reduced Attack Speed)
- **Effect:** Reduces attack/cooldown speed by 50%
- **Duration:** 3 seconds
- **Visual:** Purple tint
- **Source:** Certain boss abilities
- **Stacking:** No (refreshes duration)
- **Purge:** No

#### KNOCKBACK (Physics Push)
- **Effect:** Push enemy back 20px on hit
- **Direction:** Away from damage source
- **Recovery:** 0.3s before resuming chase behavior
- **Visual:** Sprite slides (no interpolation), snaps to position
- **Source:** All melee weapons, explosions
- **Stacking:** Yes (can be knocked back multiple times)
- **Immunity:** Brute, Shielder, Boss have 50% knockback resistance

### Status Application Rules
- **Player:** Cannot receive status effects (only HP damage)
- **Enemies:** Can receive all status effects
- **Elites:** 50% status duration reduction
- **Bosses:** Immune to Stun, Freeze, Knockback (only affected by Burn/Poison)
- **Towers:** Immune to all status effects

### Status Priority & Conflicts
- **Movement modifiers:** Freeze > Slow > Poison
  - If Frozen, Slow/Poison have no effect on movement
  - If Poisoned + Slowed, use slower of the two (40% speed)
- **Damage over time:** Burn + Poison stack (deal both simultaneously)
- **Hard CC (Crowd Control):** Stun > Freeze
  - If Stunned, cannot Freeze
  - If Frozen, cannot Stun

### Status Effect Timing
```
0.0s: Status applied
0.5s: First tick (for DoT effects)
1.0s: Second tick
1.5s: Third tick
...
N.s: Final tick → Status expires
```

### Visual Feedback Timeline
- **0.0s:** Apply visual tint + particle effect
- **Every 0.5s:** Spawn damage number (for DoT)
- **Every 0.1s:** Update particles
- **Status expire:** Remove tint + particles

---

## 22. ENDGAME & RETENTION LOOP

### What Happens After Chapter 5?

Once player clears Chapter 5 (Final Bunker - Overlord):

#### Immediate Rewards
- **Overlord Defeated:** Screen shows "VICTORY" with epic particle effect
- **Rewards:**
  - Gold: 1000-2000 (based on performance)
  - New hero unlock: Shielder (if not already unlocked)
  - Achievement: "Survivor" unlocked

#### New Game+ Mode
- **Unlock:** Clear Chapter 5 once
- **Mechanic:** Restart from Chapter 1 with increased difficulty
- **Benefits:**
  - Enemy HP +25%, Damage +10%, Spawn rate +15%
  - Gold drops +50%, XP +25%
  - Tower placement limit +1 (max 9 instead of 8)
  - New passive items drop (tier 2 passives with +50% effect)
- **Progression:** Complete all 5 chapters again → unlock New Game++ (another difficulty tier)
- **Max Tier:** New Game+++++ (5 completions total, 5 difficulty tiers)

### Meta-Progression (Permanent Upgrades)
- **Already implemented:** Section 10.1 covers permanent upgrades
- **Expansion post-Ch.5:**
  - **New upgrade tiers:** Unlock levels 11-20 for existing upgrades
    - Max HP +20 → upgrade to +30 at level 11
    - Cost escalates: Level 11 = 50,000 gold, Level 20 = 500,000 gold
  - **New upgrades:**
    - **Elite Spawn Rate +5%:** 5 levels, 100,000 gold each
    - **Boss Damage -10%:** 3 levels, 200,000 gold each
    - **Scrap Drop +20%:** 5 levels, 150,000 gold each
    - **Evolution Chance Boost:** (when weapon at Lv.5, 20% chance to evolve without catalyst) - 3 levels, 300,000 gold each

### Daily Challenges
- **Unlock:** After completing Chapter 3
- **Format:** 3 daily challenges, rotate at midnight UTC
- **Examples:**
  - "Kill 1000 Zombies" - Reward: 500 gold
  - "Survive 10 minutes in Chapter 2" - Reward: 1000 gold
  - "Use 3 different weapons in one match" - Reward: 1 passive item unlock
  - "Defeat a boss without taking damage" - Reward: 2000 gold
  - "Place 10 towers in one match" - Reward: 750 gold
- **Streak bonus:** Complete 7 days in a row → bonus 5000 gold on day 7
- **Leaderboard:** Global ranking for total challenge completions

### Weekly Challenges
- **Unlock:** After completing Chapter 5
- **Format:** 1 weekly challenge, harder than daily
- **Examples:**
  - "Complete New Game+ with <5 deaths" - Reward: Exclusive hero skin
  - "Deal 1,000,000 damage in one match" - Reward: 10,000 gold
  - "Kill the Overlord in under 3 minutes" - Reward: Legendary weapon unlock
- **Reward:** Exclusive cosmetics, large gold sums, premium currency (gems)

### Leaderboards
- **Categories:**
  - Most kills (all-time)
  - Longest survival (single match)
  - Fastest Chapter 5 clear
  - Most gold earned (all-time)
  - Most boss kills (all-time)
- **Scope:** Global (all players)
- **Update:** Real-time
- **Rewards:** Top 10 each season (1 month) receive exclusive avatar borders

### Achievement System
- **Categories:**
  - **Combat:** "Kill 10,000 enemies", "Kill 100 elites", "Kill 50 bosses"
  - **Weapons:** "Evolve all 12 weapons", "Deal 1M damage with one weapon"
  - **Towers:** "Place 100 towers", "Kill 1000 enemies with towers"
  - **Heroes:** "Unlock all heroes", "Win with all 6 heroes"
  - **Chapters:** "Complete all chapters", "Complete New Game+++++"
  - **Secret:** Hidden achievements (e.g., "Die 100 times", "Play for 24 hours total")
- **Rewards:** Gold, cosmetics, achievements badges on profile

### Retention Loop Design
**Daily play loop:**
```
Login → Claim daily reward → Check daily challenges → 
Play match (chase challenge progress) → Earn gold → 
Upgrade permanent stats → Try new build → Repeat
```

**Weekly play loop:**
```
Monday-Friday: Grind dailies, upgrade stats, test builds
Saturday: Attempt weekly challenge (harder, better rewards)
Sunday: Review week progress, prepare for new challenges
```

**Monthly play loop:**
```
Week 1: Push leaderboard rankings
Week 2: Grind gold for expensive upgrades
Week 3: Attempt New Game+ clears
Week 4: Prepare for end-of-month leaderboard settlement (rewards distributed)
```

### Season System (Post-Launch)
- **Duration:** 1 month per season
- **Season Pass:** 100 tiers, free + premium tracks
  - Free track: Gold, scrap, basic cosmetics
  - Premium track ($4.99): Gems, exclusive skins, heroes, weapon evolutions
- **Season Theme:** Each season introduces:
  - 1 new hero
  - 2 new weapons
  - 1 new chapter
  - Seasonal challenge (e.g., "Survival mode")
- **End of Season:** Leaderboard settlement, rewards distributed, new season starts

---

## 23. BALANCE TARGETS

### 23.1 Per-Match Metrics
| Metric | Target | Acceptable Range |
|--------|--------|------------------|
| Match duration (death) | 5-8 min (new player) | 3-15 min |
| Match duration (victory) | 12-15 min | 10-15 min |
| Level reached | 12-18 | 8-25 |
| Kills per match | 200-500 | 100-1000 |
| Action rate | ≥2/s (weapon fires + movement) | 1-5/s |
| Death cause | Mostly boss waves or overwhelm | — |

### 23.2 Difficulty Curves
See `scripts/simulate_balance.py` for the mathematical model. Target win rate is 40-60% on Normal difficulty.

### 23.3 Weapon Balance Targets
| Stat | Early (Lv1) | Mid (Lv3) | Late (Evolved) |
|------|-------------|-----------|----------------|
| DPS per weapon | 30-50 | 80-120 | 200-400 |
| Total DPS (all weapons) | 30-50 | 200-400 | 600-1200 |
| Enemy HP at min 1 | 20-30 | — | — |
| Enemy HP at min 10 | 200-300 | — | — |
| Enemy HP at min 15 | 500-800 | — | weapons: 600-1200 DPS vs 500-800 HP enemies = 1-2 kills per second per weapon |

Balance conclusions from the simulation model:
- Late game: 6 weapons × 200 DPS = 1200 DPS vs enemies spawning at 10/s × 500 HP = need to kill faster than spawn.
- All weapons have clear upgrade paths to evolved forms.
- Evolutions feel powerful but not game-breaking (3-4x base DPS).

### 23.4 Economy Balance
| Resource | Early Game | Mid Game | Late Game |
|----------|-----------|----------|-----------|
| Gold per match | 100-300 | 300-600 | 500-1000 |
| Scrap per match | 50-100 | 100-200 | 150-300 |
| Upgrade cost (permanent) | 500-1000 | 2000-5000 | 10000+ |
| Tower cost | 50-100 | 100-150 | Upgrades 200-450 |

---

## 24. SAVE SYSTEM

### 24.1 Saved Data
```json
{
  "version": 1,
  "gold": 5420,
  "gems": 120,
  "heroesUnlocked": ["commander", "berserker"],
  "permanentUpgrades": {
    "maxHp": 3,
    "moveSpeed": 1,
    "damage": 2,
    "pickupRange": 0,
    "extraLife": 0,
    "xpGain": 1,
    "goldFind": 0,
    "cardioFit": 0,
    "towerDiscount": 0
  },
  "chapterProgress": {
    "ch1": {"cleared": true, "bestTime": 720, "stars": 3},
    "ch2": {"cleared": false, "bestTime": 0, "stars": 0}
  },
  "settings": {
    "musicVolume": 0.7,
    "sfxVolume": 0.8,
    "haptics": true,
    "quality": "auto"
  },
  "stats": {
    "totalKills": 12450,
    "totalMatches": 45,
    "totalPlaytime": 28800,
    "totalGoldEarned": 24000
    ...
  }
}
```

 schema: `data class SaveData(...)` serialized to JSON via kotlinx.serialization
- Storage: DataStore (Preferences) — async, reliable, no SQLite needed
- Auto-save: After every match, on settings change, on purchase
- Cloud save: Future (post-MVP)

---

## 25. ANALYTICS EVENTS

| Event | When | Params |
|-------|------|--------|
| match_start | Match begins | chapterId, heroId, upgradesSnapshot |
| match_end | Match ends (win/death) | chapterId, heroId, duration, level, kills, goldEarned, deathCause |
| level_up | Player levels up in-match | levelNumber, upgradeChosen, weaponType |
| weapon_evolved | Weapon evolves | weaponType, evolvedForm, matchTime |
| tower_placed | Tower placed | towerType, cost, matchTime, totalTowers |
| boss_killed | Boss defeated | bossName, matchTime, levelAtKill |
| ad_watched | Rewarded ad | adType, rewardType, placement |
| purchase | IAP | sku, price, currency |
| hero_unlocked | Hero unlocked | heroId, method (gold/iap) |
| chapter_unlocked | Chapter unlocked | chapterId, method |
| upgrade_purchased | Permanent upgrade | upgradeId, level, cost |
| daily_challenge | Daily challenge completed | challengeId, reward |

---

## 26. DEVELOPMENT ROADMAP (Phased)

### 26.1 MVP Scope (First Playable — Week 1)
- [x] Core game loop: move, auto-attack, kill, XP, level up, choose upgrade
- [x] 3 weapons: Assault Rifle, Katana, Lightning Orb
- [x] 3 enemy types: Zombie, Runner, Brute
- [x] 1 hero: Commander
- [x] 1 chapter: Wasteland (15 min)
- [x] Leveling system with upgrade cards
- [x] Basic HP bar, XP bar, timer, joystick
- [x] Win/lose conditions
- [x] Basic SFX + 1 music track

### 26.2 Vertical Slice (Week 2)
- [x] All 12 weapons + evolutions
- [x] All 12 passive items
- [x] All 10 enemy types + boss
- [x] Tower system (6 types + upgrades)
- [x] 3 heroes
- [x] Build phase
- [x] Meta-progression (permanent upgrades)
- [x] Save system
- [x] Full audio
- [x] Settings menu

### 26.3 Launch Scope (Week 3)
- [x] 5 chapters + 5 bosses
- [x] All 6 heroes
- [ ] Monetization (rewarded ads + IAP) — tracked as #9, #120 (post-MVP)
- [ ] Daily challenges
- [ ] Analytics
- [x] Polishing pass: game feel, particles, screen shake
- [x] Performance optimization — object pooling, frustum culling, 60fps target (PR #127)

### 26.4 Post-Launch
- [ ] Battle Pass system
- [ ] Endless mode
- [ ] Daily rewards
- [ ] Cloud save
- [ ] New heroes, weapons, chapters (seasonal content)

---

## 27. ECS COMPONENT → TEMPLATE MAPPING

| Component | Template File | Notes |
|-----------|---------------|-------|
| Position, Velocity | `ecs_components.kt` | Base movement |
| Sprite | `ecs_components.kt` | Rendering |
| Health, Damage | `ecs_components.kt` | Combat core |
| Enemy + AI | `enemy_ai_system.kt` | ChaseBehavior + ShootBehavior + WaveBehavior |
| Weapon, Projectile | (NEW) `weapon_system.kt` | Needs new template |
| Pickup | (NEW) `pickup_system.kt` | Needs new template |
| Tower | (NEW) `tower_system.kt` | Needs new content |
| Passive | (NEW) `passive_system.kt` | Needs new content |
| StatusEffect | (NEW) `status_effect_system.kt` | Needs new content |
| PlayerStats | (NEW) `player_stats_component.kt` | Needs new content |
| Particle | `particle_system.kt` | Reuse existing |
| CameraFollow | `camera_system.kt`+ | Reuse existing |
| Lifetime | `ecs_components.kt` | Reuse existing |
| SquashStretch | `game_feel_system.kt` | Reuse existing |

### New Templates Needed (6)
1. `weapon_system.kt` — Auto-fire logic, targeting, projectile spawning, evolution tracking
2. `pickup_system.kt` — XP gem magnet, health pack, bomb, magnet, treasure
3. `tower_system.kt` — Tower placement, targeting, upgrade levels, scrap economy
4. `passive_system.kt` — Passive item stacking, stat recalculation
5. `status_effect_system.kt` — Burn/Poison/Freeze/Slow/Stun tick logic
6. `player_stats_component.kt` — Aggregated stats from base + permanent upgrades + passives

### Existing Templates Reused (7)
game_loop.kt, ecs_components.kt, game_canvas_screen.kt, camera_system.kt, enemy_ai_system.kt, particle_system.kt, game_feel_system.kt, audio_manager.kt, save_system.kt

### Existing Scripts Reused (3)
process_assets.py, synth_audio.py, run_visual_qa.py

### Existing Scripts Modified (1)
simulate_balance.py — Add survivor.io-style metrics (DPS curves, enemy scaling)

---

## 28. KEY DESIGN DECISIONS & RATIONALE

### Q: Why add towers to a bullet-heaven game?
**A**: Pure survivor.io clones are saturated. The tower layer creates strategic depth and differentiates us. It also:
- Gives meaning to the "build phase" breaks (breathing room between intense combat)
- Creates spatial strategy (where to place towers for max coverage)
- Adds a resource sink (scrap) separate from gold, creating dual economy

### Q: Why auto-aim weapons instead of manual aiming?
**A**: Mobile ergonomics. One thumb for movement, one thumb free for abilities/ultimates. This is proven by survivor.io/Vampire Survivors — reduces cognitive load, increases accessibility.

### Q: Why 6 weapon slots?
**A**: Enough variety for interesting builds, not so many that choices feel diluted. Survivor.io uses 6. Vampire Survivors uses 6-8. Our number is right.

### Q: Why evolution requires a specific passive?
**A**: Creates build planning. Player must think: "If I want the Minigun, I need Power Core passive AND Assault Rifle at Level 5." This adds depth beyond "always pick the highest DPS."

### Q: Why do players keep coming back to the next game?
**A**: The roguelite loop. Each match feels different because:
- Random upgrade cards (no two builds are identical)
- Random enemy spawns
- Random tower placements
- Random elite/boss modifiers
- Random pickup drops

### Q: Why not real-time multiplayer?
**A**: That's the MOBA/BR category we explicitly excluded. Our strength is single-player PvE that can be built 100% agentically without servers.

### Q: F2P with no energy system?
**A**: Yes. Energy systems hurt retention in single-player games. Revenue comes from rewarded ads (high-conversion) and IAP for cosmetics/convenience. No pay-to-win.

---

## 29. DIFFICULTY CURVE SPECIFICATION

### 29.1 Minute-by-Minute Progression (15-Minute Match)

#### Early Game (Minutes 0-3)
- **Goal:** Teach mechanics, low threat
- **Enemy Pool:** 70% Zombie, 30% Runner
- **Spawn Rate:** 1 enemy every 1.5 seconds
- **Enemy Stats:** Base HP and damage (no scaling applied)
- **Player Power:** 1 weapon, level 1-3
- **Difficulty:** 2/10 (very easy)
- **Expected Deaths:** 0% (new players should not die here)

#### Mid Game (Minutes 3-8)
- **Goal:** Increase pressure, introduce variety
- **Minute 3:** Add Brutes (15% of spawns)
- **Minute 5:** First boss wave (Alpha Beast)
  - Boss HP: 4000, spawns minions
  - Normal spawns pause for 10s
  - Build Phase: 10s break, place towers
- **Minute 6:** Add Spitters (10% of spawns)
- **Enemy Pool (min 6-8):** 40% Zombie, 30% Runner, 15% Brute, 15% Spitter
- **Spawn Rate:** Scales from 1.0s to 0.6s over 5 minutes
- **Enemy Stats:** HP ×1.5, Damage ×1.3
- **Player Power:** 3-4 weapons, level 5-8, 1-2 towers
- **Difficulty:** 5/10 (moderate)
- **Expected Deaths:** 10% (new players may die here)

#### Late Game (Minutes 8-12)
- **Goal:** High intensity, all enemy types
- **Minute 8:** Add Bombers (10% of spawns)
- **Minute 10:** Second boss wave (Alpha Beast, stronger)
  - Boss HP: 6000, new attack unlocked
  - Build Phase: 10s break
- **Minute 10.5:** Add Healers (5% of spawns), Shielders (5%)
- **Enemy Pool (min 10-12):** 25% Zombie, 25% Runner, 20% Brute, 10% Spitter, 10% Bomber, 5% Healer, 5% Shielder
- **Spawn Rate:** 0.5s per spawn (2 enemies/second)
- **Elite Chance:** 10% (random glowing enemies, 3x stats)
- **Enemy Stats:** HP ×2.5, Damage ×1.8
- **Player Power:** 5-6 weapons, level 10-15, 3-5 towers (some evolved)
- **Difficulty:** 7/10 (hard)
- **Expected Deaths:** 40% (many players die here)

#### End Game (Minutes 12-15)
- **Goal:** Maximum chaos, survival test
- **Minute 12:** Add Flyers (5% of spawns)
- **Minute 15:** Final boss wave (Alpha Beast, max power)
  - Boss HP: 10,000, all attacks unlocked
  - Build Phase: 10s break
- **Minute 15+ (if alive):** Endless mode
  - Spawn rate continues scaling
  - Enemy HP ×3.5, Damage ×2.5
  - Elite Chance: 15%
  - No more bosses, just waves
- **Enemy Pool (min 12-15):** 20% Zombie, 20% Runner, 20% Brute, 15% Spitter, 10% Bomber, 5% Healer, 5% Shielder, 5% Flyer
- **Spawn Rate:** 0.4s per spawn (2.5 enemies/second)
- **Elite Chance:** 12% (growing to 15%)
- **Enemy Stats:** HP ×3.0, Damage ×2.2
- **Player Power:** 6 weapons (all slots full), level 15-20, 5-8 towers (multiple evolved)
- **Difficulty:** 9/10 (very hard)
- **Expected Deaths:** 80% (only skilled players survive)

### Scaling Formulas (Per Minute)

#### Enemy HP Scaling
```kotlin
// Formula: baseHP × (1 + minutesElapsed × 0.15)
// Minute 0: 100% HP
// Minute 5: 175% HP (×1.75)
// Minute 10: 250% HP (×2.5)
// Minute 15: 325% HP (×3.25)

val hpMultiplier = 1.0 + (minutesElapsed * 0.15)
val scaledHP = baseHP * hpMultiplier
```

#### Enemy Damage Scaling
```kotlin
// Formula: baseDamage × (1 + minutesElapsed × 0.08)
// Minute 0: 100% damage
// Minute 5: 140% damage (×1.4)
// Minute 10: 180% damage (×1.8)
// Minute 15: 220% damage (×2.2)

val damageMultiplier = 1.0 + (minutesElapsed * 0.08)
val scaledDamage = baseDamage * damageMultiplier
```

#### Spawn Rate Scaling
```kotlin
// Formula: baseInterval / (1 + minutesElapsed × 0.20)
// Clamped to minimum 0.3s per spawn
// Minute 0: 1.0s interval (1 enemy/s)
// Minute 5: 0.5s interval (2 enemies/s)
// Minute 10: 0.33s interval (3 enemies/s)
// Minute 15: 0.3s interval (3.33 enemies/s, capped)

val rateMultiplier = 1.0 + (minutesElapsed * 0.20)
val scaledInterval = max(0.3, baseInterval / rateMultiplier)
```

#### Elite Chance Scaling
```kotlin
// Formula: 5% + minutesElapsed × 0.5%
// Minute 0: 5% elite chance
// Minute 5: 7.5% elite chance
// Minute 10: 10% elite chance
// Minute 15: 12.5% elite chance

val eliteChance = 0.05 + (minutesElapsed * 0.005)
```

### Boss Spawn Timings
- **Minute 5:** First boss (Chapter 1 boss)
  - Spawns at exactly 5:00
  - Normal spawns pause 10s before and 5s after
- **Minute 10:** Second boss (same as first, but stronger)
  - Spawns at exactly 10:00
  - Normal spawns pause 10s before and 5s after
- **Minute 15:** Final boss (max power)
  - Spawns at exactly 15:00
  - Normal spawns pause 10s before and 5s after

### Difficulty Curve Graph (Visual Representation)
```
Difficulty (1-10)
10|                    ╱╲
  |                  ╱    ╲
  |                ╱        ╲
  |              ╱            ╲
  |            ╱                ╲
  |          ╱                    ╲
  |        ╱                        ╲
  |      ╱                            ╲
  |____╱________________________________╲____
  0    3    5    8    10   12   15   20 (minutes)
       Easy Mid  Hard End  Endless
```

### Per-Chapter Difficulty Modifiers
Different chapters have different difficulty curves:

#### Chapter 1 (Wasteland) - Easy
- Base difficulty: 1.0x (reference)
- Duration: 15 minutes
- Recommended for: New players
- Expected clear rate: 60%

#### Chapter 2 (Toxic Swamp) - Medium
- Base difficulty: 1.2x (enemies +20% HP)
- Poison clouds deal 5 HP/s if standing in them
- Duration: 15 minutes
- Recommended for: Players who cleared Ch.1
- Expected clear rate: 40%

#### Chapter 3 (Abandoned City) - Hard
- Base difficulty: 1.4x (enemies +40% HP)
- Narrow corridors (enemies funnel, harder to dodge)
- Duration: 15 minutes
- Recommended for: Experienced players
- Expected clear rate: 25%

#### Chapter 4 (Underground Lab) - Very Hard
- Base difficulty: 1.6x (enemies +60% HP)
- Random laser barriers (deal 20 damage, toggle every 5s)
- Duration: 15 minutes
- Recommended for: Skilled players
- Expected clear rate: 15%

#### Chapter 5 (Final Bunker) - Extreme
- Base difficulty: 1.8x (enemies +80% HP)
- crushing walls (mechanic: must keep moving or take 50 damage)
- Duration: 20 minutes (5 minutes longer!)
- Recommended for: Expert players
- Expected clear rate: 5%

### Player Power Curve vs Enemy Power Curve
```
Power Level (relative)
100|                       Player Power (6 weapons, evolved)
  |                      /
  |                    /
  |                  /
  |      Player    /  (3 weapons, mid-game)
  |     (1 wpn)  /
  |           /
  |         / 
  |       /   Enemy Power (scales linearly)
  |_____/
  0    5    10   15   20 (minutes)
  
Critical Points:
- Minute 0-3: Player > Enemy (easy learning)
- Minute 5-8: Player ≈ Enemy (balanced, first boss)
- Minute 10-12: Player < Enemy (hard, second boss)
- Minute 15+: Player > Enemy (if evolved weapons, else player dies)
```

### Balance Validation
The difficulty curve ensures:
1. **Early game** is forgiving (teach mechanics without punishment)
2. **Mid game** ramps up smoothly (introduce new enemy types gradually)
3. **Late game** is challenging (requires multiple evolved weapons)
4. **End game** is brutal (only skilled + optimized builds survive)
5. **Boss waves** are climaxes (pause spawns, focus on boss)
6. **Build phases** are breathers (10s break every 3 minutes)
7. **Power fantasy** is realized (evolved weapons feel overpowered)

---

## 30. GLOSSARY

| Term | Definition |
|------|------------|
| **Bullet Heaven** | Genre where player auto-attacks while dodging hordes of enemies. Inverse of bullet hell. |
| **Roguelite** | Procedural upgrades each run, meta-progression persists. Not full roguelike (permadeath) but similar feel. |
| **ECS** | Entity-Component-System architecture. Entities = IDs, Components = data, Systems = logic. |
| **DPS** | Damage Per Second. |
| **AoE** | Area of Effect. |
| **DoT** | Damage over Time. |
| **PBAoE** | Point-Blank Area of Effect (centered on player). |
| **Pierce** | Projectile passes through enemies instead of being consumed. |
| **Knockback** | Force applied to entity on hit, pushing it away. |
| **Scrap** | In-match currency for towers. Not persistent. |
 Scrap. Not persistent. Not same as gold (meta currency).

---

## 31. CHECKPOINT #1 — CONCEPT SIGN-OFF

**This is a HARD GATE. No development begins until the user approves this concept.**

### What This Spec Defines
- Core game loop (move, auto-attack, level up, survive)
- 12 weapons with evolutions
- 12 passive items
- 6 tower types
- 10 enemy types + bosses
- Meta-progression system
- Monetization model
- ECS architecture
- 16-system priority order
- 6 new templates needed
- 7 existing templates reused
- Full balance targets

### What Happens After Approval
1. Provision repo (GitHub, Issue-First, TDD, Branch Protection)
2. Setup build.gradle with all dependencies
3. Begin Phase 2 (UI Design) → Phase 3 (Assets) → Phase 4 (Development)
4. All via PRs with CI gates (build, test, lint, coverage)

---

**Status: AWAITING USER APPROVAL**

*"Survivor.io clone? No. Survivor.io evolution — with towers."*
