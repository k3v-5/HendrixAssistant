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
            EmergencySosSkill(emergencyCoordinator)
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
