package com.asistente.celular.nlu.pc.wol

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Utilidad desacoplada para la construcción y emisión del Magic Packet del protocolo Wake-on-LAN (WOL).
 * Cumple con el estándar AMD Magic Packet: 6 bytes 0xFF seguidos de 16 repeticiones de la dirección MAC de 6 bytes.
 */
object WakeOnLanHelper {

    private val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
    private val MAC_COMPACT_REGEX = Regex("^[0-9A-Fa-f]{12}$")

    /**
     * Valida si una cadena corresponde a una dirección MAC válida (con delimitadores : o - o formato compacto).
     */
    fun isValidMac(mac: String?): Boolean {
        if (mac.isNullOrBlank()) return false
        val trimmed = mac.trim()
        return MAC_REGEX.matches(trimmed) || MAC_COMPACT_REGEX.matches(trimmed)
    }

    /**
     * Convierte una dirección MAC en un array de 6 bytes binarios.
     */
    fun parseMacBytes(mac: String): ByteArray {
        require(isValidMac(mac)) { "Dirección MAC no válida: $mac" }
        val clean = mac.replace(":", "").replace("-", "").trim()
        val bytes = ByteArray(6)
        for (i in 0 until 6) {
            val byteStr = clean.substring(i * 2, i * 2 + 2)
            bytes[i] = byteStr.toInt(16).toByte()
        }
        return bytes
    }

    /**
     * Construye el Magic Packet estándar de 102 bytes.
     * Estructura: 6 bytes en 0xFF + 16 iteraciones de los 6 bytes de la MAC.
     */
    fun buildMagicPacket(mac: String): ByteArray {
        val macBytes = parseMacBytes(mac)
        val packet = ByteArray(6 + 16 * 6) // 102 bytes

        // Primeros 6 bytes en 0xFF
        for (i in 0 until 6) {
            packet[i] = 0xFF.toByte()
        }

        // 16 repeticiones de la MAC
        var destPos = 6
        for (repeat in 0 until 16) {
            System.arraycopy(macBytes, 0, packet, destPos, 6)
            destPos += 6
        }

        return packet
    }

    /**
     * Envía el Magic Packet a través de un socket UDP en modo broadcast.
     * Por robustez, transmite a los puertos estándar 9 (Discard) y 7 (Echo).
     */
    fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        ports: List<Int> = listOf(9, 7)
    ): Result<Unit> = runCatching {
        val packetBytes = buildMagicPacket(macAddress)

        DatagramSocket(null).use { socket ->
            socket.broadcast = true
            socket.reuseAddress = true

            for (port in ports) {
                try {
                    val address = InetSocketAddress(broadcastIp, port)
                    val datagram = DatagramPacket(packetBytes, packetBytes.size, address)
                    socket.send(datagram)
                } catch (e: Exception) {
                    // Continuar con el siguiente puerto
                }
            }
        }
    }
}
