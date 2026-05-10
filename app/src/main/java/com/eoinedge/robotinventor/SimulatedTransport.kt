package com.eoinedge.robotinventor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class SimulatedTransport : RobotTransport {
    private val simulatedDevices = listOf(
        RobotDevice("51515-blast", "Blast", "Robot Inventor 51515", 88),
        RobotDevice("51515-charlie", "Charlie", "Robot Inventor 51515", 86),
        RobotDevice("51515-gelo", "Gelo", "Robot Inventor 51515", 90),
        RobotDevice("51515-mvp", "M.V.P.", "Robot Inventor 51515", 84),
        RobotDevice("51515-tricky", "Tricky", "Robot Inventor 51515", 91)
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
                RobotPort("Port A", "51515 motor"),
                RobotPort("Port B", "51515 motor"),
                RobotPort("Port C", "51515 motor/sensor"),
                RobotPort("Port D", "51515 motor/sensor"),
                RobotPort("Port E", "51515 color/distance/motor"),
                RobotPort("Port F", "51515 color/distance/motor")
            ),
            firmwareVersion = "1.0.0-sim",
            capabilities = listOf("probes", "telemetry", "motors")
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
