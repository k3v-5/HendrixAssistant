package com.asistente.celular.ui.components.oled

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonMagenta
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.NeonPurpleGlow
import com.asistente.celular.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbe Gigante Líquido Morado ("Liquid Purple Entity") de Hendrix Assistant.
 *
 * Simula una gota viva de plasma líquido bioluminiscente en tonalidades púrpura, violeta y lila.
 * Presenta deformación armónica continua mediante curvas Bézier orgánicas, múltiples capas
 * viscosas fluidas, brillo de tensión superficial (3D specular sheen) y micro-burbujas en suspensión.
 *
 * Reacciona dinámicamente según el estado del asistente:
 * - Reposo: Respiración fluida lenta, armónica e hipnótica.
 * - Escuchando: Ondulaciones rápidas de alta tensión superficial con bioluminiscencia en lila y magenta.
 * - Procesando: Vórtice rotacional continuo de plasma morado.
 * - Hablando: Expansiones rítmicas diafragmáticas con ondas líquidas concéntricas.
 */
@Composable
fun OledVoiceOrb(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidPurpleOrbMotion")

    // Ciclo de pulsación según estado
    val pulseDuration = when {
        isListening -> 850
        isProcessing -> 600
        isSpeaking -> 1100
        else -> 3200
    }

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isListening) 1.15f else if (isSpeaking) 1.10f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Fases de onda para deformación líquida armónica (Múltiples osciladores a distintas frecuencias)
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 2800 else 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = (2 * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 2200 else 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1600 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    // Desfase de ondas de choque concéntricas (efecto gota cayendo en líquido)
    val rippleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 1400 else 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleOffset"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f
            val baseCoreRadius = maxRadius * 0.48f * pulseScale

            // 1. Resplandor exterior de vapor violeta (Atmospheric Liquid Glow)
            val outerAuraColor = when {
                isListening -> NeonMagenta.copy(alpha = 0.35f)
                isSpeaking -> NeonPurple.copy(alpha = 0.30f)
                isProcessing -> NeonLilac.copy(alpha = 0.35f)
                else -> NeonViolet.copy(alpha = 0.18f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerAuraColor,
                        NeonPurpleGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = centerOffset
            )

            // 2. Ondas líquidas de dispersión concéntricas (Fluid Acoustic Ripples)
            val waveCount = 4
            for (w in 1..waveCount) {
                val fraction = ((w.toFloat() / (waveCount + 1)) + rippleOffset * 0.25f) % 1f
                val clampedFraction = fraction.coerceIn(0.50f, 0.98f)
                val rippleRadius = maxRadius * clampedFraction
                val rippleAlpha = when {
                    isListening -> (1f - clampedFraction) * 0.70f
                    isSpeaking -> (1f - clampedFraction) * 0.60f
                    else -> (1f - clampedFraction) * 0.25f
                }

                drawCircle(
                    color = (if (w % 2 == 0) NeonPurple else NeonViolet).copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = centerOffset,
                    style = Stroke(width = if (w % 2 == 0) 1.5f else 2.2f)
                )
            }

            // 3. Capa 1 de Fluido Viscoso Exterior (Outer Fluid Wave Membrane)
            val outerBlobPath = createLiquidBlobPath(
                center = centerOffset,
                baseRadius = baseCoreRadius * 1.18f,
                harmonics = listOf(
                    Harmonic(frequency = 3, amplitude = baseCoreRadius * 0.08f, phase = phase1),
                    Harmonic(frequency = 2, amplitude = baseCoreRadius * 0.06f, phase = -phase2),
                    Harmonic(frequency = 4, amplitude = baseCoreRadius * 0.04f, phase = phase3)
                )
            )
            drawPath(
                path = outerBlobPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = 0.40f),
                        NeonPurple.copy(alpha = 0.25f),
                        Color(0xFF3B0764).copy(alpha = 0.05f)
                    ),
                    center = centerOffset,
                    radius = baseCoreRadius * 1.25f
                )
            )
            drawPath(
                path = outerBlobPath,
                color = NeonPurple.copy(alpha = if (isListening) 0.65f else 0.35f),
                style = Stroke(width = 1.2f)
            )

            // 4. Capa 2 de Fluido Viscoso Intermedio (Counter-flowing Liquid Membrane)
            val midBlobPath = createLiquidBlobPath(
                center = centerOffset,
                baseRadius = baseCoreRadius * 1.05f,
                harmonics = listOf(
                    Harmonic(frequency = 2, amplitude = baseCoreRadius * 0.07f, phase = phase2),
                    Harmonic(frequency = 5, amplitude = baseCoreRadius * 0.05f, phase = phase1 * 1.2f),
                    Harmonic(frequency = 3, amplitude = baseCoreRadius * 0.04f, phase = -phase3)
                )
            )
            drawPath(
                path = midBlobPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.60f),
                        NeonViolet.copy(alpha = 0.45f),
                        Color(0xFF1E0A3C).copy(alpha = 0.15f)
                    ),
                    center = centerOffset,
                    radius = baseCoreRadius * 1.10f
                )
            )

            // 5. Núcleo de Líquido Volumétrico Vivo (Core Living Plasma Liquid Blob)
            val coreBlobPath = createLiquidBlobPath(
                center = centerOffset,
                baseRadius = baseCoreRadius,
                harmonics = listOf(
                    Harmonic(frequency = 3, amplitude = baseCoreRadius * 0.06f, phase = phase1),
                    Harmonic(frequency = 4, amplitude = baseCoreRadius * 0.04f, phase = phase2),
                    Harmonic(frequency = 2, amplitude = baseCoreRadius * 0.05f, phase = phase3)
                )
            )

            // Gradiente volumétrico líquido profundo
            val liquidCoreColors = when {
                isListening -> listOf(
                    NeonLilac,
                    NeonPurple,
                    NeonViolet,
                    Color(0xFF4C1D95),
                    Color(0xFF1E0A3C)
                )
                isSpeaking -> listOf(
                    Color(0xFFF0ABFC),
                    NeonMagenta,
                    NeonPurple,
                    NeonViolet,
                    Color(0xFF1E0A3C)
                )
                isProcessing -> listOf(
                    NeonLilac,
                    NeonViolet,
                    Color(0xFF9333EA),
                    Color(0xFF3B0764),
                    Color(0xFF0F041E)
                )
                else -> listOf(
                    NeonLilac.copy(alpha = 0.90f),
                    NeonPurple,
                    NeonViolet,
                    Color(0xFF3B0764),
                    Color(0xFF110324)
                )
            }

            drawPath(
                path = coreBlobPath,
                brush = Brush.radialGradient(
                    colors = liquidCoreColors,
                    center = Offset(centerOffset.x - baseCoreRadius * 0.25f, centerOffset.y - baseCoreRadius * 0.25f),
                    radius = baseCoreRadius * 1.05f
                ),
                style = Fill
            )

            // Borde brillante de tensión superficial del líquido
            drawPath(
                path = coreBlobPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonLilac,
                        NeonPurple,
                        NeonViolet.copy(alpha = 0.6f),
                        NeonMagenta.copy(alpha = 0.8f)
                    ),
                    start = Offset(centerOffset.x - baseCoreRadius, centerOffset.y - baseCoreRadius),
                    end = Offset(centerOffset.x + baseCoreRadius, centerOffset.y + baseCoreRadius)
                ),
                style = Stroke(width = if (isListening) 2.2f else 1.6f)
            )

            // 6. Brillo Especular de Superficie Líquida 3D (Liquid Surface Sheen & Caustics)
            val highlightRadius = baseCoreRadius * 0.38f
            val highlightCenter = Offset(
                centerOffset.x - baseCoreRadius * 0.30f,
                centerOffset.y - baseCoreRadius * 0.30f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isListening) 0.65f else 0.45f),
                        NeonLilac.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = highlightRadius
                ),
                radius = highlightRadius,
                center = highlightCenter
            )

            // 7. Gotas Líquidas y Bioluminiscencia en Suspensión (Floating Fluid Droplets)
            val dropletCount = if (isListening || isProcessing) 14 else 9
            for (d in 0 until dropletCount) {
                val dropletAngle = (phase1 * 40f + (d * (360f / dropletCount))) % 360f
                val dropletRad = Math.toRadians(dropletAngle.toDouble())
                val distFactor = 0.58f + 0.32f * sin(phase2 + d * 1.3f)
                val dropletDistance = baseCoreRadius * distFactor

                val dx = centerOffset.x + (dropletDistance * cos(dropletRad)).toFloat()
                val dy = centerOffset.y + (dropletDistance * sin(dropletRad)).toFloat()

                val dropletColor = when (d % 3) {
                    0 -> NeonLilac
                    1 -> NeonMagenta
                    else -> Color.White
                }

                drawCircle(
                    color = dropletColor.copy(alpha = 0.40f + 0.45f * sin(phase3 + d.toFloat())),
                    radius = if (d % 2 == 0) 2.4f else 1.6f,
                    center = Offset(dx, dy)
                )
            }
        }
    }
}

/**
 * Representa un componente armónico para la deformación de la superficie líquida.
 */
private data class Harmonic(
    val frequency: Int,
    val amplitude: Float,
    val phase: Float
)

/**
 * Genera un camino orgánico cerrado (Path) simulando una gota de líquido deformada mediante ondas armónicas.
 */
private fun createLiquidBlobPath(
    center: Offset,
    baseRadius: Float,
    harmonics: List<Harmonic>,
    steps: Int = 48
): Path {
    val path = Path()
    val points = ArrayList<Offset>(steps)

    for (i in 0 until steps) {
        val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
        var radius = baseRadius
        for (h in harmonics) {
            radius += h.amplitude * sin(h.frequency * theta + h.phase)
        }
        val px = center.x + radius * cos(theta)
        val py = center.y + radius * sin(theta)
        points.add(Offset(px, py))
    }

    if (points.isNotEmpty()) {
        path.moveTo((points[0].x + points[steps - 1].x) / 2f, (points[0].y + points[steps - 1].y) / 2f)

        for (i in 0 until steps) {
            val curr = points[i]
            val next = points[(i + 1) % steps]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            path.quadraticTo(curr.x, curr.y, midX, midY)
        }
        path.close()
    }

    return path
}
