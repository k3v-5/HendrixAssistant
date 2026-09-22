import os
import sys
import time
import ctypes
import winreg
import subprocess
import urllib.parse
from typing import Dict, Any, Optional, Tuple, List

try:
    import psutil
except ImportError:
    psutil = None

import pyautogui

class BrowserManager:
    """
    Controlador de navegadores web para Windows (Google Chrome, Brave, Opera, Edge).
    Permite lanzar instancias, enfocar ventanas, ejecutar atajos de pestañas
    y abrir búsquedas o URLs específicas en plataformas como Google, YouTube, Facebook y GitHub.
    """

    SUPPORTED_BROWSERS = {
        "chrome": {
            "name": "Google Chrome",
            "exe_names": ["chrome.exe"],
            "process_names": ["chrome.exe"],
            "title_keywords": ["google chrome", "chrome"],
            "fallback_dirs": [
                os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Google\\Chrome\\Application\\chrome.exe"),
                os.path.join(os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"), "Google\\Chrome\\Application\\chrome.exe"),
                os.path.join(os.environ.get("LOCALAPPDATA", ""), "Google\\Chrome\\Application\\chrome.exe"),
            ]
        },
        "brave": {
            "name": "Brave Browser",
            "exe_names": ["brave.exe"],
            "process_names": ["brave.exe"],
            "title_keywords": ["brave"],
            "fallback_dirs": [
                os.path.join(os.environ.get("LOCALAPPDATA", ""), "BraveSoftware\\Brave-Browser\\Application\\brave.exe"),
                os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "BraveSoftware\\Brave-Browser\\Application\\brave.exe"),
                os.path.join(os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"), "BraveSoftware\\Brave-Browser\\Application\\brave.exe"),
            ]
        },
        "opera": {
            "name": "Opera",
            "exe_names": ["launcher.exe", "opera.exe"],
            "process_names": ["opera.exe", "launcher.exe"],
            "title_keywords": ["opera"],
            "fallback_dirs": [
                os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs\\Opera\\launcher.exe"),
                os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Opera\\launcher.exe"),
                os.path.join(os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"), "Opera\\launcher.exe"),
            ]
        },
        "edge": {
            "name": "Microsoft Edge",
            "exe_names": ["msedge.exe"],
            "process_names": ["msedge.exe"],
            "title_keywords": ["microsoft edge", "edge"],
            "fallback_dirs": [
                os.path.join(os.environ.get("ProgramFiles(x86)", "C:\\Program Files (x86)"), "Microsoft\\Edge\\Application\\msedge.exe"),
                os.path.join(os.environ.get("ProgramFiles", "C:\\Program Files"), "Microsoft\\Edge\\Application\\msedge.exe"),
            ]
        }
    }

    def __init__(self):
        self._cached_paths: Dict[str, str] = {}

    def _query_registry_app_path(self, exe_name: str) -> Optional[str]:
        """Consulta el registro de Windows para obtener la ruta registrada del ejecutable."""
        sub_key = f"Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\{exe_name}"
        for root in (winreg.HKEY_CURRENT_USER, winreg.HKEY_LOCAL_MACHINE):
            try:
                with winreg.OpenKey(root, sub_key) as key:
                    path, _ = winreg.QueryValueEx(key, "")
                    if path and os.path.exists(path):
                        return path
            except Exception:
                continue
        return None

    def resolve_browser_path(self, browser_id: str) -> Tuple[Optional[str], str, bool]:
        """
        Resuelve la ruta del ejecutable del navegador.
        Retorna: (executable_path, browser_display_name, is_fallback)
        """
        target_id = (browser_id or "chrome").lower().strip()
        if target_id not in self.SUPPORTED_BROWSERS:
            # Si se pasa "default" o nombre no estándar, intentar mapear
            if "brave" in target_id:
                target_id = "brave"
            elif "opera" in target_id:
                target_id = "opera"
            elif "edge" in target_id:
                target_id = "edge"
            else:
                target_id = "chrome"

        # 1. Intentar el navegador solicitado
        path = self._find_path_for_id(target_id)
        if path:
            return path, self.SUPPORTED_BROWSERS[target_id]["name"], False

        # 2. Si no existe, degradar a Chrome -> Edge
        for fallback_id in ["chrome", "edge"]:
            if fallback_id != target_id:
                fb_path = self._find_path_for_id(fallback_id)
                if fb_path:
                    return fb_path, self.SUPPORTED_BROWSERS[fallback_id]["name"], True

        return None, self.SUPPORTED_BROWSERS[target_id]["name"], False

    def _find_path_for_id(self, browser_id: str) -> Optional[str]:
        if browser_id in self._cached_paths and os.path.exists(self._cached_paths[browser_id]):
            return self._cached_paths[browser_id]

        info = self.SUPPORTED_BROWSERS.get(browser_id)
        if not info:
            return None

        # Probar en App Paths de Windows
        for exe in info["exe_names"]:
            reg_path = self._query_registry_app_path(exe)
            if reg_path:
                self._cached_paths[browser_id] = reg_path
                return reg_path

        # Probar rutas típicas en disco
        for candidate in info["fallback_dirs"]:
            if os.path.exists(candidate):
                self._cached_paths[browser_id] = candidate
                return candidate

        return None

    def build_search_url(self, platform: Optional[str], query: Optional[str], url: Optional[str] = None) -> str:
        """
        Construye la URL destino basada en la plataforma y la consulta.
        """
        if url and (url.startswith("http://") or url.startswith("https://")):
            return url

        q = (query or "").strip()
        encoded_query = urllib.parse.quote_plus(q)
        plat = (platform or "").lower().strip()

        if plat == "youtube":
            if q:
                return f"https://www.youtube.com/results?search_query={encoded_query}"
            return "https://www.youtube.com"

        if plat == "facebook":
            if q:
                return f"https://www.facebook.com/search/top?q={encoded_query}"
            return "https://www.facebook.com"

        if plat == "github":
            if q:
                return f"https://github.com/search?q={encoded_query}"
            return "https://github.com"

        if plat == "duckduckgo":
            if q:
                return f"https://duckduckgo.com/?q={encoded_query}"
            return "https://duckduckgo.com"

        # Por defecto Google o URL directa
        if q:
            if q.startswith("http://") or q.startswith("https://"):
                return q
            # Si el query parece un dominio directo (ej. "reddit.com" o "youtube.com")
            if "." in q and " " not in q and not q.startswith("www."):
                return f"https://{q}"
            elif q.startswith("www."):
                return f"https://{q}"
            return f"https://www.google.com/search?q={encoded_query}"

        return "https://www.google.com"

    def find_browser_window(self, browser_id: str) -> Optional[int]:
        """Localiza el HWND de una ventana abierta del navegador."""
        target_id = (browser_id or "chrome").lower().strip()
        info = self.SUPPORTED_BROWSERS.get(target_id)
        if not info:
            info = self.SUPPORTED_BROWSERS.get("chrome")

        user32 = ctypes.windll.user32
        hDesk = user32.OpenInputDesktop(0, False, 0x01FF)
        if hDesk:
            user32.SetThreadDesktop(hDesk)

        found_hwnd = None
        target_pids = set()

        if psutil and info:
            proc_names = [p.lower() for p in info["process_names"]]
            for p in psutil.process_iter(['pid', 'name']):
                try:
                    pname = p.info['name'].lower()
                    if any(target in pname for target in proc_names):
                        target_pids.add(p.info['pid'])
                except Exception:
                    pass

        keywords = [k.lower() for k in info["title_keywords"]] if info else []

        def enum_proc(hwnd, lParam):
            nonlocal found_hwnd
            if user32.IsWindowVisible(hwnd):
                length = user32.GetWindowTextLengthW(hwnd)
                if length > 0:
                    buff = ctypes.create_unicode_buffer(length + 1)
                    user32.GetWindowTextW(hwnd, buff, length + 1)
                    title = buff.value.lower()
                    pid = ctypes.c_ulong()
                    user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))

                    is_match = False
                    if target_pids and pid.value in target_pids:
                        is_match = True
                    elif any(k in title for k in keywords):
                        is_match = True

                    if is_match and title not in ['default ime', 'msctfime ui', 'dde server window']:
                        found_hwnd = hwnd
                        return False
            return True

        WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
        if hDesk:
            user32.EnumDesktopWindows(hDesk, WNDENUMPROC(enum_proc), 0)
        else:
            user32.EnumWindows(WNDENUMPROC(enum_proc), 0)

        return found_hwnd

    def focus_browser_window(self, browser_id: str) -> bool:
        """Trae al primer plano la ventana del navegador si existe."""
        hwnd = self.find_browser_window(browser_id)
        if hwnd:
            ctypes.windll.user32.ShowWindow(hwnd, 9)  # SW_RESTORE
            ctypes.windll.user32.SetForegroundWindow(hwnd)
            time.sleep(0.15)
            return True
        return False

    def launch_or_navigate(
        self,
        browser_id: str = "chrome",
        platform: Optional[str] = None,
        query: Optional[str] = None,
        url: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Lanza el navegador seleccionado y abre la URL o realiza la búsqueda requerida.
        """
        target_url = self.build_search_url(platform, query, url)
        exe_path, display_name, is_fallback = self.resolve_browser_path(browser_id)

        if not exe_path:
            # Fallback final a os.startfile de la URL (navegador predeterminado de Windows)
            try:
                os.startfile(target_url)
                return {
                    "success": True,
                    "message": f"Abriendo '{target_url}' en el navegador predeterminado de Windows."
                }
            except Exception as e:
                return {
                    "success": False,
                    "message": f"No se encontró un navegador compatible ni predeterminado: {str(e)}"
                }

        try:
            subprocess.Popen([exe_path, target_url])
            # Intentar traer al frente
            time.sleep(0.3)
            self.focus_browser_window(browser_id)

            notice = f"Abierto en {display_name}."
            if is_fallback:
                notice = f"{browser_id.capitalize()} no está instalado; abriendo en {display_name}."

            if query:
                return {
                    "success": True,
                    "message": f"Buscando '{query}' en {platform or 'Google'} ({notice})"
                }
            else:
                return {
                    "success": True,
                    "message": f"Navegando a {target_url} ({notice})"
                }
        except Exception as e:
            return {
                "success": False,
                "message": f"Error al iniciar {display_name}: {str(e)}"
            }

    def execute_tab_action(self, browser_id: str, action: str) -> Dict[str, Any]:
        """Ejecuta atajos de teclado para administración de pestañas."""
        self.focus_browser_window(browser_id)
        act = action.upper()

        if act == "NEW_TAB":
            pyautogui.hotkey("ctrl", "t")
            return {"success": True, "message": "Nueva pestaña abierta"}
        elif act == "CLOSE_TAB":
            pyautogui.hotkey("ctrl", "w")
            return {"success": True, "message": "Pestaña actual cerrada"}
        elif act == "RELOAD_PAGE":
            pyautogui.hotkey("ctrl", "r")
            return {"success": True, "message": "Página recargada"}
        elif act == "REOPEN_TAB":
            pyautogui.hotkey("ctrl", "shift", "t")
            return {"success": True, "message": "Pestaña recientemente cerrada restaurada"}

        return {"success": False, "message": f"Acción de pestaña desconocida: {action}"}

    def execute(self, action_id: str, params: Optional[dict] = None) -> Dict[str, Any]:
        """Despachador central de acciones del módulo de navegadores."""
        p = params or {}
        aid = action_id.upper()
        browser = p.get("browser", "chrome")

        if aid == "OPEN_CHROME":
            return self.launch_or_navigate(browser_id="chrome", url="chrome://newtab")
        elif aid == "OPEN_BRAVE":
            return self.launch_or_navigate(browser_id="brave", url="brave://newtab")
        elif aid == "OPEN_OPERA":
            return self.launch_or_navigate(browser_id="opera", url="about:blank")
        elif aid == "SEARCH_GOOGLE":
            query = p.get("query")
            return self.launch_or_navigate(browser_id=browser, platform="google", query=query)
        elif aid == "SEARCH_YOUTUBE":
            query = p.get("query")
            return self.launch_or_navigate(browser_id=browser, platform="youtube", query=query)
        elif aid == "OPEN_YOUTUBE":
            return self.launch_or_navigate(browser_id=browser, url="https://www.youtube.com")
        elif aid == "OPEN_FACEBOOK":
            return self.launch_or_navigate(browser_id=browser, url="https://www.facebook.com")
        elif aid == "OPEN_GITHUB":
            return self.launch_or_navigate(browser_id=browser, url="https://github.com")
        elif aid in ["NEW_TAB", "CLOSE_TAB", "RELOAD_PAGE", "REOPEN_TAB"]:
            return self.execute_tab_action(browser_id=browser, action=aid)
        elif aid == "NAVIGATE_OR_SEARCH":
            return self.launch_or_navigate(
                browser_id=p.get("browser", "chrome"),
                platform=p.get("platform"),
                query=p.get("query"),
                url=p.get("url")
            )

        return {"success": False, "message": f"Acción web no soportada: {action_id}"}


browser_manager = BrowserManager()
