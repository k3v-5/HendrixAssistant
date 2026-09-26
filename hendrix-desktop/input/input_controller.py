import time
import ctypes
import pyautogui
from core.security_guard import SecurityGuard

pyautogui.FAILSAFE = False
pyautogui.PAUSE = 0.0

class InputController:
    def __init__(self):
        try:
            ctypes.windll.shcore.SetProcessDpiAwareness(2)
        except Exception:
            try:
                ctypes.windll.user32.SetProcessDPIAware()
            except Exception:
                pass
        self._ensure_input_desktop()
        self.screen_width = ctypes.windll.user32.GetSystemMetrics(0) or 1920
        self.screen_height = ctypes.windll.user32.GetSystemMetrics(1) or 1080
        self._last_size_check = time.time()

    def _ensure_input_desktop(self):
        try:
            hdesk = ctypes.windll.user32.OpenInputDesktop(0, False, 0x01FF)
            if hdesk:
                ctypes.windll.user32.SetThreadDesktop(hdesk)
                ctypes.windll.user32.CloseDesktop(hdesk)
        except Exception:
            pass

    def refresh_screen_size(self):
        now = time.time()
        # Refrescar como máximo cada 4 segundos para evitar sobrecarga del controlador gráfico
        if now - self._last_size_check > 4.0:
            self._last_size_check = now
            self._ensure_input_desktop()
            w = ctypes.windll.user32.GetSystemMetrics(0)
            h = ctypes.windll.user32.GetSystemMetrics(1)
            if w > 0 and h > 0:
                self.screen_width = w
                self.screen_height = h

    def process_action(self, action_dict: dict) -> bool:
        action_type = action_dict.get("actionType", "")

        x_ratio = action_dict.get("xRatio")
        y_ratio = action_dict.get("yRatio")

        target_x = None
        target_y = None
        if x_ratio is not None and y_ratio is not None:
            self.refresh_screen_size()
            rx = max(0.0, min(1.0, float(x_ratio)))
            ry = max(0.0, min(1.0, float(y_ratio)))
            target_x = max(0, min(self.screen_width - 1, int(rx * self.screen_width)))
            target_y = max(0, min(self.screen_height - 1, int(ry * self.screen_height)))

        # Acciones de ratón de latencia ultrabaja
        if action_type in ["CLICK", "DOUBLE_CLICK", "RIGHT_CLICK", "MOUSE_MOVE", "SCROLL"]:
            if not SecurityGuard.can_inject_mouse():
                return False

            if action_type == "MOUSE_MOVE":
                if target_x is not None and target_y is not None:
                    # Win32 SetCursorPos es instantáneo (< 0.05ms) y no bloquea el event loop
                    ctypes.windll.user32.SetCursorPos(target_x, target_y)
                return True

            self._ensure_input_desktop()

            if action_type == "CLICK":
                if target_x is not None and target_y is not None:
                    ctypes.windll.user32.SetCursorPos(target_x, target_y)
                ctypes.windll.user32.mouse_event(0x0002, 0, 0, 0, 0)  # MOUSEEVENTF_LEFTDOWN
                time.sleep(0.015)
                ctypes.windll.user32.mouse_event(0x0004, 0, 0, 0, 0)  # MOUSEEVENTF_LEFTUP
                return True

            elif action_type == "DOUBLE_CLICK":
                if target_x is not None and target_y is not None:
                    ctypes.windll.user32.SetCursorPos(target_x, target_y)
                ctypes.windll.user32.mouse_event(0x0002, 0, 0, 0, 0)
                time.sleep(0.015)
                ctypes.windll.user32.mouse_event(0x0004, 0, 0, 0, 0)
                time.sleep(0.05)
                ctypes.windll.user32.mouse_event(0x0002, 0, 0, 0, 0)
                time.sleep(0.015)
                ctypes.windll.user32.mouse_event(0x0004, 0, 0, 0, 0)
                return True

            elif action_type == "RIGHT_CLICK":
                if target_x is not None and target_y is not None:
                    ctypes.windll.user32.SetCursorPos(target_x, target_y)
                ctypes.windll.user32.mouse_event(0x0008, 0, 0, 0, 0)  # MOUSEEVENTF_RIGHTDOWN
                time.sleep(0.015)
                ctypes.windll.user32.mouse_event(0x0010, 0, 0, 0, 0)  # MOUSEEVENTF_RIGHTUP
                return True

            elif action_type == "SCROLL":
                scroll_y = int(action_dict.get("scrollDeltaY", 0) * 50)
                if scroll_y != 0:
                    pyautogui.scroll(scroll_y)
                return True

        # Acciones de teclado
        if action_type in ["KEY_PRESS", "HOTKEY", "TYPE_TEXT"]:
            if not SecurityGuard.can_inject_keyboard():
                return False

            if action_type == "KEY_PRESS":
                key_codes = action_dict.get("keyCodes", [])
                for k in key_codes:
                    pyautogui.press(k.lower())
                return True

            elif action_type == "HOTKEY":
                key_codes = action_dict.get("keyCodes", [])
                if key_codes:
                    pyautogui.hotkey(*[k.lower() for k in key_codes])
                return True

            elif action_type == "TYPE_TEXT":
                text = action_dict.get("textPayload", "")
                if text:
                    pyautogui.write(text, interval=0.01)
                return True

        # Conmutación de ventana activa
        if action_type == "FOCUS_WINDOW":
            hwnd = action_dict.get("hwnd", 0)
            if hwnd:
                from automation.window_manager import window_manager
                return window_manager.focus_window(int(hwnd))
            return False

        return False

input_controller = InputController()
