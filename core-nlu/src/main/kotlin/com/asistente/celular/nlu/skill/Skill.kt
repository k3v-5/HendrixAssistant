package com.asistente.celular.nlu.skill

import android.content.Context
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity

/**
 * Información descriptiva y permisos de una habilidad.
 */
data class SkillInfo(
    val id: String,
    val name: String,
    val description: String,
    val neededPermissions: List<String> = emptyList(),
    val isFallback: Boolean = false
)

/**
 * Plan de continuación de interacción tras responder el asistente (diálogo o terminación).
 */
sealed interface InteractionPlan {
    /** La interacción actual ha terminado satisfactoriamente. */
    data object Finish : InteractionPlan

    /** El asistente requiere una respuesta del usuario (reabrir micrófono). */
    data class ReopenMicrophone(val prompt: String? = null) : InteractionPlan

    /** Diálogo de confirmación (ej: "¿Estás seguro de llamar a Juan?"). */
    data class RequestConfirmation(val onConfirm: suspend () -> SkillOutput) : InteractionPlan
}

/**
 * Resultado producido por una habilidad para la interfaz gráfica y el sintetizador de voz.
 */
data class SkillOutput(
    val speech: String,
    val displayText: String = speech,
    val interactionPlan: InteractionPlan = InteractionPlan.Finish,
    val success: Boolean = true,
    val payload: Any? = null,
    val handledByAi: Boolean = false
)

/**
 * Contexto accesible para las habilidades durante su ejecución.
 */
interface SkillContext {
    val androidContext: Context
    val isConnectedToInternet: Boolean
    val previousOutput: SkillOutput?
}

/**
 * Interfaz base de una habilidad en el sistema (inspirado en Dicio).
 */
interface Skill {
    val info: SkillInfo
    val specificity: Specificity get() = Specificity.NORMAL

    /**
     * Evalúa qué tan bien coincide la entrada de texto con la gramática de esta habilidad.
     */
    fun score(context: SkillContext, input: String): SkillScore

    /**
     * Ejecuta la acción correspondiente y genera la respuesta hablada y visual.
     */
    suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput
}
