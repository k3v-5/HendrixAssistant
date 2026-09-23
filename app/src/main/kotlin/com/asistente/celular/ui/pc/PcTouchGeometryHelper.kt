package com.asistente.celular.ui.pc

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Resultado del ajuste y cálculo geométrico de la pantalla remota.
 */
data class FitResult(
    val fitW: Float,
    val fitH: Float,
    val scaledW: Float,
    val scaledH: Float,
    val imageScreenLeft: Float,
    val imageScreenTop: Float
)

/**
 * Motor matemático de transformaciones geométricas para la pantalla de PC remota:
 * - Soporta relaciones de aspecto estándar (16:9, 16:10) y ultra-anchas (21:9, 32:9).
 * - Conversión bidireccional exacta de coordenadas de pantalla móvil a ratios de PC [0f..1f].
 * - Algoritmo de límites de paneo (clamping) dinámico en X e Y para evitar congelamiento vertical.
 * - Zoom por centroide con preservación del punto focal bajo los dedos.
 */
object PcTouchGeometryHelper {

    /**
     * Calcula las dimensiones escaladas y la posición de inicio del fotograma en la pantalla.
     */
    fun calculateFitDimensions(
        containerW: Float,
        containerH: Float,
        bmpW: Float,
        bmpH: Float,
        scale: Float,
        offset: Offset
    ): FitResult {
        val cW = containerW.coerceAtLeast(1f)
        val cH = containerH.coerceAtLeast(1f)
        val bW = bmpW.coerceAtLeast(1f)
        val bH = bmpH.coerceAtLeast(1f)

        val bmpAspect = bW / bH
        val containerAspect = cW / cH

        val (fitW, fitH) = if (containerAspect > bmpAspect) {
            Pair(cH * bmpAspect, cH)
        } else {
            Pair(cW, cW / bmpAspect)
        }

        val scaledW = fitW * scale
        val scaledH = fitH * scale

        val imageScreenLeft = (cW / 2f + offset.x) - (scaledW / 2f)
        val imageScreenTop = (cH / 2f + offset.y) - (scaledH / 2f)

        return FitResult(
            fitW = fitW,
            fitH = fitH,
            scaledW = scaledW,
            scaledH = scaledH,
            imageScreenLeft = imageScreenLeft,
            imageScreenTop = imageScreenTop
        )
    }

    /**
     * Convierte una coordenada de pantalla móvil (en píxeles) a ratios relativos de la PC [0f..1f].
     */
    fun screenToPcRatio(
        touchX: Float,
        touchY: Float,
        imageScreenLeft: Float,
        imageScreenTop: Float,
        scaledW: Float,
        scaledH: Float
    ): Pair<Float, Float> {
        val safeW = scaledW.coerceAtLeast(1f)
        val safeH = scaledH.coerceAtLeast(1f)
        val normX = ((touchX - imageScreenLeft) / safeW).coerceIn(0f, 1f)
        val normY = ((touchY - imageScreenTop) / safeH).coerceIn(0f, 1f)
        return Pair(normX, normY)
    }

    /**
     * Convierte ratios relativos de la PC [0f..1f] a coordenadas de píxeles en la pantalla móvil.
     */
    fun pcRatioToScreen(
        ratioX: Float,
        ratioY: Float,
        imageScreenLeft: Float,
        imageScreenTop: Float,
        scaledW: Float,
        scaledH: Float
    ): Pair<Float, Float> {
        val screenX = imageScreenLeft + (ratioX.coerceIn(0f, 1f) * scaledW)
        val screenY = imageScreenTop + (ratioY.coerceIn(0f, 1f) * scaledH)
        return Pair(screenX, screenY)
    }

    /**
     * Limita el desplazamiento (offset) del viewport para que la imagen permanezca accesible y visible.
     * Permite paneo completo en X e Y tanto cuando sH > cH como cuando sH <= cH (monitores ultrawide en modo vertical).
     */
    fun clampOffset(
        rawX: Float,
        rawY: Float,
        fitW: Float,
        fitH: Float,
        cW: Float,
        cH: Float,
        scale: Float
    ): Pair<Float, Float> {
        if (scale <= 1.02f) {
            return Pair(0f, 0f)
        }
        val sW = fitW * scale
        val sH = fitH * scale

        // En X: si la imagen escalada excede el ancho del contenedor o si está ampliada
        val mX = abs(sW - cW) / 2f
        // En Y: permite navegar verticalmente entre los límites del contenedor sin congelar en 0
        val mY = abs(sH - cH) / 2f

        return Pair(
            rawX.coerceIn(-mX, mX),
            rawY.coerceIn(-mY, mY)
        )
    }

    /**
     * Calcula la distancia euclidiana entre dos puntos de contacto táctil.
     */
    fun calculateDistance(p0: Offset, p1: Offset): Float {
        val dx = p0.x - p1.x
        val dy = p0.y - p1.y
        return sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    }

    /**
     * Calcula el centroide (punto medio) entre dos puntos de contacto táctil.
     */
    fun calculateCentroid(p0: Offset, p1: Offset): Offset {
        return Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
    }
}
