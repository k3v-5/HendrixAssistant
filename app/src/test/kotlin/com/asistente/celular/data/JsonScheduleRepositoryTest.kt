package com.asistente.celular.data

import com.asistente.celular.data.backup.FakeBackupTestContext
import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class JsonScheduleRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var testContext: FakeBackupTestContext
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "schedule_repo_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        testContext = FakeBackupTestContext(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testInit_createsDefaultsIfFileMissing() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val activities = repo.activities.value
        assertTrue("Debe cargar actividades por defecto al iniciar", activities.isNotEmpty())

        val scheduleFile = File(testContext.filesDir, "schedule_activities.json")
        assertTrue("El archivo schedule_activities.json debe crearse en disco", scheduleFile.exists())
    }

    @Test
    fun testAddActivity_persistsAndUpdatesFlow() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val newActivity = ScheduleActivityItem(
            id = "act-code-1",
            title = "Algoritmos y Arquitectura",
            description = "Resolución de problemas y optimización",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.TUESDAY),
            startTime = "08:00",
            endTime = "09:30",
            category = "Desarrollo / Código",
            colorHex = "#A855F7"
        )

        repo.addActivity(newActivity)
        testDispatcher.scheduler.advanceUntilIdle()

        val itemInFlow = repo.activities.value.firstOrNull { it.id == "act-code-1" }
        assertNotNull(itemInFlow)
        assertEquals("Algoritmos y Arquitectura", itemInFlow?.title)

        // Verificar persistencia en disco
        val scheduleFile = File(testContext.filesDir, "schedule_activities.json")
        assertTrue(scheduleFile.readText().contains("Algoritmos y Arquitectura"))
    }

    @Test
    fun testUpdateActivity_modifiesAndPersists() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val original = ScheduleActivityItem(
            id = "act-gym",
            title = "Gimnasio",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY),
            startTime = "07:00",
            endTime = "08:30"
        )
        repo.addActivity(original)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = original.copy(
            title = "Crossfit Intenso",
            endTime = "08:45",
            location = "Box Central"
        )
        val success = repo.updateActivity(updated)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val fetched = repo.getActivityById("act-gym")
        assertEquals("Crossfit Intenso", fetched?.title)
        assertEquals("08:45", fetched?.endTime)
        assertEquals("Box Central", fetched?.location)
    }

    @Test
    fun testDeleteActivity_removesFromFlowAndDisk() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = ScheduleActivityItem(
            id = "act-temp",
            title = "Reunión Temporal",
            daysOfWeek = setOf(ScheduleDay.FRIDAY),
            startTime = "15:00",
            endTime = "16:00"
        )
        repo.addActivity(item)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.activities.value.any { it.id == "act-temp" })

        val deleted = repo.deleteActivity("act-temp")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(deleted)
        assertFalse(repo.activities.value.any { it.id == "act-temp" })
    }

    @Test
    fun testToggleActivityEnabled() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val item = ScheduleActivityItem(
            id = "act-toggle",
            title = "Clase de Guitarra",
            daysOfWeek = setOf(ScheduleDay.SATURDAY),
            startTime = "11:00",
            endTime = "12:00",
            isEnabled = true
        )
        repo.addActivity(item)
        testDispatcher.scheduler.advanceUntilIdle()

        val toggled1 = repo.toggleActivityEnabled("act-toggle")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(toggled1)
        assertFalse(toggled1!!.isEnabled)

        val toggled2 = repo.toggleActivityEnabled("act-toggle")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(toggled2)
        assertTrue(toggled2!!.isEnabled)
    }

    @Test
    fun testGetActivitiesForDay_filtersAndSortsChronologically() = runTest(testDispatcher) {
        val repo = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        // Limpiar para test limpio de ordenamiento
        val current = repo.activities.value
        current.forEach { repo.deleteActivity(it.id) }
        testDispatcher.scheduler.advanceUntilIdle()

        val actAfternoon = ScheduleActivityItem(
            id = "tarde",
            title = "Tarde",
            daysOfWeek = setOf(ScheduleDay.MONDAY),
            startTime = "16:00",
            endTime = "17:00"
        )
        val actMorning = ScheduleActivityItem(
            id = "manana",
            title = "Mañana",
            daysOfWeek = setOf(ScheduleDay.MONDAY),
            startTime = "08:30",
            endTime = "10:00"
        )
        val actOtherDay = ScheduleActivityItem(
            id = "martes",
            title = "Solo Martes",
            daysOfWeek = setOf(ScheduleDay.TUESDAY),
            startTime = "09:00",
            endTime = "10:00"
        )

        repo.addActivity(actAfternoon)
        repo.addActivity(actMorning)
        repo.addActivity(actOtherDay)
        testDispatcher.scheduler.advanceUntilIdle()

        val mondayActs = repo.getActivitiesForDay(ScheduleDay.MONDAY)
        assertEquals(2, mondayActs.size)
        assertEquals("manana", mondayActs[0].id)
        assertEquals("tarde", mondayActs[1].id)

        val tuesdayActs = repo.getActivitiesForDay(ScheduleDay.TUESDAY)
        assertEquals(1, tuesdayActs.size)
        assertEquals("martes", tuesdayActs[0].id)
    }

    @Test
    fun testPersistenceReloadAcrossRepositoryInstances() = runTest(testDispatcher) {
        val repo1 = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val customItem = ScheduleActivityItem(
            id = "custom-persisted",
            title = "Sesión Producción Musical",
            description = "Edición de vocales en Ableton",
            daysOfWeek = setOf(ScheduleDay.THURSDAY, ScheduleDay.SUNDAY),
            startTime = "20:00",
            endTime = "22:30",
            category = "Música / Producción",
            colorHex = "#A855F7"
        )
        repo1.addActivity(customItem)
        testDispatcher.scheduler.advanceUntilIdle()

        // Crear una nueva instancia simulando reinicio o nueva pantalla de la app
        val repo2 = JsonScheduleRepository(testContext, testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val restored = repo2.getActivityById("custom-persisted")
        assertNotNull("La actividad debe haberse recuperado del archivo en disco", restored)
        assertEquals("Sesión Producción Musical", restored?.title)
        assertEquals("Edición de vocales en Ableton", restored?.description)
        assertEquals("20:00", restored?.startTime)
        assertEquals("22:30", restored?.endTime)
        assertEquals("Música / Producción", restored?.category)
        assertTrue(restored!!.occursOn(ScheduleDay.THURSDAY))
        assertTrue(restored.occursOn(ScheduleDay.SUNDAY))
        assertFalse(restored.occursOn(ScheduleDay.MONDAY))
    }
}
