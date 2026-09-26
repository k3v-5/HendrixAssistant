package com.asistente.celular.ai.search

import android.util.Log
import com.asistente.celular.nlu.search.WebSearchEngine
import com.asistente.celular.nlu.search.WebSearchResponse
import com.asistente.celular.nlu.ui.WebSearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Motor de búsqueda e investigación en tiempo real basado en HTTP y APIs de conocimiento públicas
 * (DuckDuckGo Knowledge API, OpenSearch y extracción de contenido).
 * Diseñado con tolerancia a fallos, soporte para inyección de cliente HTTP personalizado y
 * síntesis de resúmenes hablados concisos para Hendrix.
 */
class DefaultWebSearchEngine(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : WebSearchEngine {

    companion object {
        private const val TAG = "DefaultWebSearchEngine"
        private const val DUCKDUCKGO_API_URL = "https://api.duckduckgo.com/?q=%s&format=json&no_html=1&skip_disambig=1"
        private const val WIKIPEDIA_SUMMARY_URL = "https://es.wikipedia.org/api/rest_v1/page/summary/%s"
    }

    override suspend fun searchAndSynthesize(
        query: String,
        maxResults: Int
    ): WebSearchResponse = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext WebSearchResponse(
                query = query,
                spokenSummary = "La consulta de búsqueda está vacía.",
                detailedMarkdown = "Por favor indica un término de búsqueda válido.",
                isSuccess = false
            )
        }

        try {
            // 1. Intentar consulta a DuckDuckGo Instant Answer API
            val ddgResponse = fetchDuckDuckGoInstantAnswer(cleanQuery, maxResults)
            if (ddgResponse != null && ddgResponse.results.isNotEmpty()) {
                return@withContext ddgResponse
            }

            // 2. Fallback complementario a Wikipedia Summary si no hay respuesta directa
            val wikiResponse = fetchWikipediaSummary(cleanQuery)
            if (wikiResponse != null) {
                return@withContext wikiResponse
            }

            // 3. Si no hay respuestas estructuradas pero la consulta fue enviada con éxito
            return@withContext WebSearchResponse(
                query = cleanQuery,
                spokenSummary = "Encontré información sobre $cleanQuery, pero no hay un resumen directo disponible en este momento.",
                detailedMarkdown = "### Búsqueda: $cleanQuery\n\nNo se encontraron tarjetas de respuesta directa para esta consulta. Prueba reformular tu pregunta.",
                results = emptyList(),
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al ejecutar búsqueda web para '$cleanQuery': ${e.message}", e)
            val isNetwork = e is IOException
            val errorMsg = if (isNetwork) {
                "No pude conectarme a internet para investigar sobre $cleanQuery."
            } else {
                "Ocurrió un error al procesar la búsqueda de $cleanQuery."
            }
            return@withContext WebSearchResponse(
                query = cleanQuery,
                spokenSummary = errorMsg,
                detailedMarkdown = "⚠️ **Error de búsqueda web**\n\n$errorMsg",
                results = emptyList(),
                isSuccess = false
            )
        }
    }

    private fun fetchDuckDuckGoInstantAnswer(query: String, maxResults: Int): WebSearchResponse? {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = String.format(DUCKDUCKGO_API_URL, encoded)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HendrixAssistant/2.0 (Android; Mobile)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null

            val root = json.parseToJsonElement(bodyString).jsonObject
            val abstractText = root["AbstractText"]?.jsonPrimitive?.content ?: ""
            val abstractSource = root["AbstractSource"]?.jsonPrimitive?.content ?: "DuckDuckGo"
            val abstractUrl = root["AbstractURL"]?.jsonPrimitive?.content ?: ""
            val heading = root["Heading"]?.jsonPrimitive?.content ?: query

            val results = mutableListOf<WebSearchResultItem>()

            if (abstractText.isNotBlank()) {
                results.add(
                    WebSearchResultItem(
                        title = heading.ifBlank { query },
                        snippet = abstractText,
                        url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encoded" },
                        sourceName = abstractSource
                    )
                )
            }

            // Parsear RelatedTopics
            val relatedTopics = root["RelatedTopics"]?.jsonArray
            if (relatedTopics != null) {
                for (elem in relatedTopics) {
                    if (results.size >= maxResults) break
                    val obj = elem.jsonObject
                    val text = obj["Text"]?.jsonPrimitive?.content
                    val firstUrl = obj["FirstURL"]?.jsonPrimitive?.content
                    if (!text.isNullOrBlank() && !firstUrl.isNullOrBlank()) {
                        results.add(
                            WebSearchResultItem(
                                title = text.take(60) + if (text.length > 60) "..." else "",
                                snippet = text,
                                url = firstUrl,
                                sourceName = "DuckDuckGo"
                            )
                        )
                    }
                }
            }

            if (results.isEmpty()) return null

            val spokenAnswer = formatSpokenAnswer(query, results.first().snippet)
            val markdown = buildMarkdownReport(query, spokenAnswer, results)

            return WebSearchResponse(
                query = query,
                spokenSummary = spokenAnswer,
                detailedMarkdown = markdown,
                results = results,
                isSuccess = true
            )
        }
    }

    private fun fetchWikipediaSummary(query: String): WebSearchResponse? {
        val encoded = URLEncoder.encode(query.replace(" ", "_"), StandardCharsets.UTF_8.name())
        val url = String.format(WIKIPEDIA_SUMMARY_URL, encoded)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HendrixAssistant/2.0 (Android; Mobile)")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null

                val root = json.parseToJsonElement(bodyString).jsonObject
                val title = root["title"]?.jsonPrimitive?.content ?: query
                val extract = root["extract"]?.jsonPrimitive?.content ?: return null
                val pageUrl = root["content_urls"]?.jsonObject?.get("desktop")?.jsonObject?.get("page")?.jsonPrimitive?.content
                    ?: "https://es.wikipedia.org/wiki/$encoded"

                val item = WebSearchResultItem(
                    title = title,
                    snippet = extract,
                    url = pageUrl,
                    sourceName = "Wikipedia"
                )

                val spokenAnswer = formatSpokenAnswer(query, extract)
                val markdown = buildMarkdownReport(query, spokenAnswer, listOf(item))

                WebSearchResponse(
                    query = query,
                    spokenSummary = spokenAnswer,
                    detailedMarkdown = markdown,
                    results = listOf(item),
                    isSuccess = true
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatSpokenAnswer(query: String, snippet: String): String {
        val clean = snippet
            .replace(Regex("""\[\d+\]"""), "") // Quitar citas [1], [2]
            .replace(Regex("""\s+"""), " ")
            .trim()

        val firstSentence = clean.split(Regex("""(?<=[.!?])\s+""")).firstOrNull() ?: clean
        return if (firstSentence.length > 250) {
            firstSentence.take(247) + "..."
        } else {
            firstSentence
        }
    }

    private fun buildMarkdownReport(
        query: String,
        spokenAnswer: String,
        sources: List<WebSearchResultItem>
    ): String {
        val sb = StringBuilder()
        sb.append("### 🌐 Investigación Web: $query\n\n")
        sb.append(spokenAnswer).append("\n\n")

        if (sources.isNotEmpty()) {
            sb.append("**Fuentes y Enlaces:**\n")
            sources.forEach { source ->
                sb.append("- [${source.title}](${source.url}) — *${source.sourceName}*\n")
            }
        }

        return sb.toString().trim()
    }
}
