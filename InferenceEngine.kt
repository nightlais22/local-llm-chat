package com.example.localllm.llm

/** Inference-слой: реализация отвечает за загрузку модели и генерацию. */
interface InferenceEngine {
    suspend fun loadModel(path: String): Result<Unit>
    suspend fun generate(prompt: String, onToken: (String) -> Unit): Result<String>
    fun isLoaded(): Boolean
    fun unload()
}
