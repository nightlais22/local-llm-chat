package com.example.localllm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localllm.llm.InferenceEngine
import com.example.localllm.llm.DummyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val modelPath: String? = null,
    val isGenerating: Boolean = false,
    val status: String = "Модель не выбрана"
)

class ChatViewModel : ViewModel() {

    private val engine: InferenceEngine = DummyEngine() // TODO: заменить на LlamaCppEngine

    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui

    fun onModelPicked(uri: Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.value = _ui.value.copy(status = "Загрузка модели…")
            engine.loadModel(uri.toString())
                .onSuccess {
                    _ui.value = _ui.value.copy(modelPath = name, status = "Готово: $name")
                }
                .onFailure {
                    _ui.value = _ui.value.copy(status = "Ошибка загрузки: ${it.message}")
                }
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        _ui.value = _ui.value.copy(
            messages = _ui.value.messages + ChatMessage("user", text),
            isGenerating = true
        )
        viewModelScope.launch(Dispatchers.IO) {
            val full = StringBuilder()
            engine.generate(text) { token ->
                full.append(token)
                _ui.value = _ui.value.copy(
                    messages = _ui.value.messages.dropLastWhile { it.role == "assistant" } +
                        ChatMessage("assistant", full.toString())
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(status = "Ошибка: ${e.message}")
            }
            _ui.value = _ui.value.copy(isGenerating = false)
        }
    }
}
