package com.asistente.celular.nlu.expenses

import kotlinx.serialization.Serializable

/**
 * Categorías estándar para clasificación de gastos personales.
 */
@Serializable
enum class ExpenseCategory(val displayName: String) {
    FOOD("Comida"),
    TRANSPORT("Transporte"),
    SERVICES("Servicios"),
    HOME("Hogar"),
    ENTERTAINMENT("Entretenimiento"),
    HEALTH("Salud"),
    SHOPPING("Compras"),
    EDUCATION("Educación"),
    OTHER("Otros")
}

/**
 * Modelo inmutable de un registro de gasto financiero.
 */
@Serializable
data class ExpenseItem(
    val id: String,
    val amount: Double,
    val currency: String = "MXN",
    val category: ExpenseCategory,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resumen consolidado mensual de gastos.
 */
@Serializable
data class ExpenseMonthlySummary(
    val yearMonth: String, // Formato "YYYY-MM"
    val totalAmount: Double,
    val currency: String,
    val byCategory: Map<ExpenseCategory, Double>,
    val count: Int
)

/**
 * Contrato desacoplado para repositorio de gastos (OCP).
 */
interface ExpenseRepository {
    suspend fun addExpense(expense: ExpenseItem): Boolean
    suspend fun getExpenses(limit: Int = 50): List<ExpenseItem>
    suspend fun getMonthlySummary(year: Int, month: Int): ExpenseMonthlySummary
    suspend fun deleteExpense(id: String): Boolean
}
