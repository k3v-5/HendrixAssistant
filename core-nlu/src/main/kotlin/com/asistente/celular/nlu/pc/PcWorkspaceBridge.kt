package com.asistente.celular.nlu.pc

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato principal para la comunicación bidireccional entre Hendrix Assistant y la PC.
 * Desacopla por completo la capa de presentación y NLU de la implementación de transporte
 * (WebSocket local, HTTP, o túneles P2P seguros).
 */
interface PcWorkspaceBridge {

    /**
     * Flujo reactivo con el modo de operación actualmente activo.
     */
    val currentMode: StateFlow<PcOperationMode>

    /**
     * Flujo reactivo con la última telemetría del sistema recibida de la PC.
     */
    val telemetry: StateFlow<PcSystemTelemetry?>

    /**
     * Flujo reactivo con el último fotograma de snapshot (WebP / Bitmap en bytes).
     */
    val latestSnapshot: StateFlow<ByteArray?>

    /**
     * Indica si existe una sesión activa y autenticada con la PC.
     */
    val isConnected: StateFlow<Boolean>

    /**
     * Flujo reactivo con eventos de nuevos archivos completados en el Dropzone de la PC.
     */
    val dropzoneEvents: StateFlow<com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con el último contenido conocido del portapapeles de la PC.
     */
    val clipboardState: StateFlow<com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con el estado actual del mezclador de audio por aplicación de Windows.
     */
    val audioMixerState: StateFlow<com.asistente.celular.nlu.pc.audio.PcAudioMixerState?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con la telemetría detallada de hardware (GPU, VRAM, CPU, Temperatura).
     */
    val hardwareTelemetry: StateFlow<com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con eventos emitidos por el Watchdog de renderizado.
     */
    val watchdogEvents: StateFlow<com.asistente.celular.nlu.pc.hardware.PcRenderWatchdogEvent?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con alertas proactivas emitidas por el Centinela de la PC.
     */
    val proactiveAlerts: StateFlow<com.asistente.celular.nlu.pc.alert.PcProactiveAlert?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con el estado del streaming de audio inalámbrico desde la PC.
     */
    val audioStreamState: StateFlow<com.asistente.celular.nlu.pc.audio.PcAudioStreamState>
        get() = kotlinx.coroutines.flow.MutableStateFlow(com.asistente.celular.nlu.pc.audio.PcAudioStreamState())

    /**
     * Flujo reactivo con el nombre del ejecutable o identificador de la aplicación en primer plano en la PC.
     */
    val foregroundApp: StateFlow<String>
        get() = kotlinx.coroutines.flow.MutableStateFlow("")

    /**
     * Flujo reactivo con alertas de errores de compilación o ejecución detectados en la terminal de la PC.
     */
    val terminalErrorAlerts: StateFlow<com.asistente.celular.nlu.pc.workspace.PcTerminalErrorAlert?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(null)

    /**
     * Flujo reactivo con el listado y progreso de transferencias P2P de Hendrix AirSync.
     */
    val airSyncTransfers: StateFlow<List<com.asistente.celular.nlu.pc.airsync.AirSyncTransfer>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    /**
     * Flujo reactivo con los archivos compartidos disponibles en la PC para AirSync.
     */
    val airSyncSharedFiles: StateFlow<List<com.asistente.celular.nlu.pc.airsync.AirSyncSharedFile>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    /**
     * Cambia el modo de operación de la conexión remota.
     */
    suspend fun setOperationMode(mode: PcOperationMode): Boolean

    /**
     * Solicita una captura de pantalla bajo demanda.
     * @param cropToActiveWindow Si es true, la PC recorta el área a la ventana en primer plano.
     */
    suspend fun requestSnapshot(cropToActiveWindow: Boolean = false): ByteArray?

    /**
     * Envía una acción de interacción física (clic, arrastre, scroll, tecla o atajo).
     */
    suspend fun sendInteraction(action: PcInteractionAction): Boolean

    /**
     * Escribe un texto arbitrario en el foco activo de la PC (dictado por voz).
     */
    suspend fun typeTextDirectly(text: String): Boolean

    /**
     * Ejecuta una acción semántica rápida del sistema en la PC.
     * @param command Acción admitida: "shutdown", "sleep", "lock", "restart", "volume_up",
     * "volume_down", "volume_mute", "media_play_pause", "media_next", "media_prev",
     * o lanzamiento de programa "launch:<app_name>".
     */
    suspend fun executeQuickCommand(command: String): Boolean

    /**
     * Genera un plan de tarea autónoma para un objetivo de lenguaje natural (RPA/UIA).
     */
    suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan

    /**
     * Ejecuta un plan de tarea autónoma previamente generado y aprobado por el usuario.
     */
    suspend fun executeApprovedPlan(planId: String): Boolean

    /**
     * Consulta la telemetría actual de la PC de forma síncrona/suspendible.
     */
    suspend fun queryTelemetry(): PcSystemTelemetry?

    /**
     * Conecta o reconecta con el host especificado usando token o PIN.
     */
    suspend fun connect(host: String, port: Int = 8899, pin: String? = null): Boolean

    /**
     * Desconecta la sesión activa.
     */
    suspend fun disconnect()

    /**
     * Consulta la lista de proyectos y espacios de trabajo registrados en Antigravity.
     */
    suspend fun queryAntigravityProjects(): List<AntigravityProject> = emptyList()

    /**
     * Consulta las conversaciones recientes de Antigravity para un proyecto específico o globales.
     */
    suspend fun queryAntigravityChats(projectId: String? = null): List<AntigravityChat> = emptyList()

    /**
     * Ejecuta una acción en Antigravity (abrir, conmutar proyecto, redactar en chat nuevo o existente).
     */
    suspend fun executeAntigravityAction(
        mode: AntigravityTargetMode,
        projectId: String? = null,
        conversationId: String? = null,
        prompt: String? = null
    ): Boolean = true

    /**
     * Ejecuta una acción de producción musical o gestión de sesión en el DAW objetivo.
     */
    suspend fun executeDawAction(
        request: com.asistente.celular.nlu.pc.daw.DawActionRequest
    ): com.asistente.celular.nlu.pc.daw.DawActionResult = com.asistente.celular.nlu.pc.daw.DawActionResult(success = true)

    /**
     * Consulta los proyectos de audio recientes encontrados para el DAW especificado.
     */
    suspend fun queryDawProjects(
        dawType: com.asistente.celular.nlu.pc.daw.DawType = com.asistente.celular.nlu.pc.daw.DawType.ABLETON_LIVE
    ): List<com.asistente.celular.nlu.pc.daw.DawProjectInfo> = emptyList()

    /**
     * Ejecuta una acción rápida de un módulo o plugin (Ableton, FL Studio, Adobe, Blender, Unreal).
     */
    suspend fun executeModuleAction(
        request: com.asistente.celular.nlu.pc.module.PcModuleActionRequest
    ): com.asistente.celular.nlu.pc.module.PcModuleActionResult =
        com.asistente.celular.nlu.pc.module.PcModuleActionResult(success = true)

    /**
     * Consulta el estado actual, proveedor de nube y archivos recientes del Dropzone en la PC.
     */
    suspend fun queryDropzoneInfo(): com.asistente.celular.nlu.pc.dropzone.PcDropzoneInfo? = null

    /**
     * Actualiza la ruta raíz del Dropzone en la PC.
     */
    suspend fun setDropzonePath(path: String): Boolean = true

    /**
     * Emite un paquete mágico Wake-on-LAN (WOL) para encender o reactivar la PC.
     * @param macAddress Dirección MAC opcional (si es null, usa la configurada o conocida).
     * @param broadcastIp IP de difusión en la subred (por defecto 255.255.255.255).
     */
    suspend fun wakeOnLan(macAddress: String? = null, broadcastIp: String? = null): Boolean = false

    /**
     * Desbloquea la sesión de Windows enviando el PIN a la pantalla de bloqueo (Winlogon).
     */
    suspend fun unlockSession(pin: String = ""): Boolean = false

    /**
     * Obtiene el contenido actual del portapapeles de la computadora.
     */
    suspend fun getClipboard(): com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload? = null

    /**
     * Establece el contenido del portapapeles de la PC y opcionalmente pega de inmediato con Ctrl+V.
     * Retorna el payload con el hash SHA-256 verificado y el recuento de caracteres.
     */
    suspend fun setClipboard(text: String, pasteImmediately: Boolean = false): com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload? = null

    /**
     * Consulta el estado del mezclador de audio de Windows (volumen maestro y faders por app).
     */
    suspend fun queryAudioMixer(): com.asistente.celular.nlu.pc.audio.PcAudioMixerState? = null

    /**
     * Establece el volumen de una aplicación específica en la PC (0 a 100).
     */
    suspend fun setAppVolume(processName: String, volumePercent: Int): Boolean = false

    /**
     * Alterna el estado de silencio (Mute) de una aplicación específica en la PC.
     */
    suspend fun setAppMute(processName: String, isMuted: Boolean): Boolean = false

    /**
     * Establece el volumen maestro de Windows (0 a 100).
     */
    suspend fun setMasterVolume(volumePercent: Int): Boolean = false

    /**
     * Alterna el estado de silencio (Mute) maestro de Windows.
     */
    suspend fun setMasterMute(isMuted: Boolean): Boolean = false

    /**
     * Ejecuta una macro encadenada o escena de estudio multi-paso.
     */
    suspend fun executeStudioScene(
        sceneId: String
    ): com.asistente.celular.nlu.pc.scene.PcSceneExecutionResult =
        com.asistente.celular.nlu.pc.scene.PcSceneExecutionResult(
            sceneId = sceneId,
            success = true,
            stepsExecuted = 1,
            totalSteps = 1,
            message = "Escena simulada en modo desacoplado"
        )

    /**
     * Consulta la lista de proyectos creativos indexados en la computadora.
     */
    suspend fun queryRemoteProjects(
        category: com.asistente.celular.nlu.pc.project.PcProjectCategory? = null,
        query: String? = null
    ): List<com.asistente.celular.nlu.pc.project.PcProjectItem> = emptyList()

    /**
     * Lanza o abre un archivo de proyecto (.als, .flp, .blend, .prproj, .uproject) en la PC.
     */
    suspend fun launchRemoteProject(projectPath: String): Boolean = false

    /**
     * Consulta la telemetría detallada de hardware (GPU NVIDIA/Intel/AMD, VRAM, CPU, Temperatura).
     */
    suspend fun queryHardwareTelemetry(): com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry? = null

    /**
     * Inicia el vigilante de render para alertar al finalizar y opcionalmente suspender la PC.
     */
    suspend fun startRenderWatchdog(
        processName: String = "blender",
        autoSuspend: Boolean = true
    ): Boolean = false

    /**
     * Captura la pantalla/ventana activa de la PC y ejecuta un análisis multimodal con IA (visión).
     */
    suspend fun analyzeScreenWithAi(
        prompt: String,
        cropToActiveWindow: Boolean = true
    ): com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult =
        com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult(
            success = false,
            analysisMarkdown = "No conectado",
            errorSummary = "Sin conexión con la PC"
        )

    /**
     * Consulta la lista de plugins de usuario instalados y activos en Hendrix Desktop.
     */
    suspend fun queryCustomPlugins(): List<com.asistente.celular.nlu.pc.plugin.PcPluginDefinition> = emptyList()

    /**
     * Ejecuta una acción de un plugin personalizado de usuario en Hendrix Desktop.
     */
    suspend fun executeCustomPluginAction(
        pluginId: String,
        actionId: String,
        params: Map<String, Any> = emptyMap()
    ): com.asistente.celular.nlu.pc.plugin.PcPluginActionResult =
        com.asistente.celular.nlu.pc.plugin.PcPluginActionResult(
            pluginId = pluginId,
            actionId = actionId,
            success = false,
            message = "Sin conexión con la PC"
        )

    /**
     * Inicia el streaming de audio en tiempo real desde la PC (WASAPI Loopback).
     */
    suspend fun startAudioMonitoring(sampleRate: Int = 24000): Boolean = false

    /**
     * Detiene el streaming de audio en tiempo real desde la PC.
     */
    suspend fun stopAudioMonitoring(): Boolean = false

    /**
     * Dispara una alerta proactiva de prueba simulada desde la PC.
     */
    suspend fun triggerTestAlert(category: String = "GPU_OVERHEAT"): Boolean = false

    /**
     * Consulta el contexto del espacio de trabajo en la PC (proceso activo, repos Git, procesos creativos y errores de terminal).
     */
    suspend fun queryWorkspaceContext(): com.asistente.celular.nlu.pc.workspace.PcWorkspaceContext? = null

    /**
     * Consulta los archivos indexados y compartidos para Hendrix AirSync en la PC.
     */
    suspend fun queryAirSyncSharedFiles(): List<com.asistente.celular.nlu.pc.airsync.AirSyncSharedFile> = emptyList()

    /**
     * Solicita a la PC que prepare y comparta un archivo por AirSync a través de su ruta local.
     */
    suspend fun shareAirSyncFile(filePath: String): Boolean = false

    /**
     * Inicia la descarga P2P LAN de un archivo desde la PC hacia el dispositivo móvil.
     */
    suspend fun downloadAirSyncFile(fileId: String, destinationPath: String): Boolean = false

    /**
     * Simula un error de compilación/terminal en la PC para pruebas del centinela de diagnóstico.
     */
    suspend fun simulateTerminalError(
        command: String = "gradlew build",
        errorMessage: String = "Compilation error in MainActivity.kt"
    ): Boolean = false

    /**
     * Sube un archivo desde el dispositivo móvil hacia la PC por streaming P2P TCP (Hendrix AirSync),
     * con soporte opcional para inyección directa en portapapeles y auto-pegado (Lens-to-Workspace).
     */
    suspend fun uploadAirSyncFile(
        file: java.io.File,
        options: com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions = com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions()
    ): Boolean = false

    /**
     * Termina / fuerza el cierre de un proceso por nombre en la PC.
     */
    suspend fun killProcess(processName: String): Boolean = false

    /**
     * Ejecuta comandos de disposición de ventanas en la PC (MINIMIZE_ALL, TOGGLE_MAXIMIZE, etc.).
     */
    suspend fun executeWindowCommand(action: String): Boolean = false

    /**
     * Consulta la salud del hardware de la PC (CPU %, RAM, GPU temp, VRAM).
     */
    suspend fun queryHardwareHealth(): com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry? = null

    /**
     * Envía un respaldo de la Bóveda Hendrix para guardarse en la PC.
     */
    suspend fun backupVault(vaultJson: String): Boolean = false

    /**
     * Lista los nombres de los respaldos disponibles en la PC.
     */
    suspend fun listVaultBackups(): List<String> = emptyList()

    /**
     * Descarga y obtiene el JSON de un respaldo de bóveda guardado en la PC.
     */
    suspend fun restoreVault(filename: String? = null): String? = null

    /**
     * Obtiene la lista de aplicaciones abiertas visibles en la barra de tareas de Windows.
     */
    suspend fun getOpenWindows(): List<PcWindowInfo> = emptyList()

    /**
     * Trae al primer plano y restaura la ventana de Windows con el identificador [hwnd] especificado.
     */
    suspend fun focusWindow(hwnd: Long): Boolean = false
}
