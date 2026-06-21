# Survivor TD

> **Bullet-heaven tower defense roguelite.** Survivor.io-style auto-combat + strategic tower building layer.
> Built fully agentically with Kotlin + Jetpack Compose Canvas + Fleks ECS + dyn4j physics.

## Quick Links
- 📋 [Game Design Spec](GAME_SPEC.md)
- 🔒 [Enforcement Config](.project/enforcement.yaml)
- 🏗️ [CI Workflow](.github/workflows/ci.yml)

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose Canvas (drawBehind/drawWithCache) |
| Architecture | Fleks ECS |
| Physics | dyn4j 5.0.2 |
| Audio | SoundPool (SFX) + Media3/ExoPlayer (BGM) |
| Screenshots | Roborazzi |
| Visual QA | AGY / Gemini Vision |

## Build
```bash
# Windows (via WSL)
/mnt/c/Windows/System32/cmd.exe /c "cd /d D:\Projects\survivor-td && set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot&& set ANDROID_HOME=D:\Android_Data\Local\Sdk&& gradlew.bat assembleDebug --no-daemon"
```

## Development Workflow
This project follows the [Default Project Workflow](https://github.com/xarlord/survivor-td):
Issue-First → Feature Branch → TDD → PR → CI Gates → Merge

- **No direct pushes to main**
- **All changes require an issue reference**: `feat(#NN): description`
- **TDD enforced**: tests must exist for all changed source files
- **Branch protection**: enforce_admins = true (NO bypass possible)
