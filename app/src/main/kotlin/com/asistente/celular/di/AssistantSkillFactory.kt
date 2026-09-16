package com.asistente.celular.di

import android.content.Context
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.ai.harness.ModelHarness
import com.asistente.celular.ai.local.LocalInferenceEngine
import com.asistente.celular.ai.local.LocalModelManager
import com.asistente.celular.ai.rag.DefaultPersonalContextProvider
import com.asistente.celular.ai.rag.PersonalContextProvider
import com.asistente.celular.data.AndroidCalendarRepository
import com.asistente.celular.data.JsonNoteRepository
import com.asistente.celular.data.JsonRoutineRepository
import com.asistente.celular.data.JsonSemanticMemoryRepository
import com.asistente.celular.data.JsonTaskRepository
import com.asistente.celular.nlu.evaluator.SkillEvaluator
import com.asistente.celular.nlu.evaluator.SkillRanker
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.service.TaskReminderScheduler
import com.asistente.celular.skills.alarm.AlarmSkill
import com.asistente.celular.skills.applauncher.AppLauncherSkill
import com.asistente.celular.skills.calendar.CalendarSkill
import com.asistente.celular.skills.communication.PhoneCallSkill
import com.asistente.celular.skills.communication.WhatsAppSkill
import com.asistente.celular.skills.flashlight.AndroidFlashlightController
import com.asistente.celular.skills.flashlight.FlashlightController
import com.asistente.celular.skills.flashlight.FlashlightSkill
import com.asistente.celular.skills.help.HelpSkill
import com.asistente.celular.skills.media.DeepMediaSkill
import com.asistente.celular.skills.media.MediaControlSkill
import com.asistente.celular.skills.memory.SemanticMemorySkill
import com.asistente.celular.skills.notes.NotesSkill
import com.asistente.celular.skills.routines.RoutineSkill
import com.asistente.celular.skills.system.AndroidBrightnessController
import com.asistente.celular.skills.system.BrightnessController
import com.asistente.celular.skills.system.SystemSettingsSkill
import com.asistente.celular.skills.system.VolumeSkill
import com.asistente.celular.data.JsonSmartHomeRepository
import com.asistente.celular.data.JsonExpenseRepository
import com.asistente.celular.data.MediaStoreFileSearchEngine
import com.asistente.celular.driving.DrivingModeCoordinator
import com.asistente.celular.emergency.EmergencySosCoordinator
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.ocr.LocalOcrEngine
import com.asistente.celular.rhythm.SleepWakeCoordinator
import com.asistente.celular.service.HendrixAccessibilityService
import com.asistente.celular.skills.driving.DrivingModeSkill
import com.asistente.celular.skills.emergency.EmergencySosSkill
import com.asistente.celular.skills.expenses.ExpenseSkill
import com.asistente.celular.skills.files.LocalFileSearchSkill
import com.asistente.celular.skills.rhythm.SmartRhythmSkill
import com.asistente.celular.skills.smarthome.SmartHomeSkill
import com.asistente.celular.skills.tasks.TasksSkill
import com.asistente.celular.skills.time.CurrentTimeSkill
import com.asistente.celular.skills.timer.TimerSkill
import com.asistente.celular.skills.deepapp.DeepAppSkill
import com.asistente.celular.skills.math.MathSkill
import com.asistente.celular.skills.system.DeviceControlSkill
import com.asistente.celular.skills.smarthome.SmartSceneSkill
import com.asistente.celular.skills.vision.CameraGlanceSkill
import com.asistente.celular.skills.vision.ScreenUnderstandingSkill
import com.asistente.celular.biometrics.LocalVoiceBiometricsEngine
import com.asistente.celular.hardware.SensorHardwareGestureDetector
import com.asistente.celular.data.LocalKnowledgeGraphRepository
import com.asistente.celular.journal.DefaultVoiceJournalEngine
import com.asistente.celular.mesh.HendrixMeshCoordinator
import com.asistente.celular.telecom.HendrixCallScreeningCoordinator
import com.asistente.celular.translation.DualLanguageInterpreterCoordinator
import com.asistente.celular.context.GeofenceRuleCoordinator
import com.asistente.celular.ambient.DockModeCoordinator
import com.asistente.celular.battery.BatteryHealthCoordinator
import com.asistente.celular.security.LocalPrivacyFirewallCoordinator
import com.asistente.celular.security.PackageManagerSecurityAuditor
import com.asistente.celular.camera.VoiceCameraDirectorCoordinator
import com.asistente.celular.health.LocalHealthTelemetryCoordinator
import com.asistente.celular.planner.LocalAutonomousPlannerCoordinator
import com.asistente.celular.skills.biometrics.VoiceprintSkill
import com.asistente.celular.skills.rag.KnowledgeRagSkill
import com.asistente.celular.skills.journal.VoiceJournalSkill
import com.asistente.celular.skills.mesh.MeshSyncSkill
import com.asistente.celular.skills.telecom.CallScreeningSkill
import com.asistente.celular.skills.translation.InterpreterSkill
import com.asistente.celular.skills.context.ContextTriggerSkill
import com.asistente.celular.skills.ambient.AmbientDockSkill
import com.asistente.celular.skills.battery.BatteryHealthSkill
import com.asistente.celular.skills.security.PrivacyFirewallSkill
import com.asistente.celular.skills.security.SecurityAuditSkill
import com.asistente.celular.skills.gesture.HardwareGestureSkill
import com.asistente.celular.skills.camera.CameraDirectorSkill
import com.asistente.celular.skills.health.HealthTelemetrySkill
import com.asistente.celular.skills.planner.AutonomousPlannerSkill
import com.asistente.celular.automotive.HendrixCarAppCoordinator
import com.asistente.celular.wear.HendrixWearCoordinator
import com.asistente.celular.meeting.LocalMeetingRecorderCoordinator
import com.asistente.celular.ai.LocalMultiModelOrchestrator
import com.asistente.celular.documents.LocalDocumentKnowledgeCoordinator
import com.asistente.celular.soundscape.ProceduralSoundscapeCoordinator
import com.asistente.celular.voicecraft.LocalVoiceCraftStudioCoordinator
import com.asistente.celular.skills.automotive.AutomotiveSkill
import com.asistente.celular.skills.wear.WearCompanionSkill
import com.asistente.celular.skills.meeting.MeetingRecorderSkill
import com.asistente.celular.skills.ai.MultiModelOrchestratorSkill
import com.asistente.celular.skills.documents.DocumentChatSkill
import com.asistente.celular.skills.soundscape.SoundscapeSkill
import com.asistente.celular.skills.voicecraft.VoiceCraftSkill
import kotlinx.coroutines.CoroutineScope

/**
 * Factoría unificada de dependencias para el subsistema de habilidades y contexto de Hendrix.
 * Garantiza consistencia total entre ViewModel, Dialog flotante y servicios en segundo plano.
 */
class AssistantSkillFactory(
    private val context: Context,
    private val scope: CoroutineScope
) {

    val taskScheduler = TaskReminderScheduler(context)
    val taskRepository = JsonTaskRepository(context, taskScheduler, scope)
    val noteRepository = JsonNoteRepository(context, scope)
    val routineRepository = JsonRoutineRepository(context, scope)
    val semanticMemoryRepository = JsonSemanticMemoryRepository(context, scope)
    val calendarRepository = AndroidCalendarRepository(context)
    val smartHomeRepository: SmartHomeRepository = JsonSmartHomeRepository(context, scope)
    val flashlightController: FlashlightController = AndroidFlashlightController(context)
    val expenseRepository = JsonExpenseRepository(context)
    val fileSearchEngine = MediaStoreFileSearchEngine(context)
    val ocrEngine = LocalOcrEngine(context)
    val drivingModeCoordinator = DrivingModeCoordinator(context)
    val sleepWakeCoordinator = SleepWakeCoordinator(context, smartHomeRepository)
    val emergencyCoordinator = EmergencySosCoordinator(context)

    // Coordinadores de la nueva suite autónoma y de hardware
    val biometricsEngine = LocalVoiceBiometricsEngine(context)
    val hardwareGestureDetector = SensorHardwareGestureDetector(context)
    val knowledgeGraphRepository = LocalKnowledgeGraphRepository(context)
    val voiceJournalEngine = DefaultVoiceJournalEngine(context)
    val meshCoordinator = HendrixMeshCoordinator(context)
    val callScreeningCoordinator = HendrixCallScreeningCoordinator(context)
    val interpreterCoordinator = DualLanguageInterpreterCoordinator(context)
    val geofenceCoordinator = GeofenceRuleCoordinator(context)
    val dockModeCoordinator = DockModeCoordinator(context)
    val batteryHealthCoordinator = BatteryHealthCoordinator(context, smartHomeRepository)
    val privacyFirewallCoordinator = LocalPrivacyFirewallCoordinator(context)
    val securityAuditor = PackageManagerSecurityAuditor(context)
    val cameraDirectorCoordinator = VoiceCameraDirectorCoordinator(context)
    val healthTelemetryCoordinator = LocalHealthTelemetryCoordinator(context)
    val plannerCoordinator = LocalAutonomousPlannerCoordinator(context)

    // Coordinadores de grado comercial (Automotive, Wear, Meeting, MultiModel, DocumentChat, Soundscape, VoiceCraft)
    val automotiveCoordinator = HendrixCarAppCoordinator(context)
    val wearCoordinator = HendrixWearCoordinator(context)
    val meetingRecorderCoordinator = LocalMeetingRecorderCoordinator(context, biometricsEngine)
    val multiModelOrchestrator = LocalMultiModelOrchestrator(context)
    val documentKnowledgeCoordinator = LocalDocumentKnowledgeCoordinator(context, knowledgeGraphRepository)
    val soundscapeCoordinator = ProceduralSoundscapeCoordinator(context)
    val voiceCraftCoordinator = LocalVoiceCraftStudioCoordinator(context)

    val personalContextProvider: PersonalContextProvider = DefaultPersonalContextProvider(
        taskRepository = taskRepository,
        noteRepository = noteRepository,
        semanticMemoryRepository = semanticMemoryRepository,
        calendarRepository = calendarRepository
    )

    fun createSkills(
        commandExecutor: (suspend (String) -> String)? = null
    ): List<Skill> {
        return listOf(
            HelpSkill(),
            RoutineSkill(routineRepository, commandExecutor),
            SemanticMemorySkill(semanticMemoryRepository),
            CalendarSkill(calendarRepository),
            SmartSceneSkill(smartHomeRepository),
            SmartHomeSkill(smartHomeRepository),
            MathSkill(),
            DeviceControlSkill(),
            DeepMediaSkill(),
            FlashlightSkill(flashlightController),
            TimerSkill(),
            AlarmSkill(),
            AppLauncherSkill(),
            CurrentTimeSkill(),
            MediaControlSkill(),
            VolumeSkill(),
            SystemSettingsSkill(),
            PhoneCallSkill(),
            WhatsAppSkill(),
            DeepAppSkill(),
            TasksSkill(taskRepository, taskScheduler),
            NotesSkill(noteRepository),
            ScreenUnderstandingSkill(HendrixAccessibilityService.instance),
            CameraGlanceSkill(ocrEngine),
            DrivingModeSkill(drivingModeCoordinator),
            SmartRhythmSkill(sleepWakeCoordinator),
            LocalFileSearchSkill(fileSearchEngine),
            ExpenseSkill(expenseRepository),
            EmergencySosSkill(emergencyCoordinator),
            VoiceprintSkill(biometricsEngine),
            KnowledgeRagSkill(knowledgeGraphRepository),
            VoiceJournalSkill(voiceJournalEngine),
            MeshSyncSkill(meshCoordinator),
            CallScreeningSkill(callScreeningCoordinator),
            InterpreterSkill(interpreterCoordinator),
            ContextTriggerSkill(geofenceCoordinator),
            AmbientDockSkill(dockModeCoordinator),
            BatteryHealthSkill(batteryHealthCoordinator),
            PrivacyFirewallSkill(privacyFirewallCoordinator),
            SecurityAuditSkill(securityAuditor),
            HardwareGestureSkill(hardwareGestureDetector),
            CameraDirectorSkill(cameraDirectorCoordinator),
            HealthTelemetrySkill(healthTelemetryCoordinator),
            AutonomousPlannerSkill(plannerCoordinator),
            AutomotiveSkill(automotiveCoordinator),
            WearCompanionSkill(wearCoordinator),
            MeetingRecorderSkill(meetingRecorderCoordinator),
            MultiModelOrchestratorSkill(multiModelOrchestrator),
            DocumentChatSkill(documentKnowledgeCoordinator),
            SoundscapeSkill(soundscapeCoordinator),
            VoiceCraftSkill(voiceCraftCoordinator)
        )
    }

    fun createSkillEvaluator(
        skillContext: SkillContext,
        localModelManager: LocalModelManager,
        localInferenceEngine: LocalInferenceEngine,
        configProvider: () -> LlmConfig,
        onSpeak: suspend (String) -> Unit,
        onReopenMic: suspend () -> Unit = {}
    ): SkillEvaluator {
        val llmClient = LlmClient(
            localModelManager = localModelManager,
            localInferenceEngine = localInferenceEngine,
            configProvider = configProvider
        )
        val modelHarness = ModelHarness()
        val aiFallbackSkill = com.asistente.celular.ai.router.AiRouterSkill(
            llmClient = llmClient,
            modelHarness = modelHarness,
            personalContextProvider = personalContextProvider,
            configProvider = configProvider
        )

        var evaluatorRef: SkillEvaluator? = null

        val skills = createSkills(
            commandExecutor = { command ->
                val res = evaluatorRef?.processInput(command)
                res?.speech ?: ""
            }
        )

        val ranker = SkillRanker(
            skills = skills,
            fallbackSkill = aiFallbackSkill
        )

        val evaluator = SkillEvaluator(
            ranker = ranker,
            skillContext = skillContext,
            onSpeak = onSpeak,
            onReopenMic = onReopenMic
        )
        evaluatorRef = evaluator
        return evaluator
    }
}
