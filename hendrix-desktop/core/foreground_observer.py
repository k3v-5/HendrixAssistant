import time
import threading
import logging
from typing import Optional, Callable, Dict, Any, List

logger = logging.getLogger("hendrix_desktop.foreground_observer")

class ForegroundObserver:
    """
    Observador de la ventana y proceso en primer plano activo en Windows.
    Detecta de inmediato cuándo el usuario cambia de aplicación (Blender, Ableton, Premiere, VS Code, etc.)
    y emite eventos para que el móvil conmute automáticamente el perfil del Macro Deck.
    """

    def __init__(self, check_interval_sec: float = 0.25):
        self.check_interval_sec = check_interval_sec
        self._callbacks: List[Callable[[Dict[str, Any]], None]] = []
        self._running = False
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None

        self._current_window_info: Dict[str, Any] = {
            "title": "Windows Desktop",
            "process": "explorer.exe",
            "pid": 0
        }

    def register_callback(self, callback: Callable[[Dict[str, Any]], None]):
        if callback not in self._callbacks:
            self._callbacks.append(callback)

    def unregister_callback(self, callback: Callable[[Dict[str, Any]], None]):
        if callback in self._callbacks:
            self._callbacks.remove(callback)

    def get_current_foreground_info(self) -> Dict[str, Any]:
        return dict(self._current_window_info)

    def start(self):
        if self._running:
            return
        self._running = True
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._loop, daemon=True, name="ForegroundObserverThread")
        self._thread.start()
        logger.info("👀 Observador de ventana en primer plano iniciado")

    def stop(self):
        self._running = False
        self._stop_event.set()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=1.0)
        logger.info("👀 Observador de ventana en primer plano detenido")

    def _loop(self):
        while not self._stop_event.is_set():
            try:
                info = self._query_active_window()
                if info and (info["process"].lower() != self._current_window_info["process"].lower() or
                            info["title"] != self._current_window_info["title"]):
                    self._current_window_info = info
                    self._dispatch_change(info)
            except Exception as e:
                logger.debug(f"Error consultando ventana activa: {e}")

            if self._stop_event.wait(timeout=self.check_interval_sec):
                break

    def _query_active_window(self) -> Optional[Dict[str, Any]]:
        """Obtiene el título y proceso de la ventana activa mediante la API de Windows."""
        try:
            import ctypes
            from ctypes import wintypes
            import psutil

            hwnd = ctypes.windll.user32.GetForegroundWindow()
            if not hwnd:
                return None

            length = ctypes.windll.user32.GetWindowTextLengthW(hwnd)
            buff = ctypes.create_unicode_buffer(length + 1)
            ctypes.windll.user32.GetWindowTextW(hwnd, buff, length + 1)
            title = buff.value.strip() or "Sin Título"

            pid = wintypes.DWORD()
            ctypes.windll.user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
            proc_name = "explorer.exe"
            try:
                proc_name = psutil.Process(pid.value).name()
            except Exception:
                pass

            return {
                "title": title,
                "process": proc_name,
                "pid": pid.value
            }
        except Exception:
            return None

    def _dispatch_change(self, info: Dict[str, Any]):
        payload = {
            "type": "FOREGROUND_APP_CHANGED",
            "app": info,
            "timestampEpoch": int(time.time() * 1000)
        }
        for cb in list(self._callbacks):
            try:
                cb(payload)
            except Exception as e:
                logger.debug(f"Error despachando evento de ventana activa: {e}")

foreground_observer = ForegroundObserver()
