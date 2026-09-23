import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from storage.clipboard_hub import clipboard_hub
from ui.theme import (
    COLOR_VOID_BLACK,
    COLOR_VOID_SURFACE,
    COLOR_VOID_BORDER,
    COLOR_NEON_VIOLET,
    COLOR_NEON_PURPLE,
    COLOR_NEON_LILAC,
    COLOR_NEON_CYAN,
    COLOR_TEXT_PRIMARY,
    COLOR_TEXT_SECONDARY,
    FONT_TITLE,
    FONT_BODY,
    FONT_CAPTION,
    FONT_TERMINAL,
    create_neon_button,
    create_secondary_button,
    create_accent_button,
    create_danger_button,
)
import time

class ClipboardSnippetsView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background=COLOR_VOID_BLACK)
        self.selected_history_item = None
        self.selected_snippet = None
        self._build_ui()

        clipboard_hub.on_history_updated = self._on_history_updated_threadsafe
        clipboard_hub.on_snippets_updated = self._on_snippets_updated_threadsafe

    def _on_history_updated_threadsafe(self):
        self.after(0, self.refresh_history)

    def _on_snippets_updated_threadsafe(self):
        self.after(0, self.refresh_snippets)

    def _build_ui(self):
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

        # 1. Sección: Historial de Portapapeles
        self._build_history_card()

        # 2. Sección: Biblioteca de Snippets
        self._build_snippets_card()

    def _build_history_card(self):
        hist_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        hist_card.pack(fill="x", pady=(0, 12))

        header_row = ttk.Frame(hist_card, style="Card.TFrame")
        header_row.pack(fill="x", padx=16, pady=(12, 6))

        title = ttk.Label(
            header_row,
            text="Historial de Portapapeles en Tiempo Real",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        title.pack(side="left")

        clear_btn = create_secondary_button(
            header_row,
            text="Limpiar",
            command=self._clear_history,
            padx=8,
            pady=2
        )
        clear_btn.pack(side="right")

        # Lista de elementos recientes
        list_frame = tk.Frame(
            hist_card,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        list_frame.pack(fill="x", padx=16, pady=(0, 8))

        self.history_listbox = tk.Listbox(
            list_frame,
            height=6,
            font=FONT_TERMINAL,
            bg=COLOR_VOID_SURFACE,
            fg=COLOR_TEXT_PRIMARY,
            selectbackground=COLOR_NEON_VIOLET,
            selectforeground=COLOR_TEXT_PRIMARY,
            relief="flat",
            highlightthickness=0
        )
        self.history_listbox.pack(side="left", fill="both", expand=True, padx=4, pady=4)
        self.history_listbox.bind("<<ListboxSelect>>", self._on_history_select)

        hist_scroll = ttk.Scrollbar(list_frame, orient="vertical", command=self.history_listbox.yview)
        hist_scroll.pack(side="right", fill="y")
        self.history_listbox.config(yscrollcommand=hist_scroll.set)

        # Vista previa del texto
        preview_container = tk.Frame(
            hist_card,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        preview_container.pack(fill="x", padx=16, pady=(0, 8))

        self.history_preview = tk.Text(
            preview_container,
            height=3,
            font=FONT_TERMINAL,
            bg=COLOR_VOID_SURFACE,
            fg=COLOR_NEON_CYAN,
            relief="flat",
            wrap="word",
            padx=6,
            pady=4
        )
        self.history_preview.pack(fill="x")

        # Botonera de acciones
        btn_row = ttk.Frame(hist_card, style="Card.TFrame")
        btn_row.pack(fill="x", padx=16, pady=(0, 12))

        copy_btn = create_neon_button(
            btn_row,
            text="Copiar al Portapapeles",
            command=self._copy_history_item,
            padx=10,
            pady=4
        )
        copy_btn.pack(side="left", padx=(0, 6))

        push_btn = create_accent_button(
            btn_row,
            text="Enviar al Móvil (Push)",
            command=self._push_history_item,
            padx=10,
            pady=4
        )
        push_btn.pack(side="left", padx=(0, 6))

        save_snip_btn = create_secondary_button(
            btn_row,
            text="Guardar como Snippet",
            command=self._save_history_as_snippet,
            padx=8,
            pady=4
        )
        save_snip_btn.pack(side="left")

        self.refresh_history()

    def refresh_history(self):
        self.history_listbox.delete(0, tk.END)
        for item in clipboard_hub.history:
            prev = item.get("preview", "")
            t_str = time.strftime("%H:%M:%S", time.localtime(item.get("timestamp", time.time())))
            chars = item.get("char_count", 0)
            self.history_listbox.insert(tk.END, f"[{t_str}] ({chars} car.) {prev}")

    def _on_history_select(self, event):
        sel = self.history_listbox.curselection()
        if sel:
            idx = sel[0]
            if idx < len(clipboard_hub.history):
                self.selected_history_item = clipboard_hub.history[idx]
                self.history_preview.delete("1.0", tk.END)
                self.history_preview.insert(tk.END, self.selected_history_item.get("text", ""))

    def _copy_history_item(self):
        if self.selected_history_item:
            clipboard_hub.copy_to_clipboard(self.selected_history_item.get("text", ""))
            messagebox.showinfo("Copiado", "Texto restablecido al portapapeles de Windows.")

    def _push_history_item(self):
        if self.selected_history_item:
            text = self.selected_history_item.get("text", "")
            clipboard_hub.broadcast_to_mobile(text)
            messagebox.showinfo("Enviado", f"Enviando texto ({len(text)} caracteres) hacia los teléfonos conectados.")

    def _save_history_as_snippet(self):
        if not self.selected_history_item:
            return
        text = self.selected_history_item.get("text", "")
        title = simpledialog.askstring("Nuevo Snippet", "Título o nombre para este snippet:", parent=self)
        if title:
            category = simpledialog.askstring("Categoría", "Categoría (ej: Git, Código, Notas):", initialvalue="General", parent=self)
            clipboard_hub.add_snippet(title=title, content=text, category=category or "General")
            messagebox.showinfo("Guardado", f"Snippet '{title}' guardado con éxito.")

    def _clear_history(self):
        if messagebox.askyesno("Limpiar Historial", "¿Deseas vaciar el historial de portapapeles?"):
            clipboard_hub.clear_history()
            self.history_preview.delete("1.0", tk.END)
            self.selected_history_item = None

    # --- Sección Snippets ---
    def _build_snippets_card(self):
        snip_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        snip_card.pack(fill="x", pady=(0, 16))

        header_row = ttk.Frame(snip_card, style="Card.TFrame")
        header_row.pack(fill="x", padx=16, pady=(12, 6))

        title = ttk.Label(
            header_row,
            text="Biblioteca de Snippets & Plantillas",
            font=FONT_TITLE,
            foreground=COLOR_NEON_LILAC
        )
        title.pack(side="left")

        new_btn = create_neon_button(
            header_row,
            text="Nuevo Snippet",
            command=self._create_new_snippet,
            padx=10,
            pady=3
        )
        new_btn.pack(side="right")

        # Filtro de categorías
        filter_row = ttk.Frame(snip_card, style="Card.TFrame")
        filter_row.pack(fill="x", padx=16, pady=(0, 8))

        ttk.Label(filter_row, text="Categoría:", font=FONT_CAPTION, foreground=COLOR_TEXT_SECONDARY).pack(side="left", padx=(0, 6))
        self.category_var = tk.StringVar(value="Todos")
        self.category_combo = ttk.Combobox(
            filter_row,
            textvariable=self.category_var,
            state="readonly",
            font=FONT_BODY,
            width=20
        )
        self.category_combo.pack(side="left")
        self.category_combo.bind("<<ComboboxSelected>>", lambda e: self.refresh_snippets())

        # Lista de snippets
        snip_frame = tk.Frame(
            snip_card,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        snip_frame.pack(fill="x", padx=16, pady=(0, 8))

        self.snippets_listbox = tk.Listbox(
            snip_frame,
            height=6,
            font=FONT_BODY,
            bg=COLOR_VOID_SURFACE,
            fg=COLOR_TEXT_PRIMARY,
            selectbackground=COLOR_NEON_VIOLET,
            selectforeground=COLOR_TEXT_PRIMARY,
            relief="flat",
            highlightthickness=0
        )
        self.snippets_listbox.pack(side="left", fill="both", expand=True, padx=4, pady=4)
        self.snippets_listbox.bind("<<ListboxSelect>>", self._on_snippet_select)

        snip_scroll = ttk.Scrollbar(snip_frame, orient="vertical", command=self.snippets_listbox.yview)
        snip_scroll.pack(side="right", fill="y")
        self.snippets_listbox.config(yscrollcommand=snip_scroll.set)

        # Vista previa del contenido del snippet
        preview_container = tk.Frame(
            snip_card,
            background=COLOR_VOID_SURFACE,
            highlightbackground=COLOR_VOID_BORDER,
            highlightthickness=1
        )
        preview_container.pack(fill="x", padx=16, pady=(0, 8))

        self.snippet_preview = tk.Text(
            preview_container,
            height=3,
            font=FONT_TERMINAL,
            bg=COLOR_VOID_SURFACE,
            fg=COLOR_NEON_LILAC,
            relief="flat",
            wrap="word",
            padx=6,
            pady=4
        )
        self.snippet_preview.pack(fill="x")

        # Botonera de acciones
        snip_btn_row = ttk.Frame(snip_card, style="Card.TFrame")
        snip_btn_row.pack(fill="x", padx=16, pady=(0, 12))

        copy_snip_btn = create_neon_button(
            snip_btn_row,
            text="Copiar Snippet",
            command=self._copy_snippet,
            padx=10,
            pady=4
        )
        copy_snip_btn.pack(side="left", padx=(0, 6))

        push_snip_btn = create_accent_button(
            snip_btn_row,
            text="Enviar a Móvil",
            command=self._push_snippet,
            padx=10,
            pady=4
        )
        push_snip_btn.pack(side="left", padx=(0, 6))

        del_snip_btn = create_danger_button(
            snip_btn_row,
            text="Eliminar",
            command=self._delete_snippet,
            padx=10,
            pady=4
        )
        del_snip_btn.pack(side="right")

        self.refresh_snippets()

    def refresh_snippets(self):
        cats = clipboard_hub.get_categories()
        self.category_combo["values"] = cats
        if self.category_var.get() not in cats:
            self.category_var.set("Todos")

        cat = self.category_var.get()
        self.displayed_snippets = clipboard_hub.get_all_snippets(cat)

        self.snippets_listbox.delete(0, tk.END)
        for s in self.displayed_snippets:
            self.snippets_listbox.insert(tk.END, f"[{s.get('category', 'General')}] {s.get('title', '')}")

    def _on_snippet_select(self, event):
        sel = self.snippets_listbox.curselection()
        if sel:
            idx = sel[0]
            if idx < len(self.displayed_snippets):
                self.selected_snippet = self.displayed_snippets[idx]
                self.snippet_preview.delete("1.0", tk.END)
                self.snippet_preview.insert(tk.END, self.selected_snippet.get("content", ""))

    def _copy_snippet(self):
        if self.selected_snippet:
            clipboard_hub.copy_to_clipboard(self.selected_snippet.get("content", ""))
            messagebox.showinfo("Copiado", f"Snippet '{self.selected_snippet.get('title')}' copiado al portapapeles.")

    def _push_snippet(self):
        if self.selected_snippet:
            content = self.selected_snippet.get("content", "")
            clipboard_hub.broadcast_to_mobile(content)
            messagebox.showinfo("Enviado", f"Snippet enviado a los móviles conectados.")

    def _delete_snippet(self):
        if self.selected_snippet:
            title = self.selected_snippet.get("title", "")
            if messagebox.askyesno("Eliminar Snippet", f"¿Deseas eliminar el snippet '{title}'?"):
                clipboard_hub.delete_snippet(self.selected_snippet.get("id"))
                self.snippet_preview.delete("1.0", tk.END)
                self.selected_snippet = None

    def _create_new_snippet(self):
        title = simpledialog.askstring("Nuevo Snippet", "Título descriptivo:", parent=self)
        if not title:
            return
        category = simpledialog.askstring("Categoría", "Categoría (ej: Git, Python, ADB):", initialvalue="General", parent=self)
        content = simpledialog.askstring("Contenido", "Texto o comando del snippet:", parent=self)
        if content:
            clipboard_hub.add_snippet(title=title, content=content, category=category or "General")
