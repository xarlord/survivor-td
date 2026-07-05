# Survivor TD - Visual Design Specification
**Theme:** Gothic Neon (Gothic elements merged with high-contrast, glowing neon accents)

This document provides a production-ready visual guide and Jetpack Compose specifications for the 5 screens of **Survivor TD**. It translates visual design choices directly into clean Compose APIs (Colors, Typography, Layouts, and Modifiers) that are optimized for high-density Android devices.

---

## 1. Visual Mockups

Below is the high-fidelity design carousel showing the mockups generated for all 5 game screens.

```carousel
![1. Title Screen](file:///C:/Users/plner/.gemini/antigravity-cli/brain/233e2d0c-9e39-48b4-8864-c51ae78d7b7e/title_screen_1783246907927.jpg)
<!-- slide -->
![2. Character Select](file:///C:/Users/plner/.gemini/antigravity-cli/brain/233e2d0c-9e39-48b4-8864-c51ae78d7b7e/character_select_1783246918947.jpg)
<!-- slide -->
![3. In-Game HUD](file:///C:/Users/plner/.gemini/antigravity-cli/brain/233e2d0c-9e39-48b4-8864-c51ae78d7b7e/ingame_hud_1783246929565.jpg)
<!-- slide -->
![4. Pause Menu](file:///C:/Users/plner/.gemini/antigravity-cli/brain/233e2d0c-9e39-48b4-8864-c51ae78d7b7e/pause_menu_1783246939543.jpg)
<!-- slide -->
![5. Game Over](file:///C:/Users/plner/.gemini/antigravity-cli/brain/233e2d0c-9e39-48b4-8864-c51ae78d7b7e/game_over_1783246948798.jpg)
```

---

## 2. Color Palette (Gothic Neon)

The Gothic Neon palette pairs an obsidian background with high-intensity primary glows and premium gold trims.

| Color Name | Hex Code | Compose Representation | UI Role / Usage |
| :--- | :--- | :--- | :--- |
| **Obsidian Dark** | `#0D0B10` | `Color(0xFF0D0B10)` | Core screen backgrounds, dark canvas panels. |
| **Gothic Charcoal** | `#1C1921` | `Color(0xFF1C1921)` | Card backgrounds, default button bases. |
| **Crimson Fire** | `#FF1F44` | `Color(0xFFFF1F44)` | HP bar, primary buttons, Game Over "DEFEATED" glow. |
| **Neon Violet** | `#A832FF` | `Color(0xFFA832FF)` | Secondary UI elements, weapon slot borders, active selectors. |
| **Gilded Gold** | `#FFD700` | `Color(0xFFFFD700)` | Gold text, level/score indicators, premium card borders. |
| **Neon Green** | `#39FF14` | `Color(0xFF39FF14)` | XP progress bar, system success states, "RESUME" buttons. |
| **Pure Glow White** | `#F3EFF7` | `Color(0xFFF3EFF7)` | High-readability labels, timer digits, base text. |

> [!TIP]
> To implement the neon glow effect in Jetpack Compose, use custom modifier extensions with blurred shadows or multiple layers of drawing operations on `drawBehind {}`.

---

## 3. Typography Guide

Typography uses a combination of a gothic display font for titles/headers and a highly readable geometric sans-serif for stats and numbers.

* **Primary Font Family (Gothic display):** `Cinzel` or `Cinzel Decorative` (Google Fonts)
* **Secondary Font Family (UI/Numbers):** `Outfit` or `Inter` (Google Fonts)

### Typographic Styles

```kotlin
val SurvivorTypography = Typography(
    // Large Titles: "SURVIVOR TD", "DEFEATED"
    displayLarge = TextStyle(
        fontFamily = Cinzel,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 2.sp,
        color = Color(0xFFFF1F44) // Crimson glow
    ),
    // Card titles / Character names: "Alaric the Undead"
    headlineMedium = TextStyle(
        fontFamily = Cinzel,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFFFFD700) // Gilded Gold
    ),
    // HUD Stats, timer: "04:20"
    titleLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color(0xFFF3EFF7) // Pure White
    ),
    // Buttons: "START GAME", "RETRY"
    labelLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
        color = Color(0xFFF3EFF7)
    ),
    // Stat details & values
    bodyMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Color(0xFFB0A2BC) // Soft Muted Violet
    )
)
```

---

## 4. Button Design System

Buttons feature robust scaling animations and intense glow layers.

### Component Implementation

```kotlin
@Composable
fun GothicNeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowColor: Color = Color(0xFFFF1F44),
    borderColor: Color = Color(0xFFFFD700)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale on press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .background(
                color = if (enabled) Color(0xFF1C1921) else Color(0xFF1C1921).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = if (enabled) borderColor else Color.DarkGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 14.dp, horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color(0xFFF3EFF7) else Color.Gray
        )
    }
}
```

### Visual Specifications
- **Default State:** Obsidian background (`#1C1921`), `2.dp` Gold (`#FFD700`) border, Crimson glow shadow (`#FF1F44`).
- **Pressed State:** Scale factor `0.95x`, glow intensifies, background shifts slightly lighter.
- **Disabled State:** Opacity `50%`, border shifts to gray, shadows/glows disabled.

---

## 5. Screen Layout Wireframes & Specs

### Title Screen
- **Background:** `Color(0xFF0D0B10)` with a subtle radial gradient radiating Crimson from the center.
- **Logo:** Nested in a `Column` at the top with `Spacer(modifier = Modifier.height(80.dp))` spacing. Uses glow text shadow.
- **Play Button:** Center-bottom layout. Center aligned, bottom offset using a spacer of `120.dp`.
- **Settings Gear:** Placed inside a top-right `Box` with `24.dp` padding.

### Character Select Screen
- **Layout structure:**
  - `Title` at top ("SELECT CHAMPION").
  - `HorizontalPager` or `LazyRow` occupying the center block (`height(320.dp)`).
    - Selected card gets a `Modifier.border(width = 3.dp, color = Color(0xFFA832FF))` (Neon Violet) to highlight choice.
  - Detail block: An obsidian card showing properties (Name, stats).
  - Bottom: `GothicNeonButton("START GAME")` padded with `24.dp` from bottom edge.

### In-Game HUD Layout (Responsive Specifications)
The HUD spans the top and bottom of the Canvas container, layered via standard Compose `Box`.

```
+---------------------------------------------------------+
| [============= XP ProgressBar: Neon Green =============] |
+---------------------------------------------------------+
| [ HP Bar (Crimson) ]     [ Timer (04:20) ]  [ Score (G) ]|
|                                                         |
|                                                         |
|                     [ Gameplay Canvas ]                 |
|                                                         |
|                                                         |
+---------------------------------------------------------+
|  [Slot 1]  [Slot 2]  [Slot 3]  [Slot 4]  [Slot 5]  [Slot 6]|
+---------------------------------------------------------+
```

* **XP Progress Bar (Full Width):** `height(6.dp)` at the very top. Uses `Color(0xFF39FF14)`.
* **HP Health Bar:** `width(100.dp)`, `height(14.dp)` on the left side, padded `12.dp` from top-left.
* **Timer:** Center-aligned top. Uses digital `Outfit` monospace font to avoid spacing jitter.
* **Score & Wave:** Right-aligned top. Stacked column, padded `12.dp`.
* **Weapon Slots:** `6` slots arranged inside a `Row` at bottom. Spaced with `8.dp` margins. Height/Width: `48.dp` each (responsive).

---

## 6. HUD Sizing Relative to Density

To maintain readable sizes on different devices (ldpi to xxxhdpi), use fixed standard-density target dimensions and relative layout bounds:

| Element | Base Size (MDPI) | Target Size (XXHDPI) | Jetpack Compose Implementation |
| :--- | :--- | :--- | :--- |
| **XP Progress Bar** | `4dp` height | `4dp` (fixed DP) | `Modifier.fillMaxWidth().height(4.dp)` |
| **HP Bar** | `80dp` x `12dp` | `100dp` x `14dp` | `Modifier.width(100.dp).height(14.dp)` |
| **Weapon Item Slots** | `40dp` x `40dp` | `48dp` x `48dp` | `Modifier.size(48.dp)` |
| **Interaction Buttons**| `180dp` width | `220dp` width | `Modifier.widthIn(min = 200.dp)` |
| **HUD Padding** | `8dp` edge | `16dp` edge | `Modifier.padding(16.dp)` |

---

## 7. Icon Style Guide

- **Style:** Outlined vector art, bold styling.
- **Stroke Weight:** `2.5.dp` stroke thickness to make icons highly visible even on small mobile screens.
- **Corner Radius:** Subtle roundings (`4.dp` corners on interior details).
- **Effects:** A `Color.White` stroke with an underlying neon purple/crimson glow shadow layered using canvas-drawing.

---

## 8. Transitions & Animations

### Screen Navigation Transitions
Use Compose Navigation animation features to glide between screens:

```kotlin
composable(
    route = Screen.CharacterSelect.route,
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(400))
    },
    exitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(400)
        ) + fadeOut(animationSpec = tween(400))
    }
)
```

### Micro-Animations
1. **HP Pulse:** When HP falls below 20%, trigger a continuous pulse animation on the HP Bar border:
   `val pulseAlpha by animateFloatAsState(targetValue = if (lowHp) 0.3f else 1.0f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse))`
2. **XP Bar Fill:** Smoothen XP progress using a spring animation (`animateFloatAsState(targetValue = xpPercentage, animationSpec = spring())`).
3. **Weapon Slot Highlight:** When a weapon upgrades, draw a flash/particle animation on the active slot boundary using Compose Canvas custom drawing.
