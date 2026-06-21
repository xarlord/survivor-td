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

###  evolution System (Detailed)
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

##  build phase.
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

 16 systems, deterministic tick order

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
    {"x": 960, "y":  spec" }
  ],
  "obstacles": [
    {"type": "crate", "x": 400, "y": 300, "hp": 30, "loot": "gold_50"},
    {"type": "wall", "x": 640, "y": 100, "width": 200, "height": 20}
  ],
  "bossTimeMinutes": [5, 10, 15],
  "enemyPool": {
    "0": {"zombie": 70, "runner": 30},
    "3": {"zombie": 50, "spawn_tower": 15},
    "5": {"z" "zombie": 40, "runner": 30, "brute": 15, "spitter": 15},
    "10": {"zombie": 30, "runner": 25, "brute": 20, "spitter": 10, "bomber": 10, "healer": 5},
    "15": {"zombie": 20, "runner": 20, "brHP": 20, "spitter": 15, "bomber": 10, "healer": 10, "shielder": 5}
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

###  Survivor TD, the "pickup magnet" ability pulls all gems toward the player.

---

## 16. AUDIO DESIGN (continued)

### 16.3 Audio Synthesis Pipeline
All audio synthesized programmatically via `scripts/synth_audio.py`:
- SFX: Square/sawtooth waves with envelopes, noise bursts for explosions
- Music: Layered synth (bass + drums + lead), procedural loops
- All files: Mono 22050Hz 16-bit OGG (SoundPool) or MP3 (BGM)
- Each SFX < 50KB, total audio budget < 3MB

---

## 17. BALANCE TARGETS

### 17.1 Per-Match Metrics
| Metric | Target | Acceptable Range |
|--------|--------|------------------|
| Match duration (death) | 5-8 min (new player) | 3-15 min |
| Match duration (victory) | 12-15 min | 10-15 min |
| Level reached | 12-18 | 8-25 |
| Kills per match | 200-500 | 100-1000 |
| Action rate | ≥2/s (weapon fires + movement) | 1-5/s |
| Death cause | Mostly boss waves or overwhelm | — |

### 15.2 Difficulty Curves
See `scripts/simulate_balance.py` for the mathematical model.

---

## 40-60% on Normal difficulty.

### 17.2 Weapon Balance Targets
| Stat | Early (Lv1) | Mid (Lv3) | Late (Evolved) |
|------|-------------|-----------|----------------|
| DPS per weapon | 30-50 | 80-120 | 200-400 |
| Total DPS (all weapons) | 30-50 | 200-400 | 600-1200 |
| Enemy HP at min 1 | 20-30 | — | — |
| Enemy HP at min 10 | 200-300 | — | — |
| Enemy HP at min 15 | 500-800 | — | weapons: 600-1200 DPS vs 500-800 HP enemies = 1-2 kills per second per weapon
- Late game: 6 weapons × 200 DPS = 1200 DPS vs enemies spawning at 10/s × 500 HP = need to kill faster than spawn

### 17.3 Economy Balance
| Resource | Early Game | Mid Game | Late Game |
|----------|-----------|----------|-----------|
| Gold per match | 100-300 | 300-600 | 500-1000 |
| Scrap per match | 50-100 | 100-200 | 150-300 |
| Upgrade cost (permanent) | 500-1000 | 2000-5000 | 10000+ |
| Tower cost | 50-100 | 100-150 | Upgrades 200-450 |

---

##  simulation.
4. All weapons have clear upgrade paths to evolved forms.
5. Evolutions feel powerful but not game-breaking (3-4x base DPS).

---

## 18. SAVE SYSTEM

### 18.1 Saved Data
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
  },
"
}
```

 schema: `data class SaveData(...)` serialized to JSON via kotlinx.serialization
- Storage: DataStore (Preferences) — async, reliable, no SQLite needed
- Auto-save: After every match, on settings change, on purchase
- Cloud save: Future (post-MVP)

---

## 19. ANALYTICS EVENTS

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

## 20. DEVELOPMENT ROADMAP (Phased)

### 20.1 MVP Scope (First Playable — Week 1)
- [ ] Core game loop: move, auto-attack, kill, XP, level up, choose upgrade
- [ ] 3 weapons: Assault Rifle, Katana, Lightning Orb
-  [ ] 3 enemy types: Zombie, Runner, Brute
- [ ] 1 hero: Commander
- [ ] 1 chapter: Wasteland (15 min)
- [ ] Leveling system with upgrade cards
- [ ] Basic HP bar, XP bar, timer, joystick
- [ ] Win/lose conditions
- [ ] Basic SFX + 1 music track

### 20.2 Vertical Slice (Week 2)
- [ ] All 12 weapons + evolutions
- [ ] All 12 passive items
-  [ ] All 10 enemy types + boss
- [ ] Tower system (6 types + upgrades)
- [ ] 3 heroes
- [ ] Build phase
- [ ] Meta-progression (permanent upgrades)
-  [ ] Save system
- [ ] Full audio
- [ ] Settings menu

### 20.3 Launch Scope (Week 3)
- [ ] 5 chapters + 5 bosses
- [ ] All 6 heroes
- [ ] Monetization (rewarded ads + IAP)
-  [ ] Daily challenges
-  [ ] Analytics
- [ ] Polishing pass: game feel, particles, screen shake
- [ ] Performance optimization
- roadmap. If you take the time to read through all this, congrats, that's dedication.

### 20.4 Post-Launch
- [ ] Battle Pass system
- [ ] Endless mode
-  [ ] Daily rewards
- [ ] Cloud save
-  iving | Reward |
|---------|--------|
| Endless mode | 50 gold per 5 min survived |

---

## 21. ECS Component → Template Mapping

| Component | Template File | Notes |
|-----------|---------------|-------|
| Position, Velocity | `ecs_components.kt` | Base movement |
| Sprite | `ecs_components.kt` | Rendering |
| Health, Damage | `ecs components.kt` | Combat core |
| Enemy + AI | `enemy_ai_system.kt` | ChaseBehavior + ShootBehavior + WaveBehavior |
| Weapon, Projectile | (NEW) `weapon_system.kt` | Needs new template |
| Pickup | (NEW) `pickup_system.kt` | Needs new template |
| Tower | (NEW) `tower_system.kt` | Needs new content |
| Passive | (NEW) `passive_system.kt` | Needs new content |
| StatusEffect | (NEW) §`status_effect_system.kt` | Needs new content |
| PlayerStats | (NEW) `player_stats_component.kt` | Needs new content |
| Particle | `particle_system.kt` | Reuse existing |
| CameraFollow | `camera_system.kt`+ | Reuse existing |
| Lifetime | `ecs_components.kt` | Reuse existing |
| SquashStretch | `game_feel_system.kt` | Reuse existing |

### New Templates Needed (6)
1. `weapon_system.kt` — Auto-fire logic, targeting, projectile spawning, evolution tracking
2. `pickup_system.kt` — XP gem magnet, health pack, bomb, magnet, treasure
3. `  `tower_system.kt` — Tower placement, targeting, upgrade levels, scrap economy
4. `passive_system.kt` — Passive item stacking, stat recalculation
5. `status_effect_system.kt` — Burn/Poison/Freeze/Slow/Stun tick logic
6. `player_stats_component.kt` — Aggregated stats from base + permanent upgrades + passives

### Existing Templates Reused (7)
game_loop.kt, ecs_components.kt, game_canvas_screen.kt, camera_system.kt, enemy_ai_system.kt, particle_system.kt, game_feel system.kt, audio_manager.kt, save_system.kt

### Existing Scripts Reused (3)
process_assets.py, synth_audio.py, run_visual_qa.py

### Existing Scripts Modified (1)
simulate_balance.py — Add survivor.io-style metrics (DPS curves, enemy scaling)

---

## 22. KEY DESIGN DECISIONS & RATIONALE

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

###  to the next game."
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

---

## 23. GLOSSARY

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

## 24. CHECKPOINT #1 — CONCEPT SIGN-OFF

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
