package com.asistente.celular.nlu.mesh

enum class MeshDeviceType {
    PC,
    ANDROID,
    TABLET,
    SERVER
}

data class MeshNode(
    val nodeId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8899,
    val deviceType: MeshDeviceType = MeshDeviceType.PC,
    val isPaired: Boolean = true,
    val lastSeenEpoch: Long = System.currentTimeMillis()
)

enum class MeshPacketType {
    CLIPBOARD_SYNC,
    QUICK_COMMAND,
    FILE_TRANSFER,
    NOTIFICATION_RELAY,
    HEARTBEAT
}

data class MeshPacket(
    val packetId: String,
    val type: MeshPacketType,
    val payload: String,
    val senderId: String,
    val timestampEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para el subsistema de comunicación Mesh P2P en red local entre Hendrix Android y PC.
 * Permite sincronización sin depender de servidores en la nube externos.
 */
interface HendrixMeshController {
    suspend fun discoverLocalPeers(): List<MeshNode>
    suspend fun sendPacketToPeer(targetNodeId: String, packet: MeshPacket): Boolean
    suspend fun broadcastClipboard(content: String): Int
    fun getConnectedPeers(): List<MeshNode>
    fun isServiceActive(): Boolean
}
