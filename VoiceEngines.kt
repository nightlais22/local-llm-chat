package com.example.localllm.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Распознавание речи (speech-to-text). */
interface SpeechEngine {
    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
}

/** Синтез речи (text-to-speech). */
interface TtsEngine {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}

/**
 * Реализация на системном SpeechRecognizer.
 * Работает офлайн, если скачан языковой пакет.
 * TODO: локальная Whisper-модель через llama.cpp вместо системного движка.
 */
class AndroidSpeechEngine(private val context: Context) : SpeechEngine {

    private var recognizer: SpeechRecognizer? = null

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Распознавание речи недоступно")
            return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                if (text.isNotBlank()) onResult(text)
                stopListening()
            }
            override fun onError(error: Int) { onError("Ошибка распознавания: $error"); stopListening() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.destroy()
        recognizer = null
    }
}

/** Реализация на системном TextToSpeech (офлайн, установленные голоса). */
class AndroidTtsEngine(context: Context) : TtsEngine {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var pending: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.language = Locale.getDefault()
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    override fun speak(text: String) {
        if (!ready) { pending = text; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    override fun stop() { tts?.stop() }

    override fun shutdown() { tts?.shutdown(); tts = null }
}
