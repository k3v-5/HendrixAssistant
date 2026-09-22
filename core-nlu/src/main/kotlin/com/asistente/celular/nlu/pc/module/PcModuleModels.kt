package com.asistente.celular.nlu.pc.module

/**
 * Identificadores únicos para módulos y suites de software soportados en la PC.
 */
enum class PcModuleId {
    ABLETON_LIVE,
    FL_STUDIO,
    ADOBE_CREATIVE,
    BLENDER,
    UNREAL_ENGINE,
    WEB_BROWSERS,
    CLIPBOARD_MANAGER,
    AUDIO_MIXER,
    STUDIO_SCENES,
    PROJECT_BROWSER,
    HARDWARE_WATCHDOG,
    SCREEN_COPILOT,
    CUSTOM_PLUGINS,
    WIRELESS_AUDIO_MONITOR,
    AUTOMATED_ROUTINES,
    AIRSYNC_P2P,
    WORKSPACE_TERMINAL_MEMORY
}

/**
 * Categorías de software para organización ergonómica en el panel de control.
 */
enum class PcModuleCategory(val displayName: String, val iconEmoji: String) {
    AUDIO_DAW("Audio & DAWs", "🎹"),
    CREATIVE_DESIGN("Diseño & Video", "🎨"),
    THREE_D_VFX("3D & Modelado", "🧊"),
    GAME_ENGINE("Motores de Videojuegos", "🎮"),
    WEB_INTERNET("Navegación & Web", "🌐"),
    SYSTEM_TOOLS("Sistema & Utilidades", "⚙️")
}

/**
 * Definición atómica de un comando rápido / macro táctil en la tarjeta del módulo.
 */
data class PcQuickMacro(
    val actionId: String,
    val label: String,
    val iconEmoji: String,
    val shortcutHint: String? = null,
    val colorHex: Long = 0xFF6366F1
)

/**
 * Definición declarativa de un módulo o plugin de PC.
 * Diseñado bajo el principio de escalabilidad preventiva (OCP) para admitir
 * nuevos softwares sin alterar el motor de renderizado.
 */
data class PcModuleDefinition(
    val id: PcModuleId,
    val name: String,
    val category: PcModuleCategory,
    val iconEmoji: String,
    val isEnabledByDefault: Boolean = true,
    val description: String,
    val accentColorHex: Long = 0xFF8B5CF6,
    val quickMacros: List<PcQuickMacro>
)

/**
 * Petición de ejecución de una acción rápida de un módulo hacia la computadora.
 */
data class PcModuleActionRequest(
    val moduleId: PcModuleId,
    val actionId: String,
    val params: Map<String, String> = emptyMap()
)

/**
 * Resultado de la ejecución de una acción de módulo.
 */
data class PcModuleActionResult(
    val success: Boolean = true,
    val message: String = ""
)

/**
 * Registro predeterminado de módulos de control de PC.
 */
object PcModuleRegistry {

    val ABLETON_LIVE_MODULE = PcModuleDefinition(
        id = PcModuleId.ABLETON_LIVE,
        name = "Ableton Live",
        category = PcModuleCategory.AUDIO_DAW,
        iconEmoji = "🎹",
        isEnabledByDefault = true,
        description = "Estación de audio digital profesional para producción y directo.",
        accentColorHex = 0xFF8B5CF6,
        quickMacros = listOf(
            PcQuickMacro("PLAY_PAUSE", "Play / Pausa", "▶️", "Espacio", 0xFF10B981),
            PcQuickMacro("RECORD", "Grabar", "⏺️", "F9", 0xFFEF4444),
            PcQuickMacro("TOGGLE_LOOP", "Bucle", "🔁", "Ctrl+L", 0xFF3B82F6),
            PcQuickMacro("TOGGLE_METRONOME", "Metrónomo", "⏱️", "C", 0xFFF59E0B),
            PcQuickMacro("SAVE_PROJECT", "Guardar", "💾", "Ctrl+S", 0xFF6366F1),
            PcQuickMacro("NEW_PROJECT", "Nuevo Set", "✨", "Ctrl+N", 0xFF8B5CF6),
            PcQuickMacro("EXPORT_AUDIO", "Exportar", "🎧", "Ctrl+Shift+R", 0xFF06B6D4)
        )
    )

    val FL_STUDIO_MODULE = PcModuleDefinition(
        id = PcModuleId.FL_STUDIO,
        name = "FL Studio",
        category = PcModuleCategory.AUDIO_DAW,
        iconEmoji = "🍊",
        isEnabledByDefault = true,
        description = "Entorno completo de producción musical y secuenciación por patrones.",
        accentColorHex = 0xFFF97316,
        quickMacros = listOf(
            PcQuickMacro("PLAY_PAUSE", "Play / Pausa", "▶️", "Espacio", 0xFF10B981),
            PcQuickMacro("RECORD", "Grabar", "⏺️", "R", 0xFFEF4444),
            PcQuickMacro("TOGGLE_MODE", "Pat / Song", "🔀", "L", 0xFFF59E0B),
            PcQuickMacro("TOGGLE_METRONOME", "Metrónomo", "⏱️", "Ctrl+M", 0xFF3B82F6),
            PcQuickMacro("SAVE_PROJECT", "Guardar", "💾", "Ctrl+S", 0xFF6366F1),
            PcQuickMacro("EXPORT_AUDIO", "Exportar", "🎵", "Ctrl+R", 0xFF06B6D4),
            PcQuickMacro("VIEW_MIXER", "Mezclador", "🎚️", "F9", 0xFF8B5CF6),
            PcQuickMacro("VIEW_PIANO_ROLL", "Piano Roll", "🎹", "F7", 0xFFEC4899)
        )
    )

    val ADOBE_CREATIVE_MODULE = PcModuleDefinition(
        id = PcModuleId.ADOBE_CREATIVE,
        name = "Adobe Suite",
        category = PcModuleCategory.CREATIVE_DESIGN,
        iconEmoji = "🎨",
        isEnabledByDefault = true,
        description = "Herramientas creativas de edición de video (Premiere Pro) y diseño (Photoshop).",
        accentColorHex = 0xFF3B82F6,
        quickMacros = listOf(
            PcQuickMacro("PLAY_PAUSE", "Play / Stop", "▶️", "Espacio", 0xFF10B981),
            PcQuickMacro("RAZOR_TOOL", "Cuchilla", "✂️", "C", 0xFFEF4444),
            PcQuickMacro("SELECT_TOOL", "Selección", "↖️", "V", 0xFF3B82F6),
            PcQuickMacro("RIPPLE_DELETE", "Eliminar Hueco", "🗑️", "Shift+Supr", 0xFFF59E0B),
            PcQuickMacro("RENDER_TIMELINE", "Renderizar", "⚡", "Enter", 0xFF10B981),
            PcQuickMacro("EXPORT_MEDIA", "Exportar Video", "🎬", "Ctrl+M", 0xFF06B6D4),
            PcQuickMacro("BRUSH_TOOL", "Pincel (PS)", "🖌️", "B", 0xFF8B5CF6),
            PcQuickMacro("SAVE_PROJECT", "Guardar", "💾", "Ctrl+S", 0xFF6366F1)
        )
    )

    val BLENDER_MODULE = PcModuleDefinition(
        id = PcModuleId.BLENDER,
        name = "Blender 3D",
        category = PcModuleCategory.THREE_D_VFX,
        iconEmoji = "🧊",
        isEnabledByDefault = true,
        description = "Suite integral de modelado, animación, renderizado y efectos visuales 3D.",
        accentColorHex = 0xFFEA580C,
        quickMacros = listOf(
            PcQuickMacro("RENDER_IMAGE", "Render Imagen", "🖼️", "F12", 0xFF10B981),
            PcQuickMacro("RENDER_ANIM", "Render Video", "🎞️", "Ctrl+F12", 0xFFEF4444),
            PcQuickMacro("TOGGLE_SHADING", "Sombreado", "🔮", "Z", 0xFF8B5CF6),
            PcQuickMacro("VIEW_CAMERA", "Vista Cámara", "📷", "Num 0", 0xFF3B82F6),
            PcQuickMacro("TOOL_MOVE", "Mover (Grab)", "✋", "G", 0xFFF59E0B),
            PcQuickMacro("TOOL_ROTATE", "Rotar", "🔄", "R", 0xFF06B6D4),
            PcQuickMacro("TOOL_SCALE", "Escalar", "📐", "S", 0xFFEC4899),
            PcQuickMacro("SAVE_FILE", "Guardar .blend", "💾", "Ctrl+S", 0xFF6366F1)
        )
    )

    val UNREAL_ENGINE_MODULE = PcModuleDefinition(
        id = PcModuleId.UNREAL_ENGINE,
        name = "Unreal Engine 5",
        category = PcModuleCategory.GAME_ENGINE,
        iconEmoji = "🎮",
        isEnabledByDefault = true,
        description = "Motor de videojuegos y gráficos fotorrealistas en tiempo real de Epic Games.",
        accentColorHex = 0xFF0284C7,
        quickMacros = listOf(
            PcQuickMacro("PLAY_IN_EDITOR", "Play (PIE)", "▶️", "Alt+P", 0xFF10B981),
            PcQuickMacro("SIMULATE", "Simular", "⚙️", "Alt+S", 0xFFF59E0B),
            PcQuickMacro("STOP_SIMULATION", "Detener", "⏹️", "Esc", 0xFFEF4444),
            PcQuickMacro("CONTENT_DRAWER", "Content Drawer", "📁", "Ctrl+Espacio", 0xFF3B82F6),
            PcQuickMacro("SAVE_ALL", "Guardar Todo", "💾", "Ctrl+Shift+S", 0xFF6366F1),
            PcQuickMacro("BUILD_ALL", "Compilar Nivel", "🏗️", "Ctrl+Shift+;", 0xFF8B5CF6),
            PcQuickMacro("FOCUS_ACTOR", "Enfocar Actor", "🎯", "F", 0xFF06B6D4)
        )
    )

    val WEB_BROWSERS_MODULE = PcModuleDefinition(
        id = PcModuleId.WEB_BROWSERS,
        name = "Navegadores Web",
        category = PcModuleCategory.WEB_INTERNET,
        iconEmoji = "🌐",
        isEnabledByDefault = true,
        description = "Navegación y búsquedas por defecto en Google Chrome, Brave, Opera y navegador predeterminado.",
        accentColorHex = 0xFF2563EB,
        quickMacros = listOf(
            PcQuickMacro("OPEN_CHROME", "Chrome", "🌐", null, 0xFF4285F4),
            PcQuickMacro("OPEN_BRAVE", "Brave", "🦁", null, 0xFFFF5722),
            PcQuickMacro("OPEN_OPERA", "Opera", "🔴", null, 0xFFFF1B2D),
            PcQuickMacro("SEARCH_GOOGLE", "Buscar Google", "🔍", "Google", 0xFF34A853),
            PcQuickMacro("SEARCH_YOUTUBE", "Buscar YouTube", "📺", "YouTube", 0xFFFF0000),
            PcQuickMacro("OPEN_YOUTUBE", "YouTube", "▶️", "youtube.com", 0xFFDC2626),
            PcQuickMacro("OPEN_FACEBOOK", "Facebook", "👥", "facebook.com", 0xFF1877F2),
            PcQuickMacro("OPEN_GITHUB", "GitHub", "🐙", "github.com", 0xFF333333),
            PcQuickMacro("NEW_TAB", "Nueva Pestaña", "➕", "Ctrl+T", 0xFF3B82F6),
            PcQuickMacro("CLOSE_TAB", "Cerrar Pestaña", "❌", "Ctrl+W", 0xFFEF4444),
            PcQuickMacro("RELOAD_PAGE", "Recargar", "🔄", "Ctrl+R", 0xFF10B981)
        )
    )

    val CLIPBOARD_MANAGER_MODULE = PcModuleDefinition(
        id = PcModuleId.CLIPBOARD_MANAGER,
        name = "Portapapeles & Snippets",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "📋",
        isEnabledByDefault = true,
        description = "Sincronización bidireccional, verificación de integridad SHA-256 y biblioteca de fragmentos rápidos.",
        accentColorHex = 0xFF10B981,
        quickMacros = listOf(
            PcQuickMacro("GET_CLIPBOARD", "Leer Portapapeles", "📥", null, 0xFF10B981),
            PcQuickMacro("PASTE_CLIPBOARD", "Pegar en PC", "⚡", "Ctrl+V", 0xFF6366F1),
            PcQuickMacro("CLEAR_CLIPBOARD", "Limpiar", "🗑️", null, 0xFFEF4444)
        )
    )

    val AUDIO_MIXER_MODULE = PcModuleDefinition(
        id = PcModuleId.AUDIO_MIXER,
        name = "Mezclador de Audio",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "🎚️",
        isEnabledByDefault = true,
        description = "Mini-Mixer de audio por aplicación para Windows, DAWs, navegadores y reproductores multimedia.",
        accentColorHex = 0xFFF59E0B,
        quickMacros = listOf(
            PcQuickMacro("MUTE_MASTER", "Mute Maestro", "🔇", null, 0xFFEF4444),
            PcQuickMacro("VOLUME_UP", "Subir Volumen", "🔊", "+5%", 0xFF10B981),
            PcQuickMacro("VOLUME_DOWN", "Bajar Volumen", "🔉", "-5%", 0xFF3B82F6),
            PcQuickMacro("REFRESH_MIXER", "Actualizar Apps", "🔄", null, 0xFFF59E0B)
        )
    )

    val STUDIO_SCENES_MODULE = PcModuleDefinition(
        id = PcModuleId.STUDIO_SCENES,
        name = "Escenas de Estudio & Macros",
        category = PcModuleCategory.AUDIO_DAW,
        iconEmoji = "🎬",
        isEnabledByDefault = true,
        description = "Rutinas encadenadas para Producción Musical, Render Nocturno, Streaming y Cierre de Estudio con salvado automático.",
        accentColorHex = 0xFF8B5CF6,
        quickMacros = listOf(
            PcQuickMacro("STUDIO_MUSIC_MODE", "Modo Producción", "🎵", "Ableton/FL", 0xFF8B5CF6),
            PcQuickMacro("STUDIO_RENDER_NIGHT_MODE", "Render Nocturno", "🌙", "Watchdog + Suspensión", 0xFF6366F1),
            PcQuickMacro("STUDIO_STREAMING_MODE", "Modo Streaming", "🎙️", "OBS + DAW", 0xFF10B981),
            PcQuickMacro("STUDIO_CLOSE_MODE", "Cerrar Estudio", "🛑", "Save + Sleep", 0xFFEF4444)
        )
    )

    val PROJECT_BROWSER_MODULE = PcModuleDefinition(
        id = PcModuleId.PROJECT_BROWSER,
        name = "Explorador de Proyectos",
        category = PcModuleCategory.CREATIVE_DESIGN,
        iconEmoji = "📂",
        isEnabledByDefault = true,
        description = "Búsqueda e indexación remota de proyectos creativos (Ableton, FL Studio, Blender, Premiere, Unreal) en todos los discos del PC.",
        accentColorHex = 0xFF06B6D4,
        quickMacros = listOf(
            PcQuickMacro("FILTER_ALL", "Todos", "📂", null, 0xFF06B6D4),
            PcQuickMacro("FILTER_AUDIO", "Audio/DAW", "🎹", null, 0xFF8B5CF6),
            PcQuickMacro("FILTER_3D", "3D & VFX", "🧊", null, 0xFFF59E0B),
            PcQuickMacro("FILTER_VIDEO", "Video & Edit", "🎬", null, 0xFFEC4899)
        )
    )

    val HARDWARE_WATCHDOG_MODULE = PcModuleDefinition(
        id = PcModuleId.HARDWARE_WATCHDOG,
        name = "Hardware & Render Watchdog",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "⚡",
        isEnabledByDefault = true,
        description = "Telemetría en tiempo real de GPU, VRAM, CPU y temperatura con vigilancia inteligente de renderizado y auto-suspensión.",
        accentColorHex = 0xFFEF4444,
        quickMacros = listOf(
            PcQuickMacro("WATCHDOG_BLENDER", "Vigilar Blender", "🧊", "Auto-Suspensión", 0xFFF59E0B),
            PcQuickMacro("WATCHDOG_AFTER_EFFECTS", "Vigilar After Effects", "🎬", "Auto-Suspensión", 0xFF8B5CF6),
            PcQuickMacro("REFRESH_TELEMETRY", "Actualizar Métricas", "🔄", null, 0xFF10B981)
        )
    )

    val SCREEN_COPILOT_MODULE = PcModuleDefinition(
        id = PcModuleId.SCREEN_COPILOT,
        name = "AI Screen Copilot",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "👁️",
        isEnabledByDefault = true,
        description = "Diagnóstico visual bajo demanda de la pantalla o ventana activa de la PC mediante IA multimodal (visión).",
        accentColorHex = 0xFF6366F1,
        quickMacros = listOf(
            PcQuickMacro("COPILOT_DIAGNOSE", "Diagnosticar", "🔍", null, 0xFF6366F1),
            PcQuickMacro("COPILOT_ERRORS", "Revisar Errores", "⚠️", null, 0xFFEF4444),
            PcQuickMacro("COPILOT_TERMINAL", "Analizar Terminal", "💻", null, 0xFF10B981)
        )
    )

    val CUSTOM_PLUGINS_MODULE = PcModuleDefinition(
        id = PcModuleId.CUSTOM_PLUGINS,
        name = "Plugins de Usuario",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "🧩",
        isEnabledByDefault = true,
        description = "Ecosistema de plugins y scripts en Python ejecutados directamente en Hendrix Desktop.",
        accentColorHex = 0xFF8B5CF6,
        quickMacros = listOf(
            PcQuickMacro("PLUGINS_REFRESH", "Recargar", "🔄", null, 0xFF8B5CF6),
            PcQuickMacro("CLEAN_TEMP", "Limpiar Temp", "🧹", null, 0xFFEF4444),
            PcQuickMacro("BACKUP_PROJECTS", "Respaldar", "💾", null, 0xFF10B981)
        )
    )

    val WIRELESS_AUDIO_MONITOR_MODULE = PcModuleDefinition(
        id = PcModuleId.WIRELESS_AUDIO_MONITOR,
        name = "Monitor de Audio Inalámbrico",
        category = PcModuleCategory.AUDIO_DAW,
        iconEmoji = "🎧",
        isEnabledByDefault = true,
        description = "Monitoreo en tiempo real del audio de la PC en el celular (WASAPI Loopback) con latencia ultrabaja y vúmetro reactivo.",
        accentColorHex = 0xFF06B6D4,
        quickMacros = listOf(
            PcQuickMacro("START_MONITOR", "Iniciar Audio", "▶️", null, 0xFF06B6D4),
            PcQuickMacro("STOP_MONITOR", "Detener", "⏹️", null, 0xFFEF4444)
        )
    )

    val AUTOMATED_ROUTINES_MODULE = PcModuleDefinition(
        id = PcModuleId.AUTOMATED_ROUTINES,
        name = "Automatizaciones & Presencia",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "⚡",
        isEnabledByDefault = true,
        description = "Rutinas inteligentes 'Zero-Touch' disparadas por Wi-Fi, Geocercas, horarios o eventos críticos de la computadora.",
        accentColorHex = 0xFF10B981,
        quickMacros = listOf(
            PcQuickMacro("STUDIO_ARRIVAL", "Llegada al Estudio", "🏡", null, 0xFF10B981),
            PcQuickMacro("LEAVE_HOME", "Salida de Casa", "🚗", null, 0xFFEF4444)
        )
    )

    val AIRSYNC_P2P_MODULE = PcModuleDefinition(
        id = PcModuleId.AIRSYNC_P2P,
        name = "Hendrix AirSync P2P",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "⚡",
        isEnabledByDefault = true,
        description = "Transferencia LAN P2P de archivos pesados de alta velocidad sin internet.",
        accentColorHex = 0xFF00E5FF,
        quickMacros = listOf(
            PcQuickMacro("REFRESH_FILES", "Refrescar Archivos", "🔄", null, 0xFF00E5FF)
        )
    )

    val WORKSPACE_TERMINAL_MEMORY_MODULE = PcModuleDefinition(
        id = PcModuleId.WORKSPACE_TERMINAL_MEMORY,
        name = "Memoria de Trabajo & Terminal",
        category = PcModuleCategory.SYSTEM_TOOLS,
        iconEmoji = "🧠",
        isEnabledByDefault = true,
        description = "Contexto Git, procesos creativos activos y centinela de errores de compilación con IA.",
        accentColorHex = 0xFF8B5CF6,
        quickMacros = listOf(
            PcQuickMacro("REFRESH_CONTEXT", "Actualizar Contexto", "🔄", null, 0xFF8B5CF6)
        )
    )

    /** Lista completa de todos los módulos disponibles en la plataforma. */
    val ALL_MODULES: List<PcModuleDefinition> = listOf(
        ABLETON_LIVE_MODULE,
        FL_STUDIO_MODULE,
        ADOBE_CREATIVE_MODULE,
        BLENDER_MODULE,
        UNREAL_ENGINE_MODULE,
        WEB_BROWSERS_MODULE,
        CLIPBOARD_MANAGER_MODULE,
        AUDIO_MIXER_MODULE,
        STUDIO_SCENES_MODULE,
        PROJECT_BROWSER_MODULE,
        HARDWARE_WATCHDOG_MODULE,
        SCREEN_COPILOT_MODULE,
        CUSTOM_PLUGINS_MODULE,
        WIRELESS_AUDIO_MONITOR_MODULE,
        AUTOMATED_ROUTINES_MODULE,
        AIRSYNC_P2P_MODULE,
        WORKSPACE_TERMINAL_MEMORY_MODULE
    )

    fun findById(id: PcModuleId): PcModuleDefinition? = ALL_MODULES.firstOrNull { it.id == id }
}
