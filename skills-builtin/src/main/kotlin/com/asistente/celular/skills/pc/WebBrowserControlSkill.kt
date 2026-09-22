package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.module.PcModuleActionRequest
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para control y búsqueda inteligente en navegadores web en la PC
 * (Google Chrome, Brave Browser, Opera, Microsoft Edge).
 *
 * Soporta búsquedas directas con plataforma parametrizada:
 * - "abrir brave y buscar en google plugins de produccion en facebook"
 * - "abrir chrome y buscar en youtube como realizar una cancion de cero a 100"
 * - "abre opera y busca tutoriales de blender en youtube"
 * - "abrir chrome" / "nueva pestaña en brave" / "cerrar pestaña"
 */
class WebBrowserControlSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "web_browser_control_skill",
        name = "Navegación Web y Búsqueda",
        description = "Abre navegadores web (Chrome, Brave, Opera) para búsquedas directas o administración de pestañas."
    ),
    specificity = Specificity.HIGH
) {

    data class BrowserIntent(
        val browserId: String,
        val browserDisplayName: String,
        val platform: String?,
        val query: String?,
        val actionId: String,
        val speechText: String
    )

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("abrir", "abre", "inicia", "ejecuta")),
            OptionalConstruct(WordConstruct("el", "la")),
            OptionalConstruct(WordConstruct("navegador")),
            WordConstruct("chrome", "brave", "opera", "edge"),
            OptionalConstruct(WordConstruct("y")),
            OptionalConstruct(WordConstruct("buscar", "busca", "search")),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("google", "youtube", "facebook", "github"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val hasBrowser = lower.contains("chrome") ||
                lower.contains("brave") ||
                lower.contains("opera") ||
                lower.contains("edge") ||
                lower.contains("navegador")

        val hasWebAction = lower.contains("abrir") ||
                lower.contains("abre") ||
                lower.contains("buscar") ||
                lower.contains("busca") ||
                lower.contains("search") ||
                lower.contains("pestana") ||
                lower.contains("recargar") ||
                lower.contains("actualizar")

        val hasPlatformSearch = lower.contains("en youtube") ||
                lower.contains("en google") ||
                lower.contains("en facebook") ||
                lower.contains("en github")

        if (hasBrowser && (hasWebAction || hasPlatformSearch)) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        if (hasPlatformSearch && (lower.contains("buscar") || lower.contains("busca"))) {
            return SkillScore(confidence = 0.95f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    fun parseIntent(input: String): BrowserIntent {
        val lower = MatchContext.normalize(input)

        // 1. Detectar navegador
        val (browserId, browserDisplayName) = when {
            lower.contains("brave") -> "brave" to "Brave"
            lower.contains("opera") -> "opera" to "Opera"
            lower.contains("edge") -> "edge" to "Edge"
            else -> "chrome" to "Google Chrome"
        }

        // 2. Comandos de pestañas
        if (lower.contains("nueva pestana") || lower.contains("abrir pestana") || lower.contains("crear pestana")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = null,
                query = null,
                actionId = "NEW_TAB",
                speechText = "Abriendo nueva pestaña en $browserDisplayName."
            )
        }
        if (lower.contains("cerrar pestana") || lower.contains("cierra la pestana") || lower.contains("cerrar la pestana")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = null,
                query = null,
                actionId = "CLOSE_TAB",
                speechText = "Cerrando pestaña actual en $browserDisplayName."
            )
        }
        if (lower.contains("recargar") || lower.contains("recarga la pagina") || lower.contains("actualizar pagina") || lower.contains("actualiza la pagina")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = null,
                query = null,
                actionId = "RELOAD_PAGE",
                speechText = "Recargando página en $browserDisplayName."
            )
        }

        // 3. Páginas conocidas directas
        if (lower.contains("abrir youtube") || lower.contains("abre youtube")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = "youtube",
                query = null,
                actionId = "OPEN_YOUTUBE",
                speechText = "Abriendo YouTube en $browserDisplayName."
            )
        }
        if (lower.contains("abrir facebook") || lower.contains("abre facebook")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = "facebook",
                query = null,
                actionId = "OPEN_FACEBOOK",
                speechText = "Abriendo Facebook en $browserDisplayName."
            )
        }
        if (lower.contains("abrir github") || lower.contains("abre github")) {
            return BrowserIntent(
                browserId = browserId,
                browserDisplayName = browserDisplayName,
                platform = "github",
                query = null,
                actionId = "OPEN_GITHUB",
                speechText = "Abriendo GitHub en $browserDisplayName."
            )
        }

        // 4. Búsqueda contextual
        val isSearch = lower.contains("buscar") || lower.contains("busca") || lower.contains("search")
        if (isSearch) {
            // Caso 4A: "buscar en <platform> <query>"
            val searchInPlatformRegex = Regex("""(?:buscar|busca|buscame|search)\s+en\s+(google|youtube|facebook|github|duckduckgo)\s*(?:sobre|para|por)?\s*(.*)""", RegexOption.IGNORE_CASE)
            val matchPrefix = searchInPlatformRegex.find(lower)

            if (matchPrefix != null) {
                val matchedPlatform = matchPrefix.groupValues[1].lowercase()
                var queryCandidate = matchPrefix.groupValues[2].trim()

                // Si el queryCandidate termina en "en <browser>", quitarlo
                val trailingBrowser = Regex("""^(.*?)\s+en\s+(chrome|brave|opera|edge)$""", RegexOption.IGNORE_CASE).find(queryCandidate)
                if (trailingBrowser != null) {
                    queryCandidate = trailingBrowser.groupValues[1].trim()
                }

                val platformDisplayName = when (matchedPlatform) {
                    "youtube" -> "YouTube"
                    "google" -> "Google"
                    "facebook" -> "Facebook"
                    "github" -> "GitHub"
                    "duckduckgo" -> "DuckDuckGo"
                    else -> matchedPlatform.replaceFirstChar { it.uppercase() }
                }

                val cleanQuery = queryCandidate.ifBlank { null }
                val speech = if (cleanQuery != null) {
                    "Buscando '$cleanQuery' en $platformDisplayName con $browserDisplayName."
                } else {
                    "Abriendo $platformDisplayName en $browserDisplayName."
                }

                return BrowserIntent(
                    browserId = browserId,
                    browserDisplayName = browserDisplayName,
                    platform = matchedPlatform,
                    query = cleanQuery,
                    actionId = "NAVIGATE_OR_SEARCH",
                    speechText = speech
                )
            }

            // Caso 4B: "buscar <query> en <platform>" o "buscar <query>"
            val generalSearchRegex = Regex("""(?:buscar|busca|buscame|search)\s+(?:sobre|para|por)?\s*(.+)""", RegexOption.IGNORE_CASE)
            val matchGeneral = generalSearchRegex.find(lower)

            if (matchGeneral != null) {
                var queryCandidate = matchGeneral.groupValues[1].trim()
                var targetPlatform = "google"

                // Comprobar si termina en "en <browser>"
                val trailingBrowser = Regex("""^(.*?)\s+en\s+(chrome|brave|opera|edge)$""", RegexOption.IGNORE_CASE).find(queryCandidate)
                if (trailingBrowser != null) {
                    queryCandidate = trailingBrowser.groupValues[1].trim()
                }

                // Comprobar si termina en "en <platform>"
                val trailingPlatform = Regex("""^(.*?)\s+en\s+(google|youtube|facebook|github|duckduckgo)$""", RegexOption.IGNORE_CASE).find(queryCandidate)
                if (trailingPlatform != null) {
                    queryCandidate = trailingPlatform.groupValues[1].trim()
                    targetPlatform = trailingPlatform.groupValues[2].lowercase()
                }

                val platformDisplayName = when (targetPlatform) {
                    "youtube" -> "YouTube"
                    "google" -> "Google"
                    "facebook" -> "Facebook"
                    "github" -> "GitHub"
                    "duckduckgo" -> "DuckDuckGo"
                    else -> targetPlatform.replaceFirstChar { it.uppercase() }
                }

                val cleanQuery = queryCandidate.ifBlank { null }
                val speech = if (cleanQuery != null) {
                    "Buscando '$cleanQuery' en $platformDisplayName con $browserDisplayName."
                } else {
                    "Abriendo $browserDisplayName en la PC."
                }

                return BrowserIntent(
                    browserId = browserId,
                    browserDisplayName = browserDisplayName,
                    platform = targetPlatform,
                    query = cleanQuery,
                    actionId = "NAVIGATE_OR_SEARCH",
                    speechText = speech
                )
            }
        }

        // 5. Apertura simple de navegador sin consulta
        val defaultAction = when (browserId) {
            "brave" -> "OPEN_BRAVE"
            "opera" -> "OPEN_OPERA"
            else -> "OPEN_CHROME"
        }

        return BrowserIntent(
            browserId = browserId,
            browserDisplayName = browserDisplayName,
            platform = null,
            query = null,
            actionId = defaultAction,
            speechText = "Abriendo navegador $browserDisplayName en la PC."
        )
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val intent = parseIntent(input)

        val params = mutableMapOf<String, String>()
        params["browser"] = intent.browserId
        if (intent.platform != null) {
            params["platform"] = intent.platform
        }
        if (intent.query != null) {
            params["query"] = intent.query
        }

        val bridge = pcBridge
        val result = if (bridge != null) {
            bridge.executeModuleAction(
                PcModuleActionRequest(
                    moduleId = PcModuleId.WEB_BROWSERS,
                    actionId = intent.actionId,
                    params = params
                )
            )
        } else {
            com.asistente.celular.nlu.pc.module.PcModuleActionResult(success = true)
        }

        val responseMessage = if (result.success) {
            intent.speechText
        } else {
            "Error al controlar navegador en la PC: ${result.message}"
        }

        return SkillOutput(
            speech = responseMessage,
            displayText = responseMessage
        )
    }
}
