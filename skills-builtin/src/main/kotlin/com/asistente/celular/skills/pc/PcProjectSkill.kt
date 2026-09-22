package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.project.PcProjectCategory
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para buscar e iniciar proyectos creativos (DAWs, 3D, Video, Unreal) en la PC.
 */
class PcProjectSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_project_skill",
        name = "Explorador de Proyectos Creativos",
        description = "Busca y abre proyectos de Ableton, FL Studio, Blender, Premiere y Unreal en la computadora."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("abre", "abrir", "lanza", "lanzar", "busca", "buscar"),
            OptionalConstruct(WordConstruct("el", "los")),
            WordConstruct("proyecto", "proyectos", "archivo", "sesion"),
            OptionalConstruct(WordConstruct("de", "en")),
            OptionalConstruct(WordConstruct("ableton", "fl", "blender", "premiere", "audio", "video", "3d"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("abre el proyecto") ||
            lower.contains("abrir proyecto") ||
            lower.contains("lanza el proyecto") ||
            lower.contains("lanzar proyecto") ||
            lower.contains("busca el proyecto") ||
            lower.contains("buscar proyectos") ||
            lower.contains("proyectos de ableton") ||
            lower.contains("proyectos de audio") ||
            lower.contains("proyectos de fl") ||
            lower.contains("proyectos de blender") ||
            lower.contains("proyectos de 3d") ||
            lower.contains("proyectos de video")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para consultar o abrir proyectos.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()

        // Determinar categoría
        val category = when {
            lower.contains("ableton") || lower.contains("fl") || lower.contains("audio") || lower.contains("daw") ->
                PcProjectCategory.AUDIO_DAW
            lower.contains("blender") || lower.contains("3d") || lower.contains("modelado") ->
                PcProjectCategory.THREE_D_VFX
            lower.contains("premiere") || lower.contains("after") || lower.contains("video") || lower.contains("edicion") ->
                PcProjectCategory.VIDEO_DESIGN
            lower.contains("unreal") || lower.contains("juego") || lower.contains("game") || lower.contains("codigo") ->
                PcProjectCategory.CODE_DEV
            else -> null
        }

        val isLaunchIntent = lower.contains("abre") || lower.contains("abrir") || lower.contains("lanza") || lower.contains("lanzar")

        // Extraer posible nombre específico de búsqueda
        val query = extractProjectName(lower)

        val projects = bridge.queryRemoteProjects(category, query)

        if (projects.isEmpty()) {
            return SkillOutput(
                speech = "No encontré proyectos en la PC con ese criterio de búsqueda.",
                displayText = "Sin proyectos encontrados"
            )
        }

        if (isLaunchIntent && query.isNotBlank()) {
            // Intentar abrir el proyecto más relevante
            val targetProject = projects.first()
            val launched = bridge.launchRemoteProject(targetProject.path)
            return if (launched) {
                SkillOutput(
                    speech = "Abriendo el proyecto ${targetProject.name} en tu computadora.",
                    displayText = "🚀 Abriendo ${targetProject.name}"
                )
            } else {
                SkillOutput(
                    speech = "Hubo un error al intentar abrir ${targetProject.name}.",
                    displayText = "⚠️ Error abriendo proyecto"
                )
            }
        }

        val projectNames = projects.take(3).joinToString(", ") { it.name }
        return SkillOutput(
            speech = "Encontré ${projects.size} proyectos en la PC. Los más recientes son: $projectNames.",
            displayText = "📂 ${projects.size} proyectos encontrados"
        )
    }

    private fun extractProjectName(input: String): String {
        val clean = input
            .replace("abre el proyecto", "")
            .replace("abrir proyecto", "")
            .replace("lanza el proyecto", "")
            .replace("lanzar proyecto", "")
            .replace("busca el proyecto", "")
            .replace("buscar proyectos", "")
            .replace("proyectos de ableton", "")
            .replace("proyectos de fl", "")
            .replace("proyectos de blender", "")
            .replace("proyectos de audio", "")
            .replace("proyectos de video", "")
            .replace("proyectos de 3d", "")
            .replace("de ableton", "")
            .replace("de fl", "")
            .replace("de blender", "")
            .trim()
        return clean
    }
}
