import tkinter as tk
from tkinter import ttk, messagebox
from config import config
from core.session_manager import session_manager
from core.network_diagnostic import network_diagnostic
from core.tunnel_manager import tunnel_manager
from ui.pairing_card import PairingCard
from ui.theme import (
    COLOR_VOID_BLACK,
    COLOR_VOID_SURFACE,
    COLOR_VOID_SURFACE_ELEVATED,
    COLOR_VOID_BORDER,
    COLOR_NEON_LILAC,
    COLOR_NEON_GREEN,
    COLOR_NEON_AMBER,
    COLOR_NEON_ROSE,
    COLOR_TEXT_PRIMARY,
    COLOR_TEXT_SECONDARY,
    COLOR_TEXT_MUTED,
    FONT_TITLE,
    FONT_BODY,
    FONT_BODY_BOLD,
    FONT_BODY_ITALIC,
    FONT_CAPTION,
    create_neon_button,
    create_secondary_button,
    create_accent_button,
    create_warning_button,
    create_danger_button,
)
import time

class DevicesNetworkView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background=COLOR_VOID_BLACK)
        self._build_ui()
        session_manager.on_devices_updated = self._refresh_devices_threadsafe

    def _refresh_devices_threadsafe(self):
        self.after(0, self.refresh_devices)

    def _build_ui(self):
        # Canvas scrollable para que todo quepa cómodamente
        canvas = tk.Canvas(self, background=COLOR_VOID_BLACK, highlightthickness=0)
        scrollbar = ttk.Scrollbar(self, orient="vertical", command=canvas.yview)
        self.scroll_content = tk.Frame(canvas, background=COLOR_VOID_BLACK)

        self.scroll_content.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        canvas.create_window((0, 0), window=self.scroll_content, anchor="nw", width=620)
        canvas.configure(yscrollcommand=scrollbar.set)

        canvas.pack(side="left", fill="both", expand=True, padx=(0, 4))
        scrollbar.pack(side="right", fill="y")

        # 1. Tarjeta de Emparejamiento Local (QR y PIN)
        self.pairing_card = PairingCard(self.scroll_content)
        self.pairing_card.pack(fill="x", pady=(0, 12))

        # 2. Tarjeta de Acceso Remoto Global WAN
        self._build_wan_card()

        # 3. Tarjeta de Dispositivos Móviles Conectados / Emparejados
        self._build_devices_card()

        # 4. Tarjeta de Diagnóstico de Red y Firewall de Windows
        self._build_diagnostic_card()

    def _build_wan_card(self):
        wan_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        wan_card.pack(fill="x", pady=(0, 12))

        wan_title = ttk.Label(
            wan_card,
            text="Acceso Remoto Global (WAN / Fuera de Casa)",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        wan_title.pack(anchor="w", padx=16, pady=(12, 4))

        wan_body = ttk.Frame(wan_card, style="Card.TFrame")
        wan_body.pack(fill="x", padx=16, pady=(0, 12))

        wan_desc = ttk.Label(
            wan_body,
            text="Conecta tu dispositivo móvil fuera del hogar mediante un túnel seguro:",
            font=FONT_BODY,
            foreground=COLOR_TEXT_SECONDARY
        )
        wan_desc.pack(anchor="w", pady=(0, 6))

        wan_input_row = ttk.Frame(wan_body, style="Card.TFrame")
        wan_input_row.pack(fill="x", pady=2)

        entry_container = tk.Frame(
            wan_input_row,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        entry_container.pack(side="left", fill="x", expand=True, padx=(0, 8))

        self.tunnel_entry_var = tk.StringVar(value=getattr(config, "remote_tunnel_url", "") or "")
        self.tunnel_entry = tk.Entry(
            entry_container,
            textvariable=self.tunnel_entry_var,
            font=FONT_BODY,
            bg=COLOR_VOID_SURFACE,
            fg=COLOR_TEXT_PRIMARY,
            insertbackground=COLOR_NEON_LILAC,
            relief="flat"
        )
        self.tunnel_entry.pack(fill="x", padx=6, ipady=4)

        save_tunnel_btn = create_neon_button(
            wan_input_row,
            text="Guardar URL",
            command=self._save_tunnel_url,
            padx=10,
            pady=4
        )
        save_tunnel_btn.pack(side="right")

        self.wan_status_lbl = ttk.Label(
            wan_body,
            text="Modo: Red Local WiFi únicamente" if not getattr(config, "remote_tunnel_url", None) else f"Túnel configurado: {config.remote_tunnel_url}",
            font=FONT_BODY_ITALIC,
            foreground=COLOR_NEON_GREEN if getattr(config, "remote_tunnel_url", None) else COLOR_TEXT_MUTED
        )
        self.wan_status_lbl.pack(anchor="w", pady=(6, 0))

    def _save_tunnel_url(self):
        new_url = self.tunnel_entry_var.get().strip()
        tunnel_manager.set_custom_tunnel_url(new_url)
        if new_url:
            self.wan_status_lbl.configure(text=f"Túnel configurado: {new_url}", foreground=COLOR_NEON_GREEN)
        else:
            self.wan_status_lbl.configure(text="Modo: Red Local WiFi únicamente", foreground=COLOR_TEXT_MUTED)

    def _build_devices_card(self):
        dev_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        dev_card.pack(fill="x", pady=(0, 12))

        header_row = ttk.Frame(dev_card, style="Card.TFrame")
        header_row.pack(fill="x", padx=16, pady=(12, 6))

        dev_title = ttk.Label(
            header_row,
            text="Dispositivos Móviles Emparejados",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        dev_title.pack(side="left")

        refresh_btn = create_secondary_button(
            header_row,
            text="Actualizar",
            command=self.refresh_devices,
            padx=8,
            pady=2
        )
        refresh_btn.pack(side="right")

        self.devices_container = ttk.Frame(dev_card, style="Card.TFrame")
        self.devices_container.pack(fill="x", padx=16, pady=(0, 12))

        self.refresh_devices()

    def refresh_devices(self):
        for widget in self.devices_container.winfo_children():
            widget.destroy()

        devices = session_manager.get_all_paired_devices()
        if not devices:
            lbl = ttk.Label(
                self.devices_container,
                text="No hay dispositivos móviles emparejados actualmente.\nEscanea el código QR desde Hendrix Assistant en tu teléfono.",
                font=FONT_BODY_ITALIC,
                foreground=COLOR_TEXT_MUTED
            )
            lbl.pack(anchor="w", pady=6)
            return

        for dev in devices:
            token = dev.get("token")
            is_online = dev.get("is_online", False)
            name = dev.get("name", "Móvil")
            ip = dev.get("ip", "Desconocida")

            row = tk.Frame(
                self.devices_container,
                background=COLOR_VOID_SURFACE,
                highlightbackground=COLOR_VOID_BORDER,
                highlightthickness=1
            )
            row.pack(fill="x", pady=3, padx=0)

            status_text = "En línea" if is_online else "Desconectado"
            status_color = COLOR_NEON_GREEN if is_online else COLOR_TEXT_MUTED

            info_box = tk.Frame(row, background=COLOR_VOID_SURFACE)
            info_box.pack(side="left", fill="x", expand=True, padx=10, pady=8)

            title_row = tk.Frame(info_box, background=COLOR_VOID_SURFACE)
            title_row.pack(anchor="w")

            title_lbl = tk.Label(
                title_row,
                text=name,
                font=FONT_BODY_BOLD,
                foreground=COLOR_TEXT_PRIMARY if is_online else COLOR_TEXT_SECONDARY,
                background=COLOR_VOID_SURFACE
            )
            title_lbl.pack(side="left")

            badge_lbl = tk.Label(
                title_row,
                text=f" • {status_text}",
                font=FONT_CAPTION,
                foreground=status_color,
                background=COLOR_VOID_SURFACE
            )
            badge_lbl.pack(side="left", padx=4)

            detail_lbl = tk.Label(
                info_box,
                text=f"IP: {ip} | Token: {token[:8]}...",
                font=FONT_CAPTION,
                foreground=COLOR_TEXT_MUTED,
                background=COLOR_VOID_SURFACE
            )
            detail_lbl.pack(anchor="w")

            btn_box = tk.Frame(row, background=COLOR_VOID_SURFACE)
            btn_box.pack(side="right", padx=10)

            if is_online:
                disc_btn = create_warning_button(
                    btn_box,
                    text="Desconectar",
                    command=lambda t=token: self._disconnect_device(t),
                    padx=8,
                    pady=2
                )
                disc_btn.pack(side="left", padx=3)

            rev_btn = create_danger_button(
                btn_box,
                text="Olvidar",
                command=lambda t=token, n=name: self._revoke_device(t, n),
                padx=8,
                pady=2
            )
            rev_btn.pack(side="left", padx=3)

    def _disconnect_device(self, token: str):
        session_manager.disconnect_device(token)
        self.refresh_devices()

    def _revoke_device(self, token: str, name: str):
        if messagebox.askyesno("Confirmar", f"¿Deseas desvincular '{name}' permanentemente?\nTendrá que ingresar el PIN nuevamente."):
            session_manager.revoke_device(token)
            self.refresh_devices()

    def _build_diagnostic_card(self):
        diag_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        diag_card.pack(fill="x", pady=(0, 16))

        diag_title = ttk.Label(
            diag_card,
            text="Diagnóstico de Red & Firewall de Windows",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        diag_title.pack(anchor="w", padx=16, pady=(12, 6))

        diag_body = ttk.Frame(diag_card, style="Card.TFrame")
        diag_body.pack(fill="x", padx=16, pady=(0, 12))

        self.port_status_lbl = ttk.Label(
            diag_body,
            text="Comprobando puerto local...",
            font=FONT_BODY,
            foreground=COLOR_TEXT_SECONDARY
        )
        self.port_status_lbl.pack(anchor="w", pady=2)

        self.firewall_status_lbl = ttk.Label(
            diag_body,
            text="Comprobando Firewall de Windows...",
            font=FONT_BODY,
            foreground=COLOR_TEXT_SECONDARY
        )
        self.firewall_status_lbl.pack(anchor="w", pady=2)

        action_row = ttk.Frame(diag_body, style="Card.TFrame")
        action_row.pack(fill="x", pady=(8, 0))

        self.repair_fw_btn = create_accent_button(
            action_row,
            text="Reparar Regla en Firewall de Windows",
            command=self._repair_firewall,
            padx=12,
            pady=5
        )
        self.repair_fw_btn.pack(side="left", padx=(0, 8))

        recheck_btn = create_secondary_button(
            action_row,
            text="Re-analizar Red",
            command=self.run_diagnostics,
            padx=10,
            pady=5
        )
        recheck_btn.pack(side="left")

        self.after(500, self.run_diagnostics)

    def run_diagnostics(self):
        # 1. Comprobar puerto 8899
        port = getattr(config, "port", 8899)
        is_port_ok = network_diagnostic.check_port_listening(port)
        if is_port_ok:
            self.port_status_lbl.configure(
                text=f"Puerto {port} TCP: En escucha activa (Loopback OK)",
                foreground=COLOR_NEON_GREEN
            )
        else:
            self.port_status_lbl.configure(
                text=f"Puerto {port} TCP: No responde en loopback local",
                foreground=COLOR_NEON_ROSE
            )

        # 2. Comprobar regla en Windows Defender Firewall
        fw_info = network_diagnostic.check_windows_firewall_rule()
        if fw_info.get("exists") and fw_info.get("enabled"):
            self.firewall_status_lbl.configure(
                text="Firewall de Windows: Regla de entrada autorizada para Hendrix",
                foreground=COLOR_NEON_GREEN
            )
        elif fw_info.get("exists"):
            self.firewall_status_lbl.configure(
                text="Firewall de Windows: Regla encontrada pero deshabilitada",
                foreground=COLOR_NEON_AMBER
            )
        else:
            self.firewall_status_lbl.configure(
                text="Firewall de Windows: Sin regla de entrada (Podría bloquear conexiones LAN)",
                foreground=COLOR_NEON_ROSE
            )

    def _repair_firewall(self):
        port = getattr(config, "port", 8899)
        self.repair_fw_btn.configure(text="Creando regla con UAC...", state="disabled")
        self.update()

        def _worker():
            success, msg = network_diagnostic.repair_windows_firewall_rule(port)
            def _done():
                self.repair_fw_btn.configure(text="Reparar Regla en Firewall de Windows", state="normal")
                if success:
                    messagebox.showinfo("Firewall Configurado", msg)
                else:
                    messagebox.showwarning("Aviso de Firewall", msg)
                self.run_diagnostics()
            self.after(0, _done)

        import threading
        threading.Thread(target=_worker, daemon=True).start()
