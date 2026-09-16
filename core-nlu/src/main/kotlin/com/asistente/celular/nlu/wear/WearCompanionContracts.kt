package com.asistente.celular.nlu.wear

data class WearDeviceNode(
    val nodeId: String,
    val name: String,
    val batteryPercent: Int = 85,
    val isConnected: Boolean = true
)

enum class WearPacketType {
    VOICE_TRIGGER,
    COMPACT_CARD,
    HAPTIC_ALERT,
    HEART_RATE_STREAM
}

data class WearAssistantPacket(
    val packetId: String,
    val type: WearPacketType,
    val payload: String,
    val timestampEpoch: Long = System.currentTimeMillis()
)

data class WearScreenState(
    val connectedWearDevices: List<WearDeviceNode> = emptyList(),
    val lastWristCommand: String? = null,
    val isWristMicActive: Boolean = false
)

/**
 * Contrato para el control del ecosistema de relojes inteligentes Wear OS.
 */
interface WearCompanionController {
    fun getConnectedWearDevices(): List<WearDeviceNode>
    suspend fun sendCompactNotificationToWrist(title: String, message: String): Boolean
    suspend fun triggerWristHapticPulse(pulsePattern: String): Boolean
    fun notifyWristVoiceInputReceived(spokenText: String)
}
