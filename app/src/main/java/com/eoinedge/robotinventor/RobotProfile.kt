package com.eoinedge.robotinventor

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RobotProfile(
    val id: String,
    val name: String,
    val kind: String,
    val source: String,
    val confidence: String,
    val ports: List<ProfilePort>
)

@Serializable
data class ProfilePort(
    val port: String,
    val type: String,
    val role: String
)

class ProfileRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseProfiles(jsonString: String): List<RobotProfile> {
        return json.decodeFromString<List<RobotProfile>>(jsonString)
    }

    fun loadProfiles(): List<RobotProfile> {
        return try {
            val jsonString = context.assets.open("robot_profiles_51515.json").bufferedReader().use { it.readText() }
            parseProfiles(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
