package com.asistente.celular.hardware

/**
 * Perfiles de sensibilidad inercial para el gesto "Shake to Wake" (Sacudir para despertar).
 *
 * @property threshold Umbral de variación de aceleración lineal (m/s²).
 * @property requiredShakes Número de oscilaciones consecutivas requeridas.
 * @property debounceMillis Tiempo de enfriamiento antirrebote entre activaciones.
 * @property displayName Nombre descriptivo para la interfaz de usuario.
 */
enum class ShakeSensitivity(
    val threshold: Float,
    val requiredShakes: Int,
    val debounceMillis: Long,
    val displayName: String
) {
    /** Suave: Despierta con un movimiento ligero de muñeca (~8.0 m/s²). */
    GENTLE(
        threshold = 8.0f,
        requiredShakes = 2,
        debounceMillis = 1500L,
        displayName = "Suave"
    ),

    /** Normal: Equilibrio estándar (~11.0 m/s²). */
    NORMAL(
        threshold = 11.0f,
        requiredShakes = 2,
        debounceMillis = 2000L,
        displayName = "Normal"
    ),

    /** Firme: Requiere agitación decidida (~14.0 m/s²), ideal para evitar falsos positivos al trotar o caminar. */
    VIGOROUS(
        threshold = 14.0f,
        requiredShakes = 3,
        debounceMillis = 2500L,
        displayName = "Firme"
    );

    companion object {
        fun fromName(name: String?, default: ShakeSensitivity = NORMAL): ShakeSensitivity {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}
