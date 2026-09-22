package com.asistente.celular.skills.pc

import com.asistente.celular.ai.client.LlmClient

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
 * Habilidad NLU para consultar la memoria de trabajo cruzada entre la PC y el móvil:
 * repositorios Git activos, estado de ramas y cambios locales, procesos creativos en ejecución
 * y diagnóstico proactivo asistido por IA de fallas de compilación en terminal.
 */
class PcWorkspaceMemorySkill(
    private val pcBridge: PcWorkspaceBridge? = null,
    private val llmClient: LlmClient? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_workspace_memory_skill",
        name = "Cross-Workspace Memory & Terminal Diagnosis",
        description = "Recuerda qué estabas editando en la PC, ramas de Git y diagnostica fallas de compilación."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("que", "en", "estado", "diagnostica", "revisa"),
            OptionalConstruct(WordConstruct("estaba", "haciendo", "editando", "de", "el", "la")),
            WordConstruct("pc", "computadora", "git", "terminal", "rama", "error", "codigo", "compilacion"),
            OptionalConstruct(WordConstruct("en", "de", "la", "mi")),
            OptionalConstruct(WordConstruct("pc", "computadora", "trabajo"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("que estaba editando") ||
            lower.contains("en que me quede") ||
            lower.contains("en que rama me quede") ||
            lower.contains("que estaba haciendo en la pc") ||
            lower.contains("estado de git") ||
            lower.contains("repositorio de git") ||
            lower.contains("que programas estan abiertos") ||
            lower.contains("que procesos creativos") ||
            lower.contains("que error dio la terminal") ||
            lower.contains("ultimo error de compilacion") ||
            lower.contains("diagnostica el error") ||
            lower.contains("diagnostico de terminal") ||
            lower.contains("por que fallo la compilacion") ||
            lower.contains("memoria de trabajo")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para consultar la memoria de trabajo del workspace.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()
        val isErrorIntent = lower.contains("error") ||
                lower.contains("diagnostica") ||
                lower.contains("diagnostico") ||
                lower.contains("fallo") ||
                lower.contains("compilacion")

        if (isErrorIntent) {
            return handleTerminalDiagnosis(bridge)
        }

        return handleWorkspaceMemory(bridge)
    }

    private suspend fun handleWorkspaceMemory(bridge: PcWorkspaceBridge): SkillOutput {
        val ctx = bridge.queryWorkspaceContext()
        if (ctx == null) {
            return SkillOutput(
                speech = "No pude obtener el contexto del espacio de trabajo desde la PC.",
                displayText = "Error de contexto"
            )
        }

        val fgInfo = if (ctx.foregroundTitle.isNotBlank()) {
            "Ventana activa: ${ctx.foregroundTitle} (${ctx.foregroundProcess})."
        } else {
            "Aplicación activa: ${ctx.foregroundProcess.ifBlank { "Escritorio" }}."
        }

        val gitInfo = if (ctx.activeGitRepos.isNotEmpty()) {
            val mainRepo = ctx.activeGitRepos.first()
            val changeStatus = if (mainRepo.hasUncommittedChanges) {
                "${mainRepo.uncommittedFilesCount} archivos sin confirmar"
            } else {
                "directorio limpio"
            }
            "En ${mainRepo.repoName} estás en la rama '${mainRepo.branch}' con $changeStatus."
        } else {
            "No se detectaron repositorios Git activos en los directorios de trabajo."
        }

        val procsInfo = if (ctx.runningCreativeProcesses.isNotEmpty()) {
            "Procesos abiertos: " + ctx.runningCreativeProcesses.take(3).joinToString(", ") { it.name }
        } else {
            ""
        }

        val speech = "$fgInfo $gitInfo $procsInfo".trim()

        val displayMarkdown = buildString {
            appendLine("### 🧠 Memoria de Trabajo del Workspace")
            appendLine("- **Foco:** `${ctx.foregroundProcess}` — ${ctx.foregroundTitle}")
            if (ctx.activeGitRepos.isNotEmpty()) {
                appendLine("- **Repositorios Git:**")
                for (repo in ctx.activeGitRepos) {
                    val statusEmoji = if (repo.hasUncommittedChanges) "⚠️" else "✅"
                    appendLine("  - $statusEmoji **${repo.repoName}** (`${repo.branch}`) — ${repo.lastCommitMessage}")
                    if (repo.hasUncommittedChanges) {
                        appendLine("    *Cambios sin commit: ${repo.uncommittedFilesCount} archivos*")
                    }
                }
            }
            if (ctx.runningCreativeProcesses.isNotEmpty()) {
                appendLine("- **Procesos Creativos:**")
                for (proc in ctx.runningCreativeProcesses) {
                    appendLine("  - 💻 **${proc.name}**: ${proc.title} (${proc.cpuPercent}% CPU, ${proc.memoryMb.toInt()} MB)")
                }
            }
        }

        return SkillOutput(
            speech = speech,
            displayText = displayMarkdown
        )
    }

    private suspend fun handleTerminalDiagnosis(bridge: PcWorkspaceBridge): SkillOutput {
        val lastError = bridge.terminalErrorAlerts.value ?: bridge.queryWorkspaceContext()?.recentTerminalErrors?.lastOrNull()

        if (lastError == null) {
            return SkillOutput(
                speech = "No hay registros de errores recientes de compilación o ejecución en la terminal de la PC.",
                displayText = "✅ Terminal limpia: Sin fallas registradas."
            )
        }

        var diagnosis = lastError.aiDiagnosisPrompt
        if (llmClient != null && diagnosis.isNotBlank()) {
            try {
                val prompt = "Eres un asistente de desarrollo sénior. Analiza brevemente este fallo de compilación o ejecución y explica en 2 oraciones la causa y solución más probable:\n" +
                        "Comando: ${lastError.command}\n" +
                        "Error: ${lastError.errorMessage}\n" +
                        "Archivo: ${lastError.failedFile ?: "desconocido"}:${lastError.failedLine ?: 0}"

                val result = llmClient.generateResponse(prompt)
                val text = result.getOrNull()
                if (!text.isNullOrBlank()) {
                    diagnosis = text.trim()
                }
            } catch (e: Exception) {
                // Mantener diagnosis por defecto
            }
        }

        val speech = "Falla detectada en ${lastError.source} al ejecutar '${lastError.command}'. " +
                (if (lastError.failedFile != null) "Archivo: ${lastError.failedFile}, línea ${lastError.failedLine ?: 0}. " else "") +
                diagnosis

        val displayMarkdown = buildString {
            appendLine("### 🚨 Diagnóstico de Terminal / Compilación")
            appendLine("- **Herramienta:** `${lastError.source.uppercase()}`")
            appendLine("- **Comando:** `${lastError.command}`")
            if (lastError.failedFile != null) {
                appendLine("- **Ubicación:** `${lastError.failedFile}:${lastError.failedLine ?: 0}`")
            }
            appendLine("- **Error Crudo:**")
            appendLine("```")
            appendLine(lastError.errorMessage.take(400))
            appendLine("```")
            appendLine("- **Diagnóstico IA:** $diagnosis")
        }

        return SkillOutput(
            speech = speech,
            displayText = displayMarkdown
        )
    }
}
