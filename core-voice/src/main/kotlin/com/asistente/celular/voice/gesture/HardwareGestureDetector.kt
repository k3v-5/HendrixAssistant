package com.asistente.celular.voice.gesture

enum class HardwareGestureType {
    QUICK_TAP_BACK,
    FLIP_FACE_DOWN,
    SHAKE_TRIGGER,
    LIFT_TO_EAR
}

data class GestureEvent(
    val gestureType: HardwareGestureType,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)

/**
 * Contrato para el detector de gestos físicos de hardware del dispositivo.
 * Permite capturar acciones rápidas como tap en la tapa trasera o voltear boca abajo
 * mediante acelerómetro/giroscopio de bajo consumo.
 */
interface HardwareGestureDetector {
    fun startListening(onGesture: (GestureEvent) -> Unit)
    fun stopListening()
    fun isListening(): Boolean
    fun setSensitivity(sensitivityMultiplier: Float)
}
