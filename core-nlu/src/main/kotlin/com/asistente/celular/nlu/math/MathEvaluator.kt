package com.asistente.celular.nlu.math

import com.asistente.celular.nlu.parser.SpanishNumberParser
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Resultado de una evaluación matemática.
 */
data class MathResult(
    val expression: String,
    val value: BigDecimal,
    val formattedResult: String,
    val speechText: String
)

/**
 * Evaluador aritmético 100% offline para resolver cálculos matemáticos en lenguaje natural en español.
 */
object MathEvaluator {

    private val mathContext = MathContext(10, RoundingMode.HALF_UP)
    private val displayFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("es", "MX")))

    /**
     * Intenta evaluar una expresión hablada o escrita en español.
     * Ej: "cuánto es 1500 entre 12", "el 16 por ciento de 850", "45 por 3 más 80".
     */
    fun evaluate(input: String): MathResult? {
        val clean = cleanInput(input)
        if (clean.isBlank()) return null

        // 1. Evaluar porcentaje tipo "el 15% de 800" o "15 por ciento de 800"
        evaluateDirectPercentage(clean)?.let { return it }

        // 2. Evaluar potencia simple tipo "5 al cuadrado", "2 al cubo", "4 elevado a la 3"
        evaluatePower(clean)?.let { return it }

        // 3. Evaluar expresión aritmética general (suma, resta, multiplicación, división)
        return evaluateArithmeticExpression(clean)
    }

    private fun cleanInput(raw: String): String {
        var text = raw.lowercase(Locale.ROOT).trim()

        // Eliminar prefijos de pregunta o petición
        val prefixes = listOf(
            "cuanto es", "cuánto es", "cuanto da", "cuánto da",
            "calcula", "calcular", "dime cuanto es", "dime cuánto es",
            "resuelve", "resolver", "por favor", "porfa"
        )
        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                text = text.removePrefix(prefix).trim()
            }
        }

        // Reemplazar palabras numéricas por dígitos ("cinco" -> "5")
        val words = text.split("\\s+".toRegex())
        val mappedWords = words.map { word ->
            SpanishNumberParser.parseNumber(word)?.toString() ?: word
        }
        text = mappedWords.joinToString(" ")

        // Normalizar operadores hablados
        text = text
            .replace("por ciento", "%")
            .replace("porciento", "%")
            .replace("multiplicado por", "*")
            .replace("dividido entre", "/")
            .replace("dividido por", "/")
            .replace("dividido en", "/")
            .replace("sumado a", "+")
            .replace("restado de", "-")
            .replace("menos", "-")
            .replace("mas", "+")
            .replace("más", "+")
            .replace("entre", "/")
            .replace("sobre", "/")
            .replace(" por ", " * ")
            .replace(" x ", " * ")
            .replace(" punto ", ".")
            .replace(" coma ", ".")

        return text.trim()
    }

    private fun evaluateDirectPercentage(text: String): MathResult? {
        // "15 % de 800" o "el 15 % de 800"
        val regex = """(?:el\s*)?([0-9]+(?:\.[0-9]+)?)\s*%\s*(?:de\s*)?([0-9]+(?:\.[0-9]+)?)""".toRegex()
        val match = regex.find(text) ?: return null

        val percentValue = match.groupValues[1].toBigDecimalOrNull() ?: return null
        val totalValue = match.groupValues[2].toBigDecimalOrNull() ?: return null

        val result = totalValue.multiply(percentValue).divide(BigDecimal(100), mathContext)
        val formatted = formatNumber(result)
        val speech = "El $percentValue por ciento de $totalValue es $formatted."

        return MathResult(
            expression = "$percentValue% de $totalValue",
            value = result,
            formattedResult = formatted,
            speechText = speech
        )
    }

    private fun evaluatePower(text: String): MathResult? {
        // "5 al cuadrado" -> 5^2
        val squareRegex = """([0-9]+(?:\.[0-9]+)?)\s*al\s+cuadrado""".toRegex()
        squareRegex.find(text)?.let { m ->
            val base = m.groupValues[1].toDoubleOrNull() ?: return null
            val res = BigDecimal(base * base, mathContext)
            val formatted = formatNumber(res)
            return MathResult(
                expression = "$base²",
                value = res,
                formattedResult = formatted,
                speechText = "$base al cuadrado es $formatted."
            )
        }

        // "2 al cubo" -> 2^3
        val cubeRegex = """([0-9]+(?:\.[0-9]+)?)\s*al\s+cubo""".toRegex()
        cubeRegex.find(text)?.let { m ->
            val base = m.groupValues[1].toDoubleOrNull() ?: return null
            val res = BigDecimal(base * base * base, mathContext)
            val formatted = formatNumber(res)
            return MathResult(
                expression = "$base³",
                value = res,
                formattedResult = formatted,
                speechText = "$base al cubo es $formatted."
            )
        }

        // "2 elevado a la 4"
        val powRegex = """([0-9]+(?:\.[0-9]+)?)\s*elevado\s+(?:a\s+la\s+|a\s+)?([0-9]+)""".toRegex()
        powRegex.find(text)?.let { m ->
            val base = m.groupValues[1].toDoubleOrNull() ?: return null
            val exponent = m.groupValues[2].toIntOrNull() ?: return null
            if (exponent in 0..100) {
                val res = BigDecimal(Math.pow(base, exponent.toDouble()), mathContext)
                val formatted = formatNumber(res)
                return MathResult(
                    expression = "$base^$exponent",
                    value = res,
                    formattedResult = formatted,
                    speechText = "$base elevado a la $exponent es $formatted."
                )
            }
        }

        return null
    }

    private fun evaluateArithmeticExpression(text: String): MathResult? {
        // Tokenizar números y operadores (+, -, *, /)
        val sanitized = text.replace("[^0-9.+\\-*/]".toRegex(), " ")
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()

        var i = 0
        while (i < sanitized.length) {
            val c = sanitized[i]
            if (c in "+-*/") {
                if (currentToken.isNotBlank()) {
                    tokens.add(currentToken.toString().trim())
                    currentToken.clear()
                }
                tokens.add(c.toString())
            } else if (c.isDigit() || c == '.') {
                currentToken.append(c)
            } else if (c.isWhitespace()) {
                if (currentToken.isNotBlank()) {
                    tokens.add(currentToken.toString().trim())
                    currentToken.clear()
                }
            }
            i++
        }
        if (currentToken.isNotBlank()) {
            tokens.add(currentToken.toString().trim())
        }

        if (tokens.size < 3) return null

        return try {
            // Evaluar con precedencia de operadores (*, / primero, luego +, -)
            val values = mutableListOf<BigDecimal>()
            val operators = mutableListOf<String>()

            var idx = 0
            while (idx < tokens.size) {
                val token = tokens[idx]
                if (token in listOf("+", "-", "*", "/")) {
                    operators.add(token)
                } else {
                    val num = token.toBigDecimalOrNull() ?: return null
                    values.add(num)
                }
                idx++
            }

            if (values.size != operators.size + 1) return null

            // Paso 1: Resolver multiplicación y división
            var opIdx = 0
            while (opIdx < operators.size) {
                val op = operators[opIdx]
                if (op == "*" || op == "/") {
                    val left = values[opIdx]
                    val right = values[opIdx + 1]
                    val result = if (op == "*") {
                        left.multiply(right, mathContext)
                    } else {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            return MathResult(
                                expression = text,
                                value = BigDecimal.ZERO,
                                formattedResult = "Indefinido",
                                speechText = "No se puede dividir entre cero."
                            )
                        }
                        left.divide(right, mathContext)
                    }
                    values[opIdx] = result
                    values.removeAt(opIdx + 1)
                    operators.removeAt(opIdx)
                } else {
                    opIdx++
                }
            }

            // Paso 2: Resolver suma y resta
            var finalResult = values[0]
            for (j in operators.indices) {
                val op = operators[j]
                val nextVal = values[j + 1]
                finalResult = if (op == "+") {
                    finalResult.add(nextVal, mathContext)
                } else {
                    finalResult.subtract(nextVal, mathContext)
                }
            }

            val formatted = formatNumber(finalResult)
            val exprString = tokens.joinToString(" ")
            val speech = "El resultado de $exprString es $formatted."

            MathResult(
                expression = exprString,
                value = finalResult,
                formattedResult = formatted,
                speechText = speech
            )
        } catch (e: Exception) {
            null
        }
    }

    fun formatNumber(number: BigDecimal): String {
        return try {
            val stripped = number.stripTrailingZeros()
            displayFormat.format(stripped)
        } catch (e: Exception) {
            number.toPlainString()
        }
    }
}
