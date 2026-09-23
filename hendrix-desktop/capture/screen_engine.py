import io
import mss
from PIL import Image
import ctypes
from ctypes import wintypes

class RECT(ctypes.Structure):
    _fields_ = [
        ("left", ctypes.c_long),
        ("top", ctypes.c_long),
        ("right", ctypes.c_long),
        ("bottom", ctypes.c_long)
    ]

class ScreenEngine:
    def __init__(self):
        self._sct = None

    def _ensure_input_desktop(self):
        """Asegura que el hilo actual esté vinculado al escritorio interactivo activo (Default)."""
        try:
            hdesk = ctypes.windll.user32.OpenInputDesktop(0, False, 0x01FF)
            if hdesk:
                ctypes.windll.user32.SetThreadDesktop(hdesk)
                ctypes.windll.user32.CloseDesktop(hdesk)
        except Exception:
            pass

    def _get_sct(self):
        self._ensure_input_desktop()
        if self._sct is None:
            self._sct = mss.MSS()
        return self._sct

    def get_active_window_rect(self):
        self._ensure_input_desktop()
        try:
            hwnd = ctypes.windll.user32.GetForegroundWindow()
            if not hwnd:
                return None
            rect = RECT()
            ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
            width = rect.right - rect.left
            height = rect.bottom - rect.top
            if width > 50 and height > 50:
                return {
                    "left": max(0, rect.left),
                    "top": max(0, rect.top),
                    "width": width,
                    "height": height
                }
        except Exception:
            pass
        return None

    def is_session_locked(self) -> bool:
        """Determina si la estación de trabajo de Windows está en pantalla de bloqueo (Win+L / Winlogon)."""
        try:
            desk = ctypes.windll.user32.OpenInputDesktop(0, False, 0x0100)
            if desk == 0:
                return True
            ctypes.windll.user32.CloseDesktop(desk)
            return False
        except Exception:
            return False

    def capture_webp(self, quality: int = 75, crop_to_active: bool = False) -> bytes:
        self._ensure_input_desktop()
        if self.is_session_locked():
            # Sesión de Windows bloqueada (Winlogon): generar fotograma estilizado de alta resolución
            from PIL import ImageDraw
            img = Image.new("RGB", (1280, 720), color="#0B0F19")
            draw = ImageDraw.Draw(img)
            draw.rounded_rectangle([(80, 80), (1200, 640)], radius=24, fill="#0F172A", outline="#EF4444", width=3)
            draw.text((640, 280), "🔒 Computadora Bloqueada (Windows Lock Screen)", fill="#EF4444", anchor="mm")
            draw.text((640, 330), "Windows aísla la pantalla de bloqueo en el escritorio seguro 'Winlogon'.", fill="#E2E8F0", anchor="mm")
            draw.text((640, 370), "La conexión remota sigue 100% activa. Puedes enviar tu PIN o contraseña", fill="#94A3B8", anchor="mm")
            draw.text((640, 410), "usando el teclado de Hendrix en tu móvil para desbloquearla.", fill="#94A3B8", anchor="mm")
            draw.rounded_rectangle([(440, 480), (840, 530)], radius=12, fill="#1E293B", outline="#38BDF8", width=2)
            draw.text((640, 505), "⚡ Desbloquear con Hendrix Assistant", fill="#38BDF8", anchor="mm")

            buffer = io.BytesIO()
            img.save(buffer, format="WEBP", quality=quality, method=0)
            return buffer.getvalue()

        sct = self._get_sct()
        monitor = sct.monitors[1] if len(sct.monitors) > 1 else sct.monitors[0]

        if crop_to_active:
            win_rect = self.get_active_window_rect()
            if win_rect:
                monitor = {
                    "left": win_rect["left"],
                    "top": win_rect["top"],
                    "width": win_rect["width"],
                    "height": win_rect["height"]
                }

        img = None
        # Intento 1: MSS con el monitor seleccionado
        try:
            sct_img = sct.grab(monitor)
            img = Image.frombytes("RGB", sct_img.size, sct_img.bgra, "raw", "BGRX")
        except Exception:
            # Recrear instancia de MSS si falló el contexto gráfico
            try:
                self._ensure_input_desktop()
                if self._sct:
                    try:
                        self._sct.close()
                    except Exception:
                        pass
                self._sct = mss.MSS()
                sct_img = self._sct.grab(monitor)
                img = Image.frombytes("RGB", sct_img.size, sct_img.bgra, "raw", "BGRX")
            except Exception:
                pass

        # Intento 2: Fallback con PIL ImageGrab si MSS no pudo capturar
        if img is None:
            try:
                self._ensure_input_desktop()
                from PIL import ImageGrab
                if crop_to_active and isinstance(monitor, dict):
                    bbox = (
                        monitor["left"],
                        monitor["top"],
                        monitor["left"] + monitor["width"],
                        monitor["top"] + monitor["height"]
                    )
                    img = ImageGrab.grab(bbox=bbox)
                else:
                    img = ImageGrab.grab()
            except Exception:
                pass

        # Intento 3: Fallback estilizado si la pantalla no está accesible
        if img is None:
            from PIL import ImageDraw
            img = Image.new("RGB", (1280, 720), color="#0F172A")
            draw = ImageDraw.Draw(img)
            draw.rectangle([(40, 40), (1240, 680)], outline="#38BDF8", width=3)
            draw.text((640, 330), "Hendrix PC Workspace", fill="#38BDF8", anchor="mm")
            draw.text((640, 370), "Pantalla de Windows en espera o segundo plano", fill="#94A3B8", anchor="mm")
            draw.text((640, 410), "Listo para recibir comandos de raton, teclado y dictado", fill="#64748B", anchor="mm")

        buffer = io.BytesIO()
        img.save(buffer, format="WEBP", quality=quality, method=0)
        return buffer.getvalue()

screen_engine = ScreenEngine()
