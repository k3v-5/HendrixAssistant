package com.asistente.celular.nlu.pc

/**
 * Representa un proyecto o espacio de trabajo registrado en Antigravity.
 */
data class AntigravityProject(
    val id: String,
    val name: String,
    val workspaceUri: String,
    val lastActiveEpoch: Long = 0,
    val totalConversations: Int = 0
)

/**
 * Representa una conversación o chat dentro de un proyecto de Antigravity.
 */
data class AntigravityChat(
    val conversationId: String,
    val title: String,
    val preview: String,
    val lastModifiedEpoch: Long = 0,
    val stepCount: Int = 0,
    val projectId: String = "",
    val workspaceUri: String = ""
)

/**
 * Modo de destino al interactuar con Antigravity.
 */
enum class AntigravityTargetMode {
    /** Abrir un chat nuevo en el proyecto y redactar prompt. */
    NEW_CHAT,

    /** Continuar una conversación existente y redactar prompt. */
    EXISTING_CHAT,

    /** Únicamente conmutar o enfocar el proyecto en Antigravity. */
    SWITCH_PROJECT_ONLY,

    /** Lanzar o enfocar la aplicación de escritorio Antigravity. */
    LAUNCH_OR_FOCUS
}
