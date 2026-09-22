package com.asistente.celular.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Coordinador de la ventana flotante (Mini HUD) de Hendrix Assistant.
 * Utiliza WindowManager con TYPE_APPLICATION_OVERLAY para presentar el asistente
 * sin pausar ni minimizar la app activa en primer plano.
 */
class AssistantOverlayCoordinator(
    private val context: Context
) {

    companion object {
        private const val TAG = "AssistantOverlayCoord"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false

    val currentState = mutableStateOf(OverlayState.LISTENING)
    val transcriptionText = mutableStateOf("")
    val responseText = mutableStateOf("")

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        init {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showOverlay(
        initialCommand: String? = null,
        onExpandToApp: () -> Unit = {},
        onSendToPc: () -> Unit = {}
    ) {
        if (!canDrawOverlays()) {
            Log.w(TAG, "No se puede mostrar overlay: permiso de superposición no concedido.")
            return
        }

        mainHandler.post {
            if (isShowing) {
                if (!initialCommand.isNullOrBlank()) {
                    transcriptionText.value = initialCommand
                    currentState.value = OverlayState.PROCESSING
                }
                return@post
            }

            try {
                transcriptionText.value = initialCommand ?: ""
                responseText.value = ""
                currentState.value = if (!initialCommand.isNullOrBlank()) OverlayState.PROCESSING else OverlayState.LISTENING

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 140
                }
                layoutParams = params

                val owner = OverlayLifecycleOwner()
                lifecycleOwner = owner

                val view = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)

                    setContent {
                        MaterialTheme {
                            FloatingAssistantOverlayView(
                                state = currentState.value,
                                transcriptionText = transcriptionText.value,
                                responseText = responseText.value,
                                onDismiss = { dismiss() },
                                onExpandToApp = {
                                    dismiss()
                                    onExpandToApp()
                                },
                                onDragDelta = { dx, dy ->
                                    params.x += dx.toInt()
                                    params.y += dy.toInt()
                                    try {
                                        windowManager.updateViewLayout(this, params)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Error updating overlay drag: ${e.message}")
                                    }
                                },
                                onSendToPc = onSendToPc
                            )
                        }
                    }
                }

                composeView = view
                windowManager.addView(view, params)
                isShowing = true
                Log.i(TAG, "Overlay flotante mostrado exitosamente.")
            } catch (e: Exception) {
                Log.e(TAG, "Error al mostrar overlay flotante: ${e.message}", e)
                dismiss()
            }
        }
    }

    fun updateTranscription(text: String) {
        mainHandler.post {
            transcriptionText.value = text
            if (currentState.value != OverlayState.SPEAKING) {
                currentState.value = OverlayState.LISTENING
            }
        }
    }

    fun updateResponse(speech: String, display: String? = null) {
        mainHandler.post {
            currentState.value = OverlayState.SPEAKING
            responseText.value = display ?: speech

            // Auto-descarte después de que el usuario ve el resultado (6 segundos)
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({
                dismiss()
            }, 6500L)
        }
    }

    fun dismiss() {
        mainHandler.post {
            if (!isShowing) return@post
            try {
                composeView?.let { windowManager.removeView(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Error al remover vista de overlay: ${e.message}")
            } finally {
                composeView = null
                lifecycleOwner?.destroy()
                lifecycleOwner = null
                isShowing = false
                Log.i(TAG, "Overlay flotante cerrado.")
            }
        }
    }
}
