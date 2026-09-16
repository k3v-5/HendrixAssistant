package com.asistente.celular.skills.expenses

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.expenses.ExpenseCategory
import com.asistente.celular.nlu.expenses.ExpenseItem
import com.asistente.celular.nlu.expenses.ExpenseRepository
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.ExpenseReportUiPayload
import java.util.Calendar
import java.util.UUID

/**
 * Habilidad de gestión rápida de finanzas y gastos personales por voz.
 * Registra compras al instante, categoriza y provee resúmenes mensuales con tarjetas interactivas.
 */
class ExpenseSkill(
    private val expenseRepository: ExpenseRepository? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "expense_skill",
        name = "Gestor de Gastos",
        description = "Registra y consulta tus gastos personales por voz de forma instantánea."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. "anota gasto de 100 pesos en comida", "registra gasto"
        SequenceConstruct(
            WordConstruct("anota", "anotame", "registra", "registrame", "guarda", "guardame", "agrega"),
            OptionalConstruct(WordConstruct("un", "el")),
            WordConstruct("gasto", "compra", "pago")
        ),
        // 2. "gaste 150 pesos en comida"
        SequenceConstruct(
            WordConstruct("gaste", "pague", "compre")
        ),
        // 3. "cuanto he gastado este mes", "resumen de gastos"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("cual", "dame", "ver")),
            WordConstruct("cuanto", "resumen", "total"),
            OptionalConstruct(WordConstruct("he", "de")),
            WordConstruct("gastado", "gastos")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("cuanto gaste") || lower.contains("cuanto he gastado") ||
            lower.contains("resumen de gastos") || lower.contains("total de gastos")) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "query")
            )
        }

        if (lower.contains("gaste") || lower.contains("gasto") || lower.contains("registra gasto") || lower.contains("anota gasto")) {
            // Intentar extraer monto
            val amountRegex = Regex("([0-9]+(?:\\.[0-9]+)?)")
            val amountMatch = amountRegex.find(lower)
            if (amountMatch != null) {
                val amount = amountMatch.value
                val category = detectCategory(lower)
                val currency = when {
                    lower.contains("dolar") || lower.contains("usd") -> "USD"
                    lower.contains("euro") || lower.contains("eur") -> "EUR"
                    else -> "MXN"
                }

                return SkillScore(
                    confidence = 0.95f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf(
                        "action" to "add",
                        "amount" to amount,
                        "currency" to currency,
                        "category" to category.name,
                        "note" to input.trim()
                    )
                )
            }
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val repo = expenseRepository
        if (repo == null) {
            val msg = "El repositorio de gastos no está disponible."
            return SkillOutput(speech = msg, displayText = "💰 $msg", success = false)
        }

        val action = score.capturedSlots["action"] ?: "query"
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        return if (action == "add") {
            val amount = score.capturedSlots["amount"]?.toDoubleOrNull() ?: 0.0
            val currency = score.capturedSlots["currency"] ?: "MXN"
            val categoryName = score.capturedSlots["category"] ?: ExpenseCategory.OTHER.name
            val category = runCatching { ExpenseCategory.valueOf(categoryName) }.getOrDefault(ExpenseCategory.OTHER)
            val note = score.capturedSlots["note"] ?: input

            val item = ExpenseItem(
                id = UUID.randomUUID().toString(),
                amount = amount,
                currency = currency,
                category = category,
                note = note
            )

            repo.addExpense(item)
            val summary = repo.getMonthlySummary(year, month)

            val speech = "Anoté tu gasto de $${"%.2f".format(amount)} $currency en ${category.displayName}. Total del mes: $${"%.2f".format(summary.totalAmount)} $currency."
            val display = "💰 **Gasto Registrado:**\n• Monto: $${"%.2f".format(amount)} $currency\n• Categoría: ${category.displayName}\n• Total del Mes: $${"%.2f".format(summary.totalAmount)} $currency"

            val categoryMap = summary.byCategory.mapKeys { it.key.displayName }
            val payload = ExpenseReportUiPayload(
                totalAmount = summary.totalAmount,
                currency = currency,
                recentExpenses = repo.getExpenses(5),
                categoryTotals = categoryMap
            )

            SkillOutput(speech = speech, displayText = display, success = true, payload = payload)
        } else {
            val summary = repo.getMonthlySummary(year, month)
            val recent = repo.getExpenses(5)
            val speech = "Este mes llevas gastado un total de $${"%.2f".format(summary.totalAmount)} ${summary.currency} en ${summary.count} registros."
            val display = "📊 **Resumen Mensual de Gastos:**\n• Total: $${"%.2f".format(summary.totalAmount)} ${summary.currency}\n• Operaciones: ${summary.count}"

            val categoryMap = summary.byCategory.mapKeys { it.key.displayName }
            val payload = ExpenseReportUiPayload(
                totalAmount = summary.totalAmount,
                currency = summary.currency,
                recentExpenses = recent,
                categoryTotals = categoryMap
            )

            SkillOutput(speech = speech, displayText = display, success = true, payload = payload)
        }
    }

    private fun detectCategory(text: String): ExpenseCategory {
        return when {
            text.contains("comida") || text.contains("tacos") || text.contains("cena") ||
            text.contains("almuerzo") || text.contains("cafe") || text.contains("restaurante") ||
            text.contains("pizza") || text.contains("hamburguesa") || text.contains("super") -> ExpenseCategory.FOOD

            text.contains("uber") || text.contains("didi") || text.contains("taxi") ||
            text.contains("gasolina") || text.contains("gas") || text.contains("pasaje") ||
            text.contains("camion") || text.contains("metro") || text.contains("transporte") -> ExpenseCategory.TRANSPORT

            text.contains("luz") || text.contains("agua") || text.contains("internet") ||
            text.contains("telefono") || text.contains("gas") || text.contains("servicio") -> ExpenseCategory.SERVICES

            text.contains("farmacia") || text.contains("medico") || text.contains("medicina") ||
            text.contains("consulta") || text.contains("hospital") -> ExpenseCategory.HEALTH

            text.contains("cine") || text.contains("bar") || text.contains("cerveza") ||
            text.contains("juego") || text.contains("fiesta") -> ExpenseCategory.ENTERTAINMENT

            text.contains("ropa") || text.contains("zapatos") || text.contains("amazon") ||
            text.contains("tienda") || text.contains("compra") -> ExpenseCategory.SHOPPING

            else -> ExpenseCategory.OTHER
        }
    }
}
