package com.asistente.celular.nlu.automation

import java.util.UUID

/**
 * Parser de lenguaje natural para inferir y auto-generar rutinas automatizadas Zero-Touch al vuelo.
 * Extrae desencadenantes (triggers de red, eventos de PC, lanzamiento de apps, horarios) y
 * acciones encadenadas (comandos de asistente, escenas, control de hardware y atajos).
 */
object NaturalLanguageRoutineParser {

    fun parse(input: String): AutomatedRoutine? {
        val lower = input.lowercase().trim()

        // 1. Identificar si es una solicitud de creación de regla o automatización
        val isCreationRequest = lower.startsWith("cada vez que") ||
                lower.startsWith("cuando") ||
                lower.startsWith("siempre que") ||
                lower.startsWith("crea una rutina") ||
                lower.startsWith("creame una rutina") ||
                lower.startsWith("crear rutina") ||
                lower.contains("crea una automatizacion") ||
                lower.contains("crear automatizacion")

        if (!isCreationRequest) return null

        // 2. Extraer Trigger y Acciones preservando mayúsculas/minúsculas originales
        var triggerPart = ""
        var actionsPart = ""

        if (input.contains(",")) {
            val commaSplit = input.split(",", limit = 2)
            triggerPart = commaSplit[0].trim()
            actionsPart = commaSplit.getOrElse(1) { "" }.trim()
        } else if (input.contains(" entonces ", ignoreCase = true)) {
            val split = input.split(Regex("(?i)\\s+entonces\\s+"), limit = 2)
            triggerPart = split[0].trim()
            actionsPart = split.getOrElse(1) { "" }.trim()
        } else {
            triggerPart = input
            actionsPart = ""
        }

        val triggers = parseTriggers(triggerPart)
        if (triggers.isEmpty()) return null

        val actions = parseActions(actionsPart)
        if (actions.isEmpty()) return null

        val routineName = buildRoutineName(triggerPart, actionsPart)

        return AutomatedRoutine(
            id = "auto_${UUID.randomUUID().toString().take(8)}",
            name = routineName,
            description = "Automatización creada por voz: $input",
            iconEmoji = determineEmoji(triggers, actions),
            isEnabled = true,
            triggers = triggers,
            actions = actions
        )
    }

    private fun parseTriggers(triggerText: String): List<AutomatedRoutineTrigger> {
        val triggers = mutableListOf<AutomatedRoutineTrigger>()
        val triggerLower = triggerText.lowercase()

        when {
            // Caso: Lanzamiento de app ("cada vez que abra Unreal", "cuando abra Blender")
            triggerLower.contains("abra ") || triggerLower.contains("inicie ") || triggerLower.contains("abro ") -> {
                val regex = Regex("""(?i)(?:abra|inicie|abro)\s+([a-zA-Z0-9_\-]+)""")
                val match = regex.find(triggerText)
                val app = match?.groupValues?.getOrNull(1)?.trim() ?: "app"
                triggers.add(AutomatedRoutineTrigger.PcEventTrigger("FOREGROUND_APP:${app.lowercase()}"))
            }

            // Caso: Wi-Fi conectado ("cuando me conecte al wifi Casa")
            triggerLower.contains("wifi") || triggerLower.contains("wi-fi") -> {
                val regex = Regex("""(?i)(?:wifi|wi-fi)\s+([a-zA-Z0-9_\-]+)""")
                val match = regex.find(triggerText)
                val ssid = match?.groupValues?.getOrNull(1)?.trim() ?: "Home_WiFi"
                triggers.add(AutomatedRoutineTrigger.WifiSsidTrigger(ssid = ssid, transition = WifiTransition.CONNECTED))
            }

            // Caso: Render o proceso terminado
            triggerLower.contains("render") && (triggerLower.contains("termine") || triggerLower.contains("complete")) -> {
                triggers.add(AutomatedRoutineTrigger.PcEventTrigger("RENDER_COMPLETED"))
            }

            // Caso: Sobrecalentamiento de GPU o hardware
            triggerLower.contains("caliente") || triggerLower.contains("temperatura") || triggerLower.contains("overheat") -> {
                triggers.add(AutomatedRoutineTrigger.PcEventTrigger("GPU_OVERHEAT"))
            }

            // Caso: Carga del móvil
            triggerLower.contains("cargue") || triggerLower.contains("cargando") || triggerLower.contains("enchufe") -> {
                triggers.add(AutomatedRoutineTrigger.ChargingTrigger(isCharging = true))
            }

            // Default: Voice Phrase Trigger
            else -> {
                triggers.add(AutomatedRoutineTrigger.VoicePhraseTrigger(listOf(triggerText)))
            }
        }

        return triggers
    }

    private fun parseActions(actionsText: String): List<AutomatedRoutineAction> {
        if (actionsText.isBlank()) return emptyList()

        // Separar múltiples acciones por "y", "luego", "después"
        val rawActions = actionsText
            .split(Regex("""(?i)\s+(?:y|luego|después|ademas|además)\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val result = mutableListOf<AutomatedRoutineAction>()

        for (act in rawActions) {
            val actLower = act.lowercase()
            when {
                // Caso: Cerrar navegadores / apps
                actLower.contains("cierra ") || actLower.contains("cerrar ") -> {
                    result.add(AutomatedRoutineAction.AssistantCommandAction(act))
                }

                // Caso: Ventilador o ventilación de hardware
                actLower.contains("ventilador") && (actLower.contains("maximo") || actLower.contains("máximo") || actLower.contains("100")) -> {
                    result.add(
                        AutomatedRoutineAction.PcPluginAction(
                            pluginId = "fan_control",
                            actionId = "set_speed",
                            params = mapOf("speed" to 100)
                        )
                    )
                }

                // Caso: Suspender PC
                actLower.contains("suspende") || actLower.contains("dormir") || actLower.contains("apaga la pc") -> {
                    result.add(AutomatedRoutineAction.PcQuickCommandAction("sleep"))
                }

                // Caso: Modo Standby
                actLower.contains("standby") || actLower.contains("pantalla de escritorio") -> {
                    result.add(AutomatedRoutineAction.EnterDeskStandbyAction(isKeepScreenOn = true))
                }

                // Caso: Escenas de estudio
                actLower.contains("escena ") -> {
                    val scene = act.substring(actLower.indexOf("escena ") + 7).trim().take(20)
                    result.add(AutomatedRoutineAction.PcStudioSceneAction(scene))
                }

                // Caso: Texto / TTS
                actLower.contains("avisa") || actLower.contains("dime ") || actLower.contains("notifica") -> {
                    result.add(AutomatedRoutineAction.SpeakTtsAction(act))
                }

                // Por defecto: comando de asistente genérico
                else -> {
                    result.add(AutomatedRoutineAction.AssistantCommandAction(act))
                }
            }
        }

        return result
    }

    private fun buildRoutineName(trigger: String, actions: String): String {
        val cleanTrigger = trigger
            .replace(Regex("""^(?i)(?:cada vez que|cuando|siempre que|crea una rutina)\s+"""), "")
            .trim()
            .take(30)
            .replaceFirstChar { it.uppercase() }

        return if (cleanTrigger.isNotBlank()) {
            "Auto: $cleanTrigger"
        } else {
            "Nueva Rutina Inteligente"
        }
    }

    private fun determineEmoji(
        triggers: List<AutomatedRoutineTrigger>,
        actions: List<AutomatedRoutineAction>
    ): String {
        val firstTrigger = triggers.firstOrNull()
        return when (firstTrigger) {
            is AutomatedRoutineTrigger.PcEventTrigger -> {
                if (firstTrigger.eventType.contains("unreal", ignoreCase = true)) "🎮"
                else if (firstTrigger.eventType.contains("blender", ignoreCase = true)) "🎨"
                else if (firstTrigger.eventType.contains("render", ignoreCase = true)) "🎞️"
                else "💻"
            }
            is AutomatedRoutineTrigger.WifiSsidTrigger -> "📶"
            is AutomatedRoutineTrigger.ChargingTrigger -> "⚡"
            is AutomatedRoutineTrigger.ScheduleTrigger -> "⏰"
            else -> "⚡"
        }
    }
}
