package com.asistente.celular.skills

import com.asistente.celular.nlu.driving.DrivingModeController
import com.asistente.celular.nlu.emergency.EmergencySosController
import com.asistente.celular.nlu.emergency.EmergencyTriggerResult
import com.asistente.celular.nlu.expenses.ExpenseCategory
import com.asistente.celular.nlu.expenses.ExpenseItem
import com.asistente.celular.nlu.expenses.ExpenseMonthlySummary
import com.asistente.celular.nlu.expenses.ExpenseRepository
import com.asistente.celular.nlu.files.FileCategory
import com.asistente.celular.nlu.files.FileSearchEngine
import com.asistente.celular.nlu.files.FileSearchQuery
import com.asistente.celular.nlu.files.LocalFileItem
import com.asistente.celular.nlu.ocr.OcrBlock
import com.asistente.celular.nlu.ocr.OcrEngine
import com.asistente.celular.nlu.ocr.OcrResult
import com.asistente.celular.nlu.rhythm.SleepWakeController
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.DrivingModeUiPayload
import com.asistente.celular.nlu.ui.EmergencySosUiPayload
import com.asistente.celular.nlu.ui.ExpenseReportUiPayload
import com.asistente.celular.nlu.ui.LocalFileResultsUiPayload
import com.asistente.celular.nlu.ui.OcrGlanceUiPayload
import com.asistente.celular.nlu.ui.ScreenVisionUiPayload
import com.asistente.celular.nlu.vision.ScreenContentSnapshot
import com.asistente.celular.nlu.vision.ScreenUnderstandingProvider
import com.asistente.celular.skills.driving.DrivingModeSkill
import com.asistente.celular.skills.emergency.EmergencySosSkill
import com.asistente.celular.skills.expenses.ExpenseSkill
import com.asistente.celular.skills.files.LocalFileSearchSkill
import com.asistente.celular.skills.rhythm.SmartRhythmSkill
import com.asistente.celular.skills.vision.CameraGlanceSkill
import com.asistente.celular.skills.vision.ScreenUnderstandingSkill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedCapabilitiesSkillsTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    @Test
    fun testScreenUnderstandingSkill() = runBlocking {
        val fakeProvider = object : ScreenUnderstandingProvider {
            override fun isAvailable(): Boolean = true
            override suspend fun captureScreenContent(): ScreenContentSnapshot {
                return ScreenContentSnapshot(
                    packageName = "com.android.chrome",
                    title = "Noticias de Tecnología",
                    texts = listOf("Inteligencia Artificial avanza en móviles", "Nuevo chip cuántico anunciado", "5 min de lectura"),
                    rawTextDump = "Inteligencia Artificial avanza en móviles\nNuevo chip cuántico anunciado"
                )
            }
        }

        val skill = ScreenUnderstandingSkill(fakeProvider)

        val score = skill.score(dummyContext, "resumeme lo que veo en mi pantalla")
        assertTrue("Debe detectar intención de resumir pantalla", score.isMatch)
        assertEquals("summary", score.capturedSlots["action"])

        val output = skill.execute(dummyContext, "resumeme lo que veo en mi pantalla", score)
        assertTrue(output.success)
        assertTrue(output.payload is ScreenVisionUiPayload)
        val payload = output.payload as ScreenVisionUiPayload
        assertEquals(3, payload.texts.size)
    }

    @Test
    fun testCameraGlanceSkill() = runBlocking {
        val fakeOcr = object : OcrEngine {
            override fun isAvailable(): Boolean = true
            override suspend fun recognizeText(imageUriOrPath: String): OcrResult {
                return OcrResult(
                    fullText = "RESTAURANTE EL PORTAL\nTOTAL: $450.00 MXN\nGRACIAS POR SU COMPRA",
                    blocks = listOf(
                        OcrBlock("RESTAURANTE EL PORTAL", lines = listOf("RESTAURANTE EL PORTAL")),
                        OcrBlock("TOTAL: $450.00 MXN", lines = listOf("TOTAL: $450.00 MXN")),
                        OcrBlock("GRACIAS POR SU COMPRA", lines = listOf("GRACIAS POR SU COMPRA"))
                    )
                )
            }
        }

        val skill = CameraGlanceSkill(fakeOcr)

        val score = skill.score(dummyContext, "lee este papel que tengo aqui")
        assertTrue("Debe coincidir con solicitud de lectura visual", score.isMatch)

        val output = skill.execute(dummyContext, "lee este papel que tengo aqui", score)
        assertTrue(output.success)
        assertTrue(output.payload is OcrGlanceUiPayload)
        val payload = output.payload as OcrGlanceUiPayload
        assertTrue(payload.fullText.contains("RESTAURANTE EL PORTAL"))
    }

    @Test
    fun testDrivingModeSkill() = runBlocking {
        var drivingActive = false
        val fakeController = object : DrivingModeController {
            override fun isDrivingModeActive(): Boolean = drivingActive
            override fun setDrivingMode(active: Boolean): Boolean {
                drivingActive = active
                return true
            }
            override fun getConnectedVehicleName(): String = "Honda Civic Bluetooth"
        }

        val skill = DrivingModeSkill(fakeController)

        val scoreActivate = skill.score(dummyContext, "activa el modo auto")
        assertTrue(scoreActivate.isMatch)
        assertEquals("activate", scoreActivate.capturedSlots["action"])

        val outputActivate = skill.execute(dummyContext, "activa el modo auto", scoreActivate)
        assertTrue(outputActivate.success)
        assertTrue(drivingActive)
        val payload = outputActivate.payload as DrivingModeUiPayload
        assertTrue(payload.isActive)
        assertEquals("Honda Civic Bluetooth", payload.connectedDeviceName)

        val scoreDeactivate = skill.score(dummyContext, "termine de manejar desactiva modo auto")
        assertTrue(scoreDeactivate.isMatch)
        assertEquals("deactivate", scoreDeactivate.capturedSlots["action"])
        val outputDeactivate = skill.execute(dummyContext, "termine de manejar desactiva modo auto", scoreDeactivate)
        assertTrue(outputDeactivate.success)
        assertTrue(!drivingActive)
    }

    @Test
    fun testSmartRhythmSkill() = runBlocking {
        var nightCalled = false
        var morningCalled = false
        val fakeRhythm = object : SleepWakeController {
            override suspend fun executeGoodNightRoutine(): String {
                nightCalled = true
                return "Luces apagadas y DND activado."
            }
            override suspend fun executeGoodMorningRoutine(): String {
                morningCalled = true
                return "Luces al 100% y clima templado."
            }
        }

        val skill = SmartRhythmSkill(fakeRhythm)

        val scoreNight = skill.score(dummyContext, "buenas noches Hendrix")
        assertTrue(scoreNight.isMatch)
        val outputNight = skill.execute(dummyContext, "buenas noches Hendrix", scoreNight)
        assertTrue(outputNight.success)
        assertTrue(nightCalled)

        val scoreMorning = skill.score(dummyContext, "buenos dias Hendrix")
        assertTrue(scoreMorning.isMatch)
        val outputMorning = skill.execute(dummyContext, "buenos dias Hendrix", scoreMorning)
        assertTrue(outputMorning.success)
        assertTrue(morningCalled)
    }

    @Test
    fun testLocalFileSearchSkill() = runBlocking {
        val fakeFiles = listOf(
            LocalFileItem("Factura_Septiembre.pdf", "/storage/emulated/0/Download/Factura_Septiembre.pdf", "application/pdf", 102400, System.currentTimeMillis(), FileCategory.DOCUMENT),
            LocalFileItem("Recibo_CFE.pdf", "/storage/emulated/0/Download/Recibo_CFE.pdf", "application/pdf", 51200, System.currentTimeMillis(), FileCategory.DOCUMENT)
        )

        val fakeEngine = object : FileSearchEngine {
            override fun isAvailable(): Boolean = true
            override suspend fun searchFiles(query: FileSearchQuery): List<LocalFileItem> {
                return fakeFiles.filter { it.name.contains(query.query, ignoreCase = true) }
            }
        }

        val skill = LocalFileSearchSkill(fakeEngine)

        val score = skill.score(dummyContext, "busca el documento Factura")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "busca el documento Factura", score)
        assertTrue(output.success)
        assertTrue(output.payload is LocalFileResultsUiPayload)
        val payload = output.payload as LocalFileResultsUiPayload
        assertEquals(1, payload.files.size)
        assertEquals("Factura_Septiembre.pdf", payload.files.first().name)
    }

    @Test
    fun testExpenseSkill() = runBlocking {
        val stored = mutableListOf<ExpenseItem>()
        val fakeRepo = object : ExpenseRepository {
            override suspend fun addExpense(expense: ExpenseItem): Boolean {
                stored.add(expense)
                return true
            }
            override suspend fun getExpenses(limit: Int): List<ExpenseItem> = stored
            override suspend fun getMonthlySummary(year: Int, month: Int): ExpenseMonthlySummary {
                val total = stored.sumOf { it.amount }
                return ExpenseMonthlySummary(
                    yearMonth = "$year-$month",
                    totalAmount = total,
                    currency = "MXN",
                    byCategory = stored.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } },
                    count = stored.size
                )
            }
            override suspend fun deleteExpense(id: String): Boolean = true
        }

        val skill = ExpenseSkill(fakeRepo)

        val scoreAdd = skill.score(dummyContext, "anota gasto de 150 pesos en comida")
        assertTrue(scoreAdd.isMatch)
        assertEquals("add", scoreAdd.capturedSlots["action"])
        assertEquals("150", scoreAdd.capturedSlots["amount"])
        assertEquals(ExpenseCategory.FOOD.name, scoreAdd.capturedSlots["category"])

        val outputAdd = skill.execute(dummyContext, "anota gasto de 150 pesos en comida", scoreAdd)
        assertTrue(outputAdd.success)
        assertEquals(1, stored.size)
        assertEquals(150.0, stored.first().amount, 0.01)

        val scoreQuery = skill.score(dummyContext, "cuanto he gastado este mes")
        assertTrue(scoreQuery.isMatch)
        assertEquals("query", scoreQuery.capturedSlots["action"])
        val outputQuery = skill.execute(dummyContext, "cuanto he gastado este mes", scoreQuery)
        assertTrue(outputQuery.success)
        assertTrue(outputQuery.payload is ExpenseReportUiPayload)
    }

    @Test
    fun testEmergencySosSkill() = runBlocking {
        var triggered = false
        var canceled = false
        val fakeEmergency = object : EmergencySosController {
            override suspend fun triggerEmergencySos(): EmergencyTriggerResult {
                triggered = true
                return EmergencyTriggerResult(
                    success = true,
                    locationUrl = "https://maps.google.com/?q=19.4326,-99.1332",
                    notifiedContacts = listOf("+525512345678"),
                    sirenStarted = true,
                    flashlightSosStarted = true
                )
            }
            override fun cancelEmergencySos(): Boolean {
                canceled = true
                return true
            }
            override fun isEmergencyActive(): Boolean = triggered && !canceled
        }

        val skill = EmergencySosSkill(fakeEmergency)

        val scoreTrigger = skill.score(dummyContext, "emergencia auxilio")
        assertTrue(scoreTrigger.isMatch)
        assertEquals("trigger", scoreTrigger.capturedSlots["action"])

        val outputTrigger = skill.execute(dummyContext, "emergencia auxilio", scoreTrigger)
        assertTrue(outputTrigger.success)
        assertTrue(triggered)
        val payload = outputTrigger.payload as EmergencySosUiPayload
        assertTrue(payload.isTriggered)
        assertEquals("https://maps.google.com/?q=19.4326,-99.1332", payload.locationUrl)

        val scoreCancel = skill.score(dummyContext, "falsa alarma cancela emergencia")
        assertTrue(scoreCancel.isMatch)
        assertEquals("cancel", scoreCancel.capturedSlots["action"])
        val outputCancel = skill.execute(dummyContext, "falsa alarma cancela emergencia", scoreCancel)
        assertTrue(outputCancel.success)
        assertTrue(canceled)
    }
}
