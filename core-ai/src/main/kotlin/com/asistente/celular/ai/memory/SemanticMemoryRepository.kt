package com.asistente.celular.ai.memory

import kotlinx.coroutines.flow.StateFlow

/**
 * Resultado de una búsqueda vectorial semántica.
 */
data class MemorySearchResult(
    val entry: MemoryEntry,
    val similarityScore: Float
)

/**
 * Contrato abstracto para el almacenamiento y búsqueda semántica de recuerdos a largo plazo.
 */
interface SemanticMemoryRepository {

    /**
     * Flujo reactivo con la totalidad de recuerdos guardados en memoria permanente.
     */
    val memories: StateFlow<List<MemoryEntry>>

    /**
     * Almacena un nuevo hecho o preferencia en la memoria a largo plazo calculando su vector semántico.
     */
    suspend fun remember(
        text: String,
        category: String = "general",
        tags: List<String> = emptyList()
    ): MemoryEntry

    /**
     * Realiza una búsqueda por similitud vectorial coseno retornando las coincidencias más afines.
     */
    suspend fun search(
        query: String,
        topK: Int = 3,
        minSimilarity: Float = 0.25f
    ): List<MemorySearchResult>

    /**
     * Obtiene todos los recuerdos existentes.
     */
    suspend fun getAllMemories(): List<MemoryEntry>

    /**
     * Elimina un recuerdo por su identificador.
     */
    suspend fun forget(id: String): Boolean

    /**
     * Busca y elimina recuerdos que coincidan semánticamente con la consulta de eliminación.
     */
    suspend fun forgetByQuery(query: String): Int
}
