import ctypes
from ctypes import wintypes
import logging
import psutil
from typing import List, Dict, Any, Optional

logger = logging.getLogger("hendrix_desktop.window_manager")

DWMWA_CLOAKED = 14
WS_EX_TOOLWINDOW = 0x00000080
WS_EX_APPWINDOW = 0x00040000
GW_OWNER = 4
GWL_EXSTYLE = -20
GWL_STYLE = -16
WS_CHILD = 0x40000000
SW_RESTORE = 9
SW_SHOW = 5
SW_MINIMIZE = 6
WM_CLOSE = 0x0010
VK_MENU = 0x12  # Tecla ALT

class WindowManager:
    """
    Gestor Win32 de ventanas y aplicaciones activas en Windows.
    Permite enumerar las ventanas legítimas de la barra de tareas y conmutar el foco
    a cualquiera de ellas instantáneamente desde el móvil.
    """

    def __init__(self):
        self._user32 = ctypes.windll.user32
        self._dwmapi = ctypes.windll.dwmapi
        self._ensure_input_desktop()

    def _ensure_input_desktop(self):
        """Asegura que el hilo esté conectado al escritorio interactivo activo (Default)."""
        try:
            hdesk = self._user32.OpenInputDesktop(0, False, 0x01FF)
            if hdesk:
                self._user32.SetThreadDesktop(hdesk)
                self._user32.CloseDesktop(hdesk)
        except Exception:
            pass

    def get_taskbar_windows(self) -> List[Dict[str, Any]]:
        """
        Enumera todas las ventanas abiertas visibles en la barra de tareas o Alt+Tab.
        Filtra ventanas invisibles, tooltips, overlays y procesos internos del sistema.
        """
        self._ensure_input_desktop()
        windows: List[Dict[str, Any]] = []
        foreground_hwnd = self._user32.GetForegroundWindow()

        ignored_titles = {
            "program manager", "settings", "configuración", "default ime", "msctfime ui",
            "windows input experience", "cortana", "search", "inicio", "start",
            "battery indicator", "volume control"
        }

        ignored_procs = {
            "shellexperiencehost.exe", "searchhost.exe", "startmenuexperiencehost.exe",
            "textinputhost.exe", "applicationframehost.exe"
        }

        def enum_proc(hwnd, lparam):
            if not self._user32.IsWindowVisible(hwnd):
                return True

            length = self._user32.GetWindowTextLengthW(hwnd)
            if length == 0:
                return True

            # Filtrar si está oculta por DWM (escritorios virtuales, apps minimizadas UWP)
            cloaked = wintypes.DWORD()
            self._dwmapi.DwmGetWindowAttribute(hwnd, DWMWA_CLOAKED, ctypes.byref(cloaked), ctypes.sizeof(cloaked))
            if cloaked.value != 0:
                return True

            style = self._user32.GetWindowLongW(hwnd, GWL_STYLE)
            if style & WS_CHILD:
                return True

            ex_style = self._user32.GetWindowLongW(hwnd, GWL_EXSTYLE)
            if (ex_style & WS_EX_TOOLWINDOW) and not (ex_style & WS_EX_APPWINDOW):
                return True

            owner = self._user32.GetWindow(hwnd, GW_OWNER)
            if owner != 0 and not (ex_style & WS_EX_APPWINDOW):
                return True

            buff = ctypes.create_unicode_buffer(length + 1)
            self._user32.GetWindowTextW(hwnd, buff, length + 1)
            title = buff.value.strip()

            if not title or title.lower() in ignored_titles:
                return True

            pid = wintypes.DWORD()
            self._user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
            proc_name = "unknown"
            try:
                proc_name = psutil.Process(pid.value).name()
            except Exception:
                pass

            if proc_name.lower() in ignored_procs:
                return True

            # Si es explorer.exe, solo permitir ventanas de explorador de archivos reales
            if proc_name.lower() == "explorer.exe":
                class_buf = ctypes.create_unicode_buffer(256)
                self._user32.GetClassNameW(hwnd, class_buf, 256)
                class_name = class_buf.value
                if class_name not in ["CabinetWClass", "ExploreWClass"]:
                    return True

            windows.append({
                "hwnd": int(hwnd),
                "title": title,
                "process": proc_name,
                "pid": int(pid.value),
                "isForeground": (hwnd == foreground_hwnd)
            })
            return True

        WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
        self._user32.EnumWindows(WNDENUMPROC(enum_proc), 0)

        # Ordenar: la ventana activa primero, luego por título alfabético
        windows.sort(key=lambda w: (not w["isForeground"], w["process"].lower(), w["title"].lower()))
        return windows

    def focus_window(self, hwnd: int) -> bool:
        """
        Restaura y trae la ventana al primer plano en Windows con latencia < 1ms.
        Aplica el bypass Win32 con tecla ALT para forzar la transferencia de foco en Windows 10/11.
        """
        self._ensure_input_desktop()
        if not self._user32.IsWindow(hwnd):
            logger.warning(f"Ventana inválida o destruida: HWND={hwnd}")
            return False

        try:
            # 1. Si está minimizada, restaurar con SW_RESTORE
            if self._user32.IsIconic(hwnd):
                self._user32.ShowWindow(hwnd, SW_RESTORE)
            else:
                self._user32.ShowWindow(hwnd, SW_SHOW)

            # 2. Bypass de transferencia de foco en Windows 10 y 11:
            # Simular presión de la tecla ALT para otorgar permiso de foco al hilo en segundo plano
            self._user32.keybd_event(VK_MENU, 0, 0, 0)
            self._user32.SetForegroundWindow(hwnd)
            self._user32.keybd_event(VK_MENU, 0, 2, 0)  # KEYEVENTF_KEYUP = 2

            self._user32.BringWindowToTop(hwnd)
            logger.info(f"🪟 Ventana enfocada con éxito: HWND={hwnd}")
            return True
        except Exception as e:
            logger.error(f"Error enfocando ventana HWND={hwnd}: {e}")
            return False

    def minimize_window(self, hwnd: int) -> bool:
        """Minimiza la ventana especificada."""
        self._ensure_input_desktop()
        if not self._user32.IsWindow(hwnd):
            return False
        return bool(self._user32.ShowWindow(hwnd, SW_MINIMIZE))

    def close_window(self, hwnd: int) -> bool:
        """Envía solicitud estándar WM_CLOSE para cerrar la ventana limpiamente."""
        self._ensure_input_desktop()
        if not self._user32.IsWindow(hwnd):
            return False
        return bool(self._user32.PostMessageW(hwnd, WM_CLOSE, 0, 0))

window_manager = WindowManager()
