package com.eoinedge.robotinventor

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

private val INTENTS = listOf("beep", "drive", "wave", "probe")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeScreen(profile: RobotProfile?, mcpClient: MindstormsMcpClient) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a robot in the Fleet tab first.")
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Python", "Blocks")
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            PythonEditor(profile, mcpClient)
        } else {
            BlocklyEditor(onRunCode = { code ->
                scope.launch { /* BLE/MCP dispatch placeholder */ }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonEditor(profile: RobotProfile, mcpClient: MindstormsMcpClient) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedIntent by remember { mutableStateOf(INTENTS[0]) }
    var intentMenuExpanded by remember { mutableStateOf(false) }
    var code by remember {
        mutableStateOf(defaultCode(profile, INTENTS[0]))
    }
    var isGenerating by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Intent selector + action row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Intent dropdown
            ExposedDropdownMenuBox(
                expanded = intentMenuExpanded,
                onExpandedChange = { intentMenuExpanded = !intentMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedIntent.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Intent") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .width(140.dp)
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = intentMenuExpanded,
                    onDismissRequest = { intentMenuExpanded = false }
                ) {
                    INTENTS.forEach { intent ->
                        DropdownMenuItem(
                            text = { Text(intent.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedIntent = intent
                                intentMenuExpanded = false
                                code = defaultCode(profile, intent)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Generate button
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        try {
                            val generated = mcpClient.generateCode(profile.id, selectedIntent)
                            if (generated.isNotBlank()) code = generated
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(4.dp))
                Text("Generate")
            }

            Spacer(Modifier.width(8.dp))

            // Export .lms button
            OutlinedButton(
                onClick = {
                    exportLmsFile(context, profile, selectedIntent, code)
                    exportStatus = "Exported as ${profile.id}-$selectedIntent.lms"
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Export .lms")
            }
        }

        exportStatus?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Code display
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1A2A2A))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF80FFCC),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Profile-aware fallback code generated locally when no server is available. */
private fun defaultCode(profile: RobotProfile, intent: String): String {
    val ports = profile.ports.joinToString("\n") { "# Port ${it.port}: ${it.role} (${it.type})" }
    return when (intent) {
        "beep" -> """
# ${profile.name} — Beep intent (Pybricks)
from pybricks.hubs import InventorHub
from pybricks.tools import wait

hub = InventorHub()
$ports

hub.speaker.beep(440, 500)
wait(200)
hub.speaker.beep(880, 300)
        """.trimIndent()

        "drive" -> {
            val motorA = profile.ports.firstOrNull { "drive" in it.role.lowercase() || it.role == "left_drive" }?.port ?: "A"
            val motorB = profile.ports.firstOrNull { it.role == "right_drive" }?.port ?: "B"
            """
# ${profile.name} — Drive intent (Pybricks)
from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port, Direction
from pybricks.tools import wait

hub = InventorHub()
$ports

left  = Motor(Port.$motorA, Direction.COUNTERCLOCKWISE)
right = Motor(Port.$motorB)

# Drive forward 500 mm, then stop
left.run_time(300, 2000)
right.run_time(300, 2000)
wait(2200)
left.stop()
right.stop()
            """.trimIndent()
        }

        "wave" -> {
            val armPort = profile.ports.firstOrNull { "arm" in it.role.lowercase() || "wave" in it.role.lowercase() }?.port ?: "C"
            """
# ${profile.name} — Wave intent (Pybricks)
from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port
from pybricks.tools import wait

hub = InventorHub()
$ports

arm = Motor(Port.$armPort)
hub.speaker.beep(660, 200)

for _ in range(3):
    arm.run_angle(200, 45)
    wait(150)
    arm.run_angle(200, -45)
    wait(150)

arm.stop()
            """.trimIndent()
        }

        "probe" -> """
# ${profile.name} — Probe intent (Pybricks)
from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port
from pybricks.tools import wait, StopWatch

hub = InventorHub()
$ports

# Safe low-power step response probe
watch = StopWatch()
results = []

for port_letter in [${profile.ports.joinToString(", ") { "\"${it.port}\"" }}]:
    try:
        m = Motor(getattr(Port, port_letter))
        watch.reset()
        m.run_time(150, 500)      # gentle 15% duty, 0.5 s
        elapsed = watch.time()
        results.append((port_letter, m.angle(), elapsed))
        m.stop()
        wait(300)
    except Exception as e:
        results.append((port_letter, None, str(e)))

for r in results:
    print(r)
        """.trimIndent()

        else -> """
# ${profile.name} — Default program
from pybricks.hubs import InventorHub
hub = InventorHub()
$ports
print("Hello from ${profile.name}!")
        """.trimIndent()
    }
}

/**
 * Packages the Python code as a minimal .lms ZIP archive and shares it
 * via the Android share sheet so users can open it in the LEGO app.
 */
private fun exportLmsFile(context: Context, profile: RobotProfile, intent: String, code: String) {
    val dir = File(context.cacheDir, "lms_exports").also { it.mkdirs() }
    val file = File(dir, "${profile.id}-$intent.lms")

    ZipOutputStream(file.outputStream().buffered()).use { zip ->
        zip.putNextEntry(ZipEntry("scratch.py"))
        zip.write(code.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "${profile.name} — $intent.lms")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Open in LEGO App"))
}

@Composable
fun BlocklyEditor(onRunCode: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRunCode(code: String) { onRunCode(code) }
                }, "AndroidBridge")
                loadUrl("file:///android_asset/blockly_editor.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CodeScreen(profile: RobotProfile?, mcpClient: MindstormsMcpClient) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a robot in the Fleet tab first.")
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Python", "Blocks")
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            PythonEditor(profile)
        } else {
            BlocklyEditor(onRunCode = { code ->
                scope.launch {
                    // TODO: Implement code execution via MCP or direct BLE
                    // For now, we'll just log it or show a snackbar if we had one
                }
            })
        }
    }
}

@Composable
fun BlocklyEditor(onRunCode: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRunCode(code: String) {
                        onRunCode(code)
                    }
                }, "AndroidBridge")

                loadUrl("file:///android_asset/blockly_editor.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PythonEditor(profile: RobotProfile?) {
    val code = """
import hub
import time

# ${profile?.name} Python Program
# ------------------------------

def main():
    print("Starting ${profile?.name}...")
    # Port map:
    ${profile?.ports?.joinToString("\n    ") { "# ${it.port}: ${it.role} (${it.type})" }}
    
    # Add your logic here
    pass

if __name__ == "__main__":
    main()
    """.trimIndent()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Python (Pybricks)", fontWeight = FontWeight.Bold)
            Row {
                Button(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Run")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { /* TODO */ }) {
                    Text("Export")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BlocksPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Visual Block Editor", style = MaterialTheme.typography.titleLarge)
            Text("Blockly integration coming soon.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(Modifier.width(200.dp))
        }
    }
}
