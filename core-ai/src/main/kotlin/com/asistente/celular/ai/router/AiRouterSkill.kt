package com.asistente.celular.ai.router

import com.asistente.celular.ai.analyzer.ComplexityAnalyzer
import com.asistente.celular.ai.analyzer.ComplexityLevel
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.harness.ModelHarness
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput

/**
 * Habilidad de Fallback y Enrutador Inteligente hacia Modelos de Lenguaje (LLM/SLM).
 * Incorpora el Model Routing Harness para delegar al modelo más apto.
 */
class AiRouterSkill(
    private val llmClient: LlmClient,
    private val modelHarness: ModelHarness = ModelHarness(),
    private val personalContextProvider: com.asistente.celular.ai.rag.PersonalContextProvider? = null,
    private val configProvider: () -> LlmConfig
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "ai_router_skill",
        name = "Inteligencia Artificial",
        description = "Responde a preguntas abiertas, razonamiento, resúmenes y conocimiento general.",
        isFallback = true
    )

    override val specificity: Specificity
        get() = Specificity.FALLBACK

    override fun score(context: SkillContext, input: String): SkillScore {
        val complexity = ComplexityAnalyzer.analyze(input)

        return if (complexity == ComplexityLevel.COMPLEX_AI_REQUIRED) {
            // Alta prioridad directa para preguntas explícitas a la IA (ej. "explícame qué es...")
            SkillScore(
                confidence = 0.95f,
                matchedWords = input.split(" ").size,
                totalWords = input.split(" ").size,
                specificity = Specificity.HIGH
            )
        } else {
            // Prioridad de fallback: captura todo lo que las habilidades locales no entiendan
            SkillScore(
                confidence = 0.50f,
                matchedWords = 0,
                totalWords = input.split(" ").size,
                specificity = Specificity.FALLBACK
            )
        }
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val baseConfig = configProvider()

        // Si está activo el modo Zero-Cloud, forzar estrictamente el proveedor LOCAL_SLM (100% privado en dispositivo)
        val activeConfig = if (baseConfig.zeroCloudMode) {
            baseConfig.copy(provider = AiProvider.LOCAL_SLM)
        } else {
            baseConfig
        }

        // Formatear el prompt del sistema con la personalidad activa (Jarvis, Friday, Mentor, etc.)
        val personalizedPrompt = com.asistente.celular.ai.personality.PersonalityEngine.formatSystemPrompt(
            basePrompt = activeConfig.systemPrompt,
            personality = activeConfig.personality
        )

        // 0. Enriquecimiento de contexto personal y memoria (RAG local)
        val enrichedPrompt = personalContextProvider?.buildEnrichedSystemPrompt(personalizedPrompt, input)
            ?: personalizedPrompt
        val config = activeConfig.copy(systemPrompt = enrichedPrompt)

        // 1. Verificación de conectividad offline
        if (!context.isConnectedToInternet && config.provider != AiProvider.LOCAL_SLM) {
            val offlineMsg = "Esta consulta requiere razonamiento de Inteligencia Artificial y actualmente no tienes conexión a internet."
            return SkillOutput(
                speech = offlineMsg,
                displayText = offlineMsg,
                success = false,
                handledByAi = true
            )
        }

        // 2. Comprobación de API Key si no es local
        if (config.provider in listOf(AiProvider.GEMINI, AiProvider.OPENAI, AiProvider.GROQ) && config.apiKey.isBlank()) {
            val noKeyMsg = "Para responder consultas con ${config.provider.displayName}, por favor configura tu clave de API en los ajustes del asistente."
            return SkillOutput(
                speech = noKeyMsg,
                displayText = noKeyMsg,
                success = false,
                handledByAi = true
            )
        }

        // 3. Ejecución: Si el Harness está activo y el proveedor es Gemini, usar enrutamiento dinámico
        if (config.provider == AiProvider.GEMINI && config.isModelHarnessEnabled) {
            val harnessResult = modelHarness.executeWithRouting(input, config)
            return if (harnessResult.isSuccess) {
                val data = harnessResult.getOrThrow()
                SkillOutput(
                    speech = data.responseText,
                    displayText = data.responseText,
                    success = true,
                    handledByAi = true,
                    payload = data
                )
            } else {
                val errorMsg = "No pude obtener respuesta de la IA: ${harnessResult.exceptionOrNull()?.message ?: "error de red"}"
                SkillOutput(
                    speech = errorMsg,
                    displayText = errorMsg,
                    success = false,
                    handledByAi = true
                )
            }
        }

        // 4. Ejecución estándar con modelo fijo
        val result = llmClient.generateResponse(input, config)

        return if (result.isSuccess) {
            val aiResponse = result.getOrThrow()
            SkillOutput(
                speech = aiResponse,
                displayText = aiResponse,
                success = true,
                handledByAi = true
            )
        } else {
            val errorMsg = "No pude obtener respuesta de la IA: ${result.exceptionOrNull()?.message ?: "error de red"}"
            SkillOutput(
                speech = errorMsg,
                displayText = errorMsg,
                success = false,
                handledByAi = true
            )
        }
    }
}
