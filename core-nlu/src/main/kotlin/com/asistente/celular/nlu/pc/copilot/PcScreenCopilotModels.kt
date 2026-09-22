package com.asistente.celular.nlu.pc.copilot

/**
 * Solicitud de análisis visual bajo demanda de la pantalla o ventana activa de la PC.
 */
data class PcScreenAnalysisRequest(
    val prompt: String,
    val cropToActiveWindow: Boolean = true,
    val contextNotes: String? = null
)

/**
 * Resultado del análisis multimodal de la pantalla de la PC.
 */
data class PcScreenAnalysisResult(
    val success: Boolean,
    val analysisMarkdown: String,
    val detectedWindow: String? = null,
    val errorSummary: String? = null,
    val suggestedActions: List<String> = emptyList(),
    val rawImageBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PcScreenAnalysisResult

        if (success != other.success) return false
        if (analysisMarkdown != other.analysisMarkdown) return false
        if (detectedWindow != other.detectedWindow) return false
        if (errorSummary != other.errorSummary) return false
        if (suggestedActions != other.suggestedActions) return false
        if (rawImageBytes != null) {
            if (other.rawImageBytes == null) return false
            if (!rawImageBytes.contentEquals(other.rawImageBytes)) return false
        } else if (other.rawImageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + analysisMarkdown.hashCode()
        result = 31 * result + (detectedWindow?.hashCode() ?: 0)
        result = 31 * result + (errorSummary?.hashCode() ?: 0)
        result = 31 * result + suggestedActions.hashCode()
        result = 31 * result + (rawImageBytes?.contentHashCode() ?: 0)
        return result
    }
}
