package com.eoinedge.robotinventor

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RobotAppTests {

    @Test
    fun testProfileParsing() {
        val jsonString = """
            [
              {
                "id": "test-robot",
                "name": "Test Robot",
                "kind": "test_kind",
                "source": "test_source",
                "confidence": "high",
                "ports": [
                  {
                    "port": "A",
                    "type": "motor",
                    "role": "drive"
                  }
                ]
              }
            ]
        """.trimIndent()
        
        val profiles = Json.decodeFromString<List<RobotProfile>>(jsonString)
        
        assertEquals(1, profiles.size)
        assertEquals("test-robot", profiles[0].id)
        assertEquals("A", profiles[0].ports[0].port)
    }

    @Test
    fun testSimulatedTransportScan() = runBlocking {
        val transport = SimulatedTransport()
        val devices = transport.scan()
        
        assertTrue(devices.isNotEmpty())
        assertTrue(devices.any { it.name == "Blast" })
        assertTrue(devices.any { it.name == "Gelo" })
    }

    @Test
    fun testProbeSessionSerialization() {
        val telemetry = listOf(
            ProbeTelemetry(
                tMs = 1000L,
                ports = mapOf("A" to PortData(position = 10f)),
                imu = ImuData(ax = 0.1f, ay = 0.2f, az = 0.3f)
            )
        )
        val session = ProbeSession(
            sessionId = "test-session",
            profileId = "blast",
            label = "Test Label",
            notes = "Test Notes",
            timestamp = 2000L,
            telemetry = telemetry
        )
        
        val json = Json { prettyPrint = true }
        val serialized = json.encodeToString(session)
        
        assertTrue(serialized.contains("\"sessionId\": \"test-session\""))
        assertTrue(serialized.contains("\"ax\": 0.1"))
        
        val deserialized = json.decodeFromString<ProbeSession>(serialized)
        assertEquals(session.sessionId, deserialized.sessionId)
        assertEquals(session.telemetry.size, deserialized.telemetry.size)
        assertEquals(10f, deserialized.telemetry[0].ports["A"]?.position)
    }
    
    @Test
    fun testSimulatedTransportProbe() = runBlocking {
        val transport = SimulatedTransport()
        val plan = ProbePlan("Test", 0.3f, 500)
        val telemetry = transport.runProbe(plan).toList()
        
        assertTrue(telemetry.isNotEmpty())
        assertNotNull(telemetry[0].imu)
        assertNotNull(telemetry[0].tMs)
    }
}
