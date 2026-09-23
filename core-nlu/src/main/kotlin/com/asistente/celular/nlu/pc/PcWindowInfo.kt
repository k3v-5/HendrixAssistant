package com.asistente.celular.nlu.pc

/**
 * Representa una ventana de aplicación abierta visible en la barra de tareas de Windows.
 * Permite al móvil conocer qué programas se están ejecutando y conmutar a cualquiera de ellos.
 *
 * @property hwnd Identificador único de ventana de Windows (HWND)
 * @property title Título visible de la ventana
 * @property process Nombre del ejecutable del proceso (ej. "ableton.exe", "brave.exe")
 * @property pid Identificador de proceso (PID)
 * @property isForeground Indica si esta ventana está actualmente en primer plano
 */
data class PcWindowInfo(
    val hwnd: Long,
    val title: String,
    val process: String,
    val pid: Int = 0,
    val isForeground: Boolean = false
)
