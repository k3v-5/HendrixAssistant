package com.asistente.celular.nlu.pc.clipboard

import java.security.MessageDigest

/**
 * Payload de contenido del portapapeles universal entre el móvil y la PC.
 * Incluye recuento de caracteres y hash criptográfico SHA-256 para
 * garantizar que los textos y prompts largos se transmitan con 100% de integridad
 * sin truncamientos accidentales.
 */
data class PcClipboardPayload(
    val text: String,
    val charCount: Int = text.length,
    val checksumSha256: String = computeSha256(text),
    val timestampEpoch: Long = System.currentTimeMillis()
) {
    companion object {
        fun computeSha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }

        fun fromText(text: String): PcClipboardPayload {
            return PcClipboardPayload(
                text = text,
                charCount = text.length,
                checksumSha256 = computeSha256(text),
                timestampEpoch = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Plantilla o fragmento de texto rápido guardado para reutilización frecuente
 * (ej. prompts de render 3D, notas de producción musical, scripts).
 */
data class PcSnippetItem(
    val id: String,
    val title: String,
    val content: String,
    val iconEmoji: String = "📝"
)
