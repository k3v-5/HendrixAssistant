"""
Hendrix Desktop Companion - Resilient PyAutoGUI Loader
Asegura que la importación de PyAutoGUI nunca genere un crash fatal al iniciar el servidor
de escritorio (por ejemplo, ante la ausencia de DISPLAY, errores de socket X11 o modo headless estricto).
"""
import sys
import time
from collections import namedtuple

Size = namedtuple("Size", ["width", "height"])
Point = namedtuple("Point", ["x", "y"])

class _DummyPyAutoGUI:
    """Implementación segura y no bloqueante de PyAutoGUI para modo Headless o sin X11."""
    FAILSAFE = False
    PAUSE = 0.0

    @staticmethod
    def size() -> Size:
        return Size(1920, 1080)

    @staticmethod
    def position() -> Point:
        return Point(0, 0)

    @staticmethod
    def moveTo(x: int, y: int, duration: float = 0.0) -> None:
        pass

    @staticmethod
    def moveRel(xOffset: int, yOffset: int, duration: float = 0.0) -> None:
        pass

    @staticmethod
    def click(x: int = None, y: int = None, clicks: int = 1, interval: float = 0.0, button: str = "left") -> None:
        pass

    @staticmethod
    def doubleClick(x: int = None, y: int = None) -> None:
        pass

    @staticmethod
    def rightClick(x: int = None, y: int = None) -> None:
        pass

    @staticmethod
    def mouseDown(x: int = None, y: int = None, button: str = "left") -> None:
        pass

    @staticmethod
    def mouseUp(x: int = None, y: int = None, button: str = "left") -> None:
        pass

    @staticmethod
    def press(keys, presses: int = 1, interval: float = 0.0) -> None:
        pass

    @staticmethod
    def hotkey(*args, **kwargs) -> None:
        pass

    @staticmethod
    def write(message: str, interval: float = 0.0) -> None:
        pass

    @staticmethod
    def scroll(clicks: int, x: int = None, y: int = None) -> None:
        pass

    @staticmethod
    def sleep(seconds: float) -> None:
        time.sleep(seconds)

    @staticmethod
    def screenshot(*args, **kwargs):
        return None

def ensure_safe_pyautogui():
    """
    Garantiza que 'pyautogui' esté importable sin lanzar excepciones no controladas.
    En Linux, primero resuelve automáticamente la variable DISPLAY.
    Si la importación de pyautogui o mouseinfo falla (ej: KeyError DISPLAY o ConnectionRefused),
    se registra un mock de degradación elegante en sys.modules['pyautogui'].
    """
    if "pyautogui" in sys.modules and not isinstance(sys.modules["pyautogui"], _DummyPyAutoGUI):
        return sys.modules["pyautogui"]

    if sys.platform.startswith("linux"):
        try:
            from core.platform.linux.display_env import ensure_linux_display
            ensure_linux_display()
        except Exception:
            pass

    try:
        import pyautogui
        pyautogui.FAILSAFE = False
        pyautogui.PAUSE = 0.0
        return pyautogui
    except Exception:
        dummy = _DummyPyAutoGUI()
        sys.modules["pyautogui"] = dummy
        return dummy
