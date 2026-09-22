package com.asistente.celular.skills.pc

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

/**
 * Habilidad NLU para consultar y gestionar el buzón de entregables (Dropzone) y
 * sincronización en la nube (Google Drive, OneDrive o local) para Blender, Ableton, FL Studio y Adobe.
 */
class PcDropzoneSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_dropzone_skill",
        name = "Buzón Dropzone y Sincronización en la Nube",
        description = "Consulta y gestiona la carpeta compartida en la nube donde se guardan los renders de Blender, canciones y entregables."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("donde", "cual", "ver", "carpeta", "buzon", "ultimos"),
            OptionalConstruct(WordConstruct("se", "es", "la", "de", "los")),
            OptionalConstruct(WordConstruct("guardan", "carpeta", "directorio", "renders", "archivos")),
            OptionalConstruct(WordConstruct("los", "las", "en", "de")),
            WordConstruct("renders", "canciones", "drive", "dropzone", "entregables")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val mentionsTarget = lower.contains("dropzone") ||
                lower.contains("entregables") ||
                (lower.contains("carpeta") && (lower.contains("drive") || lower.contains("render") || lower.contains("cancion"))) ||
                (lower.contains("donde") && (lower.contains("guardan") || lower.contains("queda")) && (lower.contains("render") || lower.contains("cancion") || lower.contains("blender"))) ||
                (lower.contains("ultimos") && lower.contains("renders"))

        if (mentionsTarget) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        val info = bridge?.queryDropzoneInfo()

        if (info == null) {
            val msg = "No hay información del buzón Dropzone disponible. Verifica la conexión con la PC."
            return SkillOutput(speech = msg, displayText = msg)
        }

        val syncStatus = if (info.isCloudSynced) {
            "sincronizada en la nube con ${info.cloudProvider}"
        } else {
            "en modo de almacenamiento local"
        }

        val recentDesc = if (info.recentFiles.isNotEmpty()) {
            val top = info.recentFiles.first()
            val count = info.recentFiles.size
            " Hay $count archivos generados recientemente; el último es '${top.fileName}' (${top.category})."
        } else {
            " No hay entregables recientes aún."
        }

        val speech = "La carpeta de entregables de Hendrix Studio está en ${info.rootPath}, $syncStatus.$recentDesc"

        return SkillOutput(
            speech = speech,
            displayText = speech
        )
    }
}
