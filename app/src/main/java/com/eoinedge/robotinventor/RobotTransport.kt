package com.eoinedge.robotinventor

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

data class RobotDevice(
    val id: String,
    val name: String,
    val type: String, // e.g., "EV3", "Spike", "M5Stack", "Simulated"
    val batteryLevel: Int? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

interface RobotConnection {
    val deviceId: String
    suspend fun disconnect()
}

data class RobotDescription(
    val ports: List<RobotPort>,
    val firmwareVersion: String,
    val capabilities: List<String>
)

data class RobotPort(
    val name: String,
    val connectedDevice: String? = null // e.g., "Large Motor", "Color Sensor"
)

data class ProbePlan(
    val routineName: String,
    val maxDuty: Float,
    val durationMs: Long
)

@Serializable
data class ProbeTelemetry(
    val tMs: Long,
    val ports: Map<String, PortData> = emptyMap(),
    val imu: ImuData? = null
)

@Serializable
data class PortData(
    val position: Float? = null,
    val speed: Float? = null,
    val duty: Float? = null,
    val stalled: Boolean? = null
)

@Serializable
data class ImuData(
    val ax: Float? = null, val ay: Float? = null, val az: Float? = null,
    val gx: Float? = null, val gy: Float? = null, val gz: Float? = null
)

@Serializable
data class ProbeSession(
    val sessionId: String,
    val profileId: String,
    val label: String? = null,
    val notes: String? = null,
    val timestamp: Long, // Start timestamp
    val sampleRateHz: Double = 10.0,
    val telemetry: List<ProbeTelemetry> = emptyList(),
    val commands: List<ProbeCommand> = emptyList()
)

@Serializable
data class ProbeCommand(
    val tMs: Long,
    val port: String,
    val mode: String,
    val value: Float
)

interface RobotTransport {
    suspend fun scan(): List<RobotDevice>
    suspend fun connect(deviceId: String): RobotConnection
    suspend fun describe(): RobotDescription
    suspend fun runProbe(plan: ProbePlan): Flow<ProbeTelemetry>
    suspend fun stopAll()
}
