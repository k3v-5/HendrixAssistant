package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.automation.AutomatedRoutineAction
import com.asistente.celular.nlu.automation.AutomatedRoutineRepository
import com.asistente.celular.nlu.automation.AutomatedRoutineTrigger
import com.asistente.celular.nlu.automation.NaturalLanguageRoutineParser
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.AutomatedRoutineCreatedUiPayload

/**
 * Habilidad NLU para consultar, activar, desactivar, ejecutar y auto-generar al vuelo
 * rutinas de automatización "Zero-Touch" en la PC y el móvil.
 */
class PcAutomatedRoutineSkill(
    private val routineRepository: AutomatedRoutineRepository? = null,
    private val pcBridge: PcWorkspaceBridge? = null,
    private val onEnterDeskStandby: (() -> Unit)? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_automated_routine_skill",
        name = "Rutinas de Automatización Zero-Touch",
        description = "Ejecuta, auto-genera por voz, consulta o activa rutinas automatizadas encadenadas en la PC y el móvil."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("cada", "cuando", "siempre", "crea", "creame", "crear", "ejecuta", "ejecutar", "inicia", "iniciar", "corre", "correr", "activa", "activar", "desactiva", "desactivar", "rutina", "rutinas"),
            OptionalConstruct(WordConstruct("vez", "la", "las", "el", "mis", "que", "una")),
            WordConstruct("rutina", "rutinas", "automatizacion", "automatizaciones", "que", "abra", "conecte"),
            OptionalConstruct(WordConstruct("de", "en", "para"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        // Creación de rutinas en lenguaje natural ("cada vez que...", "cuando...", "crea una rutina...")
        if (lower.startsWith("cada vez que") ||
            lower.startsWith("cuando abra") ||
            lower.startsWith("cuando me conecte") ||
            lower.startsWith("crea una rutina") ||
            lower.startsWith("creame una rutina") ||
            lower.startsWith("crear rutina") ||
            lower.contains("crea una automatizacion")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        if (lower.contains("rutina") || lower.contains("rutinas") || lower.contains("automatizacion")) {
            if (lower.contains("ejecuta") || lower.contains("inicia") || lower.contains("corre") ||
                lower.contains("activa") || lower.contains("desactiva") || lower.contains("listar") ||
                lower.contains("cuales son") || lower.contains("mis rutinas") || lower.contains("que rutinas")
            ) {
                return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
            }
        }

        // Reconocimiento de frases directas de triggers de rutinas por voz
        val repo = routineRepository
        if (repo != null) {
            val matched = repo.engine.evaluateVoiceEvent(lower)
            if (matched.isNotEmpty()) {
                return SkillScore(confidence = 0.95f, specificity = Specificity.HIGH)
            }
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val repo = routineRepository
        if (repo == null) {
            return SkillOutput(
                speech = "El gestor de rutinas automatizadas no está disponible.",
                displayText = "Error de Módulo"
            )
        }

        val lower = input.lowercase().trim()
        val allRoutines = repo.routines.value.ifEmpty { repo.engine.getRoutines() }

        // Caso 0: Creación automática de rutina al vuelo
        if (lower.startsWith("cada vez que") ||
            lower.startsWith("cuando") ||
            lower.contains("crea una rutina") ||
            lower.contains("creame una rutina") ||
            lower.contains("crear rutina") ||
            lower.contains("crea una automatizacion")
        ) {
            val created = NaturalLanguageRoutineParser.parse(input)
            if (created != null) {
                repo.saveRoutine(created)
                val triggerDesc = summarizeTrigger(created.triggers.firstOrNull())
                val actionsDesc = summarizeActions(created.actions)
                val payload = AutomatedRoutineCreatedUiPayload(
                    routineId = created.id,
                    routineName = created.name,
                    triggerSummary = triggerDesc,
                    actionsSummary = actionsDesc,
                    isEnabled = true
                )
                val speech = "He creado la rutina '${created.name}'. Se ejecutará automáticamente cuando $triggerDesc."
                return SkillOutput(
                    speech = speech,
                    displayText = "⚡ ${created.iconEmoji} Rutina Creada: ${created.name}\nDisparador: $triggerDesc",
                    payload = payload
                )
            }
        }

        // Caso 1: Listar rutinas
        if (lower.contains("listar") || lower.contains("cuales") || lower.contains("que rutinas") || lower.contains("mis rutinas")) {
            if (allRoutines.isEmpty()) {
                return SkillOutput(
                    speech = "No tienes rutinas de automatización configuradas actualmente.",
                    displayText = "Sin rutinas"
                )
            }
            val names = allRoutines.joinToString(", ") { "${it.iconEmoji} ${it.name} (${if (it.isEnabled) "activa" else "pausada"})" }
            return SkillOutput(
                speech = "Tienes ${allRoutines.size} rutinas: $names.",
                displayText = "📋 Rutinas: ${allRoutines.size} disponibles"
            )
        }

        // Caso 2: Activar o desactivar rutina
        val isToggleOn = lower.contains("activa") || lower.contains("habilitar")
        val isToggleOff = lower.contains("desactiva") || lower.contains("deshabilitar") || lower.contains("apaga")

        if (isToggleOn || isToggleOff) {
            val target = allRoutines.find { lower.contains(it.name.lowercase()) || lower.contains(it.id.lowercase()) }
            if (target != null) {
                val newState = repo.toggleRoutine(target.id)
                val stateText = if (newState) "activada" else "desactivada"
                return SkillOutput(
                    speech = "Rutina '${target.name}' ha sido $stateText.",
                    displayText = "⚙️ ${target.name}: $stateText"
                )
            }
        }

        // Caso 3: Ejecución de rutina (por nombre, id o frase de voz)
        val matchedVoice = repo.engine.evaluateVoiceEvent(lower)
        val targetRoutine = matchedVoice.firstOrNull()
            ?: allRoutines.find { lower.contains(it.name.lowercase()) || lower.contains(it.id.lowercase()) }
            ?: allRoutines.firstOrNull { r ->
                val keywords = r.name.lowercase().split(" ")
                keywords.any { it.length > 3 && lower.contains(it) }
            }

        if (targetRoutine == null) {
            return SkillOutput(
                speech = "No encontré ninguna rutina que coincida con tu solicitud.",
                displayText = "Rutina no encontrada"
            )
        }

        var customSpeech = ""
        var triggeredStandby = false
        val executedCount = repo.engine.executeRoutine(targetRoutine) { action ->
            when (action) {
                is AutomatedRoutineAction.PcQuickCommandAction -> {
                    when (action.command.lowercase()) {
                        "wake_on_lan" -> pcBridge?.wakeOnLan() ?: false
                        "unlock" -> pcBridge?.unlockSession() ?: false
                        else -> pcBridge?.executeQuickCommand(action.command.lowercase()) ?: false
                    }
                }
                is AutomatedRoutineAction.PcStudioSceneAction -> {
                    pcBridge?.executeStudioScene(action.sceneId)?.success ?: false
                }
                is AutomatedRoutineAction.PcPluginAction -> {
                    pcBridge?.executeCustomPluginAction(action.pluginId, action.actionId, action.params)?.success ?: false
                }
                is AutomatedRoutineAction.SpeakTtsAction -> {
                    customSpeech = action.text
                    true
                }
                is AutomatedRoutineAction.AssistantCommandAction -> true
                is AutomatedRoutineAction.DelayAction -> true
                is AutomatedRoutineAction.EnterDeskStandbyAction -> {
                    triggeredStandby = true
                    onEnterDeskStandby?.invoke()
                    true
                }
            }
        }

        val speech = if (customSpeech.isNotBlank()) {
            customSpeech
        } else {
            "Rutina '${targetRoutine.name}' ejecutada con éxito ($executedCount acciones completadas)."
        }

        return SkillOutput(
            speech = speech,
            displayText = "⚡ ${targetRoutine.iconEmoji} ${targetRoutine.name} ejecutada",
            payload = if (triggeredStandby) "ACTION_DESK_STANDBY" else null
        )
    }

    private fun summarizeTrigger(trigger: AutomatedRoutineTrigger?): String {
        return when (trigger) {
            is AutomatedRoutineTrigger.PcEventTrigger -> {
                if (trigger.eventType.startsWith("FOREGROUND_APP:")) {
                    "se abra ${trigger.eventType.substringAfter("FOREGROUND_APP:").uppercase()} en la PC"
                } else {
                    "ocurra el evento ${trigger.eventType} en la PC"
                }
            }
            is AutomatedRoutineTrigger.WifiSsidTrigger -> {
                "te conectes al Wi-Fi '${trigger.ssid}'"
            }
            is AutomatedRoutineTrigger.GeofenceTrigger -> {
                "entres en la zona '${trigger.zoneName}'"
            }
            is AutomatedRoutineTrigger.ChargingTrigger -> {
                "conectes el cargador del móvil"
            }
            is AutomatedRoutineTrigger.ScheduleTrigger -> {
                "sean las ${trigger.timeString}"
            }
            is AutomatedRoutineTrigger.VoicePhraseTrigger -> {
                "digas '${trigger.phrases.firstOrNull()}'"
            }
            null -> "se active"
        }
    }

    private fun summarizeActions(actions: List<AutomatedRoutineAction>): List<String> {
        return actions.map { action ->
            when (action) {
                is AutomatedRoutineAction.AssistantCommandAction -> action.commandText
                is AutomatedRoutineAction.PcPluginAction -> "Ajustar ventilador al 100%"
                is AutomatedRoutineAction.PcQuickCommandAction -> "Comando PC: ${action.command}"
                is AutomatedRoutineAction.PcStudioSceneAction -> "Cargar escena '${action.sceneId}'"
                is AutomatedRoutineAction.EnterDeskStandbyAction -> "Activar Desk Standby"
                is AutomatedRoutineAction.SpeakTtsAction -> "Avisar por voz: ${action.text}"
                is AutomatedRoutineAction.DelayAction -> "Esperar ${action.delayMillis}ms"
            }
        }
    }
}
