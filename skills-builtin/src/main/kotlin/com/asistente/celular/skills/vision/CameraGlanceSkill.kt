package com.asistente.celular.skills.vision

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.ocr.OcrEngine
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.OcrGlanceUiPayload

/**
 * Habilidad de Camera Glance & OCR Offline: lee letreros, recibos, hojas y documentos
 * capturados por la cámara o imágenes locales sin requerir conexión a internet.
 */
class CameraGlanceSkill(
    private val ocrEngine: OcrEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "camera_glance_skill",
        name = "Camera Glance & OCR",
        description = "Reconoce y lee textos de la cámara, recibos y documentos usando OCR offline en el dispositivo."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. "mira esto", "mira lo que tengo aqui"
        SequenceConstruct(
            WordConstruct("mira", "observa"),
            OptionalConstruct(WordConstruct("lo", "que", "tengo")),
            WordConstruct("esto", "aqui")
        ),
        // 2. "lee esto", "lee este texto", "lee este papel", "lee este letrero", "lee este cartel"
        SequenceConstruct(
            WordConstruct("lee", "leeme"),
            WordConstruct("esto", "este", "esta"),
            OptionalConstruct(WordConstruct("texto", "papel", "letrero", "cartel", "recibo", "documento", "imagen", "foto"))
        ),
        // 3. "que dice aqui", "que dice este papel", "que dice el cartel"
        SequenceConstruct(
            WordConstruct("que"),
            WordConstruct("dice"),
            OptionalConstruct(WordConstruct("en", "el", "este", "esta")),
            WordConstruct("aqui", "texto", "papel", "cartel", "letrero", "recibo", "foto", "imagen")
        ),
        // 4. "escanea este documento", "extrae el texto de la camara"
        SequenceConstruct(
            WordConstruct("escanea", "extrae"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("texto", "documento", "recibo", "papel")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // Descartar si menciona explícitamente "pantalla" (eso le corresponde a ScreenUnderstandingSkill)
        if (lower.contains("pantalla")) return SkillScore.NO_MATCH

        if (lower.contains("lee esto") || lower.contains("mira esto") || lower.contains("que dice aqui") ||
            lower.contains("letrero") || lower.contains("cartel") || lower.contains("recibo") ||
            (lower.contains("lee") && (lower.contains("papel") || lower.contains("documento") || lower.contains("texto")))) {
            return SkillScore(
                confidence = 0.94f,
                specificity = Specificity.HIGH
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val engine = ocrEngine
        if (engine == null || !engine.isAvailable()) {
            val msg = "El motor de reconocimiento óptico (OCR) offline no está activo."
            return SkillOutput(
                speech = msg,
                displayText = "📷 **Camera Glance:** Motor OCR no disponible.",
                success = false
            )
        }

        // Reconocimiento de texto (usa cámara o frame snapshot)
        val result = engine.recognizeText(imageUriOrPath = "camera_glance_latest")
        if (result.fullText.isBlank()) {
            val msg = "No pude encontrar ningún texto legible en la imagen."
            return SkillOutput(
                speech = msg,
                displayText = "📷 **Camera Glance:** No se detectó texto en el encuadre.",
                success = false
            )
        }

        val lines = result.blocks.flatMap { it.lines }.filter { it.isNotBlank() }
        val preview = lines.take(4).joinToString(". ")
        val speech = "Texto detectado: $preview"
        val display = "📷 **Texto Detectado (OCR Offline):**\n" + lines.joinToString("\n• ", prefix = "• ")

        val payload = OcrGlanceUiPayload(
            fullText = result.fullText,
            lines = lines
        )

        return SkillOutput(
            speech = speech,
            displayText = display,
            success = true,
            payload = payload
        )
    }
}
