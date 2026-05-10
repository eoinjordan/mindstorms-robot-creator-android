package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfilePortItem(port: ProfilePort) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Port ${port.port}", fontWeight = FontWeight.Bold)
                Text(port.role)
            }
            Text(port.type, color = Color(0xFF006A6A), fontWeight = FontWeight.Bold)
        }
    }
}

fun simulatedProbeSummary(profile: RobotProfile): String {
    val motorCount = profile.ports.count { it.type == "motor" }
    val sensorCount = profile.ports.count { it.type == "sensor" }
    val signature = when {
        profile.kind.contains("quadruped") -> "walker gait signature"
        profile.kind.contains("modular") -> "steer plus drive signature"
        profile.kind.contains("sports") -> "drive plus kicker signature"
        profile.kind.contains("launcher") -> "drive plus arm/action signature"
        else -> "body motion signature"
    }
    return "$signature: $motorCount motors, $sensorCount sensors"
}

@Composable
fun DeviceItem(device: RobotDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = device.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Type: ${device.type}", style = MaterialTheme.typography.bodySmall)
            device.batteryLevel?.let {
                Text(text = "Battery: $it%", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Status: ${device.connectionState}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
