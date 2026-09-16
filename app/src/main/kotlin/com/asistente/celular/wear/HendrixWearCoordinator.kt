package com.asistente.celular.wear

import android.content.Context
import com.asistente.celular.nlu.wear.WearCompanionController
import com.asistente.celular.nlu.wear.WearDeviceNode
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Coordinador para el ecosistema Wear OS y reloj inteligente de Hendrix Assistant.
 */
class HendrixWearCoordinator(
    private val context: Context
) : WearCompanionController {

    private val connectedWearDevices = CopyOnWriteArrayList<WearDeviceNode>()
    var lastReceivedWristCommand: String? = null
        private set

    init {
        // Dispositivo reloj conectado de demostración
        connectedWearDevices.add(
            WearDeviceNode(
                nodeId = "galaxy_watch_node",
                name = "Galaxy Watch 6 (Wear OS)",
                batteryPercent = 78,
                isConnected = true
            )
        )
    }

    override fun getConnectedWearDevices(): List<WearDeviceNode> {
        return connectedWearDevices.toList()
    }

    override suspend fun sendCompactNotificationToWrist(title: String, message: String): Boolean {
        return connectedWearDevices.isNotEmpty()
    }

    override suspend fun triggerWristHapticPulse(pulsePattern: String): Boolean {
        return connectedWearDevices.isNotEmpty()
    }

    override fun notifyWristVoiceInputReceived(spokenText: String) {
        lastReceivedWristCommand = spokenText
    }
}
