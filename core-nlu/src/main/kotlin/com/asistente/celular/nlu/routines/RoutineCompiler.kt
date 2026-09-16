package com.asistente.celular.nlu.routines

import java.util.Locale

/**
 * Compilador de alto nivel para interpretar y transformar solicitudes en lenguaje natural
 * en grafos secuenciales de acciones estructuradas ([RoutineItem]).
 *
 * Soporta patrones como:
 * - "Cuando diga 'modo noche', apaga el foco, pon el volumen al 20% y dime que duerma bien"
 * - "Crea una rutina llamada 'modo estudio' que prenda la luz, ponga el celular en silencio y reproduzca lofi"
 * - "Si digo 'a trabajar', sube el brillo, pon el foco blanco frio y abre slack"
 */
object RoutineCompiler {

    private val triggerPatterns = listOf(
        Regex("""(?:cuando|si)\s+(?:yo\s+)?(?:diga|digo)\s+["']?([^"',]+?)["']?(?:,\s*|\s*:\s*|\s+que\s+)(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:al\s+decir)\s+["']?([^"',]+?)["']?(?:,\s*|\s*:\s*|\s+que\s+)(.+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:crea|crear|haz|hazme|nueva)\s+(?:una\s+)?rutina\s+(?:llamada|para|de)?\s+["']?([^"',]+?)["']?(?:,\s*|\s*:\s*|\s+que\s+)(.+)""", RegexOption.IGNORE_CASE)
    )

    /**
     * Compila un texto de solicitud en lenguaje natural en un [RoutineItem] estructurado.
     * Retorna null si la frase no coincide con una intención clara de creación de rutina.
     */
    fun compile(rawInput: String): RoutineItem? {
        val trimmed = rawInput.trim()
        for (pattern in triggerPatterns) {
            val match = pattern.find(trimmed) ?: continue
            val triggerRaw = match.groupValues[1].trim().trim('"', '\'')
            val actionsRaw = match.groupValues[2].trim()

            if (triggerRaw.isBlank() || actionsRaw.isBlank()) continue

            val actions = parseActions(actionsRaw)
            if (actions.isEmpty()) continue

            val routineName = triggerRaw.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }

            return RoutineItem(
                name = routineName,
                description = "Rutina activada por voz: '$triggerRaw'",
                triggerPhrases = listOf(triggerRaw.lowercase(), "modo ${triggerRaw.lowercase()}"),
                actions = actions
            )
        }
        return null
    }

    /**
     * Descompone una cadena de acciones concatenadas por comas o conjunciones en una lista de [RoutineAction].
     */
    fun parseActions(actionsBlock: String): List<RoutineAction> {
        val delimiters = Regex(""",\s*|\s+y\s+|\s+luego\s+|\s+despu[eé]s\s+""", RegexOption.IGNORE_CASE)
        val rawClauses = actionsBlock.split(delimiters)
        val result = mutableListOf<RoutineAction>()

        for (clause in rawClauses) {
            val clean = clause.trim().trim('.', ';')
            if (clean.isBlank()) continue

            val lower = clean.lowercase()

            when {
                // 1. Hablar / Decir algo
                lower.startsWith("dime ") || lower.startsWith("di ") || lower.startsWith("notifica ") -> {
                    val speechText = clean.substringAfter(" ").trim().trim('"', '\'')
                    if (speechText.isNotBlank()) {
                        result.add(RoutineAction.SpeakAction(speechText))
                    }
                }
                // 2. Esperar / Pausa
                lower.startsWith("espera ") || lower.startsWith("pausa de ") -> {
                    val seconds = Regex("""(\d+)""").find(lower)?.groupValues?.get(1)?.toLongOrNull() ?: 3L
                    result.add(RoutineAction.DelayAction(seconds * 1000L))
                }
                // 3. Comandos estándar del asistente
                else -> {
                    // Normalizar verbos iniciales ("que prenda" -> "prende", "apague" -> "apaga")
                    var command = clean
                    if (command.lowercase().startsWith("que ")) {
                        command = command.substring(4).trim()
                    }
                    if (command.lowercase().startsWith("prenda ")) {
                        command = "prende " + command.substring(7)
                    } else if (command.lowercase().startsWith("apague ")) {
                        command = "apaga " + command.substring(7)
                    } else if (command.lowercase().startsWith("ponga ")) {
                        command = "pon " + command.substring(6)
                    }
                    result.add(RoutineAction.ExecuteCommandAction(command))
                }
            }
        }
        return result
    }
}
