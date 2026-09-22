package com.asistente.celular.nlu.pc

/**
 * Tipo de interacción enviada desde el móvil a la computadora.
 */
enum class PcActionType {
    CLICK,
    DOUBLE_CLICK,
    RIGHT_CLICK,
    MOUSE_DOWN,
    MOUSE_UP,
    MOUSE_MOVE,
    SCROLL,
    KEY_PRESS,
    KEY_DOWN,
    KEY_UP,
    HOTKEY,
    TYPE_TEXT
}

/**
 * Representa una acción de entrada de usuario serializable para inyectar en la PC.
 * Las coordenadas se manejan en ratios normalizados (0.0 a 1.0) para garantizar
 * total independencia de la resolución o escala del monitor de la PC.
 */
data class PcInteractionAction(
    val type: PcActionType,
    val xRatio: Float? = null,
    val yRatio: Float? = null,
    val scrollDeltaX: Float = 0f,
    val scrollDeltaY: Float = 0f,
    val textPayload: String? = null,
    val keyCodes: List<String> = emptyList(),
    val timestampEpoch: Long = System.currentTimeMillis()
)
