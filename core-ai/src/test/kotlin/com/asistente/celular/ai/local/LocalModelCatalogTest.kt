package com.asistente.celular.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelCatalogTest {

    @Test
    fun testCatalogHasSupportedModels() {
        val models = LocalModelCatalog.ALL_MODELS
        assertTrue("El catálogo debe contener al menos 3 modelos", models.size >= 3)
    }

    @Test
    fun testDefaultModelIsSmolLm135m() {
        val defaultModel = LocalModelCatalog.DEFAULT_LOCAL_MODEL
        assertEquals("smollm2-135m", defaultModel.id)
        assertEquals(LocalModelFormat.GGUF, defaultModel.format)
        assertTrue(defaultModel.sizeBytes > 50_000_000L)
    }

    @Test
    fun testFindByIdReturnsCorrectSpec() {
        val qwen = LocalModelCatalog.findById("qwen2.5-0.5b")
        assertNotNull(qwen)
        assertEquals("Qwen 2.5 0.5B Instruct", qwen?.name)
        assertTrue(qwen?.downloadUrl?.startsWith("https://") == true)
        assertTrue(qwen?.fileName?.endsWith(".gguf") == true)
    }

    @Test
    fun testAllModelsHaveValidUrlsAndFormats() {
        for (model in LocalModelCatalog.ALL_MODELS) {
            assertTrue("La URL de ${model.name} debe ser HTTPS", model.downloadUrl.startsWith("https://"))
            assertTrue("El archivo debe terminar en .gguf", model.fileName.endsWith(".gguf"))
            assertTrue("El tamaño debe ser positivo", model.sizeBytes > 0)
            assertTrue("La RAM recomendada debe ser >= 1", model.recommendedRamGb >= 1)
        }
    }
}
