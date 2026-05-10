# Agent Guide

This is the active Android Studio project for the LEGO MINDSTORMS AI Creator app.

## Project Role

This app is the Android operator console for the `lego-mindstorms-mcp` repo.

It should become:

- a 51515 robot fleet/profile browser
- a safe probe runner
- a local dataset recorder
- an on-device classifier surface
- a later BLE/Pybricks/LEGO SPIKE hub controller

## Canonical Paths

Active Android app:

```text
C:\Users\Eoin\AndroidStudioProjects\MindstormsAICreator
```

MCP/data repo:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp
```

The repo-side Android scaffold is not the canonical app:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp\android\robot-inventor-app
```

Use it only as reference unless the user explicitly says otherwise.

## First Hardware Target

Build for LEGO MINDSTORMS Robot Inventor 51515 first.

The first app experience should center on:

- Blast
- Charlie
- Gelo
- M.V.P.
- Tricky

Profiles are available in:

```text
app\src\main\assets\robot_profiles_51515.json
```

## Engineering Rules

- Prefer Kotlin and Jetpack Compose.
- Keep `RobotTransport` as the transport abstraction.
- Keep `SimulatedTransport` working while BLE is being developed.
- Store probe sessions in a schema-compatible shape before adding ML.
- Add visible stop/abort controls before any real motor command path.
- Keep data export explicit and user-triggered.

## Build And Check

Use:

```powershell
.\gradlew.bat :app:assembleDebug
```

If Java is missing:

```powershell
java -version
echo $env:JAVA_HOME
```

Do not change Gradle plugin versions unless the current versions are the build blocker.

## Next Code Tasks

1. Load profile asset in `MainActivity.kt`.
2. Show selected robot details.
3. Show per-port role list.
4. Add simulated probe run screen.
5. Add JSON export for probe sessions.
6. Add BLE scan after simulated profile/probe flow is stable.

