package com.asistente.celular.ui.pc

import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction

/**
 * Muelle de Acciones Flotante (PcFloatingActionDock) de ultra alta ergonomía móvil.
 * Provee accesos directos táctiles rápidos para controlar la computadora:
 * - Inyección directa del portapapeles del teléfono a la PC.
 * - Conmutador rápido de ventanas (Alt+Tab).
 * - Alternar modo enfoque (Auto-crop de ventana activa vs escritorio completo).
 * - Alternar Lupa de precisión HD.
 * - Teclado virtual expandible con modificadores físicos de Windows (Ctrl, Alt, Win, Shift, Esc, etc.).
 */
@Composable
fun PcFloatingActionDock(
    isFocusWindowActive: Boolean,
    onToggleFocusWindow: () -> Unit,
    isLoupeActive: Boolean,
    onToggleLoupe: () -> Unit,
    onSendAction: (PcInteractionAction) -> Unit,
    onTypeText: (String) -> Unit,
    onStartVoiceDictation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    var isKeyboardExpanded by remember { mutableStateOf(false) }
    var inlineTextInput by remember { mutableStateOf("") }
    val activeModifiers = remember { mutableStateListOf<String>() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fila principal: Cápsula de Acciones Rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Pegar portapapeles de Android en la PC
                DockActionIcon(
                    icon = Icons.Default.ContentPaste,
                    tooltip = "Pegar",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clipText.isNullOrBlank()) {
                            onTypeText(clipText)
                        }
                    }
                )

                // 2. Alt+Tab para ciclar ventanas
                DockActionIcon(
                    icon = Icons.Default.Tab,
                    tooltip = "Alt+Tab",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.HOTKEY,
                                keyCodes = listOf("alt", "tab")
                            )
                        )
                    }
                )

                // 3. Conmutar Auto-crop a Ventana Activa
                DockActionPill(
                    text = if (isFocusWindowActive) "Ventana" else "Pantalla",
                    icon = if (isFocusWindowActive) Icons.Default.CenterFocusStrong else Icons.Default.FitScreen,
                    isActive = isFocusWindowActive,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onToggleFocusWindow()
                    }
                )

                // 4. Conmutar Lupa HD de Precisión
                DockActionPill(
                    text = "Lupa",
                    icon = Icons.Default.Search,
                    isActive = isLoupeActive,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onToggleLoupe()
                    }
                )

                // 5. Dictado por voz
                DockActionIcon(
                    icon = Icons.Default.Mic,
                    tooltip = "Dictar",
                    tint = Color(0xFF38BDF8),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onStartVoiceDictation()
                    }
                )

                // 6. Desplegar / Colapsar teclado de modificadores
                DockActionIcon(
                    icon = if (isKeyboardExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Keyboard,
                    tooltip = "Teclado PC",
                    tint = if (isKeyboardExpanded) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        isKeyboardExpanded = !isKeyboardExpanded
                    }
                )
            }

            // Panel expandible de modificadores y teclas especiales de Windows
            AnimatedVisibility(
                visible = isKeyboardExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fila horizontal deslizable de teclas modificadoras y de navegación
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Modificadores adhesivos
                        listOf("Ctrl", "Alt", "Shift", "Win").forEach { mod ->
                            val isToggled = activeModifiers.contains(mod.lowercase())
                            ModifierChip(
                                label = mod,
                                isSelected = isToggled,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val key = mod.lowercase()
                                    if (isToggled) {
                                        activeModifiers.remove(key)
                                    } else {
                                        activeModifiers.add(key)
                                    }
                                }
                            )
                        }

                        // Teclas directas de acción
                        SpecialKeyButton("Esc") {
                            sendKeyOrHotkey(activeModifiers, "esc", onSendAction)
                        }
                        SpecialKeyButton("Tab") {
                            sendKeyOrHotkey(activeModifiers, "tab", onSendAction)
                        }
                        SpecialKeyButton("Enter") {
                            sendKeyOrHotkey(activeModifiers, "enter", onSendAction)
                        }
                        SpecialKeyButton("⌫ Borrar") {
                            sendKeyOrHotkey(activeModifiers, "backspace", onSendAction)
                        }
                        SpecialKeyButton("Supr") {
                            sendKeyOrHotkey(activeModifiers, "delete", onSendAction)
                        }
                        SpecialKeyButton("Win+D") {
                            onSendAction(PcInteractionAction(type = PcActionType.HOTKEY, keyCodes = listOf("win", "d")))
                        }
                        SpecialKeyButton("Alt+F4") {
                            onSendAction(PcInteractionAction(type = PcActionType.HOTKEY, keyCodes = listOf("alt", "f4")))
                        }
                        SpecialKeyButton("Ctrl+Z") {
                            onSendAction(PcInteractionAction(type = PcActionType.HOTKEY, keyCodes = listOf("ctrl", "z")))
                        }
                        SpecialKeyButton("Ctrl+C") {
                            onSendAction(PcInteractionAction(type = PcActionType.HOTKEY, keyCodes = listOf("ctrl", "c")))
                        }
                        SpecialKeyButton("Ctrl+V") {
                            onSendAction(PcInteractionAction(type = PcActionType.HOTKEY, keyCodes = listOf("ctrl", "v")))
                        }

                        // Teclas de dirección
                        SpecialKeyButton("↑") {
                            sendKeyOrHotkey(activeModifiers, "up", onSendAction)
                        }
                        SpecialKeyButton("↓") {
                            sendKeyOrHotkey(activeModifiers, "down", onSendAction)
                        }
                        SpecialKeyButton("←") {
                            sendKeyOrHotkey(activeModifiers, "left", onSendAction)
                        }
                        SpecialKeyButton("→") {
                            sendKeyOrHotkey(activeModifiers, "right", onSendAction)
                        }
                    }

                    // Campo de entrada de texto directo en línea para enviar rápidamente
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = inlineTextInput,
                            onValueChange = { inlineTextInput = it },
                            placeholder = {
                                Text(
                                    text = if (activeModifiers.isNotEmpty()) "Escribir con [${activeModifiers.joinToString("+").uppercase()}]..." else "Escribir directamente en PC...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        IconButton(
                            onClick = {
                                if (inlineTextInput.isNotBlank()) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onTypeText(inlineTextInput)
                                    inlineTextInput = ""
                                    activeModifiers.clear()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar texto",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sendKeyOrHotkey(
    activeModifiers: List<String>,
    key: String,
    onSendAction: (PcInteractionAction) -> Unit
) {
    if (activeModifiers.isEmpty()) {
        onSendAction(
            PcInteractionAction(
                type = PcActionType.KEY_PRESS,
                keyCodes = listOf(key)
            )
        )
    } else {
        val hotkeys = activeModifiers + key
        onSendAction(
            PcInteractionAction(
                type = PcActionType.HOTKEY,
                keyCodes = hotkeys
            )
        )
    }
}

@Composable
private fun DockActionIcon(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE2E8F0)
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DockActionPill(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF0F172A))
            .border(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Color.White else Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun ModifierChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A))
            .border(
                1.dp,
                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )
    }
}
