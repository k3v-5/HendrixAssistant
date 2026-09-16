package com.asistente.celular.nlu.agent

/**
 * Acciones atómicas de navegación e interacción de interfaz de usuario (RPA para Android).
 */
sealed interface UiNavigationAction {
    data class ClickByText(val text: String, val exactMatch: Boolean = false) : UiNavigationAction
    data class ClickById(val viewId: String) : UiNavigationAction
    data class TypeText(val text: String) : UiNavigationAction
    data object ScrollForward : UiNavigationAction
    data object ScrollBackward : UiNavigationAction
    data object PressBack : UiNavigationAction
    data object PressHome : UiNavigationAction
    data class WaitForElement(val text: String, val timeoutMs: Long = 3000L) : UiNavigationAction
}

/**
 * Resultado de una operación ejecutada por el agente de navegación autónomo.
 */
data class NavigationResult(
    val success: Boolean,
    val message: String,
    val targetFound: Boolean = success
)

/**
 * Contrato desacoplado para el agente de navegación autónomo de Android (OCP).
 * Permite ejecutar acciones simuladas en pruebas y desacopla la capa NLU del servicio de accesibilidad.
 */
interface AutonomousNavigationAgent {
    fun isEnabled(): Boolean
    suspend fun executeAction(action: UiNavigationAction): NavigationResult
    suspend fun findTextOnScreen(text: String): Boolean
}
