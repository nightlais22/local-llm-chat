package com.example.localllm.rag

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Временная реализация: SQLite + LIKE-поиск.
 * TODO: заменить на эмбеддинги (llama.cpp embedding endpoint) + cosine similarity,
 * интерфейс RagEngine менять не придётся.
 */
class SqliteRagEngine(context: Context) : RagEngine {

    private val db: SQLiteDatabase = context.openOrCreateDatabase("rag.db", Context.MODE_PRIVATE, null)

    init {
        db.execSQL("CREATE TABLE IF NOT EXISTS documents (id TEXT PRIMARY KEY, text TEXT)")
    }

    override suspend fun ingest(docs: List<Document>): Result<Unit> = runCatching {
        docs.forEach { d ->
            db.execSQL(
                "INSERT OR REPLACE INTO documents (id, text) VALUES (?, ?)",
                arrayOf(d.id, d.text)
            )
        }
    }

    override suspend fun search(query: String, topK: Int): List<SearchResult> {
        val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        val where = terms.joinToString(" AND ") { "text LIKE ?" }
        val args = terms.map { "%$it%" }.toTypedArray()
        val cursor = db.rawQuery(
            "SELECT id, text FROM documents WHERE $where LIMIT ?", arrayOf(*args, topK.toString())
        )
        val out = mutableListOf<SearchResult>()
        while (cursor.moveToNext()) {
            out.add(SearchResult(cursor.getString(0), cursor.getString(1).take(300), 1f))
        }
        cursor.close()
        return out
    }
}
