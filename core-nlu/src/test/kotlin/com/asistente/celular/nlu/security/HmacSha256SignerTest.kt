package com.asistente.celular.nlu.security

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HmacSha256SignerTest {

    private val secretKey = "test_super_secret_key_hendrix_12345"

    @Test
    fun `sign should add valid timestamp nonce and signature`() {
        var currentTime = 1000000L
        val signer = HmacSha256Signer(timeProvider = { currentTime })

        val payload = JSONObject().apply {
            put("type", "INPUT_ACTION")
            put("actionType", "CLICK")
            put("xRatio", 0.5)
            put("yRatio", 0.8)
        }

        val signed = signer.sign(payload, secretKey)

        assertTrue(signed.has(HmacSha256Signer.FIELD_TIMESTAMP))
        assertTrue(signed.has(HmacSha256Signer.FIELD_NONCE))
        assertTrue(signed.has(HmacSha256Signer.FIELD_SIGNATURE))
        assertEquals(currentTime, signed.getLong(HmacSha256Signer.FIELD_TIMESTAMP))

        val result = signer.verify(signed, secretKey)
        assertEquals(SignatureVerificationResult.Valid, result)
    }

    @Test
    fun `verify should detect tampered payload content`() {
        val currentTime = 1000000L
        val signer = HmacSha256Signer(timeProvider = { currentTime })

        val payload = JSONObject().apply {
            put("type", "QUICK_COMMAND")
            put("command", "LOCK_WORKSTATION")
        }

        val signed = signer.sign(payload, secretKey)

        // Alterar el contenido después de ser firmado
        signed.put("command", "SHUTDOWN_FORCE")

        val result = signer.verify(signed, secretKey)
        assertTrue(result is SignatureVerificationResult.InvalidSignature)
    }

    @Test
    fun `verify should reject expired timestamp`() {
        var currentTime = 1000000L
        val signer = HmacSha256Signer(timeProvider = { currentTime })

        val payload = JSONObject().apply {
            put("type", "TELEMETRY_REQUEST")
        }

        val signed = signer.sign(payload, secretKey)

        // Simular que han transcurrido 10 segundos (> tolerancia de 5s)
        currentTime += 10000L

        val result = signer.verify(signed, secretKey, toleranceMs = 5000L)
        assertTrue(result is SignatureVerificationResult.Expired)
        val expired = result as SignatureVerificationResult.Expired
        assertEquals(10000L, expired.ageMs)
    }

    @Test
    fun `verify should reject replayed nonce`() {
        val currentTime = 1000000L
        val signer = HmacSha256Signer(timeProvider = { currentTime })

        val payload = JSONObject().apply {
            put("type", "INPUT_ACTION")
            put("actionType", "KEY_PRESS")
            put("keyCode", 13)
        }

        val signed = signer.sign(payload, secretKey)

        // Primera verificación exitosa
        val firstResult = signer.verify(signed, secretKey)
        assertEquals(SignatureVerificationResult.Valid, firstResult)

        // Segundo intento de verificación con exactamente el mismo paquete (Replay Attack)
        val secondResult = signer.verify(signed, secretKey)
        assertTrue(secondResult is SignatureVerificationResult.Replayed)
    }

    @Test
    fun `verify should reject signature created with different secret key`() {
        val currentTime = 1000000L
        val signer = HmacSha256Signer(timeProvider = { currentTime })

        val payload = JSONObject().apply {
            put("type", "KILL_PROCESS")
            put("pid", 1234)
        }

        val signed = signer.sign(payload, "key_a")
        val result = signer.verify(signed, "key_b")

        assertTrue(result is SignatureVerificationResult.InvalidSignature)
    }

    @Test
    fun `verify should fail if metadata is missing`() {
        val signer = HmacSha256Signer()
        val emptyPayload = JSONObject().apply {
            put("type", "TEST")
        }

        val result = signer.verify(emptyPayload, secretKey)
        assertTrue(result is SignatureVerificationResult.MissingMetadata)
    }

    @Test
    fun `verify should validate Python generated signature vector`() {
        val simulatedTime = 1700000000000L
        val signer = HmacSha256Signer(timeProvider = { simulatedTime })

        val pythonPayload = JSONObject().apply {
            put("type", "QUICK_COMMAND")
            put("command", "LOCK_WORKSTATION")
            put(HmacSha256Signer.FIELD_TIMESTAMP, 1700000000000L)
            put(HmacSha256Signer.FIELD_NONCE, "d8fc4c70-ceab-4d43-ac13-4788ee897c4b")
            put(HmacSha256Signer.FIELD_SIGNATURE, "c56ddb6a6ca538b0277d6cedf599a3ee7e71a1a860279ab82ccb09dda27efb1c")
        }

        val result = signer.verify(pythonPayload, "test_key")
        assertEquals(SignatureVerificationResult.Valid, result)
    }
}
