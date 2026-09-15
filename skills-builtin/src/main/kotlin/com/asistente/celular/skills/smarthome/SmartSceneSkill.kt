package com.asistente.celular.skills.smarthome

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.PresetSmartScenes
import com.asistente.celular.nlu.smarthome.RingerModeSetting
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartScene

/**
 * Habilidad de Escenas Inteligentes de Domótica y Coordinación con el Dispositivo.
 * Ejecuta ambientes predefinidos (Modo Lectura, Modo Estudio, Buenas Noches, Buenos Días, Modo Cine).
 */
class SmartSceneSkill(
    private val smartHomeRepository: SmartHomeRepository,
    private val scenes: List<SmartScene> = PresetSmartScenes.ALL_PRESETS
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "smart_scene_skill",
        name = "Escenas Inteligentes",
        description = "Activa ambientes coordinados entre tu foco inteligente y los ajustes del teléfono."
    )

    override val specificity: Specificity = Specificity.HIGH

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        for (scene in scenes) {
            for (trigger in scene.triggerPhrases) {
                val normTrigger = MatchContext.normalize(trigger)
                if (lower == normTrigger || lower.contains(normTrigger)) {
                    return SkillScore(
                        confidence = 0.96f,
                        specificity = Specificity.HIGH,
                        capturedSlots = mapOf("scene_id" to scene.id)
                    )
                }
            }
        }

        // Patrón tipo "activa la escena [nombre]" o "escena [nombre]"
        if (lower.contains("escena") || lower.contains("modo")) {
            for (scene in scenes) {
                val normName = MatchContext.normalize(scene.name)
                if (lower.contains(normName)) {
                    return SkillScore(
                        confidence = 0.94f,
                        specificity = Specificity.HIGH,
                        capturedSlots = mapOf("scene_id" to scene.id)
                    )
                }
            }
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val sceneId = score.capturedSlots["scene_id"] ?: return SkillOutput(
            speech = "No reconocí la escena solicitada.",
            displayText = "Escena no encontrada",
            success = false
        )

        val scene = scenes.firstOrNull { it.id == sceneId } ?: return SkillOutput(
            speech = "Escena no disponible.",
            displayText = "Escena no disponible",
            success = false
        )

        // 1. Coordinar ajustes del teléfono (DND, Sonido)
        scene.deviceConfig?.let { devCfg ->
            applyDeviceConfig(context, devCfg)
        }

        // 2. Coordinar el foco inteligente en la red local
        scene.lightConfig?.let { lightCfg ->
            applyLightConfig(lightCfg)
        }

        val display = """
            ${scene.icon} **${scene.name}**
            
            ${scene.description}
            
            • *Acción ejecutada correctamente.*
        """.trimIndent()

        return SkillOutput(
            speech = scene.speechResponse,
            displayText = display,
            success = true,
            payload = scene
        )
    }

    private suspend fun applyLightConfig(lightCfg: com.asistente.celular.nlu.smarthome.SceneLightConfig) {
        // Auto-descubrir dispositivos si la lista local está vacía
        if (smartHomeRepository.devices.value.isEmpty()) {
            smartHomeRepository.discoverDevices()
        }

        val devices = smartHomeRepository.devices.value
        if (devices.isEmpty()) return

        // 1. Estado de encendido / apagado
        if (lightCfg.power == false) {
            smartHomeRepository.executeAction(null, DeviceAction.TurnOff)
            return // Si se apaga, no es necesario enviar brillo ni color
        } else if (lightCfg.power == true) {
            smartHomeRepository.executeAction(null, DeviceAction.TurnOn)
        }

        // 2. Temperatura de color
        lightCfg.colorTemperature?.let { kelvin ->
            smartHomeRepository.executeAction(null, DeviceAction.SetColorTemperature(kelvin))
        }

        // 3. Brillo
        lightCfg.brightness?.let { bright ->
            smartHomeRepository.executeAction(null, DeviceAction.SetBrightness(bright))
        }

        // 4. Color RGB
        lightCfg.rgbColor?.let { rgb ->
            smartHomeRepository.executeAction(null, DeviceAction.SetColor(rgb))
        }
    }

    private fun applyDeviceConfig(context: SkillContext, devCfg: com.asistente.celular.nlu.smarthome.SceneDeviceConfig) {
        try {
            // Perfil de sonido
            devCfg.ringerMode?.let { mode ->
                val am = context.androidContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null) {
                    when (mode) {
                        RingerModeSetting.SILENT -> am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        RingerModeSetting.VIBRATE -> am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        RingerModeSetting.NORMAL -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                }
            }

            // Modo No Molestar
            devCfg.setDnd?.let { enableDnd ->
                val nm = context.androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                    val filter = if (enableDnd) {
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    } else {
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    }
                    nm.setInterruptionFilter(filter)
                }
            }
        } catch (e: Exception) {
            // Ignorar excepciones si el permiso no está otorgado para no bloquear la escena de iluminación
        }
    }
}
