package com.asistente.celular.nlu.tasks

/**
 * Contrato para programación y cancelación de recordatorios de tareas.
 * Desacopla la lógica de temporizadores/alarmas de la plataforma subyacente.
 */
interface TaskScheduler {
    fun scheduleReminder(task: TaskItem)
    fun cancelReminder(taskId: String)
}
