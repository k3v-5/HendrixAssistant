package com.asistente.celular.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.asistente.celular.nlu.agent.AutonomousNavigationAgent
import com.asistente.celular.nlu.agent.NavigationResult
import com.asistente.celular.nlu.agent.UiNavigationAction
import kotlinx.coroutines.delay

/**
 * Servicio de accesibilidad nativo para navegación autónoma y RPA en Android.
 * Permite a Hendrix inspeccionar nodos de pantalla, hacer clic, scroll, escribir texto y automatizar apps como Spotify.
 */
class HendrixAccessibilityService : AccessibilityService(), AutonomousNavigationAgent {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "HendrixAccessibilityService conectado con éxito.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Eventos de cambio de ventana y contenido de pantalla
    }

    override fun onInterrupt() {
        Log.w(TAG, "HendrixAccessibilityService interrumpido.")
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun isEnabled(): Boolean {
        return instance != null
    }

    override suspend fun executeAction(action: UiNavigationAction): NavigationResult {
        val root = rootInActiveWindow ?: return NavigationResult(
            success = false,
            message = "No se pudo acceder a la ventana activa en pantalla.",
            targetFound = false
        )

        return when (action) {
            is UiNavigationAction.ClickByText -> {
                val clicked = clickNodeByText(root, action.text, action.exactMatch)
                NavigationResult(
                    success = clicked,
                    message = if (clicked) "Clic realizado en '${action.text}'." else "No se encontró el elemento '${action.text}' para hacer clic.",
                    targetFound = clicked
                )
            }
            is UiNavigationAction.ClickById -> {
                val clicked = clickNodeById(root, action.viewId)
                NavigationResult(
                    success = clicked,
                    message = if (clicked) "Clic realizado en ID '${action.viewId}'." else "No se encontró el elemento con ID '${action.viewId}'.",
                    targetFound = clicked
                )
            }
            is UiNavigationAction.TypeText -> {
                val typed = typeTextIntoFocusedNode(root, action.text)
                NavigationResult(
                    success = typed,
                    message = if (typed) "Texto ingresado con éxito." else "No se encontró un campo de texto activo para escribir."
                )
            }
            is UiNavigationAction.ScrollForward -> {
                val scrolled = scrollNode(root, forward = true)
                NavigationResult(
                    success = scrolled,
                    message = if (scrolled) "Desplazamiento hacia adelante realizado." else "No se encontró contenedor desplazable."
                )
            }
            is UiNavigationAction.ScrollBackward -> {
                val scrolled = scrollNode(root, forward = false)
                NavigationResult(
                    success = scrolled,
                    message = if (scrolled) "Desplazamiento hacia atrás realizado." else "No se encontró contenedor desplazable."
                )
            }
            is UiNavigationAction.PressBack -> {
                val performed = performGlobalAction(GLOBAL_ACTION_BACK)
                NavigationResult(success = performed, message = "Acción Atrás ejecutada.")
            }
            is UiNavigationAction.PressHome -> {
                val performed = performGlobalAction(GLOBAL_ACTION_HOME)
                NavigationResult(success = performed, message = "Acción Inicio ejecutada.")
            }
            is UiNavigationAction.WaitForElement -> {
                val startTime = System.currentTimeMillis()
                var found = false
                while (System.currentTimeMillis() - startTime < action.timeoutMs) {
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null && findNodeByText(currentRoot, action.text, exactMatch = false) != null) {
                        found = true
                        break
                    }
                    delay(200)
                }
                NavigationResult(
                    success = found,
                    message = if (found) "Elemento '${action.text}' detectado en pantalla." else "Tiempo de espera agotado para '${action.text}'."
                )
            }
        }
    }

    override suspend fun findTextOnScreen(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findNodeByText(root, text, exactMatch = false) != null
    }

    /**
     * Automatización especializada para Spotify: da Me Gusta (corazón) a la canción actual.
     */
    suspend fun likeSpotifyCurrentTrack(): NavigationResult {
        val root = rootInActiveWindow ?: return NavigationResult(false, "Spotify no está activo en pantalla.")
        val heartNode = findNodeRecursive(root) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""
            desc.contains("me gusta") || desc.contains("guardar en tu biblioteca") ||
            desc.contains("like") || id.contains("heart") || id.contains("btn_like")
        }

        return if (heartNode != null && performClick(heartNode)) {
            NavigationResult(true, "Canción agregada a tus Me Gusta en Spotify.")
        } else {
            NavigationResult(false, "No se encontró el botón de Me Gusta en la pantalla de Spotify.")
        }
    }

    private fun clickNodeByText(root: AccessibilityNodeInfo, text: String, exactMatch: Boolean): Boolean {
        val node = findNodeByText(root, text, exactMatch) ?: return false
        return performClick(node)
    }

    private fun clickNodeById(root: AccessibilityNodeInfo, viewId: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        val target = nodes.firstOrNull() ?: return false
        return performClick(target)
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            curr = curr.parent
        }
        return false
    }

    private fun typeTextIntoFocusedNode(root: AccessibilityNodeInfo, text: String): Boolean {
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findNodeRecursive(root) { it.isEditable }
            ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scrollNode(root: AccessibilityNodeInfo, forward: Boolean): Boolean {
        val scrollable = findNodeRecursive(root) { it.isScrollable } ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String, exactMatch: Boolean): AccessibilityNodeInfo? {
        val candidates = root.findAccessibilityNodeInfosByText(text)
        return if (exactMatch) {
            candidates.firstOrNull { it.text?.toString().equals(text, ignoreCase = true) }
        } else {
            candidates.firstOrNull()
        } ?: findNodeRecursive(root) { node ->
            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""
            if (exactMatch) {
                nodeText.equals(text, ignoreCase = true) || nodeDesc.equals(text, ignoreCase = true)
            } else {
                nodeText.contains(text, ignoreCase = true) || nodeDesc.contains(text, ignoreCase = true)
            }
        }
    }

    private fun findNodeRecursive(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeRecursive(child, predicate)
            if (found != null) return found
        }
        return null
    }

    companion object {
        private const val TAG = "HendrixAccessibility"
        @Volatile
        var instance: HendrixAccessibilityService? = null

        fun requestEnableIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
