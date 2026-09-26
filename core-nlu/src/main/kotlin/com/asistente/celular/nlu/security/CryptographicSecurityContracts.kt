package com.asistente.celular.nlu.security

import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Contrato para el firmado criptográfico y verificación anti-repetición (Anti-Replay)
 * de paquetes e instrucciones remotas entre Android y el servidor Hendrix Desktop.
 */
interface RemoteRequestSigner {
    /**
     * Firma criptográficamente un JSONObject inyectándole los campos de seguridad:
     * - `_ts`: Timestamp Unix epoch en milisegundos.
     * - `_nonce`: Identificador único aleatorio para prevención de repetición.
     * - `_sig`: Firma HMAC-SHA256 calculada sobre la forma canónica del paquete.
     */
    fun sign(payload: JSONObject, secretKey: String): JSONObject

    /**
     * Verifica la autenticidad, frescura e integridad de un payload firmado.
     */
    fun verify(
        payload: JSONObject,
        secretKey: String,
        toleranceMs: Long = 5000L
    ): SignatureVerificationResult
}

sealed interface SignatureVerificationResult {
    data object Valid : SignatureVerificationResult
    data class Expired(val ageMs: Long, val toleranceMs: Long) : SignatureVerificationResult
    data class Replayed(val nonce: String) : SignatureVerificationResult
    data class InvalidSignature(val reason: String) : SignatureVerificationResult
    data class MissingMetadata(val missingField: String) : SignatureVerificationResult
}

/**
 * Implementación de grado industrial de firmado HMAC-SHA256 con protección Anti-Replay por nonces
 * y canonización determinista compatible entre Kotlin y Python.
 */
class HmacSha256Signer(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : RemoteRequestSigner {

    // Caché en memoria de nonces observados con su timestamp de recepción para evitar replay attacks
    private val seenNonces = ConcurrentHashMap<String, Long>()

    override fun sign(payload: JSONObject, secretKey: String): JSONObject {
        require(secretKey.isNotBlank()) { "Secret key no puede estar vacía" }

        val ts = timeProvider()
        val nonce = UUID.randomUUID().toString()

        payload.put(FIELD_TIMESTAMP, ts)
        payload.put(FIELD_NONCE, nonce)

        val canonicalData = buildCanonicalString(payload, ts, nonce)
        val signature = computeHmacSha256Hex(canonicalData, secretKey)
        payload.put(FIELD_SIGNATURE, signature)

        return payload
    }

    override fun verify(
        payload: JSONObject,
        secretKey: String,
        toleranceMs: Long
    ): SignatureVerificationResult {
        if (!payload.has(FIELD_TIMESTAMP)) {
            return SignatureVerificationResult.MissingMetadata(FIELD_TIMESTAMP)
        }
        if (!payload.has(FIELD_NONCE)) {
            return SignatureVerificationResult.MissingMetadata(FIELD_NONCE)
        }
        if (!payload.has(FIELD_SIGNATURE)) {
            return SignatureVerificationResult.MissingMetadata(FIELD_SIGNATURE)
        }

        val ts = payload.optLong(FIELD_TIMESTAMP, 0L)
        val nonce = payload.optString(FIELD_NONCE, "")
        val signature = payload.optString(FIELD_SIGNATURE, "")

        if (nonce.isBlank()) {
            return SignatureVerificationResult.MissingMetadata(FIELD_NONCE)
        }
        if (signature.isBlank()) {
            return SignatureVerificationResult.MissingMetadata(FIELD_SIGNATURE)
        }

        val now = timeProvider()
        val ageMs = Math.abs(now - ts)
        if (ageMs > toleranceMs) {
            return SignatureVerificationResult.Expired(ageMs = ageMs, toleranceMs = toleranceMs)
        }

        // Poda periódica de nonces expirados de la caché
        purgeExpiredNonces(now, toleranceMs)

        // Verificación de repetición (Replay Attack)
        if (seenNonces.putIfAbsent(nonce, now) != null) {
            return SignatureVerificationResult.Replayed(nonce)
        }

        // Recomputar firma esperada
        val canonicalData = buildCanonicalString(payload, ts, nonce)
        val expectedSignature = computeHmacSha256Hex(canonicalData, secretKey)

        return if (MessageDigest.isEqual(expectedSignature.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))) {
            SignatureVerificationResult.Valid
        } else {
            SignatureVerificationResult.InvalidSignature("Firma no coincide con el digest canónico")
        }
    }

    private fun purgeExpiredNonces(now: Long, toleranceMs: Long) {
        val cutoff = now - (toleranceMs * 2)
        val iterator = seenNonces.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < cutoff) {
                iterator.remove()
            }
        }
    }

    companion object {
        const val FIELD_TIMESTAMP = "_ts"
        const val FIELD_NONCE = "_nonce"
        const val FIELD_SIGNATURE = "_sig"

        /**
         * Construye la cadena canónica determinista:
         * format: type|nonce|timestamp|sha256Hex(sorted_keys_json)
         */
        fun buildCanonicalString(payload: JSONObject, ts: Long, nonce: String): String {
            val type = payload.optString("type", "")

            // Extraer y ordenar alfabéticamente todas las claves excluyendo metadatos de firma
            val sortedKeys = payload.keys().asSequence()
                .filter { it != FIELD_TIMESTAMP && it != FIELD_NONCE && it != FIELD_SIGNATURE }
                .sorted()
                .toList()

            val sb = StringBuilder()
            for (key in sortedKeys) {
                val value = payload.opt(key)
                val str = when (value) {
                    is Boolean -> value.toString()
                    is Double -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
                    is Float -> if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()
                    null -> ""
                    JSONObject.NULL -> ""
                    else -> value.toString()
                }
                sb.append(key).append("=").append(str).append(";")
            }

            val bodyDigest = computeSha256Hex(sb.toString())
            return "$type|$nonce|$ts|$bodyDigest"
        }

        fun computeSha256Hex(input: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun computeHmacSha256Hex(data: String, key: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
