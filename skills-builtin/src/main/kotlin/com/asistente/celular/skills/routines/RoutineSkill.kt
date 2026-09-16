package com.asistente.celular.skills.routines

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.routines.RoutineAction
import com.asistente.celular.nlu.routines.RoutineItem
import com.asistente.celular.nlu.routines.RoutineRepository
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.delay

/**
 * Habilidad para la ejecución y consulta de rutinas y automatizaciones encadenadas.
 */
class RoutineSkill(
    private val routineRepository: RoutineRepository,
    private val commandExecutor: (suspend (command: String) -> String)? = null
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "routine_skill",
        name = "Rutinas y Automatización",
        description = "Ejecuta rutinas encadenadas como 'buenas noches', 'buenos días' o 'modo estudio'."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val listPatterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("dime", "cuales", "ver", "mostrar")),
            OptionalConstruct(WordConstruct("son")),
            OptionalConstruct(WordConstruct("mis", "las")),
            WordConstruct("rutinas")
        ),
        WordConstruct("rutinas")
    )

    private val commandPatterns: List<Construct> = listOf(
        SequenceConstruct(WordConstruct("buenas"), WordConstruct("noches")),
        SequenceConstruct(WordConstruct("buenos"), WordConstruct("dias")),
        SequenceConstruct(WordConstruct("modo"), CapturingConstruct("mode_name")),
        SequenceConstruct(
            WordConstruct("ejecuta", "ejecutar", "inicia", "iniciar", "activa", "activar", "pon", "ponme"),
            OptionalConstruct(WordConstruct("la", "el")),
            WordConstruct("rutina", "modo"),
            CapturingConstruct("mode_name")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        // 0. Detección de creación de rutinas en lenguaje natural ("Cuando diga 'X', haz Y...")
        if (com.asistente.celular.nlu.routines.RoutineCompiler.compile(input) != null) {
            return SkillScore(
                confidence = 0.99f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "create")
            )
        }

        // 1. Listar rutinas
        for (pattern in listPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx) && ctx.isAtEnd) {
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = mapOf("action" to "list")
                )
            }
        }

        // 2. Coincidencia con disparadores de rutinas registradas en el repositorio
        val currentRoutines = routineRepository.routines.value
        for (routine in currentRoutines) {
            if (!routine.isEnabled) continue
            for (trigger in routine.triggerPhrases) {
                val triggerNorm = MatchContext.normalize(trigger)
                if (normalized == triggerNorm || normalized.contains(triggerNorm)) {
                    return SkillScore(
                        confidence = 0.98f,
                        matchedWords = countWords(triggerNorm),
                        totalWords = countWords(normalized),
                        specificity = specificity,
                        capturedSlots = mapOf("matched_routine_id" to routine.id)
                    )
                }
            }
        }

        // 3. Patrones generales ("modo ...", "ejecuta rutina ...")
        for (pattern in commandPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                return SkillScore(
                    confidence = 0.90f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = ctx.capturedSlots
                )
            }
        }

        return SkillScore.NO_MATCH
    }

    private fun countWords(text: String): Int = text.split("\\s+".toRegex()).count { it.isNotBlank() }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        if (score.capturedSlots["action"] == "create") {
            val compiled = com.asistente.celular.nlu.routines.RoutineCompiler.compile(input)
            if (compiled != null) {
                routineRepository.saveRoutine(compiled)
                val summaries = compiled.actions.map { action ->
                    when (action) {
                        is RoutineAction.ExecuteCommandAction -> "• Ejecutar: \"${action.commandText}\""
                        is RoutineAction.SpeakAction -> "• Decir: \"${action.text}\""
                        is RoutineAction.DelayAction -> "• Esperar: ${action.delayMillis / 1000}s"
                        is RoutineAction.SettingAction -> "• Ajuste: ${action.settingKey} = ${action.value}"
                    }
                }
                val trigger = compiled.triggerPhrases.firstOrNull() ?: compiled.name
                val msg = "Rutina '${compiled.name}' creada con éxito. Se activará cuando digas '$trigger'."
                val display = """
                    ⚡ **Rutina Creada: ${compiled.name}**
                    
                    **Activación:** "$trigger"
                    **Acciones (${compiled.actions.size}):**
                    ${summaries.joinToString("\n")}
                """.trimIndent()
                val payload = com.asistente.celular.nlu.ui.RoutineUiPayload(
                    routineId = compiled.id,
                    name = compiled.name,
                    triggerPhrase = trigger,
                    actionsCount = compiled.actions.size,
                    actionSummaries = summaries,
                    isCreated = true
                )
                return SkillOutput(speech = msg, displayText = display, success = true, payload = payload)
            }
        }

        if (score.capturedSlots["action"] == "list") {
            return listRoutines()
        }

        val routineId = score.capturedSlots["matched_routine_id"]
        var routine: RoutineItem? = null

        if (routineId != null) {
            routine = routineRepository.getRoutineById(routineId)
        }

        if (routine == null) {
            val modeName = score.capturedSlots["mode_name"]
            routine = if (modeName != null) {
                routineRepository.findMatchingRoutine(modeName)
            } else {
                routineRepository.findMatchingRoutine(input)
            }
        }

        if (routine == null) {
            val msg = "No encontré una rutina que coincida con esa petición."
            return SkillOutput(speech = msg, displayText = msg, success = false)
        }

        return executeRoutine(routine)
    }

    private suspend fun executeRoutine(routine: RoutineItem): SkillOutput {
        val speechBuilder = StringBuilder()
        var hadError = false

        for (action in routine.actions) {
            when (action) {
                is RoutineAction.SpeakAction -> {
                    if (speechBuilder.isNotEmpty()) speechBuilder.append(" ")
                    speechBuilder.append(action.text)
                }
                is RoutineAction.ExecuteCommandAction -> {
                    if (commandExecutor != null) {
                        try {
                            val result = commandExecutor.invoke(action.commandText)
                            if (result.isNotBlank()) {
                                if (speechBuilder.isNotEmpty()) speechBuilder.append(" ")
                                speechBuilder.append(result)
                            }
                        } catch (_: Exception) {
                            hadError = true
                        }
                    }
                }
                is RoutineAction.DelayAction -> {
                    if (action.delayMillis > 0) {
                        delay(action.delayMillis.coerceAtMost(5000))
                    }
                }
                is RoutineAction.SettingAction -> {
                    // Acción de configuración directa
                }
            }
        }

        val finalSpeech = if (speechBuilder.isNotBlank()) {
            speechBuilder.toString()
        } else {
            "Rutina ${routine.name} ejecutada."
        }

        return SkillOutput(
            speech = finalSpeech,
            displayText = "⚡ Rutina: ${routine.name}\n$finalSpeech",
            success = !hadError
        )
    }

    private fun listRoutines(): SkillOutput {
        val list = routineRepository.routines.value.filter { it.isEnabled }
        if (list.isEmpty()) {
            val msg = "No tienes rutinas configuradas actualmente."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val speech = buildString {
            append("Tienes ${list.size} rutinas configuradas: ")
            append(list.joinToString(", ") { it.name })
            append(". Puedes activarlas diciendo su nombre.")
        }

        val display = buildString {
            appendLine("⚡ **Tus Rutinas Configuradas:**")
            for (r in list) {
                appendLine("• **${r.name}**: ${r.description}")
            }
        }

        return SkillOutput(speech = speech, displayText = display.trim(), success = true)
    }
}
