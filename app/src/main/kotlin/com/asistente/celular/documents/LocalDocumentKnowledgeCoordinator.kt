package com.asistente.celular.documents

import android.content.Context
import com.asistente.celular.nlu.documents.DocumentKnowledgeEngine
import com.asistente.celular.nlu.documents.DocumentQueryResult
import com.asistente.celular.nlu.documents.DocumentSection
import com.asistente.celular.nlu.documents.ParsedDocument
import com.asistente.celular.nlu.rag.KnowledgeGraphRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinador para ingestión documental y consultas interactivas (Chat con PDFs/Archivos) offline.
 */
class LocalDocumentKnowledgeCoordinator(
    private val context: Context,
    private val knowledgeRepository: KnowledgeGraphRepository? = null
) : DocumentKnowledgeEngine {

    private val documents = ConcurrentHashMap<String, ParsedDocument>()

    init {
        // Documento de demostración pre-cargado
        val demoDoc = ParsedDocument(
            documentId = "doc_contrato_arrendamiento",
            fileName = "Contrato_Arrendamiento_2026.pdf",
            fileExtension = "pdf",
            totalPages = 6,
            sections = listOf(
                DocumentSection(1, "Cláusula 1: Objeto y Plazo", "El plazo forzoso del arrendamiento es de doce meses contados a partir del 1 de enero.", 18),
                DocumentSection(2, "Cláusula 2: Renta y Depósito", "La renta mensual será de $12,500 MXN pagaderos los primeros 5 días hábiles de cada mes.", 16),
                DocumentSection(3, "Cláusula 5: Rescisión Anticipada", "En caso de rescisión anticipada por el arrendatario, se notificará con 30 días de anticipación sin penalización adicional.", 20)
            )
        )
        documents[demoDoc.documentId] = demoDoc
    }

    override suspend fun ingestDocument(filePath: String, rawContent: String): ParsedDocument {
        val file = File(filePath)
        val name = file.name.ifBlank { "documento_${System.currentTimeMillis()}.txt" }
        val ext = file.extension.ifBlank { "txt" }

        val paragraphs = rawContent.split("\n\n").filter { it.isNotBlank() }
        val sections = paragraphs.mapIndexed { idx, p ->
            DocumentSection(
                sectionIndex = idx + 1,
                title = "Sección ${idx + 1}",
                textChunk = p.trim(),
                wordCount = p.split(" ").size
            )
        }

        val doc = ParsedDocument(
            documentId = UUID.randomUUID().toString().take(8),
            fileName = name,
            fileExtension = ext,
            totalPages = (sections.size / 3).coerceAtLeast(1),
            sections = sections
        )
        documents[doc.documentId] = doc
        return doc
    }

    override suspend fun queryDocument(documentId: String?, query: String): DocumentQueryResult {
        val doc = if (documentId != null) documents[documentId] else documents.values.firstOrNull()
        if (doc == null) {
            return DocumentQueryResult(
                answer = "No hay documentos cargados en el asistente.",
                sourceFileName = "Ninguno",
                relevantSectionTitle = "Sin sección",
                confidenceScore = 0f,
                referenceExcerpt = ""
            )
        }

        val queryWords = query.lowercase().split(" ").filter { it.length >= 3 }
        var bestSection = doc.sections.first()
        var bestMatches = 0

        for (section in doc.sections) {
            val textLower = section.textChunk.lowercase()
            val matches = queryWords.count { textLower.contains(it) }
            if (matches > bestMatches) {
                bestMatches = matches
                bestSection = section
            }
        }

        val score = if (bestMatches > 0) (0.65f + (bestMatches * 0.1f)).coerceAtMost(0.98f) else 0.5f
        return DocumentQueryResult(
            answer = "Según ${doc.fileName} en la '${bestSection.title}': ${bestSection.textChunk}",
            sourceFileName = doc.fileName,
            relevantSectionTitle = bestSection.title,
            confidenceScore = score,
            referenceExcerpt = bestSection.textChunk
        )
    }

    override fun getIngestedDocuments(): List<ParsedDocument> {
        return documents.values.toList()
    }

    override fun deleteDocument(documentId: String): Boolean {
        return documents.remove(documentId) != null
    }
}
