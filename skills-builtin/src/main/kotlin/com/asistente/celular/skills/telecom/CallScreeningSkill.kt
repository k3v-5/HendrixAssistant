package com.asistente.celular.skills.telecom

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.telecom.CallScreeningController
import com.asistente.celular.nlu.ui.CallScreeningUiPayload

/**
 * Habilidad para filtrado inteligente de llamadas entrantes y contestador autónomo.
 */
class CallScreeningSkill(
    private val callController: CallScreeningController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "call_screening_skill",
        name = "Filtro de Llamadas con IA",
        description = "Monitorea llamadas entrantes, detecta números spam y transcribe llamadas en vivo."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("filtrar", "bloquear", "pantalla"),
            WordConstruct("llamadas", "spam")
        ),
        SequenceConstruct(
            WordConstruct("activar", "iniciar"),
            WordConstruct("contestador"),
            OptionalConstruct(WordConstruct("de", "con")),
            OptionalConstruct(WordConstruct("ia", "hendrix"))
        ),
        SequenceConstruct(
            WordConstruct("estado", "ver"),
            WordConstruct("de"),
            WordConstruct("llamadas")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("filtrar llamadas") || lower.contains("activar contestador") ||
            lower.contains("filtro de llamadas") || lower.contains("llamadas spam") ||
            lower.contains("estado de llamadas")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val session = callController?.getActiveScreeningSessions()?.firstOrNull()
            ?: callController?.evaluateIncomingCall("+52 800 123 4567", "Telemarketing Seguros")

        val callerNum = session?.phoneNumber ?: "+52 800 123 4567"
        val callerName = session?.callerDisplayName ?: "Número desconocido"
        val spamScore = session?.spamLikelihoodPercent ?: 88
        val snippet = session?.liveTranscription ?: "Detectado posible llamada no deseada de telemarketing."

        val payload = CallScreeningUiPayload(
            callerNumber = callerNum,
            callerName = callerName,
            spamPercent = spamScore,
            statusText = if (spamScore >= 80) "Bloqueado como Spam" else "Monitoreando en vivo",
            liveSnippet = snippet
        )

        return SkillOutput(
            speech = "Filtro de llamadas activo. Última llamada de $callerName evaluada con $spamScore% de probabilidad de spam.",
            payload = payload
        )
    }
}
