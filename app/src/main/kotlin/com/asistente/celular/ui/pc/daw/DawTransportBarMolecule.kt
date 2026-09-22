package com.asistente.celular.ui.pc.daw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.daw.DawAction

/**
 * Molécula atómica de transporte de producción musical para Ableton Live (y DAWs compatibles).
 * Proporciona controles táctiles directos de reproducción, grabación, bucle de arreglo
 * y exportación rápida de mezclas.
 */
@Composable
fun DawTransportBarMolecule(
    isPlaying: Boolean,
    isRecording: Boolean,
    onSendAction: (DawAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play / Pausa (Barra espaciadora)
        FilledTonalButton(
            onClick = { onSendAction(DawAction.PLAY_PAUSE) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isPlaying) Color(0xFF10B981) else MaterialTheme.colorScheme.surface,
                contentColor = if (isPlaying) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPlaying) "Pausar" else "Play",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Grabación (F9)
        FilledTonalButton(
            onClick = { onSendAction(DawAction.RECORD) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.surface,
                contentColor = if (isRecording) Color.White else Color(0xFFEF4444)
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isRecording) Color.White else Color(0xFFEF4444), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRecording) "Grabando" else "Rec",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bucle de Arreglo (Ctrl+L)
        FilledTonalIconButton(
            onClick = { onSendAction(DawAction.TOGGLE_LOOP) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(38.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Loop",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Metrónomo (C)
        FilledTonalIconButton(
            onClick = { onSendAction(DawAction.TOGGLE_METRONOME) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(38.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Metrónomo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Exportar Audio (Ctrl+Shift+R)
        Button(
            onClick = { onSendAction(DawAction.EXPORT_AUDIO) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6)
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.IosShare,
                contentDescription = "Exportar",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Exportar Audio",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
