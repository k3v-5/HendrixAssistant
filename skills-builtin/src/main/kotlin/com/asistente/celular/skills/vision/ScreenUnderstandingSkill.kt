package com.asistente.celular.skills.vision

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.ScreenVisionUiPayload
import com.asistente.celular.nlu.vision.ScreenUnderstandingProvider

/**
 * Habilidad de Screen Understanding: inspecciona y comprende el contenido de la pantalla activa
 * mediante accesibilidad nativa para resúmenes, lectura y traducción instantánea.
 */
class ScreenUnderstandingSkill(
    private val screenProvider: ScreenUnderstandingProvider? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "screen_understanding_skill",
        name = "Visión de Pantalla",
        description = "Lee, resume y traduce el contenido visible en pantalla utilizando el árbol de accesibilidad nativo."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Consulta general: "que hay en mi pantalla", "que ves en la pantalla"
        SequenceConstruct(
            WordConstruct("que"),
            WordConstruct("hay", "dice", "ves", "tengo", "aparece"),
            OptionalConstruct(WordConstruct("en", "de")),
            OptionalConstruct(WordConstruct("mi", "la", "esta")),
            WordConstruct("pantalla")
        ),
        // 2. Resumen: "resume la pantalla", "resumeme lo que veo", "resumen de pantalla"
        SequenceConstruct(
            WordConstruct("resume", "resumeme", "resumen", "sintetiza"),
            OptionalConstruct(WordConstruct("de", "a")),
            OptionalConstruct(WordConstruct("la", "mi", "lo", "esta")),
            OptionalConstruct(WordConstruct("que")),
            OptionalConstruct(WordConstruct("veo", "hay")),
            OptionalConstruct(WordConstruct("en")),
            OptionalConstruct(WordConstruct("pantalla", "aqui"))
        ),
        // 3. Traducción: "traduce la pantalla", "traduce lo que dice aqui"
        SequenceConstruct(
            WordConstruct("traduce", "traduceme", "traduccion"),
            OptionalConstruct(WordConstruct("de")),
            OptionalConstruct(WordConstruct("la", "mi", "lo", "esta")),
            OptionalConstruct(WordConstruct("que")),
            OptionalConstruct(WordConstruct("dice", "veo")),
            OptionalConstruct(WordConstruct("en", "aqui")),
            OptionalConstruct(WordConstruct("pantalla"))
        ),
        // 4. Lectura: "leeme la pantalla", "lee la pantalla"
        SequenceConstruct(
            WordConstruct("lee", "leeme", "leer"),
            OptionalConstruct(WordConstruct("la", "mi", "esta")),
            WordConstruct("pantalla")
        ),
        // 5. Explicación: "explica la pantalla", "explicame lo que veo"
        SequenceConstruct(
            WordConstruct("explica", "explicame"),
            OptionalConstruct(WordConstruct("la", "lo", "esta")),
            OptionalConstruct(WordConstruct("que")),
            OptionalConstruct(WordConstruct("veo", "pantalla"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("pantalla") || lower.contains("lo que veo") || lower.contains("que dice aqui")) {
            val action = when {
                lower.contains("resume") || lower.contains("resumeme") || lower.contains("resumen") -> "summary"
                lower.contains("traduce") || lower.contains("traduceme") -> "translate"
                lower.contains("lee") || lower.contains("leeme") -> "read"
                lower.contains("explica") || lower.contains("explicame") -> "explain"
                else -> "general"
            }
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to action)
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val provider = screenProvider
        if (provider == null || !provider.isAvailable()) {
            val msg = "Para leer tu pantalla, activa el Servicio de Accesibilidad de Hendrix en Ajustes."
            return SkillOutput(
                speech = msg,
                displayText = "📱 **Visión de Pantalla:** Requiere activar el Servicio de Accesibilidad en Ajustes.",
                success = false
            )
        }

        val snapshot = provider.captureScreenContent()
        if (snapshot == null || snapshot.texts.isEmpty()) {
            val msg = "No detecté texto visible o interactivo en la pantalla actual."
            return SkillOutput(
                speech = msg,
                displayText = "📱 **Pantalla:** No hay elementos de texto legibles actualmente.",
                success = false
            )
        }

        val action = score.capturedSlots["action"] ?: "general"
        val topTexts = snapshot.texts.take(8)
        val textSnippet = topTexts.joinToString("\n• ")

        val (speech, summary) = when (action) {
            "summary" -> {
                val appDesc = snapshot.title?.let { "en $it" } ?: ""
                val spoken = "En tu pantalla $appDesc veo ${snapshot.texts.size} elementos de texto. Los principales son: ${topTexts.take(3).joinToString(", ")}."
                val sum = "Resumen de pantalla ($appDesc):\n• $textSnippet"
                Pair(spoken, sum)
            }
            "translate" -> {
                val spoken = "He capturado el texto en pantalla para traducir. Hay ${snapshot.texts.size} fragmentos de texto detectados."
                val sum = "Texto capturado para traducción:\n• $textSnippet"
                Pair(spoken, sum)
            }
            "read" -> {
                val spoken = "Leyendo pantalla: " + topTexts.take(4).joinToString(". ")
                val sum = "Lectura de pantalla:\n• $textSnippet"
                Pair(spoken, sum)
            }
            else -> {
                val appDesc = snapshot.title?.let { "de $it" } ?: ""
                val spoken = "En la pantalla $appDesc detecté ${snapshot.texts.size} fragmentos de texto."
                val sum = "Contenido en pantalla $appDesc:\n• $textSnippet"
                Pair(spoken, sum)
            }
        }

        val payload = ScreenVisionUiPayload(
            packageName = snapshot.packageName,
            title = snapshot.title,
            texts = snapshot.texts,
            summary = summary
        )

        return SkillOutput(
            speech = speech,
            displayText = "📱 **Visión de Pantalla**\n$summary",
            success = true,
            payload = payload
        )
    }
}
