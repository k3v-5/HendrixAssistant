package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.AntigravityTargetMode
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.AntigravityNavigatorUiPayload

/**
 * Habilidad de PNL en español para controlar el entorno de desarrollo Antigravity en la PC:
 * - Abrir o enfocar Antigravity.
 * - Navegar y listar proyectos o espacios de trabajo.
 * - Crear nuevos chats y enviar prompts iniciales.
 * - Continuar conversaciones previas e inyectar instrucciones directamente.
 */
class AntigravitySkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "antigravity_skill",
        name = "Control de Antigravity IDE",
        description = "Abre Antigravity, navega entre proyectos y escribe instrucciones en chats nuevos o existentes desde tu teléfono."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Abrir o enfocar Antigravity
        SequenceConstruct(
            WordConstruct("abre", "abrir", "inicia", "iniciar", "conecta", "conectar"),
            WordConstruct("antigravity")
        ),
        // 2. Listar proyectos
        SequenceConstruct(
            OptionalConstruct(WordConstruct("ver", "muestra", "muestrame", "listar", "lista")),
            WordConstruct("proyectos"),
            WordConstruct("en", "de"),
            WordConstruct("antigravity")
        ),
        // 3. Nuevo chat con prompt
        SequenceConstruct(
            WordConstruct("en"),
            WordConstruct("antigravity"),
            WordConstruct("nuevo"),
            WordConstruct("chat"),
            OptionalConstruct(WordConstruct("y", "para")),
            OptionalConstruct(WordConstruct("escribe", "escribir")),
            CapturingConstruct("prompt_text")
        ),
        // 4. Escribir directamente en Antigravity
        SequenceConstruct(
            WordConstruct("escribe", "escribir", "dicta", "dictar"),
            WordConstruct("en"),
            WordConstruct("antigravity"),
            CapturingConstruct("prompt_text")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("antigravity")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)
        val bridge = pcBridge

        // 1. Listar proyectos de Antigravity
        if (lower.contains("proyectos") && lower.contains("antigravity")) {
            val projects = bridge?.queryAntigravityProjects() ?: emptyList()
            val recentChats = if (projects.isNotEmpty()) {
                bridge?.queryAntigravityChats(projects.first().id) ?: emptyList()
            } else {
                bridge?.queryAntigravityChats(null) ?: emptyList()
            }

            val speech = if (projects.isNotEmpty()) {
                "Tienes ${projects.size} proyectos en Antigravity. El más reciente es ${projects.first().name}."
            } else {
                "He consultado tus proyectos de Antigravity. Selecciona uno en pantalla para comenzar."
            }

            val payload = AntigravityNavigatorUiPayload(
                projects = projects,
                selectedProject = projects.firstOrNull(),
                recentChats = recentChats,
                statusMessage = "Proyectos listados (${projects.size})",
                hostname = bridge?.telemetry?.value?.hostname ?: "PC-Host"
            )

            return SkillOutput(
                speech = speech,
                displayText = speech,
                payload = payload
            )
        }

        // 2. Nuevo chat en Antigravity ("en antigravity nuevo chat y escribe ...")
        if (lower.contains("nuevo chat") || lower.contains("crea un chat")) {
            val prompt = extractPromptAfter(input, listOf("escribe", "escribir", "chat"))
            val projects = bridge?.queryAntigravityProjects() ?: emptyList()
            val targetProject = findMentionedProject(lower, projects) ?: projects.firstOrNull()

            bridge?.executeAntigravityAction(
                mode = AntigravityTargetMode.NEW_CHAT,
                projectId = targetProject?.id,
                conversationId = null,
                prompt = prompt.takeIf { it.isNotBlank() }
            )

            val recentChats = bridge?.queryAntigravityChats(targetProject?.id) ?: emptyList()
            val speech = if (prompt.isNotBlank()) {
                "Iniciando nuevo chat en Antigravity y enviando: \"$prompt\""
            } else {
                "Iniciando nuevo chat en Antigravity para el proyecto ${targetProject?.name ?: "activo"}."
            }

            val payload = AntigravityNavigatorUiPayload(
                projects = projects,
                selectedProject = targetProject,
                recentChats = recentChats,
                statusMessage = "Nuevo chat iniciado",
                lastPromptSent = prompt.takeIf { it.isNotBlank() },
                hostname = bridge?.telemetry?.value?.hostname ?: "PC-Host"
            )

            return SkillOutput(
                speech = speech,
                displayText = speech,
                payload = payload
            )
        }

        // 3. Escribir o continuar chat existente en Antigravity
        if (lower.contains("escribe") || lower.contains("dicta") || lower.contains("continua")) {
            val prompt = extractPromptAfter(input, listOf("antigravity", "escribe", "chat"))
            val projects = bridge?.queryAntigravityProjects() ?: emptyList()
            val recentChats = bridge?.queryAntigravityChats(null) ?: emptyList()
            val targetChat = findMentionedChat(lower, recentChats) ?: recentChats.firstOrNull()

            if (prompt.isNotBlank()) {
                bridge?.executeAntigravityAction(
                    mode = if (targetChat != null) AntigravityTargetMode.EXISTING_CHAT else AntigravityTargetMode.NEW_CHAT,
                    projectId = targetChat?.projectId,
                    conversationId = targetChat?.conversationId,
                    prompt = prompt
                )

                val speech = "Enviando mensaje a Antigravity: \"$prompt\""
                val payload = AntigravityNavigatorUiPayload(
                    projects = projects,
                    selectedProject = projects.firstOrNull { it.id == targetChat?.projectId },
                    recentChats = recentChats,
                    selectedChat = targetChat,
                    statusMessage = "Prompt enviado a Antigravity",
                    lastPromptSent = prompt,
                    hostname = bridge?.telemetry?.value?.hostname ?: "PC-Host"
                )

                return SkillOutput(
                    speech = speech,
                    displayText = speech,
                    payload = payload
                )
            }
        }

        // 4. Acción por defecto: Abrir o enfocar Antigravity y cargar navegador visual
        bridge?.executeAntigravityAction(AntigravityTargetMode.LAUNCH_OR_FOCUS)
        val projects = bridge?.queryAntigravityProjects() ?: emptyList()
        val recentChats = bridge?.queryAntigravityChats(null) ?: emptyList()

        val speech = "Abriendo Antigravity. Aquí tienes tus proyectos y chats recientes para interactuar."
        val payload = AntigravityNavigatorUiPayload(
            projects = projects,
            selectedProject = projects.firstOrNull(),
            recentChats = recentChats,
            statusMessage = "Antigravity enfocado",
            hostname = bridge?.telemetry?.value?.hostname ?: "PC-Host"
        )

        return SkillOutput(
            speech = speech,
            displayText = speech,
            payload = payload
        )
    }

    private fun extractPromptAfter(input: String, keywords: List<String>): String {
        for (kw in keywords) {
            if (input.contains(kw, ignoreCase = true)) {
                val after = input.substringAfter(kw, "").trim()
                if (after.isNotBlank()) {
                    return after.removePrefix("en").removePrefix("que").removePrefix(":").trim()
                }
            }
        }
        return ""
    }

    private fun findMentionedProject(lowerInput: String, projects: List<com.asistente.celular.nlu.pc.AntigravityProject>): com.asistente.celular.nlu.pc.AntigravityProject? {
        return projects.firstOrNull { proj ->
            lowerInput.contains(proj.name.lowercase())
        }
    }

    private fun findMentionedChat(lowerInput: String, chats: List<com.asistente.celular.nlu.pc.AntigravityChat>): com.asistente.celular.nlu.pc.AntigravityChat? {
        return chats.firstOrNull { chat ->
            chat.title.isNotBlank() && lowerInput.contains(chat.title.lowercase())
        }
    }
}
