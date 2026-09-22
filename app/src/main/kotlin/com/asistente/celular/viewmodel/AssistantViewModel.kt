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
import com.asistente.celular.hardware.ShakeSensitivity
import com.asistente.celular.voice.SttEngine
import com.asistente.celular.voice.kws.SherpaOnnxKwsEngine
import com.asistente.celular.voice.kws.WakeWordSensitivity
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
    val wakeWordSensitivity: WakeWordSensitivity = WakeWordSensitivity.MEDIUM,
    val isShakeToWakeEnabled: Boolean = false,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.NORMAL,
    val isPocketSilenceEnabled: Boolean = true,
    val isFlipToMuteEnabled: Boolean = true,
    val ttsPitch: Float = 1.08f,
    val ttsSpeechRate: Float = 1.02f,
    val smartHomeCustomSubnet: String? = null,
    val isOverlayEnabled: Boolean = true,
    val sttEngineType: com.asistente.celular.voice.stt.SttEngineType = com.asistente.celular.voice.stt.SttEngineType.ANDROID_SYSTEM,
    val offlineAsrModelId: String = "sherpa_onnx_zipformer_es",
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

    // Coordinador de Espacio de Trabajo Remoto en PC y Gestor de Módulos
    val pcRemoteCoordinator = factory.pcRemoteCoordinator
    val pcModuleManager = com.asistente.celular.pc.module.PcModuleManager(application)
    val automatedRoutineRepository = factory.automatedRoutineRepository
    val macroDeckProfileRepository = factory.macroDeckProfileRepository
    val personalRagCoordinator = factory.personalRagCoordinator

    private val _isDeskStandbyActive = MutableStateFlow(false)
    val isDeskStandbyActive: StateFlow<Boolean> = _isDeskStandbyActive.asStateFlow()

    // Coordinador de Descubrimiento Zero-Config UDP / mDNS
    val pcDiscoveryCoordinator = com.asistente.celular.pc.discovery.PcDiscoveryCoordinator(
        context = application,
        scope = viewModelScope,
        settingsRepo = settingsRepo,
        pcRemoteCoordinator = pcRemoteCoordinator
    )

    // Gestor de descarga y almacenamiento de modelos acústicos ASR offline
    val offlineAsrModelManager: com.asistente.celular.voice.stt.OfflineAsrModelManager =
        com.asistente.celular.voice.stt.DefaultOfflineAsrModelManager(application, viewModelScope)

    // Repositorio de la Bóveda Hendrix (Backup & Restore)
    val vaultRepository = com.asistente.celular.vault.HendrixVaultRepository(application)

    // Motores de Audio y Voz
    private val ttsEngine = AndroidNativeTtsEngine(
        context = application,
        pitch = 1.08f * activeLlmConfig.personality.speechPitchMultiplier,
        speechRate = 1.02f * activeLlmConfig.personality.speechRateMultiplier
    )
    // Motor STT desacoplado mediante factoría
    private val sttEngine: SttEngine = com.asistente.celular.voice.stt.SttEngineFactory.createEngine(
        context = application,
        type = settingsRepo.sttEngineType
    )

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
        val basePitch = settingsRepo.ttsPitch
        val baseRate = settingsRepo.ttsSpeechRate
        ttsEngine.updateVoiceParameters(
            basePitch * activeLlmConfig.personality.speechPitchMultiplier,
            baseRate * activeLlmConfig.personality.speechRateMultiplier
        )
        _uiState.value = _uiState.value.copy(
            isConnected = checkInternetConnection(),
            llmConfig = activeLlmConfig,
            isWakeWordActive = isWakeActive,
            wakeWordSensitivity = settingsRepo.wakeWordSensitivity,
            isShakeToWakeEnabled = settingsRepo.isShakeToWakeEnabled,
            shakeSensitivity = settingsRepo.shakeSensitivity,
            isPocketSilenceEnabled = settingsRepo.isPocketSilenceEnabled,
            isFlipToMuteEnabled = settingsRepo.isFlipToMuteEnabled,
            ttsPitch = basePitch,
            ttsSpeechRate = baseRate,
            smartHomeCustomSubnet = settingsRepo.smartHomeCustomSubnet,
            isOverlayEnabled = settingsRepo.isOverlayEnabled,
            sttEngineType = settingsRepo.sttEngineType,
            offlineAsrModelId = settingsRepo.offlineAsrModelId
        )

        // Registrar callback de activación de Desk Standby desde habilidades y rutinas
        factory.onEnterDeskStandby = {
            _isDeskStandbyActive.value = true
        }

        // Iniciar descubrimiento Zero-Config en la red local
        pcDiscoveryCoordinator.startDiscovery()

        // Sincronizar automáticamente Notas y Tareas con el motor RAG vectorial
        viewModelScope.launch {
            noteRepository.notes.collect { notes ->
                personalRagCoordinator.indexNotes(notes)
            }
        }
        viewModelScope.launch {
            taskRepository.tasks.collect { tasks ->
                personalRagCoordinator.indexTasks(tasks)
            }
        }
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
                if (output.payload == "ACTION_DESK_STANDBY") {
                    _isDeskStandbyActive.value = true
                }
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

    fun enterDeskStandby() {
        _isDeskStandbyActive.value = true
    }

    fun exitDeskStandby() {
        _isDeskStandbyActive.value = false
    }

    fun updateLlmConfig(newConfig: LlmConfig) {
        activeLlmConfig = newConfig
        settingsRepo.saveLlmConfig(newConfig)
        val pitch = settingsRepo.ttsPitch * newConfig.personality.speechPitchMultiplier
        val rate = settingsRepo.ttsSpeechRate * newConfig.personality.speechRateMultiplier
        ttsEngine.updateVoiceParameters(pitch, rate)
        _uiState.value = _uiState.value.copy(llmConfig = newConfig)
    }

    fun toggleWakeWord(enable: Boolean) {
        settingsRepo.isWakeWordActive = enable
        _uiState.value = _uiState.value.copy(isWakeWordActive = enable)
    }

    fun updateWakeWordSensitivity(sensitivity: WakeWordSensitivity) {
        settingsRepo.wakeWordSensitivity = sensitivity
        _uiState.value = _uiState.value.copy(wakeWordSensitivity = sensitivity)
    }

    fun toggleShakeToWake(enable: Boolean) {
        settingsRepo.isShakeToWakeEnabled = enable
        _uiState.value = _uiState.value.copy(isShakeToWakeEnabled = enable)
    }

    fun updateShakeSensitivity(sensitivity: ShakeSensitivity) {
        settingsRepo.shakeSensitivity = sensitivity
        _uiState.value = _uiState.value.copy(shakeSensitivity = sensitivity)
    }

    fun updateTtsParameters(pitch: Float, rate: Float) {
        settingsRepo.ttsPitch = pitch
        settingsRepo.ttsSpeechRate = rate
        val finalPitch = pitch * _uiState.value.llmConfig.personality.speechPitchMultiplier
        val finalRate = rate * _uiState.value.llmConfig.personality.speechRateMultiplier
        ttsEngine.updateVoiceParameters(finalPitch, finalRate)
        _uiState.value = _uiState.value.copy(ttsPitch = pitch, ttsSpeechRate = rate)
    }

    fun testTtsVoice() {
        viewModelScope.launch {
            ttsEngine.speak("Hola, soy Hendrix. Así suena mi voz con la velocidad y tono configurados.")
        }
    }

    fun updateSmartHomeCustomSubnet(subnet: String?) {
        settingsRepo.smartHomeCustomSubnet = subnet
        _uiState.value = _uiState.value.copy(smartHomeCustomSubnet = subnet)
    }

    fun togglePocketSilence(enable: Boolean) {
        settingsRepo.isPocketSilenceEnabled = enable
        _uiState.value = _uiState.value.copy(isPocketSilenceEnabled = enable)
    }

    fun toggleFlipToMute(enable: Boolean) {
        settingsRepo.isFlipToMuteEnabled = enable
        _uiState.value = _uiState.value.copy(isFlipToMuteEnabled = enable)
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
    fun discoverSmartDevices(customSubnet: String? = settingsRepo.smartHomeCustomSubnet) {
        viewModelScope.launch {
            isScanningSmartDevices.value = true
            try {
                smartHomeRepository.discoverDevices(timeoutMillis = 2500L, customSubnetPrefix = customSubnet)
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

    fun toggleOverlayEnabled(enabled: Boolean) {
        settingsRepo.isOverlayEnabled = enabled
        _uiState.value = _uiState.value.copy(isOverlayEnabled = enabled)
    }

    fun updateSttEngine(type: com.asistente.celular.voice.stt.SttEngineType) {
        settingsRepo.sttEngineType = type
        _uiState.value = _uiState.value.copy(sttEngineType = type)
    }

    fun startAsrModelDownload(modelId: String) {
        offlineAsrModelManager.startDownload(modelId)
    }

    fun deleteAsrModel(modelId: String) {
        offlineAsrModelManager.deleteModel(modelId)
    }

    fun backupVaultToPc(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val json = vaultRepository.exportVaultJson()
            val success = pcRemoteCoordinator.backupVault(json)
            onResult(success)
        }
    }

    fun restoreVaultFromPc(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val json = pcRemoteCoordinator.restoreVault()
            if (json != null) {
                val ok = vaultRepository.restoreVaultJson(json)
                onResult(ok)
            } else {
                onResult(false)
            }
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
        pcDiscoveryCoordinator.stopDiscovery()
        com.asistente.celular.util.MicCoordinator.releaseMicLock("MainActivity")
        sttEngine.release()
        ttsEngine.release()
        localInferenceEngine.unload()
    }
}
