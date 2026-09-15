package com.asistente.celular.nlu.smarthome

import kotlinx.serialization.Serializable

enum class RingerModeSetting {
    SILENT,
    VIBRATE,
    NORMAL
}

@Serializable
data class SceneLightConfig(
    val power: Boolean? = null,
    val brightness: Int? = null,
    val colorTemperature: Int? = null,
    val rgbColor: Int? = null
)

@Serializable
data class SceneDeviceConfig(
    val setDnd: Boolean? = null,
    val ringerMode: RingerModeSetting? = null
)

@Serializable
data class SmartScene(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val triggerPhrases: List<String>,
    val lightConfig: SceneLightConfig? = null,
    val deviceConfig: SceneDeviceConfig? = null,
    val speechResponse: String
)

/**
 * Catálogo de escenas preconfiguradas offline para Hendrix.
 */
object PresetSmartScenes {

    val READING = SmartScene(
        id = "reading",
        name = "Modo Lectura",
        description = "Luz cálida y tenue (40%, 2700K) ideal para lectura y descanso visual.",
        icon = "📖",
        triggerPhrases = listOf(
            "modo lectura", "activar modo lectura", "pon modo lectura", "hora de leer", "luz para leer"
        ),
        lightConfig = SceneLightConfig(
            power = true,
            brightness = 40,
            colorTemperature = 2700
        ),
        speechResponse = "Modo lectura activado. Luz cálida al 40%."
    )

    val STUDY = SmartScene(
        id = "study",
        name = "Modo Estudio / Trabajo",
        description = "Luz blanca brillante (100%, 5500K) para máxima concentración.",
        icon = "💻",
        triggerPhrases = listOf(
            "modo estudio", "modo trabajo", "modo concentracion", "modo concentración", "hora de trabajar", "hora de estudiar"
        ),
        lightConfig = SceneLightConfig(
            power = true,
            brightness = 100,
            colorTemperature = 5500
        ),
        speechResponse = "Modo estudio activado. Luz blanca al máximo para concentrarte."
    )

    val NIGHT = SmartScene(
        id = "night",
        name = "Buenas Noches",
        description = "Apaga el foco y activa el modo No Molestar en el teléfono.",
        icon = "🌙",
        triggerPhrases = listOf(
            "buenas noches", "modo noche", "hora de dormir", "me voy a dormir", "a descansar"
        ),
        lightConfig = SceneLightConfig(
            power = false
        ),
        deviceConfig = SceneDeviceConfig(
            setDnd = true,
            ringerMode = RingerModeSetting.SILENT
        ),
        speechResponse = "Buenas noches. Foco apagado y modo No Molestar activado. Que descanses."
    )

    val MORNING = SmartScene(
        id = "morning",
        name = "Buenos Días",
        description = "Enciende la luz al 80%, desactiva No Molestar y restablece el sonido normal.",
        icon = "🌅",
        triggerPhrases = listOf(
            "buenos dias", "buenos días", "despertar", "modo mañana", "arriba"
        ),
        lightConfig = SceneLightConfig(
            power = true,
            brightness = 80,
            colorTemperature = 4000
        ),
        deviceConfig = SceneDeviceConfig(
            setDnd = false,
            ringerMode = RingerModeSetting.NORMAL
        ),
        speechResponse = "¡Buenos días! Foco encendido y sonido del teléfono restablecido."
    )

    val CINEMA = SmartScene(
        id = "cinema",
        name = "Modo Cine / Relax",
        description = "Luz tenue ambiental (15%) para ver películas o descansar.",
        icon = "🍿",
        triggerPhrases = listOf(
            "modo cine", "modo pelicula", "modo película", "modo relax", "luz de cine"
        ),
        lightConfig = SceneLightConfig(
            power = true,
            brightness = 15,
            colorTemperature = 2200
        ),
        deviceConfig = SceneDeviceConfig(
            ringerMode = RingerModeSetting.VIBRATE
        ),
        speechResponse = "Modo cine activado. Luz tenue lista para la película."
    )

    val ALL_PRESETS = listOf(READING, STUDY, NIGHT, MORNING, CINEMA)
}
