package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var serverUrl by remember { mutableStateOf(prefs.getString("mcp_server_url", "http://10.0.2.2:3095") ?: "http://10.0.2.2:3095") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("MCP Server Configuration", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { 
                    serverUrl = it
                    prefs.edit().putString("mcp_server_url", it).apply()
                },
                label = { Text("Server URL (e.g. http://192.168.1.50:3095)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Use 10.0.2.2 to access your computer's localhost from an emulator.",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text("Features", style = MaterialTheme.typography.titleMedium)
            Text("• Simulated Mode: Enabled", style = MaterialTheme.typography.bodyMedium)
            Text("• Blockly Editor: Integrated", style = MaterialTheme.typography.bodyMedium)
            Text("• Dataset Capture: Enabled", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
