import tkinter as tk
from tkinter import ttk
from datetime import datetime
from ui.theme import (
    COLOR_VOID_SURFACE,
    COLOR_VOID_BORDER,
    COLOR_NEON_LILAC,
    COLOR_TEXT_PRIMARY,
    FONT_TITLE,
    FONT_TERMINAL,
)

class ActivityLogView(ttk.Frame):
    def __init__(self, parent):
        super().__init__(parent)
        self.configure(style="Card.TFrame")
        self._build_ui()

    def _build_ui(self):
        title_lbl = ttk.Label(
            self,
            text="Registro de Actividad en Tiempo Real",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        title_lbl.pack(anchor="w", padx=16, pady=(16, 8))

        log_container = tk.Frame(
            self,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        log_container.pack(fill="both", expand=True, padx=16, pady=(0, 16))

        self.text_area = tk.Text(
            log_container,
            height=7,
            background=COLOR_VOID_SURFACE,
            foreground=COLOR_TEXT_PRIMARY,
            insertbackground=COLOR_NEON_LILAC,
            selectbackground=COLOR_NEON_LILAC,
            selectforeground="#000000",
            font=FONT_TERMINAL,
            relief="flat",
            wrap="word",
            state="disabled",
            padx=8,
            pady=6
        )
        scrollbar = ttk.Scrollbar(log_container, orient="vertical", command=self.text_area.yview)
        self.text_area.configure(yscrollcommand=scrollbar.set)

        self.text_area.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")

    def append_log(self, text: str):
        timestamp = datetime.now().strftime("%H:%M:%S")
        formatted = f"[{timestamp}] {text}\n"

        self.text_area.configure(state="normal")
        self.text_area.insert("end", formatted)
        self.text_area.see("end")
        self.text_area.configure(state="disabled")
