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
        val speech = "Puedo gestionar tus tareas y notas, encender la linterna, decirte la hora y la fecha, programar alarmas y temporizadores, controlar tu música, abrir aplicaciones y responder a tus dudas con Inteligencia Artificial."
        val displayText = """¡Hola! Esto es lo que puedo hacer:
✅ Tareas: "Recuérdame comprar leche mañana a las 5 pm", "¿Cuáles son mis tareas?"
📝 Notas: "Toma una nota: código de puerta 1234", "Mis notas"
🔦 Linterna: "Enciende la linterna", "Apaga la linterna"
⏰ Alarmas y Hora: "Pon una alarma a las 7:00", "Temporizador de 5 minutos", "¿Qué hora es?"
🎵 Música: "Pausa la música", "Siguiente canción", "Reproduce"
📱 Aplicaciones: "Abre WhatsApp", "Abre YouTube"
🤖 Inteligencia Artificial: Pregúntame sobre cualquier tema libre."""

        return SkillOutput(
            speech = speech,
            displayText = displayText,
            success = true
        )
    }
}
