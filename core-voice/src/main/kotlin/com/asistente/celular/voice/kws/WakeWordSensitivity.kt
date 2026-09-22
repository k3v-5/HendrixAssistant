package com.asistente.celular.voice.kws

/**
 * Nivel de sensibilidad para la detección continua de la palabra clave ("Oye Hendrix").
 *
 * Configura los parámetros acústicos del analizador de energía RMS (VAD) para adaptarse
 * a distintos ambientes (silencioso, normal de oficina o ruidoso de calle/tráfico).
 *
 * @property voiceRmsThreshold Umbral mínimo de energía RMS del micrófono para considerar actividad vocal.
 * @property requiredVoiceFrames Número de frames de audio consecutivos (~100ms c/u) con energía vocal sostenida.
 * @property cooldownMillis Tiempo de enfriamiento mínimo en milisegundos entre verificaciones acústicas.
 * @property displayName Nombre legible para la interfaz de usuario.
 */
enum class WakeWordSensitivity(
    val voiceRmsThreshold: Float,
    val requiredVoiceFrames: Int,
    val cooldownMillis: Long,
    val displayName: String
) {
    /** Baja sensibilidad: Entornos ruidosos o llamadas; requiere hablar muy cerca y de forma sostenida. */
    LOW(
        voiceRmsThreshold = 0.065f,
        requiredVoiceFrames = 3,
        cooldownMillis = 2500L,
        displayName = "Baja (Ambientes ruidosos)"
    ),

    /** Sensibilidad equilibrada estándar para uso habitual en interiores / oficina. */
    MEDIUM(
        voiceRmsThreshold = 0.045f,
        requiredVoiceFrames = 2,
        cooldownMillis = 2000L,
        displayName = "Media (Equilibrada)"
    ),

    /** Alta sensibilidad: Detección rápida y reactiva a mayor distancia o con voz tenue. */
    HIGH(
        voiceRmsThreshold = 0.025f,
        requiredVoiceFrames = 1,
        cooldownMillis = 1500L,
        displayName = "Alta (Sensible)"
    );

    companion object {
        fun fromName(name: String?, default: WakeWordSensitivity = MEDIUM): WakeWordSensitivity {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}
