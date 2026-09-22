import os
import tkinter as tk
from tkinter import ttk, messagebox
from core.plugin_engine import plugin_engine

class PluginsManagerView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background="#0B0F19")
        self._build_ui()
        plugin_engine.on_plugins_changed = self._on_plugins_changed_threadsafe

    def _on_plugins_changed_threadsafe(self):
        self.after(0, self.refresh_plugins_list)

    def _build_ui(self):
        # Header y barra de herramientas
        top_bar = tk.Frame(self, background="#0B0F19")
        top_bar.pack(fill="x", padx=16, pady=(12, 8))

        title_lbl = tk.Label(
            top_bar,
            text="🧩 Plugins y Scripts de Usuario",
            font=("Segoe UI", 12, "bold"),
            foreground="#38BDF8",
            background="#0B0F19"
        )
        title_lbl.pack(side="left")

        btn_box = tk.Frame(top_bar, background="#0B0F19")
        btn_box.pack(side="right")

        reload_btn = tk.Button(
            btn_box,
            text="🔄 Recargar (Hot Reload)",
            font=("Segoe UI", 8, "bold"),
            bg="#8B5CF6",
            fg="white",
            relief="flat",
            command=self._reload_plugins,
            padx=8,
            pady=3
        )
        reload_btn.pack(side="left", padx=(0, 6))

        open_folder_btn = tk.Button(
            btn_box,
            text="📁 Abrir Carpeta",
            font=("Segoe UI", 8),
            bg="#334155",
            fg="white",
            relief="flat",
            command=self._open_plugins_folder,
            padx=8,
            pady=3
        )
        open_folder_btn.pack(side="left")

        # Subtítulo explicativo
        desc_lbl = tk.Label(
            self,
            text="Agrega scripts en 'hendrix-desktop/plugins/' para extender Hendrix con macros y automatizaciones.",
            font=("Segoe UI", 9),
            foreground="#94A3B8",
            background="#0B0F19"
        )
        desc_lbl.pack(anchor="w", padx=16, pady=(0, 8))

        # Canvas scrollable con tarjetas de plugins
        canvas = tk.Canvas(self, background="#0B0F19", highlightthickness=0)
        scrollbar = ttk.Scrollbar(self, orient="vertical", command=canvas.yview)
        self.scroll_content = tk.Frame(canvas, background="#0B0F19")

        self.scroll_content.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        canvas.create_window((0, 0), window=self.scroll_content, anchor="nw", width=620)
        canvas.configure(yscrollcommand=scrollbar.set)

        canvas.pack(side="left", fill="both", expand=True, padx=(16, 4), pady=(0, 12))
        scrollbar.pack(side="right", fill="y", pady=(0, 12))

        self.refresh_plugins_list()

    def refresh_plugins_list(self):
        for widget in self.scroll_content.winfo_children():
            widget.destroy()

        plugins = plugin_engine.plugins.values()
        if not plugins:
            empty_lbl = ttk.Label(
                self.scroll_content,
                text="No se detectaron plugins en la carpeta 'plugins/'.\nCrea un archivo 'plugin_*.py' siguiendo la documentación.",
                font=("Segoe UI", 9, "italic"),
                foreground="#94A3B8"
            )
            empty_lbl.pack(anchor="w", pady=12)
            return

        for plugin in plugins:
            self._render_plugin_card(plugin)

    def _render_plugin_card(self, plugin):
        card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        card.pack(fill="x", pady=(0, 10))

        header = ttk.Frame(card, style="Card.TFrame")
        header.pack(fill="x", padx=14, pady=(10, 4))

        icon_lbl = tk.Label(
            header,
            text=plugin.icon_emoji,
            font=("Segoe UI", 16),
            background="#1E293B"
        )
        icon_lbl.pack(side="left", padx=(0, 8))

        info_box = ttk.Frame(header, style="Card.TFrame")
        info_box.pack(side="left", fill="x", expand=True)

        name_lbl = tk.Label(
            info_box,
            text=f"{plugin.name} (v{plugin.version})",
            font=("Segoe UI", 10, "bold"),
            foreground="#FFFFFF",
            background="#1E293B"
        )
        name_lbl.pack(anchor="w")

        author_lbl = tk.Label(
            info_box,
            text=f"Por: {plugin.author} | Categoría: {plugin.category}",
            font=("Segoe UI", 8),
            foreground="#64748B",
            background="#1E293B"
        )
        author_lbl.pack(anchor="w")

        desc_lbl = tk.Label(
            card,
            text=plugin.description,
            font=("Segoe UI", 9),
            foreground="#94A3B8",
            background="#1E293B",
            wraplength=580,
            justify="left"
        )
        desc_lbl.pack(anchor="w", padx=14, pady=(0, 8))

        # Acciones expuestas por el plugin
        actions = plugin.get_actions()
        if actions:
            act_container = ttk.Frame(card, style="Card.TFrame")
            act_container.pack(fill="x", padx=14, pady=(0, 10))

            for act in actions:
                act_id = act.get("id")
                act_name = act.get("name", act_id)
                act_emoji = act.get("icon_emoji", "⚡")

                act_row = tk.Frame(act_container, background="#0F172A", relief="flat")
                act_row.pack(fill="x", pady=2)

                act_lbl = tk.Label(
                    act_row,
                    text=f"{act_emoji} {act_name}",
                    font=("Segoe UI", 9, "bold"),
                    foreground="#E2E8F0",
                    background="#0F172A"
                )
                act_lbl.pack(side="left", padx=8, pady=4)

                act_desc = tk.Label(
                    act_row,
                    text=act.get("description", ""),
                    font=("Segoe UI", 8),
                    foreground="#64748B",
                    background="#0F172A"
                )
                act_desc.pack(side="left", padx=4)

                run_btn = tk.Button(
                    act_row,
                    text="Ejecutar",
                    font=("Segoe UI", 8, "bold"),
                    bg="#0284C7",
                    fg="white",
                    relief="flat",
                    command=lambda p_id=plugin.id, a_id=act_id, a_name=act_name: self._run_action(p_id, a_id, a_name),
                    padx=8,
                    pady=2
                )
                run_btn.pack(side="right", padx=6, pady=3)

    def _run_action(self, plugin_id: str, action_id: str, action_name: str):
        res = plugin_engine.execute_action(plugin_id, action_id)
        if res.get("success"):
            messagebox.showinfo(action_name, f"✅ {res.get('message', 'Éxito')}")
        else:
            messagebox.showerror(action_name, f"❌ Error: {res.get('message', 'Fallo')}")

    def _reload_plugins(self):
        plugins_meta = plugin_engine.reload_plugins()
        self.refresh_plugins_list()
        messagebox.showinfo("Hot Reload", f"Se recargaron {len(plugins_meta)} plugin(s) con éxito.")

    def _open_plugins_folder(self):
        try:
            os.startfile(plugin_engine.plugins_dir)
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo abrir la carpeta: {e}")
