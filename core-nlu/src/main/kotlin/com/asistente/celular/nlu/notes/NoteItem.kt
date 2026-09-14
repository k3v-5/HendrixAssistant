package com.asistente.celular.nlu.notes

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Modelo de datos inmutable para Notas (Google Keep style).
 * Conforme a las reglas de escalabilidad preventiva y modelos evolutivos.
 */
@Serializable
data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String,
    val isPinned: Boolean = false,
    val category: String = DEFAULT_CATEGORY,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val DEFAULT_CATEGORY = "General"
    }
}
