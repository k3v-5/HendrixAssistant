package com.asistente.celular.biometrics

import android.content.Context
import com.asistente.celular.voice.biometrics.BiometricMatchResult
import com.asistente.celular.voice.biometrics.VoiceBiometricsEngine
import com.asistente.celular.voice.biometrics.VoiceprintProfile
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Implementación de biometría vocal offline basada en características espectro-acústicas locales.
 * Permite enrolar al dueño del dispositivo y verificar quién habla con cero telemetría externa.
 */
class LocalVoiceBiometricsEngine(
    private val context: Context
) : VoiceBiometricsEngine {

    private val profiles = ConcurrentHashMap<String, VoiceprintProfile>()

    init {
        // Inicializar con un perfil de demostración del propietario registrado
        val defaultOwnerVector = FloatArray(16) { 0.45f + (it * 0.03f) }
        profiles["owner_default"] = VoiceprintProfile(
            speakerId = "owner_default",
            speakerName = "Propietario",
            acousticCentroid = defaultOwnerVector,
            energyThreshold = 0.04f,
            sampleCount = 3
        )
    }

    override fun extractVoiceprint(pcmData: ShortArray): FloatArray {
        if (pcmData.isEmpty()) return FloatArray(16) { 0f }
        val vector = FloatArray(16)
        val chunkSize = (pcmData.size / 16).coerceAtLeast(1)

        for (i in 0 until 16) {
            var sum = 0.0
            var zcr = 0
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(pcmData.size)
            for (j in start until end) {
                val sample = pcmData[j].toDouble() / Short.MAX_VALUE
                sum += abs(sample)
                if (j > start && (pcmData[j] >= 0 != pcmData[j - 1] >= 0)) {
                    zcr++
                }
            }
            val energy = if (end > start) sum / (end - start) else 0.0
            vector[i] = (energy * 0.8 + (zcr.toDouble() / chunkSize.coerceAtLeast(1)) * 0.2).toFloat()
        }
        return vector
    }

    override suspend fun enrollSpeaker(
        speakerId: String,
        speakerName: String,
        pcmSamples: List<ShortArray>
    ): VoiceprintProfile {
        val aggregated = FloatArray(16)
        var count = 0
        for (sample in pcmSamples) {
            val vec = extractVoiceprint(sample)
            for (i in 0 until 16) {
                aggregated[i] += vec[i]
            }
            count++
        }
        val factor = if (count > 0) 1f / count else 1f
        for (i in 0 until 16) {
            aggregated[i] *= factor
        }

        val profile = VoiceprintProfile(
            speakerId = speakerId,
            speakerName = speakerName,
            acousticCentroid = aggregated,
            sampleCount = count.coerceAtLeast(1)
        )
        profiles[speakerId] = profile
        return profile
    }

    override suspend fun verifySpeaker(
        pcmData: ShortArray,
        targetSpeakerId: String?
    ): BiometricMatchResult {
        if (pcmData.isEmpty()) {
            return BiometricMatchResult(
                isMatch = false,
                confidence = 0f,
                identifiedSpeaker = null,
                details = "Muestra de audio vacía."
            )
        }

        val inputVec = extractVoiceprint(pcmData)
        var bestSpeaker: String? = null
        var highestScore = -1f

        val targetProfiles = if (targetSpeakerId != null) {
            listOfNotNull(profiles[targetSpeakerId])
        } else {
            profiles.values.toList()
        }

        for (profile in targetProfiles) {
            val similarity = computeCosineSimilarity(inputVec, profile.acousticCentroid)
            if (similarity > highestScore) {
                highestScore = similarity
                bestSpeaker = profile.speakerName
            }
        }

        val isMatch = highestScore >= 0.70f
        return BiometricMatchResult(
            isMatch = isMatch,
            confidence = highestScore.coerceIn(0f, 1f),
            identifiedSpeaker = if (isMatch) bestSpeaker else null,
            details = if (isMatch) "Huella vocal coincidente con $bestSpeaker (${(highestScore * 100).toInt()}%)"
                      else "Voz no reconocida o confianza insuficiente (${(highestScore * 100).toInt()}%)"
        )
    }

    override fun getRegisteredProfiles(): List<VoiceprintProfile> {
        return profiles.values.toList()
    }

    override fun deleteProfile(speakerId: String): Boolean {
        return profiles.remove(speakerId) != null
    }

    private fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }
}
