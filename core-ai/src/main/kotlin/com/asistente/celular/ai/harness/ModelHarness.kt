package com.asistente.celular.ai.harness

import android.util.Log
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.model.LlmConfig

/**
 * Resultado integral devuelto por el Harness tras orquestar el enrutamiento y la ejecución.
 */
data class HarnessResult(
    val responseText: String,
    val usedModel: ModelProfile,
    val decision: RoutingDecision,
    val isFallbackUsed: Boolean = false,
    val latencyMs: Long = 0L
)

/**
 * Orquestador principal del Model Routing Harness.
 * Coordina la evaluación del router, la llamada al modelo seleccionado y la tolerancia a fallos.
 */
class ModelHarness(
    private val router: ModelRouter = GeminiHeuristicRouter(),
    private val modelPool: List<ModelProfile> = ModelRegistry.DEFAULT_GEMINI_POOL,
    private val clientFactory: (LlmConfig) -> LlmClient = { config -> LlmClient { config } }
) {
    /**
     * Ejecuta la consulta utilizando el modelo más apto según el Harness.
     */
    suspend fun executeWithRouting(
        prompt: String,
        baseConfig: LlmConfig
    ): Result<HarnessResult> {
        val startTime = System.currentTimeMillis()

        // 1. Evaluar el modelo más idóneo para la consulta
        val decision = router.route(prompt, modelPool)
        Log.d(TAG, "Harness enrutó hacia '${decision.selectedModel.displayName}'. Razón: ${decision.reason}")

        // 2. Intentar ejecutar con el modelo seleccionado
        val primaryConfig = baseConfig.copy(
            modelName = decision.selectedModel.modelId
        )
        val primaryClient = clientFactory(primaryConfig)
        val primaryResult = primaryClient.generateResponse(prompt)

        if (primaryResult.isSuccess) {
            val latency = System.currentTimeMillis() - startTime
            return Result.success(
                HarnessResult(
                    responseText = primaryResult.getOrThrow(),
                    usedModel = decision.selectedModel,
                    decision = decision,
                    isFallbackUsed = false,
                    latencyMs = latency
                )
            )
        }

        // 3. Tolerancia a fallos: Si el modelo avanzado falló (ej: rate-limit 429), probar con el modelo de respaldo (Flash)
        val failureError = primaryResult.exceptionOrNull()
        Log.w(TAG, "Fallo en modelo primario (${decision.selectedModel.modelId}): ${failureError?.message}. Intentando fallback a '${decision.fallbackModel.displayName}'...")

        if (decision.selectedModel.modelId != decision.fallbackModel.modelId) {
            val fallbackConfig = baseConfig.copy(
                modelName = decision.fallbackModel.modelId
            )
            val fallbackClient = clientFactory(fallbackConfig)
            val fallbackResult = fallbackClient.generateResponse(prompt)

            if (fallbackResult.isSuccess) {
                val latency = System.currentTimeMillis() - startTime
                Log.i(TAG, "Recuperación exitosa mediante fallback al modelo Flash.")
                return Result.success(
                    HarnessResult(
                        responseText = fallbackResult.getOrThrow(),
                        usedModel = decision.fallbackModel,
                        decision = decision,
                        isFallbackUsed = true,
                        latencyMs = latency
                    )
                )
            }
        }

        // Si ambos fallaron, retornar el error original
        return Result.failure(failureError ?: IllegalStateException("Error al invocar modelo de IA."))
    }

    companion object {
        private const val TAG = "ModelHarness"
    }
}
