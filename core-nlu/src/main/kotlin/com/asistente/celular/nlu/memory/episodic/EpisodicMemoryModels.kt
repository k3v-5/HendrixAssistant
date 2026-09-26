package com.asistente.celular.nlu.memory.episodic

/**
 * Entrada histórica de una sesión en un software creativo (Unreal, Blender, Ableton, FL Studio, VS Code).
 */
data class CreativeSessionEntry(
    val id: String,
    val appName: String,
    val projectTitle: String,
    val projectPath: String? = null,
    val lastActiveEpochMillis: Long = System.currentTimeMillis(),
    val notesSummary: String? = null,
    val gitBranchOrState: String? = null
)

/**
 * Contrato de repositorio para la Memoria Episódica persistente de Hendrix.
 * Registra y rastrea el contexto de trabajo del usuario en la PC a lo largo del tiempo.
 */
interface EpisodicMemoryRepository {

    /**
     * Registra o actualiza una sesión de trabajo creativa activa.
     */
    suspend fun recordSession(entry: CreativeSessionEntry)

    /**
     * Obtiene las sesiones creativas más recientes ordenadas por última actividad descendente.
     */
    suspend fun getRecentSessions(limit: Int = 5): List<CreativeSessionEntry>

    /**
     * Obtiene la sesión de trabajo más reciente o activa actualmente.
     */
    suspend fun getLastActiveSession(): CreativeSessionEntry?

    /**
     * Obtiene la última sesión registrada para una aplicación específica (ej. "Unreal", "Blender").
     */
    suspend fun getSessionForApp(appName: String): CreativeSessionEntry?

    /**
     * Limpia el historial episódico.
     */
    suspend fun clearSessions()
}
