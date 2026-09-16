package com.asistente.celular.skills

import com.asistente.celular.nlu.ambient.DockModeController
import com.asistente.celular.nlu.ambient.DockModeState
import com.asistente.celular.nlu.ambient.DockType
import com.asistente.celular.nlu.battery.BatteryHealthMonitor
import com.asistente.celular.nlu.battery.BatteryHealthSnapshot
import com.asistente.celular.nlu.battery.ThermalRating
import com.asistente.celular.nlu.camera.CameraCaptureConfig
import com.asistente.celular.nlu.camera.CameraCaptureResult
import com.asistente.celular.nlu.camera.CameraDirectorController
import com.asistente.celular.nlu.camera.CameraLensType
import com.asistente.celular.nlu.context.ContextRule
import com.asistente.celular.nlu.context.ContextTriggerEngine
import com.asistente.celular.nlu.context.ContextTriggerType
import com.asistente.celular.nlu.context.CurrentContextState
import com.asistente.celular.nlu.health.HealthTelemetryRepository
import com.asistente.celular.nlu.health.HealthTelemetrySnapshot
import com.asistente.celular.nlu.journal.JournalEntry
import com.asistente.celular.nlu.journal.MoodSentiment
import com.asistente.celular.nlu.journal.VoiceJournalEngine
import com.asistente.celular.nlu.mesh.HendrixMeshController
import com.asistente.celular.nlu.mesh.MeshDeviceType
import com.asistente.celular.nlu.mesh.MeshNode
import com.asistente.celular.nlu.mesh.MeshPacket
import com.asistente.celular.nlu.planner.AutonomousPlannerEngine
import com.asistente.celular.nlu.planner.PlanStep
import com.asistente.celular.nlu.planner.TaskPlan
import com.asistente.celular.nlu.rag.EntityType
import com.asistente.celular.nlu.rag.KnowledgeEntity
import com.asistente.celular.nlu.rag.KnowledgeGraphRepository
import com.asistente.celular.nlu.rag.KnowledgeRelation
import com.asistente.celular.nlu.rag.KnowledgeSearchResult
import com.asistente.celular.nlu.security.DeviceSecurityAuditor
import com.asistente.celular.nlu.security.NetworkConnectionAudit
import com.asistente.celular.nlu.security.PrivacyFirewallEngine
import com.asistente.celular.nlu.security.SecurityAuditReport
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.telecom.CallScreeningController
import com.asistente.celular.nlu.telecom.CallScreeningSession
import com.asistente.celular.nlu.telecom.ScreeningVerdict
import com.asistente.celular.nlu.translation.InterpreterEngine
import com.asistente.celular.nlu.translation.InterpreterSession
import com.asistente.celular.nlu.translation.InterpreterTurn
import com.asistente.celular.nlu.ui.AmbientDockUiPayload
import com.asistente.celular.nlu.ui.BatteryHealthUiPayload
import com.asistente.celular.nlu.ui.CallScreeningUiPayload
import com.asistente.celular.nlu.ui.CameraDirectorUiPayload
import com.asistente.celular.nlu.ui.ContextTriggerUiPayload
import com.asistente.celular.nlu.ui.HardwareGestureUiPayload
import com.asistente.celular.nlu.ui.HealthTelemetryUiPayload
import com.asistente.celular.nlu.ui.InterpreterUiPayload
import com.asistente.celular.nlu.ui.KnowledgeRagUiPayload
import com.asistente.celular.nlu.ui.MeshSyncUiPayload
import com.asistente.celular.nlu.ui.PrivacyFirewallUiPayload
import com.asistente.celular.nlu.ui.SecurityAuditUiPayload
import com.asistente.celular.nlu.ui.TaskPlanUiPayload
import com.asistente.celular.nlu.ui.VoiceJournalUiPayload
import com.asistente.celular.nlu.ui.VoiceprintUiPayload
import com.asistente.celular.skills.ambient.AmbientDockSkill
import com.asistente.celular.skills.battery.BatteryHealthSkill
import com.asistente.celular.skills.biometrics.VoiceprintSkill
import com.asistente.celular.skills.camera.CameraDirectorSkill
import com.asistente.celular.skills.context.ContextTriggerSkill
import com.asistente.celular.skills.gesture.HardwareGestureSkill
import com.asistente.celular.skills.health.HealthTelemetrySkill
import com.asistente.celular.skills.journal.VoiceJournalSkill
import com.asistente.celular.skills.mesh.MeshSyncSkill
import com.asistente.celular.skills.planner.AutonomousPlannerSkill
import com.asistente.celular.skills.rag.KnowledgeRagSkill
import com.asistente.celular.skills.security.PrivacyFirewallSkill
import com.asistente.celular.skills.security.SecurityAuditSkill
import com.asistente.celular.skills.telecom.CallScreeningSkill
import com.asistente.celular.skills.translation.InterpreterSkill
import com.asistente.celular.voice.biometrics.BiometricMatchResult
import com.asistente.celular.voice.biometrics.VoiceBiometricsEngine
import com.asistente.celular.voice.biometrics.VoiceprintProfile
import com.asistente.celular.voice.gesture.GestureEvent
import com.asistente.celular.voice.gesture.HardwareGestureDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextGenAutonomousCapabilitiesTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    @Test
    fun testVoiceprintSkill() = runBlocking {
        val fakeBiometrics = object : VoiceBiometricsEngine {
            override fun extractVoiceprint(pcmData: ShortArray): FloatArray = FloatArray(16) { 0.5f }
            override suspend fun enrollSpeaker(speakerId: String, speakerName: String, pcmSamples: List<ShortArray>): VoiceprintProfile {
                return VoiceprintProfile(speakerId, speakerName, FloatArray(16))
            }
            override suspend fun verifySpeaker(pcmData: ShortArray, targetSpeakerId: String?): BiometricMatchResult {
                return BiometricMatchResult(true, 0.95f, "Carlos", "Match exacto")
            }
            override fun getRegisteredProfiles(): List<VoiceprintProfile> = listOf(
                VoiceprintProfile("owner", "Carlos", FloatArray(16))
            )
            override fun deleteProfile(speakerId: String): Boolean = true
        }

        val skill = VoiceprintSkill(fakeBiometrics)
        val score = skill.score(dummyContext, "reconoce mi voz")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "reconoce mi voz", score)
        assertTrue(output.success)
        assertTrue(output.payload is VoiceprintUiPayload)
        assertEquals("Carlos", (output.payload as VoiceprintUiPayload).speakerName)
    }

    @Test
    fun testKnowledgeRagSkill() = runBlocking {
        val fakeRepo = object : KnowledgeGraphRepository {
            override suspend fun saveEntity(entity: KnowledgeEntity) {}
            override suspend fun linkEntities(relation: KnowledgeRelation) {}
            override suspend fun searchRelevantKnowledge(query: String, topK: Int): List<KnowledgeSearchResult> {
                return listOf(
                    KnowledgeSearchResult(
                        entity = KnowledgeEntity("e1", "Contrato Casa", EntityType.OBJECT),
                        textSnippet = "Vence en octubre 2026",
                        similarityScore = 0.88f
                    )
                )
            }
            override suspend fun getAllEntities(): List<KnowledgeEntity> = emptyList()
            override suspend fun getAllRelations(): List<KnowledgeRelation> = emptyList()
            override suspend fun clearAll() {}
        }

        val skill = KnowledgeRagSkill(fakeRepo)
        val score = skill.score(dummyContext, "busca en mi memoria sobre mi casa")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "busca en mi memoria sobre mi casa", score)
        assertTrue(output.success)
        assertTrue(output.payload is KnowledgeRagUiPayload)
        val payload = output.payload as KnowledgeRagUiPayload
        assertEquals(1, payload.results.size)
        assertEquals("Contrato Casa", payload.topEntity)
    }

    @Test
    fun testVoiceJournalSkill() = runBlocking {
        val fakeJournal = object : VoiceJournalEngine {
            override suspend fun processMindDump(rawTranscript: String): JournalEntry {
                return JournalEntry(
                    id = "j1",
                    rawTranscript = rawTranscript,
                    executiveSummary = "Resumen del día",
                    sentiment = MoodSentiment.POSITIVE
                )
            }
            override suspend fun getRecentEntries(limit: Int): List<JournalEntry> = emptyList()
            override suspend fun deleteEntry(entryId: String): Boolean = true
        }

        val skill = VoiceJournalSkill(fakeJournal)
        val score = skill.score(dummyContext, "diario de voz")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "diario de voz hoy aprendí mucho", score)
        assertTrue(output.success)
        assertTrue(output.payload is VoiceJournalUiPayload)
    }

    @Test
    fun testMeshSyncSkill() = runBlocking {
        val fakeMesh = object : HendrixMeshController {
            override suspend fun discoverLocalPeers(): List<MeshNode> = listOf(
                MeshNode("pc1", "PC Estudio", "192.168.1.50", 8899, MeshDeviceType.PC)
            )
            override suspend fun sendPacketToPeer(targetNodeId: String, packet: MeshPacket): Boolean = true
            override suspend fun broadcastClipboard(content: String): Int = 1
            override fun getConnectedPeers(): List<MeshNode> = emptyList()
            override fun isServiceActive(): Boolean = true
        }

        val skill = MeshSyncSkill(fakeMesh)
        val score = skill.score(dummyContext, "sincronizar con pc")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "sincronizar con pc", score)
        assertTrue(output.success)
        assertTrue(output.payload is MeshSyncUiPayload)
        assertEquals(1, (output.payload as MeshSyncUiPayload).peersCount)
    }

    @Test
    fun testCallScreeningSkill() = runBlocking {
        val fakeCall = object : CallScreeningController {
            override fun evaluateIncomingCall(phoneNumber: String, callerName: String?): CallScreeningSession {
                return CallScreeningSession(
                    callId = "c1",
                    phoneNumber = phoneNumber,
                    callerDisplayName = callerName,
                    spamLikelihoodPercent = 85,
                    verdict = ScreeningVerdict.REJECT_SPAM
                )
            }
            override suspend fun startAiScreening(callId: String): CallScreeningSession = evaluateIncomingCall("+800111", null)
            override suspend fun appendTranscriptChunk(callId: String, text: String) {}
            override fun getActiveScreeningSessions(): List<CallScreeningSession> = emptyList()
            override fun dismissSession(callId: String) {}
        }

        val skill = CallScreeningSkill(fakeCall)
        val score = skill.score(dummyContext, "filtrar llamadas")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "filtrar llamadas", score)
        assertTrue(output.success)
        assertTrue(output.payload is CallScreeningUiPayload)
    }

    @Test
    fun testInterpreterSkill() = runBlocking {
        val fakeInterpreter = object : InterpreterEngine {
            override suspend fun translateTurn(speakerId: String, text: String, sourceLang: String, targetLang: String): InterpreterTurn {
                return InterpreterTurn(speakerId, text, "Hello", sourceLang, targetLang)
            }
            override fun getActiveSession(): InterpreterSession? = null
            override fun startSession(languageA: String, languageB: String): InterpreterSession {
                return InterpreterSession("s1", languageA, languageB)
            }
            override fun endSession(): Boolean = true
        }

        val skill = InterpreterSkill(fakeInterpreter)
        val score = skill.score(dummyContext, "modo interprete ingles español")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "modo interprete ingles español", score)
        assertTrue(output.success)
        assertTrue(output.payload is InterpreterUiPayload)
    }

    @Test
    fun testContextTriggerSkill() = runBlocking {
        val fakeContextEngine = object : ContextTriggerEngine {
            override suspend fun evaluateContextChanges(newState: CurrentContextState): List<ContextRule> = emptyList()
            override fun registerRule(rule: ContextRule) {}
            override fun getRegisteredRules(): List<ContextRule> = listOf(
                ContextRule("r1", "Casa", ContextTriggerType.WIFI_SSID_CONNECTED, "HomeNet", listOf("luz on"))
            )
            override fun deleteRule(ruleId: String): Boolean = true
            override fun getCurrentState(): CurrentContextState = CurrentContextState(
                currentWifiSsid = "HomeNet",
                currentGeofenceZone = "Casa"
            )
        }

        val skill = ContextTriggerSkill(fakeContextEngine)
        val score = skill.score(dummyContext, "reglas de contexto")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "reglas de contexto", score)
        assertTrue(output.success)
        assertTrue(output.payload is ContextTriggerUiPayload)
    }

    @Test
    fun testAmbientDockSkill() = runBlocking {
        val fakeDock = object : DockModeController {
            override fun notifyDockStateChanged(isDocked: Boolean, type: DockType?) {}
            override fun getCurrentDockState(): DockModeState = DockModeState(isDocked = true, dockType = DockType.NIGHTSTAND)
            override fun setNightTheme(enabled: Boolean) {}
            override fun setCustomAmbientMessage(message: String?) {}
        }

        val skill = AmbientDockSkill(fakeDock)
        val score = skill.score(dummyContext, "modo dock")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "modo dock", score)
        assertTrue(output.success)
        assertTrue(output.payload is AmbientDockUiPayload)
    }

    @Test
    fun testBatteryHealthSkill() = runBlocking {
        val fakeBattery = object : BatteryHealthMonitor {
            override fun getBatteryHealthSnapshot(): BatteryHealthSnapshot {
                return BatteryHealthSnapshot(
                    levelPercent = 82,
                    temperatureCelsius = 28.5f,
                    currentMicroAmperes = 1100000L,
                    isCharging = true,
                    thermalRating = ThermalRating.OPTIMAL
                )
            }
            override suspend fun checkShouldTriggerSmartCutoff(): Boolean = true
            override fun setSmartCutoffLimit(targetPercent: Int) {}
        }

        val skill = BatteryHealthSkill(fakeBattery)
        val score = skill.score(dummyContext, "salud de la batería")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "salud de la batería", score)
        assertTrue(output.success)
        assertTrue(output.payload is BatteryHealthUiPayload)
        assertEquals(82, (output.payload as BatteryHealthUiPayload).level)
    }

    @Test
    fun testPrivacyFirewallSkill() = runBlocking {
        val fakeFirewall = object : PrivacyFirewallEngine {
            override fun recordConnection(audit: NetworkConnectionAudit) {}
            override fun getRecentConnections(limit: Int): List<NetworkConnectionAudit> = emptyList()
            override fun getBlockedTrackersCount(): Int = 30
            override fun isFirewallActive(): Boolean = true
        }

        val skill = PrivacyFirewallSkill(fakeFirewall)
        val score = skill.score(dummyContext, "firewall de privacidad")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "firewall de privacidad", score)
        assertTrue(output.success)
        assertTrue(output.payload is PrivacyFirewallUiPayload)
        assertEquals(30, (output.payload as PrivacyFirewallUiPayload).blockedTrackersCount)
    }

    @Test
    fun testSecurityAuditSkill() = runBlocking {
        val fakeAuditor = object : DeviceSecurityAuditor {
            override suspend fun performSecurityAudit(): SecurityAuditReport {
                return SecurityAuditReport(92, 1, emptyList())
            }
            override fun getLastAuditReport(): SecurityAuditReport? = null
        }

        val skill = SecurityAuditSkill(fakeAuditor)
        val score = skill.score(dummyContext, "auditar permisos")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "auditar permisos", score)
        assertTrue(output.success)
        assertTrue(output.payload is SecurityAuditUiPayload)
        assertEquals(92, (output.payload as SecurityAuditUiPayload).securityScore)
    }

    @Test
    fun testHardwareGestureSkill() = runBlocking {
        val fakeDetector = object : HardwareGestureDetector {
            override fun startListening(onGesture: (GestureEvent) -> Unit) {}
            override fun stopListening() {}
            override fun isListening(): Boolean = true
            override fun setSensitivity(sensitivityMultiplier: Float) {}
        }

        val skill = HardwareGestureSkill(fakeDetector)
        val score = skill.score(dummyContext, "toque trasero")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "toque trasero", score)
        assertTrue(output.success)
        assertTrue(output.payload is HardwareGestureUiPayload)
    }

    @Test
    fun testCameraDirectorSkill() = runBlocking {
        val fakeCamera = object : CameraDirectorController {
            override suspend fun scheduleVoiceShutter(config: CameraCaptureConfig): CameraCaptureResult {
                return CameraCaptureResult(true, "/path/to/img.jpg", 3, "Capturada")
            }
            override fun switchLens(lens: CameraLensType): Boolean = true
            override fun toggleFlash(enabled: Boolean): Boolean = true
            override suspend fun countPeopleInFrame(): Int = 3
        }

        val skill = CameraDirectorSkill(fakeCamera)
        val score = skill.score(dummyContext, "sacar foto en 5 segundos")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "sacar foto en 5 segundos", score)
        assertTrue(output.success)
        assertTrue(output.payload is CameraDirectorUiPayload)
        assertEquals(5, (output.payload as CameraDirectorUiPayload).countdownSeconds)
    }

    @Test
    fun testHealthTelemetrySkill() = runBlocking {
        val fakeHealth = object : HealthTelemetryRepository {
            override suspend fun getTodaySnapshot(): HealthTelemetrySnapshot {
                return HealthTelemetrySnapshot(9500, 10000, 65, 8.0f, 500, "Excelente")
            }
            override suspend fun recordManualVitals(heartRate: Int?, stepsDelta: Int?) {}
            override fun isConnectedToHealthConnect(): Boolean = true
        }

        val skill = HealthTelemetrySkill(fakeHealth)
        val score = skill.score(dummyContext, "mis pasos")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "mis pasos", score)
        assertTrue(output.success)
        assertTrue(output.payload is HealthTelemetryUiPayload)
        assertEquals(9500, (output.payload as HealthTelemetryUiPayload).steps)
    }

    @Test
    fun testAutonomousPlannerSkill() = runBlocking {
        val fakePlanner = object : AutonomousPlannerEngine {
            override suspend fun decomposeGoalIntoPlan(userGoal: String): TaskPlan {
                return TaskPlan(
                    planId = "p1",
                    userGoal = userGoal,
                    steps = listOf(
                        PlanStep(1, "Revisar agenda", "ver agenda"),
                        PlanStep(2, "Silenciar llamadas", "silenciar")
                    )
                )
            }
            override suspend fun executeStep(planId: String, stepNumber: Int, stepExecutor: suspend (String) -> String): TaskPlan = error("unused")
            override fun getActivePlan(): TaskPlan? = null
            override fun cancelPlan(planId: String) {}
        }

        val skill = AutonomousPlannerSkill(fakePlanner)
        val score = skill.score(dummyContext, "planifica mi tarde")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "planifica mi tarde", score)
        assertTrue(output.success)
        assertTrue(output.payload is TaskPlanUiPayload)
        assertEquals(2, (output.payload as TaskPlanUiPayload).steps.size)
    }
}
