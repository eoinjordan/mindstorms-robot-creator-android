package com.eoinedge.robotinventor

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidFeatureCoverageTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun profiles(): List<RobotProfile> {
        val file = listOf(
            File("app/src/main/assets/robot_profiles.json"),
            File("src/main/assets/robot_profiles.json")
        ).firstOrNull { it.exists() }
            ?: File("app/src/main/assets/robot_profiles.json")
        assertTrue("Unified robot profile asset is missing", file.exists())
        return json.decodeFromString(file.readText())
    }

    @Test
    fun unifiedProfileAssetCoversAllSupportedFamilies() {
        val profiles = profiles()
        val families = profiles.map { it.family }.toSet()

        assertTrue(profiles.size >= 16)
        assertTrue(families.containsAll(listOf("wedo2", "robot-inventor", "ev3", "nxt", "rcx", "m5stack-basex")))
        assertTrue(profiles.any { it.id == "51515-blast" })
        assertTrue(profiles.any { it.id == "wedo2-milo" })
        assertTrue(profiles.any { it.id == "ev3-ev3rstorm" })
        assertTrue(profiles.any { it.id == "nxt-alpha-rex" })
        assertTrue(profiles.any { it.id == "rcx-pushbot" })
    }

    @Test
    fun codeTargetsAndExtensionsMatchEverySupportedFamily() {
        val byFamily = profiles().groupBy { it.family }

        assertEquals(listOf("wedo2-micropython", "pybricks-city"), targetsFor(byFamily.getValue("wedo2").first()).map { it.value })
        assertEquals(listOf("pybricks-python", "spike-stock"), targetsFor(byFamily.getValue("robot-inventor").first()).map { it.value })
        assertEquals(listOf("pybricks-ev3", "ev3dev-python"), targetsFor(byFamily.getValue("ev3").first()).map { it.value })
        assertEquals(listOf("nxt-python"), targetsFor(byFamily.getValue("nxt").first()).map { it.value })
        assertEquals(listOf("rcx-nqc"), targetsFor(byFamily.getValue("rcx").first()).map { it.value })
        assertEquals(listOf("arduino-basex"), targetsFor(byFamily.getValue("m5stack-basex").first()).map { it.value })

        assertEquals("lms", extensionFor("lego-stock-python"))
        assertEquals("lms", extensionFor("spike-stock"))
        assertEquals("py", extensionFor("pybricks-python"))
        assertEquals("py", extensionFor("ev3dev-python"))
        assertEquals("nqc", extensionFor("rcx-nqc"))
        assertEquals("ino", extensionFor("arduino-basex"))
    }

    @Test
    fun localFallbackCodeUsesFamilySpecificRuntimeAndSafeStops() {
        val byFamily = profiles().groupBy { it.family }

        val wedo = defaultCode(byFamily.getValue("wedo2").first(), "drive", "wedo2-micropython")
        assertTrue(wedo.contains("import hub"))
        assertTrue(wedo.contains(".stop()"))

        val inventor = defaultCode(byFamily.getValue("robot-inventor").first(), "drive", "pybricks-python")
        assertTrue(inventor.contains("InventorHub"))
        assertTrue(inventor.contains("motor.stop()"))

        val ev3 = defaultCode(byFamily.getValue("ev3").first(), "drive", "pybricks-ev3")
        assertTrue(ev3.contains("EV3Brick"))
        assertTrue(ev3.contains("motor.stop()"))

        val nxt = defaultCode(byFamily.getValue("nxt").first(), "drive", "nxt-python")
        assertTrue(nxt.contains("nxt.locator"))
        assertTrue(nxt.contains("motor.brake()"))

        val rcx = defaultCode(byFamily.getValue("rcx").first(), "drive", "rcx-nqc")
        assertTrue(rcx.contains("task main()"))
        assertTrue(rcx.contains("Off("))

        val basex = defaultCode(byFamily.getValue("m5stack-basex").first(), "probe", "arduino-basex")
        assertTrue(basex.contains("Serial.begin"))
        assertTrue(basex.contains("Keep duty low"))
    }

    @Test
    fun fakeMcpClientSupportsBuilderAndTargetSpecificCode() = runBlocking {
        val client = FakeMindstormsMcpClient()

        val session = client.startBuilderSession("51515-blast", "make it wave", "kid")
        assertEquals("51515-blast", session.profileId)
        assertFalse(session.steps.isEmpty())

        val noMove = client.appendObservation(session.id, "The motor did not move")
        assertTrue(noMove.likelyIssues.contains("missing_motor_or_wrong_port"))
        assertEquals("The motor did not move", noMove.latestObservation)

        val reversed = client.appendObservation(session.id, "The arm moved backward")
        assertTrue(reversed.likelyIssues.contains("motor_direction_reversed"))

        val nqc = client.generateCode("rcx-pushbot", "beep", "rcx-nqc")
        assertTrue(nqc.contains("task main()"))
        assertFalse(nqc.contains("\\n"))

        val arduino = client.generateCode("sim-two-wheel-drive", "probe", "arduino-basex")
        assertTrue(arduino.contains("void setup()"))
        assertFalse(arduino.contains("\\n"))
    }

    @Test
    fun blockEditorAssetIsOfflineAndAndroidBridgeReady() {
        val file = listOf(
            File("app/src/main/assets/blockly_editor.html"),
            File("src/main/assets/blockly_editor.html")
        ).firstOrNull { it.exists() }
            ?: File("app/src/main/assets/blockly_editor.html")
        assertTrue("Block editor asset is missing", file.exists())

        val html = file.readText()
        assertFalse("Android block editor must not depend on CDN scripts", html.contains("https://"))
        assertFalse("Android block editor must not depend on external Blockly script loading", html.contains("unpkg.com"))
        assertTrue(html.contains("id=\"palette\""))
        assertTrue(html.contains("id=\"program\""))
        assertTrue(html.contains("data-kind=\"motor\""))
        assertTrue(html.contains("data-kind=\"pair\""))
        assertTrue(html.contains("function generatePython()"))
        assertTrue(html.contains("window.AndroidBridge.onRunCode"))
        assertTrue(html.contains("window.getCode = getCode"))
        assertTrue(html.contains("body.kids"))
    }

    @Test
    fun simulatedTransportCoversFamiliesAndRejectsUnsafeProbePlans() = runBlocking {
        val transport = SimulatedTransport()
        val devices = transport.scan()
        val deviceTypes = devices.map { it.type }.toSet()

        assertTrue(deviceTypes.containsAll(listOf("WeDo 2.0 Smart Hub", "Robot Inventor 51515", "EV3 Brick", "NXT Brick", "RCX IR Tower", "M5Stack BaseX")))
        assertEquals("wedo2-milo", transport.connect("wedo2-milo").deviceId)

        val description = transport.describe()
        assertTrue(description.capabilities.contains("simulated probes"))
        assertTrue(description.capabilities.contains("manual handoff"))

        val telemetry = transport.runProbe(ProbePlan("safe", 0.3f, 250)).toList()
        assertTrue(telemetry.isNotEmpty())
        assertNotNull(telemetry.first().imu)
        assertEquals(0.3f, telemetry.first().ports.getValue("A").duty)

        assertFailsWithIllegalArgument { transport.connect("missing-device") }
        assertFailsWithIllegalArgument { transport.runProbe(ProbePlan("unsafe", 0.9f, 250)).toList() }
        assertFailsWithIllegalArgument { transport.runProbe(ProbePlan("too-long", 0.3f, 20_000)).toList() }
    }

    private suspend fun assertFailsWithIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
