package com.asistente.celular.nlu.skill

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity

/**
 * Habilidad basada en combinadores sintácticos (Constructs).
 * Evalúa automáticamente la lista de patrones y calcula la puntuación con sus slots asociados.
 */
abstract class StandardRecognizerSkill(
    override val info: SkillInfo,
    override val specificity: Specificity = Specificity.NORMAL
) : Skill {

    abstract val patterns: List<Construct>

    override fun score(context: SkillContext, input: String): SkillScore {
        var bestScore = SkillScore.NO_MATCH

        for (pattern in patterns) {
            val matchContext = MatchContext(input)
            val matches = pattern.match(matchContext)

            if (matches) {
                val matchedWords = matchContext.tokenIndex
                val totalWords = matchContext.tokens.size

                // Proporción de palabras consumidas
                val ratio = if (totalWords > 0) matchedWords.toFloat() / totalWords.toFloat() else 1.0f
                // Si consumió todas las palabras o la gran mayoría, confianza alta
                val confidence = if (matchContext.isAtEnd) 1.0f else (0.7f + 0.3f * ratio)

                val currentScore = SkillScore(
                    confidence = confidence,
                    matchedWords = matchedWords,
                    totalWords = totalWords,
                    specificity = this.specificity,
                    capturedSlots = matchContext.capturedSlots
                )

                if (currentScore > bestScore) {
                    bestScore = currentScore
                }
            }
        }

        return bestScore
    }
}
