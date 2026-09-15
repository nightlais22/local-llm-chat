package com.example.localllm.llm

/** Заглушка, чтобы UI работал до подключения llama.cpp. */
class DummyEngine : InferenceEngine {
    @Volatile private var loaded = false
    private var path: String = ""

    override suspend fun loadModel(path: String): Result<Unit> {
        this.path = path
        loaded = true
        return Result.success(Unit)
    }

    override suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String> {
        if (!loaded) return Result.failure(IllegalStateException("Модель не загружена"))
        val reply = "[DummyEngine, модель: $path]\nПолучил: $prompt\n\nПодключи llama.cpp JNI (см. README)."
        reply.chunked(4).forEach { onToken(it); kotlinx.coroutines.delay(30) }
        return Result.success(reply)
    }

    override fun isLoaded() = loaded
    override fun unload() { loaded = false }
}
