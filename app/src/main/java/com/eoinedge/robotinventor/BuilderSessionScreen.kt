package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BuilderSessionScreen(
    profile: RobotProfile?,
    mcpClient: MindstormsMcpClient
) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a robot in the Fleet tab first.")
        }
        return
    }

    var goal by remember { mutableStateOf("test safe movement") }
    var audience by remember { mutableStateOf("kid") }
    var session by remember { mutableStateOf<BuilderSession?>(null) }
    var observation by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Builder Session", style = MaterialTheme.typography.headlineMedium)
        Text("One safe test, one observation, one next change.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(Modifier.height(16.dp))

        if (session == null) {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Goal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    session = mcpClient.startBuilderSession(profile.id, goal, audience)
                }
            }) {
                Text("Start Session")
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Session ID: ${session!!.id}", style = MaterialTheme.typography.labelSmall)
                    Text("Goal: ${session!!.goal}", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(Modifier.weight(1f)) {
                items(session!!.steps) { step ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F4))
                    ) {
                        Text(step.text, Modifier.padding(12.dp))
                    }
                }
                
                session!!.summary?.let { summary ->
                    item {
                        if (summary.likelyIssues.isNotEmpty()) {
                            Text("Likely Issues", fontWeight = FontWeight.Bold, color = Color.Red)
                            summary.likelyIssues.forEach { Text("• $it") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Next Actions", fontWeight = FontWeight.Bold, color = Color(0xFF006A6A))
                        summary.nextActions.forEach { Text("• $it") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = observation,
                onValueChange = { observation = it },
                label = { Text("Observation") },
                placeholder = { Text("What happened?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    scope.launch {
                        val summary = mcpClient.appendObservation(session!!.id, observation)
                        session = session!!.copy(summary = summary)
                        observation = ""
                    }
                }) {
                    Text("Record Observation")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { session = null }) {
                    Text("End Session")
                }
            }
        }
    }
}
