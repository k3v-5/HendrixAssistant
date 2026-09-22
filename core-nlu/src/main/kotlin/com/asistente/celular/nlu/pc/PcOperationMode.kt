package com.asistente.celular.nlu.pc

/**
 * Modos de operación configurables para la conexión entre Hendrix y la PC.
 * Permite alternar dinámicamente entre bajo consumo de datos y máxima interactividad.
 */
enum class PcOperationMode {
    /**
     * Solo comandos semánticos (volumen, energía, scripts, atajos) y telemetría periódica.
     * Consumo de red prácticamente nulo (< 1 KB/s).
     */
    COMMAND_ONLY,

    /**
     * Modo Eco / Bajo consumo: Captura de pantalla WebP bajo demanda disparada por eventos.
     * Se actualiza únicamente al interactuar (tocar, hacer clic o escribir), con soporte
     * de rectángulos sucios (dirty-rects). Cero consumo en reposo.
     */
    INTERACTIVE_SNAPSHOT,

    /**
     * Transmisión continua de video a alta tasa de cuadros (WebRTC / H.264 / stream continuo).
     * Recomendado para escenarios donde se requiere observar animaciones o video fluido.
     */
    FULL_STREAM,

    /**
     * Modo de ejecución autónoma o semi-automática (Computer-Use RPA).
     * El agente inspecciona el árbol de accesibilidad (UIA) o analiza la pantalla con IA
     * para ejecutar tareas complejas de múltiples pasos.
     */
    AUTONOMOUS_RPA
}
