package com.asistente.celular.data

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.expenses.ExpenseCategory
import com.asistente.celular.nlu.expenses.ExpenseItem
import com.asistente.celular.nlu.expenses.ExpenseMonthlySummary
import com.asistente.celular.nlu.expenses.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar

/**
 * Repositorio persistente de finanzas y gastos en formato JSON local.
 * Sigue el principio de desacoplamiento y no requiere conexión a internet.
 */
class JsonExpenseRepository(
    private val context: Context,
    private val fileName: String = "hendrix_expenses.json"
) : ExpenseRepository {

    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val file: File
        get() = File(context.filesDir, fileName)

    override suspend fun addExpense(expense: ExpenseItem): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadListInternal().toMutableList()
            current.add(0, expense)
            saveListInternal(current)
        }
    }

    override suspend fun getExpenses(limit: Int): List<ExpenseItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadListInternal().take(limit)
        }
    }

    override suspend fun getMonthlySummary(year: Int, month: Int): ExpenseMonthlySummary = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = loadListInternal()
            val cal = Calendar.getInstance()

            val filtered = all.filter { item ->
                cal.timeInMillis = item.timestamp
                val itemYear = cal.get(Calendar.YEAR)
                val itemMonth = cal.get(Calendar.MONTH) + 1
                itemYear == year && itemMonth == month
            }

            val total = filtered.sumOf { it.amount }
            val byCat = filtered.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val monthStr = if (month < 10) "0$month" else "$month"
            val currency = filtered.firstOrNull()?.currency ?: "MXN"

            ExpenseMonthlySummary(
                yearMonth = "$year-$monthStr",
                totalAmount = total,
                currency = currency,
                byCategory = byCat,
                count = filtered.size
            )
        }
    }

    override suspend fun deleteExpense(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadListInternal().toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                saveListInternal(current)
            }
            removed
        }
    }

    private fun loadListInternal(): List<ExpenseItem> {
        return try {
            if (!file.exists()) return emptyList()
            val content = file.readText()
            if (content.isBlank()) emptyList() else json.decodeFromString(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo gastos desde archivo JSON", e)
            emptyList()
        }
    }

    private fun saveListInternal(list: List<ExpenseItem>): Boolean {
        return try {
            val content = json.encodeToString(list)
            file.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando gastos en archivo JSON", e)
            false
        }
    }

    companion object {
        private const val TAG = "JsonExpenseRepository"
    }
}
