# Gemini Build Guide

This is the active Android app project.

Canonical app path:

```text
C:\Users\Eoin\AndroidStudioProjects\MindstormsAICreator
```

Do not build the older scaffold under:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp\android\robot-inventor-app
```

That scaffold is reference material only. Build and edit this Android Studio project unless the user explicitly asks for repo-side scaffolding.

## Current Goal

Build the first usable app for LEGO MINDSTORMS Robot Inventor 51515.

Start with the five official 51515 robots:

- Blast
- Charlie
- Gelo
- M.V.P.
- Tricky

The app should first work as:

- a 51515 fleet/profile browser
- a simulated probe runner
- a dataset capture/export shell
- later, a BLE/Pybricks or LEGO SPIKE hub connection app

## Source Of Truth

Repo with MCP server, schemas, manuals, and profile data:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp
```

Important repo files:

- `examples/profiles/51515/*.json`
- `examples/manuals/51515-manual-index.json`
- `schemas/robot-profile.schema.json`
- `schemas/probe-session.schema.json`
- `docs/ANDROID_APP.md`
- `docs/sources/51515-profile-sources.md`

Local Android asset copied into this project:

```text
app\src\main\assets\robot_profiles_51515.json
```

## Build Commands

From this directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

If Gradle says Java is missing, install/configure a JDK or set `JAVA_HOME`.

This machine has Android Studio's bundled JDK at:

```text
C:\Program Files\Android\Android Studio\jbr
```

See `docs\JAVA_SETUP.md`.

## Immediate Implementation Order

1. Keep the package namespace as `com.eoinedge.robotinventor`.
2. Preserve `RobotTransport.kt` as the app-facing transport contract.
3. Make `SimulatedTransport` expose the five 51515 robot devices first.
4. Load `robot_profiles_51515.json` from assets and show profile details.
5. Add a robot detail screen with ports, sensors, source confidence, and next action.
6. Add a simulated probe button that emits `ProbeTelemetry`.
7. Add JSON export for a probe session matching the MCP repo schema.
8. Only then add BLE scanning/connection.

## Safety Rules

- Do not run real motors until the app has a visible stop control.
- Keep simulated mode as the default.
- Do not upload data to Edge Impulse or any cloud service without explicit user action.
- Do not hard-code one robot's port map into all robots.
- Charlie's profile is marked `needsConfirmation`; show that uncertainty in the UI.

## Coordination With Codex/MCP Repo

When profile data changes in the repo, sync it here:

```powershell
node C:\Users\Eoin\git\lego-mindstorms-mcp\scripts\sync-android-51515-assets.js
```

That script currently syncs the repo scaffold asset. If this Android Studio project is the target, prefer copying or regenerating `app\src\main\assets\robot_profiles_51515.json` here.
