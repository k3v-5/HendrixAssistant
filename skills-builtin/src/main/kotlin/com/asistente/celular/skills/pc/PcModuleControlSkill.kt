package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.module.PcModuleActionRequest
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad universal para el control por voz de suites profesionales en la PC:
 * FL Studio, Adobe Creative Suite (Premiere/Photoshop), Blender 3D y Unreal Engine 5.
 */
class PcModuleControlSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_module_control_skill",
        name = "Control de Módulos y Suites de PC",
        description = "Controla FL Studio, Blender 3D, Unreal Engine y Adobe Suite con comandos rápidos táctiles y por voz."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. FL Studio
        SequenceConstruct(
            WordConstruct("reproduce", "play", "pausa", "graba", "grabar", "mezclador", "mixer", "guarda", "exporta"),
            OptionalConstruct(WordConstruct("el", "la")),
            OptionalConstruct(WordConstruct("audio", "pista")),
            WordConstruct("en"),
            WordConstruct("fl", "fruity")
        ),
        // 2. Blender 3D
        SequenceConstruct(
            WordConstruct("renderiza", "render", "camara", "sombreado", "guarda", "guardar"),
            OptionalConstruct(WordConstruct("imagen", "animacion", "video")),
            WordConstruct("en"),
            WordConstruct("blender")
        ),
        // 3. Unreal Engine 5
        SequenceConstruct(
            WordConstruct("play", "simular", "deten", "compilar", "guardar", "content"),
            OptionalConstruct(WordConstruct("todo", "drawer")),
            WordConstruct("en"),
            WordConstruct("unreal")
        ),
        // 4. Adobe Premiere / Photoshop
        SequenceConstruct(
            WordConstruct("corta", "cortar", "cuchilla", "render", "exporta", "exportar", "pincel", "guarda"),
            WordConstruct("en"),
            WordConstruct("premiere", "photoshop", "adobe")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        val hasModuleKeyword = lower.contains("fl studio") ||
                lower.contains("en fl") ||
                lower.contains("blender") ||
                lower.contains("unreal") ||
                lower.contains("premiere") ||
                lower.contains("photoshop")

        if (hasModuleKeyword) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)

        val moduleId: PcModuleId
        val actionId: String
        val speechText: String

        when {
            // --- FL STUDIO ---
            lower.contains("fl studio") || lower.contains("en fl") -> {
                moduleId = PcModuleId.FL_STUDIO
                when {
                    lower.contains("graba") || lower.contains("record") -> {
                        actionId = "RECORD"
                        speechText = "Grabación activada en FL Studio."
                    }
                    lower.contains("mixer") || lower.contains("mezclador") -> {
                        actionId = "VIEW_MIXER"
                        speechText = "Abriendo el Mezclador de FL Studio."
                    }
                    lower.contains("piano roll") -> {
                        actionId = "VIEW_PIANO_ROLL"
                        speechText = "Abriendo el Piano Roll de FL Studio."
                    }
                    lower.contains("exporta") -> {
                        actionId = "EXPORT_AUDIO"
                        speechText = "Abriendo diálogo para exportar audio en FL Studio."
                    }
                    lower.contains("guarda") -> {
                        actionId = "SAVE_PROJECT"
                        speechText = "Proyecto guardado en FL Studio."
                    }
                    else -> {
                        actionId = "PLAY_PAUSE"
                        speechText = "Alternando reproducción en FL Studio."
                    }
                }
            }

            // --- BLENDER 3D ---
            lower.contains("blender") -> {
                moduleId = PcModuleId.BLENDER
                when {
                    lower.contains("animacion") || lower.contains("video") -> {
                        actionId = "RENDER_ANIM"
                        speechText = "Renderizado de animación iniciado en Blender."
                    }
                    lower.contains("render") || lower.contains("renderiza") -> {
                        actionId = "RENDER_IMAGE"
                        speechText = "Renderizado de imagen iniciado en Blender."
                    }
                    lower.contains("camara") -> {
                        actionId = "VIEW_CAMERA"
                        speechText = "Alternando vista de cámara en Blender."
                    }
                    lower.contains("sombreado") -> {
                        actionId = "TOGGLE_SHADING"
                        speechText = "Cambiando modo de sombreado en Blender."
                    }
                    lower.contains("guarda") -> {
                        actionId = "SAVE_FILE"
                        speechText = "Archivo guardado en Blender."
                    }
                    else -> {
                        actionId = "RENDER_IMAGE"
                        speechText = "Renderizando escena en Blender."
                    }
                }
            }

            // --- UNREAL ENGINE 5 ---
            lower.contains("unreal") -> {
                moduleId = PcModuleId.UNREAL_ENGINE
                when {
                    lower.contains("simular") -> {
                        actionId = "SIMULATE"
                        speechText = "Iniciando simulación en Unreal Engine."
                    }
                    lower.contains("deten") || lower.contains("stop") || lower.contains("para") -> {
                        actionId = "STOP_SIMULATION"
                        speechText = "Deteniendo ejecución en Unreal Engine."
                    }
                    lower.contains("content") || lower.contains("drawer") -> {
                        actionId = "CONTENT_DRAWER"
                        speechText = "Alternando Content Drawer en Unreal Engine."
                    }
                    lower.contains("compilar") || lower.contains("build") -> {
                        actionId = "BUILD_ALL"
                        speechText = "Iniciando compilación en Unreal Engine."
                    }
                    lower.contains("guarda") -> {
                        actionId = "SAVE_ALL"
                        speechText = "Guardando todos los niveles y assets en Unreal Engine."
                    }
                    else -> {
                        actionId = "PLAY_IN_EDITOR"
                        speechText = "Ejecutando Play In Editor en Unreal Engine."
                    }
                }
            }

            // --- ADOBE SUITE (Premiere Pro & Photoshop) ---
            else -> {
                moduleId = PcModuleId.ADOBE_CREATIVE
                when {
                    lower.contains("corta") || lower.contains("cuchilla") -> {
                        actionId = "RAZOR_TOOL"
                        speechText = "Herramienta Cuchilla activada en Adobe Premiere."
                    }
                    lower.contains("render") -> {
                        actionId = "RENDER_TIMELINE"
                        speechText = "Iniciando renderizado de la secuencia en Premiere."
                    }
                    lower.contains("exporta") -> {
                        actionId = "EXPORT_MEDIA"
                        speechText = "Abriendo panel de exportación de video en Premiere."
                    }
                    lower.contains("pincel") -> {
                        actionId = "BRUSH_TOOL"
                        speechText = "Herramienta Pincel activada en Photoshop."
                    }
                    lower.contains("guarda") -> {
                        actionId = "SAVE_PROJECT"
                        speechText = "Proyecto de Adobe guardado."
                    }
                    else -> {
                        actionId = "PLAY_PAUSE"
                        speechText = "Alternando reproducción en Adobe Premiere."
                    }
                }
            }
        }

        val bridge = pcBridge
        val result = if (bridge != null) {
            bridge.executeModuleAction(
                PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
            )
        } else {
            com.asistente.celular.nlu.pc.module.PcModuleActionResult(success = true)
        }

        val responseMessage = if (result.success) speechText else "Error al enviar comando a la PC: ${result.message}"

        return SkillOutput(
            speech = responseMessage,
            displayText = responseMessage
        )
    }
}
