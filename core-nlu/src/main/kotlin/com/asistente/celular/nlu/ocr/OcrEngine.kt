package com.asistente.celular.nlu.ocr

/**
 * Bloque individual de texto detectado por un motor de OCR.
 */
data class OcrBlock(
    val text: String,
    val confidence: Float = 1.0f,
    val lines: List<String> = emptyList()
)

/**
 * Resultado completo del reconocimiento óptico de caracteres (OCR).
 */
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList(),
    val language: String? = null,
    val processingTimeMs: Long = 0L
)

/**
 * Contrato desacoplado para reconocimiento óptico de caracteres offline/online (OCP).
 */
interface OcrEngine {
    fun isAvailable(): Boolean
    suspend fun recognizeText(imageUriOrPath: String): OcrResult
}
