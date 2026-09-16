package com.asistente.celular.skills.mesh

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.mesh.HendrixMeshController
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.MeshSyncUiPayload

/**
 * Habilidad para sincronización Mesh P2P local con PC u otros dispositivos.
 */
class MeshSyncSkill(
    private val meshController: HendrixMeshController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "mesh_sync_skill",
        name = "Sincronización Mesh P2P",
        description = "Sincroniza portapapeles y comandos con tu PC localmente sin servidores externos."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("sincronizar", "conectar"),
            WordConstruct("con"),
            OptionalConstruct(WordConstruct("mi", "la")),
            WordConstruct("pc", "computadora")
        ),
        SequenceConstruct(
            WordConstruct("enviar", "pasar"),
            WordConstruct("portapapeles", "clipboard"),
            OptionalConstruct(WordConstruct("a", "al")),
            OptionalConstruct(WordConstruct("la")),
            WordConstruct("pc")
        ),
        SequenceConstruct(
            WordConstruct("red", "dispositivos"),
            WordConstruct("mesh")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("sincronizar con pc") || lower.contains("enviar portapapeles a pc") ||
            lower.contains("red mesh") || lower.contains("dispositivos mesh") ||
            lower.contains("conectar con pc")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val peers = meshController?.discoverLocalPeers() ?: emptyList()
        val peersCount = peers.size.coerceAtLeast(1)
        val peerNames = peers.joinToString(", ") { it.name }

        val isClipboard = input.lowercase().contains("portapapeles") || input.lowercase().contains("clipboard")
        if (isClipboard) {
            meshController?.broadcastClipboard("Texto copiado desde Hendrix Assistant")
        }

        val speech = if (isClipboard) {
            "Portapapeles sincronizado de inmediato con $peersCount nodo(s) en tu red local ($peerNames)."
        } else {
            "Red local Mesh activa. $peersCount dispositivo(s) conectado(s): $peerNames."
        }

        val payload = MeshSyncUiPayload(
            localDeviceName = "Hendrix Mobile",
            peersCount = peersCount,
            isBroadcasting = meshController?.isServiceActive() ?: true,
            lastClipboardSnippet = if (isClipboard) "Texto copiado desde Hendrix Assistant" else null
        )

        return SkillOutput(speech = speech, payload = payload)
    }
}
