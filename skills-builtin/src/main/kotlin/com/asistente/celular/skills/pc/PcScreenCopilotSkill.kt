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
import com.asistente.celular.nlu.ui.ScreenCopilotGuideUiPayload

/**
 * Habilidad NLU para solicitar diagnóstico visual bajo demanda y asistencia interactiva paso a paso
 * (Action Guidance) de la pantalla o ventana activa de la PC utilizando visión multimodal por IA.
 */
class PcScreenCopilotSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_screen_copilot_skill",
        name = "AI Screen Copilot",
        description = "Analiza visualmente la pantalla de la PC bajo demanda, guiándote sobre qué botón o menú presionar y diagnosticando errores."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("mira", "observa", "analiza", "analizar", "diagnostica", "diagnosticar", "revisa", "revisar", "que", "como", "dime", "donde"),
            OptionalConstruct(WordConstruct("lo", "la", "el", "mi", "hay", "ves", "dice", "esta", "en", "que")),
            WordConstruct("pantalla", "monitor", "display", "copilot", "error", "terminal", "render", "boton", "botón", "click", "clic"),
            OptionalConstruct(WordConstruct("de", "en", "la", "para", "debo", "presionar", "hacer")),
            OptionalConstruct(WordConstruct("pc", "computadora", "ordenador", "pantalla", "monitor", "click", "clic"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        // Coincidencias específicas para guía de acción interactiva
        if (lower.contains("mira lo que tengo en pantalla") ||
            lower.contains("mira mi pantalla") ||
            lower.contains("mira la pantalla") ||
            lower.contains("que boton debo presionar") ||
            lower.contains("que boton presionar") ||
            lower.contains("que boton presiono") ||
            lower.contains("que boton apretar") ||
            lower.contains("donde hago click") ||
            lower.contains("donde dar click") ||
            lower.contains("donde hago clic") ||
            lower.contains("donde presiono") ||
            lower.contains("guiame en la pantalla") ||
            lower.contains("ayudame con lo que tengo en pantalla")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        // Coincidencias de diagnóstico y análisis general
        if (lower.contains("analiza la pantalla") ||
            lower.contains("analizar pantalla") ||
            lower.contains("analiza mi pantalla") ||
            lower.contains("que hay en la pantalla") ||
            lower.contains("que ves en la pantalla") ||
            lower.contains("que ves en mi pantalla") ||
            lower.contains("diagnostica la pantalla") ||
            lower.contains("diagnostico visual") ||
            lower.contains("copilot de pantalla") ||
            lower.contains("screen copilot") ||
            lower.contains("que error hay en la pantalla") ||
            lower.contains("revisa la pantalla de la pc") ||
            lower.contains("revisa mi pantalla") ||
            lower.contains("que dice el error en la pc") ||
            lower.contains("que dice la terminal de la pc") ||
            lower.contains("analiza el monitor") ||
            lower.contains("diagnostica el error en la pc")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para capturar y analizar la pantalla.",
                displayText = "PC Desconectada"
            )
        }

        val isGuidance = isActionGuidanceQuery(input)
        val prompt = extractCopilotPrompt(input, isGuidance)
        val result = bridge.analyzeScreenWithAi(prompt = prompt, cropToActiveWindow = true)

        if (!result.success) {
            return SkillOutput(
                speech = "Hubo un inconveniente al revisar la pantalla: ${result.errorSummary ?: result.analysisMarkdown}",
                displayText = "⚠️ Diagnóstico fallido: ${result.errorSummary ?: "Error desconocido"}"
            )
        }

        val windowInfo = result.detectedWindow?.let { " ($it)" } ?: ""
        val targetGoal = extractTargetGoal(input)

        // Parsear pasos y detalles estructurados para UI interactiva
        val steps = extractGuidanceSteps(result)
        val shortcut = extractKeyboardShortcut(result.analysisMarkdown)
        val targetArea = extractTargetArea(result.analysisMarkdown)

        val payload = ScreenCopilotGuideUiPayload(
            activeWindow = result.detectedWindow,
            guidanceTitle = if (targetGoal.isNotBlank()) "Guía: $targetGoal" else "Copilot Visual$windowInfo",
            guidanceSteps = steps,
            targetAreaDescription = targetArea,
            recommendedShortcut = shortcut,
            screenshotBytes = result.rawImageBytes
        )

        val speech = formatGuidanceSpeech(targetGoal, steps, shortcut, result.analysisMarkdown)

        return SkillOutput(
            speech = speech,
            displayText = "🤖 Screen Copilot$windowInfo:\n\n${result.analysisMarkdown}",
            payload = payload
        )
    }

    private fun isActionGuidanceQuery(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("boton") ||
                lower.contains("botón") ||
                lower.contains("click") ||
                lower.contains("clic") ||
                lower.contains("presionar") ||
                lower.contains("apretar") ||
                lower.contains("donde") ||
                lower.contains("dónde") ||
                lower.contains("guiame") ||
                lower.contains("guíame") ||
                lower.contains("como hago") ||
                lower.contains("cómo hago")
    }

    private fun extractTargetGoal(input: String): String {
        val lower = input.lowercase().trim()
        val regex = Regex("""(?i)(?:para|debo presionar para|hacer para|como|cómo)\s+(.+)$""")
        val match = regex.find(lower)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractCopilotPrompt(input: String, isGuidance: Boolean): String {
        val lower = input.lowercase().trim()
        if (isGuidance) {
            return "El usuario pregunta: '$input'. Analiza visualmente la pantalla activa y guía al usuario paso a paso con máxima precisión: indica qué botón, pestaña o menú exacto debe presionar, su posición en la ventana y atajo de teclado si existe."
        }

        val isGeneric = lower == "analiza la pantalla" ||
                lower == "analizar pantalla" ||
                lower == "analiza mi pantalla" ||
                lower == "que hay en la pantalla" ||
                lower == "que ves en la pantalla" ||
                lower == "que ves en mi pantalla" ||
                lower == "diagnostica la pantalla" ||
                lower == "screen copilot" ||
                lower == "copilot de pantalla"

        return if (isGeneric) {
            "Diagnostica la pantalla actual, identifica errores, procesos o ventanas activas, advertencias y sugiere soluciones o pasos a seguir."
        } else {
            input
        }
    }

    private fun extractGuidanceSteps(result: com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult): List<String> {
        if (result.suggestedActions.isNotEmpty()) {
            return result.suggestedActions
        }

        val lines = result.analysisMarkdown.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val stepLines = lines.filter { line ->
            line.matches(Regex("""^(?:\d+[\.\)]|[-*•])\s+.*"""))
        }.map { line ->
            line.replace(Regex("""^(?:\d+[\.\)]|[-*•])\s+"""), "").trim()
        }

        return if (stepLines.isNotEmpty()) {
            stepLines.take(5)
        } else {
            lines.take(3)
        }
    }

    private fun extractKeyboardShortcut(markdown: String): String? {
        val regex = Regex("""(?i)\b(Ctrl|Alt|Shift|Win)\s*\+\s*([A-Za-z0-9]+)\b""")
        val match = regex.find(markdown)
        return match?.value?.uppercase()
    }

    private fun extractTargetArea(markdown: String): String? {
        val lower = markdown.lowercase()
        return when {
            lower.contains("barra superior") || lower.contains("menu bar") || lower.contains("menú superior") -> "Barra de menús superior"
            lower.contains("panel derecho") || lower.contains("details panel") || lower.contains("inspector") -> "Panel lateral derecho"
            lower.contains("panel izquierdo") || lower.contains("outliner") || lower.contains("jerarquia") -> "Panel lateral izquierdo"
            lower.contains("barra inferior") || lower.contains("content drawer") || lower.contains("timeline") -> "Barra inferior / cajón de contenidos"
            lower.contains("esquina superior derecha") -> "Esquina superior derecha"
            lower.contains("esquina superior izquierda") -> "Esquina superior izquierda"
            else -> null
        }
    }

    private fun formatGuidanceSpeech(
        goal: String,
        steps: List<String>,
        shortcut: String?,
        fullMarkdown: String
    ): String {
        val sb = StringBuilder()
        if (goal.isNotBlank()) {
            sb.append("Para $goal: ")
        }

        if (shortcut != null) {
            sb.append("Puedes usar el atajo $shortcut, o ")
        }

        if (steps.isNotEmpty()) {
            val firstStep = steps.first().removeSuffix(".")
            sb.append(firstStep)
            if (steps.size > 1) {
                sb.append(", luego ${steps[1].removeSuffix(".")}")
            }
            sb.append(".")
        } else {
            sb.append(formatGenericSummary(fullMarkdown))
        }

        val resultStr = sb.toString().trim()
        return if (resultStr.length > 280) {
            resultStr.take(277) + "..."
        } else {
            resultStr
        }
    }

    private fun formatGenericSummary(markdown: String): String {
        val clean = markdown
            .replace(Regex("#+\\s*"), "")
            .replace("**", "")
            .replace("*", "")
            .replace("`", "")
            .trim()

        val lines = clean.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "Diagnóstico de pantalla completado."

        val summary = lines.take(2).joinToString(" ")
        return if (summary.length > 280) {
            summary.take(277) + "..."
        } else {
            summary
        }
    }
}
