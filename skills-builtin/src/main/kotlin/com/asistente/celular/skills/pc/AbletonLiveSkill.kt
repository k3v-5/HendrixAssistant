package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.daw.DawAction
import com.asistente.celular.nlu.pc.daw.DawActionRequest
import com.asistente.celular.nlu.pc.daw.DawActionResult
import com.asistente.celular.nlu.pc.daw.DawType
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.DawControlUiPayload

/**
 * Habilidad especializada en el control, automatización y gestión de proyectos
 * de Ableton Live (y DAWs compatibles) desde Hendrix Assistant.
 */
class AbletonLiveSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "ableton_live_skill",
        name = "Control de Ableton Live DAW",
        description = "Abre Ableton Live, guarda proyectos, crea nuevos sets, carga proyectos, exporta audio y controla el transporte desde tu teléfono."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Abrir o enfocar Ableton Live
        SequenceConstruct(
            WordConstruct("abre", "abrir", "inicia", "iniciar", "pon"),
            WordConstruct("ableton", "live")
        ),
        // 2. Guardar proyecto
        SequenceConstruct(
            WordConstruct("guarda", "guardar", "salva", "salvar"),
            OptionalConstruct(WordConstruct("el", "este")),
            WordConstruct("proyecto", "set", "sesion"),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("ableton"))
        ),
        // 3. Nuevo proyecto
        SequenceConstruct(
            WordConstruct("nuevo", "crear", "crea"),
            OptionalConstruct(WordConstruct("un")),
            WordConstruct("proyecto", "set"),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("ableton"))
        ),
        // 4. Exportar audio
        SequenceConstruct(
            WordConstruct("exporta", "exportar", "renderiza", "render"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("audio", "cancion", "pista", "mezcla"),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("ableton"))
        ),
        // 5. Cargar proyecto
        SequenceConstruct(
            WordConstruct("carga", "cargar", "abrir"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("proyecto", "set"),
            CapturingConstruct("project_name")
        ),
        // 6. Transporte (Play/Pausa/Record)
        SequenceConstruct(
            WordConstruct("reproduce", "play", "pausa", "deten", "para", "graba", "grabar", "loop"),
            OptionalConstruct(WordConstruct("en")),
            WordConstruct("ableton")
        ),
        // 7. Confirmación interactiva de modal de guardado
        SequenceConstruct(
            WordConstruct("si", "guardar", "descartar", "cancelar", "cancela"),
            OptionalConstruct(WordConstruct("cambios", "proyecto")),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("ableton"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        val isAbletonMentioned = lower.contains("ableton") || lower.contains("live set") || (lower.contains("live") && (lower.contains("abre") || lower.contains("audio") || lower.contains("daw")))
        if (isAbletonMentioned) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        val wasWaitingConfirmation = (context.previousOutput?.payload as? DawControlUiPayload)?.isWaitingSaveConfirmation == true
        if (wasWaitingConfirmation && (lower.contains("guardar") || lower.contains("descartar") || lower.contains("cancelar") || lower.contains("cancela") || lower.contains("no guardar"))) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)

        val action: DawAction
        var targetProject: String? = null
        var saveCurrentFirst: Boolean? = null
        var speechText: String
        var statusMessage: String

        when {
            // Confirmación modal: Guardar cambios y continuar
            lower.contains("si guardar") || lower.contains("guarda y continua") || lower.contains("guardar cambios") -> {
                action = DawAction.CONFIRM_SAVE_BEFORE_ACTION
                speechText = "Guardando cambios del proyecto en Ableton Live."
                statusMessage = "Cambios guardados"
            }
            // Confirmación modal: Descartar cambios sin guardar
            lower.contains("descartar") || lower.contains("no guardar") -> {
                action = DawAction.DISCARD_AND_CONTINUE
                speechText = "Descartando cambios sin guardar en Ableton Live."
                statusMessage = "Cambios descartados"
            }
            // Confirmación modal: Cancelar acción
            lower.contains("cancelar") || lower.contains("cancela") -> {
                action = DawAction.CANCEL_ACTION
                speechText = "Operación cancelada. Se mantiene tu proyecto actual en Ableton Live."
                statusMessage = "Operación cancelada"
            }
            // Nuevo proyecto (prioritario sobre guardar cuando se especifica 'guardando el actual' o 'sin guardar')
            lower.contains("nuevo") || lower.contains("nueva") || lower.contains("crear") -> {
                action = DawAction.NEW_PROJECT
                if (lower.contains("guardando") || lower.contains("guarda el actual")) {
                    saveCurrentFirst = true
                    speechText = "Guardando proyecto actual y creando nuevo Live Set."
                    statusMessage = "Nuevo Live Set guardando actual (Ctrl+S -> Ctrl+N)"
                } else if (lower.contains("sin guardar") || lower.contains("descartando")) {
                    saveCurrentFirst = false
                    speechText = "Creando nuevo Live Set sin guardar cambios."
                    statusMessage = "Nuevo Live Set sin guardar (Ctrl+N)"
                } else {
                    saveCurrentFirst = null
                    speechText = "Nuevo Live Set creado en Ableton Live."
                    statusMessage = "Nuevo Live Set (Ctrl+N)"
                }
            }
            lower.contains("carga") || lower.contains("cargar") || lower.contains("abrir proyecto") -> {
                action = DawAction.OPEN_PROJECT
                targetProject = extractProjectName(input)
                speechText = if (targetProject != null) {
                    "Cargando el proyecto $targetProject en Ableton Live."
                } else {
                    "Abriendo selector de proyectos en Ableton Live."
                }
                statusMessage = "Abrir proyecto (Ctrl+O)"
            }
            lower.contains("guardar como") || lower.contains("guarda como") -> {
                action = DawAction.SAVE_PROJECT_AS
                speechText = "Abriendo diálogo para guardar proyecto como en Ableton Live."
                statusMessage = "Guardar como activado"
            }
            lower.contains("guarda") || lower.contains("guardar") || lower.contains("salva") -> {
                action = DawAction.SAVE_PROJECT
                speechText = "Proyecto de Ableton Live guardado correctamente."
                statusMessage = "Proyecto guardado (Ctrl+S)"
            }
            lower.contains("exporta") || lower.contains("exportar") || lower.contains("render") -> {
                action = DawAction.EXPORT_AUDIO
                speechText = "Iniciando exportación de audio en Ableton Live."
                statusMessage = "Exportar Audio/Video (Ctrl+Shift+R)"
            }
            lower.contains("graba") || lower.contains("grabar") || lower.contains("record") -> {
                action = DawAction.RECORD
                speechText = "Grabación activada en Ableton Live."
                statusMessage = "Grabación en curso (F9)"
            }
            lower.contains("loop") || lower.contains("bucle") -> {
                action = DawAction.TOGGLE_LOOP
                speechText = "Bucle de arreglo conmutado en Ableton Live."
                statusMessage = "Bucle conmutado (Ctrl+L)"
            }
            lower.contains("play") || lower.contains("reproduce") || lower.contains("pausa") || lower.contains("deten") -> {
                action = DawAction.PLAY_PAUSE
                speechText = "Alternando reproducción en Ableton Live."
                statusMessage = "Transporte alternado (Espacio)"
            }
            else -> {
                action = DawAction.LAUNCH_OR_FOCUS
                speechText = "Abriendo Ableton Live en tu computadora."
                statusMessage = "Ableton Live enfocado"
            }
        }

        val bridge = pcBridge
        val actionResult = if (bridge != null) {
            val req = DawActionRequest(
                dawType = DawType.ABLETON_LIVE,
                action = action,
                targetProjectNameOrPath = targetProject,
                saveCurrentFirst = saveCurrentFirst
            )
            bridge.executeDawAction(req)
        } else {
            DawActionResult(success = true)
        }

        var isWaitingSaveConfirmation = false
        var confirmationTitle: String? = null

        if (actionResult.requiresConfirmation) {
            isWaitingSaveConfirmation = true
            confirmationTitle = actionResult.confirmationTitle ?: "¿Deseas guardar los cambios del proyecto actual?"
            speechText = "Ableton Live pregunta si deseas guardar los cambios del proyecto actual antes de continuar. ¿Deseas guardarlos, descartarlos o cancelar?"
            statusMessage = "Confirmación requerida: Guardar proyecto"
        }

        val recentProjects = bridge?.queryDawProjects(DawType.ABLETON_LIVE) ?: emptyList()

        val payload = DawControlUiPayload(
            dawType = DawType.ABLETON_LIVE,
            isRunning = actionResult.success,
            activeProjectName = targetProject ?: (recentProjects.firstOrNull()?.name ?: "Proyecto sin título"),
            recentProjects = recentProjects,
            statusMessage = statusMessage,
            hostname = bridge?.telemetry?.value?.hostname ?: "PC-Workstation",
            isPlaying = action == DawAction.PLAY_PAUSE,
            isRecording = action == DawAction.RECORD,
            isWaitingSaveConfirmation = isWaitingSaveConfirmation,
            confirmationDialogTitle = confirmationTitle
        )

        return SkillOutput(
            speech = speechText,
            displayText = speechText,
            payload = payload
        )
    }

    private fun extractProjectName(query: String): String? {
        val patterns = listOf(
            Regex("""(?:carga|cargar|abrir|abre)\s+(?:el\s+)?(?:proyecto|set)\s+([a-zA-Z0-9_\-\s]+?)(?:\s+en\s+ableton|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:en\s+ableton\s+)?(?:carga|cargar|abrir|abre)\s+([a-zA-Z0-9_\-\s]+?)(?:\s+en\s+ableton|$)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(query)
            if (match != null && match.groupValues.size > 1) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() && !candidate.equals("ableton", ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return null
    }
}
