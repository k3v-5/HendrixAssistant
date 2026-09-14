package com.asistente.celular.nlu.evaluator

import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext

/**
 * Coincidencia evaluada con su puntuación.
 */
data class SkillMatch(
    val skill: Skill,
    val score: SkillScore
)

/**
 * Evalúa una frase contra todas las habilidades registradas y clasifica la mejor.
 * Si ninguna habilidad local alcanza el umbral de confianza, devuelve la habilidad de Fallback (IA).
 */
class SkillRanker(
    private val skills: List<Skill>,
    private val fallbackSkill: Skill
) {
    /**
     * Evalúa la entrada y encuentra la habilidad más idónea.
     */
    fun findBestSkill(context: SkillContext, input: String): SkillMatch {
        var bestMatch: SkillMatch? = null

        for (skill in skills) {
            val score = skill.score(context, input)
            if (score.isMatch) {
                if (bestMatch == null || score > bestMatch.score) {
                    bestMatch = SkillMatch(skill, score)
                }
            }
        }

        // Si se encontró una coincidencia local clara (>= 0.65)
        if (bestMatch != null) {
            return bestMatch
        }

        // Si ninguna regla local coincide, se enruta a la habilidad de Fallback (IA)
        val fallbackScore = fallbackSkill.score(context, input)
        return SkillMatch(fallbackSkill, fallbackScore)
    }
}
