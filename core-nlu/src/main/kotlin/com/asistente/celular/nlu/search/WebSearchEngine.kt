package com.asistente.celular.nlu.search

import com.asistente.celular.nlu.ui.WebSearchResultItem

/**
 * Resultado de una búsqueda e investigación en tiempo real en la web.
 */
data class WebSearchResponse(
    val query: String,
    val spokenSummary: String,
    val detailedMarkdown: String,
    val results: List<WebSearchResultItem> = emptyList(),
    val isSuccess: Boolean = true
)

/**
 * Contrato para motores de investigación y búsqueda web en vivo.
 * Sigue el principio DIP (SOLID) permitiendo múltiples proveedores de búsqueda
 * (DuckDuckGo, SearXNG, Google Custom Search o LLM con herramientas de navegación).
 */
interface WebSearchEngine {

    /**
     * Ejecuta una consulta en la web, extrae los fragmentos más relevantes y
     * sintetiza una respuesta hablada (<280 caracteres) y estructurada en Markdown.
     */
    suspend fun searchAndSynthesize(
        query: String,
        maxResults: Int = 3
    ): WebSearchResponse
}
