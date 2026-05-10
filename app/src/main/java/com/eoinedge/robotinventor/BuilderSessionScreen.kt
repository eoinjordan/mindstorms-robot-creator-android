package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var session by remember { mutableStateOf<BuilderSession?>(null) }
    var observation by remember { mutableStateOf("") }
    var showHandoffDialog by remember { mutableStateOf(false) }
    var handoffSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingHandoff by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showHandoffDialog) {
        AlertDialog(
            onDismissRequest = { showHandoffDialog = false },
            title = { Text("Handoff Steps") },
            text = {
                if (isLoadingHandoff) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column {
                        Text(
                            "Manual steps to run this in the official LEGO app:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        handoffSteps.forEachIndexed { index, step ->
                            Text(
                                "${index + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHandoffDialog = false }) { Text("Done") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Builder Session", style = MaterialTheme.typography.headlineMedium)
        Text("One safe test, one observation, one next change.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        val activeSession = session
        if (activeSession == null) {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Goal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    session = mcpClient.startBuilderSession(profile.id, goal, "kid")
                }
            }) {
                Text("Start Session")
            }
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Session ID: ${activeSession.id}", style = MaterialTheme.typography.labelSmall)
                Text("Goal: ${activeSession.goal}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(activeSession.steps) { step ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A2A))
                ) {
                    Text(step.text, Modifier.padding(12.dp), color = Color.White)
                }
            }

            activeSession.summary?.let { summary ->
                item {
                    if (summary.likelyIssues.isNotEmpty()) {
                        Text("Likely Issues", fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                        summary.likelyIssues.forEach { Text("- $it") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Next Actions", fontWeight = FontWeight.Bold, color = Color(0xFF006A6A))
                    summary.nextActions.forEach { Text("- $it") }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = observation.isNotBlank(),
                onClick = {
                    scope.launch {
                        val text = observation
                        val summary = mcpClient.appendObservation(activeSession.id, text)
                        val step = BuilderStep(
                            id = "local-${activeSession.steps.size + 1}",
                            type = "observation",
                            text = text,
                            author = "human"
                        )
                        session = activeSession.copy(
                            steps = activeSession.steps + step,
                            summary = summary
                        )
                        observation = ""
                    }
                }
            ) {
                Text("Record")
            }

            OutlinedButton(onClick = {
                showHandoffDialog = true
                isLoadingHandoff = true
                scope.launch {
                    try {
                        val handoff = mcpClient.getOfficialHandoff(profile.id, activeSession.goal)
                        handoffSteps = handoff.steps
                    } catch (err: Exception) {
                        handoffSteps = listOf("Could not load handoff: ${err.message}")
                    } finally {
                        isLoadingHandoff = false
                    }
                }
            }) {
                Text("Handoff Steps")
            }

            TextButton(onClick = { session = null }) {
                Text("End")
            }
        }
    }
}
