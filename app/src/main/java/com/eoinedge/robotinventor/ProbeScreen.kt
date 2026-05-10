package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProbeScreen(
    transport: RobotTransport,
    profile: RobotProfile?
) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a robot in the Fleet tab first.")
        }
        return
    }

    var showProbeRunner by remember { mutableStateOf(false) }

    if (showProbeRunner) {
        ProbeRunner(transport, profile, onDismiss = { showProbeRunner = false })
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Probe Tools", style = MaterialTheme.typography.headlineMedium)
        Text("Collect high-frequency telemetry for training models.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Simulated Probe", style = MaterialTheme.typography.titleLarge)
                Text("Ready to run a 5-second safe motor sweep.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showProbeRunner = true }) {
                    Text("Open Probe Runner")
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Classifier", style = MaterialTheme.typography.titleLarge)
                Text("No trained model available for ${profile.name} yet.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { /* TODO */ }, enabled = false) {
                    Text("Classify Hardware Signature")
                }
            }
        }
    }
}
