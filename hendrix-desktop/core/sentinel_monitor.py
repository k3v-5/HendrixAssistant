import time
import threading
import logging
from typing import Dict, Any, Optional, Callable, List

logger = logging.getLogger("hendrix_desktop.sentinel_monitor")

class SentinelMonitor:
    """
    Monitor Centinela Proactivo para Hendrix Desktop.
    Supervisa continuamente la salud térmica del hardware, procesos de render y compilación,
    emitiendo alertas proactivas accionables hacia los dispositivos móviles vinculados.
    """

    DEFAULT_TEMP_THRESHOLD = 82  # Grados Celsius
    TEMP_TRIGGER_DURATION = 10   # Segundos consecutivos sobre el umbral
    ALERT_COOLDOWN_SECONDS = 120 # Tiempo mínimo entre alertas de la misma categoría

    def __init__(self, hardware_watchdog=None):
        self.watchdog = hardware_watchdog
        self._alert_callbacks: List[Callable[[Dict[str, Any]], None]] = []
        self._running = False
        self._stop_event = threading.Event()
        self._monitor_thread: Optional[threading.Thread] = None

        self.temp_threshold = self.DEFAULT_TEMP_THRESHOLD
        self._overheat_start_time: Optional[float] = None
        self._last_alert_time: Dict[str, float] = {}

    def register_callback(self, callback: Callable[[Dict[str, Any]], None]):
        """Registra un callback que recibe alertas para difusión hacia el WebSocket/Android."""
        if callback not in self._alert_callbacks:
            self._alert_callbacks.append(callback)

    def start(self):
        """Inicia el bucle de supervisión en segundo plano."""
        if self._running:
            return
        self._running = True
        self._stop_event.clear()
        self._monitor_thread = threading.Thread(target=self._run_loop, daemon=True, name="SentinelMonitorThread")
        self._monitor_thread.start()
        logger.info("🛡️ Centinela Proactivo iniciado")

    def stop(self):
        """Detiene la supervisión en segundo plano."""
        self._running = False
        self._stop_event.set()
        if self._monitor_thread and self._monitor_thread.is_alive():
            self._monitor_thread.join(timeout=1.5)
        logger.info("🛡️ Centinela Proactivo detenido")

    def _run_loop(self):
        while not self._stop_event.is_set():
            try:
                self._check_thermal_state()
            except Exception as e:
                logger.error(f"Error en bucle de supervisión centinela: {e}")
            if self._stop_event.wait(timeout=3.0):
                break

    def _check_thermal_state(self):
        if not self.watchdog:
            return

        telemetry = self.watchdog.get_hardware_telemetry()
        gpu_temp = telemetry.get("gpuTempCelsius", 0)
        gpu_name = telemetry.get("gpuName", "GPU")
        heavy_proc = telemetry.get("activeHeavyProcess")

        now = time.time()

        if gpu_temp >= self.temp_threshold:
            if self._overheat_start_time is None:
                self._overheat_start_time = now
            elif (now - self._overheat_start_time) >= self.TEMP_TRIGGER_DURATION:
                # Verificar cooldown
                last_time = self._last_alert_time.get("GPU_OVERHEAT", 0)
                if (now - last_time) >= self.ALERT_COOLDOWN_SECONDS:
                    self._last_alert_time["GPU_OVERHEAT"] = now
                    proc_info = f" (Proceso: {heavy_proc})" if heavy_proc else ""
                    self.emit_alert(
                        category="GPU_OVERHEAT",
                        title=f"⚠️ Temperatura Crítica en {gpu_name} ({gpu_temp}°C)",
                        message=f"La GPU ha permanecido sobre {self.temp_threshold}°C durante más de {self.TEMP_TRIGGER_DURATION}s{proc_info}.",
                        severity="WARNING",
                        actions=[
                            {"id": "suspend_pc", "label": "Suspender PC", "dangerous": False},
                            {"id": "pause_process", "label": "Pausar Proceso", "dangerous": False},
                            {"id": "view_copilot", "label": "Ver Pantalla", "dangerous": False},
                            {"id": "dismiss", "label": "Descartar", "dangerous": False}
                        ]
                    )
        else:
            self._overheat_start_time = None

    def emit_render_completed_alert(self, event_data: Dict[str, Any]):
        """Emite una alerta proactiva cuando el watchdog de render detecta finalización."""
        process = event_data.get("processName", "Render")
        duration = event_data.get("durationSeconds", 0)
        peak_temp = event_data.get("peakGpuTemp", 0)
        auto_suspend = event_data.get("autoSuspendTriggered", False)

        now = time.time()
        self._last_alert_time["RENDER_COMPLETED"] = now

        msg = f"El render en {process} ha concluido ({duration}s, temp pico: {peak_temp}°C)."
        if auto_suspend:
            msg += " Auto-suspensión de Windows programada."

        self.emit_alert(
            category="RENDER_COMPLETED",
            title=f"🎨 Render Finalizado: {process}",
            message=msg,
            severity="INFO",
            actions=[
                {"id": "suspend_pc", "label": "Suspender Ahora", "dangerous": False},
                {"id": "view_copilot", "label": "Ver Resultado", "dangerous": False},
                {"id": "dismiss", "label": "Listo", "dangerous": False}
            ]
        )

    def emit_alert(
        self,
        category: str,
        title: str,
        message: str,
        severity: str = "INFO",
        actions: Optional[List[Dict[str, Any]]] = None
    ) -> Dict[str, Any]:
        """Construye y despacha una alerta proactiva a todos los suscriptores."""
        alert_payload = {
            "type": "PROACTIVE_ALERT",
            "alertId": f"alert_{int(time.time() * 1000)}",
            "category": category,
            "title": title,
            "message": message,
            "severity": severity,
            "actions": actions or [
                {"id": "dismiss", "label": "Aceptar", "dangerous": False}
            ],
            "timestampEpoch": int(time.time() * 1000)
        }

        logger.info(f"🚨 Alerta Centinela Proactiva emitida: [{category}] {title}")
        for cb in list(self._alert_callbacks):
            try:
                cb(alert_payload)
            except Exception as e:
                logger.error(f"Error invocando callback de alerta: {e}")

        return alert_payload
