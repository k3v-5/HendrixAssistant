package com.asistente.celular.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

sealed interface ParsedNotification {
    data class OtpCode(
        val code: String,
        val serviceOrSender: String,
        val originalText: String
    ) : ParsedNotification

    data class BankAlert(
        val amount: Double,
        val currency: String,
        val merchant: String?,
        val transactionType: String,
        val originalText: String
    ) : ParsedNotification

    data class Messaging(
        val sender: String,
        val message: String,
        val packageName: String
    ) : ParsedNotification

    data class Other(
        val title: String,
        val text: String,
        val packageName: String
    ) : ParsedNotification
}

/**
 * Motor de reglas inteligentes para parsear notificaciones multiapp en tiempo real.
 * Extrae automáticamente códigos 2FA OTP al portapapeles y reconoce movimientos bancarios.
 */
object NotificationRuleEngine {

    private const val TAG = "NotificationRuleEngine"

    // Regex para montos de compras bancarias
    private val BANK_KEYWORDS = listOf("compra", "cargo", "transferencia", "retiro", "deposito", "spei", "tarjeta", "banco", "bbva", "santander", "citibanamex", "mercado pago", "nu")
    private val AMOUNT_PATTERN = Regex("""(?:\$|MXN|USD|EUR)\s?([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{2})?)""")

    fun parse(packageName: String, title: String, text: String): ParsedNotification {
        val combined = "$title $text"
        val lower = combined.lowercase()

        // 1. Detección de Código 2FA OTP
        val hasOtpKeyword = lower.contains("codigo") || lower.contains("código") ||
                lower.contains("clave") || lower.contains("otp") || lower.contains("pin") ||
                lower.contains("verificacion") || lower.contains("verificación") ||
                lower.contains("seguridad") || lower.contains("acceso") || lower.contains("password")

        if (hasOtpKeyword) {
            val digitsMatch = Regex("""\b([0-9]{4,8})\b""").find(combined)
            if (digitsMatch != null) {
                val code = digitsMatch.groupValues[1]
                runCatching { Log.i(TAG, "Código 2FA detectado: $code procedente de '$title'") }
                return ParsedNotification.OtpCode(
                    code = code,
                    serviceOrSender = title,
                    originalText = text
                )
            }
        }

        // 2. Detección de Alertas Bancarias / Financieras
        val isBankAlert = BANK_KEYWORDS.any { lower.contains(it) }
        if (isBankAlert) {
            val amountMatch = AMOUNT_PATTERN.find(combined)
            if (amountMatch != null) {
                val rawAmount = amountMatch.groupValues[1].replace(",", "")
                val amount = rawAmount.toDoubleOrNull() ?: 0.0
                val type = when {
                    lower.contains("transferencia") || lower.contains("spei") -> "Transferencia"
                    lower.contains("retiro") -> "Retiro"
                    lower.contains("deposito") || lower.contains("abono") -> "Depósito"
                    else -> "Compra / Cargo"
                }

                // Heurística para detectar comercio (ej: "en OXXO CENTRO con...")
                val merchantMatch = Regex("""(?i)\ben\s+([A-Za-z0-9_]+(?:\s+[A-Za-z0-9_]+)?)(?:\s+(?:con|por|el|la|terminaci[oó]n|\.)|$)""").find(combined)
                val merchant = merchantMatch?.groupValues?.get(1)?.trim()

                return ParsedNotification.BankAlert(
                    amount = amount,
                    currency = if (lower.contains("usd") || lower.contains("dolar")) "USD" else "MXN",
                    merchant = merchant,
                    transactionType = type,
                    originalText = text
                )
            }
        }

        // 3. Notificación de mensajería (WhatsApp, Telegram, SMS, Messenger)
        if (packageName.contains("whatsapp") || packageName.contains("telegram") ||
            packageName.contains("messaging") || packageName.contains("mms") || packageName.contains("orca")) {
            return ParsedNotification.Messaging(
                sender = title,
                message = text,
                packageName = packageName
            )
        }

        return ParsedNotification.Other(
            title = title,
            text = text,
            packageName = packageName
        )
    }

    /**
     * Copia un código 2FA al portapapeles del sistema para pegado inmediato.
     */
    fun copyToClipboard(context: Context, code: String, label: String = "Código 2FA"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, code)
            clipboard.setPrimaryClip(clip)
            Log.i(TAG, "Código OTP copiado al portapapeles con éxito.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando código al portapapeles", e)
            false
        }
    }
}
