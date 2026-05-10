# Build Target

This repository is the public Android app project. Build from the repo root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

On macOS/Linux:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Do not document or commit user-specific absolute paths. If a task needs the MCP/data repo, use:

```powershell
$env:MINDSTORMS_MCP_REPO_DIR="<mcp-repo-root>"
```

## Current Project State

- Android namespace: `com.eoinedge.robotinventor`
- Application ID: `com.eoinedge.robotinventor`
- Main activity: `app/src/main/java/com/eoinedge/robotinventor/MainActivity.kt`
- Transport contract: `RobotTransport.kt`
- Simulated transport: `SimulatedTransport.kt`
- 51515 profile asset: `app/src/main/assets/robot_profiles_51515.json`

## Expected First Screen

The app should open directly to the usable fleet screen. It should show the 51515 robots, not a landing page.
