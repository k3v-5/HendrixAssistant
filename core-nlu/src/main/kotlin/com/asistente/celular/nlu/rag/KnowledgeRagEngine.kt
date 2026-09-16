package com.asistente.celular.nlu.rag

enum class EntityType {
    PERSON,
    PLACE,
    PREFERENCE,
    EVENT,
    TOPIC,
    OBJECT,
    GENERAL
}

data class KnowledgeEntity(
    val id: String,
    val name: String,
    val type: EntityType = EntityType.GENERAL,
    val attributes: Map<String, String> = emptyMap(),
    val notes: String = "",
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class KnowledgeRelation(
    val sourceEntityId: String,
    val relationship: String,
    val targetEntityId: String,
    val context: String = ""
)

data class VectorEmbedding(
    val values: FloatArray
) {
    val dimension: Int get() = values.size

    fun cosineSimilarity(other: VectorEmbedding): Float {
        if (values.size != other.values.size || values.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in values.indices) {
            dot += values[i] * other.values[i]
            normA += values[i] * values[i]
            normB += other.values[i] * other.values[i]
        }
        val denom = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorEmbedding) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }
}

data class KnowledgeSearchResult(
    val entity: KnowledgeEntity?,
    val textSnippet: String,
    val similarityScore: Float,
    val matchedKeywords: List<String> = emptyList()
)

/**
 * Contrato de repositorio y motor para el grafo de conocimiento personal
 * y RAG vectorial local embebido en Hendrix.
 */
interface KnowledgeGraphRepository {
    suspend fun saveEntity(entity: KnowledgeEntity)
    suspend fun linkEntities(relation: KnowledgeRelation)
    suspend fun searchRelevantKnowledge(query: String, topK: Int = 5): List<KnowledgeSearchResult>
    suspend fun getAllEntities(): List<KnowledgeEntity>
    suspend fun getAllRelations(): List<KnowledgeRelation>
    suspend fun clearAll()
}
