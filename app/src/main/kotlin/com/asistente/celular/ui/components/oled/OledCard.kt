package com.asistente.celular.ui.components.oled

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface

/**
 * Contenedor atómico estándar para tarjetas y módulos del sistema "OLED Void".
 * Provee un contenedor nítido de 1px con radio estándar de 16dp y compatibilidad interactiva.
 */
@Composable
fun OledCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = VoidSurface,
    borderColor: Color = VoidBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape = shape)
            .clickable { onClick() }
    } else {
        modifier
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape = shape)
    }

    Surface(
        modifier = cardModifier,
        color = backgroundColor,
        shape = shape
    ) {
        Box(content = content)
    }
}
