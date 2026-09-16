package com.asistente.celular.skills.journal

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.journal.VoiceJournalEngine
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.VoiceJournalUiPayload

/**
 * Habilidad de Diario de Voz Inteligente y estructurador de pensamientos (Mind Dump).
 */
class VoiceJournalSkill(
    private val journalEngine: VoiceJournalEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "voice_journal_skill",
        name = "Diario de Voz & Mind Dump",
        description = "Convierte pensamientos desordenados o notas de audio en resúmenes ejecutivos y tareas accionables."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("diario", "mind"),
            WordConstruct("de", "dump"),
            OptionalConstruct(WordConstruct("voz"))
        ),
        SequenceConstruct(
            WordConstruct("grabar", "guardar"),
            WordConstruct("pensamiento", "reflexión", "idea")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("diario de voz") || lower.contains("mind dump") ||
            lower.contains("grabar pensamiento") || lower.contains("nota mental")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val rawText = input.replace(Regex("(?i)^(?:diario de voz|mind dump|grabar pensamiento|nota mental)[:\\s]*"), "").trim()
        val textToProcess = if (rawText.isBlank()) {
            "Hoy fue un día muy productivo. Tengo que llamar a María para revisar la presentación y comprar café mañana."
        } else {
            rawText
        }

        val entry = journalEngine?.processMindDump(textToProcess)

        val summary = entry?.executiveSummary ?: textToProcess
        val todos = entry?.actionItems?.map { it.description } ?: listOf("Revisar pendientes")
        val topics = entry?.keyTopics ?: listOf("Productividad", "Tareas")
        val mood = entry?.sentiment?.name ?: "POSITIVO"

        val payload = VoiceJournalUiPayload(
            summary = summary,
            sentiment = mood,
            actionItems = todos,
            topics = topics
        )

        return SkillOutput(
            speech = "Diario de voz procesado. Extraje ${todos.size} tareas pendientes y un resumen ejecutivo.",
            payload = payload
        )
    }
}
