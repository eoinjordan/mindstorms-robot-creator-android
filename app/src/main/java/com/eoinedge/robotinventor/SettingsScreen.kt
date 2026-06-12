package com.eoinedge.robotinventor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    simpleMode: Boolean,
    onSimpleModeChange: (Boolean) -> Unit,
    advancedMode: Boolean,
    onAdvancedModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var serverUrl by remember {
        mutableStateOf(prefs.getString("mcp_server_url", "http://10.0.2.2:3095") ?: "http://10.0.2.2:3095")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Student Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SettingSwitchRow(
                title = "Open in WeDo Blockly",
                subtitle = "Default kid-friendly screen with large blocks and one run/export action.",
                checked = simpleMode,
                onCheckedChange = onSimpleModeChange
            )
            SettingSwitchRow(
                title = "Show advanced tabs",
                subtitle = "Adds Probe, Voice, and Data tabs for teachers and debugging.",
                checked = advancedMode,
                onCheckedChange = onAdvancedModeChange
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("MCP Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    prefs.edit().putString("mcp_server_url", it).apply()
                },
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Use 10.0.2.2 to access your computer's localhost from an emulator.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Available Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("WeDo Blockly: default student workflow", style = MaterialTheme.typography.bodyMedium)
            Text("Builder: visible in standard mode", style = MaterialTheme.typography.bodyMedium)
            Text("Probe, Voice, Data: hidden unless advanced tabs are enabled", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
