package com.example.localllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import com.example.localllm.voice.AndroidSpeechEngine
import com.example.localllm.voice.AndroidTtsEngine

class MainActivity : ComponentActivity() {

    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val ui by vm.ui.collectAsStateWithLifecycle()
                var input by remember { mutableStateOf("") }
                val listState = rememberLazyListState()

                val speech = remember { AndroidSpeechEngine(this@MainActivity) }
                val tts = remember { AndroidTtsEngine(this@MainActivity) }
                var voiceOutEnabled by remember { mutableStateOf(true) }
                var isListening by remember { mutableStateOf(false) }
                DisposableEffect(Unit) { onDispose { speech.stopListening(); tts.shutdown() } }

                // Озвучка готового ответа целиком (не по токенам)
                LaunchedEffect(ui.isGenerating) {
                    if (!ui.isGenerating && voiceOutEnabled) {
                        ui.messages.lastOrNull { it.role == "assistant" }?.let { tts.speak(it.text) }
                    }
                }

                val pickModel = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let { vm.onModelPicked(it, it.lastPathSegment ?: "model.gguf") } }

                LaunchedEffect(ui.messages.size) {
                    if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.size - 1)
                }

                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ui.status, style = MaterialTheme.typography.bodySmall)
                        Row {
                            IconButton(onClick = {
                                voiceOutEnabled = !voiceOutEnabled
                                if (!voiceOutEnabled) tts.stop()
                            }) {
                                Icon(
                                    if (voiceOutEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Озвучка"
                                )
                            }
                            Button(onClick = { pickModel.launch(arrayOf("*/*")) }) {
                                Text("Выбрать GGUF")
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(ui.messages) { msg ->
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.role == "user")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    msg.text,
                                    Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Сообщение…") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = {
                            if (isListening) { speech.stopListening(); isListening = false }
                            else {
                                isListening = true
                                speech.startListening(
                                    onResult = { text -> input = text; isListening = false },
                                    onError = { isListening = false }
                                )
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic, contentDescription = "Голос",
                                tint = if (isListening) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { vm.send(input); input = "" },
                            enabled = !ui.isGenerating
                        ) { Text("→") }
                    }
                }
            }
        }
    }
}
