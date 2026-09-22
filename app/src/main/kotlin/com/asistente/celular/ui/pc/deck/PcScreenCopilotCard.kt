package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult
import kotlinx.coroutines.launch

@Composable
fun PcScreenCopilotCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val telemetry by pcBridge.telemetry.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var cropToActiveWindow by remember { mutableStateOf(true) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<PcScreenAnalysisResult?>(null) }

    val activeWindow = telemetry?.activeWindowTitle ?: "Sin ventana activa"

    fun executeAnalysis(query: String) {
        val effectiveQuery = query.ifBlank { "Diagnostica la pantalla actual, identifica errores, procesos y sugiere soluciones." }
        isAnalyzing = true
        scope.launch {
            try {
                val res = pcBridge.analyzeScreenWithAi(
                    prompt = effectiveQuery,
                    cropToActiveWindow = cropToActiveWindow
                )
                analysisResult = res
                if (res.success) {
                    onShowSnackbar("Diagnóstico completado con éxito")
                } else {
                    onShowSnackbar("Error: ${res.errorSummary ?: "Fallo al analizar"}")
                }
            } catch (e: Exception) {
                analysisResult = PcScreenAnalysisResult(
                    success = false,
                    analysisMarkdown = "Error inesperado: ${e.localizedMessage ?: e.message}",
                    errorSummary = e.localizedMessage
                )
                onShowSnackbar("Error al ejecutar copilot: ${e.localizedMessage}")
            } finally {
                isAnalyzing = false
            }
        }
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👁️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Screen Copilot",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (activeWindow.isNotBlank()) "Foco: $activeWindow" else "Diagnóstico visual bajo demanda",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switch solo ventana activa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (cropToActiveWindow) "Recortar a ventana en foco" else "Capturar pantalla completa",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (cropToActiveWindow) "Mayor resolución en la app activa" else "Diagnostica todo el monitor",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = cropToActiveWindow,
                    onCheckedChange = { cropToActiveWindow = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips de sugerencias rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        promptText = "Diagnostica errores o excepciones visibles en esta ventana y cómo solucionarlos."
                        executeAnalysis(promptText)
                    },
                    label = { Text("⚠️ Errores", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFF87171)
                    )
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        promptText = "Explica qué está sucediendo en esta pantalla o qué proceso se está ejecutando."
                        executeAnalysis(promptText)
                    },
                    label = { Text("🔍 ¿Qué pasa?", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF60A5FA)
                    )
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        promptText = "¿Cómo va el render o progreso de la tarea y cuánto le falta aproximadamente?"
                        executeAnalysis(promptText)
                    },
                    label = { Text("⏳ Render", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF34D399)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Campo de texto de consulta personalizada
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = { Text("Pregunta algo específico sobre tu pantalla...", fontSize = 12.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    if (promptText.isNotBlank()) {
                        IconButton(onClick = { promptText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botón de acción principal
            Button(
                onClick = { executeAnalysis(promptText) },
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizando pantalla con IA...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("🚀 Diagnosticar Pantalla Bajo Demanda", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Resultado del análisis si existe
            analysisResult?.let { res ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (res.success) Color(0xFF0F172A) else Color(0xFF7F1D1D).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (res.success) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (res.success) "📋 Diagnóstico de IA:" else "⚠️ Error:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) Color(0xFF60A5FA) else Color(0xFFF87171)
                            )
                            Row {
                                if (res.success) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(res.analysisMarkdown))
                                            onShowSnackbar("Diagnóstico copiado al portapapeles")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Copiar", fontSize = 11.sp, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Button(
                                    onClick = { analysisResult = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Cerrar", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = res.analysisMarkdown,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
