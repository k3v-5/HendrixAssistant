package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.ai.memory.EmbeddingEngine
import com.asistente.celular.ai.memory.FeatureHashingEmbeddingEngine
import com.asistente.celular.ai.memory.MemoryEntry
import com.asistente.celular.ai.memory.MemorySearchResult
import com.asistente.celular.ai.memory.SemanticMemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repositorio vectorial reactivo para memoria semántica a largo plazo.
 * Persiste los recuerdos y sus vectores densos en 'semantic_memory.json'.
 */
class JsonSemanticMemoryRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val embeddingEngine: EmbeddingEngine = FeatureHashingEmbeddingEngine(dimensions = 128)
) : SemanticMemoryRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "semantic_memory.json")
    private val mutex = Mutex()
    private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    override val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            loadMemories()
        }
    }

    override suspend fun remember(
        text: String,
        category: String,
        tags: List<String>
    ): MemoryEntry {
        val vector = embeddingEngine.generateEmbedding(text)
        val entry = MemoryEntry(
            text = text,
            category = category,
            embedding = vector,
            tags = tags
        )

        mutex.withLock {
            val current = _memories.value.toMutableList()
            current.add(entry)
            _memories.value = current
            persistToFile(current)
        }

        return entry
    }

    override suspend fun search(
        query: String,
        topK: Int,
        minSimilarity: Float
    ): List<MemorySearchResult> {
        val queryVector = embeddingEngine.generateEmbedding(query)
        val all = _memories.value

        val results = all.mapNotNull { entry ->
            val similarity = if (entry.embedding.isNotEmpty()) {
                embeddingEngine.cosineSimilarity(queryVector, entry.embedding)
            } else 0f

            if (similarity >= minSimilarity) {
                MemorySearchResult(entry = entry, similarityScore = similarity)
            } else null
        }

        return results.sortedByDescending { it.similarityScore }.take(topK)
    }

    override suspend fun getAllMemories(): List<MemoryEntry> {
        return _memories.value
    }

    override suspend fun forget(id: String): Boolean {
        return mutex.withLock {
            val current = _memories.value.toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                _memories.value = current
                persistToFile(current)
            }
            removed
        }
    }

    override suspend fun forgetByQuery(query: String): Int {
        val queryLower = query.lowercase().trim()
        val matches = search(query, topK = 5, minSimilarity = 0.30f).map { it.entry.id }.toSet()

        return mutex.withLock {
            val current = _memories.value.toMutableList()
            val removedCount = current.count { it.id in matches || it.text.lowercase().contains(queryLower) }
            current.removeAll { it.id in matches || it.text.lowercase().contains(queryLower) }

            if (removedCount > 0) {
                _memories.value = current
                persistToFile(current)
            }
            removedCount
        }
    }

    private suspend fun loadMemories() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.exists()) {
                _memories.value = emptyList()
                return@withContext
            }

            try {
                val content = file.readText()
                val list = json.decodeFromString<List<MemoryEntry>>(content)
                _memories.value = list
            } catch (_: Exception) {
                _memories.value = emptyList()
            }
        }
    }

    private fun persistToFile(list: List<MemoryEntry>) {
        try {
            val content = json.encodeToString(list)
            val tempFile = File(context.filesDir, "semantic_memory.json.tmp")
            tempFile.writeText(content)
            tempFile.renameTo(file)
        } catch (_: Exception) {}
    }
}
