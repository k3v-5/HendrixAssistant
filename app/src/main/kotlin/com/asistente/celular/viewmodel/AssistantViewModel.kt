package com.asistente.celular.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.harness.ModelHarness
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.ai.router.AiRouterSkill
import com.asistente.celular.data.SettingsRepository
import com.asistente.celular.util.HapticFeedbackManager
import com.asistente.celular.nlu.evaluator.InteractionEntry
import com.asistente.celular.nlu.evaluator.SkillEvaluator
import com.asistente.celular.nlu.evaluator.SkillRanker
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.alarm.AlarmSkill
import com.asistente.celular.skills.applauncher.AppLauncherSkill
import com.asistente.celular.skills.flashlight.FlashlightSkill
import com.asistente.celular.skills.help.HelpSkill
import com.asistente.celular.skills.media.MediaControlSkill
import com.asistente.celular.skills.time.CurrentTimeSkill
import com.asistente.celular.skills.timer.TimerSkill
import com.asistente.celular.voice.SttEngine
import com.asistente.celular.voice.kws.SherpaOnnxKwsEngine
import com.asistente.celular.voice.stt.AndroidSpeechRecognizerEngine
import com.asistente.celular.voice.tts.AndroidNativeTtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado general para la interfaz de usuario en Compose.
 */
data class AssistantUiState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialText: String = "",
    val interactions: List<InteractionEntry> = emptyList(),
    val llmConfig: LlmConfig = LlmConfig(),
    val isWakeWordActive: Boolean = false,
    val isConnected: Boolean = true,
    val error: String? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val hapticManager = HapticFeedbackManager(application)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    // Gestor de descarga y almacenamiento de modelos locales (SLM/GGUF)
    val localModelManager: com.asistente.celular.ai.local.LocalModelManager =
        com.asistente.celular.ai.local.DefaultLocalModelManager(application)

    // Motor de Inferencia Nativo llama.cpp on-device
    val localInferenceEngine: com.asistente.celular.ai.local.LocalInferenceEngine =
        com.asistente.celular.ai.local.LlamaCppInferenceEngine()

    // Configuración persistida de IA
    private var activeLlmConfig: LlmConfig = settingsRepo.loadLlmConfig()

    // Subconjunto de Tareas y Notas (Google Tasks & Google Keep offline-first)
    val taskScheduler: com.asistente.celular.service.TaskReminderScheduler =
        com.asistente.celular.service.TaskReminderScheduler(application)

    val taskRepository: com.asistente.celular.nlu.tasks.TaskRepository =
        com.asistente.celular.data.JsonTaskRepository(application, taskScheduler, viewModelScope)

    val noteRepository: com.asistente.celular.nlu.notes.NoteRepository =
        com.asistente.celular.data.JsonNoteRepository(application, viewModelScope)

    val tasksState = taskRepository.tasks
    val notesState = noteRepository.notes

    // Proveedor de Contexto Personal y Memoria (RAG Local)
    val personalContextProvider: com.asistente.celular.ai.rag.PersonalContextProvider =
        com.asistente.celular.ai.rag.DefaultPersonalContextProvider(taskRepository, noteRepository)

    // Cliente IA y Model Routing Harness
    private val llmClient = LlmClient(
        localModelManager = localModelManager,
        localInferenceEngine = localInferenceEngine,
        configProvider = { activeLlmConfig }
    )
    private val modelHarness = ModelHarness()
    private val aiFallbackSkill = AiRouterSkill(
        llmClient = llmClient,
        modelHarness = modelHarness,
        personalContextProvider = personalContextProvider,
        configProvider = { activeLlmConfig }
    )

    // Habilidades locales del dispositivo (PNL Offline)
    private val localSkills = listOf(
        HelpSkill(),
        FlashlightSkill(),
        TimerSkill(),
        AlarmSkill(),
        AppLauncherSkill(),
        CurrentTimeSkill(),
        MediaControlSkill(),
        com.asistente.celular.skills.system.VolumeSkill(),
        com.asistente.celular.skills.system.SystemSettingsSkill(),
        com.asistente.celular.skills.communication.PhoneCallSkill(),
        com.asistente.celular.skills.communication.WhatsAppSkill(),
        com.asistente.celular.skills.tasks.TasksSkill(taskRepository, taskScheduler),
        com.asistente.celular.skills.notes.NotesSkill(noteRepository)
    )

    private val skillRanker = SkillRanker(
        skills = localSkills,
        fallbackSkill = aiFallbackSkill
    )

    // Motores de Audio y Voz
    private val ttsEngine = AndroidNativeTtsEngine(application)
    // Motor STT nativo funcional inmediato
    private val sttEngine: SttEngine = AndroidSpeechRecognizerEngine(application)

    // Contexto de ejecución para las habilidades
    private val skillContext = object : SkillContext {
        override val androidContext: Context get() = getApplication<Application>()
        override val isConnectedToInternet: Boolean get() = checkInternetConnection()
        override val previousOutput: SkillOutput?
            get() = _uiState.value.interactions.lastOrNull()?.output
    }

    // Evaluador y orquestador
    private val evaluator = SkillEvaluator(
        ranker = skillRanker,
        skillContext = skillContext,
        onSpeak = { text ->
            _uiState.value = _uiState.value.copy(isSpeaking = true)
            try {
                ttsEngine.speak(text)
            } finally {
                _uiState.value = _uiState.value.copy(isSpeaking = false)
            }
        },
        onReopenMic = {
            startListening()
        }
    )

    init {
        val isWakeActive = settingsRepo.isWakeWordActive
        _uiState.value = _uiState.value.copy(
            isConnected = checkInternetConnection(),
            llmConfig = activeLlmConfig,
            isWakeWordActive = isWakeActive
        )
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        _uiState.value = _uiState.value.copy(isListening = true, partialText = "", error = null)

        // Pausar el servicio de fondo para tomar posesión limpia del micrófono
        com.asistente.celular.util.MicCoordinator.acquireMicLock("MainActivity")

        hapticManager.vibrateStartListening()

        sttEngine.startListening(
            onPartialResult = { partial ->
                _uiState.value = _uiState.value.copy(partialText = partial)
            },
            onFinalResult = { final ->
                com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivity")
                _uiState.value = _uiState.value.copy(isListening = false, partialText = "")
                processCommand(final)
            },
            onError = { err ->
                com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivity")
                hapticManager.vibrateError()
                val friendlyError = if (err.message?.contains("palabra clara") == true || err.message?.contains("tiempo agotado") == true) {
                    "No te escuché con claridad. Toca el micrófono para intentar de nuevo."
                } else {
                    err.message
                }
                _uiState.value = _uiState.value.copy(isListening = false, error = friendlyError)
            }
        )
    }

    fun stopListening() {
        sttEngine.stopListening()
        com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivity")
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    private var lastProcessedCommand: String = ""
    private var lastProcessedTimestamp: Long = 0L

    fun processCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isBlank()) return

        val now = System.currentTimeMillis()
        // Evitar duplicados o ecos parciales generados en ráfaga por el reconocedor de voz
        if ((now - lastProcessedTimestamp < 2500) &&
            (trimmed.equals(lastProcessedCommand, ignoreCase = true) ||
             lastProcessedCommand.startsWith(trimmed, ignoreCase = true) ||
             trimmed.startsWith(lastProcessedCommand, ignoreCase = true))) {
            android.util.Log.w("AssistantViewModel", "Ignorando comando duplicado/eco en ráfaga: '$trimmed'")
            return
        }

        lastProcessedCommand = trimmed
        lastProcessedTimestamp = now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val output = evaluator.processInput(commandText)
                if (output.success) {
                    hapticManager.vibrateSuccess()
                } else {
                    hapticManager.vibrateError()
                }
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    interactions = evaluator.state.value.history
                )
            } catch (e: Exception) {
                hapticManager.vibrateError()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.localizedMessage
                )
            }
        }
    }

    fun updateLlmConfig(newConfig: LlmConfig) {
        activeLlmConfig = newConfig
        settingsRepo.saveLlmConfig(newConfig)
        _uiState.value = _uiState.value.copy(llmConfig = newConfig)
    }

    fun toggleWakeWord(enable: Boolean) {
        settingsRepo.isWakeWordActive = enable
        _uiState.value = _uiState.value.copy(isWakeWordActive = enable)
    }

    fun startModelDownload(modelId: String) {
        localModelManager.startDownload(modelId)
    }

    fun cancelModelDownload(modelId: String) {
        localModelManager.cancelDownload(modelId)
    }

    fun deleteLocalModel(modelId: String) {
        if (localInferenceEngine.loadedModelId == modelId) {
            localInferenceEngine.unload()
        }
        localModelManager.deleteModel(modelId)
    }

    fun selectLocalModel(modelId: String) {
        settingsRepo.activeLocalModelId = modelId
        if (localInferenceEngine.loadedModelId != modelId) {
            localInferenceEngine.unload()
        }
        if (activeLlmConfig.provider == com.asistente.celular.ai.model.AiProvider.LOCAL_SLM) {
            updateLlmConfig(activeLlmConfig.copy(modelName = modelId))
        }
    }

    fun stopSpeech() {
        ttsEngine.stop()
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }

    // Operaciones de Tareas
    fun addTask(task: com.asistente.celular.nlu.tasks.TaskItem) {
        viewModelScope.launch {
            taskRepository.addTask(task)
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.clearCompletedTasks()
        }
    }

    // Operaciones de Notas
    fun addNote(note: com.asistente.celular.nlu.notes.NoteItem) {
        viewModelScope.launch {
            noteRepository.addNote(note)
        }
    }

    fun updateNote(note: com.asistente.celular.nlu.notes.NoteItem) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun toggleNotePin(noteId: String) {
        viewModelScope.launch {
            noteRepository.togglePin(noteId)
        }
    }

    private fun checkInternetConnection(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivity")
        sttEngine.release()
        ttsEngine.release()
        localInferenceEngine.unload()
    }
}
