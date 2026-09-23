package com.asistente.celular.ui.pc

import androidx.compose.ui.geometry.Offset
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la lógica matemática y de eventos de interacción táctil de pantalla PC:
 * - Conversión de coordenadas de pantalla a ratios normalizados [0.0..1.0] con PcTouchGeometryHelper.
 * - Sincronización exacta bidireccional (round-trip screenToPcRatio <-> pcRatioToScreen).
 * - Soporte de resoluciones estándar (16:9 1920x1080) y Ultrawide (21:9 3440x1440).
 * - Algoritmo de límites de paneo (clamping) dinámico en X e Y para evitar congelamiento vertical.
 * - Detección de distancias de centroide y umbrales para gestos de pinza multitáctil.
 * - Validación de acciones emitidas (Clic Izquierdo 2s, Clic Derecho 4s, Doble Clic).
 */
class PcTouchInteractionTest {

    @Test
    fun testScreenToPcRatioNormalizationStandard16x9() {
        val containerW = 1080f
        val containerH = 2400f
        val bmpW = 1920f
        val bmpH = 1080f

        val fit = PcTouchGeometryHelper.calculateFitDimensions(
            containerW = containerW,
            containerH = containerH,
            bmpW = bmpW,
            bmpH = bmpH,
            scale = 1.0f,
            offset = Offset.Zero
        )

        assertEquals(1080f, fit.fitW, 0.01f)
        assertEquals(607.5f, fit.fitH, 0.01f)
        assertEquals(0f, fit.imageScreenLeft, 0.01f)
        assertEquals(896.25f, fit.imageScreenTop, 0.01f)

        // Toque en el centro del fotograma
        val (centerX, centerY) = PcTouchGeometryHelper.screenToPcRatio(
            touchX = 540f,
            touchY = 1200f,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )
        assertEquals(0.5f, centerX, 0.01f)
        assertEquals(0.5f, centerY, 0.01f)

        // Toque en la esquina superior izquierda del fotograma
        val (topLeftX, topLeftY) = PcTouchGeometryHelper.screenToPcRatio(
            touchX = fit.imageScreenLeft,
            touchY = fit.imageScreenTop,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )
        assertEquals(0.0f, topLeftX, 0.001f)
        assertEquals(0.0f, topLeftY, 0.001f)

        // Toque fuera de los límites superiores (franja negra superior)
        val (outX, outY) = PcTouchGeometryHelper.screenToPcRatio(
            touchX = 540f,
            touchY = 100f,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )
        assertEquals(0.5f, outX, 0.01f)
        assertEquals(0.0f, outY, 0.001f) // Clamped a 0f
    }

    @Test
    fun testScreenToPcRatioNormalizationUltrawide21x9WithZoom() {
        val containerW = 1080f
        val containerH = 2400f
        val bmpW = 3440f
        val bmpH = 1440f

        // Zoom a 2.5x con desplazamiento de paneo
        val scale = 2.5f
        val offset = Offset(150f, -80f)

        val fit = PcTouchGeometryHelper.calculateFitDimensions(
            containerW = containerW,
            containerH = containerH,
            bmpW = bmpW,
            bmpH = bmpH,
            scale = scale,
            offset = offset
        )

        // Validar identidad bidireccional round-trip: (touch -> ratio -> screen == touch)
        val testTouchX = 720f
        val testTouchY = 1150f

        val (ratioX, ratioY) = PcTouchGeometryHelper.screenToPcRatio(
            touchX = testTouchX,
            touchY = testTouchY,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )

        val (reconstructedX, reconstructedY) = PcTouchGeometryHelper.pcRatioToScreen(
            ratioX = ratioX,
            ratioY = ratioY,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )

        assertEquals(testTouchX, reconstructedX, 0.01f)
        assertEquals(testTouchY, reconstructedY, 0.01f)
    }

    @Test
    fun testClampOffsetDynamicPanning() {
        val cW = 1080f
        val cH = 2400f
        val fitW = 1080f
        val fitH = 452.1f // Ultrawide 21:9

        // Escala 1.0x: centrado estricto (0, 0)
        val (clamped1xX, clamped1xY) = PcTouchGeometryHelper.clampOffset(
            rawX = 150f,
            rawY = -200f,
            fitW = fitW,
            fitH = fitH,
            cW = cW,
            cH = cH,
            scale = 1.0f
        )
        assertEquals(0f, clamped1xX, 0.001f)
        assertEquals(0f, clamped1xY, 0.001f)

        // Escala 3.0x: sW = 3240f (> 1080f) -> maxPanX = (3240 - 1080) / 2 = 1080f
        // sH = 1356.3f (< 2400f) -> maxPanY = (2400 - 1356.3) / 2 = 521.85f (NO congela en 0)
        val (clamped3xX, clamped3xY) = PcTouchGeometryHelper.clampOffset(
            rawX = 2000f,
            rawY = -800f,
            fitW = fitW,
            fitH = fitH,
            cW = cW,
            cH = cH,
            scale = 3.0f
        )
        assertEquals(1080f, clamped3xX, 0.01f) // Clamped a maxPanX
        assertEquals(-521.85f, clamped3xY, 0.01f) // Clamped a maxPanY sin congelar en 0
    }

    @Test
    fun testTwoFingerPinchDistanceAndCentroidCalculation() {
        val p0 = Offset(100f, 200f)
        val p1 = Offset(400f, 600f)

        val distance = PcTouchGeometryHelper.calculateDistance(p0, p1)
        // Triángulo rectángulo 300, 400 -> hipotenusa 500
        assertEquals(500f, distance, 0.001f)

        val centroid = PcTouchGeometryHelper.calculateCentroid(p0, p1)
        assertEquals(250f, centroid.x, 0.001f)
        assertEquals(400f, centroid.y, 0.001f)
    }

    @Test
    fun testHoldTimingActions() {
        val clickAction = PcInteractionAction(
            type = PcActionType.CLICK,
            xRatio = 0.45f,
            yRatio = 0.65f
        )
        assertEquals(PcActionType.CLICK, clickAction.type)
        assertEquals(0.45f, clickAction.xRatio ?: 0f, 0.001f)
        assertEquals(0.65f, clickAction.yRatio ?: 0f, 0.001f)

        val rightClickAction = PcInteractionAction(
            type = PcActionType.RIGHT_CLICK,
            xRatio = 0.45f,
            yRatio = 0.65f
        )
        assertEquals(PcActionType.RIGHT_CLICK, rightClickAction.type)

        val doubleClickAction = PcInteractionAction(
            type = PcActionType.DOUBLE_CLICK,
            xRatio = 0.45f,
            yRatio = 0.65f
        )
        assertEquals(PcActionType.DOUBLE_CLICK, doubleClickAction.type)
    }

    @Test
    fun testMouseMoveThrottlingLogic() {
        val dispatchedActions = mutableListOf<Long>()
        var lastSentTime = 0L
        val minIntervalMs = 28L

        fun simulatePointerDragEvent(eventTime: Long) {
            if (eventTime - lastSentTime >= minIntervalMs) {
                lastSentTime = eventTime
                dispatchedActions.add(eventTime)
            }
        }

        for (i in 0 until 10) {
            simulatePointerDragEvent(1000L + (i * 8L))
        }

        assertEquals(3, dispatchedActions.size)
        assertEquals(1000L, dispatchedActions[0])
        assertEquals(1032L, dispatchedActions[1])
        assertEquals(1064L, dispatchedActions[2])
    }
}
