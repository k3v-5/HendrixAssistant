package com.asistente.celular.nlu.camera

enum class CameraLensType {
    BACK_MAIN,
    BACK_WIDE,
    FRONT
}

data class CameraCaptureConfig(
    val countdownSeconds: Int = 3,
    val lens: CameraLensType = CameraLensType.BACK_MAIN,
    val flashEnabled: Boolean = false,
    val countPeopleBeforeShutter: Boolean = false
)

data class CameraCaptureResult(
    val success: Boolean,
    val imagePath: String? = null,
    val detectedFacesCount: Int = 0,
    val message: String = ""
)

/**
 * Contrato para el asistente de cámara y fotografía manos libres.
 */
interface CameraDirectorController {
    suspend fun scheduleVoiceShutter(config: CameraCaptureConfig): CameraCaptureResult
    fun switchLens(lens: CameraLensType): Boolean
    fun toggleFlash(enabled: Boolean): Boolean
    suspend fun countPeopleInFrame(): Int
}
