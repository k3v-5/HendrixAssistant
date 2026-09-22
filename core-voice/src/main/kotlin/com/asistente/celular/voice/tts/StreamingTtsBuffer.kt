package com.asistente.celular.voice.tts

/**
 * Buffer acumulador de oraciones para Streaming de Tokens hacia síntesis de voz TTS.
 * 
 * Permite segmentar el flujo continuo de tokens devuelto por un LLM en oraciones completas y
 * naturales tan pronto como estén gramaticalmente listas, reduciendo la latencia de respuesta
 * de varios segundos a menos de 400 milisegundos.
 */
class StreamingTtsBuffer(
    private val minSentenceLength: Int = 18
) {
    private val buffer = StringBuilder()

    companion object {
        // Delimitadores de corte oracional
        private val SENTENCE_ENDINGS = listOf(". ", "? ", "! ", ".\n", "?\n", "!\n", "\n\n")
        private val ABBREVIATIONS = setOf("dr.", "sr.", "sra.", "lic.", "prof.", "ej.", "p.ej.", "vs.", "etc.")
    }

    /**
     * Añade un fragmento de texto (token o grupo de tokens) y extrae las oraciones completadas.
     *
     * @param chunk Fragmento de texto emitido por el LLM.
     * @return Lista de oraciones listas para ser sintetizadas por el motor TTS.
     */
    @Synchronized
    fun append(chunk: String): List<String> {
        buffer.append(chunk)
        val sentences = mutableListOf<String>()

        var searchIndex = 0
        while (searchIndex < buffer.length) {
            val delimiterIndex = findNextDelimiter(buffer, searchIndex)
            if (delimiterIndex == -1) break

            val candidate = buffer.substring(0, delimiterIndex).trim()
            val delimiterChar = buffer[delimiterIndex]

            // Verificar si es una abreviatura común (ej: Dr. o etc.)
            val lastWord = candidate.substringAfterLast(' ', "").lowercase() + delimiterChar
            if (ABBREVIATIONS.contains(lastWord)) {
                searchIndex = delimiterIndex + 1
                continue
            }

            // Verificar si es un número decimal (ej. 3.14)
            if (delimiterChar == '.' && isDecimalNumber(buffer, delimiterIndex)) {
                searchIndex = delimiterIndex + 1
                continue
            }

            // Validar longitud mínima para mantener prosodia y cadencia natural
            if (candidate.length >= minSentenceLength) {
                sentences.add(candidate)
                buffer.delete(0, delimiterIndex + 1)
                searchIndex = 0
            } else {
                searchIndex = delimiterIndex + 1
            }
        }

        return sentences
    }

    /**
     * Vacía cualquier texto restante acumulado al finalizar la generación del LLM.
     */
    @Synchronized
    fun flush(): String? {
        val remaining = buffer.toString().trim()
        buffer.clear()
        return if (remaining.isNotEmpty()) remaining else null
    }

    /**
     * Limpia completamente el acumulador.
     */
    @Synchronized
    fun clear() {
        buffer.clear()
    }

    private fun findNextDelimiter(sb: StringBuilder, startIndex: Int): Int {
        for (i in startIndex until sb.length) {
            val ch = sb[i]
            if (ch == '.' || ch == '?' || ch == '!' || ch == '\n') {
                // Verificar que haya espacio o fin de línea posterior si es puntuación
                if (ch == '\n') return i
                if (i + 1 < sb.length && (sb[i + 1] == ' ' || sb[i + 1] == '\n' || sb[i + 1] == '\t')) {
                    return i + 1
                }
            }
        }
        return -1
    }

    private fun isDecimalNumber(sb: StringBuilder, dotIndex: Int): Boolean {
        if (dotIndex > 0 && dotIndex + 1 < sb.length) {
            return sb[dotIndex - 1].isDigit() && sb[dotIndex + 1].isDigit()
        }
        return false
    }
}
