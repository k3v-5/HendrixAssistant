package com.asistente.celular.skills.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService

/**
 * Contrato desacoplado para el control de la linterna / flash del dispositivo (OCP).
 * Permite mockear fácilmente el hardware en pruebas unitarias y soportar diferentes
 * implementaciones en distintos dispositivos.
 */
interface FlashlightController {
    /**
     * Enciende o apaga la linterna del dispositivo.
     * @return true si la operación tuvo éxito, false si falló o no está disponible.
     */
    fun setTorch(enabled: Boolean): Boolean

    /**
     * Alterna el estado actual de la linterna.
     * @return el nuevo estado de la linterna (true si quedó encendida, false si apagada).
     */
    fun toggleTorch(): Boolean

    /**
     * Indica si la linterna está encendida en este momento.
     */
    fun isTorchOn(): Boolean

    /**
     * Indica si el dispositivo cuenta con una linterna o flash disponible.
     */
    fun isAvailable(): Boolean
}

/**
 * Implementación nativa de Android usando CameraManager.
 * Detecta de forma segura la cámara adecuada con flash (priorizando la cámara trasera)
 * y rastrea de forma reactiva el estado del flash mediante TorchCallback.
 */
class AndroidFlashlightController(
    private val context: Context
) : FlashlightController {

    private val cameraManager: CameraManager? by lazy {
        context.getSystemService<CameraManager>()
    }

    @Volatile
    private var torchState: Boolean = false

    private val resolvedCameraId: String? by lazy {
        resolveTorchCameraId()
    }

    init {
        try {
            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == resolvedCameraId) {
                        torchState = enabled
                    }
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    if (cameraId == resolvedCameraId) {
                        torchState = false
                    }
                }
            }, null)
        } catch (_: Exception) {
            // TorchCallback no crítico en entornos restringidos
        }
    }

    private fun resolveTorchCameraId(): String? {
        val mgr = cameraManager ?: return null
        return try {
            val cameraIds = mgr.cameraIdList

            // 1. Priorizar cámara trasera con flash
            val backWithFlash = cameraIds.firstOrNull { id ->
                val chars = mgr.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && (facing == CameraCharacteristics.LENS_FACING_BACK)
            }
            if (backWithFlash != null) return backWithFlash

            // 2. Cualquier cámara con flash disponible
            cameraIds.firstOrNull { id ->
                val chars = mgr.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun setTorch(enabled: Boolean): Boolean {
        val mgr = cameraManager ?: return false
        val camId = resolvedCameraId ?: return false
        return try {
            mgr.setTorchMode(camId, enabled)
            torchState = enabled
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun toggleTorch(): Boolean {
        val newState = !torchState
        val success = setTorch(newState)
        return if (success) newState else torchState
    }

    override fun isTorchOn(): Boolean = torchState

    override fun isAvailable(): Boolean = resolvedCameraId != null
}
