# Java Setup For Gradle

The project has an Android Studio bundled JDK available at:

```text
C:\Program Files\Android\Android Studio\jbr
```

If `.\gradlew.bat :app:assembleDebug` fails with:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

Run the build from this project with:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug --console=plain
```

Gemini should use the same `JAVA_HOME` setting when invoking Gradle from a shell.

