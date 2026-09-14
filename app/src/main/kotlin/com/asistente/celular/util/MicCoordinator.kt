package com.asistente.celular.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinador reactivo para garantizar la exclusividad del hardware de micrófono.
 * Evita colisiones de audio entre el servicio de segundo plano (Wake Word)
 * y las actividades interactivas (MainActivity / AssistantDialogActivity).
 */
object MicCoordinator {

    private const val TAG = "MicCoordinator"

    private val _isMicLockedByUi = MutableStateFlow(false)
    val isMicLockedByUi: StateFlow<Boolean> = _isMicLockedByUi.asStateFlow()

    private var activeOwner: String? = null

    /**
     * Solicita la exclusividad del micrófono para una actividad UI.
     * Pausa el motor de Wake Word en segundo plano.
     */
    @Synchronized
    fun acquireMicLock(owner: String) {
        activeOwner = owner
        _isMicLockedByUi.value = true
        Log.d(TAG, "Micrófono bloqueado por: '$owner'")
    }

    /**
     * Libera el micrófono cuando la UI termina de capturar voz o sintetizar respuesta.
     * Permite reanudar la detección en segundo plano.
     */
    @Synchronized
    fun releaseMicLock(owner: String) {
        if (activeOwner == owner || activeOwner == null) {
            activeOwner = null
            _isMicLockedByUi.value = false
            Log.d(TAG, "Micrófono liberado por: '$owner'")
        }
    }
}
