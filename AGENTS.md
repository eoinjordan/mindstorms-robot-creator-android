# Agent Guide

This is the public Android app repository for the LEGO MINDSTORMS AI Creator app.

## Project Role

This app is the Android operator console for the Mindstorms Robot Creator project. It should support:

- a 51515 robot fleet/profile browser
- a human-in-the-loop builder session
- a safe probe runner
- a local dataset recorder
- an on-device classifier surface
- later BLE/Pybricks/LEGO SPIKE hub control

## Repository Boundaries

- Build this Android repo from its repo root.
- Use the MCP/data/web/desktop repo as an external source of schemas, profile data, and server behavior.
- If a local script needs the MCP repo, set `MINDSTORMS_MCP_REPO_DIR`.
- Do not commit local absolute paths, SDK paths, or `local.properties`.

## First Hardware Target

Build for LEGO MINDSTORMS Robot Inventor 51515 first:

- Blast
- Charlie
- Gelo
- M.V.P.
- Tricky

Profiles are available in:

```text
app/src/main/assets/robot_profiles_51515.json
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
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

If Java is missing:

```powershell
java -version
echo $env:JAVA_HOME
```

Do not change Gradle plugin versions unless the current versions are the build blocker.
