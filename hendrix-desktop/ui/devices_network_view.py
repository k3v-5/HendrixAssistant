import tkinter as tk
from tkinter import ttk, messagebox
from config import config
from core.session_manager import session_manager
from core.network_diagnostic import network_diagnostic
from core.tunnel_manager import tunnel_manager
from ui.pairing_card import PairingCard
import time

class DevicesNetworkView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background="#0B0F19")
        self._build_ui()
        session_manager.on_devices_updated = self._refresh_devices_threadsafe

    def _refresh_devices_threadsafe(self):
        self.after(0, self.refresh_devices)

    def _build_ui(self):
        # Canvas scrollable para que todo quepa cómodamente
        canvas = tk.Canvas(self, background="#0B0F19", highlightthickness=0)
        scrollbar = ttk.Scrollbar(self, orient="vertical", command=canvas.yview)
        self.scroll_content = tk.Frame(canvas, background="#0B0F19")

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
            text="🌐 Acceso Remoto Global (WAN / Fuera de Casa)",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        wan_title.pack(anchor="w", padx=16, pady=(12, 4))

        wan_body = ttk.Frame(wan_card, style="Card.TFrame")
        wan_body.pack(fill="x", padx=16, pady=(0, 12))

        wan_desc = ttk.Label(
            wan_body,
            text="Conecta tu dispositivo móvil fuera del hogar mediante túnel seguro:",
            font=("Segoe UI", 9),
            foreground="#94A3B8"
        )
        wan_desc.pack(anchor="w", pady=(0, 6))

        wan_input_row = ttk.Frame(wan_body, style="Card.TFrame")
        wan_input_row.pack(fill="x", pady=2)

        self.tunnel_entry_var = tk.StringVar(value=getattr(config, "remote_tunnel_url", "") or "")
        self.tunnel_entry = tk.Entry(
            wan_input_row,
            textvariable=self.tunnel_entry_var,
            font=("Segoe UI", 9),
            bg="#0F172A",
            fg="#38BDF8",
            insertbackground="#38BDF8",
            relief="flat"
        )
        self.tunnel_entry.pack(side="left", fill="x", expand=True, padx=(0, 8), ipady=4)

        save_tunnel_btn = tk.Button(
            wan_input_row,
            text="Guardar URL",
            font=("Segoe UI", 8, "bold"),
            bg="#0284C7",
            fg="white",
            relief="flat",
            command=self._save_tunnel_url,
            padx=10,
            pady=3
        )
        save_tunnel_btn.pack(side="right")

        self.wan_status_lbl = ttk.Label(
            wan_body,
            text="⚪ Modo: Red Local WiFi únicamente" if not getattr(config, "remote_tunnel_url", None) else f"🟢 Túnel configurado: {config.remote_tunnel_url}",
            font=("Segoe UI", 9, "italic"),
            foreground="#10B981" if getattr(config, "remote_tunnel_url", None) else "#94A3B8"
        )
        self.wan_status_lbl.pack(anchor="w", pady=(4, 0))

    def _save_tunnel_url(self):
        new_url = self.tunnel_entry_var.get().strip()
        tunnel_manager.set_custom_tunnel_url(new_url)
        if new_url:
            self.wan_status_lbl.configure(text=f"🟢 Túnel configurado: {new_url}", foreground="#10B981")
        else:
            self.wan_status_lbl.configure(text="⚪ Modo: Red Local WiFi únicamente", foreground="#94A3B8")

    def _build_devices_card(self):
        dev_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        dev_card.pack(fill="x", pady=(0, 12))

        header_row = ttk.Frame(dev_card, style="Card.TFrame")
        header_row.pack(fill="x", padx=16, pady=(12, 6))

        dev_title = ttk.Label(
            header_row,
            text="📱 Dispositivos Móviles Emparejados",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        dev_title.pack(side="left")

        refresh_btn = tk.Button(
            header_row,
            text="🔄 Actualizar",
            font=("Segoe UI", 8),
            bg="#334155",
            fg="white",
            relief="flat",
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
                font=("Segoe UI", 9, "italic"),
                foreground="#94A3B8"
            )
            lbl.pack(anchor="w", pady=6)
            return

        for dev in devices:
            token = dev.get("token")
            is_online = dev.get("is_online", False)
            name = dev.get("name", "Móvil")
            ip = dev.get("ip", "Desconocida")

            row = tk.Frame(self.devices_container, background="#0F172A", relief="flat")
            row.pack(fill="x", pady=3, padx=0)

            status_icon = "🟢" if is_online else "⚪"
            status_text = "En línea" if is_online else "Desconectado"

            info_box = tk.Frame(row, background="#0F172A")
            info_box.pack(side="left", fill="x", expand=True, padx=10, pady=8)

            title_lbl = tk.Label(
                info_box,
                text=f"{status_icon} {name}",
                font=("Segoe UI", 9, "bold"),
                foreground="#FFFFFF" if is_online else "#94A3B8",
                background="#0F172A"
            )
            title_lbl.pack(anchor="w")

            detail_lbl = tk.Label(
                info_box,
                text=f"IP: {ip} | Estado: {status_text} | Token: {token[:8]}...",
                font=("Segoe UI", 8),
                foreground="#64748B",
                background="#0F172A"
            )
            detail_lbl.pack(anchor="w")

            btn_box = tk.Frame(row, background="#0F172A")
            btn_box.pack(side="right", padx=10)

            if is_online:
                disc_btn = tk.Button(
                    btn_box,
                    text="Desconectar",
                    font=("Segoe UI", 8),
                    bg="#D97706",
                    fg="white",
                    relief="flat",
                    command=lambda t=token: self._disconnect_device(t),
                    padx=6,
                    pady=2
                )
                disc_btn.pack(side="left", padx=3)

            rev_btn = tk.Button(
                btn_box,
                text="Olvidar",
                font=("Segoe UI", 8),
                bg="#EF4444",
                fg="white",
                relief="flat",
                command=lambda t=token, n=name: self._revoke_device(t, n),
                padx=6,
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
            text="🛡️ Diagnóstico de Red & Firewall de Windows",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        diag_title.pack(anchor="w", padx=16, pady=(12, 6))

        diag_body = ttk.Frame(diag_card, style="Card.TFrame")
        diag_body.pack(fill="x", padx=16, pady=(0, 12))

        self.port_status_lbl = ttk.Label(
            diag_body,
            text="⏳ Comprobando puerto local...",
            font=("Segoe UI", 9),
            foreground="#E2E8F0"
        )
        self.port_status_lbl.pack(anchor="w", pady=2)

        self.firewall_status_lbl = ttk.Label(
            diag_body,
            text="⏳ Comprobando Firewall de Windows...",
            font=("Segoe UI", 9),
            foreground="#E2E8F0"
        )
        self.firewall_status_lbl.pack(anchor="w", pady=2)

        action_row = ttk.Frame(diag_body, style="Card.TFrame")
        action_row.pack(fill="x", pady=(8, 0))

        self.repair_fw_btn = tk.Button(
            action_row,
            text="🔧 Reparar / Abrir Regla en Firewall de Windows",
            font=("Segoe UI", 9, "bold"),
            bg="#8B5CF6",
            fg="white",
            relief="flat",
            command=self._repair_firewall,
            padx=12,
            pady=5
        )
        self.repair_fw_btn.pack(side="left", padx=(0, 8))

        recheck_btn = tk.Button(
            action_row,
            text="🔄 Re-analizar Red",
            font=("Segoe UI", 8),
            bg="#334155",
            fg="white",
            relief="flat",
            command=self.run_diagnostics,
            padx=8,
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
                text=f"🟢 Puerto {port} TCP: En escucha activa (Loopback OK)",
                foreground="#10B981"
            )
        else:
            self.port_status_lbl.configure(
                text=f"🔴 Puerto {port} TCP: No responde en loopback local",
                foreground="#EF4444"
            )

        # 2. Comprobar regla en Windows Defender Firewall
        fw_info = network_diagnostic.check_windows_firewall_rule()
        if fw_info.get("exists") and fw_info.get("enabled"):
            self.firewall_status_lbl.configure(
                text="🟢 Firewall de Windows: Regla de entrada autorizada para Hendrix",
                foreground="#10B981"
            )
        elif fw_info.get("exists"):
            self.firewall_status_lbl.configure(
                text="🟡 Firewall de Windows: Regla encontrada pero deshabilitada",
                foreground="#F59E0B"
            )
        else:
            self.firewall_status_lbl.configure(
                text="⚠️ Firewall de Windows: Sin regla de entrada (Podría bloquear conexiones LAN)",
                foreground="#EF4444"
            )

    def _repair_firewall(self):
        port = getattr(config, "port", 8899)
        self.repair_fw_btn.configure(text="⏳ Creando regla con UAC...", state="disabled")
        self.update()

        def _worker():
            success, msg = network_diagnostic.repair_windows_firewall_rule(port)
            def _done():
                self.repair_fw_btn.configure(text="🔧 Reparar / Abrir Regla en Firewall de Windows", state="normal")
                if success:
                    messagebox.showinfo("Firewall Configurado", msg)
                else:
                    messagebox.showwarning("Aviso de Firewall", msg)
                self.run_diagnostics()
            self.after(0, _done)

        import threading
        threading.Thread(target=_worker, daemon=True).start()
