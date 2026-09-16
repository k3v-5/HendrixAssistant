package com.asistente.celular.nlu.smarthome

/**
 * Tipo de dispositivo inteligente.
 */
enum class DeviceType(val displayName: String) {
    LIGHT("Luz / Foco"),
    SWITCH("Interruptor"),
    PLUG("Enchufe"),
    FAN("Ventilador"),
    OTHER("Dispositivo")
}

/**
 * Protocolo de comunicación con el dispositivo.
 */
enum class SmartProtocol(val displayName: String) {
    YEELIGHT_LAN("Yeelight / Xiaomi LAN"),
    XIAOMI_MIIO("Xiaomi miIO (Local)"),
    HOME_ASSISTANT("Home Assistant"),
    MATTER("Matter Local"),
    CUSTOM("Personalizado")
}

/**
 * Entidad que representa un dispositivo inteligente registrado o descubierto en la red local.
 */
data class SmartDevice(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val type: DeviceType = DeviceType.LIGHT,
    val ipAddress: String,
    val port: Int = 55443,
    val protocol: SmartProtocol = SmartProtocol.YEELIGHT_LAN,
    val isOnline: Boolean = true,
    val properties: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Determina si el texto dado coincide con el nombre o algún alias del dispositivo.
     */
    fun matchesName(query: String): Boolean {
        val cleanQuery = query.lowercase().trim()
            .removePrefix("el ")
            .removePrefix("la ")
            .removePrefix("los ")
            .removePrefix("las ")
            .removePrefix("del ")
            .removePrefix("de la ")
            .removePrefix("de el ")
            .removePrefix("en el ")
            .removePrefix("en la ")
            .trim()
        if (cleanQuery.isBlank()) return false
        if (name.lowercase().contains(cleanQuery) || cleanQuery.contains(name.lowercase())) return true
        return aliases.any { alias ->
            alias.lowercase().contains(cleanQuery) || cleanQuery.contains(alias.lowercase())
        }
    }

    val isPoweredOn: Boolean
        get() = properties["power"]?.equals("on", ignoreCase = true) == true

    val brightness: Int
        get() = properties["bright"]?.toIntOrNull() ?: 100
}

/**
 * Estado tipado de un foco o luz para componentes de interfaz interactiva (Generative UI).
 */
data class SmartBulbUiData(
    val deviceName: String,
    val isPowerOn: Boolean,
    val brightness: Int = 100,
    val colorRgb: Int? = null,
    val colorTemp: Int? = null,
    val activeMode: String = "normal", // "normal", "candle", "party", "night"
    val ipAddress: String? = null
)
