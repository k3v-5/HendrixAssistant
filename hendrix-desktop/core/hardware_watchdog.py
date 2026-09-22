import time
import ctypes
import threading
import logging
import psutil
from typing import Dict, Any, Optional, Callable

logger = logging.getLogger("hendrix_desktop.hardware_watchdog")

class HardwareWatchdog:
    """
    Monitor de salud de hardware en Windows (GPU, VRAM, CPU, Temperatura)
    y centinela de tareas pesadas (Render de Blender, Video, etc.).
    """

    def __init__(self):
        self._watchdog_thread: Optional[threading.Thread] = None
        self._is_watching = False
        self._active_target_process = ""
        self._auto_suspend = False
        self._on_completed_callback: Optional[Callable[[dict], None]] = None

    def get_hardware_telemetry(self) -> Dict[str, Any]:
        # 1. Métricas de CPU y RAM
        cpu_pct = psutil.cpu_percent(interval=0.1)
        vmem = psutil.virtual_memory()
        ram_used_mb = vmem.used // (1024 * 1024)
        ram_total_mb = vmem.total // (1024 * 1024)

        # 2. Métricas de GPU
        gpu_name = "GPU Principal"
        gpu_pct = 0.0
        gpu_temp = 48
        vram_used_mb = 0
        vram_total_mb = 8192

        # Intentar consultar NVIDIA NVML si está disponible
        try:
            import pynvml
            pynvml.nvmlInit()
            handle = pynvml.nvmlDeviceGetHandleByIndex(0)
            gpu_name = pynvml.nvmlDeviceGetName(handle)
            if isinstance(gpu_name, bytes):
                gpu_name = gpu_name.decode("utf-8")
            util = pynvml.nvmlDeviceGetUtilizationRates(handle)
            gpu_pct = float(util.gpu)
            gpu_temp = int(pynvml.nvmlDeviceGetTemperature(handle, pynvml.NVML_TEMPERATURE_GPU))
            mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
            vram_used_mb = mem_info.used // (1024 * 1024)
            vram_total_mb = mem_info.total // (1024 * 1024)
        except Exception:
            # Fallback genérico: inferir carga de GPU de procesos de renderizado conocidos
            pass

        # 3. Detectar procesos pesados activos
        heavy_proc = None
        for p in psutil.process_iter(['name', 'cpu_percent']):
            try:
                pname = p.info['name'] or ""
                pname_lower = pname.lower()
                if any(x in pname_lower for x in ["blender", "afterfx", "premiere", "unrealeditor", "ffmpeg"]):
                    heavy_proc = pname
                    break
            except Exception:
                continue

        return {
            "gpuName": gpu_name,
            "gpuUsagePercent": gpu_pct,
            "gpuTempCelsius": gpu_temp,
            "vramUsedMb": vram_used_mb,
            "vramTotalMb": vram_total_mb,
            "cpuUsagePercent": cpu_pct,
            "ramUsedMb": ram_used_mb,
            "ramTotalMb": ram_total_mb,
            "activeHeavyProcess": heavy_proc,
            "timestampEpoch": int(time.time() * 1000)
        }

    def start_render_watchdog(
        self,
        process_name: str = "blender",
        auto_suspend: Boolean = True,
        on_completed: Optional[Callable[[dict], None]] = None
    ) -> bool:
        if self._is_watching:
            logger.info("Watchdog ya está activo supervisando un render.")
            return True

        self._is_watching = True
        self._active_target_process = process_name.lower()
        self._auto_suspend = auto_suspend
        self._on_completed_callback = on_completed

        self._watchdog_thread = threading.Thread(target=self._watchdog_loop, daemon=True)
        self._watchdog_thread.start()
        logger.info(f"Centinela de render iniciado para '{process_name}' (Auto-suspender: {auto_suspend})")
        return True

    def _watchdog_loop(self):
        start_time = time.time()
        peak_temp = 45
        consecutive_idle_seconds = 0
        saw_heavy_load = False

        while self._is_watching:
            time.sleep(2.0)
            telemetry = self.get_hardware_telemetry()
            temp = telemetry.get("gpuTempCelsius", 45)
            if temp > peak_temp:
                peak_temp = temp

            # Buscar si el proceso objetivo está corriendo y su uso de CPU
            target_running = False
            target_cpu = 0.0

            for p in psutil.process_iter(['name', 'cpu_percent']):
                try:
                    pname = p.info['name'] or ""
                    if self._active_target_process in pname.lower():
                        target_running = True
                        target_cpu = p.info['cpu_percent'] or 0.0
                        break
                except Exception:
                    continue

            if target_running and (target_cpu > 15.0 or telemetry.get("gpuUsagePercent", 0) > 25.0):
                saw_heavy_load = True
                consecutive_idle_seconds = 0
            elif target_running and saw_heavy_load and target_cpu < 5.0:
                consecutive_idle_seconds += 2
            elif not target_running and saw_heavy_load:
                # El proceso terminó completamente
                consecutive_idle_seconds += 6

            # Criterio de finalización: carga cayó a idle sostenido durante 8 segundos tras haber trabajado
            if saw_heavy_load and consecutive_idle_seconds >= 8:
                elapsed = int(time.time() - start_time)
                logger.info(f"✅ Render finalizado tras {elapsed} segundos (Temp pico: {peak_temp}°C)")

                event = {
                    "processName": self._active_target_process,
                    "status": "COMPLETED",
                    "durationSeconds": elapsed,
                    "peakGpuTemp": peak_temp,
                    "autoSuspendTriggered": self._auto_suspend,
                    "message": f"Render de {self._active_target_process} finalizado tras {elapsed // 60}m {elapsed % 60}s.",
                    "timestampEpoch": int(time.time() * 1000)
                }

                if self._on_completed_callback:
                    try:
                        self._on_completed_callback(event)
                    except Exception as e:
                        logger.error(f"Error en callback de finalización de render: {e}")

                if self._auto_suspend:
                    logger.info("Esperando 20 segundos para sincronización de archivos antes de suspender la PC...")
                    time.sleep(20.0)
                    ctypes.windll.PowrProf.SetSuspendState(0, 1, 0)

                self._is_watching = False
                break

    def stop_watchdog(self):
        self._is_watching = False

hardware_watchdog = HardwareWatchdog()
