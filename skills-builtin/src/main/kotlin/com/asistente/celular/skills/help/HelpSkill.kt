package com.asistente.celular.skills.help

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad offline para explicar las capacidades y comandos disponibles en Hendrix.
 */
class HelpSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "help_skill",
        name = "Ayuda y Capacidades",
        description = "Explica qué puede hacer el asistente y qué comandos admite."
    ),
    specificity = Specificity.HIGH
) {
    override val patterns: List<Construct> = listOf(
        // "qué puedes hacer", "qué cosas puedes hacer", "qué sabes hacer"
        SequenceConstruct(
            WordConstruct("que"),
            OptionalConstruct(WordConstruct("cosas")),
            WordConstruct("puedes", "sabes"),
            WordConstruct("hacer")
        ),
        // "cuáles son tus funciones", "cuáles son tus habilidades", "comandos"
        SequenceConstruct(
            WordConstruct("cuales", "que"),
            OptionalConstruct(WordConstruct("son")),
            OptionalConstruct(WordConstruct("tus")),
            WordConstruct("funciones", "habilidades", "comandos", "capacidades")
        ),
        // "para qué sirves", "de qué sirves"
        SequenceConstruct(
            WordConstruct("para", "de"),
            WordConstruct("que"),
            WordConstruct("sirves")
        ),
        // "ayuda", "dame ayuda", "necesito ayuda"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("dame", "necesito")),
            WordConstruct("ayuda")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val speech = "Puedo ejecutar tus rutinas, recordar tus preferencias, consultar tu calendario, reproducir música en Spotify o YouTube, gestionar tus tareas y notas, controlar el volumen y ajustes, realizar llamadas y responder preguntas con Inteligencia Artificial."
        val displayText = """¡Hola! Esto es lo que puedo hacer:
⚡ Rutinas: "Buenas noches", "Modo estudio", "Mis rutinas"
🧠 Memoria Personal: "Recuerda que no tomo café", "¿Qué sabes sobre mí?"
📅 Calendario: "¿Qué tengo en mi calendario hoy?", "Agrega evento cita mañana a las 4 pm"
🎵 Música: "Pon Starboy en Spotify", "Reproduce rock en YouTube Music"
✅ Tareas: "Recuérdame comprar leche mañana a las 5 pm", "¿Cuáles son mis tareas?"
📝 Notas: "Toma una nota: código de puerta 1234", "Mis notas"
🔊 Volumen y Sistema: "Volumen al 50%", "Silencia el teléfono", "Ajustes de Wi-Fi", "No Molestar"
📞 Llamadas y WhatsApp: "Llama a Juan", "Envía un WhatsApp a Mamá diciendo ya voy"
🔦 Linterna y Herramientas: "Enciende la linterna", "Pon una alarma a las 7 am", "Temporizador de 5 minutos"
🤖 Inteligencia Artificial: Pregúntame sobre tus pendientes o cualquier tema libre."""

        return SkillOutput(
            speech = speech,
            displayText = displayText,
            success = true
        )
    }
}
