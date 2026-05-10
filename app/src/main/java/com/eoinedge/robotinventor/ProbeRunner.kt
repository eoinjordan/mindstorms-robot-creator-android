package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Composable
fun ProbeRunner(transport: RobotTransport, profile: RobotProfile, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RobotDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var sessionLabel by remember { mutableStateOf("") }
    var sessionNotes by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var telemetryList by remember { mutableStateOf(mutableListOf<ProbeTelemetry>()) }
    var currentTelemetry by remember { mutableStateOf<ProbeTelemetry?>(null) }
    var savedSessionId by remember { mutableStateOf<String?>(null) }
    
    // History for graphs
    val axHistory = remember { mutableStateListOf<Float>() }
    val ayHistory = remember { mutableStateListOf<Float>() }
    val azHistory = remember { mutableStateListOf<Float>() }
    val maxHistory = 50

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = { Text("Probe Runner: ${profile.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (savedSessionId != null) {
                    Text("Session saved to local history.", color = Color(0xFF006A6A), fontWeight = FontWeight.Bold)
                    Text("Session ID: $savedSessionId", style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(
                        value = sessionLabel,
                        onValueChange = { sessionLabel = it },
                        label = { Text("Session Label (e.g. 'Walking on carpet')") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isRunning) {
                        Text("PROBE ACTIVE", color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Graphs
                        TelemetryGraph("Accel X", axHistory.toList(), color = Color.Red)
                        TelemetryGraph("Accel Y", ayHistory.toList(), color = Color.Green)
                        TelemetryGraph("Accel Z", azHistory.toList(), color = Color.Blue)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        currentTelemetry?.let { data ->
                            Text("Ports: ${data.ports.keys.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (telemetryList.isNotEmpty()) {
                        Text("Probe Finished. Recorded ${telemetryList.size} data points.")
                    } else {
                        Text("Safe probe routine will exercise all motors at 30% duty.")
                    }
                }
            }
        },
        confirmButton = {
            if (savedSessionId != null) {
                Button(onClick = onDismiss) { Text("Close") }
            } else if (!isRunning) {
                Row {
                    if (telemetryList.isNotEmpty()) {
                        Button(onClick = {
                            val sessionId = UUID.randomUUID().toString()
                            val session = ProbeSession(
                                sessionId = sessionId,
                                profileId = profile.id,
                                label = sessionLabel,
                                notes = sessionNotes,
                                timestamp = System.currentTimeMillis(),
                                telemetry = telemetryList
                            )
                            val jsonPayload = Json { prettyPrint = true }.encodeToString(session)
                            
                            scope.launch {
                                database.sessionDao().insertSession(
                                    StoredSession(
                                        id = sessionId,
                                        profileId = profile.id,
                                        label = sessionLabel,
                                        notes = sessionNotes,
                                        timestamp = session.timestamp,
                                        jsonPayload = jsonPayload
                                    )
                                )
                                savedSessionId = sessionId
                            }
                        }) {
                            Text("Save to History")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(onClick = {
                        scope.launch {
                            isRunning = true
                            telemetryList = mutableListOf()
                            savedSessionId = null
                            axHistory.clear()
                            ayHistory.clear()
                            azHistory.clear()
                            
                            transport.runProbe(ProbePlan("Safe Check", 0.3f, 5000))
                                .collectLatest {
                                    currentTelemetry = it
                                    telemetryList.add(it)
                                    
                                    it.imu?.let { imu ->
                                        imu.ax?.let { axHistory.add(it) }
                                        imu.ay?.let { ayHistory.add(it) }
                                        imu.az?.let { azHistory.add(it) }
                                        if (axHistory.size > maxHistory) {
                                            axHistory.removeAt(0)
                                            ayHistory.removeAt(0)
                                            azHistory.removeAt(0)
                                        }
                                    }
                                }
                            isRunning = false
                        }
                    }) {
                        Text(if (telemetryList.isEmpty()) "Start Probe" else "Restart")
                    }
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            transport.stopAll()
                            isRunning = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("EMERGENCY STOP")
                }
            }
        },
        dismissButton = {
            if (!isRunning && savedSessionId == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
