package com.asistente.celular.voice.earcon

/**
 * Tipos de earcons acústicos de respuesta y retroalimentación inmediata del asistente.
 */
enum class EarconType {
    /** Tono dual de alta tecnología al detectar wake word ("Hendrix"). */
    WAKE_WORD_PING,

    /** Tono suave de confirmación al ejecutar exitosamente una acción o comando reflejo. */
    SUCCESS_CONFIRMATION,

    /** Tono de error o comando no comprendido. */
    ERROR_ALERT,

    /** Tono suave descendente al cerrar la ventana de escucha ambiental (follow-up). */
    DISMISS_PROMPT,

    /** Alarma rítmica acústica cuando un temporizador finaliza (00:00). */
    TIMER_ALARM
}

/**
 * Contrato para el motor de generación y reproducción de earcons y sonidos del sistema.
 */
interface EarconEngine {
    /**
     * Reproduce un earcon inmediato sin bloquear el hilo principal.
     */
    fun playEarcon(type: EarconType)

    /**
     * Detiene la alarma continua de temporizador si está sonando.
     */
    fun stopAlarm()

    /**
     * Libera recursos de audio.
     */
    fun release()
}
