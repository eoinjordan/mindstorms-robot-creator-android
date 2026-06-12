package com.eoinedge.robotinventor

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val INTENTS = listOf("beep", "drive", "wave", "probe")

internal data class CodeTarget(val value: String, val label: String)

@Composable
fun CodeScreen(profile: RobotProfile?, mcpClient: MindstormsMcpClient, transport: RobotTransport? = null) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a robot in the Fleet tab first.")
        }
        return
    }

    var selectedTab by remember(profile.id) { mutableStateOf(if (profile.family == "wedo2") 1 else 0) }
    val tabs = listOf("Code", "Blocks")
    val context = LocalContext.current
    var blockStatus by remember(profile.id) { mutableStateOf<String?>(null) }

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
            CodeEditor(profile, mcpClient, transport)
        } else {
            blockStatus?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            BlocklyEditor(kidsMode = profile.family == "wedo2") { code ->
                exportProgramFile(context, profile, "blocks", blockTargetFor(profile), code)
                blockStatus = "Exported ${downloadName(profile, "blocks", blockTargetFor(profile))}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditor(profile: RobotProfile, mcpClient: MindstormsMcpClient, transport: RobotTransport? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targets = remember(profile.id) { targetsFor(profile) }

    var selectedIntent by remember(profile.id) { mutableStateOf(INTENTS[0]) }
    var selectedTarget by remember(profile.id) { mutableStateOf(targets.first().value) }
    var intentMenuExpanded by remember { mutableStateOf(false) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var code by remember(profile.id) { mutableStateOf(defaultCode(profile, INTENTS[0], selectedTarget)) }
    var isGenerating by remember { mutableStateOf(false) }
    var isDeploying by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .width(116.dp)
                        .menuAnchor()
                )
                DropdownMenu(
                    expanded = intentMenuExpanded,
                    onDismissRequest = { intentMenuExpanded = false }
                ) {
                    INTENTS.forEach { intent ->
                        DropdownMenuItem(
                            text = { Text(intent.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedIntent = intent
                                intentMenuExpanded = false
                                code = defaultCode(profile, intent, selectedTarget)
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = targetMenuExpanded,
                onExpandedChange = { targetMenuExpanded = !targetMenuExpanded }
            ) {
                OutlinedTextField(
                    value = targets.first { it.value == selectedTarget }.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier
                        .weight(1f)
                        .menuAnchor()
                )
                DropdownMenu(
                    expanded = targetMenuExpanded,
                    onDismissRequest = { targetMenuExpanded = false }
                ) {
                    targets.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(target.label) },
                            onClick = {
                                selectedTarget = target.value
                                targetMenuExpanded = false
                                code = defaultCode(profile, selectedIntent, target.value)
                            }
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        try {
                            val generated = mcpClient.generateCode(
                                profile.id,
                                serverIntent(selectedIntent),
                                selectedTarget
                            )
                            if (generated.isNotBlank() && !generated.contains("\"ok\": false")) code = generated
                        } catch (_: Exception) {
                            code = defaultCode(profile, selectedIntent, selectedTarget)
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating
            ) {
                if (isGenerating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Generate")
            }

            OutlinedButton(
                onClick = {
                    exportProgramFile(context, profile, selectedIntent, selectedTarget, code)
                    exportStatus = "Exported ${downloadName(profile, selectedIntent, selectedTarget)}"
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Export")
            }

            OutlinedButton(
                onClick = {
                    val deployer = transport as? ProgramDeployTransport
                    if (deployer == null) {
                        exportStatus = "Bluetooth flash is not available for the selected transport."
                        return@OutlinedButton
                    }
                    isDeploying = true
                    scope.launch {
                        val result = try {
                            deployer.deployProgram(profile, code)
                        } catch (e: Exception) {
                            ProgramDeployResult(false, "Bluetooth flash/run failed: ${e.message}")
                        }
                        exportStatus = result.message
                        isDeploying = false
                    }
                },
                enabled = !isDeploying
            ) {
                if (isDeploying) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Flash BLE")
            }
        }

        exportStatus?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

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

internal fun targetsFor(profile: RobotProfile): List<CodeTarget> {
    val values = if (profile.programTargets.isNotEmpty()) profile.programTargets else when (profile.family) {
        "wedo2" -> listOf("wedo2-micropython", "pybricks-city")
        "robot-inventor", "spike-prime" -> listOf("lego-stock-python", "pybricks-python")
        "ev3" -> listOf("pybricks-ev3", "ev3dev-python")
        "nxt" -> listOf("nxt-python")
        "rcx" -> listOf("rcx-nqc")
        "m5stack-basex" -> listOf("arduino-basex")
        else -> listOf("pybricks-python")
    }
    return values.map { CodeTarget(it, targetLabel(it)) }
}

internal fun targetLabel(target: String): String = when (target) {
    "wedo2-micropython" -> "WeDo 2.0 App"
    "pybricks-city" -> "Pybricks CityHub"
    "lego-stock-python" -> "LEGO App Python"
    "spike-stock" -> "LEGO/SPIKE App Python"
    "pybricks-python" -> "Pybricks Python"
    "pybricks-ev3" -> "Pybricks EV3"
    "ev3dev-python" -> "ev3dev Python"
    "nxt-python" -> "NXT Python"
    "rcx-nqc" -> "RCX NQC"
    "arduino-basex" -> "M5Stack BaseX Arduino"
    else -> target
}

internal fun blockTargetFor(profile: RobotProfile): String = when (profile.family) {
    "wedo2" -> "wedo2-micropython"
    "rcx" -> "rcx-nqc"
    "nxt" -> "nxt-python"
    "ev3" -> "ev3dev-python"
    "m5stack-basex" -> "arduino-basex"
    else -> "lego-stock-python"
}

internal fun serverIntent(intent: String): String = when (intent) {
    "beep" -> "beep_hello"
    "drive" -> "drive_forward"
    "probe" -> "safe_probe"
    else -> intent
}

internal fun extensionFor(target: String): String = when (target) {
    "lego-stock-python", "spike-stock" -> "lms"
    "rcx-nqc" -> "nqc"
    "arduino-basex" -> "ino"
    else -> "py"
}

internal fun downloadName(profile: RobotProfile, intent: String, target: String): String {
    val safeIntent = intent.replace(Regex("[^A-Za-z0-9_-]"), "-")
    return "${profile.id}-$safeIntent.${extensionFor(target)}"
}

internal fun defaultCode(profile: RobotProfile, intent: String, target: String): String {
    return when (target) {
        "wedo2-micropython" -> wedoCode(profile, intent)
        "pybricks-city" -> cityHubPybricksCode(profile, intent)
        "pybricks-ev3", "ev3dev-python" -> ev3Code(profile, intent, target)
        "nxt-python" -> nxtCode(profile, intent)
        "rcx-nqc" -> rcxCode(profile, intent)
        "arduino-basex" -> basexCode(profile, intent)
        else -> inventorCode(profile, intent)
    }
}

private fun header(profile: RobotProfile, target: String): String {
    val ports = profile.ports.joinToString("\n") { "# Port ${it.port}: ${it.role} (${it.type})" }
    return "# ${profile.name} - ${profile.kit.ifBlank { profile.family }}\n# Target: $target\n$ports\n"
}

private fun motors(profile: RobotProfile): List<ProfilePort> = profile.ports.filter { it.type == "motor" }

private fun firstMotor(profile: RobotProfile, fallback: String = "A"): String =
    motors(profile).firstOrNull()?.port ?: fallback

private fun driveMotors(profile: RobotProfile): List<ProfilePort> =
    motors(profile).filter { it.role.contains("drive", true) || it.role.contains("track", true) || it.role.contains("wheel", true) }
        .ifEmpty { motors(profile).take(2) }

private fun varName(role: String): String =
    role.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').lowercase().ifBlank { "motor" }

private fun wedoCode(profile: RobotProfile, intent: String): String {
    val bindings = motors(profile).joinToString("\n") { "${varName(it.role)} = hub.port.${it.port}.motor" }
    val mainMotor = varName(motors(profile).firstOrNull()?.role ?: "motor_a")
    val drive = driveMotors(profile).map { varName(it.role) }
    val body = when (intent) {
        "beep" -> "hub.led(6)\ntime.sleep(0.5)\nhub.led(0)"
        "drive" -> (drive.ifEmpty { listOf(mainMotor) }).joinToString("\n") { "$it.start(speed=35)" } +
            "\ntime.sleep(1)\n" + (drive.ifEmpty { listOf(mainMotor) }).joinToString("\n") { "$it.stop()" }
        "wave" -> "for _ in range(3):\n    $mainMotor.run_for_seconds(speed=35, seconds=0.3)\n    time.sleep(0.1)\n    $mainMotor.run_for_seconds(speed=-35, seconds=0.3)"
        else -> "$mainMotor.run_for_seconds(speed=30, seconds=0.5)\ntime.sleep(0.2)\n$mainMotor.run_for_seconds(speed=-30, seconds=0.5)\n$mainMotor.stop()"
    }
    return """
${header(profile, "WeDo 2.0 MicroPython")}
import hub
import time

$bindings

$body
""".trimIndent()
}

private fun cityHubPybricksCode(profile: RobotProfile, intent: String): String {
    val port = firstMotor(profile)
    return """
${header(profile, "Pybricks CityHub")}
from pybricks.hubs import CityHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port, Color
from pybricks.tools import wait

hub = CityHub()
motor = Motor(Port.$port)

${pybricksMotorBody(intent)}
""".trimIndent()
}

private fun inventorCode(profile: RobotProfile, intent: String): String {
    val port = firstMotor(profile)
    return """
${header(profile, "Pybricks/LEGO hub Python")}
from pybricks.hubs import InventorHub
from pybricks.pupdevices import Motor
from pybricks.parameters import Port
from pybricks.tools import wait

hub = InventorHub()
motor = Motor(Port.$port)

${pybricksMotorBody(intent)}
""".trimIndent()
}

private fun ev3Code(profile: RobotProfile, intent: String, target: String): String {
    val port = firstMotor(profile, "B")
    val hubClass = if (target == "pybricks-ev3") "EV3Brick" else "EV3Brick"
    return """
${header(profile, target)}
from pybricks.hubs import $hubClass
from pybricks.ev3devices import Motor
from pybricks.parameters import Port
from pybricks.tools import wait

ev3 = $hubClass()
motor = Motor(Port.$port)

${pybricksMotorBody(intent)}
""".trimIndent()
}

private fun pybricksMotorBody(intent: String): String = when (intent) {
    "beep" -> "hub.speaker.beep(440, 300) if 'hub' in globals() else ev3.speaker.beep()"
    "drive" -> "motor.run_time(250, 1000)\nmotor.stop()"
    "wave" -> "for _ in range(3):\n    motor.run_angle(250, 45)\n    wait(100)\n    motor.run_angle(250, -45)"
    else -> "motor.run_time(150, 500)\nwait(200)\nmotor.run_time(-150, 500)\nmotor.stop()"
}

private fun nxtCode(profile: RobotProfile, intent: String): String {
    val port = firstMotor(profile)
    return """
${header(profile, "nxt-python")}
import nxt.locator
from nxt.motor import Port, Motor
import time

brick = nxt.locator.find()
motor = Motor(brick, Port.$port)

${when (intent) {
        "drive" -> "motor.run(50)\ntime.sleep(1)\nmotor.brake()"
        "wave" -> "for _ in range(3):\n    motor.turn(50, 45)\n    motor.turn(50, -45)"
        "beep" -> "brick.play_tone(440, 300)"
        else -> "motor.run(35)\ntime.sleep(0.5)\nmotor.brake()"
    }}
""".trimIndent()
}

private fun rcxCode(profile: RobotProfile, intent: String): String {
    val drive = driveMotors(profile).map { "OUT_${it.port}" }.ifEmpty { listOf("OUT_A") }
    val outs = drive.joinToString(" + ")
    return """
// ${profile.name} - RCX NQC
// Target: rcx-nqc
task main()
{
${when (intent) {
        "beep" -> "  PlayTone(440, 30);"
        "drive" -> "  OnFwd($outs);\n  Wait(100);\n  Off($outs);"
        "wave" -> "  repeat(3) {\n    OnFwd(${drive.first()}); Wait(30);\n    OnRev(${drive.first()}); Wait(30);\n    Off(${drive.first()});\n  }"
        else -> "  OnFwd(${drive.first()}); Wait(50); Off(${drive.first()});"
    }}
}
""".trimIndent()
}

private fun basexCode(profile: RobotProfile, intent: String): String {
    return """
// ${profile.name} - M5Stack BaseX Arduino sketch
// Target: arduino-basex
// Keep duty low and stop all motors after each test.
void setup() {
  Serial.begin(115200);
}

void loop() {
  // TODO: connect to the BaseX adapter firmware command set.
  // Intent: $intent
  Serial.println("Run safe BaseX test for ${profile.id}");
  delay(1000);
}
""".trimIndent()
}

internal fun exportProgramFile(context: Context, profile: RobotProfile, intent: String, target: String, code: String) {
    val ext = extensionFor(target)
    if (ext == "lms") {
        exportLmsFile(context, profile, intent, code)
        return
    }
    val dir = File(context.cacheDir, "program_exports").also { it.mkdirs() }
    val file = File(dir, downloadName(profile, intent, target))
    file.writeText(code)
    shareFile(context, file, "text/plain", "${profile.name} - ${file.name}")
}

internal fun exportLmsFile(context: Context, profile: RobotProfile, intent: String, code: String) {
    val dir = File(context.cacheDir, "lms_exports").also { it.mkdirs() }
    val file = File(dir, "${profile.id}-$intent.lms")

    ZipOutputStream(file.outputStream().buffered()).use { zip ->
        zip.putNextEntry(ZipEntry("scratch.py"))
        zip.write(code.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    shareFile(context, file, "application/octet-stream", "${profile.name} - $intent.lms")
}

private fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share robot program"))
}

@Composable
fun BlocklyEditor(kidsMode: Boolean = false, onRunCode: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRunCode(code: String) { onRunCode(code) }
                }, "AndroidBridge")
                val url = "file:///android_asset/blockly_editor.html" + if (kidsMode) "?kids=1" else ""
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
