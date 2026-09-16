package com.asistente.celular.ai.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalKnowledgeGraphTest {

    @Test
    fun testKnowledgeRelationExtractor() {
        val relLikes = KnowledgeRelationExtractor.extract("a mi perro le gusta correr en el parque")
        assertNotNull(relLikes)
        assertEquals("perro", relLikes!!.subject)
        assertEquals("le_gusta", relLikes.predicate)
        assertTrue(relLikes.obj.contains("correr"))

        val relProperty = KnowledgeRelationExtractor.extract("mi color favorito es azul")
        assertNotNull(relProperty)
        assertEquals("color favorito", relProperty!!.subject)
        assertEquals("es", relProperty.predicate)
        assertEquals("azul", relProperty.obj)
    }

    @Test
    fun testInMemoryKnowledgeGraph() = runBlocking {
        val graph = InMemoryPersonalKnowledgeGraph()

        graph.addRelation("perro", "se_llama", "Hendrix")
        graph.addRelation("usuario", "trabaja_en", "Google")
        graph.addRelation("usuario", "le_gusta", "cafe")

        val userRelations = graph.findRelationsAbout("usuario")
        assertEquals(2, userRelations.size)

        val dogRelations = graph.findRelationsAbout("perro")
        assertEquals(1, dogRelations.size)
        assertEquals("Hendrix", dogRelations[0].obj)

        val searchMatches = graph.findRelationsAbout("cafe")
        assertEquals(1, searchMatches.size)
        assertEquals("cafe", searchMatches[0].obj)

        val allRelations = graph.getAllRelations()
        assertEquals(3, allRelations.size)
    }
}
