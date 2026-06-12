package com.eoinedge.robotinventor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    val author: String? = null,
    val data: JsonObject? = null
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
    val profileName: String? = null,
    val safety: List<String> = emptyList(),
    val steps: List<String>
)

@Serializable
private data class ActionRequest(val action: String, val params: Map<String, String>)

@Serializable
private data class BuilderStartResponse(val ok: Boolean, val session: BuilderSession)

@Serializable
private data class BuilderUpdateResponse(
    val ok: Boolean,
    val session: BuilderSession? = null,
    val summary: BuilderSummary? = null
)

@Serializable
private data class OfficialHandoffResponse(val ok: Boolean, val handoff: OfficialClientHandoff)

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
        json.decodeFromString<OfficialHandoffResponse>(response).handoff
    }

    override suspend fun generateCode(profileId: String, intent: String, target: String): String = withContext(Dispatchers.IO) {
        val params = mapOf("profileId" to profileId, "intent" to intent, "target" to target)
        val response = post("code_generate", params)
        // Response is {ok:true, source:"..."}; older servers used code.
        try {
            val obj = json.parseToJsonElement(response)
            obj.jsonObject["source"]?.jsonPrimitive?.content
                ?: obj.jsonObject["code"]?.jsonPrimitive?.content
                ?: response
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

            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            stream.bufferedReader().use { it.readText() }
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
                BuilderStep("1", "agent_instruction", "Open the LEGO app and run a short test.")
            )
        )
    }

    override suspend fun appendObservation(sessionId: String, text: String): BuilderSummary {
        val likelyIssues = when {
            text.contains("backward", true) -> listOf("motor_direction_reversed")
            text.contains("did not move", true) || text.contains("not move", true) -> listOf("missing_motor_or_wrong_port")
            text.contains("nothing happened", true) -> listOf("missing_motor_or_wrong_port")
            else -> emptyList()
        }
        return BuilderSummary(
            likelyIssues = likelyIssues,
            nextActions = if (likelyIssues.contains("motor_direction_reversed")) {
                listOf("Reverse the motor direction in code", "Run one short test again")
            } else {
                listOf("Check port connections", "Run one short low-power motor test")
            },
            latestObservation = text
        )
    }

    override suspend fun summarizeSession(sessionId: String): BuilderSummary {
        return BuilderSummary(nextActions = listOf("Ready to generate final code."))
    }

    override suspend fun getOfficialHandoff(profileId: String, goal: String): OfficialClientHandoff {
        return OfficialClientHandoff(
            clientId = "robot-inventor-51515",
            profileName = profileId,
            safety = listOf("Keep the stop control visible."),
            steps = listOf("Open app", "Connect BLE", "Add motor block", "Press Play")
        )
    }

    override suspend fun generateCode(profileId: String, intent: String, target: String): String {
        return when (target) {
            "rcx-nqc" -> """
                // Fake MCP code for $profileId
                task main()
                {
                  PlayTone(440, 30);
                }
            """.trimIndent()
            "arduino-basex" -> """
                // Fake MCP BaseX sketch for $profileId
                void setup() {
                  Serial.begin(115200);
                }
                void loop() {
                  Serial.println("$intent");
                  delay(1000);
                }
            """.trimIndent()
            else -> when (intent) {
                "beep", "beep_hello" -> """
                    import hub
                    hub.sound.beep(440, 500)
                    print('Beep! $profileId')
                """.trimIndent()
                "drive", "drive_forward" -> """
                    import hub
                    import time
                    hub.port.A.motor.run_for_rotations(2, 50)
                    hub.port.B.motor.run_for_rotations(2, 50)
                """.trimIndent()
                "wave" -> """
                    import hub
                    import time
                    for i in range(3):
                        hub.port.A.motor.run_for_degrees(90, 30)
                        time.sleep_ms(300)
                        hub.port.A.motor.run_for_degrees(-90, 30)
                """.trimIndent()
                else -> """
                    import hub
                    print('Hello from $profileId!')
                """.trimIndent()
            }
        }
    }
}
