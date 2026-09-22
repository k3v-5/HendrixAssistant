package com.asistente.celular.ai.memory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Tipo de ítem personal indexado en la base de conocimiento RAG.
 */
enum class RagItemType {
    NOTE,
    TASK,
    MEMORY,
    PC_DOCUMENT
}

/**
 * Elemento indexado en el espacio vectorial personal.
 */
data class RagIndexItem(
    val id: String,
    val type: RagItemType,
    val title: String,
    val textContent: String,
    val metadata: Map<String, String> = emptyMap(),
    val embedding: List<Float>
)

/**
 * Resultado de coincidencia semántica devuelta por el coordinador RAG.
 */
data class RagSearchResult(
    val item: RagIndexItem,
    val similarityScore: Float
)

/**
 * Coordinador de Búsqueda Semántica Vectorial y RAG Personal (Retrieval-Augmented Generation).
 * 
 * Indexa unificadamente Notas, Tareas, Memorias Semánticas y Documentos de PC en un espacio
 * vectorial local denso, permitiendo recuperar información relevante y enriquecer los prompts del LLM.
 */
class PersonalRagCoordinator(
    private val embeddingEngine: EmbeddingEngine = FeatureHashingEmbeddingEngine(dimensions = 128),
    private val semanticMemoryRepository: SemanticMemoryRepository? = null
) {
    private val itemStore = ConcurrentHashMap<String, RagIndexItem>()
    private val mutex = Mutex()

    /**
     * Indexa o actualiza una colección de Notas personales.
     */
    suspend fun indexNotes(notes: List<com.asistente.celular.nlu.notes.NoteItem>) {
        for (note in notes) {
            indexNote(note.id, note.title, note.content, listOf(note.category))
        }
    }

    /**
     * Indexa o actualiza una Nota personal.
     */
    suspend fun indexNote(id: String, title: String, content: String, tags: List<String> = emptyList()) {
        val fullText = "$title. $content ${tags.joinToString(" ")}"
        val vector = embeddingEngine.generateEmbedding(fullText)
        val item = RagIndexItem(
            id = "note_$id",
            type = RagItemType.NOTE,
            title = title,
            textContent = content,
            metadata = mapOf("tags" to tags.joinToString(",")),
            embedding = vector
        )
        itemStore[item.id] = item
    }

    /**
     * Indexa o actualiza una colección de Tareas del usuario.
     */
    suspend fun indexTasks(tasks: List<com.asistente.celular.nlu.tasks.TaskItem>) {
        for (task in tasks) {
            indexTask(
                id = task.id,
                title = task.title,
                category = task.listName,
                dueTime = task.dueDateMillis?.toString(),
                isCompleted = task.isCompleted
            )
        }
    }

    /**
     * Indexa o actualiza una Tarea del usuario.
     */
    suspend fun indexTask(id: String, title: String, category: String = "general", dueTime: String? = null, isCompleted: Boolean = false) {
        val fullText = "Tarea: $title. Categoría: $category. ${if (isCompleted) "Completada." else "Pendiente."} ${dueTime ?: ""}"
        val vector = embeddingEngine.generateEmbedding(fullText)
        val item = RagIndexItem(
            id = "task_$id",
            type = RagItemType.TASK,
            title = title,
            textContent = fullText,
            metadata = mapOf(
                "category" to category,
                "isCompleted" to isCompleted.toString(),
                "dueTime" to (dueTime ?: "")
            ),
            embedding = vector
        )
        itemStore[item.id] = item
    }

    /**
     * Indexa un fragmento de documento o código proveniente de la PC de trabajo.
     */
    suspend fun indexPcDocument(id: String, fileName: String, contentSnippet: String) {
        val fullText = "Documento PC $fileName: $contentSnippet"
        val vector = embeddingEngine.generateEmbedding(fullText)
        val item = RagIndexItem(
            id = "pcdoc_$id",
            type = RagItemType.PC_DOCUMENT,
            title = fileName,
            textContent = contentSnippet,
            metadata = mapOf("fileName" to fileName),
            embedding = vector
        )
        itemStore[item.id] = item
    }

    /**
     * Elimina un elemento del índice vectorial.
     */
    fun removeItem(id: String) {
        itemStore.remove(id)
        itemStore.remove("note_$id")
        itemStore.remove("task_$id")
        itemStore.remove("pcdoc_$id")
    }

    /**
     * Realiza una búsqueda por similitud vectorial coseno en todas las fuentes de datos.
     */
    suspend fun search(
        query: String,
        maxResults: Int = 4,
        minSimilarity: Float = 0.28f
    ): List<RagSearchResult> = withContext(Dispatchers.Default) {
        val queryVector = embeddingEngine.generateEmbedding(query)
        val results = mutableListOf<RagSearchResult>()

        // 1. Buscar en elementos locales indexados (notas, tareas, docs PC)
        for (item in itemStore.values) {
            val sim = embeddingEngine.cosineSimilarity(queryVector, item.embedding)
            if (sim >= minSimilarity) {
                results.add(RagSearchResult(item, sim))
            }
        }

        // 2. Buscar en memoria semántica a largo plazo si está disponible
        semanticMemoryRepository?.let { repo ->
            try {
                val memResults = repo.search(query, topK = maxResults, minSimilarity = minSimilarity)
                for (mr in memResults) {
                    val memItem = RagIndexItem(
                        id = "mem_${mr.entry.id}",
                        type = RagItemType.MEMORY,
                        title = "Recuerdo: ${mr.entry.category}",
                        textContent = mr.entry.text,
                        metadata = mapOf("category" to mr.entry.category),
                        embedding = mr.entry.embedding
                    )
                    results.add(RagSearchResult(memItem, mr.similarityScore))
                }
            } catch (_: Exception) {}
        }

        results.sortedByDescending { it.similarityScore }.take(maxResults)
    }

    /**
     * Construye un bloque sintético de contexto en formato Markdown para enriquecer el prompt del LLM.
     * Si no hay coincidencias relevantes que superen el umbral, retorna cadena vacía.
     */
    suspend fun buildAugmentedContextBlock(
        query: String,
        maxResults: Int = 3,
        minSimilarity: Float = 0.32f
    ): String {
        val matches = search(query, maxResults, minSimilarity)
        if (matches.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("\n[Contexto Personal Recuperado (RAG)]:\n")
        for (m in matches) {
            val prefix = when (m.item.type) {
                RagItemType.NOTE -> "• [Nota: ${m.item.title}]"
                RagItemType.TASK -> "• [Tarea]"
                RagItemType.MEMORY -> "• [Recuerdo Personal]"
                RagItemType.PC_DOCUMENT -> "• [Archivo PC: ${m.item.title}]"
            }
            sb.append("$prefix ${m.item.textContent}\n")
        }
        return sb.toString()
    }
}
