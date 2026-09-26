package com.asistente.celular.skills.memory

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.memory.episodic.EpisodicMemoryRepository
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.EpisodicProjectUiPayload

/**
 * Habilidad de Memoria Episódica y Contexto Persistente para espacios de trabajo creativos.
 * Recuerda el último proyecto activo en Unreal Engine, Blender, Ableton Live o FL Studio,
 * y permite reanudar o conmutar entre sesiones de trabajo sin perder el hilo.
 */
class EpisodicMemorySkill(
    private val episodicMemoryRepository: EpisodicMemoryRepository? = null,
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "episodic_memory_skill",
        name = "Memoria Episódica de Proyectos",
        description = "Recuerda en qué proyecto estabas trabajando y reanuda el contexto creativo en la PC."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("donde", "dónde", "en", "cual", "cuál", "que", "qué"),
            OptionalConstruct(WordConstruct("me", "estaba", "fue", "es", "el", "mi")),
            WordConstruct("quede", "quedé", "trabajando", "proyecto", "ultimo", "último", "sesion", "sesión"),
            OptionalConstruct(WordConstruct("en", "de", "el", "la")),
            OptionalConstruct(WordConstruct("unreal", "blender", "ableton", "fl", "studio", "pc", "proyecto"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("donde me quede") ||
            lower.contains("dónde me quedé") ||
            lower.contains("en que me quede") ||
            lower.contains("en qué me quedé") ||
            lower.contains("en que estaba trabajando") ||
            lower.contains("en qué estaba trabajando") ||
            lower.contains("cual fue mi ultimo proyecto") ||
            lower.contains("cuál fue mi último proyecto") ||
            lower.contains("cual es mi proyecto actual") ||
            lower.contains("cuál es mi proyecto actual") ||
            lower.contains("en que proyecto estaba") ||
            lower.contains("en qué proyecto estaba") ||
            lower.contains("cierra el proyecto actual y abre el anterior") ||
            lower.contains("abre el proyecto anterior")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val repo = episodicMemoryRepository
        if (repo == null) {
            return SkillOutput(
                speech = "La memoria episódica de proyectos no está disponible en este momento.",
                displayText = "⚠️ Sin memoria episódica"
            )
        }

        val lower = input.lowercase().trim()

        // Caso: "cierra el proyecto actual y abre el anterior" / "abre el proyecto anterior"
        if (lower.contains("anterior") && (lower.contains("abre") || lower.contains("cambia") || lower.contains("conmuta"))) {
            val recent = repo.getRecentSessions(limit = 2)
            if (recent.size < 2) {
                return SkillOutput(
                    speech = "No tengo registro de un proyecto anterior para cambiar en este momento.",
                    displayText = "Sin proyecto anterior"
                )
            }
            val previous = recent[1]
            val path = previous.projectPath
            if (path != null && pcBridge != null) {
                pcBridge.launchRemoteProject(path)
            }

            val payload = EpisodicProjectUiPayload(
                appName = previous.appName,
                projectName = previous.projectTitle,
                lastActiveFormatted = formatTimeAgo(previous.lastActiveEpochMillis),
                filePath = previous.projectPath
            )

            return SkillOutput(
                speech = "Cambiando al proyecto anterior: '${previous.projectTitle}' en ${previous.appName}.",
                displayText = "🔄 Reanudando ${previous.appName}: ${previous.projectTitle}",
                payload = payload
            )
        }

        // Consultar por aplicación específica (Unreal, Blender, Ableton, FL Studio)
        val targetApp = detectTargetApp(lower)
        val session = if (targetApp != null) {
            repo.getSessionForApp(targetApp)
        } else {
            repo.getLastActiveSession()
        }

        if (session == null) {
            val appMsg = if (targetApp != null) " para $targetApp" else ""
            return SkillOutput(
                speech = "No tengo registros recientes de proyectos abiertos$appMsg.",
                displayText = "Sin historial de proyectos"
            )
        }

        val timeAgo = formatTimeAgo(session.lastActiveEpochMillis)
        val speech = "Estabas trabajando en el proyecto '${session.projectTitle}' en ${session.appName}, $timeAgo."

        val payload = EpisodicProjectUiPayload(
            appName = session.appName,
            projectName = session.projectTitle,
            lastActiveFormatted = timeAgo,
            filePath = session.projectPath
        )

        return SkillOutput(
            speech = speech,
            displayText = "🧠 **${session.appName}**: ${session.projectTitle}\n*Última actividad: $timeAgo*",
            payload = payload
        )
    }

    private fun detectTargetApp(lower: String): String? {
        return when {
            lower.contains("unreal") -> "Unreal Engine"
            lower.contains("blender") -> "Blender"
            lower.contains("ableton") -> "Ableton Live"
            lower.contains("fl studio") || lower.contains("fl") -> "FL Studio"
            lower.contains("code") || lower.contains("visual studio") -> "Visual Studio Code"
            else -> null
        }
    }

    private fun formatTimeAgo(epochMillis: Long): String {
        val diffMs = System.currentTimeMillis() - epochMillis
        val diffMinutes = (diffMs / (1000 * 60)).toInt()

        return when {
            diffMinutes < 1 -> "hace unos segundos"
            diffMinutes == 1 -> "hace 1 minuto"
            diffMinutes < 60 -> "hace $diffMinutes minutos"
            else -> {
                val hours = diffMinutes / 60
                if (hours == 1) "hace 1 hora" else "hace $hours horas"
            }
        }
    }
}
