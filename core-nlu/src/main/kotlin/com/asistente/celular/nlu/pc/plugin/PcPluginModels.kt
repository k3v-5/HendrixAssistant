package com.asistente.celular.nlu.pc.plugin

/**
 * Parámetro configurable de una acción de plugin.
 */
data class PcPluginParam(
    val name: String,
    val type: String = "string",
    val default: Any? = null,
    val description: String = ""
)

/**
 * Acción ejecutable expuesta por un plugin de usuario.
 */
data class PcPluginAction(
    val id: String,
    val label: String,
    val description: String = "",
    val dangerous: Boolean = false,
    val params: List<PcPluginParam> = emptyList()
)

/**
 * Metadatos y definición de un plugin dinámico de usuario en Hendrix Desktop.
 */
data class PcPluginDefinition(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val author: String = "",
    val icon: String = "code",
    val actions: List<PcPluginAction> = emptyList()
)

/**
 * Resultado de la ejecución de una acción de plugin en la PC.
 */
data class PcPluginActionResult(
    val pluginId: String,
    val actionId: String,
    val success: Boolean,
    val message: String,
    val output: String = "",
    val elapsedMs: Long = 0L
)
