package com.asistente.celular.ai.memory

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa una unidad atómica de información recordada a largo plazo sobre el usuario
 * (preferencias, datos personales, restricciones de salud, gustos, etc.).
 */
@Serializable
data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val category: String = "general", // "preference", "fact", "health", "personal", "general"
    val embedding: List<Float> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
