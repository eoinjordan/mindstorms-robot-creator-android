package com.eoinedge.robotinventor

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { RobotDatabase.getDatabase(context) }
    val sessions by database.sessionDao().getAllSessions().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedSession by remember { mutableStateOf<StoredSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Probe History") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No sessions recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        onClick = { selectedSession = session },
                        onDelete = {
                            scope.launch {
                                database.sessionDao().deleteSession(session)
                            }
                        },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, session.jsonPayload)
                                putExtra(Intent.EXTRA_SUBJECT, "Mindstorms Probe Session: ${session.label}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Session JSON"))
                        }
                    )
                }
            }
        }

        if (selectedSession != null) {
            SessionDetailDialog(
                session = selectedSession!!,
                onDismiss = { selectedSession = null }
            )
        }
    }
}

@Composable
fun SessionItem(session: StoredSession, onClick: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.label.ifEmpty { "Untitled Session" }, fontWeight = FontWeight.Bold)
                Text("${session.profileId} • $date", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF006A6A))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(session: StoredSession, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(session.label.ifEmpty { "Session Detail" }) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Notes: ${session.notes}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("JSON Payload:", fontWeight = FontWeight.Bold)
                Text(
                    text = session.jsonPayload,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
