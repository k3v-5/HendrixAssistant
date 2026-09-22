package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.plugin.PcPluginAction
import com.asistente.celular.nlu.pc.plugin.PcPluginDefinition
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para consultar y ejecutar scripts y plugins de usuario en Python en la PC.
 */
class PcCustomPluginSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_custom_plugin_skill",
        name = "Custom PC Plugins",
        description = "Consulta y ejecuta scripts y plugins dinámicos de usuario en Python dentro de Hendrix Desktop."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("ejecuta", "ejecutar", "corre", "correr", "lista", "listar", "plugins", "scripts", "limpiar", "backup"),
            OptionalConstruct(WordConstruct("el", "los", "de", "en", "la")),
            WordConstruct("plugin", "plugins", "script", "scripts", "temporales", "cache", "proyectos", "dns"),
            OptionalConstruct(WordConstruct("de", "en", "la")),
            OptionalConstruct(WordConstruct("pc", "computadora", "desktop"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("plugins de la pc") ||
            lower.contains("que plugins hay") ||
            lower.contains("plugins instalados") ||
            lower.contains("scripts de la pc") ||
            lower.contains("lista de plugins") ||
            lower.contains("ejecuta el plugin") ||
            lower.contains("ejecutar plugin") ||
            lower.contains("corre el plugin") ||
            lower.contains("limpia los temporales") ||
            lower.contains("limpiar temporales") ||
            lower.contains("limpiar cache") ||
            lower.contains("backup de proyectos") ||
            lower.contains("hacer backup") ||
            lower.contains("flush dns") ||
            lower.contains("limpiar dns") ||
            lower.contains("espacio en disco")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para consultar o ejecutar plugins de usuario.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()
        val isQueryIntent = lower.contains("que plugins") ||
                lower.contains("lista de plugins") ||
                lower.contains("plugins instalados") ||
                lower.contains("scripts de la pc") ||
                lower.contains("plugins de la pc") && !lower.contains("ejecuta") && !lower.contains("corre")

        val plugins = bridge.queryCustomPlugins()

        if (isQueryIntent) {
            if (plugins.isEmpty()) {
                return SkillOutput(
                    speech = "No se encontraron plugins de usuario cargados en la carpeta plugins de la computadora.",
                    displayText = "🧩 0 Plugins detectados"
                )
            }
            val names = plugins.joinToString(", ") { it.name }
            return SkillOutput(
                speech = "Tienes ${plugins.size} plugins disponibles en la PC: $names.",
                displayText = "🧩 ${plugins.size} Plugins en PC:\n" + plugins.joinToString("\n") { "• ${it.name} (v${it.version})" }
            )
        }

        // Determinar qué plugin y acción ejecutar
        val matchedTarget = resolvePluginAndAction(lower, plugins)
        if (matchedTarget == null) {
            return SkillOutput(
                speech = "No identifiqué qué plugin o acción deseas ejecutar. Puedes consultar la lista diciendo '¿Qué plugins hay?'.",
                displayText = "❓ Acción de plugin no reconocida"
            )
        }

        val (plugin, action) = matchedTarget
        val result = bridge.executeCustomPluginAction(plugin.id, action.id)

        return if (result.success) {
            SkillOutput(
                speech = "Acción ${action.label} completada con éxito. ${result.message}",
                displayText = "✅ ${action.label}\n${result.message}" + if (result.output.isNotBlank()) "\n\n${result.output}" else ""
            )
        } else {
            SkillOutput(
                speech = "La acción ${action.label} no se pudo completar. ${result.message}",
                displayText = "⚠️ Error en ${action.label}\n${result.message}"
            )
        }
    }

    private fun resolvePluginAndAction(
        lower: String,
        plugins: List<PcPluginDefinition>
    ): Pair<PcPluginDefinition, PcPluginAction>? {
        // Coincidencias rápidas por palabras clave de los plugins por defecto
        if (lower.contains("backup") || lower.contains("respaldo")) {
            val p = plugins.find { it.id == "backup_projects" }
            val a = p?.actions?.find { it.id == "backup_now" } ?: p?.actions?.firstOrNull()
            if (p != null && a != null) return Pair(p, a)
        }
        if (lower.contains("temporal") || lower.contains("temp") || lower.contains("cache")) {
            val p = plugins.find { it.id == "temp_cleaner" }
            val a = p?.actions?.find { it.id == "clean_now" } ?: p?.actions?.firstOrNull()
            if (p != null && a != null) return Pair(p, a)
        }
        if (lower.contains("dns")) {
            val p = plugins.find { it.id == "command_runner" }
            val a = p?.actions?.find { it.id == "flush_dns" } ?: p?.actions?.firstOrNull()
            if (p != null && a != null) return Pair(p, a)
        }
        if (lower.contains("disco") || lower.contains("espacio")) {
            val p = plugins.find { it.id == "command_runner" }
            val a = p?.actions?.find { it.id == "check_disk" } ?: p?.actions?.firstOrNull()
            if (p != null && a != null) return Pair(p, a)
        }

        // Búsqueda dinámica entre los plugins registrados
        for (plugin in plugins) {
            val pluginMatch = lower.contains(plugin.id.lowercase()) || lower.contains(plugin.name.lowercase())
            for (action in plugin.actions) {
                if (pluginMatch || lower.contains(action.id.lowercase()) || lower.contains(action.label.lowercase())) {
                    return Pair(plugin, action)
                }
            }
        }

        return null
    }
}
