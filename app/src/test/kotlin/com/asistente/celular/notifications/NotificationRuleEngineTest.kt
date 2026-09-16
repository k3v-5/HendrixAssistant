package com.asistente.celular.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRuleEngineTest {

    @Test
    fun testDetectOtpCodeStandard() {
        val parsed = NotificationRuleEngine.parse(
            packageName = "com.google.android.apps.messaging",
            title = "Google",
            text = "Tu código de verificación de Google es 492015. No lo compartas con nadie."
        )

        assertTrue("Debe detectar como OtpCode", parsed is ParsedNotification.OtpCode)
        val otp = parsed as ParsedNotification.OtpCode
        assertEquals("492015", otp.code)
        assertEquals("Google", otp.serviceOrSender)
    }

    @Test
    fun testDetectOtpCodeAlternativeFormat() {
        val parsed = NotificationRuleEngine.parse(
            packageName = "org.telegram.messenger",
            title = "Telegram",
            text = "Código de inicio de sesión: 83921. Si no lo solicitaste, ignora este mensaje."
        )

        assertTrue(parsed is ParsedNotification.OtpCode)
        val otp = parsed as ParsedNotification.OtpCode
        assertEquals("83921", otp.code)
    }

    @Test
    fun testDetectBankPurchaseAlert() {
        val parsed = NotificationRuleEngine.parse(
            packageName = "com.bbva.bancomer",
            title = "BBVA México",
            text = "Compra por $450.50 en OXXO CENTRO con tu tarjeta terminación 1234."
        )

        assertTrue("Debe detectar alerta bancaria", parsed is ParsedNotification.BankAlert)
        val bank = parsed as ParsedNotification.BankAlert
        assertEquals(450.50, bank.amount, 0.01)
        assertEquals("MXN", bank.currency)
        assertEquals("OXXO CENTRO", bank.merchant)
        assertEquals("Compra / Cargo", bank.transactionType)
    }

    @Test
    fun testDetectBankSpeiTransfer() {
        val parsed = NotificationRuleEngine.parse(
            packageName = "com.santander.app",
            title = "Santander Móvil",
            text = "Transferencia enviada por $1,250.00 SPEI exitosa."
        )

        assertTrue(parsed is ParsedNotification.BankAlert)
        val bank = parsed as ParsedNotification.BankAlert
        assertEquals(1250.00, bank.amount, 0.01)
        assertEquals("Transferencia", bank.transactionType)
    }

    @Test
    fun testDetectMessaging() {
        val parsed = NotificationRuleEngine.parse(
            packageName = "com.whatsapp",
            title = "Mamá",
            text = "¿A qué hora llegas a comer?"
        )

        assertTrue(parsed is ParsedNotification.Messaging)
        val msg = parsed as ParsedNotification.Messaging
        assertEquals("Mamá", msg.sender)
        assertEquals("¿A qué hora llegas a comer?", msg.message)
    }
}
