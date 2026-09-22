package com.asistente.celular.nlu.pc.scene

/**
 * Representa un paso atómico ejecutable dentro de una escena de estudio.
 */
sealed interface PcSceneStep {
    val description: String

    data class WakeOnLan(
        override val description: String = "Despertar PC vía Wake-on-LAN",
        val timeoutMs: Long = 15000L
    ) : PcSceneStep

    data class UnlockSession(
        override val description: String = "Desbloquear sesión de Windows",
        val pin: String? = null
    ) : PcSceneStep

    data class LaunchAppOrProject(
        override val description: String,
        val target: String
    ) : PcSceneStep

    data class SetMixerAudio(
        override val description: String = "Ajustar niveles de audio",
        val masterVolume: Int? = null,
        val isMasterMuted: Boolean? = null,
        val appVolumes: Map<String, Int> = emptyMap(),
        val appMutes: Map<String, Boolean> = emptyMap()
    ) : PcSceneStep

    data class SaveAllOpenProjects(
        override val description: String = "Guardar cambios en todas las suites abiertas (Ctrl+S)"
    ) : PcSceneStep

    data class Delay(
        override val description: String,
        val millis: Long
    ) : PcSceneStep

    data class StartHardwareWatchdog(
        override val description: String = "Vigilar render y notificar al finalizar",
        val processName: String = "blender",
        val autoSuspendOnComplete: Boolean = true
    ) : PcSceneStep

    data class PowerAction(
        override val description: String,
        val action: String // "sleep", "shutdown", "lock"
    ) : PcSceneStep
}

/**
 * Escena o rutina encadenada multi-paso para flujos de trabajo de estudio.
 */
data class PcStudioScene(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val accentColorHex: Long = 0xFF8B5CF6,
    val steps: List<PcSceneStep>
)

/**
 * Resultado de la ejecución de una escena.
 */
data class PcSceneExecutionResult(
    val sceneId: String,
    val success: Boolean,
    val stepsExecuted: Int,
    val totalSteps: Int,
    val message: String
)

/**
 * Catálogo central de escenas de fábrica.
 */
object PcStudioSceneRegistry {

    const val SCENE_MUSIC_PRODUCTION = "STUDIO_MUSIC_MODE"
    const val SCENE_RENDER_NIGHT = "STUDIO_RENDER_NIGHT_MODE"
    const val SCENE_CLOSE = "STUDIO_CLOSE_MODE"
    const val SCENE_STREAMING = "STUDIO_STREAMING_MODE"

    val MUSIC_PRODUCTION_SCENE = PcStudioScene(
        id = SCENE_MUSIC_PRODUCTION,
        name = "Modo Producción Musical",
        description = "Despierta PC, desbloquea Windows, lanza Ableton/FL Studio y ajusta el audio al 80%.",
        iconEmoji = "🎹",
        accentColorHex = 0xFF8B5CF6,
        steps = listOf(
            PcSceneStep.WakeOnLan(),
            PcSceneStep.UnlockSession(),
            PcSceneStep.LaunchAppOrProject("Iniciar DAW de Audio", "Ableton Live"),
            PcSceneStep.SetMixerAudio(
                masterVolume = 80,
                appVolumes = mapOf("Ableton" to 90),
                appMutes = mapOf("Discord" to true, "chrome" to true)
            )
        )
    )

    val RENDER_NIGHT_SCENE = PcStudioScene(
        id = "STUDIO_RENDER_NIGHT_MODE",
        name = "Modo Render Nocturno",
        description = "Salva Blender, vigila el render de GPU y suspende la PC automáticamente al terminar.",
        iconEmoji = "🌙",
        accentColorHex = 0xFF06B6D4,
        steps = listOf(
            PcSceneStep.SaveAllOpenProjects("Asegurar proyecto de Blender 3D"),
            PcSceneStep.StartHardwareWatchdog(
                processName = "blender",
                autoSuspendOnComplete = true
            )
        )
    )

    val CLOSE_STUDIO_SCENE = PcStudioScene(
        id = "STUDIO_CLOSE_MODE",
        name = "Cerrar Estudio / Dormir",
        description = "Guarda proyectos en todos los programas, silencia el audio y suspende la computadora.",
        iconEmoji = "🔒",
        accentColorHex = 0xFFEF4444,
        steps = listOf(
            PcSceneStep.SaveAllOpenProjects(),
            PcSceneStep.Delay("Esperar guardado en disco", 2500L),
            PcSceneStep.SetMixerAudio(masterVolume = 0, isMasterMuted = true),
            PcSceneStep.PowerAction("Poner PC en suspensión de bajo consumo", "sleep")
        )
    )

    val STREAMING_CONTENT_SCENE = PcStudioScene(
        id = "STUDIO_STREAMING_MODE",
        name = "Modo Streaming & Contenido",
        description = "Abre YouTube/Twitch, configura faders balanceados y lanza entorno de creación.",
        iconEmoji = "🚀",
        accentColorHex = 0xFFF59E0B,
        steps = listOf(
            PcSceneStep.LaunchAppOrProject("Abrir panel de streaming", "chrome"),
            PcSceneStep.SetMixerAudio(masterVolume = 70, appVolumes = mapOf("chrome" to 60, "Spotify" to 35))
        )
    )

    val ALL_SCENES = listOf(
        MUSIC_PRODUCTION_SCENE,
        RENDER_NIGHT_SCENE,
        CLOSE_STUDIO_SCENE,
        STREAMING_CONTENT_SCENE
    )

    fun findById(id: String): PcStudioScene? = ALL_SCENES.firstOrNull { it.id == id }
}
