# Mindstorms Robot Creator — Android

Android companion app for [Mindstorms Robot Creator](https://github.com/eoinjordan/mindstorms-robot-creator).  
Build, code, and control LEGO MINDSTORMS robots from your phone.

**[⬇ Download APK](https://github.com/eoinjordan/mindstorms-robot-creator-android/releases/latest)**

## Features

- BLE + USB connection to MINDSTORMS hubs (EV3, 51515, SPIKE)
- AI-powered MicroPython code generator
- Builder session with live hub status and observation log
- Session history (Room database)
- Safe motor probe runner
- Voice keyword spotting (Edge Impulse WebAssembly)
- Telemetry graphs
- Simulated transport for offline testing
- Works offline — no cloud, no account required

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room database for session history
- Kotlinx Serialization + Coroutines
- Material 3 Adaptive layout (phone + tablet)

## Build

Requires Android Studio Ladybug or later, JDK 17+.

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # unsigned release APK
```

APKs output to `app/build/outputs/apk/`.

## License

MIT — see [LICENSE](LICENSE)


The MCP/data repo is:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp
```

## Current State

- Kotlin/Compose Android project.
- `RobotTransport` interface exists.
- `SimulatedTransport` exists and should stay working.
- 51515 profile asset is present at `app/src/main/assets/robot_profiles_51515.json`.
- The first target robot set is Blast, Charlie, Gelo, M.V.P., and Tricky.

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

If build fails because Java is unavailable, set `JAVA_HOME` to a JDK and retry.
On this machine, Android Studio's bundled JDK is documented in `docs/JAVA_SETUP.md`.

## Agent Docs

- `GEMINI.md`
- `AGENTS.md`
- `docs/BUILD_TARGET.md`
- `docs/51515_PROFILES.md`
- `docs/JAVA_SETUP.md`
