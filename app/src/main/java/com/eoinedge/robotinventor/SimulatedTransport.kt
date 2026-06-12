package com.eoinedge.robotinventor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class SimulatedTransport : RobotTransport {
    private val simulatedDevices = listOf(
        RobotDevice("wedo2-milo", "Milo", "WeDo 2.0 Smart Hub", 82),
        RobotDevice("wedo2-kraz", "Kraz", "WeDo 2.0 Smart Hub", 80),
        RobotDevice("wedo2-custom", "WeDo 2.0 Custom", "WeDo 2.0 Smart Hub", 78),
        RobotDevice("51515-blast", "Blast", "Robot Inventor 51515", 88),
        RobotDevice("51515-charlie", "Charlie", "Robot Inventor 51515", 86),
        RobotDevice("51515-gelo", "Gelo", "Robot Inventor 51515", 90),
        RobotDevice("51515-mvp", "M.V.P.", "Robot Inventor 51515", 84),
        RobotDevice("51515-tricky", "Tricky", "Robot Inventor 51515", 91),
        RobotDevice("ev3-ev3rstorm", "EV3RSTORM", "EV3 Brick", 72),
        RobotDevice("ev3-gripp3r", "GRIPP3R", "EV3 Brick", 73),
        RobotDevice("nxt-alpha-rex", "Alpha Rex", "NXT Brick", 66),
        RobotDevice("nxt-tribot", "Tribot", "NXT Brick", 68),
        RobotDevice("rcx-pushbot", "Pushbot", "RCX IR Tower", 55),
        RobotDevice("sim-two-wheel-drive", "Simulated Two Wheel Drive Base", "M5Stack BaseX", 100),
        RobotDevice("sim-tracked-vehicle", "Simulated Tracked Vehicle", "M5Stack BaseX", 100),
        RobotDevice("sim-gripper", "Simulated Motorized Gripper", "M5Stack BaseX", 100)
    )

    override suspend fun scan(): List<RobotDevice> {
        delay(1000) // Simulate scan delay
        return simulatedDevices
    }

    override suspend fun connect(deviceId: String): RobotConnection {
        delay(500)
        return object : RobotConnection {
            override val deviceId: String = deviceId
            override suspend fun disconnect() {
                // No-op
            }
        }
    }

    override suspend fun describe(): RobotDescription {
        return RobotDescription(
            ports = listOf(
                RobotPort("A", "motor"),
                RobotPort("B", "motor"),
                RobotPort("C", "motor or sensor"),
                RobotPort("1", "legacy sensor"),
                RobotPort("2", "legacy sensor"),
                RobotPort("IR", "RCX infrared tower")
            ),
            firmwareVersion = "1.0.0-sim",
            capabilities = listOf("profiles", "code generation", "simulated probes", "manual handoff")
        )
    }

    override suspend fun runProbe(plan: ProbePlan): Flow<ProbeTelemetry> = flow {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < plan.durationMs) {
            val now = System.currentTimeMillis()
            emit(
                ProbeTelemetry(
                    tMs = now - startTime,
                    ports = mapOf(
                        "A" to PortData(position = Random.nextFloat() * 360, speed = Random.nextFloat() * 100),
                        "B" to PortData(position = Random.nextFloat() * 360, speed = Random.nextFloat() * 100)
                    ),
                    imu = ImuData(
                        ax = Random.nextFloat() * 2 - 1,
                        ay = Random.nextFloat() * 2 - 1,
                        az = Random.nextFloat() * 2 - 1,
                        gx = Random.nextFloat() * 500 - 250,
                        gy = Random.nextFloat() * 500 - 250,
                        gz = Random.nextFloat() * 500 - 250
                    )
                )
            )
            delay(100)
        }
    }

    override suspend fun stopAll() {
        // No-op
    }
}
