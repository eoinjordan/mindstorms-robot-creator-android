# 51515 Profiles

The active Android app starts with the five official LEGO MINDSTORMS Robot Inventor 51515 builds.

Profile asset:

```text
app\src\main\assets\robot_profiles_51515.json
```

Source repo:

```text
C:\Users\Eoin\git\lego-mindstorms-mcp\examples\profiles\51515
```

## Robots

| Robot | Kind | Port source | Notes |
| --- | --- | --- | --- |
| Blast | humanoid drive/action robot | Pybricks + local manual | High confidence. |
| Charlie | humanoid companion robot | local manual image inspection | Needs physical/app confirmation. |
| Gelo | quadruped walker | Pybricks + local manual | High confidence. |
| M.V.P. | modular vehicle platform | Pybricks basic buggy + local manual | High confidence for basic buggy mode. |
| Tricky | sports/kicker robot | Pybricks activity variants + local manual | High confidence by activity variant. |

## UI Requirements

Show these fields for each robot:

- name
- kind
- source
- confidence
- port list
- motor/sensor count
- warning if confidence is `needs-confirmation`

## Probe Requirements

The first probe runner can be simulated.

For each robot, generate a summary like:

- active motor ports
- sensor ports
- expected morphology class
- sample telemetry rows

Do not add real motor control until stop/abort is visible.

