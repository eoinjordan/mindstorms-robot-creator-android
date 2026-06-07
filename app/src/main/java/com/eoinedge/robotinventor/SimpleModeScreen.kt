package com.eoinedge.robotinventor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Kid-friendly "Simple Mode" — a full-screen Blockly coding canvas designed as
 * the primary teaching interface for children. The complex tabs (Fleet, Builder,
 * Probe, Data, Settings) are hidden so kids only see big colourful blocks and a
 * single "Run on Robot" button. Tap the gear to return to Advanced mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleModeScreen(
    profile: RobotProfile?,
    onExitSimpleMode: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val kidPurple = Color(0xFF6D28D9)
    val kidBlue = Color(0xFF1D4ED8)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF1A1A2E),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(kidPurple, kidBlue)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "🤖 Robot Coder",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            profile?.name ?: "Pick a robot in Advanced mode",
                            color = Color(0xFFDBEAFE),
                            fontSize = 13.sp
                        )
                    }
                    // Discreet exit to advanced mode (for teachers/parents)
                    OutlinedButton(
                        onClick = onExitSimpleMode,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Advanced")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Tap Advanced, choose a robot in the Fleet tab,\nthen come back to start coding!",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Big, kid-friendly Blockly canvas. The in-canvas green
                // "Run on Robot" button drives the export-to-LEGO flow below.
                BlocklyEditor(kidsMode = true) { code ->
                    exportLmsFile(context, profile, "blocks", code)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Great job! 🎉 Your program is ready — open it in the LEGO app to run it."
                        )
                    }
                }
            }
        }
    }
}
