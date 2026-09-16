package com.asistente.celular.skills

import com.asistente.celular.ai.orchestration.ModelExecutionPlan
import com.asistente.celular.ai.orchestration.MultiModelOrchestrator
import com.asistente.celular.ai.orchestration.QueryComplexityLevel
import com.asistente.celular.nlu.automotive.AutomotiveCarController
import com.asistente.celular.nlu.automotive.AutomotiveScreenState
import com.asistente.celular.nlu.automotive.CarNavigationShortcut
import com.asistente.celular.nlu.automotive.CarScreenTemplate
import com.asistente.celular.nlu.documents.DocumentKnowledgeEngine
import com.asistente.celular.nlu.documents.DocumentQueryResult
import com.asistente.celular.nlu.documents.ParsedDocument
import com.asistente.celular.nlu.meeting.MeetingAgreement
import com.asistente.celular.nlu.meeting.MeetingRecorderEngine
import com.asistente.celular.nlu.meeting.MeetingSession
import com.asistente.celular.nlu.meeting.MeetingSpeakerTurn
import com.asistente.celular.nlu.meeting.MeetingStatus
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.AutomotiveUiPayload
import com.asistente.celular.nlu.ui.DocumentChatUiPayload
import com.asistente.celular.nlu.ui.MeetingRecorderUiPayload
import com.asistente.celular.nlu.ui.MultiModelOrchestratorUiPayload
import com.asistente.celular.nlu.ui.SoundscapeUiPayload
import com.asistente.celular.nlu.ui.VoiceCraftUiPayload
import com.asistente.celular.nlu.ui.WearCompanionUiPayload
import com.asistente.celular.nlu.wear.WearCompanionController
import com.asistente.celular.nlu.wear.WearDeviceNode
import com.asistente.celular.skills.ai.MultiModelOrchestratorSkill
import com.asistente.celular.skills.automotive.AutomotiveSkill
import com.asistente.celular.skills.documents.DocumentChatSkill
import com.asistente.celular.skills.meeting.MeetingRecorderSkill
import com.asistente.celular.skills.soundscape.SoundscapeSkill
import com.asistente.celular.skills.voicecraft.VoiceCraftSkill
import com.asistente.celular.skills.wear.WearCompanionSkill
import com.asistente.celular.voice.soundscape.SoundscapeAudioEngine
import com.asistente.celular.voice.soundscape.SoundscapeState
import com.asistente.celular.voice.soundscape.SoundscapeType
import com.asistente.celular.voice.voicecraft.VoiceCraftStudioController
import com.asistente.celular.voice.voicecraft.VoiceProfileCraftConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommercialGradeCapabilitiesTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    // ==========================================
    // 1. Android Auto Native Tests
    // ==========================================
    @Test
    fun testAutomotiveSkill_matchingAndExecution() = runBlocking {
        val fakeCarController = object : AutomotiveCarController {
            private var state = AutomotiveScreenState(
                isCarConnected = true,
                headUnitName = "Tesla Model 3 Dashboard",
                currentTemplate = CarScreenTemplate.VOICE_COMMAND,
                shortcuts = listOf(CarNavigationShortcut("Casa", "Av. Principal 123", 15))
            )
            override fun notifyCarConnected(headUnitName: String) { state = state.copy(isCarConnected = true, headUnitName = headUnitName) }
            override fun notifyCarDisconnected() { state = state.copy(isCarConnected = false) }
            override fun getCurrentScreenState(): AutomotiveScreenState = state
            override fun switchTemplate(template: CarScreenTemplate) { state = state.copy(currentTemplate = template) }
            override suspend fun triggerVoiceSessionFromSteeringWheel(): Boolean = true
        }

        val skill = AutomotiveSkill(fakeCarController)

        // Use an exact trigger phrase from AutomotiveSkill.score()
        val score = skill.score(dummyContext, "android auto")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "android auto", score)
        // Actual speech: "Modo Android Auto proyectado en el tablero. Atajos de navegación rápidos listos."
        assertTrue(output.speech.contains("Android Auto"))
        assertTrue(output.payload is AutomotiveUiPayload)
        val payload = output.payload as AutomotiveUiPayload
        assertTrue(payload.isCarConnected)
        assertEquals("Tesla Model 3 Dashboard", payload.headUnitName)
    }

    // ==========================================
    // 2. Wear OS Companion Tests
    // ==========================================
    @Test
    fun testWearCompanionSkill_matchingAndExecution() = runBlocking {
        val fakeWearController = object : WearCompanionController {
            override fun getConnectedWearDevices(): List<WearDeviceNode> = listOf(
                WearDeviceNode(nodeId = "wear_123", name = "Galaxy Watch 6", batteryPercent = 88, isConnected = true)
            )
            override suspend fun sendCompactNotificationToWrist(title: String, message: String): Boolean = true
            override suspend fun triggerWristHapticPulse(pulsePattern: String): Boolean = true
            override fun notifyWristVoiceInputReceived(spokenText: String) {}
        }

        val skill = WearCompanionSkill(fakeWearController)
        val score = skill.score(dummyContext, "sincronizar reloj inteligente wear")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "sincronizar reloj inteligente wear", score)
        assertTrue(output.speech.contains("Galaxy Watch 6"))
        assertTrue(output.payload is WearCompanionUiPayload)
        val payload = output.payload as WearCompanionUiPayload
        assertEquals(1, payload.connectedWearCount)
    }

    // ==========================================
    // 3. Meeting Recorder & Diarization Tests
    // ==========================================
    @Test
    fun testMeetingRecorderSkill_matchingAndExecution() = runBlocking {
        val fakeRecorder = object : MeetingRecorderEngine {
            override suspend fun startMeeting(title: String): MeetingSession = MeetingSession(
                sessionId = "session_test",
                title = title,
                turns = listOf(MeetingSpeakerTurn("Carlos", "Hola a todos")),
                status = MeetingStatus.RECORDING
            )
            override suspend fun appendAudioChunk(pcmChunk: ShortArray): MeetingSpeakerTurn? = null
            override suspend fun stopMeeting(): MeetingSession = MeetingSession(
                sessionId = "session_test",
                title = "Reunión de Equipo",
                turns = listOf(MeetingSpeakerTurn("Carlos", "Acordamos lanzar el viernes")),
                agreements = listOf(MeetingAgreement("agr_1", "Lanzar el viernes", "Carlos")),
                status = MeetingStatus.COMPLETED
            )
            override fun getActiveMeeting(): MeetingSession? = null
            override suspend fun extractAgreements(sessionId: String): List<MeetingAgreement> = emptyList()
        }

        val skill = MeetingRecorderSkill(fakeRecorder)
        // Use exact trigger phrase from MeetingRecorderSkill.score()
        val score = skill.score(dummyContext, "grabar reunión")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "grabar reunión", score)
        // Actual speech: "Grabación de reunión iniciada con diarización de voces activa..."
        assertTrue(output.speech.contains("Grabación de reunión iniciada"))
        assertTrue(output.payload is MeetingRecorderUiPayload)
        val payload = output.payload as MeetingRecorderUiPayload
        assertTrue(payload.isRecording)
    }

    // ==========================================
    // 4. Multi-Model Orchestration (<300ms) Tests
    // ==========================================
    @Test
    fun testMultiModelOrchestratorSkill_matchingAndExecution() = runBlocking {
        val fakeOrchestrator = object : MultiModelOrchestrator {
            override fun planExecution(prompt: String): ModelExecutionPlan = ModelExecutionPlan(
                selectedModelName = "Qwen-2.5-0.5B-Fast",
                expectedLatencyMs = 120,
                useSpeculativeFastPath = true,
                requiresCloudEscalation = false,
                complexityLevel = QueryComplexityLevel.TRIVIAL_DEVICE_ACTION
            )
            override suspend fun executeSpeculative(prompt: String, onTokenStream: (String) -> Unit): String =
                "Respuesta ultra-rápida en 120ms"
            override fun getOrchestrationTelemetry(): Map<String, Any> = emptyMap()
        }

        val skill = MultiModelOrchestratorSkill(fakeOrchestrator)
        // Use exact trigger from MultiModelOrchestratorSkill.score()
        val score = skill.score(dummyContext, "modo baja latencia")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "modo baja latencia", score)
        // Actual speech: "Orquestador multi-modelo activo. Enrutamiento especulativo operando con latencia esperada de 120ms en Qwen-2.5-0.5B-Fast."
        assertTrue(output.speech.contains("120ms"))
        assertTrue(output.payload is MultiModelOrchestratorUiPayload)
        val payload = output.payload as MultiModelOrchestratorUiPayload
        assertTrue(payload.fastPathActive)
        assertEquals(120, payload.expectedLatencyMs)
    }

    // ==========================================
    // 5. Document Q&A Assistant Tests
    // ==========================================
    @Test
    fun testDocumentChatSkill_matchingAndExecution() = runBlocking {
        val fakeDocEngine = object : DocumentKnowledgeEngine {
            override suspend fun ingestDocument(filePath: String, rawContent: String): ParsedDocument =
                ParsedDocument("doc_1", "Contrato_Servicios.pdf", "pdf", 10, emptyList())
            override suspend fun queryDocument(documentId: String?, query: String): DocumentQueryResult =
                DocumentQueryResult(
                    answer = "El plazo estipulado para el pago es a 30 días hábiles.",
                    sourceFileName = "Contrato_Servicios.pdf",
                    relevantSectionTitle = "Cláusula 4: Términos de Pago",
                    confidenceScore = 0.95f,
                    referenceExcerpt = "El pago se realizará en 30 días hábiles tras recibir la factura."
                )
            override fun getIngestedDocuments(): List<ParsedDocument> = emptyList()
            override fun deleteDocument(documentId: String): Boolean = true
        }

        val skill = DocumentChatSkill(fakeDocEngine)
        val score = skill.score(dummyContext, "consultar documento")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "consultar documento", score)
        assertTrue(output.speech.contains("30 días hábiles"))
        assertTrue(output.payload is DocumentChatUiPayload)
        val payload = output.payload as DocumentChatUiPayload
        assertEquals("Contrato_Servicios.pdf", payload.fileName)
        assertEquals(95, payload.confidencePercent)
    }

    // ==========================================
    // 6. Soundscape & Binaural Waves Tests
    // ==========================================
    @Test
    fun testSoundscapeSkill_matchingAndExecution() = runBlocking {
        val fakeSoundscape = object : SoundscapeAudioEngine {
            private var state = SoundscapeState()
            override fun startSoundscape(type: SoundscapeType, durationMinutes: Int?): SoundscapeState {
                state = SoundscapeState(isPlaying = true, activeType = type, volume = 0.7f, remainingMinutes = durationMinutes)
                return state
            }
            override fun stopSoundscape(): Boolean {
                state = SoundscapeState(isPlaying = false)
                return true
            }
            override fun setVolume(volume: Float) { state = state.copy(volume = volume) }
            override fun getCurrentState(): SoundscapeState = state
        }

        val skill = SoundscapeSkill(fakeSoundscape)
        // Exact trigger from SoundscapeSkill.score()
        val score = skill.score(dummyContext, "ruido marron")
        assertTrue("Score should match", score.isMatch)

        val output = skill.execute(dummyContext, "ruido marron", score)
        // Actual speech: "Generando paisaje sonoro procedural: Ruido Marrón Cálido. Temporizador programado para 45 minutos."
        assertTrue(output.speech.contains("paisaje sonoro"))
        assertTrue(output.payload is SoundscapeUiPayload)
        val payload = output.payload as SoundscapeUiPayload
        assertTrue(payload.isPlaying)
        assertEquals("Ruido Marrón Cálido", payload.soundscapeName)
    }

    // ==========================================
    // 7. VoiceCraft Studio Tests
    // ==========================================
    @Test
    fun testVoiceCraftSkill_matchingAndExecution() = runBlocking {
        val fakeVoiceCraft = object : VoiceCraftStudioController {
            private var currentProfile = VoiceProfileCraftConfig(voiceId = "default", styleName = "Original")
            private val presets = listOf(
                VoiceProfileCraftConfig(voiceId = "default", styleName = "Original", pitchSemitones = 0.0f),
                VoiceProfileCraftConfig(voiceId = "deep", styleName = "Grave & Profundo", pitchSemitones = -2.5f),
                VoiceProfileCraftConfig(voiceId = "dynamic", styleName = "Dinámico", pitchSemitones = 1.5f),
                VoiceProfileCraftConfig(voiceId = "night", styleName = "Nocturno Suave", pitchSemitones = -1.0f)
            )
            override fun applyProfile(config: VoiceProfileCraftConfig) { currentProfile = config }
            override fun getCurrentProfile(): VoiceProfileCraftConfig = currentProfile
            override fun getAvailablePresets(): List<VoiceProfileCraftConfig> = presets
            override suspend fun previewVoice(sampleText: String): Boolean = true
        }

        val skill = VoiceCraftSkill(fakeVoiceCraft)
        // Use exact trigger from VoiceCraftSkill.score()
        val score = skill.score(dummyContext, "estudio de voz")
        assertTrue("Score should match", score.isMatch)

        // Execute with "grave" keyword so it picks preset index 1 (Grave & Profundo)
        val output = skill.execute(dummyContext, "voz grave profesional estudio de voz", score)
        // Actual speech: "Perfil acústico actualizado a 'Grave & Profundo'. Pitch: -2.5 semitonos, velocidad: 1.0x."
        assertTrue(output.speech.contains("Perfil acústico actualizado"))
        assertTrue(output.payload is VoiceCraftUiPayload)
        val payload = output.payload as VoiceCraftUiPayload
        assertEquals("Grave & Profundo", payload.styleName)
        assertEquals(-2.5f, payload.pitchShift)
    }
}
