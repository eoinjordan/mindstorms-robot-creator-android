package com.eoinedge.robotinventor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MindstormsFleetScreen(
    transport: RobotTransport,
    profiles: List<RobotProfile>,
    selectedProfile: RobotProfile?,
    onProfileSelect: (RobotProfile) -> Unit,
    isSimulated: Boolean,
    onTransportChange: (Boolean) -> Unit
) {
    var scanning by remember { mutableStateOf(false) }
    var foundDevices by remember { mutableStateOf(emptyList<RobotDevice>()) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Sidebar: Fleet List
        LazyColumn(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(Color(0xFF172026))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Robot Fleet",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Transport", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSimulated,
                        onCheckedChange = { onTransportChange(it) },
                        colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
                    )
                    Text("Simulated", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            scanning = true
                            foundDevices = transport.scan()
                            scanning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A6A))
                ) {
                    Text(if (scanning) "Scanning..." else "Scan Devices", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        // TODO: Implement profile sync via MCP client
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Sync Profiles", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            if (foundDevices.isNotEmpty()) {
                item {
                    Text("Found Hubs", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                items(foundDevices) { device ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(device.name, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(device.id, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Profiles", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            items(profiles) { profile ->
                val active = profile.id == selectedProfile?.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) Color(0xFF008F8F) else Color(0xFF25333A)
                    ),
                    onClick = { onProfileSelect(profile) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            profile.kind,
                            color = Color(0xFFD9E5E5),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Right Content: Selected Profile Details
        selectedProfile?.let { profile ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(profile.kind, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source: ${profile.source}")
                    Text("Confidence: ${profile.confidence}")
                    if (profile.confidence.contains("needs", ignoreCase = true)) {
                        Text(
                            text = "Confirm this profile against the LEGO app or the physical build.",
                            color = Color(0xFFB00020),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    Text("Ports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(profile.ports) { port ->
                    ProfilePortItem(port)
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No profile selected")
        }
    }
}
