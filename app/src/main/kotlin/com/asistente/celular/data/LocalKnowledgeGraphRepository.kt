package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.nlu.rag.EntityType
import com.asistente.celular.nlu.rag.KnowledgeEntity
import com.asistente.celular.nlu.rag.KnowledgeGraphRepository
import com.asistente.celular.nlu.rag.KnowledgeRelation
import com.asistente.celular.nlu.rag.KnowledgeSearchResult
import com.asistente.celular.nlu.rag.VectorEmbedding
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Repositorio local de Grafo de Conocimiento y RAG Vectorial Embebido.
 * Proporciona indexación semántica y recuperación por similitud coseno offline.
 */
class LocalKnowledgeGraphRepository(
    private val context: Context
) : KnowledgeGraphRepository {

    private val entities = ConcurrentHashMap<String, KnowledgeEntity>()
    private val relations = mutableListOf<KnowledgeRelation>()

    init {
        // Sembrar entidades de conocimiento base
        val ownerEntity = KnowledgeEntity(
            id = "entity_owner",
            name = "Usuario",
            type = EntityType.PERSON,
            attributes = mapOf("rol" to "propietario", "idioma" to "es"),
            notes = "Dueño de Hendrix Assistant con privilegios completos de administración local."
        )
        val assistantEntity = KnowledgeEntity(
            id = "entity_hendrix",
            name = "Hendrix",
            type = EntityType.GENERAL,
            attributes = mapOf("version" to "2.0", "modo" to "autónomo offline"),
            notes = "Asistente inteligente con control total de hardware, voz y automatizaciones."
        )
        entities[ownerEntity.id] = ownerEntity
        entities[assistantEntity.id] = assistantEntity
        relations.add(KnowledgeRelation("entity_owner", "CONFIGURA", "entity_hendrix", "control"))
    }

    override suspend fun saveEntity(entity: KnowledgeEntity) {
        entities[entity.id] = entity
    }

    override suspend fun linkEntities(relation: KnowledgeRelation) {
        relations.add(relation)
    }

    override suspend fun searchRelevantKnowledge(
        query: String,
        topK: Int
    ): List<KnowledgeSearchResult> {
        val queryVec = computeEmbedding(query)
        val results = mutableListOf<KnowledgeSearchResult>()

        for (entity in entities.values) {
            val entityText = "${entity.name} ${entity.notes} ${entity.attributes.values.joinToString(" ")}"
            val entityVec = computeEmbedding(entityText)
            val score = queryVec.cosineSimilarity(entityVec)

            val matchedKeywords = query.lowercase().split(" ")
                .filter { it.length > 2 && entityText.lowercase().contains(it) }

            if (score > 0.15f || matchedKeywords.isNotEmpty()) {
                val boostedScore = (score * 0.7f + (matchedKeywords.size * 0.15f)).coerceIn(0f, 1f)
                results.add(
                    KnowledgeSearchResult(
                        entity = entity,
                        textSnippet = entity.notes.ifBlank { "${entity.name} (${entity.type})" },
                        similarityScore = boostedScore,
                        matchedKeywords = matchedKeywords
                    )
                )
            }
        }

        return results.sortedByDescending { it.similarityScore }.take(topK)
    }

    override suspend fun getAllEntities(): List<KnowledgeEntity> {
        return entities.values.toList()
    }

    override suspend fun getAllRelations(): List<KnowledgeRelation> {
        return relations.toList()
    }

    override suspend fun clearAll() {
        entities.clear()
        relations.clear()
    }

    private fun computeEmbedding(text: String): VectorEmbedding {
        val dim = 32
        val values = FloatArray(dim)
        val normalized = text.lowercase().trim()
        if (normalized.isEmpty()) return VectorEmbedding(values)

        // Hashing trigram projection into 32 dimensions
        for (i in 0 until normalized.length - 2) {
            val tri = normalized.substring(i, i + 3)
            val bucket = (tri.hashCode() and 0x7FFFFFFF) % dim
            values[bucket] += 1.0f
        }

        // L2 Normalization
        var sumSquares = 0f
        for (v in values) sumSquares += v * v
        val norm = sqrt(sumSquares)
        if (norm > 0f) {
            for (i in values.indices) values[i] /= norm
        }

        return VectorEmbedding(values)
    }
}
