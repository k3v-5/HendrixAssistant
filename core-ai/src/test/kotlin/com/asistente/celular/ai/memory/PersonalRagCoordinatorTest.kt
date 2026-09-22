package com.asistente.celular.ai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonalRagCoordinatorTest {

    private lateinit var coordinator: PersonalRagCoordinator
    private val embeddingEngine = FeatureHashingEmbeddingEngine(dimensions = 128)

    @Before
    fun setUp() {
        coordinator = PersonalRagCoordinator(embeddingEngine = embeddingEngine)
    }

    @Test
    fun `test indexing notes and tasks and searching by similarity`() = runBlocking {
        coordinator.indexNote(
            id = "n1",
            title = "Receta Salsa Picante",
            content = "Lleva tomates asados, chiles habaneros, cebolla y ajo.",
            tags = listOf("cocina", "recetas")
        )

        coordinator.indexTask(
            id = "t1",
            title = "Comprar verduras y tomates en el mercado",
            category = "compras",
            dueTime = "18:00",
            isCompleted = false
        )

        coordinator.indexPcDocument(
            id = "d1",
            fileName = "audio_dsp_notes.txt",
            contentSnippet = "Algoritmo de ecualización paramétrica y convolución de respuesta al impulso."
        )

        // Búsqueda de comida / cocina
        val foodResults = coordinator.search("receta tomates salsa", maxResults = 2, minSimilarity = 0.20f)
        assertTrue(foodResults.isNotEmpty())
        val topMatch = foodResults[0]
        assertTrue(topMatch.item.title.contains("Receta") || topMatch.item.title.contains("tomates"))

        // Búsqueda de audio en PC
        val audioResults = coordinator.search("algoritmo de audio ecualizador", maxResults = 2, minSimilarity = 0.20f)
        assertTrue(audioResults.isNotEmpty())
        assertEquals(RagItemType.PC_DOCUMENT, audioResults[0].item.type)
    }

    @Test
    fun `test buildAugmentedContextBlock generates formatted markdown`() = runBlocking {
        coordinator.indexNote(
            id = "note_proj",
            title = "Proyecto Hendrix",
            content = "Asistente inteligente con arquitectura modular en Kotlin y Python.",
            tags = listOf("programacion")
        )

        val contextBlock = coordinator.buildAugmentedContextBlock("arquitectura asistente modular", maxResults = 2, minSimilarity = 0.25f)
        assertTrue(contextBlock.contains("[Contexto Personal Recuperado (RAG)]"))
        assertTrue(contextBlock.contains("Proyecto Hendrix"))
    }

    @Test
    fun `test removeItem clears item from index`() = runBlocking {
        coordinator.indexNote(
            id = "tmp1",
            title = "Temporal",
            content = "Dato efímero para borrar"
        )

        coordinator.removeItem("tmp1")
        val results = coordinator.search("Dato efímero", minSimilarity = 0.20f)
        assertTrue(results.none { it.item.id == "note_tmp1" })
    }
}
