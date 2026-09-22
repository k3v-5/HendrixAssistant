package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.InteractionPlan
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.PcTaskApprovalUiPayload
import com.asistente.celular.nlu.ui.PcWorkspaceUiPayload

/**
 * Habilidad de PNL en español para controlar la computadora de forma remota,
 * interactuar con el espacio de trabajo en modo Snapshot WebP bajo demanda,
 * dictar texto y orquestar tareas autónomas (RPA/UIA).
 */
class PcControlSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_control_skill",
        name = "Control de PC y Espacio de Trabajo",
        description = "Controla tu computadora remotamente, visualiza la pantalla en modo Snapshot de bajo consumo y automatiza acciones con el asistente."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // Ver pantalla / Workspace / Control Remoto
        SequenceConstruct(
            WordConstruct("muestra", "muestrame", "ver", "abre", "conecta", "conectame", "controla", "controlar"),
            OptionalConstruct(WordConstruct("a", "al", "la")),
            WordConstruct("pantalla", "escritorio", "workspace", "pc", "computadora"),
            OptionalConstruct(WordConstruct("remota", "remoto", "de", "en")),
            OptionalConstruct(WordConstruct("mi", "la")),
            OptionalConstruct(WordConstruct("pc", "computadora"))
        ),
        // Energía: apagar, suspender, bloquear, reiniciar
        SequenceConstruct(
            WordConstruct("apaga", "apagar", "suspende", "suspender", "bloquea", "bloquear", "reinicia", "reiniciar"),
            OptionalConstruct(WordConstruct("mi", "la", "el")),
            WordConstruct("pc", "computadora", "ordenador", "laptop")
        ),
        // Volumen y medios en PC
        SequenceConstruct(
            WordConstruct("sube", "subir", "baja", "bajar", "silencia", "silenciar", "pausa", "pausar", "reanuda", "siguiente"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("volumen", "musica", "cancion", "audio"),
            OptionalConstruct(WordConstruct("de", "en")),
            OptionalConstruct(WordConstruct("mi", "la")),
            WordConstruct("pc", "computadora")
        ),
        // Silenciar directamente ("silencia la pc")
        SequenceConstruct(
            WordConstruct("silencia", "silenciar", "mutea", "mutear"),
            OptionalConstruct(WordConstruct("el", "la", "mi")),
            OptionalConstruct(WordConstruct("audio", "volumen", "sonido")),
            OptionalConstruct(WordConstruct("de", "en")),
            OptionalConstruct(WordConstruct("mi", "la")),
            WordConstruct("pc", "computadora")
        ),
        // Apertura de programas
        SequenceConstruct(
            WordConstruct("abre", "abrir", "inicia", "iniciar", "ejecuta", "ejecutar"),
            CapturingConstruct("app_name"),
            WordConstruct("en"),
            OptionalConstruct(WordConstruct("la", "mi")),
            WordConstruct("pc", "computadora")
        ),
        // Dictado / Escritura directa
        SequenceConstruct(
            WordConstruct("escribe", "escribir", "dicta", "dictar"),
            WordConstruct("en"),
            OptionalConstruct(WordConstruct("la", "mi")),
            WordConstruct("pc", "computadora"),
            CapturingConstruct("text_to_type")
        ),
        // Tarea autónoma / RPA
        SequenceConstruct(
            WordConstruct("en"),
            OptionalConstruct(WordConstruct("la", "mi")),
            WordConstruct("pc", "computadora"),
            CapturingConstruct("autonomous_goal")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("en la pc") || lower.contains("en la computadora") ||
            lower.contains("pantalla de la pc") || lower.contains("apaga la pc") ||
            lower.contains("bloquea la pc") || lower.contains("suspende la pc") ||
            lower.contains("conecta a la pc") || lower.contains("escritorio remoto") ||
            lower.contains("volumen de la pc") || lower.contains("abre en la pc") ||
            lower.contains("controlar pc") || lower.contains("controlar la pc") || lower.contains("controla la pc") ||
            lower.contains("silencia la pc") || lower.contains("silencia la computadora")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)

        // 1. Acciones de energía
        if (lower.contains("apaga") || lower.contains("apagar")) {
            return handlePowerCommand("shutdown", "¿Estás seguro de apagar la computadora?", "Apagando la computadora.")
        }
        if (lower.contains("reinicia") || lower.contains("reiniciar")) {
            return handlePowerCommand("restart", "¿Confirmas que deseas reiniciar la computadora?", "Reiniciando la computadora.")
        }
        if (lower.contains("suspende") || lower.contains("suspender")) {
            pcBridge?.executeQuickCommand("sleep")
            return createWorkspaceOutput("Poniendo la computadora en modo suspensión.", "Suspender PC ejecutado.")
        }
        if (lower.contains("bloquea") || lower.contains("bloquear")) {
            pcBridge?.executeQuickCommand("lock")
            return createWorkspaceOutput("Pantalla de la computadora bloqueada.", "PC bloqueada correctamente.")
        }

        // 2. Volumen y multimedia
        if (lower.contains("sube") || lower.contains("subir")) {
            pcBridge?.executeQuickCommand("volume_up")
            return createWorkspaceOutput("Volumen de la PC aumentado.", "Volumen +10%")
        }
        if (lower.contains("baja") || lower.contains("bajar")) {
            pcBridge?.executeQuickCommand("volume_down")
            return createWorkspaceOutput("Volumen de la PC reducido.", "Volumen -10%")
        }
        if (lower.contains("silencia") || lower.contains("silenciar")) {
            pcBridge?.executeQuickCommand("volume_mute")
            return createWorkspaceOutput("Audio de la PC silenciado.", "Mute alternado.")
        }
        if (lower.contains("pausa") || lower.contains("pausar") || lower.contains("reanuda")) {
            pcBridge?.executeQuickCommand("media_play_pause")
            return createWorkspaceOutput("Reproducción multimedia alternada en la PC.", "Play/Pause ejecutado.")
        }

        // 3. Dictado o escritura directa
        if (lower.contains("escribe en la pc") || lower.contains("dicta en la pc") ||
            lower.contains("escribe en la computadora") || lower.contains("dicta en la computadora")) {
            val text = if (input.contains("pc", ignoreCase = true)) {
                input.substringAfter("pc", "").trim()
            } else {
                input.substringAfter("computadora", "").trim()
            }
            if (text.isNotBlank()) {
                val success = pcBridge?.typeTextDirectly(text) ?: false
                val msg = if (success) "Texto escrito en el foco activo de la PC." else "No se pudo escribir en la PC (verifica la conexión)."
                return createWorkspaceOutput(msg, text)
            }
        }


        // 4. Apertura de programas ("abre Spotify en la pc")
        val appMatch = Regex("(?:abre|inicia|ejecuta)\\s+(.+?)\\s+en\\s+(?:la|mi)?\\s*(?:pc|computadora)", RegexOption.IGNORE_CASE).find(input)
        if (appMatch != null) {
            val appName = appMatch.groupValues[1].trim()
            pcBridge?.executeQuickCommand("launch:$appName")
            return createWorkspaceOutput("Abriendo $appName en tu computadora.", "Lanzando $appName...")
        }

        // 5. Automatización semi-autónoma RPA
        val autonomousMatch = Regex("en\\s+(?:la|mi)?\\s*(?:pc|computadora)\\s+(?:abre\\s+.+)", RegexOption.IGNORE_CASE).find(input)
        if (autonomousMatch != null && pcBridge != null) {
            val goal = input.replace(Regex("^(en\\s+(la|mi)?\\s*(pc|computadora))", RegexOption.IGNORE_CASE), "").trim()
            val plan = pcBridge.planAutonomousGoal(goal)
            return if (plan.requiresUserApproval) {
                SkillOutput(
                    speech = "He preparado un plan de automatización con ${plan.steps.size} pasos para la PC. ¿Deseas que lo ejecute?",
                    displayText = "Plan para: ${plan.userGoal}",
                    payload = PcTaskApprovalUiPayload(plan = plan, hostname = pcBridge.telemetry.value?.hostname ?: "PC-Host"),
                    interactionPlan = InteractionPlan.RequestConfirmation(
                        onConfirm = {
                            pcBridge.executeApprovedPlan(plan.planId)
                            SkillOutput(
                                speech = "Plan ejecutado correctamente en la PC.",
                                displayText = "Tarea '${plan.userGoal}' completada."
                            )
                        }
                    )
                )
            } else {
                pcBridge.executeApprovedPlan(plan.planId)
                createWorkspaceOutput("Ejecutando acción autónoma en la PC: $goal", "Meta: $goal")
            }
        }

        // 6. Por defecto: Conectar / Ver pantalla y abrir el Workspace
        val snapshotBytes = pcBridge?.requestSnapshot(cropToActiveWindow = false)
        val telemetry = pcBridge?.telemetry?.value
        val isConnected = pcBridge?.isConnected?.value ?: false

        val speech = if (isConnected) {
            "Conectado a ${telemetry?.hostname ?: "tu PC"}. Pantalla actualizada en modo Snapshot de bajo consumo."
        } else {
            "Abriendo el espacio de trabajo de la PC. Esperando conexión del agente de escritorio."
        }

        val payload = PcWorkspaceUiPayload(
            hostname = telemetry?.hostname ?: "PC-Host",
            isConnected = isConnected,
            currentMode = pcBridge?.currentMode?.value ?: PcOperationMode.INTERACTIVE_SNAPSHOT,
            telemetry = telemetry,
            latestSnapshotPreview = snapshotBytes,
            statusMessage = if (isConnected) "Conectado y listo" else "Esperando conexión",
            activeTransportType = telemetry?.activeTransportType ?: com.asistente.celular.nlu.pc.TransportType.LAN_DIRECT
        )

        return SkillOutput(
            speech = speech,
            displayText = speech,
            payload = payload
        )
    }

    private fun handlePowerCommand(action: String, confirmPrompt: String, successSpeech: String): SkillOutput {
        return SkillOutput(
            speech = confirmPrompt,
            displayText = confirmPrompt,
            interactionPlan = InteractionPlan.RequestConfirmation(
                onConfirm = {
                    pcBridge?.executeQuickCommand(action)
                    SkillOutput(
                        speech = successSpeech,
                        displayText = successSpeech
                    )
                }
            )
        )
    }

    private fun createWorkspaceOutput(speech: String, detail: String): SkillOutput {
        val telemetry = pcBridge?.telemetry?.value
        val isConnected = pcBridge?.isConnected?.value ?: false
        val payload = PcWorkspaceUiPayload(
            hostname = telemetry?.hostname ?: "PC-Host",
            isConnected = isConnected,
            currentMode = pcBridge?.currentMode?.value ?: PcOperationMode.INTERACTIVE_SNAPSHOT,
            telemetry = telemetry,
            statusMessage = detail,
            activeTransportType = telemetry?.activeTransportType ?: com.asistente.celular.nlu.pc.TransportType.LAN_DIRECT
        )
        return SkillOutput(
            speech = speech,
            displayText = detail,
            payload = payload
        )
    }
}
