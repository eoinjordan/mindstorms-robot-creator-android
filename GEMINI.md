# Android Build Guide

This is the active public Android app repository.

Build from this repo root:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

## Current Goal

Build the first usable app for LEGO MINDSTORMS Robot Inventor 51515.

Start with the five official 51515 robots:

- Blast
- Charlie
- Gelo
- M.V.P.
- Tricky

The app should work first as:

- a 51515 fleet/profile browser
- a builder/debug session surface
- a simulated probe runner
- a dataset capture/export shell
- later, a BLE/Pybricks or LEGO SPIKE hub connection app

## Source Of Truth

The MCP/data/web/desktop repo contains schemas, server behavior, and source profile data:

```text
https://github.com/eoinjordan/mindstorms-robot-creator
```

For local automation, set:

```powershell
$env:MINDSTORMS_MCP_REPO_DIR="<mcp-repo-root>"
```

Local Android asset:

```text
app/src/main/assets/robot_profiles_51515.json
```

## Implementation Order

1. Keep the package namespace as `com.eoinedge.robotinventor`.
2. Preserve `RobotTransport.kt` as the app-facing transport contract.
3. Keep `SimulatedTransport` exposing the five 51515 robot devices.
4. Load `robot_profiles_51515.json` from assets and show profile details.
5. Keep Builder Session as the primary human-in-the-loop workflow.
6. Keep Probe Runner safe and simulated before real motor control.
7. Export probe sessions in a schema-compatible format.
8. Add BLE scanning/connection after the simulated flow is stable.

## Safety Rules

- Do not run real motors until the app has a visible stop control.
- Keep simulated mode available.
- Do not upload data to Edge Impulse or any cloud service without explicit user action.
- Do not hard-code one robot's port map into all robots.
- Charlie's profile is marked `needsConfirmation`; show that uncertainty in the UI.
