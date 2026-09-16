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

    // Factoría unificada de habilidades, repositorios y contexto personal
    private val factory = com.asistente.celular.di.AssistantSkillFactory(application, viewModelScope)

    val taskScheduler = factory.taskScheduler
    val taskRepository = factory.taskRepository
    val noteRepository = factory.noteRepository
    val routineRepository = factory.routineRepository
    val semanticMemoryRepository = factory.semanticMemoryRepository
    val calendarRepository = factory.calendarRepository
    val smartHomeRepository = factory.smartHomeRepository
    val personalContextProvider = factory.personalContextProvider

    val tasksState = taskRepository.tasks
    val notesState = noteRepository.notes
    val smartDevicesState = smartHomeRepository.devices
    val isScanningSmartDevices = MutableStateFlow(false)

    // Motores de Audio y Voz
    private val ttsEngine = AndroidNativeTtsEngine(
        context = application,
        pitch = 1.08f * activeLlmConfig.personality.speechPitchMultiplier,
        speechRate = 1.02f * activeLlmConfig.personality.speechRateMultiplier
    )
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
    private val evaluator = factory.createSkillEvaluator(
        skillContext = skillContext,
        localModelManager = localModelManager,
        localInferenceEngine = localInferenceEngine,
        configProvider = { activeLlmConfig },
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
                val msg = err.message ?: ""
                val friendlyError = if (
                    msg.contains("palabra clara") ||
                    msg.contains("tiempo agotado") ||
                    msg.contains("desconectó temporalmente") ||
                    msg.contains("11")
                ) {
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
        ttsEngine.pitch = 1.08f * newConfig.personality.speechPitchMultiplier
        ttsEngine.speechRate = 1.02f * newConfig.personality.speechRateMultiplier
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
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

    // Operaciones de Domótica / Foco Xiaomi
    fun discoverSmartDevices() {
        viewModelScope.launch {
            isScanningSmartDevices.value = true
            try {
                smartHomeRepository.discoverDevices()
            } finally {
                isScanningSmartDevices.value = false
            }
        }
    }

    fun addManualSmartDevice(name: String, ip: String) {
        viewModelScope.launch {
            val cleanIp = ip.trim()
            val device = com.asistente.celular.nlu.smarthome.SmartDevice(
                id = "manual_${cleanIp.replace(".", "_")}",
                name = name.ifBlank { "Foco Xiaomi" },
                aliases = listOf("foco", "luz", "foco xiaomi", "bombilla", "foco cuarto", "luz cuarto", "cuarto", "sala"),
                ipAddress = cleanIp,
                port = 55443,
                protocol = com.asistente.celular.nlu.smarthome.SmartProtocol.YEELIGHT_LAN
            )
            smartHomeRepository.addOrUpdateDevice(device)
            // Sincronizar estado inicial (encendido/brillo) en segundo plano
            smartHomeRepository.executeAction(device.id, com.asistente.celular.nlu.smarthome.DeviceAction.Custom("get_prop", listOf("power", "bright", "name")))
        }
    }

    fun deleteSmartDevice(id: String) {
        viewModelScope.launch {
            smartHomeRepository.removeDevice(id)
        }
    }

    fun toggleSmartDevice(device: com.asistente.celular.nlu.smarthome.SmartDevice) {
        viewModelScope.launch {
            smartHomeRepository.executeAction(device.id, com.asistente.celular.nlu.smarthome.DeviceAction.Toggle)
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
