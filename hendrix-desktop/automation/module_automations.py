import os
import time
import ctypes
from typing import Dict, Any, Optional
import pyautogui

try:
    import psutil
except ImportError:
    psutil = None

from automation.ableton_manager import ableton_manager
from automation.browser_manager import browser_manager

class BaseModuleController:
    """Clase base para controladores de automatización de aplicaciones en Windows."""

    def __init__(self, app_name: str, window_match_keywords: list, process_names: list):
        self.app_name = app_name
        self.window_match_keywords = [k.lower() for k in window_match_keywords]
        self.process_names = [p.lower() for p in process_names]

    def find_window(self) -> Optional[int]:
        """Localiza el HWND de la ventana principal de la aplicación."""
        user32 = ctypes.windll.user32
        hDesk = user32.OpenInputDesktop(0, False, 0x01FF)
        if hDesk:
            user32.SetThreadDesktop(hDesk)

        found_hwnd = None
        target_pids = set()

        if psutil:
            for p in psutil.process_iter(['pid', 'name']):
                try:
                    pname = p.info['name'].lower()
                    if any(target in pname for target in self.process_names):
                        target_pids.add(p.info['pid'])
                except Exception:
                    pass

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
                    elif any(k in title for k in self.window_match_keywords):
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

    def focus_window(self) -> bool:
        """Restaura y trae la ventana al primer plano."""
        hwnd = self.find_window()
        if hwnd:
            ctypes.windll.user32.ShowWindow(hwnd, 9)  # SW_RESTORE
            ctypes.windll.user32.SetForegroundWindow(hwnd)
            time.sleep(0.2)
            return True
        return False


class FLStudioController(BaseModuleController):
    def __init__(self):
        super().__init__(
            app_name="FL Studio",
            window_match_keywords=["fl studio", "fruity loops"],
            process_names=["fl64.exe", "fl.exe", "flstudio.exe"]
        )

    def execute(self, action: str) -> Dict[str, Any]:
        self.focus_window()
        action = action.upper()

        if action == "PLAY_PAUSE":
            pyautogui.press("space")
            return {"success": True, "message": "Play / Pausa en FL Studio"}
        elif action == "RECORD":
            pyautogui.press("r")
            return {"success": True, "message": "Grabación conmutada en FL Studio (R)"}
        elif action == "TOGGLE_MODE":
            pyautogui.press("l")
            return {"success": True, "message": "Modo Song / Pattern alternado (L)"}
        elif action == "TOGGLE_METRONOME":
            pyautogui.hotkey("ctrl", "m")
            return {"success": True, "message": "Metrónomo conmutado en FL Studio"}
        elif action == "SAVE_PROJECT":
            pyautogui.hotkey("ctrl", "s")
            return {"success": True, "message": "Proyecto guardado en FL Studio (Ctrl+S)"}
        elif action == "EXPORT_AUDIO":
            pyautogui.hotkey("ctrl", "r")
            return {"success": True, "message": "Exportar WAV abierto en FL Studio (Ctrl+R)"}
        elif action == "VIEW_MIXER":
            pyautogui.press("f9")
            return {"success": True, "message": "Mezclador abierto (F9)"}
        elif action == "VIEW_PIANO_ROLL":
            pyautogui.press("f7")
            return {"success": True, "message": "Piano Roll abierto (F7)"}

        return {"success": True, "message": f"Acción {action} enviada a FL Studio"}


class AdobeCreativeController(BaseModuleController):
    def __init__(self):
        super().__init__(
            app_name="Adobe Suite",
            window_match_keywords=["adobe premiere pro", "photoshop"],
            process_names=["adobe premiere pro.exe", "photoshop.exe"]
        )

    def execute(self, action: str) -> Dict[str, Any]:
        self.focus_window()
        action = action.upper()

        if action == "PLAY_PAUSE":
            pyautogui.press("space")
            return {"success": True, "message": "Play / Stop en Premiere"}
        elif action == "RAZOR_TOOL":
            pyautogui.press("c")
            return {"success": True, "message": "Herramienta Cuchilla activada (C)"}
        elif action == "SELECT_TOOL":
            pyautogui.press("v")
            return {"success": True, "message": "Herramienta Selección activada (V)"}
        elif action == "RIPPLE_DELETE":
            pyautogui.hotkey("shift", "delete")
            return {"success": True, "message": "Eliminar Hueco (Ripple Delete)"}
        elif action == "RENDER_TIMELINE":
            pyautogui.press("enter")
            return {"success": True, "message": "Renderizando Timeline (Enter)"}
        elif action == "EXPORT_MEDIA":
            pyautogui.hotkey("ctrl", "m")
            return {"success": True, "message": "Exportar Medios abierto (Ctrl+M)"}
        elif action == "BRUSH_TOOL":
            pyautogui.press("b")
            return {"success": True, "message": "Pincel activado en Photoshop (B)"}
        elif action == "SAVE_PROJECT":
            pyautogui.hotkey("ctrl", "s")
            return {"success": True, "message": "Proyecto Adobe guardado (Ctrl+S)"}

        return {"success": True, "message": f"Acción {action} enviada a Adobe Suite"}


class BlenderController(BaseModuleController):
    def __init__(self):
        super().__init__(
            app_name="Blender 3D",
            window_match_keywords=["blender"],
            process_names=["blender.exe"]
        )

    def execute(self, action: str) -> Dict[str, Any]:
        self.focus_window()
        action = action.upper()

        if action == "RENDER_IMAGE":
            pyautogui.press("f12")
            return {"success": True, "message": "Renderizado de Imagen iniciado en Blender (F12)"}
        elif action == "RENDER_ANIM":
            pyautogui.hotkey("ctrl", "f12")
            return {"success": True, "message": "Renderizado de Animación iniciado en Blender (Ctrl+F12)"}
        elif action == "TOGGLE_SHADING":
            pyautogui.press("z")
            return {"success": True, "message": "Menú Sombreado Viewport (Z)"}
        elif action == "VIEW_CAMERA":
            try:
                pyautogui.press("num0")
            except Exception:
                pyautogui.press("0")
            return {"success": True, "message": "Vista de Cámara alternada en Blender"}
        elif action == "TOOL_MOVE":
            pyautogui.press("g")
            return {"success": True, "message": "Herramienta Mover (Grab G)"}
        elif action == "TOOL_ROTATE":
            pyautogui.press("r")
            return {"success": True, "message": "Herramienta Rotar (R)"}
        elif action == "TOOL_SCALE":
            pyautogui.press("s")
            return {"success": True, "message": "Herramienta Escalar (S)"}
        elif action == "SAVE_FILE":
            pyautogui.hotkey("ctrl", "s")
            return {"success": True, "message": "Archivo .blend guardado (Ctrl+S)"}

        return {"success": True, "message": f"Acción {action} enviada a Blender"}


class UnrealEngineController(BaseModuleController):
    def __init__(self):
        super().__init__(
            app_name="Unreal Engine 5",
            window_match_keywords=["unreal editor", "unreal engine"],
            process_names=["unrealeditor.exe"]
        )

    def execute(self, action: str) -> Dict[str, Any]:
        self.focus_window()
        action = action.upper()

        if action == "PLAY_IN_EDITOR":
            pyautogui.hotkey("alt", "p")
            return {"success": True, "message": "Play in Editor (PIE) iniciado en Unreal (Alt+P)"}
        elif action == "SIMULATE":
            pyautogui.hotkey("alt", "s")
            return {"success": True, "message": "Simulación iniciada en Unreal (Alt+S)"}
        elif action == "STOP_SIMULATION":
            pyautogui.press("esc")
            return {"success": True, "message": "Simulación / PIE detenida en Unreal (Esc)"}
        elif action == "CONTENT_DRAWER":
            pyautogui.hotkey("ctrl", "space")
            return {"success": True, "message": "Content Drawer alternado (Ctrl+Espacio)"}
        elif action == "SAVE_ALL":
            pyautogui.hotkey("ctrl", "shift", "s")
            return {"success": True, "message": "Todos los niveles y assets guardados (Ctrl+Shift+S)"}
        elif action == "BUILD_ALL":
            pyautogui.hotkey("ctrl", "shift", ";")
            return {"success": True, "message": "Compilación iniciada en Unreal"}
        elif action == "FOCUS_ACTOR":
            pyautogui.press("f")
            return {"success": True, "message": "Actor enfocado en Viewport (F)"}

        return {"success": True, "message": f"Acción {action} enviada a Unreal Engine"}


# Instancias singleton de controladores
fl_studio_controller = FLStudioController()
adobe_controller = AdobeCreativeController()
blender_controller = BlenderController()
unreal_controller = UnrealEngineController()

def execute_module_action(module_id: str, action_id: str, params: Optional[dict] = None) -> Dict[str, Any]:
    """
    Despachador unificado de comandos de módulos hacia la aplicación correspondiente.
    """
    mid = module_id.upper()
    aid = action_id.upper()

    if mid == "ABLETON_LIVE":
        return ableton_manager.execute_action(aid)
    elif mid == "FL_STUDIO":
        return fl_studio_controller.execute(aid)
    elif mid == "ADOBE_CREATIVE":
        return adobe_controller.execute(aid)
    elif mid == "BLENDER":
        return blender_controller.execute(aid)
    elif mid == "UNREAL_ENGINE":
        return unreal_controller.execute(aid)
    elif mid == "WEB_BROWSERS":
        return browser_manager.execute(aid, params)

    return {
        "success": False,
        "message": f"Módulo no reconocido: {module_id}"
    }
