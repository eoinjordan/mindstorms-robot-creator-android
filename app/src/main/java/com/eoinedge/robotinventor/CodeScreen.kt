package com.eoinedge.robotinventor

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
