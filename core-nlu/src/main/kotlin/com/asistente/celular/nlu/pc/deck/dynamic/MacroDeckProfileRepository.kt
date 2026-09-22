package com.asistente.celular.nlu.pc.deck.dynamic

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio reactivo para la persistencia, personalización y gestión
 * de perfiles del Macro Deck táctil (Custom Deck Studio).
 */
interface MacroDeckProfileRepository {
    /**
     * Flujo reactivo con la lista completa de perfiles disponibles (de fábrica y personalizados).
     */
    val profiles: StateFlow<List<MacroDeckProfile>>

    /**
     * Guarda o actualiza un perfil (creación o edición).
     */
    suspend fun saveProfile(profile: MacroDeckProfile)

    /**
     * Elimina un perfil personalizado por su identificador.
     * Los perfiles de fábrica del sistema no pueden eliminarse.
     */
    suspend fun deleteProfile(id: String)

    /**
     * Restaura los perfiles de fábrica predeterminados.
     */
    suspend fun resetToDefaults()

    /**
     * Obtiene un perfil específico por ID de forma síncrona/directa.
     */
    fun getProfileById(id: String): MacroDeckProfile?

    /**
     * Busca el perfil que coincida con el nombre del proceso activo en Windows.
     */
    fun findProfileForProcess(processName: String?): MacroDeckProfile
}
