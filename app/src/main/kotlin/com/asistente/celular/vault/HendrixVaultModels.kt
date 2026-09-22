package com.asistente.celular.vault

import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfile
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.tasks.TaskItem

/**
 * Estrategia de resolución al restaurar una Bóveda Hendrix.
 */
enum class RestoreStrategy {
    /** Reemplaza completamente los perfiles, rutinas y datos locales con los de la copia. */
    REPLACE_ALL,

    /** Fusiona conservando elementos existentes y añadiendo los nuevos o modificados. */
    MERGE
}

/**
 * Contenedor maestro de la Bóveda de Respaldo de Hendrix Assistant.
 */
data class HendrixVaultArchive(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "2.0.0",
    val macroDecks: List<MacroDeckProfile> = emptyList(),
    val routines: List<AutomatedRoutine> = emptyList(),
    val smartDevices: List<SmartDevice> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)
