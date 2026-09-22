import os
import glob
import time
import ctypes
import subprocess
from typing import List, Dict, Any, Optional
import pyautogui

try:
    import psutil
except ImportError:
    psutil = None

class AbletonManager:
    """
    Gestor de automatización e integración con Ableton Live en Windows.
    Permite lanzar o enfocar el DAW, guardar proyectos, crear o cargar sets,
    exportar audio y controlar el transporte (play, pausa, record, loop).
    """

    DEFAULT_PATHS = [
        r"C:\Program Files\Ableton\Live 12 Suite\Program\Ableton Live 12 Suite.exe",
        r"C:\Program Files\Ableton\Live 11 Suite\Program\Ableton Live 11 Suite.exe",
        r"C:\Program Files\Ableton\Live 10 Suite\Program\Ableton Live 10 Suite.exe",
        r"C:\Program Files\Ableton\Live 12 Standard\Program\Ableton Live 12 Standard.exe",
        r"C:\Program Files\Ableton\Live 11 Standard\Program\Ableton Live 11 Standard.exe",
        r"C:\Program Files\Ableton\Live 12 Intro\Program\Ableton Live 12 Intro.exe",
        r"C:\Program Files\Ableton\Live 11 Intro\Program\Ableton Live 11 Intro.exe",
        os.path.expandvars(r"%PROGRAMDATA%\Ableton\Live 11 Suite\Program\Ableton Live 11 Suite.exe"),
        os.path.expandvars(r"%PROGRAMDATA%\Ableton\Live 12 Suite\Program\Ableton Live 12 Suite.exe"),
    ]

    def __init__(self, custom_exe_path: Optional[str] = None):
        self.custom_exe_path = custom_exe_path

    def get_executable_path(self) -> Optional[str]:
        """Obtiene la ruta del ejecutable de Ableton Live instalado."""
        if self.custom_exe_path and os.path.exists(self.custom_exe_path):
            return self.custom_exe_path

        for p in self.DEFAULT_PATHS:
            if os.path.exists(p):
                return p

        # Búsqueda dinámica en carpetas de Program Files
        candidates = glob.glob(r"C:\Program Files\Ableton\**\Ableton Live*.exe", recursive=True)
        if candidates:
            return candidates[0]

        candidates_x86 = glob.glob(r"C:\Program Files (x86)\Ableton\**\Ableton Live*.exe", recursive=True)
        if candidates_x86:
            return candidates_x86[0]

        return None

    def find_ableton_window(self) -> Optional[int]:
        """Busca el HWND de la ventana principal de Ableton Live."""
        user32 = ctypes.windll.user32
        hDesk = user32.OpenInputDesktop(0, False, 0x01FF)
        if hDesk:
            user32.SetThreadDesktop(hDesk)

        found_hwnd = None
        ableton_pids = set()

        if psutil:
            for p in psutil.process_iter(['pid', 'name']):
                try:
                    pname = p.info['name'].lower()
                    if 'ableton' in pname or 'live' in pname:
                        ableton_pids.add(p.info['pid'])
                except Exception:
                    pass

        def enum_proc(hwnd, lParam):
            nonlocal found_hwnd
            if user32.IsWindowVisible(hwnd):
                length = user32.GetWindowTextLengthW(hwnd)
                if length > 0:
                    buff = ctypes.create_unicode_buffer(length + 1)
                    user32.GetWindowTextW(hwnd, buff, length + 1)
                    title = buff.value
                    pid = ctypes.c_ulong()
                    user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))

                    is_match = False
                    if ableton_pids and pid.value in ableton_pids:
                        is_match = True
                    elif "ableton live" in title.lower():
                        is_match = True

                    if is_match and title not in ['Default IME', 'MSCTFIME UI', 'DDE Server Window']:
                        found_hwnd = hwnd
                        return False
            return True

        WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
        if hDesk:
            user32.EnumDesktopWindows(hDesk, WNDENUMPROC(enum_proc), 0)
        else:
            user32.EnumWindows(WNDENUMPROC(enum_proc), 0)

        return found_hwnd

    def is_running(self) -> bool:
        """Indica si Ableton Live se encuentra en ejecución."""
        return self.find_ableton_window() is not None

    def launch_or_focus(self) -> bool:
        """Enfoca la ventana de Ableton Live o la inicia si no está corriendo."""
        hwnd = self.find_ableton_window()
        if hwnd:
            ctypes.windll.user32.ShowWindow(hwnd, 9) # SW_RESTORE
            ctypes.windll.user32.SetForegroundWindow(hwnd)
            return True

        exe = self.get_executable_path()
        if exe and os.path.exists(exe):
            try:
                subprocess.Popen([exe])
                time.sleep(2.0)
                return True
            except Exception as e:
                print(f"[AbletonManager] Error lanzando ejecutable: {e}")
                return False

        print("[AbletonManager] No se localizó el ejecutable de Ableton Live.")
        return False

    def check_modal_dialog(self, hwnd: int) -> Optional[Dict[str, Any]]:
        """
        Verifica si la ventana de Ableton tiene un cuadro de diálogo modal activo
        (como el de '¿Desea guardar los cambios en ...?').
        """
        user32 = ctypes.windll.user32
        hDesk = user32.OpenInputDesktop(0, False, 0x01FF)
        if hDesk:
            user32.SetThreadDesktop(hDesk)

        popup_hwnd = user32.GetLastActivePopup(hwnd)
        if popup_hwnd and popup_hwnd != hwnd and user32.IsWindowVisible(popup_hwnd):
            length = user32.GetWindowTextLengthW(popup_hwnd)
            title = ""
            if length > 0:
                buff = ctypes.create_unicode_buffer(length + 1)
                user32.GetWindowTextW(popup_hwnd, buff, length + 1)
                title = buff.value
            return {
                "hwnd": popup_hwnd,
                "title": title or "¿Deseas guardar los cambios del proyecto actual?"
            }
        return None

    def execute_action(
        self,
        action: str,
        target_project: Optional[str] = None,
        export_preset: Optional[str] = None,
        save_current_first: Optional[bool] = None
    ) -> Dict[str, Any]:
        """
        Ejecuta una acción musical en Ableton Live con gestión preventiva de cuadros de diálogo modales:
        - LAUNCH_OR_FOCUS: abrir o enfocar
        - NEW_PROJECT: nuevo Live Set (Ctrl+N) con detección de diálogo de guardado
        - SAVE_PROJECT: guardar sesión (Ctrl+S)
        - SAVE_PROJECT_AS: guardar como (Ctrl+Shift+S)
        - OPEN_PROJECT: abrir set (Ctrl+O o ruta directa)
        - EXPORT_AUDIO: diálogo exportar audio (Ctrl+Shift+R)
        - PLAY_PAUSE: reproducir o pausar (Barra espaciadora)
        - RECORD: grabar (F9)
        - TOGGLE_LOOP: bucle de arreglo (Ctrl+L)
        - TOGGLE_METRONOME: metrónomo
        - CONFIRM_SAVE_BEFORE_ACTION: confirmar guardado en modal activo
        - DISCARD_AND_CONTINUE: descartar cambios en modal activo
        - CANCEL_ACTION: cancelar y cerrar modal
        """
        action = action.upper()

        if action == "LAUNCH_OR_FOCUS":
            ok = self.launch_or_focus()
            return {
                "success": ok,
                "requiresConfirmation": False,
                "message": "Ableton Live enfocado." if ok else "No se pudo enfocar Ableton Live."
            }

        # Para acciones que manipulan la sesión, enfocar primero
        hwnd = self.find_ableton_window()
        self.launch_or_focus()
        time.sleep(0.3)

        if action == "NEW_PROJECT":
            if save_current_first is True:
                pyautogui.hotkey("ctrl", "s")
                time.sleep(0.5)
                pyautogui.hotkey("ctrl", "n")
                return {
                    "success": True,
                    "requiresConfirmation": False,
                    "message": "Proyecto actual guardado y nuevo Live Set creado."
                }

            pyautogui.hotkey("ctrl", "n")
            time.sleep(0.4)

            modal = self.check_modal_dialog(hwnd) if hwnd else None
            if modal:
                if save_current_first is False:
                    pyautogui.press("d")
                    time.sleep(0.1)
                    pyautogui.press("n")
                    return {
                        "success": True,
                        "requiresConfirmation": False,
                        "message": "Cambios descartados y nuevo Live Set creado."
                    }
                else:
                    return {
                        "success": True,
                        "requiresConfirmation": True,
                        "confirmationTitle": modal.get("title") or "¿Deseas guardar los cambios del proyecto actual?",
                        "message": "Ableton Live solicita confirmación para guardar el proyecto actual antes de continuar."
                    }

            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Nuevo Live Set creado en Ableton Live."
            }

        elif action == "CONFIRM_SAVE_BEFORE_ACTION":
            pyautogui.press("enter")
            time.sleep(0.1)
            pyautogui.press("s")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Cambios guardados en Ableton Live."
            }

        elif action == "DISCARD_AND_CONTINUE":
            pyautogui.press("d")
            time.sleep(0.1)
            pyautogui.press("n")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Cambios descartados sin guardar."
            }

        elif action == "CANCEL_ACTION":
            pyautogui.press("esc")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Operación cancelada."
            }

        elif action == "SAVE_PROJECT":
            pyautogui.hotkey("ctrl", "s")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Proyecto guardado (Ctrl+S)."
            }

        elif action == "SAVE_PROJECT_AS":
            pyautogui.hotkey("ctrl", "shift", "s")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Guardar como activado (Ctrl+Shift+S)."
            }

        elif action == "OPEN_PROJECT":
            if save_current_first is True:
                pyautogui.hotkey("ctrl", "s")
                time.sleep(0.5)

            if target_project and os.path.exists(target_project):
                try:
                    os.startfile(target_project)
                    return {
                        "success": True,
                        "requiresConfirmation": False,
                        "message": f"Cargando proyecto {target_project}."
                    }
                except Exception:
                    pass
            pyautogui.hotkey("ctrl", "o")
            time.sleep(0.4)
            modal = self.check_modal_dialog(hwnd) if hwnd else None
            if modal and save_current_first is None:
                return {
                    "success": True,
                    "requiresConfirmation": True,
                    "confirmationTitle": modal.get("title") or "¿Deseas guardar los cambios del proyecto actual?",
                    "message": "Ableton Live solicita confirmación para guardar el proyecto actual."
                }
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Diálogo abrir proyecto abierto (Ctrl+O)."
            }

        elif action == "EXPORT_AUDIO":
            pyautogui.hotkey("ctrl", "shift", "r")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Exportar Audio/Video abierto (Ctrl+Shift+R)."
            }

        elif action == "PLAY_PAUSE":
            pyautogui.press("space")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Transporte alternado."
            }

        elif action == "RECORD":
            pyautogui.press("f9")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Grabación conmutada (F9)."
            }

        elif action == "TOGGLE_LOOP":
            pyautogui.hotkey("ctrl", "l")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Bucle conmutado (Ctrl+L)."
            }

        elif action == "TOGGLE_METRONOME":
            pyautogui.press("c")
            return {
                "success": True,
                "requiresConfirmation": False,
                "message": "Metrónomo conmutado."
            }

        return {
            "success": True,
            "requiresConfirmation": False,
            "message": f"Acción {action} ejecutada."
        }

    def scan_recent_projects(self, max_results: int = 15) -> List[Dict[str, Any]]:
        """
        Escanea carpetas estándar del usuario en busca de archivos de proyecto .als (Ableton Live Set).
        """
        projects = []
        search_dirs = [
            os.path.expanduser(r"~\Documents\Ableton"),
            os.path.expanduser(r"~\Music\Ableton"),
            os.path.expanduser(r"~\Music"),
            os.path.expanduser(r"~\Documents"),
            r"D:\Musica",
            r"D:\Audio",
            r"D:\Proyectos",
        ]

        found_paths = set()
        ignored_dirs = {".git", "node_modules", "build", ".gradle", "target", "bin", "obj", "dist", "temp", "tmp", "appdata"}
        max_depth = 3

        for directory in search_dirs:
            if not os.path.exists(directory):
                continue

            base_depth = directory.rstrip("/\\").count(os.sep)
            try:
                for root, dirs, files in os.walk(directory):
                    dirs[:] = [d for d in dirs if d.lower() not in ignored_dirs and not d.startswith(".")]
                    cur_depth = root.rstrip("/\\").count(os.sep)
                    if cur_depth - base_depth >= max_depth:
                        dirs[:] = []

                    for f in files:
                        if f.lower().endswith(".als") and not f.startswith("._"):
                            full_path = os.path.join(root, f)
                            if full_path in found_paths:
                                continue
                            found_paths.add(full_path)

                            try:
                                stat = os.stat(full_path)
                                name = os.path.splitext(f)[0]
                                projects.append({
                                    "name": name,
                                    "absolutePath": full_path,
                                    "lastModifiedEpoch": int(stat.st_mtime * 1000),
                                    "sizeBytes": stat.st_size,
                                    "dawType": "ABLETON_LIVE"
                                })
                            except Exception:
                                pass

                            if len(projects) >= max_results:
                                break
                    if len(projects) >= max_results:
                        break
            except Exception:
                pass

        projects.sort(key=lambda x: x["lastModifiedEpoch"], reverse=True)
        return projects[:max_results]

ableton_manager = AbletonManager()
