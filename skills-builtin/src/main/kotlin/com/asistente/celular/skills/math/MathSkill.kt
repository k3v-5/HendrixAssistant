package com.asistente.celular.skills.math

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.math.CurrencyConverter
import com.asistente.celular.nlu.math.MathEvaluator
import com.asistente.celular.nlu.math.UnitConverter
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput

/**
 * Habilidad matemática y de conversión (aritmética, divisas internacionales y unidades métricas/imperiales) 100% offline.
 */
class MathSkill(
    private val currencyConverter: CurrencyConverter = CurrencyConverter()
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "math_skill",
        name = "Calculadora y Conversor",
        description = "Resuelve operaciones matemáticas, porcentajes, conversión de monedas y unidades de medida sin internet."
    )

    override val specificity: Specificity = Specificity.HIGH

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // 1. Intentar pre-evaluar si coincide con divisas
        val currencyCheck = currencyConverter.parseAndConvert(lower)
        if (currencyCheck != null) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("type" to "currency")
            )
        }

        // 2. Intentar pre-evaluar si coincide con conversión de unidades
        val unitCheck = UnitConverter.parseAndConvert(lower)
        if (unitCheck != null) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("type" to "unit")
            )
        }

        // 3. Intentar pre-evaluar si es una expresión matemática o porcentaje
        val mathCheck = MathEvaluator.evaluate(lower)
        if (mathCheck != null) {
            return SkillScore(
                confidence = 0.94f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("type" to "math")
            )
        }

        // Palabras clave directas de cálculo
        val isMathKeyword = lower.startsWith("calcula") || lower.startsWith("cuanto es") ||
                lower.startsWith("cuánto es") || lower.contains("por ciento") || lower.contains("%")
        if (isMathKeyword && lower.any { it.isDigit() }) {
            return SkillScore(
                confidence = 0.90f,
                specificity = Specificity.NORMAL,
                capturedSlots = mapOf("type" to "math")
            )
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)

        // 1. Prioridad: Divisas
        val currencyResult = currencyConverter.parseAndConvert(lower)
        if (currencyResult != null) {
            val display = """
                💱 **Conversión de Divisas:**
                
                **${currencyResult.amount} ${currencyResult.fromName}** ≈ **${currencyResult.formattedResult}**
                *(Tasa ref: 1 ${currencyResult.fromCurrency} ≈ ${currencyResult.rate} ${currencyResult.toCurrency})*
            """.trimIndent()
            val uiPayload = com.asistente.celular.nlu.ui.CurrencyUiPayload(
                amount = currencyResult.amount.stripTrailingZeros().toPlainString(),
                fromCurrency = currencyResult.fromCurrency,
                fromName = currencyResult.fromName,
                toCurrency = currencyResult.toCurrency,
                toName = currencyResult.toName,
                resultAmount = currencyResult.formattedResult,
                rate = currencyResult.rate.stripTrailingZeros().toPlainString()
            )
            return SkillOutput(
                speech = currencyResult.speechText,
                displayText = display,
                success = true,
                payload = uiPayload
            )
        }

        // 2. Prioridad: Unidades
        val unitResult = UnitConverter.parseAndConvert(lower)
        if (unitResult != null) {
            val display = """
                📏 **Conversión de Unidades:**
                
                **${unitResult.amount} ${unitResult.fromUnitName}** = **${unitResult.formattedResult}**
            """.trimIndent()
            return SkillOutput(
                speech = unitResult.speechText,
                displayText = display,
                success = true,
                payload = unitResult
            )
        }

        // 3. Prioridad: Matemáticas / Porcentajes
        val mathResult = MathEvaluator.evaluate(lower)
        if (mathResult != null) {
            val display = """
                🧮 **Cálculo:**
                
                `${mathResult.expression}` = **${mathResult.formattedResult}**
            """.trimIndent()
            return SkillOutput(
                speech = mathResult.speechText,
                displayText = display,
                success = true,
                payload = mathResult
            )
        }

        val fallbackMsg = "No pude resolver el cálculo o la conversión solicitada."
        return SkillOutput(speech = fallbackMsg, displayText = fallbackMsg, success = false)
    }
}
