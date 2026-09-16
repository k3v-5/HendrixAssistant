package com.asistente.celular.skills.automotive

import com.asistente.celular.nlu.automotive.AutomotiveCarController
import com.asistente.celular.nlu.automotive.CarScreenTemplate
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
import com.asistente.celular.nlu.ui.AutomotiveUiPayload

/**
 * Habilidad de Integración con Android Auto y Pantalla Vehicular.
 */
class AutomotiveSkill(
    private val carController: AutomotiveCarController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "automotive_skill",
        name = "Android Auto & Tablero Vehicular",
        description = "Controla la interfaz automotriz y navegación en la pantalla del coche."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("modo", "abrir"),
            WordConstruct("android", "en"),
            WordConstruct("auto", "coche", "carro", "auto")
        ),
        SequenceConstruct(
            WordConstruct("pantalla"),
            WordConstruct("del"),
            WordConstruct("auto", "coche", "vehiculo", "vehículo")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("android auto") || lower.contains("en el coche") ||
            lower.contains("en el auto") || lower.contains("pantalla del auto") ||
            lower.contains("modo vehicular")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        carController?.switchTemplate(CarScreenTemplate.NAVIGATION)
        val state = carController?.getCurrentScreenState()

        val shortcuts = state?.shortcuts?.map { "${it.title}: ${it.destinationAddress} (~${it.estimatedMinutes} min)" }
            ?: listOf("Casa: Av. Principal 123", "Oficina: Paseo de la Reforma 500")

        val payload = AutomotiveUiPayload(
            isCarConnected = state?.isCarConnected ?: true,
            headUnitName = state?.headUnitName ?: "Consola Vehicular (Android Auto)",
            currentTemplate = state?.currentTemplate?.name ?: "NAVIGATION",
            shortcuts = shortcuts
        )

        return SkillOutput(
            speech = "Modo Android Auto proyectado en el tablero. Atajos de navegación rápidos listos.",
            payload = payload
        )
    }
}
