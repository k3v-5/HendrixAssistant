package com.asistente.celular.nlu.math

import com.asistente.celular.nlu.parser.SpanishNumberParser
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Contrato para proveedores de tasas de cambio de divisas.
 * Abierto a extensión (OCP) para conectar en el futuro con APIs en línea o fuentes personalizadas.
 */
interface CurrencyRateProvider {
    val baseCurrency: String
    fun getRate(currencyCode: String): BigDecimal?
    fun getAllRates(): Map<String, BigDecimal>
}

/**
 * Proveedor de tasas de cambio offline de referencia con las divisas más utilizadas.
 */
class DefaultCurrencyRateProvider : CurrencyRateProvider {
    override val baseCurrency: String = "USD"

    // Tasas de referencia respecto a 1 USD
    private val rates = mapOf(
        "USD" to BigDecimal("1.00"),
        "EUR" to BigDecimal("0.92"),
        "MXN" to BigDecimal("19.80"),
        "ARS" to BigDecimal("960.00"),
        "COP" to BigDecimal("4150.00"),
        "CLP" to BigDecimal("930.00"),
        "GBP" to BigDecimal("0.77"),
        "BRL" to BigDecimal("5.45"),
        "JPY" to BigDecimal("142.00"),
        "CAD" to BigDecimal("1.36"),
        "PEN" to BigDecimal("3.75")
    )

    override fun getRate(currencyCode: String): BigDecimal? = rates[currencyCode.uppercase()]
    override fun getAllRates(): Map<String, BigDecimal> = rates
}

/**
 * Resultado de una conversión de divisas.
 */
data class CurrencyConversionResult(
    val amount: BigDecimal,
    val fromCurrency: String,
    val fromName: String,
    val toCurrency: String,
    val toName: String,
    val resultAmount: BigDecimal,
    val rate: BigDecimal,
    val formattedResult: String,
    val speechText: String
)

/**
 * Convertidor de divisas 100% offline con reconocimiento en lenguaje natural en español.
 */
class CurrencyConverter(
    private val rateProvider: CurrencyRateProvider = DefaultCurrencyRateProvider()
) {
    private val mathContext = MathContext(8, RoundingMode.HALF_UP)
    private val format2Dec = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("es", "MX")))

    private val currencyAliases = mapOf(
        "dolar" to "USD", "dólar" to "USD", "dolares" to "USD", "dólares" to "USD", "usd" to "USD", "bucks" to "USD",
        "euro" to "EUR", "euros" to "EUR", "eur" to "EUR",
        "peso" to "MXN", "pesos" to "MXN", "peso mexicano" to "MXN", "pesos mexicanos" to "MXN", "mxn" to "MXN",
        "peso argentino" to "ARS", "pesos argentinos" to "ARS", "ars" to "ARS",
        "peso colombiano" to "COP", "pesos colombianos" to "COP", "cop" to "COP",
        "peso chileno" to "CLP", "pesos chilenos" to "CLP", "clp" to "CLP",
        "libra" to "GBP", "libras" to "GBP", "libra esterlina" to "GBP", "libras esterlinas" to "GBP", "gbp" to "GBP",
        "real" to "BRL", "reales" to "BRL", "real brasileño" to "BRL", "brl" to "BRL",
        "yen" to "JPY", "yenes" to "JPY", "jpy" to "JPY",
        "sol" to "PEN", "soles" to "PEN", "sol peruano" to "PEN", "pen" to "PEN",
        "dolar canadiense" to "CAD", "dólar canadiense" to "CAD", "dolares canadienses" to "CAD", "cad" to "CAD"
    )

    private val currencyDisplayNames = mapOf(
        "USD" to "Dólares",
        "EUR" to "Euros",
        "MXN" to "Pesos Mexicanos",
        "ARS" to "Pesos Argentinos",
        "COP" to "Pesos Colombianos",
        "CLP" to "Pesos Chilenos",
        "GBP" to "Libras Esterlinas",
        "BRL" to "Reales Brasileños",
        "JPY" to "Yenes",
        "PEN" to "Soles Peruanos",
        "CAD" to "Dólares Canadienses"
    )

    /**
     * Intenta interpretar una consulta de divisas en lenguaje natural.
     * Ejemplos:
     * - "100 dólares a pesos"
     * - "convierte 50 euros en dólares"
     * - "¿cuánto son 250 pesos mexicanos en euros?"
     * - "¿a cuánto está el dólar?"
     */
    fun parseAndConvert(input: String): CurrencyConversionResult? {
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Consulta de tipo de cambio simple ("a cuánto está el dólar", "precio del euro")
        if (lower.contains("a cuanto esta") || lower.contains("a cuánto está") ||
            lower.contains("tipo de cambio") || lower.contains("precio del") || lower.contains("valor del")) {
            for ((alias, code) in currencyAliases.entries.sortedByDescending { it.key.length }) {
                if (lower.contains(alias)) {
                    val targetCode = if (code == "MXN") "USD" else "MXN"
                    return convert(BigDecimal.ONE, code, targetCode)
                }
            }
        }

        // 2. Patrón de consulta inversa: "cuántos pesos son 100 dólares"
        val regexReverse = """(?:cuanto son|cuánto son|cuantos|cuántos|cuantas|cuántas)\s+([\p{L}\s]+?)\s+(?:son|en|equivalen\s+a)\s+([0-9]+(?:\.[0-9]+)?|[\p{L}]+)\s+([\p{L}\s]+)""".toRegex()
        val matchReverse = regexReverse.find(lower)
        if (matchReverse != null) {
            val rawTo = matchReverse.groupValues[1].trim()
            val rawAmount = matchReverse.groupValues[2].trim()
            val rawFrom = matchReverse.groupValues[3].trim()

            val amount = rawAmount.toBigDecimalOrNull()
                ?: SpanishNumberParser.parseNumber(rawAmount)?.toBigDecimal()
                ?: return null

            val fromCode = resolveCurrencyCode(rawFrom) ?: return null
            val toCode = resolveCurrencyCode(rawTo) ?: return null

            return convert(amount, fromCode, toCode)
        }

        // 3. Patrón de conversión directa: "[convierte|cuanto son|pasa]? [monto] [moneda1] [a|en] [moneda2]"
        val regex = """(?:convierte|cuanto son|cuánto son|pasa|cambia)?\s*([0-9]+(?:\.[0-9]+)?|[\p{L}]+)\s+([\p{L}\s]+?)\s+(?:a|en|por)\s+([\p{L}\s]+)""".toRegex()
        val match = regex.find(lower)

        if (match != null) {
            val rawAmount = match.groupValues[1].trim()
            val rawFrom = match.groupValues[2].trim()
            val rawTo = match.groupValues[3].trim()

            val amount = rawAmount.toBigDecimalOrNull()
                ?: SpanishNumberParser.parseNumber(rawAmount)?.toBigDecimal()
                ?: return null

            val fromCode = resolveCurrencyCode(rawFrom) ?: return null
            val toCode = resolveCurrencyCode(rawTo) ?: return null

            return convert(amount, fromCode, toCode)
        }

        return null
    }

    fun convert(amount: BigDecimal, fromCode: String, toCode: String): CurrencyConversionResult? {
        val fromRate = rateProvider.getRate(fromCode) ?: return null
        val toRate = rateProvider.getRate(toCode) ?: return null

        // Convertir a base USD, luego a destino
        // USD Amount = amount / fromRate
        // Final Amount = USD Amount * toRate
        val usdAmount = amount.divide(fromRate, mathContext)
        val result = usdAmount.multiply(toRate, mathContext).setScale(2, RoundingMode.HALF_UP)
        val directRate = toRate.divide(fromRate, mathContext).setScale(4, RoundingMode.HALF_UP)

        val fromName = currencyDisplayNames[fromCode] ?: fromCode
        val toName = currencyDisplayNames[toCode] ?: toCode

        val formattedResult = "${format2Dec.format(result)} $toCode"
        val speech = "${format2Dec.format(amount)} $fromName equivalen aproximadamente a $formattedResult."

        return CurrencyConversionResult(
            amount = amount,
            fromCurrency = fromCode,
            fromName = fromName,
            toCurrency = toCode,
            toName = toName,
            resultAmount = result,
            rate = directRate,
            formattedResult = formattedResult,
            speechText = speech
        )
    }

    private fun resolveCurrencyCode(text: String): String? {
        val clean = text.trim()
        // Buscar de la clave más larga a la más corta para evitar colisiones ("pesos mexicanos" antes de "pesos")
        for ((alias, code) in currencyAliases.entries.sortedByDescending { it.key.length }) {
            if (clean == alias || clean.contains(alias)) {
                return code
            }
        }
        return null
    }
}
