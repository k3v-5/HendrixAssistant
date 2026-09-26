package com.asistente.celular.ai.audit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

/**
 * Sanitizador local mediante expresiones regulares optimizadas para detección y enmascaramiento
 * de correos electrónicos, tarjetas bancarias, números telefónicos y claves de API.
 */
class RegexPiiScrubber : PiiScrubber {

    private val emailPattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val cardPattern = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b")
    private val phonePattern = Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b")
    private val apiKeyPattern = Pattern.compile("\\b(?:sk-[a-zA-Z0-9]{20,}|AIza[0-9A-Za-z\\-_]{35})\\b")

    override fun inspectAndScrub(text: String): PiiScrubResult {
        if (text.isBlank()) {
            return PiiScrubResult(scrubbedText = text, detectedCount = 0, detectedTypes = emptyList())
        }

        var current = text
        val detectedTypes = mutableListOf<String>()
        var count = 0

        // 1. Tarjetas de crédito
        val cardMatcher = cardPattern.matcher(current)
        if (cardMatcher.find()) {
            detectedTypes.add("TARJETA_BANCARIA")
            var cardMatches = 0
            val sb = StringBuffer()
            cardMatcher.reset()
            while (cardMatcher.find()) {
                cardMatches++
                cardMatcher.appendReplacement(sb, "[TARJETA_PROTEGIDA]")
            }
            cardMatcher.appendTail(sb)
            current = sb.toString()
            count += cardMatches
        }

        // 2. Claves de API / Secretos
        val keyMatcher = apiKeyPattern.matcher(current)
        if (keyMatcher.find()) {
            detectedTypes.add("API_KEY")
            var keyMatches = 0
            val sb = StringBuffer()
            keyMatcher.reset()
            while (keyMatcher.find()) {
                keyMatches++
                keyMatcher.appendReplacement(sb, "[API_KEY_PROTEGIDA]")
            }
            keyMatcher.appendTail(sb)
            current = sb.toString()
            count += keyMatches
        }

        // 3. Correos electrónicos
        val emailMatcher = emailPattern.matcher(current)
        if (emailMatcher.find()) {
            detectedTypes.add("EMAIL")
            var emailMatches = 0
            val sb = StringBuffer()
            emailMatcher.reset()
            while (emailMatcher.find()) {
                emailMatches++
                emailMatcher.appendReplacement(sb, "[EMAIL_PROTEGIDO]")
            }
            emailMatcher.appendTail(sb)
            current = sb.toString()
            count += emailMatches
        }

        // 4. Teléfonos
        val phoneMatcher = phonePattern.matcher(current)
        if (phoneMatcher.find()) {
            detectedTypes.add("TELEFONO")
            var phoneMatches = 0
            val sb = StringBuffer()
            phoneMatcher.reset()
            while (phoneMatcher.find()) {
                phoneMatches++
                phoneMatcher.appendReplacement(sb, "[TEL_PROTEGIDO]")
            }
            phoneMatcher.appendTail(sb)
            current = sb.toString()
            count += phoneMatches
        }

        return PiiScrubResult(
            scrubbedText = current,
            detectedCount = count,
            detectedTypes = detectedTypes
        )
    }
}

/**
 * Auditor de tráfico y privacidad en memoria, 100% en dispositivo y sin filtración de datos.
 * Mantiene un búfer circular de hasta [maxRecords] transacciones recientes y calcula métricas
 * de soberanía en tiempo real mediante StateFlow reactivo.
 */
class InMemoryAiTrafficAuditor(
    private val maxRecords: Int = 200
) : AiTrafficAuditor {

    private val _records = MutableStateFlow<List<AiTrafficRecord>>(emptyList())
    private val _summary = MutableStateFlow(AiPrivacyMetricsSummary())

    private val lock = Any()

    override fun recordEvent(record: AiTrafficRecord) {
        synchronized(lock) {
            val currentList = _records.value.toMutableList()
            currentList.add(0, record) // Más reciente al inicio
            if (currentList.size > maxRecords) {
                currentList.removeAt(currentList.lastIndex)
            }
            _records.value = currentList
            _summary.value = computeSummary(currentList)
        }
    }

    override fun observeTraffic(): StateFlow<List<AiTrafficRecord>> = _records.asStateFlow()

    override fun getSummary(): StateFlow<AiPrivacyMetricsSummary> = _summary.asStateFlow()

    override fun clearHistory() {
        synchronized(lock) {
            _records.value = emptyList()
            _summary.value = AiPrivacyMetricsSummary()
        }
    }

    private fun computeSummary(list: List<AiTrafficRecord>): AiPrivacyMetricsSummary {
        if (list.isEmpty()) return AiPrivacyMetricsSummary()

        val total = list.size.toLong()
        val local = list.count { it.isFullyLocal }.toLong()
        val cloud = total - local
        val localRatio = if (total > 0) (local.toDouble() / total.toDouble()) * 100.0 else 100.0

        val totalPromptBytes = list.sumOf { it.promptBytes }
        val totalResponseBytes = list.sumOf { it.responseBytes }
        val totalTokens = list.sumOf { (it.estimatedPromptTokens + it.estimatedResponseTokens).toLong() }
        val totalPii = list.sumOf { it.piiDetectedCount }
        val avgLatency = if (total > 0) list.sumOf { it.latencyMs } / total else 0L

        return AiPrivacyMetricsSummary(
            totalRequests = total,
            localRequests = local,
            cloudRequests = cloud,
            localRatioPercentage = Math.round(localRatio * 10.0) / 10.0,
            totalPromptBytes = totalPromptBytes,
            totalResponseBytes = totalResponseBytes,
            totalTokensProcessed = totalTokens,
            totalPiiEntitiesProtected = totalPii,
            averageLatencyMs = avgLatency
        )
    }
}
