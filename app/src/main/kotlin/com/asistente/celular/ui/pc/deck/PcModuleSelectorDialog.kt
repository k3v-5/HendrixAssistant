package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.module.PcModuleCategory
import com.asistente.celular.nlu.pc.module.PcModuleDefinition
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated

/**
 * Diálogo interactivo OLED de alta fidelidad para habilitar o deshabilitar módulos visibles
 * en el Quick Command Deck. Incluye filtros por categoría y acciones rápidas masivas.
 */
@Composable
fun PcModuleSelectorDialog(
    allModules: List<PcModuleDefinition>,
    onToggleModule: (PcModuleId) -> Unit,
    onEnableAll: (() -> Unit)? = null,
    onDisableAll: (() -> Unit)? = null,
    onResetDefaults: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategoryFilter by remember { mutableStateOf<PcModuleCategory?>(null) }

    val filteredModules = remember(allModules, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allModules
        else allModules.filter { it.category == selectedCategoryFilter }
    }

    val activeCount = remember(allModules) { allModules.count { it.isEnabledByDefault } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoidSurface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonLilac.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = NeonLilac,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gestor de Suites & Deck",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Controla qué software aparece en tu panel táctil",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Contador de módulos activos
                    Surface(
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$activeCount / ${allModules.size} activos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Acciones rápidas masivas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onEnableAll != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnableAll()
                            },
                            border = BorderStroke(1.dp, VoidBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Activar Todos", fontSize = 10.sp, color = TextPrimary)
                        }
                    }

                    if (onDisableAll != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDisableAll()
                            },
                            border = BorderStroke(1.dp, VoidBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ninguno", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    if (onResetDefaults != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetDefaults()
                            },
                            border = BorderStroke(1.dp, VoidBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonLilac, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Predeterminados", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chips de filtro por categoría
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("Todos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = VoidSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (selectedCategoryFilter == null) NeonCyan else VoidBorder),
                        shape = RoundedCornerShape(8.dp)
                    )

                    PcModuleCategory.values().forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = if (isSelected) null else cat },
                            label = { Text("${cat.iconEmoji} ${cat.displayName}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonLilac.copy(alpha = 0.25f),
                                selectedLabelColor = NeonLilac,
                                containerColor = VoidSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) NeonLilac else VoidBorder),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredModules, key = { it.id.name }) { module ->
                    val accentColor = Color(module.accentColorHex)
                    val isEnabled = module.isEnabledByDefault

                    Surface(
                        color = if (isEnabled) VoidSurfaceElevated else VoidBlack,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isEnabled) accentColor.copy(alpha = 0.45f) else VoidBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleModule(module.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(accentColor.copy(alpha = if (isEnabled) 0.22f else 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = module.iconEmoji, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = module.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isEnabled) TextPrimary else TextSecondary
                                        )
                                        if (module.quickMacros.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = accentColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${module.quickMacros.size} macros",
                                                    fontSize = 9.sp,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = module.description,
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        maxLines = 2
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleModule(module.id)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = VoidBlack,
                                    uncheckedBorderColor = VoidBorder
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold, color = VoidBlack, fontSize = 13.sp)
            }
        }
    )
}
