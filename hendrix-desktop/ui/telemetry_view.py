import tkinter as tk
from tkinter import ttk
import psutil
from ui.theme import (
    COLOR_NEON_LILAC,
    COLOR_NEON_CYAN,
    COLOR_TEXT_PRIMARY,
    FONT_TITLE,
    FONT_BODY_BOLD,
)

class TelemetryView(ttk.Frame):
    def __init__(self, parent):
        super().__init__(parent)
        self.configure(style="Card.TFrame")
        self._build_ui()
        self._update_loop()

    def _build_ui(self):
        title_lbl = ttk.Label(
            self,
            text="Monitor de Recursos y Telemetría",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        title_lbl.pack(anchor="w", padx=16, pady=(16, 8))

        grid_frame = ttk.Frame(self, style="Card.TFrame")
        grid_frame.pack(fill="x", padx=16, pady=(0, 16))

        # CPU
        ttk.Label(grid_frame, text="Uso de CPU:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).grid(row=0, column=0, sticky="w", pady=4)
        self.cpu_bar = ttk.Progressbar(grid_frame, length=180, mode="determinate")
        self.cpu_bar.grid(row=0, column=1, sticky="w", padx=12, pady=4)
        self.cpu_lbl = ttk.Label(grid_frame, text="0%", font=FONT_BODY_BOLD, foreground=COLOR_NEON_CYAN)
        self.cpu_lbl.grid(row=0, column=2, sticky="e", pady=4)

        # RAM
        ttk.Label(grid_frame, text="Memoria RAM:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).grid(row=1, column=0, sticky="w", pady=4)
        self.ram_bar = ttk.Progressbar(grid_frame, length=180, mode="determinate")
        self.ram_bar.grid(row=1, column=1, sticky="w", padx=12, pady=4)
        self.ram_lbl = ttk.Label(grid_frame, text="0%", font=FONT_BODY_BOLD, foreground=COLOR_NEON_CYAN)
        self.ram_lbl.grid(row=1, column=2, sticky="e", pady=4)

    def _update_loop(self):
        try:
            cpu = psutil.cpu_percent(interval=None)
            mem = psutil.virtual_memory()

            self.cpu_bar["value"] = cpu
            self.cpu_lbl.configure(text=f"{int(cpu)}%")

            self.ram_bar["value"] = mem.percent
            self.ram_lbl.configure(text=f"{int(mem.percent)}% ({round(mem.used/(1024**3), 1)}GB)")
        except Exception:
            pass

        self.after(2000, self._update_loop)
