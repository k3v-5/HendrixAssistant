"""
Hendrix Desktop Companion - Linux Input Controller
Implementación de IInputController para Linux utilizando PyAutoGUI y/o xdotool.
"""
from typing import Tuple
import shutil
import subprocess
from core.platform.interfaces import IInputController

class LinuxInputController(IInputController):
    def __init__(self):
        self._screen_width = 1920
        self._screen_height = 1080
        self._has_xdotool = shutil.which("xdotool") is not None
        
        try:
            import pyautogui
            size = pyautogui.size()
            self._screen_width = size.width
            self._screen_height = size.height
        except Exception:
            pass

    @property
    def screen_size(self) -> Tuple[int, int]:
        return (self._screen_width, self._screen_height)

    def move_cursor(self, x: int, y: int) -> None:
        try:
            import pyautogui
            pyautogui.moveTo(x, y)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                subprocess.run(["xdotool", "mousemove", str(x), str(y)], check=False, timeout=1)
            except Exception:
                pass

    def click(self, x: int, y: int, button: str = "left", double: bool = False) -> None:
        try:
            import pyautogui
            clicks = 2 if double else 1
            pyautogui.click(x=x, y=y, button=button, clicks=clicks)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                btn_map = {"left": "1", "middle": "2", "right": "3"}
                b = btn_map.get(button.lower(), "1")
                cmd = ["xdotool", "mousemove", str(x), str(y)]
                if double:
                    cmd.extend(["click", "--repeat", "2", b])
                else:
                    cmd.extend(["click", b])
                subprocess.run(cmd, check=False, timeout=1)
            except Exception:
                pass

    def mouse_down(self, x: int, y: int, button: str = "left") -> None:
        try:
            import pyautogui
            pyautogui.moveTo(x, y)
            pyautogui.mouseDown(button=button)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                btn_map = {"left": "1", "middle": "2", "right": "3"}
                b = btn_map.get(button.lower(), "1")
                subprocess.run(["xdotool", "mousemove", str(x), str(y), "mousedown", b], check=False, timeout=1)
            except Exception:
                pass

    def mouse_up(self, x: int, y: int, button: str = "left") -> None:
        try:
            import pyautogui
            pyautogui.moveTo(x, y)
            pyautogui.mouseUp(button=button)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                btn_map = {"left": "1", "middle": "2", "right": "3"}
                b = btn_map.get(button.lower(), "1")
                subprocess.run(["xdotool", "mousemove", str(x), str(y), "mouseup", b], check=False, timeout=1)
            except Exception:
                pass

    def scroll(self, x: int, y: int, amount: int) -> None:
        try:
            import pyautogui
            pyautogui.moveTo(x, y)
            pyautogui.scroll(amount)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                # En X11: botón 4 es scroll arriba, botón 5 es scroll abajo
                btn = "4" if amount > 0 else "5"
                clicks = max(1, abs(amount) // 100)
                subprocess.run(["xdotool", "mousemove", str(x), str(y), "click", "--repeat", str(clicks), btn], check=False, timeout=1)
            except Exception:
                pass

    def press_key(self, key: str) -> None:
        try:
            import pyautogui
            pyautogui.press(key)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                subprocess.run(["xdotool", "key", key], check=False, timeout=1)
            except Exception:
                pass

    def type_text(self, text: str) -> None:
        try:
            import pyautogui
            pyautogui.write(text)
            return
        except Exception:
            pass

        if self._has_xdotool:
            try:
                subprocess.run(["xdotool", "type", "--delay", "10", text], check=False, timeout=2)
            except Exception:
                pass
