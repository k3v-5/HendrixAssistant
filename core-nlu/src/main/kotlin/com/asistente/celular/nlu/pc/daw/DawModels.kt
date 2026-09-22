package com.asistente.celular.nlu.pc.daw

/**
 * Tipos de Estación de Trabajo de Audio Digital (DAW) soportados.
 * Diseñado bajo el principio de escalabilidad preventiva para admitir
 * múltiples DAWs comerciales a futuro.
 */
enum class DawType {
    ABLETON_LIVE,
    FL_STUDIO,
    REAPER,
    PRO_TOOLS,
    GENERIC_DAW
}

/**
 * Acciones universales de producción musical y gestión de sesiones DAW.
 */
enum class DawAction {
    /** Lanzar o traer al frente la ventana del DAW. */
    LAUNCH_OR_FOCUS,

    /** Crear un nuevo proyecto o Live Set vacío. */
    NEW_PROJECT,

    /** Guardar la sesión actual (Ctrl+S). */
    SAVE_PROJECT,

    /** Guardar la sesión con nuevo nombre / destino (Ctrl+Shift+S). */
    SAVE_PROJECT_AS,

    /** Abrir o cargar un proyecto existente (.als, etc.). */
    OPEN_PROJECT,

    /** Disparar el diálogo de exportación / renderizado de audio (Ctrl+Shift+R). */
    EXPORT_AUDIO,

    /** Iniciar o pausar la reproducción de la línea de tiempo (Barra espaciadora). */
    PLAY_PAUSE,

    /** Iniciar o detener la grabación en la sesión (F9). */
    RECORD,

    /** Conmutar el bucle / loop de la sección de arreglo (Ctrl+L). */
    TOGGLE_LOOP,

    /** Conmutar el metrónomo / claqueta. */
    TOGGLE_METRONOME,

    /** Confirmar y guardar cambios en el diálogo modal activo (Ctrl+S / Enter). */
    CONFIRM_SAVE_BEFORE_ACTION,

    /** Descartar cambios sin guardar en el diálogo modal activo ('d' / 'n'). */
    DISCARD_AND_CONTINUE,

    /** Cancelar la acción y cerrar el diálogo modal (Esc). */
    CANCEL_ACTION
}

/**
 * Petición unificada de acción DAW.
 */
data class DawActionRequest(
    val dawType: DawType = DawType.ABLETON_LIVE,
    val action: DawAction,
    val targetProjectNameOrPath: String? = null,
    val exportPreset: String? = null,
    val saveCurrentFirst: Boolean? = null
)

/**
 * Resultado estructurado de la ejecución de una acción DAW en la PC.
 */
data class DawActionResult(
    val success: Boolean = true,
    val requiresConfirmation: Boolean = false,
    val confirmationTitle: String? = null,
    val message: String = ""
)

/**
 * Metadatos de un proyecto o archivo de sesión DAW encontrado en la computadora.
 */
data class DawProjectInfo(
    val name: String,
    val absolutePath: String,
    val lastModifiedEpoch: Long = 0,
    val dawType: DawType = DawType.ABLETON_LIVE,
    val sizeBytes: Long = 0
)

/**
 * Estado en tiempo real del DAW activo.
 */
data class DawStatus(
    val dawType: DawType = DawType.ABLETON_LIVE,
    val isRunning: Boolean = false,
    val activeProjectTitle: String = "",
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false
)
