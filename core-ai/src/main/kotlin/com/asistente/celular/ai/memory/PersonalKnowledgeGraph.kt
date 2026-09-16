package com.asistente.celular.ai.memory

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa una relación semántica en el gráfico de conocimiento personal del usuario.
 * Modela relaciones como: ("mamá", "le_gusta", "chocolate amargo") o ("yo", "talla", "28").
 */
@Serializable
data class KnowledgeRelation(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val predicate: String,
    val obj: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Contrato para el gráfico de conocimiento personal (Personal Knowledge Graph - PKG).
 * Abierto a extensión (OCP) para persistencia en base de datos local SQLite o grafos.
 */
interface PersonalKnowledgeGraph {
    suspend fun addRelation(subject: String, predicate: String, obj: String): KnowledgeRelation
    suspend fun findRelationsAbout(entityOrConcept: String): List<KnowledgeRelation>
    suspend fun getAllRelations(): List<KnowledgeRelation>
    suspend fun removeRelation(id: String): Boolean
}

/**
 * Implementación en memoria / lista del gráfico de conocimiento personal.
 */
class InMemoryPersonalKnowledgeGraph : PersonalKnowledgeGraph {
    private val relations = mutableListOf<KnowledgeRelation>()

    override suspend fun addRelation(subject: String, predicate: String, obj: String): KnowledgeRelation {
        val rel = KnowledgeRelation(
            subject = subject.lowercase().trim(),
            predicate = predicate.lowercase().trim(),
            obj = obj.trim()
        )
        relations.add(rel)
        return rel
    }

    override suspend fun findRelationsAbout(entityOrConcept: String): List<KnowledgeRelation> {
        val needle = entityOrConcept.lowercase().trim()
        return relations.filter { rel ->
            rel.subject.contains(needle) || rel.predicate.contains(needle) || rel.obj.lowercase().contains(needle)
        }
    }

    override suspend fun getAllRelations(): List<KnowledgeRelation> {
        return relations.toList()
    }

    override suspend fun removeRelation(id: String): Boolean {
        return relations.removeIf { it.id == id }
    }
}

/**
 * Extractor heurístico de relaciones sujeto-predicado-objeto en español.
 */
object KnowledgeRelationExtractor {

    private val extractionPatterns = listOf(
        // "a [sujeto] le gusta [objeto]"
        Regex("""(?:a\s+)?(?:mi\s+)?([^,]+?)\s+(?:le\s+gusta|le\s+encanta|prefiere)\s+(.+)""", RegexOption.IGNORE_CASE) to "le_gusta",
        // "mi [predicado] es [objeto]"
        Regex("""mi\s+([^,]+?)\s+es\s+(.+)""", RegexOption.IGNORE_CASE) to "es",
        // "[sujeto] [predicado] [objeto]" (ej. "el doctor recetó ibuprofeno")
        Regex("""(?:el\s+|la\s+)?([^,]+?)\s+(?:dijo\s+que|recet[oó]|compr[oó])\s+(.+)""", RegexOption.IGNORE_CASE) to "dijo_o_receto"
    )

    fun extract(text: String): KnowledgeRelation? {
        val clean = text.trim()
        for ((regex, predicate) in extractionPatterns) {
            val match = regex.find(clean) ?: continue
            val subj = match.groupValues[1].trim()
            val obj = match.groupValues[2].trim()
            if (subj.isNotBlank() && obj.isNotBlank()) {
                return KnowledgeRelation(
                    subject = subj,
                    predicate = predicate,
                    obj = obj
                )
            }
        }
        return null
    }
}

/**
 * Proveedor RAG para enriquecer prompts de IA con recuerdos vectoriales y relaciones del grafo.
 */
object KnowledgeRagProvider {

    suspend fun buildRagContext(
        query: String,
        memoryRepository: SemanticMemoryRepository,
        knowledgeGraph: PersonalKnowledgeGraph
    ): String {
        val matchedMemories = memoryRepository.search(query, topK = 3, minSimilarity = 0.20f)
        val matchedRelations = knowledgeGraph.findRelationsAbout(query)

        if (matchedMemories.isEmpty() && matchedRelations.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("\n[Contexto Personal del Usuario (Memoria Viva / RAG)]:\n")

        matchedMemories.forEach { res ->
            sb.append("- Recuerdo: \"${res.entry.text}\"\n")
        }

        matchedRelations.forEach { rel ->
            sb.append("- Dato: ${rel.subject} -> ${rel.predicate} -> ${rel.obj}\n")
        }

        sb.append("Usa esta información con naturalidad si es relevante para responder al usuario.\n")
        return sb.toString()
    }
}
