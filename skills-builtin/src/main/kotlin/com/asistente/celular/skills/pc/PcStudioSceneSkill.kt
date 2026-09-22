package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.scene.PcStudioSceneRegistry
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para ejecutar macros encadenadas y escenas de estudio multi-paso en la PC.
 */
class PcStudioSceneSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_studio_scene_skill",
        name = "Escenas de Estudio y Macros",
        description = "Ejecuta rutinas completas como Modo Producción Musical, Render Nocturno, Streaming o Cerrar Estudio."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("activa", "inicia", "ejecuta", "pon", "cerrar", "cierra"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("modo", "escena", "estudio"),
            OptionalConstruct(WordConstruct("de", "para")),
            OptionalConstruct(WordConstruct("produccion", "musica", "render", "nocturno", "streaming", "cierre"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("modo produccion") ||
            lower.contains("produccion musical") ||
            lower.contains("modo musica") ||
            lower.contains("render nocturno") ||
            lower.contains("modo render") ||
            lower.contains("modo streaming") ||
            lower.contains("inicia streaming") ||
            lower.contains("cerrar estudio") ||
            lower.contains("cierra el estudio") ||
            lower.contains("cerrar el estudio") ||
            lower.contains("apagar estudio") ||
            lower.contains("escena de estudio")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para ejecutar escenas de estudio.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()

        val sceneId = when {
            lower.contains("render") || lower.contains("nocturno") ->
                PcStudioSceneRegistry.SCENE_RENDER_NIGHT
            lower.contains("stream") ->
                PcStudioSceneRegistry.SCENE_STREAMING
            lower.contains("cerrar") || lower.contains("cierra") || lower.contains("apaga") ->
                PcStudioSceneRegistry.SCENE_CLOSE
            else ->
                PcStudioSceneRegistry.SCENE_MUSIC_PRODUCTION
        }

        val scene = PcStudioSceneRegistry.findById(sceneId)
        val sceneName = scene?.name ?: "Escena de estudio"

        val result = bridge.executeStudioScene(sceneId)

        return if (result.success) {
            SkillOutput(
                speech = "Se ha activado ${result.sceneId.replace('_', ' ').lowercase()}. ${result.message}",
                displayText = "🎬 $sceneName ejecutada con éxito"
            )
        } else {
            SkillOutput(
                speech = "Ocurrió un problema al ejecutar la escena: ${result.message}",
                displayText = "⚠️ Error en $sceneName"
            )
        }
    }
}
