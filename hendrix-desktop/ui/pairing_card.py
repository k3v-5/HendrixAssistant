import json
import socket
import tkinter as tk
from tkinter import ttk
from PIL import Image, ImageTk
import qrcode
from config import config
from core.tunnel_manager import tunnel_manager
from ui.theme import (
    COLOR_VOID_SURFACE,
    COLOR_VOID_SURFACE_ELEVATED,
    COLOR_VOID_BORDER,
    COLOR_NEON_LILAC,
    COLOR_NEON_CYAN,
    COLOR_NEON_GREEN,
    COLOR_NEON_AMBER,
    COLOR_TEXT_PRIMARY,
    COLOR_TEXT_SECONDARY,
    COLOR_TEXT_MUTED,
    FONT_TITLE,
    FONT_BODY,
    FONT_BODY_BOLD,
    FONT_BODY_ITALIC,
    FONT_PIN_BADGE,
)

class PairingCard(ttk.Frame):
    def __init__(self, parent):
        super().__init__(parent)
        self.configure(style="Card.TFrame")
        self._qr_image_tk = None
        self._build_ui()
        tunnel_manager.add_listener(lambda active, url: self.after(0, self.update_tunnel_display))

    def _build_ui(self):
        # Título sin emojis toscos
        title_lbl = ttk.Label(
            self,
            text="Conexión y Emparejamiento Móvil",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        title_lbl.pack(anchor="w", padx=16, pady=(16, 8))

        content_frame = ttk.Frame(self, style="Card.TFrame")
        content_frame.pack(fill="x", padx=16, pady=8)

        # Lado izquierdo: Información de Red y PIN
        info_frame = ttk.Frame(content_frame, style="Card.TFrame")
        info_frame.pack(side="left", fill="both", expand=True, padx=(0, 16))

        desc_lbl = ttk.Label(
            info_frame,
            text="Escanea el código QR desde Hendrix Assistant en tu celular o ingresa los datos de conexión:",
            wraplength=280,
            font=FONT_BODY,
            foreground=COLOR_TEXT_SECONDARY
        )
        desc_lbl.pack(anchor="w", pady=(0, 10))

        # Fila IP
        ip_row = ttk.Frame(info_frame, style="Card.TFrame")
        ip_row.pack(fill="x", pady=3)
        ttk.Label(ip_row, text="Red Local WiFi:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).pack(side="left")
        self.ip_val = ttk.Label(ip_row, text=f"{config.local_ip}:{config.port}", font=FONT_BODY, foreground=COLOR_NEON_CYAN)
        self.ip_val.pack(side="right")

        # Fila Túnel WAN
        wan_row = ttk.Frame(info_frame, style="Card.TFrame")
        wan_row.pack(fill="x", pady=3)
        ttk.Label(wan_row, text="Acceso WAN Global:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).pack(side="left")
        self.wan_val = ttk.Label(wan_row, text="Inactivo (Solo WiFi)", font=FONT_BODY, foreground=COLOR_TEXT_MUTED)
        self.wan_val.pack(side="right")

        # Fila PIN con cápsula estética
        pin_row = ttk.Frame(info_frame, style="Card.TFrame")
        pin_row.pack(fill="x", pady=4)
        ttk.Label(pin_row, text="PIN de Seguridad:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).pack(side="left")

        pin_badge_container = tk.Frame(
            pin_row,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1,
            padx=8,
            pady=2
        )
        pin_badge_container.pack(side="right")

        self.pin_val = tk.Label(
            pin_badge_container,
            text=f"{config.pin}",
            font=FONT_PIN_BADGE,
            foreground=COLOR_NEON_GREEN,
            background=COLOR_VOID_SURFACE
        )
        self.pin_val.pack()

        # Fila Estado del Teléfono
        status_row = ttk.Frame(info_frame, style="Card.TFrame")
        status_row.pack(fill="x", pady=(8, 4))
        ttk.Label(status_row, text="Dispositivo Móvil:", font=FONT_BODY_BOLD, foreground=COLOR_TEXT_PRIMARY).pack(side="left")
        self.device_val = ttk.Label(status_row, text="Esperando enlace...", font=FONT_BODY_ITALIC, foreground=COLOR_NEON_AMBER)
        self.device_val.pack(side="right")

        # Lado derecho: Código QR generado con paleta Morado Neón
        qr_container = tk.Frame(
            content_frame,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1,
            padx=6,
            pady=6
        )
        qr_container.pack(side="right")

        self.qr_label = tk.Label(qr_container, background=COLOR_VOID_SURFACE)
        self.qr_label.pack()
        self.refresh_qr()
        self.update_tunnel_display()

    def refresh_qr(self):
        tunnel_url = tunnel_manager.get_tunnel_url()
        payload_data = {
            "host": config.local_ip,
            "port": config.port,
            "pin": config.pin,
            "hostname": socket.gethostname()
        }
        if tunnel_url:
            payload_data["tunnel"] = tunnel_url

        qr_payload = json.dumps(payload_data)
        qr = qrcode.QRCode(
            version=1,
            box_size=4,
            border=2,
            error_correction=qrcode.constants.ERROR_CORRECT_L
        )
        qr.add_data(qr_payload)
        qr.make(fit=True)
        # Paleta Neón: fondo oscuro profundo, módulos en Morado Neón Lila
        img = qr.make_image(fill_color=COLOR_NEON_LILAC, back_color=COLOR_VOID_SURFACE)
        img = img.resize((130, 130), Image.Resampling.NEAREST)

        self._qr_image_tk = ImageTk.PhotoImage(img)
        self.qr_label.configure(image=self._qr_image_tk)

    def update_tunnel_display(self):
        tunnel_url = tunnel_manager.get_tunnel_url()
        if tunnel_url:
            short_url = tunnel_url.replace("https://", "").replace("http://", "")
            if len(short_url) > 22:
                short_url = short_url[:20] + "..."
            self.wan_val.configure(text=f"Activo: {short_url}", foreground=COLOR_NEON_GREEN)
        else:
            self.wan_val.configure(text="Inactivo (Solo WiFi)", foreground=COLOR_TEXT_MUTED)
        self.refresh_qr()

    def set_connected_device(self, name: str | None):
        if name:
            self.device_val.configure(text=f"Conectado: {name}", font=FONT_BODY_BOLD, foreground=COLOR_NEON_GREEN)
        else:
            self.device_val.configure(text="Esperando enlace...", font=FONT_BODY_ITALIC, foreground=COLOR_NEON_AMBER)
