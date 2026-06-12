package com.eoinedge.robotinventor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

class MainActivity : ComponentActivity() {
    private lateinit var simulatedTransport: SimulatedTransport
    private lateinit var bleTransport: SpikeBleTransport
    private var currentTransport by mutableStateOf<RobotTransport?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        simulatedTransport = SimulatedTransport()
        bleTransport = SpikeBleTransport(this)
        currentTransport = simulatedTransport

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("settings", MODE_PRIVATE) }
            val serverUrl = remember { mutableStateOf(prefs.getString("mcp_server_url", "http://10.0.2.2:3095") ?: "http://10.0.2.2:3095") }

            val mcpClient = remember(serverUrl.value) {
                if (serverUrl.value.contains("fake")) FakeMindstormsMcpClient()
                else HttpMindstormsMcpClient(serverUrl.value)
            }

            var simpleMode by remember { mutableStateOf(prefs.getBoolean("simple_mode", true)) }
            var advancedMode by remember { mutableStateOf(prefs.getBoolean("advanced_mode", false)) }

            MainAppShell(
                transport = currentTransport!!,
                mcpClient = mcpClient,
                isSimulated = currentTransport is SimulatedTransport,
                onTransportChange = { isSim ->
                    currentTransport = if (isSim) simulatedTransport else bleTransport
                },
                onRefreshSettings = {
                    serverUrl.value = prefs.getString("mcp_server_url", "http://10.0.2.2:3095") ?: "http://10.0.2.2:3095"
                },
                simpleMode = simpleMode,
                onSetSimpleMode = { enabled ->
                    simpleMode = enabled
                    prefs.edit().putBoolean("simple_mode", enabled).apply()
                },
                advancedMode = advancedMode,
                onSetAdvancedMode = { enabled ->
                    advancedMode = enabled
                    prefs.edit().putBoolean("advanced_mode", enabled).apply()
                }
            )
        }
    }
}

enum class Screen(val title: String, val icon: ImageVector) {
    FLEET("Fleet", Icons.Default.Home),
    BUILDER("Builder", Icons.Default.Build),
    VOICE("Voice", Icons.Default.Mic),
    PROBE("Probe", Icons.Default.PlayArrow),
    CODE("Code", Icons.Default.Edit),
    DATA("Data", Icons.Default.List),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainAppShell(
    transport: RobotTransport,
    mcpClient: MindstormsMcpClient,
    isSimulated: Boolean,
    onTransportChange: (Boolean) -> Unit,
    onRefreshSettings: () -> Unit,
    simpleMode: Boolean,
    onSetSimpleMode: (Boolean) -> Unit,
    advancedMode: Boolean,
    onSetAdvancedMode: (Boolean) -> Unit
) {
    val darkTeal = Color(0xFF006A6A)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = darkTeal,
            secondary = Color(0xFF008F8F),
            surface = Color(0xFF172026)
        )
    ) {
        var currentScreen by remember { mutableStateOf(Screen.FLEET) }
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val isExpanded = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        
        val context = LocalContext.current
        val repository = remember { ProfileRepository(context) }
        val profiles = remember { repository.loadProfiles() }
        var selectedProfile by remember {
            mutableStateOf(profiles.firstOrNull { it.family == "wedo2" } ?: profiles.firstOrNull())
        }
        val visibleScreens = remember(advancedMode) {
            if (advancedMode) Screen.entries.toList()
            else listOf(Screen.FLEET, Screen.BUILDER, Screen.CODE, Screen.SETTINGS)
        }

        // Simple (Kids) Mode takes over the whole screen with a big Blockly canvas.
        if (simpleMode) {
            SimpleModeScreen(
                profile = selectedProfile ?: profiles.firstOrNull { it.family == "wedo2" },
                onExitSimpleMode = { onSetSimpleMode(false) }
            )
            return@MaterialTheme
        }

        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    header = {
                        KidsModeButton(onClick = { onSetSimpleMode(true) }, compact = true)
                    }
                ) {
                    visibleScreens.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    ScreenContent(
                        screen = currentScreen,
                        transport = transport,
                        mcpClient = mcpClient,
                        isSimulated = isSimulated,
                        onTransportChange = onTransportChange,
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        onProfileSelect = { selectedProfile = it },
                        onBack = { currentScreen = Screen.FLEET },
                        onRefreshSettings = onRefreshSettings,
                        simpleMode = simpleMode,
                        onSetSimpleMode = onSetSimpleMode,
                        advancedMode = advancedMode,
                        onSetAdvancedMode = onSetAdvancedMode
                    )
                }
            }
        } else {
            Scaffold(
                floatingActionButton = {
                    KidsModeButton(onClick = { onSetSimpleMode(true) }, compact = false)
                },
                bottomBar = {
                    NavigationBar {
                        visibleScreens.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    ScreenContent(
                        screen = currentScreen,
                        transport = transport,
                        mcpClient = mcpClient,
                        isSimulated = isSimulated,
                        onTransportChange = onTransportChange,
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        onProfileSelect = { selectedProfile = it },
                        onBack = { currentScreen = Screen.FLEET },
                        onRefreshSettings = onRefreshSettings,
                        simpleMode = simpleMode,
                        onSetSimpleMode = onSetSimpleMode,
                        advancedMode = advancedMode,
                        onSetAdvancedMode = onSetAdvancedMode
                    )
                }
            }
        }
    }
}

/** Prominent entry point into the kid-friendly Simple Mode. */
@Composable
private fun KidsModeButton(onClick: () -> Unit, compact: Boolean) {
    if (compact) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Face, contentDescription = "Simple Mode for kids")
        }
    } else {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Default.Face, contentDescription = null) },
            text = { Text("Kids Mode") },
            containerColor = Color(0xFF6D28D9),
            contentColor = Color.White
        )
    }
}

@Composable
fun ScreenContent(
    screen: Screen,
    transport: RobotTransport,
    mcpClient: MindstormsMcpClient,
    isSimulated: Boolean,
    onTransportChange: (Boolean) -> Unit,
    profiles: List<RobotProfile>,
    selectedProfile: RobotProfile?,
    onProfileSelect: (RobotProfile) -> Unit,
    onBack: () -> Unit,
    onRefreshSettings: () -> Unit,
    simpleMode: Boolean,
    onSetSimpleMode: (Boolean) -> Unit,
    advancedMode: Boolean,
    onSetAdvancedMode: (Boolean) -> Unit
) {
    when (screen) {
        Screen.FLEET -> MindstormsFleetScreen(
            transport = transport,
            profiles = profiles,
            selectedProfile = selectedProfile,
            onProfileSelect = onProfileSelect,
            isSimulated = isSimulated,
            onTransportChange = onTransportChange
        )
        Screen.BUILDER -> BuilderSessionScreen(
            profile = selectedProfile,
            mcpClient = mcpClient
        )
        Screen.VOICE -> VoiceScreen(
            mcpClient = mcpClient
        )
        Screen.PROBE -> ProbeScreen(
            transport = transport,
            profile = selectedProfile
        )
        Screen.CODE -> CodeScreen(
            profile = selectedProfile,
            mcpClient = mcpClient
        )
        Screen.DATA -> SessionHistoryScreen(onBack = onBack)
        Screen.SETTINGS -> SettingsScreen(onBack = {
            onRefreshSettings()
            onBack()
        }, simpleMode = simpleMode, onSimpleModeChange = onSetSimpleMode, advancedMode = advancedMode, onAdvancedModeChange = onSetAdvancedMode)
    }
}
