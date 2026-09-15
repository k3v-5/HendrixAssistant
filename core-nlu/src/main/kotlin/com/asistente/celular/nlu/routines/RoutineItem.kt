package com.asistente.celular.nlu.routines

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa una acción individual ejecutable dentro de una rutina.
 * Abierto a extensión para nuevos tipos de acciones (OCP).
 */
@Serializable
sealed interface RoutineAction {

    /**
     * Ejecuta una orden NLU como si hubiera sido dicha o escrita por el usuario.
     * Ej: "pon modo no molestar", "ajusta volumen al 20%", "reproduce música relajante".
     */
    @Serializable
    @SerialName("command")
    data class ExecuteCommandAction(
        val commandText: String
    ) : RoutineAction

    /**
     * Hace que el asistente hable o notifique un texto específico.
     * Ej: "Buenas noches, que descanses. Tus alarmas están listas."
     */
    @Serializable
    @SerialName("speak")
    data class SpeakAction(
        val text: String
    ) : RoutineAction

    /**
     * Pausa la ejecución de la rutina durante un tiempo determinado.
     */
    @Serializable
    @SerialName("delay")
    data class DelayAction(
        val delayMillis: Long
    ) : RoutineAction

    /**
     * Modifica un ajuste del sistema o hardware directamente.
     */
    @Serializable
    @SerialName("setting")
    data class SettingAction(
        val settingKey: String,
        val value: String
    ) : RoutineAction
}

/**
 * Entidad de dominio que define una rutina o automatización de acciones encadenadas.
 */
@Serializable
data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val triggerPhrases: List<String> = emptyList(),
    val actions: List<RoutineAction> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)
