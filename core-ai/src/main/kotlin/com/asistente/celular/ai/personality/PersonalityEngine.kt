package com.asistente.celular.ai.personality

import kotlinx.serialization.Serializable

/**
 * Personalidades configurables para Hendrix Assistant.
 * Modulan el system prompt, la tonalidad de las respuestas y la velocidad/pitch del motor de voz TTS.
 */
@Serializable
enum class AssistantPersonality(
    val id: String,
    val displayName: String,
    val description: String,
    val systemPromptDirective: String,
    val speechPitchMultiplier: Float = 1.0f,
    val speechRateMultiplier: Float = 1.0f
) {
    JARVIS(
        id = "jarvis",
        displayName = "J.A.R.V.I.S.",
        description = "Formal, analítico, eficiente y con alta precisión técnica.",
        systemPromptDirective = "Adopta el tono de J.A.R.V.I.S.: formal, altamente eficiente, con humor sutil británico, respetuoso y conciso. Trata al usuario con alta cortesía profesional.",
        speechPitchMultiplier = 0.95f,
        speechRateMultiplier = 1.05f
    ),
    FRIDAY(
        id = "friday",
        displayName = "F.R.I.D.A.Y.",
        description = "Optimista, cálida, dinámica y orientada a la acción inmediata.",
        systemPromptDirective = "Adopta el tono de F.R.I.D.A.Y.: enérgico, optimista, amigable, claro y resolutivo. Respuestas directas al grano con enfoque práctico.",
        speechPitchMultiplier = 1.05f,
        speechRateMultiplier = 1.08f
    ),
    MENTOR(
        id = "mentor",
        displayName = "El Mentor",
        description = "Reflexivo, filosófico, empático y orientado a la productividad.",
        systemPromptDirective = "Adopta el tono de un sabio mentor estoico: reflexivo, empático, motivador, que valora la claridad mental, el balance y los buenos hábitos.",
        speechPitchMultiplier = 0.90f,
        speechRateMultiplier = 0.95f
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk / Hacker",
        description = "Informal, audaz, ingenioso y con jerga técnica futurista.",
        systemPromptDirective = "Adopta una actitud de asistente de ciencia ficción cyberpunk: informal, audaz, ingenioso, directo y sin rodeos corporativos.",
        speechPitchMultiplier = 1.0f,
        speechRateMultiplier = 1.10f
    ),
    STANDARD(
        id = "standard",
        displayName = "Hendrix Clásico",
        description = "Equilibrado, profesional, conciso y natural.",
        systemPromptDirective = "Mantén un tono equilibrado, profesional, amable y conciso.",
        speechPitchMultiplier = 1.0f,
        speechRateMultiplier = 1.0f
    )
}

/**
 * Motor de inyección de personalidad y contexto dinámico en prompts de Inteligencia Artificial.
 */
object PersonalityEngine {

    fun formatSystemPrompt(
        basePrompt: String,
        personality: AssistantPersonality,
        ragContext: String = ""
    ): String {
        return buildString {
            append(basePrompt)
            append("\n\n[Estilo de Personalidad Activa: ${personality.displayName}]\n")
            append(personality.systemPromptDirective)
            if (ragContext.isNotBlank()) {
                append("\n")
                append(ragContext)
            }
        }
    }
}
