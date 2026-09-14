package com.asistente.celular.ai.local

/**
 * Catálogo central de modelos de lenguaje ligeros optimizados para smartphones.
 * Diseñado con arquitectura abierta a la extensión (OCP).
 */
object LocalModelCatalog {

    val SMOLLM2_135M = LocalModelSpec(
        id = "smollm2-135m",
        name = "SmolLM2 135M Instruct",
        description = "Ultra ligero y veloz. Ideal para pruebas inmediatas y celulares con memoria reducida.",
        parameterCount = "135M",
        quantization = "Q4_K_M",
        sizeBytes = 105_454_432L, // ~100.5 MB
        fileName = "SmolLM2-135M-Instruct-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf",
        recommendedRamGb = 2,
        format = LocalModelFormat.GGUF
    )

    val SMOLLM2_360M = LocalModelSpec(
        id = "smollm2-360m",
        name = "SmolLM2 360M Instruct",
        description = "Equilibrio óptimo entre velocidad y coherencia textual con mínimo consumo.",
        parameterCount = "360M",
        quantization = "Q4_K_M",
        sizeBytes = 270_590_880L, // ~258 MB
        fileName = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
        recommendedRamGb = 3,
        format = LocalModelFormat.GGUF
    )

    val QWEN_2_5_0_5B = LocalModelSpec(
        id = "qwen2.5-0.5b",
        name = "Qwen 2.5 0.5B Instruct",
        description = "Excelente fluidez en español, capacidad de resumen y seguimiento de instrucciones.",
        parameterCount = "0.5B",
        quantization = "Q4_K_M",
        sizeBytes = 491_400_032L, // ~468 MB
        fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
        recommendedRamGb = 4,
        format = LocalModelFormat.GGUF
    )

    val GEMMA_2_2B = LocalModelSpec(
        id = "gemma-2-2b",
        name = "Gemma 2 2B Instruct",
        description = "Máxima capacidad de razonamiento on-device. Recomendado para gama media-alta.",
        parameterCount = "2B",
        quantization = "Q4_K_M",
        sizeBytes = 1_708_582_752L, // ~1.62 GB
        fileName = "gemma-2-2b-it-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
        recommendedRamGb = 6,
        format = LocalModelFormat.GGUF
    )

    val ALL_MODELS: List<LocalModelSpec> = listOf(
        SMOLLM2_135M,
        SMOLLM2_360M,
        QWEN_2_5_0_5B,
        GEMMA_2_2B
    )

    val DEFAULT_LOCAL_MODEL: LocalModelSpec = SMOLLM2_135M

    fun findById(id: String): LocalModelSpec? {
        return ALL_MODELS.find { it.id.equals(id, ignoreCase = true) }
    }
}
