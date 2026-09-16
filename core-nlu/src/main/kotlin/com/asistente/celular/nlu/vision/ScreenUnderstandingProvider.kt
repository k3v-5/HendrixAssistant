package com.asistente.celular.nlu.vision

/**
 * Representación inmutable del contenido textual y estructural capturado de la pantalla activa.
 */
data class ScreenContentSnapshot(
    val packageName: String?,
    val title: String?,
    val texts: List<String>,
    val rawTextDump: String,
    val interactiveElementsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Contrato desacoplado para la percepción y comprensión de pantalla (Screen Understanding).
 * Sigue las directrices de desacoplamiento y extensibilidad de GEMINI.md.
 */
interface ScreenUnderstandingProvider {
    fun isAvailable(): Boolean
    suspend fun captureScreenContent(): ScreenContentSnapshot?
}
