package com.asistente.celular.ai.rag

import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.tasks.TaskItem

/**
 * Instantánea del contexto personal del usuario para enriquecer las consultas generativas a la IA.
 */
data class PersonalContextSnapshot(
    val pendingTasks: List<TaskItem> = emptyList(),
    val relevantNotes: List<NoteItem> = emptyList(),
    val currentDateFormatted: String = "",
    val formattedContextPrompt: String = ""
)

/**
 * Contrato para proveedores de contexto personal (RAG y memoria del usuario).
 * Conforme a GEMINI.md (desacoplamiento por contratos e interfaces, escalabilidad preventiva).
 * Permite cambiar la implementación subyacente (búsqueda léxica, búsqueda semántica vectorial,
 * índices SQLite-vec, etc.) sin modificar las capas superiores de IA.
 */
interface PersonalContextProvider {
    /**
     * Genera una instantánea del contexto relevante para la consulta del usuario.
     */
    suspend fun getContextSnapshot(userQuery: String): PersonalContextSnapshot

    /**
     * Enriquece el systemPrompt base del asistente con la información personal relevante.
     */
    suspend fun buildEnrichedSystemPrompt(baseSystemPrompt: String, userQuery: String): String
}
