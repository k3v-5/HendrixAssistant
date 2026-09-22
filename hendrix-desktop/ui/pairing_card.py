import json
import socket
import tkinter as tk
from tkinter import ttk
from PIL import Image, ImageTk
import qrcode
from config import config
from core.tunnel_manager import tunnel_manager

class PairingCard(ttk.Frame):
    def __init__(self, parent):
        super().__init__(parent)
        self.configure(style="Card.TFrame")
        self._qr_image_tk = None
        self._build_ui()
        tunnel_manager.add_listener(lambda active, url: self.after(0, self.update_tunnel_display))

    def _build_ui(self):
        # Título
        title_lbl = ttk.Label(
            self,
            text="📱 Conexión y Emparejamiento Móvil",
            font=("Segoe UI", 12, "bold"),
            foreground="#38BDF8"
        )
        title_lbl.pack(anchor="w", padx=16, pady=(16, 8))

        content_frame = ttk.Frame(self, style="Card.TFrame")
        content_frame.pack(fill="x", padx=16, pady=8)

        # Lado izquierdo: Información de Red y PIN
        info_frame = ttk.Frame(content_frame, style="Card.TFrame")
        info_frame.pack(side="left", fill="both", expand=True, padx=(0, 16))

        desc_lbl = ttk.Label(
            info_frame,
            text="Escanea el código QR desde Hendrix Assistant en tu celular o conéctate con los datos mostrados:",
            wraplength=280,
            font=("Segoe UI", 9),
            foreground="#94A3B8"
        )
        desc_lbl.pack(anchor="w", pady=(0, 10))

        # Fila IP
        ip_row = ttk.Frame(info_frame, style="Card.TFrame")
        ip_row.pack(fill="x", pady=3)
        ttk.Label(ip_row, text="Red Local WiFi:", font=("Segoe UI", 9, "bold"), foreground="#E2E8F0").pack(side="left")
        self.ip_val = ttk.Label(ip_row, text=f"{config.local_ip}:{config.port}", font=("Segoe UI", 9), foreground="#38BDF8")
        self.ip_val.pack(side="right")

        # Fila Túnel WAN
        wan_row = ttk.Frame(info_frame, style="Card.TFrame")
        wan_row.pack(fill="x", pady=3)
        ttk.Label(wan_row, text="Acceso WAN Global:", font=("Segoe UI", 9, "bold"), foreground="#E2E8F0").pack(side="left")
        self.wan_val = ttk.Label(wan_row, text="Inactivo (Solo WiFi)", font=("Segoe UI", 9), foreground="#94A3B8")
        self.wan_val.pack(side="right")

        # Fila PIN
        pin_row = ttk.Frame(info_frame, style="Card.TFrame")
        pin_row.pack(fill="x", pady=3)
        ttk.Label(pin_row, text="PIN de Seguridad:", font=("Segoe UI", 9, "bold"), foreground="#E2E8F0").pack(side="left")
        self.pin_val = ttk.Label(
            pin_row,
            text=f"  {config.pin}  ",
            font=("Segoe UI", 13, "bold"),
            foreground="#10B981",
            background="#064E3B"
        )
        self.pin_val.pack(side="right")

        # Fila Estado del Teléfono
        status_row = ttk.Frame(info_frame, style="Card.TFrame")
        status_row.pack(fill="x", pady=(8, 4))
        ttk.Label(status_row, text="Dispositivo Móvil:", font=("Segoe UI", 9, "bold"), foreground="#E2E8F0").pack(side="left")
        self.device_val = ttk.Label(status_row, text="Esperando enlace...", font=("Segoe UI", 9, "italic"), foreground="#F59E0B")
        self.device_val.pack(side="right")

        # Lado derecho: Código QR generado
        qr_frame = ttk.Frame(content_frame, style="Card.TFrame")
        qr_frame.pack(side="right")

        self.qr_label = ttk.Label(qr_frame, style="Card.TFrame")
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
        img = qr.make_image(fill_color="#0F172A", back_color="#38BDF8")
        img = img.resize((130, 130), Image.Resampling.NEAREST)

        self._qr_image_tk = ImageTk.PhotoImage(img)
        self.qr_label.configure(image=self._qr_image_tk)

    def update_tunnel_display(self):
        tunnel_url = tunnel_manager.get_tunnel_url()
        if tunnel_url:
            short_url = tunnel_url.replace("https://", "").replace("http://", "")
            if len(short_url) > 22:
                short_url = short_url[:20] + "..."
            self.wan_val.configure(text=f"🌐 {short_url}", foreground="#10B981")
        else:
            self.wan_val.configure(text="Inactivo (Solo WiFi)", foreground="#94A3B8")
        self.refresh_qr()

    def set_connected_device(self, name: str | None):
        if name:
            self.device_val.configure(text=f"🟢 {name}", foreground="#10B981")
        else:
            self.device_val.configure(text="Esperando enlace...", foreground="#F59E0B")
