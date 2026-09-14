package com.asistente.celular.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlamaCppInferenceEngineTest {

    @Test
    fun testQwenPromptUsesChatML() {
        val prompt = "Explica qué es la gravedad"
        val systemPrompt = "Eres un asistente útil"
        val formatted = LlamaCppInferenceEngine.formatPromptForModel(
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelId = "qwen2.5-0.5b"
        )

        assertTrue(formatted.contains("<|im_start|>system\n$systemPrompt<|im_end|>"))
        assertTrue(formatted.contains("<|im_start|>user\n$prompt<|im_end|>"))
        assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun testSmolLMPromptUsesLlama3Format() {
        val prompt = "Hola Hendrix"
        val systemPrompt = "Responde brevemente"
        val formatted = LlamaCppInferenceEngine.formatPromptForModel(
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelId = "smollm2-135m"
        )

        assertTrue(formatted.contains("<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>"))
        assertTrue(formatted.contains("<|start_header_id|>user<|end_header_id|>\n\n$prompt<|eot_id|>"))
        assertTrue(formatted.endsWith("<|start_header_id|>assistant<|end_header_id|>\n\n"))
    }

    @Test
    fun testCleanModelOutputStripsSpecialTokens() {
        val raw = "<|im_start|>assistant\nLa gravedad es una fuerza fundamental.<|im_end|>"
        val cleaned = LlamaCppInferenceEngine.cleanModelOutput(raw)
        assertEquals("assistant\nLa gravedad es una fuerza fundamental.", cleaned)

        val rawLlama = "Hola, ¿en qué te puedo ayudar hoy?<|eot_id|>"
        val cleanedLlama = LlamaCppInferenceEngine.cleanModelOutput(rawLlama)
        assertEquals("Hola, ¿en qué te puedo ayudar hoy?", cleanedLlama)
    }
}
