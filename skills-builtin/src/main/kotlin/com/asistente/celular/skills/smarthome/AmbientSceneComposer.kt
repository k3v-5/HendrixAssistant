package com.asistente.celular.skills.smarthome

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.RingerModeSetting
import com.asistente.celular.nlu.smarthome.SmartHomeRepository

/**
 * Tipos de sonido ambiental soportados por el generador y orquestador.
 */
enum class AmbientSoundType(val displayName: String, val searchKeyword: String) {
    RAIN("Lluvia Suave", "rain sounds relaxing"),
    WHITE_NOISE("Ruido Blanco / Foco", "white noise for focus"),
    FIREPLACE("Chimenea Acogedora", "crackling fireplace sounds"),
    CAFE("Cafetería / Murmullo", "ambient coffee shop sounds"),
    FOREST("Bosque y Naturaleza", "nature forest sounds relaxing"),
    NONE("Sin Sonido", "")
}

/**
 * Perfil integral de ambiente dinámico (Luz + Sonido + Estado del Teléfono).
 */
data class DynamicAmbientProfile(
    val id: String,
    val name: String,
    val description: String,
    val soundType: AmbientSoundType,
    val lightAction: DeviceAction? = null,
    val enableDnd: Boolean? = null,
    val ringerMode: RingerModeSetting? = null,
    val spotifyQuery: String? = null
)

/**
 * Orquestador de Ambientes Dinámicos (Punto 20).
 * Coordina simultáneamente iluminación domótica (Xiaomi / Home Assistant),
 * paisaje sonoro ambiental (vía Spotify o audio de relajación) y ajustes acústicos del sistema.
 */
class AmbientSceneComposer(
    private val smartHomeRepository: SmartHomeRepository
) {

    /**
     * Aplica integralmente el perfil de ambiente dinámico coordinando domótica, sonido y sistema.
     */
    suspend fun composeScene(
        context: Context,
        profile: DynamicAmbientProfile,
        targetDeviceName: String? = null
    ): AmbientCompositionResult {
        var lightSuccess = false
        var soundTriggered = false
        var systemSettingsApplied = false

        // 1. Coordinar iluminación inteligente
        if (profile.lightAction != null) {
            try {
                val actionResult = smartHomeRepository.executeAction(targetDeviceName, profile.lightAction)
                lightSuccess = actionResult.success
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo aplicar iluminación para escena '${profile.name}': ${e.message}")
            }
        } else {
            lightSuccess = true
        }

        // 2. Coordinar Ajustes de Sonido y Modo No Molestar
        try {
            applySystemSettings(context, profile.enableDnd, profile.ringerMode)
            systemSettingsApplied = true
        } catch (e: Exception) {
            Log.w(TAG, "Error aplicando ajustes de sistema para escena: ${e.message}")
        }

        // 3. Coordinar Paisaje Sonoro Ambiental
        if (profile.soundType != AmbientSoundType.NONE) {
            soundTriggered = launchAmbientSound(context, profile)
        }

        val message = "Ambiente '${profile.name}' activado. Iluminación coordinada y paisaje sonoro de ${profile.soundType.displayName} en marcha."
        return AmbientCompositionResult(
            success = lightSuccess || systemSettingsApplied || soundTriggered,
            profile = profile,
            summaryMessage = message,
            lightSuccess = lightSuccess,
            soundTriggered = soundTriggered
        )
    }

    private fun applySystemSettings(context: Context, enableDnd: Boolean?, ringerMode: RingerModeSetting?) {
        // DND (Do Not Disturb)
        if (enableDnd != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                val filter = if (enableDnd) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                nm.setInterruptionFilter(filter)
            }
        }

        // Modo de Timbre
        if (ringerMode != null) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                when (ringerMode) {
                    RingerModeSetting.SILENT -> am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    RingerModeSetting.VIBRATE -> am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    RingerModeSetting.NORMAL -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }

    private fun launchAmbientSound(context: Context, profile: DynamicAmbientProfile): Boolean {
        val query = profile.spotifyQuery ?: profile.soundType.searchKeyword
        if (query.isBlank()) return false

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("spotify:search:$query")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback a reproductor web o YouTube si Spotify no está instalado
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo lanzar audio ambiental: ${e.message}")
                false
            }
        }
    }

    companion object {
        private const val TAG = "AmbientSceneComposer"

        val PRESET_RAIN_STUDY = DynamicAmbientProfile(
            id = "ambient_rain_study",
            name = "Lluvia para Estudio",
            description = "Luz blanca neutra (4000K, 60%), DND activo y sonido continuo de lluvia relajante.",
            soundType = AmbientSoundType.RAIN,
            lightAction = DeviceAction.SetColorTemperature(4000),
            enableDnd = true,
            ringerMode = RingerModeSetting.VIBRATE
        )

        val PRESET_DEEP_FOCUS = DynamicAmbientProfile(
            id = "ambient_deep_focus",
            name = "Máxima Concentración",
            description = "Luz blanca brillante (5500K, 100%), DND estricto y ruido blanco.",
            soundType = AmbientSoundType.WHITE_NOISE,
            lightAction = DeviceAction.SetColorTemperature(5500),
            enableDnd = true,
            ringerMode = RingerModeSetting.SILENT
        )

        val PRESET_COZY_FIREPLACE = DynamicAmbientProfile(
            id = "ambient_fireplace",
            name = "Chimenea Acogedora",
            description = "Efecto vela parpadeante cálido y crepitar de chimenea en Spotify.",
            soundType = AmbientSoundType.FIREPLACE,
            lightAction = DeviceAction.StartColorFlow(0, 0, "800,2,2700,50, 1000,2,2600,75, 600,2,2800,45, 1200,2,2700,70"),
            enableDnd = false,
            ringerMode = RingerModeSetting.NORMAL
        )

        val PRESET_FOREST_ZEN = DynamicAmbientProfile(
            id = "ambient_forest_zen",
            name = "Bosque Zen",
            description = "Luz verde suave tenue y cantos de pájaros y bosque relajante.",
            soundType = AmbientSoundType.FOREST,
            lightAction = DeviceAction.SetColor(0x2E7D32),
            enableDnd = true,
            ringerMode = RingerModeSetting.VIBRATE
        )

        val ALL_AMBIENT_PRESETS = listOf(PRESET_RAIN_STUDY, PRESET_DEEP_FOCUS, PRESET_COZY_FIREPLACE, PRESET_FOREST_ZEN)
    }
}

data class AmbientCompositionResult(
    val success: Boolean,
    val profile: DynamicAmbientProfile,
    val summaryMessage: String,
    val lightSuccess: Boolean,
    val soundTriggered: Boolean
)
