package com.asistente.celular.ui.components.oled

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurfaceElevated

/**
 * Fila de cápsulas de telemetría del sistema "OLED Void & Neon Monolith".
 * Muestra métricas clave en tiempo real (CPU, RAM, GPU/PC) con estética de telemetría cuántica en ámbar.
 */
@Composable
fun OledTelemetryPillBar(
    isPcConnected: Boolean,
    telemetry: PcSystemTelemetry? = null,
    onOpenPcDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPcConnected && telemetry != null) {
            // CPU Pill
            val cpuVal = "${telemetry.cpuPercent.toInt()}%"
            TelemetryCapsule(
                label = "CPU",
                value = cpuVal,
                accentColor = NeonAmber,
                onClick = onOpenPcDeck
            )

            // RAM Pill
            val ramVal = "${telemetry.ramPercent.toInt()}%"
            TelemetryCapsule(
                label = "RAM",
                value = ramVal,
                accentColor = NeonAmber,
                onClick = onOpenPcDeck
            )

            // Latencia / Memoria
            val latencyVal = "${telemetry.roundTripLatencyMs}ms"
            TelemetryCapsule(
                label = "PING",
                value = latencyVal,
                accentColor = NeonCyan,
                onClick = onOpenPcDeck
            )
        } else {
            // Estado Offline / Conexión PC
            TelemetryCapsule(
                label = "STATUS",
                value = if (isPcConnected) "ONLINE" else "LOCAL",
                accentColor = if (isPcConnected) NeonGreen else NeonCyan,
                onClick = onOpenPcDeck
            )

            TelemetryCapsule(
                label = "PC",
                value = if (isPcConnected) "LINKED" else "OFFLINE",
                accentColor = if (isPcConnected) NeonCyan else NeonRed,
                onClick = onOpenPcDeck
            )

            TelemetryCapsule(
                label = "ENGINE",
                value = "100%",
                accentColor = NeonAmber,
                onClick = onOpenPcDeck
            )
        }
    }
}

@Composable
fun TelemetryCapsule(
    label: String,
    value: String,
    accentColor: Color = NeonAmber,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .border(
                BorderStroke(1.dp, VoidBorder),
                shape = RoundedCornerShape(20.dp)
            ),
        color = VoidSurfaceElevated,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Micro-badge del label (CPU, RAM, GPU)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Valor numérico en blanco de alto contraste
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
        }
    }
}
