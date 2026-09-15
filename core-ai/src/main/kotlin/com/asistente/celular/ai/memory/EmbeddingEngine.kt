package com.asistente.celular.ai.memory

import java.text.Normalizer
import kotlin.math.sqrt

/**
 * Contrato abstracto para motores de generación de representaciones vectoriales (Embeddings).
 * Permite intercambiar implementaciones locales, híbridas o basadas en modelos ONNX / Gemini.
 */
interface EmbeddingEngine {
    val dimensions: Int

    /**
     * Genera un vector denso normalizado (L2) para el texto proporcionado.
     */
    suspend fun generateEmbedding(text: String): List<Float>

    /**
     * Calcula la similitud del coseno entre dos vectores normalizados.
     * Retorna un valor entre -1.0f y 1.0f (típicamente 0.0f a 1.0f para textos afines).
     */
    fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float
}

/**
 * Motor de embeddings local y determinista basado en Feature Hashing (Hashing Trick),
 * n-gramas de caracteres y bigramas léxicos con normalización L2.
 * Funciona de forma 100% offline, sin latencia de red y con consumo de memoria despreciable.
 */
class FeatureHashingEmbeddingEngine(
    override val dimensions: Int = 128
) : EmbeddingEngine {

    override suspend fun generateEmbedding(text: String): List<Float> {
        val normalized = normalizeText(text)
        if (normalized.isBlank()) {
            return List(dimensions) { 0f }
        }

        val vector = FloatArray(dimensions) { 0f }
        val tokens = normalized.split("\\s+".toRegex()).filter { it.length > 1 }

        // 1. Unigramas de palabras
        for (token in tokens) {
            hashAndAdd(vector, "w:$token", weight = 1.0f)

            // Sub-palabras / n-gramas de caracteres (3 y 4) para capturar raíces léxicas en español
            if (token.length >= 3) {
                for (i in 0..token.length - 3) {
                    val tri = token.substring(i, i + 3)
                    hashAndAdd(vector, "c3:$tri", weight = 0.5f)
                }
            }
            if (token.length >= 4) {
                for (i in 0..token.length - 4) {
                    val quad = token.substring(i, i + 4)
                    hashAndAdd(vector, "c4:$quad", weight = 0.6f)
                }
            }
        }

        // 2. Bigramas de palabras contiguas ("sin azucar", "carro gris", "comida favorita")
        for (i in 0 until tokens.size - 1) {
            val bigram = "${tokens[i]}_${tokens[i + 1]}"
            hashAndAdd(vector, "bg:$bigram", weight = 1.5f)
        }

        // 3. Normalización L2 del vector resultante
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }

        if (sumSquares > 0f) {
            val norm = sqrt(sumSquares)
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector.toList()
    }

    override fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float {
        if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) return 0f

        var dotProduct = 0f
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
        }
        return dotProduct.coerceIn(-1.0f, 1.0f)
    }

    private fun hashAndAdd(vector: FloatArray, feature: String, weight: Float) {
        // Hashing trick: H1 para índice, H2 para signo (desviación media no sesgada)
        val h = feature.hashCode()
        val index = (h and 0x7FFFFFFF) % dimensions
        val sign = if (((h ushr 31) and 1) == 0) 1.0f else -1.0f
        vector[index] += sign * weight
    }

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        // Eliminar tildes y diacríticos
        val withoutDiacritics = decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        // Dejar solo letras y espacios
        return withoutDiacritics.replace("[^a-z0-9\\s]".toRegex(), " ").trim()
    }
}
