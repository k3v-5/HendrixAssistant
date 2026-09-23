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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated

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
    var isDockCollapsed by remember { mutableStateOf(false) }
    var inlineTextInput by remember { mutableStateOf("") }
    val activeModifiers = remember { mutableStateListOf<String>() }

    if (isDockCollapsed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VoidSurfaceElevated.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    isDockCollapsed = false
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Muelle de Control",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expandir muelle",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(22.dp),
            color = VoidSurfaceElevated.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoidBorder)
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
                        tint = NeonCyan,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onStartVoiceDictation()
                        }
                    )

                    // 6. Desplegar / Colapsar teclado de modificadores
                    DockActionIcon(
                        icon = if (isKeyboardExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Keyboard,
                        tooltip = "Teclado PC",
                        tint = if (isKeyboardExpanded) NeonCyan else TextMuted,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            isKeyboardExpanded = !isKeyboardExpanded
                        }
                    )

                    // 7. Minimizar muelle para pantalla completa
                    DockActionIcon(
                        icon = Icons.Default.KeyboardArrowDown,
                        tooltip = "Minimizar",
                        tint = TextMuted,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            isDockCollapsed = true
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
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = VoidBorder,
                                focusedContainerColor = VoidBlack,
                                unfocusedContainerColor = VoidBlack,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
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
                                .background(NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar texto",
                                tint = VoidBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
            .background(if (isActive) NeonCyan.copy(alpha = 0.2f) else VoidBlack)
            .border(
                1.dp,
                if (isActive) NeonCyan else VoidBorder,
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
                tint = if (isActive) NeonCyan else TextMuted
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) TextPrimary else TextSecondary
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
            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else VoidBlack)
            .border(
                1.dp,
                if (isSelected) NeonCyan else VoidBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonCyan else TextMuted
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
            .background(VoidBlack)
            .border(1.dp, VoidBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}
