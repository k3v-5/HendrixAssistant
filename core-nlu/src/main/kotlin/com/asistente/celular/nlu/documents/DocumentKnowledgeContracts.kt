package com.asistente.celular.nlu.documents

data class DocumentSection(
    val sectionIndex: Int,
    val title: String,
    val textChunk: String,
    val wordCount: Int
)

data class ParsedDocument(
    val documentId: String,
    val fileName: String,
    val fileExtension: String, // "pdf", "epub", "txt", "docx"
    val totalPages: Int,
    val sections: List<DocumentSection>,
    val ingestedEpoch: Long = System.currentTimeMillis()
)

data class DocumentQueryResult(
    val answer: String,
    val sourceFileName: String,
    val relevantSectionTitle: String,
    val confidenceScore: Float,
    val referenceExcerpt: String
)

/**
 * Contrato para ingestión documental y consultas de preguntas/respuestas (Chat con PDFs offline).
 */
interface DocumentKnowledgeEngine {
    suspend fun ingestDocument(filePath: String, rawContent: String): ParsedDocument
    suspend fun queryDocument(documentId: String?, query: String): DocumentQueryResult
    fun getIngestedDocuments(): List<ParsedDocument>
    fun deleteDocument(documentId: String): Boolean
}
