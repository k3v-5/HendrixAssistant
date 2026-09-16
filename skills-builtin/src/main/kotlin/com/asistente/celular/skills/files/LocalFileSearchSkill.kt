package com.asistente.celular.skills.files

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.files.FileCategory
import com.asistente.celular.nlu.files.FileSearchEngine
import com.asistente.celular.nlu.files.FileSearchQuery
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.LocalFileResultsUiPayload

/**
 * Habilidad de búsqueda semántica de archivos locales en el almacenamiento del dispositivo.
 * Encuentra documentos, PDFs, fotos y descargas por lenguaje natural sin salir a la nube.
 */
class LocalFileSearchSkill(
    private val searchEngine: FileSearchEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "local_file_search_skill",
        name = "Búsqueda de Archivos Locales",
        description = "Busca PDFs, documentos, imágenes y descargas locales usando consultas semánticas y filtros de tiempo."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("busca", "encuentra", "donde", "halla"),
            OptionalConstruct(WordConstruct("el", "la", "los", "las", "un", "una", "esta")),
            WordConstruct("archivo", "documento", "pdf", "foto", "imagen", "descarga", "recibo", "factura", "video")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("busca") || lower.contains("encuentra") || lower.contains("donde esta") || lower.contains("halla")) {
            val hasTarget = lower.contains("archivo") || lower.contains("documento") || lower.contains("pdf") ||
                    lower.contains("recibo") || lower.contains("factura") || lower.contains("descarga") ||
                    lower.contains("foto") || lower.contains("comprobante")

            if (hasTarget) {
                val category = when {
                    lower.contains("pdf") || lower.contains("documento") || lower.contains("recibo") || lower.contains("factura") -> FileCategory.DOCUMENT
                    lower.contains("foto") || lower.contains("imagen") -> FileCategory.IMAGE
                    lower.contains("video") -> FileCategory.VIDEO
                    lower.contains("audio") || lower.contains("cancion") -> FileCategory.AUDIO
                    lower.contains("descarga") -> FileCategory.DOWNLOAD
                    else -> null
                }

                // Extraer término de búsqueda eliminando prefijos comunes
                val cleaned = lower
                    .replace(Regex("^(busca|encuentra|donde esta|halla)\\s+(el|la|los|las|un|una)?\\s*(archivo|documento|pdf|recibo|factura|foto|imagen|descarga)?\\s*(de|con|llamado)?\\s*"), "")
                    .trim()

                return SkillScore(
                    confidence = 0.94f,
                    specificity = Specificity.HIGH,
                    capturedSlots = buildMap {
                        put("query", cleaned.ifBlank { "reciente" })
                        if (category != null) put("category", category.name)
                    }
                )
            }
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val engine = searchEngine
        if (engine == null || !engine.isAvailable()) {
            val msg = "El motor de búsqueda de archivos locales no está disponible o requiere permisos de almacenamiento."
            return SkillOutput(speech = msg, displayText = "📁 $msg", success = false)
        }

        val queryText = score.capturedSlots["query"] ?: "documento"
        val categoryStr = score.capturedSlots["category"]
        val category = categoryStr?.let { runCatching { FileCategory.valueOf(it) }.getOrNull() }

        val results = engine.searchFiles(
            FileSearchQuery(
                query = queryText,
                category = category,
                limit = 6
            )
        )

        if (results.isEmpty()) {
            val msg = "No encontré archivos que coincidan con \"$queryText\"."
            return SkillOutput(speech = msg, displayText = "📁 $msg", success = true)
        }

        val topFiles = results.take(4)
        val fileListText = topFiles.joinToString("\n• ") { file ->
            val sizeKb = (file.sizeBytes / 1024).coerceAtLeast(1)
            "${file.name} (${sizeKb} KB)"
        }

        val speech = "Encontré ${results.size} archivos. El primero es ${topFiles.first().name}."
        val display = "📁 **Archivos Encontrados:**\n• $fileListText"

        val payload = LocalFileResultsUiPayload(
            query = queryText,
            files = results
        )

        return SkillOutput(
            speech = speech,
            displayText = display,
            success = true,
            payload = payload
        )
    }
}
