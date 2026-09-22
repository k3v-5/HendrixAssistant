import threading
from PIL import Image, ImageDraw
import pystray

def create_tray_icon_image():
    # Generar un icono limpio de 64x64 con un círculo azul turquesa
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.ellipse((4, 4, 60, 60), fill="#0284C7", outline="#38BDF8", width=3)
    # Letra H estilizada
    draw.line((24, 18, 24, 46), fill="white", width=5)
    draw.line((40, 18, 40, 46), fill="white", width=5)
    draw.line((24, 32, 40, 32), fill="white", width=5)
    return image

class DesktopTrayIcon:
    def __init__(self, on_show_window, on_exit):
        self.on_show_window = on_show_window
        self.on_exit = on_exit
        self.icon = None

    def start(self):
        menu = pystray.Menu(
            pystray.MenuItem("Abrir Hendrix Desktop", self._show),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Salir", self._exit)
        )
        self.icon = pystray.Icon("HendrixDesktop", create_tray_icon_image(), "Hendrix Desktop Companion", menu)
        threading.Thread(target=self.icon.run, daemon=True).start()

    def _show(self, icon, item):
        self.on_show_window()

    def _exit(self, icon, item):
        if self.icon:
            self.icon.stop()
        self.on_exit()

    def stop(self):
        if self.icon:
            self.icon.stop()
