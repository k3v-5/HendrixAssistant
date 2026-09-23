import tkinter as tk
from tkinter import ttk
from config import config
from ui.devices_network_view import DevicesNetworkView
from ui.clipboard_snippets_view import ClipboardSnippetsView
from ui.dropzone_files_view import DropzoneFilesView
from ui.system_logs_view import SystemLogsView
from ui.tray_icon import DesktopTrayIcon
from ui.plugins_manager_view import PluginsManagerView
from ui.theme import (
    COLOR_VOID_BLACK,
    COLOR_VOID_SURFACE,
    COLOR_VOID_BORDER,
    COLOR_NEON_LILAC,
    COLOR_TEXT_PRIMARY,
    COLOR_TEXT_SECONDARY,
    FONT_HEADER,
    FONT_SUBTITLE,
    setup_theme,
    create_secondary_button,
)

class AppWindow(tk.Tk):
    def __init__(self, on_close_callback=None):
        super().__init__()
        self.on_close_callback = on_close_callback
        self.title("Hendrix Desktop Companion")
        self.geometry("680x820")
        self.minsize(640, 700)
        self.configure(background=COLOR_VOID_BLACK)

        self._setup_theme()
        self._build_ui()

        # Configurar bandeja del sistema
        self.tray = DesktopTrayIcon(on_show_window=self.restore_from_tray, on_exit=self.quit_completely)
        self.tray.start()

        self.protocol("WM_DELETE_WINDOW", self.minimize_to_tray)

    def _setup_theme(self):
        setup_theme(self)

    def _build_ui(self):
        # Header principal con estética OLED Void
        header = tk.Frame(self, background=COLOR_VOID_SURFACE, height=64)
        header.pack(fill="x", padx=0, pady=0)

        title_box = tk.Frame(header, background=COLOR_VOID_SURFACE)
        title_box.pack(side="left", padx=20, pady=12)

        logo_lbl = tk.Label(
            title_box,
            text="HENDRIX DESKTOP",
            font=FONT_HEADER,
            foreground=COLOR_NEON_LILAC,
            background=COLOR_VOID_SURFACE
        )
        logo_lbl.pack(anchor="w")

        sub_lbl = tk.Label(
            title_box,
            text=f"Servicio activo en puerto {config.port} | LAN & WAN Hub",
            font=FONT_SUBTITLE,
            foreground=COLOR_TEXT_SECONDARY,
            background=COLOR_VOID_SURFACE
        )
        sub_lbl.pack(anchor="w")

        # Botón minimizar a bandeja (diseño sobrio de perfil secundario)
        min_btn = create_secondary_button(
            header,
            text="Minimizar a Bandeja",
            command=self.minimize_to_tray,
            padx=12,
            pady=6
        )
        min_btn.pack(side="right", padx=20, pady=16)

        # Línea divisoria sutil Morado Neón
        divider = tk.Frame(self, background=COLOR_VOID_BORDER, height=1)
        divider.pack(fill="x")

        # Contenedor de Pestañas (Notebook)
        self.notebook = ttk.Notebook(self)
        self.notebook.pack(fill="both", expand=True, padx=14, pady=12)

        # Tab 1: Conexión, Dispositivos & Firewall
        self.devices_network_view = DevicesNetworkView(self.notebook)
        self.notebook.add(self.devices_network_view, text="Conexión & Red")

        # Tab 2: Portapapeles & Snippets
        self.clipboard_snippets_view = ClipboardSnippetsView(self.notebook)
        self.notebook.add(self.clipboard_snippets_view, text="Portapapeles & Snippets")

        # Tab 3: Buzón Dropzone & Compartir Archivos
        self.dropzone_files_view = DropzoneFilesView(self.notebook)
        self.notebook.add(self.dropzone_files_view, text="Buzón Dropzone")

        # Tab 4: Sistema, Permisos & Logs
        self.system_logs_view = SystemLogsView(self.notebook)
        self.notebook.add(self.system_logs_view, text="Sistema & Logs")

        # Tab 5: Plugins & Scripts de Usuario
        self.plugins_manager_view = PluginsManagerView(self.notebook)
        self.notebook.add(self.plugins_manager_view, text="Plugins & Scripts")

        # Alias para compatibilidad hacia main.py
        self.pairing_card = self.devices_network_view.pairing_card
        self.activity_log = self.system_logs_view.activity_log

    def minimize_to_tray(self):
        self.withdraw()

    def restore_from_tray(self):
        self.deiconify()
        self.lift()
        self.focus_force()

    def quit_completely(self):
        self.tray.stop()
        if self.on_close_callback:
            self.on_close_callback()
        self.destroy()
