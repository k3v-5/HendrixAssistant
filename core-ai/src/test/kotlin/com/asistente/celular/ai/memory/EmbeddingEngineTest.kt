package com.asistente.celular.ai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingEngineTest {

    private val engine = FeatureHashingEmbeddingEngine(dimensions = 128)

    @Test
    fun testGenerateEmbeddingProducesNormalizedVector() = runBlocking {
        val vec = engine.generateEmbedding("Me gusta el café sin azúcar")
        assertEquals(128, vec.size)

        var normSq = 0f
        for (v in vec) normSq += v * v
        assertTrue("El vector debe tener norma L2 cercana a 1.0", kotlin.math.abs(normSq - 1.0f) < 0.01f)
    }

    @Test
    fun testSemanticSimilarityRanking() = runBlocking {
        val vecQuery = engine.generateEmbedding("alergia a los mariscos")
        val vecSimilar = engine.generateEmbedding("soy alérgico al marisco y camarón")
        val vecDifferent = engine.generateEmbedding("la clave del wifi es 123456")

        val simRelated = engine.cosineSimilarity(vecQuery, vecSimilar)
        val simUnrelated = engine.cosineSimilarity(vecQuery, vecDifferent)

        assertTrue(
            "Textos semánticamente afines deben tener mayor similitud ($simRelated vs $simUnrelated)",
            simRelated > simUnrelated
        )
        assertTrue("La similitud entre textos afines debe ser mayor a 0.2", simRelated > 0.2f)
    }
}
