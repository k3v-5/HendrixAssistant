package com.asistente.celular.ui.pc

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction

/**
 * Molécula de productividad ergonómica para el móvil: barra flotante de atajos
 * y teclas especiales de Windows al alcance del pulgar.
 */
@Composable
fun PcProductivityToolbarMolecule(
    onSendAction: (PcInteractionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun sendHotkey(vararg keys: String) {
        triggerHaptic()
        onSendAction(
            PcInteractionAction(
                type = PcActionType.HOTKEY,
                keyCodes = keys.toList()
            )
        )
    }

    fun sendKey(key: String) {
        triggerHaptic()
        onSendAction(
            PcInteractionAction(
                type = PcActionType.KEY_PRESS,
                keyCodes = listOf(key)
            )
        )
    }

    fun sendScroll(deltaY: Float) {
        triggerHaptic()
        onSendAction(
            PcInteractionAction(
                type = PcActionType.SCROLL,
                scrollDeltaY = deltaY
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Guardar (Ctrl + S)
            KeyButton(label = "Ctrl+S", icon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp)) }) {
                sendHotkey("ctrl", "s")
            }

            // Deshacer (Ctrl + Z)
            KeyButton(label = "Ctrl+Z", icon = { Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp)) }) {
                sendHotkey("ctrl", "z")
            }

            // Copiar (Ctrl + C)
            KeyButton(label = "Ctrl+C", icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }) {
                sendHotkey("ctrl", "c")
            }

            // Pegar (Ctrl + V)
            KeyButton(label = "Ctrl+V", icon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp)) }) {
                sendHotkey("ctrl", "v")
            }

            // Alt + Tab
            KeyButton(label = "Alt+Tab") {
                sendHotkey("alt", "tab")
            }

            // Escape
            KeyButton(label = "Esc") {
                sendKey("escape")
            }

            // Tabulador
            KeyButton(label = "Tab") {
                sendKey("tab")
            }

            // Enter
            KeyButton(label = "Enter") {
                sendKey("enter")
            }

            // Flechas de navegación rápida
            IconButton(
                onClick = { sendKey("up") },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Arriba", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { sendKey("down") },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Abajo", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { sendKey("left") },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Izquierda", modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { sendKey("right") },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Derecha", modifier = Modifier.size(16.dp))
            }

            // Scroll rápido
            KeyButton(label = "Scroll ▲") {
                sendScroll(5f)
            }
            KeyButton(label = "Scroll ▼") {
                sendScroll(-5f)
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (icon != null) {
            icon()
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
