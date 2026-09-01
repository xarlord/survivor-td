package com.survivortd.game.ui

enum class TutorialStepId {
    MOVEMENT,
    AUTO_ATTACK,
    LEVEL_UP,
    UPGRADES,
    TOWERS,
    PAUSE
}

data class TutorialStepSpec(
    val id: TutorialStepId,
    val icon: AppIcon,
    val title: String,
    val instruction: String
)

object TutorialContent {
    val steps = listOf(
        TutorialStepSpec(
            id = TutorialStepId.MOVEMENT,
            icon = AppIcon.MOVEMENT,
            title = "MOVE & DASH",
            instruction = "Drag the left-side joystick in any direction, including up and down. Double-tap it to dash."
        ),
        TutorialStepSpec(
            id = TutorialStepId.AUTO_ATTACK,
            icon = AppIcon.AUTO_ATTACK,
            title = "AUTO-ATTACK",
            instruction = "Weapons fire automatically at nearby enemies."
        ),
        TutorialStepSpec(
            id = TutorialStepId.LEVEL_UP,
            icon = AppIcon.LEVEL_UP,
            title = "LEVEL UP",
            instruction = "Collect XP gems to level up and gain upgrades."
        ),
        TutorialStepSpec(
            id = TutorialStepId.UPGRADES,
            icon = AppIcon.UPGRADES,
            title = "UPGRADES",
            instruction = "Earn gold from kills and spend it in Upgrades between runs."
        ),
        TutorialStepSpec(
            id = TutorialStepId.TOWERS,
            icon = AppIcon.TOWERS,
            title = "BUILD",
            instruction = "Build towers for extra firepower."
        ),
        TutorialStepSpec(
            id = TutorialStepId.PAUSE,
            icon = AppIcon.PAUSE,
            title = "PAUSE",
            instruction = "Pause to resume the run or quit to the menu."
        )
    )
}
