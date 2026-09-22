package com.asistente.celular.nlu.pc.deck.dynamic

/**
 * Tipo de acción ejecutada al interactuar con un control del Macro Deck.
 */
sealed interface MacroDeckAction {
    data class ShortcutAction(val keySequence: String, val description: String = "") : MacroDeckAction
    data class QuickCommandAction(val command: String) : MacroDeckAction
    data class StudioSceneAction(val sceneId: String) : MacroDeckAction
    data class PluginAction(val pluginId: String, val actionId: String, val params: Map<String, Any> = emptyMap()) : MacroDeckAction
    data class RoutineAction(val routineId: String) : MacroDeckAction
}

/**
 * Control atómico dentro de la superficie táctil del Macro Deck.
 */
sealed interface MacroDeckControl {
    val id: String
    val label: String
    val iconEmoji: String
    val colorHex: Long

    data class MacroButton(
        override val id: String,
        override val label: String,
        override val iconEmoji: String,
        override val colorHex: Long = 0xFF6366F1,
        val action: MacroDeckAction,
        val subtitle: String? = null,
        val isFullWidth: Boolean = false
    ) : MacroDeckControl

    data class MacroFader(
        override val id: String,
        override val label: String,
        override val iconEmoji: String = "🎚️",
        override val colorHex: Long = 0xFF06B6D4,
        val targetParameter: String,
        val minValue: Float = 0.0f,
        val maxValue: Float = 1.0f,
        val initialValue: Float = 0.8f
    ) : MacroDeckControl

    data class MacroJogWheel(
        override val id: String,
        override val label: String,
        override val iconEmoji: String = "🔄",
        override val colorHex: Long = 0xFFF59E0B,
        val onStepForwardAction: MacroDeckAction,
        val onStepBackwardAction: MacroDeckAction
    ) : MacroDeckControl
}

/**
 * Perfil completo del Macro Deck para una aplicación específica de Windows.
 */
data class MacroDeckProfile(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val targetProcessRegex: String,
    val headerSubtitle: String,
    val themeAccentColorHex: Long,
    val controls: List<MacroDeckControl>
)

/**
 * Registro de perfiles declarativos predefinidos de grado profesional.
 */
object MacroDeckProfileRegistry {

    val BLENDER_PROFILE = MacroDeckProfile(
        id = "profile_blender",
        name = "Blender 3D",
        iconEmoji = "🧊",
        targetProcessRegex = "(?i).*blender.*",
        headerSubtitle = "Superficie de Modelado y Renderizado 3D",
        themeAccentColorHex = 0xFFF97316,
        controls = listOf(
            MacroDeckControl.MacroButton("b_render_still", "Render Imagen", "🖼️", 0xFFF97316, MacroDeckAction.ShortcutAction("F12", "F12")),
            MacroDeckControl.MacroButton("b_render_anim", "Render Anim", "🎬", 0xFFEA580C, MacroDeckAction.ShortcutAction("Ctrl+F12", "Ctrl+F12")),
            MacroDeckControl.MacroButton("b_camera_view", "Cámara 0", "📷", 0xFF3B82F6, MacroDeckAction.ShortcutAction("NumPad0", "Cámara")),
            MacroDeckControl.MacroButton("b_toggle_xray", "Rayos X", "💀", 0xFF8B5CF6, MacroDeckAction.ShortcutAction("Alt+Z", "Alt+Z")),
            MacroDeckControl.MacroButton("b_shading_render", "Shading Final", "🔮", 0xFF10B981, MacroDeckAction.ShortcutAction("Z", "Rendered")),
            MacroDeckControl.MacroButton("b_wireframe", "Wireframe", "🕸️", 0xFF64748B, MacroDeckAction.ShortcutAction("Shift+Z", "Malla"))
        )
    )

    val ABLETON_PROFILE = MacroDeckProfile(
        id = "profile_ableton",
        name = "Ableton Studio",
        iconEmoji = "🎹",
        targetProcessRegex = "(?i).*(ableton|fl64|reaper).*",
        headerSubtitle = "Superficie de Transporte y Grabación DAW",
        themeAccentColorHex = 0xFF10B981,
        controls = listOf(
            MacroDeckControl.MacroButton("daw_play_stop", "Play / Stop", "⏯️", 0xFF10B981, MacroDeckAction.ShortcutAction("Space", "Barra Espaciadora")),
            MacroDeckControl.MacroButton("daw_record", "Grabar", "🔴", 0xFFEF4444, MacroDeckAction.ShortcutAction("F9", "Grabar")),
            MacroDeckControl.MacroButton("daw_metronome", "Metrónomo", "⏱️", 0xFFF59E0B, MacroDeckAction.ShortcutAction("C", "Metrónomo")),
            MacroDeckControl.MacroButton("daw_undo", "Deshacer", "↩️", 0xFF64748B, MacroDeckAction.ShortcutAction("Ctrl+Z", "Undo")),
            MacroDeckControl.MacroFader("daw_master_fader", "Master Fader", "🎚️", 0xFF06B6D4, "master_volume", 0f, 1f, 0.75f),
            MacroDeckControl.MacroButton("daw_loop", "Bucle", "🔁", 0xFF8B5CF6, MacroDeckAction.ShortcutAction("Ctrl+L", "Loop"))
        )
    )

    val VIDEO_EDIT_PROFILE = MacroDeckProfile(
        id = "profile_video",
        name = "Suite de Edición de Video",
        iconEmoji = "🎞️",
        targetProcessRegex = "(?i).*(premiere|resolve|afterfx).*",
        headerSubtitle = "Superficie de Montaje y Timeline",
        themeAccentColorHex = 0xFF8B5CF6,
        controls = listOf(
            MacroDeckControl.MacroButton("vid_razor", "Cuchilla (Cut)", "✂️", 0xFFEC4899, MacroDeckAction.ShortcutAction("C", "Razor Tool")),
            MacroDeckControl.MacroButton("vid_select", "Selección (V)", "↖️", 0xFF3B82F6, MacroDeckAction.ShortcutAction("V", "Select Tool")),
            MacroDeckControl.MacroButton("vid_mark_in", "Punto Entrada (I)", "👉", 0xFF10B981, MacroDeckAction.ShortcutAction("I", "Mark In")),
            MacroDeckControl.MacroButton("vid_mark_out", "Punto Salida (O)", "👈", 0xFFF59E0B, MacroDeckAction.ShortcutAction("O", "Mark Out")),
            MacroDeckControl.MacroJogWheel(
                id = "vid_timeline_jog",
                label = "Timeline Jog / Shuttle",
                iconEmoji = "🎞️",
                colorHex = 0xFF8B5CF6,
                onStepForwardAction = MacroDeckAction.ShortcutAction("Right", "Next Frame"),
                onStepBackwardAction = MacroDeckAction.ShortcutAction("Left", "Prev Frame")
            )
        )
    )

    val CODE_DEV_PROFILE = MacroDeckProfile(
        id = "profile_code",
        name = "Developer Workspace",
        iconEmoji = "💻",
        targetProcessRegex = "(?i).*(code|idea64|studio64|windowsterminal).*",
        headerSubtitle = "Control de Código, Git y Pruebas",
        themeAccentColorHex = 0xFF0284C7,
        controls = listOf(
            MacroDeckControl.MacroButton("dev_run_tests", "Ejecutar Tests", "🧪", 0xFF10B981, MacroDeckAction.QuickCommandAction("run_tests")),
            MacroDeckControl.MacroButton("dev_git_commit", "Git Commit", "📦", 0xFF8B5CF6, MacroDeckAction.QuickCommandAction("git_commit")),
            MacroDeckControl.MacroButton("dev_format", "Formatear", "✨", 0xFF0284C7, MacroDeckAction.ShortcutAction("Shift+Alt+F", "Format")),
            MacroDeckControl.MacroButton("dev_terminal", "Terminal", "📟", 0xFF334155, MacroDeckAction.ShortcutAction("Ctrl+`", "Toggle Terminal")),
            MacroDeckControl.MacroButton("dev_build", "Compilar", "🔨", 0xFFF59E0B, MacroDeckAction.QuickCommandAction("build"))
        )
    )

    val DEFAULT_DESKTOP_PROFILE = MacroDeckProfile(
        id = "profile_default",
        name = "Escritorio General",
        iconEmoji = "🖥️",
        targetProcessRegex = ".*",
        headerSubtitle = "Centro de Productividad de Windows",
        themeAccentColorHex = 0xFF6366F1,
        controls = listOf(
            MacroDeckControl.MacroButton("sys_vol_mute", "Silenciar", "🔇", 0xFFEF4444, MacroDeckAction.QuickCommandAction("volume_mute")),
            MacroDeckControl.MacroButton("sys_play_pause", "Media Play", "⏯️", 0xFF10B981, MacroDeckAction.QuickCommandAction("media_play_pause")),
            MacroDeckControl.MacroButton("sys_next_track", "Siguiente", "⏭️", 0xFF3B82F6, MacroDeckAction.QuickCommandAction("media_next")),
            MacroDeckControl.MacroButton("sys_lock", "Bloquear PC", "🔒", 0xFFF59E0B, MacroDeckAction.QuickCommandAction("lock")),
            MacroDeckControl.MacroButton("sys_screen_copilot", "AI Copilot", "🧠", 0xFF8B5CF6, MacroDeckAction.QuickCommandAction("screen_copilot"))
        )
    )

    val ALL_PROFILES = listOf(
        BLENDER_PROFILE,
        ABLETON_PROFILE,
        VIDEO_EDIT_PROFILE,
        CODE_DEV_PROFILE,
        DEFAULT_DESKTOP_PROFILE
    )

    val DEV_PROFILE = CODE_DEV_PROFILE
    val PREMIERE_PROFILE = VIDEO_EDIT_PROFILE
    val DESKTOP_PROFILE = DEFAULT_DESKTOP_PROFILE

    fun getAllProfiles(): List<MacroDeckProfile> = ALL_PROFILES

    fun findProfileForProcess(processName: String?): MacroDeckProfile {
        if (processName.isNullOrBlank()) return DEFAULT_DESKTOP_PROFILE
        for (profile in ALL_PROFILES) {
            if (profile.id != DEFAULT_DESKTOP_PROFILE.id && processName.matches(Regex(profile.targetProcessRegex))) {
                return profile
            }
        }
        return DEFAULT_DESKTOP_PROFILE
    }
}
