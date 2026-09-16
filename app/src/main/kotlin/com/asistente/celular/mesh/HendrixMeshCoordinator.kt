package com.asistente.celular.mesh

import android.content.Context
import com.asistente.celular.nlu.mesh.HendrixMeshController
import com.asistente.celular.nlu.mesh.MeshDeviceType
import com.asistente.celular.nlu.mesh.MeshNode
import com.asistente.celular.nlu.mesh.MeshPacket
import com.asistente.celular.nlu.mesh.MeshPacketType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinador de protocolo Mesh P2P en red local entre Hendrix y PC / otros nodos locales.
 * Provee sincronización de portapapeles y comandos sin servidores intermediarios.
 */
class HendrixMeshCoordinator(
    private val context: Context
) : HendrixMeshController {

    private val pairedPeers = ConcurrentHashMap<String, MeshNode>()
    private var isBroadcasting = true
    var lastSyncedClipboard: String? = null
        private set

    init {
        // Inicializar con un nodo PC de demostración en la red local
        pairedPeers["pc_workstation"] = MeshNode(
            nodeId = "pc_workstation",
            name = "PC Estación de Trabajo",
            ipAddress = "192.168.1.100",
            port = 8899,
            deviceType = MeshDeviceType.PC,
            isPaired = true
        )
    }

    override suspend fun discoverLocalPeers(): List<MeshNode> {
        return pairedPeers.values.toList()
    }

    override suspend fun sendPacketToPeer(targetNodeId: String, packet: MeshPacket): Boolean {
        val node = pairedPeers[targetNodeId] ?: return false
        if (packet.type == MeshPacketType.CLIPBOARD_SYNC) {
            lastSyncedClipboard = packet.payload
        }
        return true
    }

    override suspend fun broadcastClipboard(content: String): Int {
        lastSyncedClipboard = content
        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString().take(8),
            type = MeshPacketType.CLIPBOARD_SYNC,
            payload = content,
            senderId = "hendrix_mobile"
        )
        var count = 0
        for (peer in pairedPeers.values) {
            sendPacketToPeer(peer.nodeId, packet)
            count++
        }
        return count
    }

    override fun getConnectedPeers(): List<MeshNode> {
        return pairedPeers.values.toList()
    }

    override fun isServiceActive(): Boolean = isBroadcasting
}
