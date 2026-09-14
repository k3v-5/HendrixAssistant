package com.asistente.celular.nlu.model

/**
 * Representa la puntuación de coincidencia entre una entrada de texto y una habilidad (Skill).
 *
 * @property confidence Valor normalizado entre 0.0f (sin coincidencia) y 1.0f (coincidencia perfecta).
 * @property matchedWords Cantidad de palabras coincidentes.
 * @property totalWords Cantidad total de palabras esperadas en el patrón.
 * @property specificity Nivel de especificidad de la regla gramatical que coincidió.
 */
data class SkillScore(
    val confidence: Float,
    val matchedWords: Int = 0,
    val totalWords: Int = 0,
    val specificity: Specificity = Specificity.NORMAL,
    val capturedSlots: Map<String, String> = emptyMap()
) : Comparable<SkillScore> {

    val isMatch: Boolean
        get() = confidence >= MATCH_THRESHOLD

    override fun compareTo(other: SkillScore): Int {
        // Primero comparamos la confianza
        val confDiff = this.confidence.compareTo(other.confidence)
        if (confDiff != 0) return confDiff

        // Si la confianza es igual, gana la habilidad con mayor especificidad
        val specDiff = this.specificity.level.compareTo(other.specificity.level)
        if (specDiff != 0) return specDiff

        // Si empatan, gana quien haya capturado más palabras
        return this.matchedWords.compareTo(other.matchedWords)
    }

    companion object {
        const val MATCH_THRESHOLD = 0.65f
        val NO_MATCH = SkillScore(confidence = 0.0f, specificity = Specificity.FALLBACK)
        val PERFECT_MATCH = SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
    }
}

/**
 * Nivel de especificidad sintáctica de una regla o habilidad.
 * Inspirado en la arquitectura de especificidad de Dicio.
 */
enum class Specificity(val level: Int) {
    FALLBACK(0),      // Para IA u handlers de último recurso
    LOW(10),          // Reglas genéricas / de una sola palabra
    NORMAL(20),       // Reglas habituales (verbo + objeto)
    HIGH(30),         // Reglas muy específicas con múltiples slots
    EXACT(40)         // Comandos literales exactos
}
