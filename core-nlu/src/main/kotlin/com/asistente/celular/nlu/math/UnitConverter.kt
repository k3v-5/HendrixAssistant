package com.asistente.celular.nlu.math

import com.asistente.celular.nlu.parser.SpanishNumberParser
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Resultado de una conversión de unidades.
 */
data class UnitConversionResult(
    val amount: Double,
    val fromUnitName: String,
    val fromSymbol: String,
    val toUnitName: String,
    val toSymbol: String,
    val resultAmount: Double,
    val formattedResult: String,
    val speechText: String
)

/**
 * Convertidor de unidades de medida (longitud, peso, temperatura y volumen) 100% offline.
 */
object UnitConverter {

    private val format2Dec = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("es", "MX")))

    private enum class UnitCategory { LENGTH, MASS, TEMPERATURE, VOLUME }

    private data class UnitDef(
        val name: String,
        val symbol: String,
        val category: UnitCategory,
        val toBaseRatio: Double, // Multiplicador para convertir a unidad base
        val aliases: List<String>
    )

    private val UNITS = listOf(
        // Longitud (base: metro)
        UnitDef("metros", "m", UnitCategory.LENGTH, 1.0, listOf("metro", "metros", "m")),
        UnitDef("centímetros", "cm", UnitCategory.LENGTH, 0.01, listOf("centimetro", "centimetros", "centímetro", "centímetros", "cm")),
        UnitDef("milímetros", "mm", UnitCategory.LENGTH, 0.001, listOf("milimetro", "milimetros", "milímetro", "milímetros", "mm")),
        UnitDef("kilómetros", "km", UnitCategory.LENGTH, 1000.0, listOf("kilometro", "kilometros", "kilómetro", "kilómetros", "km")),
        UnitDef("pulgadas", "in", UnitCategory.LENGTH, 0.0254, listOf("pulgada", "pulgadas", "in", "\"")),
        UnitDef("pies", "ft", UnitCategory.LENGTH, 0.3048, listOf("pie", "pies", "ft")),
        UnitDef("yardas", "yd", UnitCategory.LENGTH, 0.9144, listOf("yarda", "yardas", "yd")),
        UnitDef("millas", "mi", UnitCategory.LENGTH, 1609.344, listOf("milla", "millas", "mi")),

        // Masa (base: kilogramo)
        UnitDef("kilogramos", "kg", UnitCategory.MASS, 1.0, listOf("kilogramo", "kilogramos", "kilo", "kilos", "kg")),
        UnitDef("gramos", "g", UnitCategory.MASS, 0.001, listOf("gramo", "gramos", "g")),
        UnitDef("miligramos", "mg", UnitCategory.MASS, 0.000001, listOf("miligramo", "miligramos", "mg")),
        UnitDef("libras", "lb", UnitCategory.MASS, 0.45359237, listOf("libra", "libras", "lb", "lbs")),
        UnitDef("onzas", "oz", UnitCategory.MASS, 0.0283495, listOf("onza", "onzas", "oz")),
        UnitDef("toneladas", "t", UnitCategory.MASS, 1000.0, listOf("tonelada", "toneladas", "t")),

        // Volumen (base: litro)
        UnitDef("litros", "l", UnitCategory.VOLUME, 1.0, listOf("litro", "litros", "l")),
        UnitDef("mililitros", "ml", UnitCategory.VOLUME, 0.001, listOf("mililitro", "mililitros", "ml")),
        UnitDef("galones", "gal", UnitCategory.VOLUME, 3.78541, listOf("galon", "galones", "galón", "gal")),
        UnitDef("tazas", "cup", UnitCategory.VOLUME, 0.24, listOf("taza", "tazas", "cup")),

        // Temperatura (conversión especial)
        UnitDef("celsius", "°C", UnitCategory.TEMPERATURE, 1.0, listOf("celsius", "centigrado", "centigrados", "centígrado", "centígrados", "grados celsius", "grados")),
        UnitDef("fahrenheit", "°F", UnitCategory.TEMPERATURE, 1.0, listOf("fahrenheit", "grados fahrenheit")),
        UnitDef("kelvin", "K", UnitCategory.TEMPERATURE, 1.0, listOf("kelvin", "grados kelvin"))
    )

    /**
     * Intenta interpretar una consulta de conversión de unidades.
     * Ejemplos:
     * - "convierte 5 pulgadas a centímetros"
     * - "¿cuántos kilómetros son 10 millas?"
     * - "30 grados celsius a fahrenheit"
     * - "¿cuántas libras son 70 kilos?"
     */
    fun parseAndConvert(input: String): UnitConversionResult? {
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Patrón de consulta inversa: "¿cuántos kilómetros son 5 millas?"
        val regexReverse = """(?:cuanto|cuánto|cuantos|cuántos|cuantas|cuántas)\s+([\p{L}°\s]+?)\s+(?:son|hay\s+en|equivalen\s+a)\s+([0-9]+(?:\.[0-9]+)?|[\p{L}]+)\s+([\p{L}°\s]+)""".toRegex()
        val matchReverse = regexReverse.find(lower)
        if (matchReverse != null) {
            val rawTo = matchReverse.groupValues[1].trim()
            val rawAmount = matchReverse.groupValues[2].trim()
            val rawFrom = matchReverse.groupValues[3].trim()

            val amount = rawAmount.toDoubleOrNull()
                ?: SpanishNumberParser.parseNumber(rawAmount)?.toDouble()
                ?: return null

            val fromUnit = findUnit(rawFrom) ?: return null
            val toUnit = findUnit(rawTo) ?: return null

            if (fromUnit.category != toUnit.category) return null
            return convert(amount, fromUnit, toUnit)
        }

        // 2. Patrón directo: "[convierte]? [monto] [unidad1] [a|en] [unidad2]"
        val regexDirect = """(?:convierte|cuanto son|cuánto son|cuantos|cuántos|cuantas|cuántas|a cuántos|a cuantas)?\s*([0-9]+(?:\.[0-9]+)?|[\p{L}]+)\s+([\p{L}°\s]+?)\s+(?:a|en|son\s+en|equivalen\s+a)\s+([\p{L}°\s]+)""".toRegex()
        val matchDirect = regexDirect.find(lower) ?: return null

        val rawAmount = matchDirect.groupValues[1].trim()
        val rawFrom = matchDirect.groupValues[2].trim()
        val rawTo = matchDirect.groupValues[3].trim()

        val amount = rawAmount.toDoubleOrNull()
            ?: SpanishNumberParser.parseNumber(rawAmount)?.toDouble()
            ?: return null

        val fromUnit = findUnit(rawFrom) ?: return null
        val toUnit = findUnit(rawTo) ?: return null

        if (fromUnit.category != toUnit.category) {
            return null // No se pueden convertir unidades de categorías incompatibles (ej. metros a kilos)
        }

        return convert(amount, fromUnit, toUnit)
    }

    private fun convert(amount: Double, from: UnitDef, to: UnitDef): UnitConversionResult {
        val result = if (from.category == UnitCategory.TEMPERATURE) {
            convertTemperature(amount, from.symbol, to.symbol)
        } else {
            // Convertir a base, luego a destino
            val inBase = amount * from.toBaseRatio
            inBase / to.toBaseRatio
        }

        val formatted = "${format2Dec.format(result)} ${to.symbol}"
        val speech = "${format2Dec.format(amount)} ${from.name} equivalen a $formatted."

        return UnitConversionResult(
            amount = amount,
            fromUnitName = from.name,
            fromSymbol = from.symbol,
            toUnitName = to.name,
            toSymbol = to.symbol,
            resultAmount = result,
            formattedResult = formatted,
            speechText = speech
        )
    }

    private fun convertTemperature(value: Double, fromSymbol: String, toSymbol: String): Double {
        if (fromSymbol == toSymbol) return value

        // 1. Convertir a Celsius primero
        val celsius = when (fromSymbol) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> value
        }

        // 2. Convertir de Celsius a destino
        return when (toSymbol) {
            "°C" -> celsius
            "°F" -> (celsius * 9.0 / 5.0) + 32.0
            "K" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun findUnit(text: String): UnitDef? {
        val clean = text.trim().lowercase(Locale.ROOT)
        // 1. Coincidencia exacta con algún alias
        for (u in UNITS) {
            if (u.aliases.any { it.equals(clean, ignoreCase = true) }) {
                return u
            }
        }
        // 2. Palabras compuestas o búsqueda por token
        val words = clean.split(Regex("\\s+"))
        for (u in UNITS) {
            for (alias in u.aliases) {
                if (alias.length > 1) {
                    if (clean == alias || words.contains(alias)) {
                        return u
                    }
                } else if (words.contains(alias)) {
                    return u
                }
            }
        }
        return null
    }
}
