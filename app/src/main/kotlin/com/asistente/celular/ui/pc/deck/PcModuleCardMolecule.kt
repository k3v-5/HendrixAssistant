package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.module.PcModuleDefinition
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated

/**
 * Molécula de interfaz OLED de alta fidelidad que agrupa el panel táctil de control
 * para un módulo o software de PC (ej. Unreal Engine, Blender, Ableton Live, Adobe Suite).
 * Renderiza la cabecera temática del software y su rejilla de comandos macro de latencia ultrabaja.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PcModuleCardMolecule(
    module: PcModuleDefinition,
    onExecuteMacro: (PcModuleId, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(module.accentColorHex)

    Surface(
        color = VoidSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecera: Ícono, Nombre, Descripción y Categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = module.iconEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = module.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${module.category.displayName} • ${module.description}",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Activo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rejilla de comandos rápidos
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                module.quickMacros.forEach { macro ->
                    PcMacroButtonAtom(
                        macro = macro,
                        onClick = { onExecuteMacro(module.id, macro.actionId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
