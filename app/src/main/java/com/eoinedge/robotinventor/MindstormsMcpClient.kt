package com.eoinedge.robotinventor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class BuilderSession(
    val id: String,
    val profileId: String,
    val goal: String,
    val audience: String,
    val steps: List<BuilderStep> = emptyList(),
    val summary: BuilderSummary? = null
)

@Serializable
data class BuilderStep(
    val id: String,
    val type: String,
    val text: String,
    val data: Map<String, List<String>>? = null
)

@Serializable
data class BuilderSummary(
    val likelyIssues: List<String> = emptyList(),
    val nextActions: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val latestObservation: String? = null
)

@Serializable
data class OfficialClientHandoff(
    val clientId: String,
    val steps: List<String>
)

@Serializable
private data class ActionRequest(val action: String, val params: Map<String, String>)

@Serializable
private data class BuilderStartResponse(val ok: Boolean, val session: BuilderSession)

@Serializable
private data class BuilderUpdateResponse(val ok: Boolean, val summary: BuilderSummary? = null)

interface MindstormsMcpClient {
    suspend fun startBuilderSession(profileId: String, goal: String, audience: String): BuilderSession
    suspend fun appendObservation(sessionId: String, text: String): BuilderSummary?
    suspend fun summarizeSession(sessionId: String): BuilderSummary?
    suspend fun getOfficialHandoff(profileId: String, goal: String): OfficialClientHandoff
    suspend fun generateCode(profileId: String, intent: String, target: String = "pybricks"): String
}

class HttpMindstormsMcpClient(private val baseUrl: String) : MindstormsMcpClient {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun startBuilderSession(profileId: String, goal: String, audience: String): BuilderSession = withContext(Dispatchers.IO) {
        val params = mapOf("profileId" to profileId, "goal" to goal, "audience" to audience)
        val response = post("builder_session_start", params)
        json.decodeFromString<BuilderStartResponse>(response).session
    }

    override suspend fun appendObservation(sessionId: String, text: String): BuilderSummary? = withContext(Dispatchers.IO) {
        val params = mapOf("sessionId" to sessionId, "type" to "observation", "text" to text)
        val response = post("builder_session_append", params)
        json.decodeFromString<BuilderUpdateResponse>(response).summary
    }

    override suspend fun summarizeSession(sessionId: String): BuilderSummary? = withContext(Dispatchers.IO) {
        val params = mapOf("sessionId" to sessionId)
        val response = post("builder_session_summary", params)
        json.decodeFromString<BuilderUpdateResponse>(response).summary
    }

    override suspend fun getOfficialHandoff(profileId: String, goal: String): OfficialClientHandoff = withContext(Dispatchers.IO) {
        val params = mapOf("profileId" to profileId, "goal" to goal)
        val response = post("official_client_handoff", params)
        json.decodeFromString<OfficialClientHandoff>(response)
    }

    override suspend fun generateCode(profileId: String, intent: String, target: String): String = withContext(Dispatchers.IO) {
        val params = mapOf("profileId" to profileId, "intent" to intent, "target" to target)
        val response = post("code_generate", params)
        // Response is {ok:true, code:"..."}; extract the code field or return raw on failure
        try {
            val obj = json.parseToJsonElement(response)
            obj.jsonObject["code"]?.jsonPrimitive?.content ?: response
        } catch (e: Exception) { response }
    }

    private fun post(action: String, params: Map<String, String>): String {
        return try {
            val url = URL("$baseUrl/run")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            
            val body = json.encodeToString(ActionRequest(action, params))
            conn.outputStream.use { it.write(body.toByteArray()) }
            
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            "{\"ok\": false, \"error\": \"${e.message}\"}"
        }
    }
}

class FakeMindstormsMcpClient : MindstormsMcpClient {
    override suspend fun startBuilderSession(profileId: String, goal: String, audience: String): BuilderSession {
        return BuilderSession(
            id = "fake-${System.currentTimeMillis()}",
            profileId = profileId,
            goal = goal,
            audience = audience,
            steps = listOf(
                BuilderStep("1", "instruction", "Open the LEGO app and run a short test.")
            )
        )
    }

    override suspend fun appendObservation(sessionId: String, text: String): BuilderSummary {
        return BuilderSummary(
            likelyIssues = if (text.contains("backward", true)) listOf("motor_direction_reversed") else emptyList(),
            nextActions = listOf("Check port connections", "Try reversing the motor in code"),
            latestObservation = text
        )
    }

    override suspend fun summarizeSession(sessionId: String): BuilderSummary {
        return BuilderSummary(nextActions = listOf("Ready to generate final code."))
    }

    override suspend fun getOfficialHandoff(profileId: String, goal: String): OfficialClientHandoff {
        return OfficialClientHandoff(
            clientId = "robot-inventor-51515",
            steps = listOf("Open app", "Connect BLE", "Add motor block", "Press Play")
        )
    }

    override suspend fun generateCode(profileId: String, intent: String, target: String): String {
        return when (intent) {
            "beep" -> """import hub\nhub.sound.beep(440, 500)\nprint('Beep! ${profileId}')""" 
            "drive" -> """import hub, time\nhub.port.A.motor.run_for_rotations(2, 50)\nhub.port.B.motor.run_for_rotations(2, 50)"""
            "wave" -> """import hub, time\nfor i in range(3):\n    hub.port.A.motor.run_for_degrees(90, 30)\n    time.sleep_ms(300)\n    hub.port.A.motor.run_for_degrees(-90, 30)"""
            else -> """import hub\nprint('Hello from ${profileId}!')"""
        }
    }
}
