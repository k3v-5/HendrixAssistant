import tkinter as tk
from tkinter import ttk
from config import config
from ui.telemetry_view import TelemetryView
from ui.activity_log import ActivityLogView

class SystemLogsView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background="#0B0F19")
        self._build_ui()

    def _build_ui(self):
        # 1. Telemetría de Recursos
        self.telemetry_view = TelemetryView(self)
        self.telemetry_view.pack(fill="x", pady=(0, 12))

        # 2. Permisos y Seguridad
        sec_card = ttk.Frame(self, style="Card.TFrame")
        sec_card.pack(fill="x", pady=(0, 12))

        sec_title = ttk.Label(
            sec_card,
            text="🛡️ Permisos de Control y Seguridad",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        sec_title.pack(anchor="w", padx=16, pady=(12, 6))

        sec_opts = ttk.Frame(sec_card, style="Card.TFrame")
        sec_opts.pack(fill="x", padx=16, pady=(0, 12))

        self.mouse_var = tk.BooleanVar(value=config.allow_mouse_control)
        ttk.Checkbutton(
            sec_opts,
            text="Permitir control de Ratón físico (clics, movimientos y scrolls)",
            variable=self.mouse_var,
            command=self._save_perms
        ).pack(anchor="w", pady=2)

        self.kbd_var = tk.BooleanVar(value=config.allow_keyboard_control)
        ttk.Checkbutton(
            sec_opts,
            text="Permitir control de Teclado, Atajos y Dictado directo por voz",
            variable=self.kbd_var,
            command=self._save_perms
        ).pack(anchor="w", pady=2)

        self.cap_var = tk.BooleanVar(value=config.allow_screen_capture)
        ttk.Checkbutton(
            sec_opts,
            text="Permitir Captura de Pantalla bajo demanda (Snapshots WebP ultraligeros)",
            variable=self.cap_var,
            command=self._save_perms
        ).pack(anchor="w", pady=2)

        # 3. Registro en Vivo
        self.activity_log = ActivityLogView(self)
        self.activity_log.pack(fill="both", expand=True)

    def _save_perms(self):
        config.allow_mouse_control = self.mouse_var.get()
        config.allow_keyboard_control = self.kbd_var.get()
        config.allow_screen_capture = self.cap_var.get()
        config.save()
        self.activity_log.append_log("Configuración de permisos de seguridad actualizada")
