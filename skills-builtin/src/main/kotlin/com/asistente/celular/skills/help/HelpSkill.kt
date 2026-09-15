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
        val speech = "Puedo gestionar tus tareas y notas, controlar el volumen y ajustes, realizar llamadas y enviar WhatsApp, encender la linterna, programar alarmas y temporizadores, controlar tu música, abrir aplicaciones y responder preguntas con Inteligencia Artificial."
        val displayText = """¡Hola! Esto es lo que puedo hacer:
✅ Tareas: "Recuérdame comprar leche mañana a las 5 pm", "¿Cuáles son mis tareas?"
📝 Notas: "Toma una nota: código de puerta 1234", "Mis notas"
🔊 Volumen: "Sube el volumen", "Volumen al 50%", "Silencia el teléfono"
📞 Llamadas: "Llama a Juan", "Llamar al 5512345678"
💬 WhatsApp: "Envía un WhatsApp a Mamá diciendo ya llegué"
⚙️ Ajustes: "Abre los ajustes de Wi-Fi", "Ajustes de Bluetooth", "Modo No Molestar"
🔦 Linterna: "Enciende la linterna", "Apaga la linterna"
⏰ Alarmas y Hora: "Pon una alarma a las 7:00", "Temporizador de 5 minutos", "¿Qué hora es?"
🎵 Música: "Pausa la música", "Siguiente canción", "Reproduce"
📱 Aplicaciones: "Abre WhatsApp", "Abre YouTube"
🤖 Inteligencia Artificial: Pregúntame sobre tus pendientes o cualquier tema libre."""

        return SkillOutput(
            speech = speech,
            displayText = displayText,
            success = true
        )
    }
}
