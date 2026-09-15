package com.example.localllm.rag

/** Документ для индексации. */
data class Document(val id: String, val text: String)

/** Результат поиска. */
data class SearchResult(val docId: String, val snippet: String, val score: Float)

/** RAG-слой: сейчас заглушка на SQLite, потом — эмбеддинги через llama.cpp. */
interface RagEngine {
    suspend fun ingest(docs: List<Document>): Result<Unit>
    suspend fun search(query: String, topK: Int = 4): List<SearchResult>
}
