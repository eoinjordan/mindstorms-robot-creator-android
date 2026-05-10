# Build Target

Gemini and other agents should build this project:

```text
C:\Users\Eoin\AndroidStudioProjects\MindstormsAICreator
```

Use this command from that directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

Do not build this path unless the user explicitly asks for the repo scaffold:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp\android\robot-inventor-app
```

## Current Project State

- Android namespace: `com.eoinedge.robotinventor`
- Main activity: `app\src\main\java\com\example\mindstormsaicreator\MainActivity.kt`
- Transport contract: `RobotTransport.kt`
- Simulated transport: `SimulatedTransport.kt`
- 51515 profile asset: `app\src\main\assets\robot_profiles_51515.json`

## Expected First Screen

The app should open directly to the usable fleet screen. It should show the 51515 robots, not a landing page.

