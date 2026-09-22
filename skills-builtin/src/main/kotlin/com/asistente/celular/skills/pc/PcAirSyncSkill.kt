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
import java.io.File

/**
 * Habilidad NLU para Hendrix AirSync: transferencia LAN P2P de archivos pesados (videos, renders, zips, stems de audio)
 * sin requerir conexión a internet ni pasar por la nube.
 */
class PcAirSyncSkill(
    private val pcBridge: PcWorkspaceBridge? = null,
    private val defaultDownloadDir: File? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_airsync_skill",
        name = "Hendrix AirSync P2P Transfer",
        description = "Transfiere archivos de gran tamaño entre PC y celular por LAN de alta velocidad sin internet."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("descarga", "transfiere", "baja", "airsync", "archivos", "estado"),
            OptionalConstruct(WordConstruct("el", "la", "los", "archivo", "por", "de")),
            WordConstruct("airsync", "archivo", "archivos", "pesado", "transferencia"),
            OptionalConstruct(WordConstruct("de", "a", "la")),
            OptionalConstruct(WordConstruct("pc", "computadora", "celular", "movil"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("airsync") ||
            lower.contains("lens") ||
            lower.contains("lens to workspace") ||
            lower.contains("foto a la pc") ||
            lower.contains("boceto a la pc") ||
            lower.contains("pegar en la pc") ||
            lower.contains("pega la foto") ||
            lower.contains("transfiere el archivo") ||
            lower.contains("descarga el archivo por airsync") ||
            lower.contains("archivos de airsync") ||
            lower.contains("transferencia p2p") ||
            lower.contains("estado de transferencia") ||
            lower.contains("pasar archivo pesado")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para gestionar transferencias Hendrix AirSync.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()

        // 0. Caso: Lens-to-Workspace (Cámara/Boceto al portapapeles de PC)
        if (lower.contains("lens") || lower.contains("foto") || lower.contains("boceto") || lower.contains("pega")) {
            val autoPaste = lower.contains("pega") || lower.contains("auto")
            return SkillOutput(
                speech = if (autoPaste) {
                    "Iniciando Lens-to-Workspace: la imagen se inyectará en el portapapeles de la PC y se pegará automáticamente con Control V."
                } else {
                    "Iniciando Lens-to-Workspace: la captura se transferirá a la PC y estará disponible en el portapapeles para Control V."
                },
                displayText = "📸 **Lens-to-Workspace Activo**\nTransferencia LAN directa al portapapeles de Windows."
            )
        }

        // 1. Caso: Consultar transferencias activas
        if (lower.contains("estado") || lower.contains("progreso")) {
            val transfers = bridge.airSyncTransfers.value
            if (transfers.isEmpty()) {
                return SkillOutput(
                    speech = "No hay transferencias de AirSync activas ni recientes.",
                    displayText = "Sin transferencias activas"
                )
            }
            val active = transfers.first()
            val percent = (active.progressPercent * 100).toInt()
            val speedMb = active.speedBytesPerSec / (1024.0 * 1024.0)
            return SkillOutput(
                speech = "Transferencia de ${active.fileName}: al $percent%, a una velocidad de ${"%.1f".format(speedMb)} MB por segundo.",
                displayText = "⚡ AirSync: ${active.fileName} ($percent% - ${"%.1f".format(speedMb)} MB/s)"
            )
        }

        // 2. Caso: Descargar un archivo disponible
        if (lower.contains("descarga") || lower.contains("transfiere") || lower.contains("baja")) {
            val sharedFiles = bridge.queryAirSyncSharedFiles()
            if (sharedFiles.isEmpty()) {
                return SkillOutput(
                    speech = "No hay archivos indexados o listos para transferir en AirSync.",
                    displayText = "Sin archivos en cola de AirSync"
                )
            }

            // Buscar coincidencia por nombre o tomar el primer archivo disponible
            val targetFile = sharedFiles.firstOrNull { file ->
                lower.contains(file.fileName.lowercase())
            } ?: sharedFiles.first()

            val downloadDir = defaultDownloadDir ?: File(System.getProperty("java.io.tmpdir") ?: ".", "AirSyncDownloads")
            downloadDir.mkdirs()
            val destination = File(downloadDir, targetFile.fileName)

            val success = bridge.downloadAirSyncFile(targetFile.fileId, destination.absolutePath)
            return if (success) {
                val sizeMb = targetFile.fileSizeBytes / (1024.0 * 1024.0)
                SkillOutput(
                    speech = "Archivo ${targetFile.fileName} transferido con éxito vía AirSync en red local (${"%.1f".format(sizeMb)} MB).",
                    displayText = "✅ Descarga completada: ${targetFile.fileName}"
                )
            } else {
                SkillOutput(
                    speech = "Hubo un error al transferir ${targetFile.fileName} por AirSync.",
                    displayText = "❌ Fallo en transferencia AirSync"
                )
            }
        }

        // 3. Caso por defecto: Listar archivos listos en la PC
        val sharedFiles = bridge.queryAirSyncSharedFiles()
        if (sharedFiles.isEmpty()) {
            return SkillOutput(
                speech = "AirSync está listo en la PC pero actualmente no hay archivos compartidos.",
                displayText = "Hendrix AirSync: Listo (0 archivos compartidos)"
            )
        }

        val speech = "Hay ${sharedFiles.size} archivos listos en AirSync: " +
                sharedFiles.take(3).joinToString(", ") { it.fileName }

        val displayMarkdown = buildString {
            appendLine("### ⚡ Hendrix AirSync — Archivos Listos")
            for (f in sharedFiles) {
                val sizeMb = f.fileSizeBytes / (1024.0 * 1024.0)
                appendLine("- 📄 **${f.fileName}** (${"%.2f".format(sizeMb)} MB, ${f.totalChunks} bloques)")
            }
        }

        return SkillOutput(
            speech = speech,
            displayText = displayMarkdown
        )
    }
}
