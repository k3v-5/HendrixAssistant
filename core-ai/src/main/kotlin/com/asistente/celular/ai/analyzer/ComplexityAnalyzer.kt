package com.asistente.celular.ai.analyzer

import java.text.Normalizer
import java.util.Locale

/**
 * Nivel de complejidad evaluado para una petición.
 */
enum class ComplexityLevel {
    DEVICE_ACTION,       // Tarea directa (alarma, linterna, abrir app)
    POTENTIAL_AI,        // Consulta no estándar pero corta
    COMPLEX_AI_REQUIRED  // Pregunta abierta, explicativa, creativa o de conocimiento general
}

/**
 * Analizador lingüístico para determinar si una frase requiere razonamiento semántico avanzado (IA).
 */
object ComplexityAnalyzer {

    private val COMPLEX_STARTERS = listOf(
        "por que", "porque", "como funciona", "que es", "que significa",
        "explicame", "explica", "cuentame", "quien fue", "quien es",
        "redacta", "escribe un", "escribe una", "resume", "compara",
        "dame ideas", "ideas para", "como puedo", "recomiendame",
        "cual es la diferencia", "calcula paso a paso", "ayudame a pensar"
    )

    private val GENERAL_KNOWLEDGE_TRIGGERS = listOf(
        "historia de", "capital de", "teoria de", "filosofia", "programacion",
        "receta de", "sintomas de", "definicion de", "poema", "ensayo"
    )

    /**
     * Analiza el texto y retorna el nivel de complejidad inferido.
     */
    fun analyze(input: String): ComplexityLevel {
        val normalized = normalize(input)

        // Comprobación de prefijos de razonamiento/conocimiento
        for (starter in COMPLEX_STARTERS) {
            if (normalized.startsWith(starter) || normalized.contains(" $starter")) {
                return ComplexityLevel.COMPLEX_AI_REQUIRED
            }
        }

        for (trigger in GENERAL_KNOWLEDGE_TRIGGERS) {
            if (normalized.contains(trigger)) {
                return ComplexityLevel.COMPLEX_AI_REQUIRED
            }
        }

        // Si la frase es larga (> 8 palabras) y tiene estructura interrogativa
        val words = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size >= 8 && (normalized.contains("que") || normalized.contains("como") || normalized.contains("cual") || normalized.contains("donde"))) {
            return ComplexityLevel.COMPLEX_AI_REQUIRED
        }

        return ComplexityLevel.POTENTIAL_AI
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .trim()
    }
}
