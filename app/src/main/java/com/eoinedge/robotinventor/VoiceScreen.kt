package com.eoinedge.robotinventor

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun VoiceScreen(mcpClient: MindstormsMcpClient) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    resultText = matches[0]
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    resultText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        onDispose { recognizer.destroy() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Voice Observation", style = MaterialTheme.typography.headlineMedium)
        Text("Record what the robot is doing hands-free.", style = MaterialTheme.typography.bodySmall)
        
        Spacer(Modifier.height(32.dp))

        FloatingActionButton(
            onClick = {
                if (isListening) {
                    recognizer.stopListening()
                } else {
                    recognizer.startListening(intent)
                }
            },
            containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Listen", tint = Color.White)
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = resultText,
            onValueChange = { resultText = it },
            label = { Text("Observation") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = sessionId,
            onValueChange = { sessionId = it },
            label = { Text("Session ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    mcpClient.appendObservation(sessionId, resultText)
                    resultText = ""
                }
            },
            enabled = resultText.isNotEmpty() && sessionId.isNotEmpty()
        ) {
            Text("Send to Builder Session")
        }
    }
}
