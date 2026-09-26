import os
import tkinter as tk
from tkinter import ttk, messagebox
from core.plugin_engine import plugin_engine
from ui.theme import (
    COLOR_VOID_BLACK,
    COLOR_VOID_SURFACE,
    COLOR_VOID_SURFACE_ELEVATED,
    COLOR_VOID_BORDER,
    COLOR_NEON_LILAC,
    COLOR_TEXT_PRIMARY,
    COLOR_TEXT_SECONDARY,
    COLOR_TEXT_MUTED,
    FONT_FAMILY,
    FONT_TITLE,
    FONT_BODY,
    FONT_BODY_BOLD,
    FONT_BODY_ITALIC,
    FONT_CAPTION,
    create_neon_button,
    create_secondary_button,
)

class PluginsManagerView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background=COLOR_VOID_BLACK)
        self._build_ui()
        plugin_engine.on_plugins_changed = self._on_plugins_changed_threadsafe

    def _on_plugins_changed_threadsafe(self):
        self.after(0, self.refresh_plugins_list)

    def _build_ui(self):
        # Header y barra de herramientas
        top_bar = tk.Frame(self, background=COLOR_VOID_BLACK)
        top_bar.pack(fill="x", padx=16, pady=(12, 8))

        title_lbl = tk.Label(
            top_bar,
            text="Plugins y Scripts de Usuario",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC,
            background=COLOR_VOID_BLACK
        )
        title_lbl.pack(side="left")

        btn_box = tk.Frame(top_bar, background=COLOR_VOID_BLACK)
        btn_box.pack(side="right")

        reload_btn = create_neon_button(
            btn_box,
            text="Recargar (Hot Reload)",
            command=self._reload_plugins,
            padx=10,
            pady=3
        )
        reload_btn.pack(side="left", padx=(0, 6))

        open_folder_btn = create_secondary_button(
            btn_box,
            text="Abrir Carpeta",
            command=self._open_plugins_folder,
            padx=8,
            pady=3
        )
        open_folder_btn.pack(side="left")

        # Subtítulo explicativo
        desc_lbl = tk.Label(
            self,
            text="Agrega scripts en 'hendrix-desktop/plugins/' para extender Hendrix con macros y automatizaciones.",
            font=FONT_BODY,
            foreground=COLOR_TEXT_SECONDARY,
            background=COLOR_VOID_BLACK
        )
        desc_lbl.pack(anchor="w", padx=16, pady=(0, 8))

        # Canvas scrollable con tarjetas de plugins
        canvas = tk.Canvas(self, background=COLOR_VOID_BLACK, highlightthickness=0)
        scrollbar = ttk.Scrollbar(self, orient="vertical", command=canvas.yview)
        self.scroll_content = tk.Frame(canvas, background=COLOR_VOID_BLACK)

        self.scroll_content.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        self.canvas_window = canvas.create_window((0, 0), window=self.scroll_content, anchor="nw")
        canvas.bind("<Configure>", lambda e: canvas.itemconfig(self.canvas_window, width=max(400, e.width - 12)))
        canvas.configure(yscrollcommand=scrollbar.set)

        canvas.pack(side="left", fill="both", expand=True, padx=(16, 4), pady=(0, 12))
        scrollbar.pack(side="right", fill="y", pady=(0, 12))

        self.refresh_plugins_list()

    def refresh_plugins_list(self):
        for widget in self.scroll_content.winfo_children():
            widget.destroy()

        plugins = list(plugin_engine.plugins.values())
        if not plugins:
            empty_frame = tk.Frame(
                self.scroll_content,
                background=COLOR_VOID_SURFACE_ELEVATED,
                highlightbackground=COLOR_VOID_BORDER,
                highlightthickness=1,
                padx=16,
                pady=16
            )
            empty_frame.pack(fill="x", pady=12)

            empty_title = tk.Label(
                empty_frame,
                text="No se detectaron plugins en la carpeta 'plugins/'",
                font=FONT_BODY_BOLD,
                foreground=COLOR_TEXT_PRIMARY,
                background=COLOR_VOID_SURFACE_ELEVATED
            )
            empty_title.pack(anchor="w")

            empty_lbl = tk.Label(
                empty_frame,
                text="Agrega scripts en Python (ej. 'plugin_mi_script.py') que hereden de HendrixPlugin para extender el asistente.",
                font=FONT_BODY,
                foreground=COLOR_TEXT_SECONDARY,
                background=COLOR_VOID_SURFACE_ELEVATED
            )
            empty_lbl.pack(anchor="w", pady=(4, 0))
            return

        for plugin in plugins:
            self._render_plugin_card(plugin)

    def _render_plugin_card(self, plugin):
        card = tk.Frame(
            self.scroll_content,
            background=COLOR_VOID_SURFACE_ELEVATED,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        card.pack(fill="x", pady=(0, 10))

        header = tk.Frame(card, background=COLOR_VOID_SURFACE_ELEVATED)
        header.pack(fill="x", padx=14, pady=(10, 4))

        # Icono o emoji representativo
        icon_text = getattr(plugin, "icon_emoji", "") or "⚡"
        icon_lbl = tk.Label(
            header,
            text=icon_text,
            font=(FONT_FAMILY, 14),
            foreground=COLOR_NEON_LILAC,
            background=COLOR_VOID_SURFACE_ELEVATED
        )
        icon_lbl.pack(side="left", padx=(0, 10))

        info_box = tk.Frame(header, background=COLOR_VOID_SURFACE_ELEVATED)
        info_box.pack(side="left", fill="x", expand=True)

        name_row = tk.Frame(info_box, background=COLOR_VOID_SURFACE_ELEVATED)
        name_row.pack(anchor="w", fill="x")

        name_lbl = tk.Label(
            name_row,
            text=plugin.name,
            font=FONT_BODY_BOLD,
            foreground=COLOR_TEXT_PRIMARY,
            background=COLOR_VOID_SURFACE_ELEVATED
        )
        name_lbl.pack(side="left")

        ver_lbl = tk.Label(
            name_row,
            text=f"v{plugin.version}",
            font=FONT_CAPTION,
            foreground=COLOR_NEON_LILAC,
            background=COLOR_VOID_SURFACE,
            padx=5,
            pady=1
        )
        ver_lbl.pack(side="left", padx=(6, 0))

        author_lbl = tk.Label(
            info_box,
            text=f"Por: {plugin.author}  •  Categoría: {plugin.category}",
            font=FONT_CAPTION,
            foreground=COLOR_TEXT_MUTED,
            background=COLOR_VOID_SURFACE_ELEVATED
        )
        author_lbl.pack(anchor="w", pady=(2, 0))

        if plugin.description:
            desc_lbl = tk.Label(
                card,
                text=plugin.description,
                font=FONT_BODY,
                foreground=COLOR_TEXT_SECONDARY,
                background=COLOR_VOID_SURFACE_ELEVATED,
                wraplength=700,
                justify="left"
            )
            desc_lbl.pack(anchor="w", padx=14, pady=(2, 8))

        # Acciones expuestas por el plugin
        actions = plugin.get_actions()
        if actions:
            act_container = ttk.Frame(card, style="Card.TFrame")
            act_container.pack(fill="x", padx=14, pady=(0, 10))

            for act in actions:
                act_id = act.get("id")
                act_name = act.get("name", act_id)

                act_row = tk.Frame(
                    act_container,
                    background=COLOR_VOID_SURFACE,
                    highlightbackground=COLOR_VOID_BORDER,
                    highlightthickness=1
                )
                act_row.pack(fill="x", pady=2)

                act_lbl = tk.Label(
                    act_row,
                    text=act_name,
                    font=FONT_BODY_BOLD,
                    foreground=COLOR_TEXT_PRIMARY,
                    background=COLOR_VOID_SURFACE
                )
                act_lbl.pack(side="left", padx=8, pady=4)

                act_desc = tk.Label(
                    act_row,
                    text=act.get("description", ""),
                    font=FONT_CAPTION,
                    foreground=COLOR_TEXT_MUTED,
                    background=COLOR_VOID_SURFACE
                )
                act_desc.pack(side="left", padx=4)

                run_btn = create_neon_button(
                    act_row,
                    text="Ejecutar",
                    command=lambda p_id=plugin.id, a_id=act_id, a_name=act_name: self._run_action(p_id, a_id, a_name),
                    padx=10,
                    pady=2
                )
                run_btn.pack(side="right", padx=6, pady=3)

    def _run_action(self, plugin_id: str, action_id: str, action_name: str):
        res = plugin_engine.execute_action(plugin_id, action_id)
        if res.get("success"):
            messagebox.showinfo(action_name, f"{res.get('message', 'Éxito')}")
        else:
            messagebox.showerror(action_name, f"Error: {res.get('message', 'Fallo')}")

    def _reload_plugins(self):
        plugins_meta = plugin_engine.reload_plugins()
        self.refresh_plugins_list()
        messagebox.showinfo("Hot Reload", f"Se recargaron {len(plugins_meta)} plugin(s) con éxito.")

    def _open_plugins_folder(self):
        try:
            os.startfile(plugin_engine.plugins_dir)
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo abrir la carpeta: {e}")
