import tkinter as tk
from tkinter import ttk
from datetime import datetime

class ActivityLogView(ttk.Frame):
    def __init__(self, parent):
        super().__init__(parent)
        self.configure(style="Card.TFrame")
        self._build_ui()

    def _build_ui(self):
        title_lbl = ttk.Label(
            self,
            text="📝 Registro de Actividad en Tiempo Real",
            font=("Segoe UI", 12, "bold"),
            foreground="#38BDF8"
        )
        title_lbl.pack(anchor="w", padx=16, pady=(16, 8))

        log_container = ttk.Frame(self, style="Card.TFrame")
        log_container.pack(fill="both", expand=True, padx=16, pady=(0, 16))

        self.text_area = tk.Text(
            log_container,
            height=7,
            background="#0F172A",
            foreground="#E2E8F0",
            insertbackground="#38BDF8",
            font=("Consolas", 9),
            relief="flat",
            wrap="word",
            state="disabled"
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
