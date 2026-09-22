import os
import time
import tkinter as tk
from tkinter import ttk, filedialog, messagebox
from storage.dropzone_manager import dropzone_manager

class DropzoneFilesView(tk.Frame):
    def __init__(self, parent):
        super().__init__(parent, background="#0B0F19")
        self.selected_file_path = None
        self._build_ui()
        dropzone_manager.add_file_ready_listener(self._on_file_ready_threadsafe)

    def _on_file_ready_threadsafe(self, payload):
        self.after(0, self.refresh_files)

    def _build_ui(self):
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

        # 1. Configuración de Carpeta y Proveedor Cloud
        self._build_path_card()

        # 2. Acciones de Envío hacia el Móvil (Selector y Pegar desde Explorer)
        self._build_actions_card()

        # 3. Lista de Archivos Recientes y Entregables
        self._build_recent_files_card()

    def _build_path_card(self):
        path_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        path_card.pack(fill="x", pady=(0, 12))

        title = ttk.Label(
            path_card,
            text="📁 Buzón Dropzone & Sincronización",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        title.pack(anchor="w", padx=16, pady=(12, 4))

        body = ttk.Frame(path_card, style="Card.TFrame")
        body.pack(fill="x", padx=16, pady=(0, 12))

        self.path_lbl = ttk.Label(
            body,
            text=f"Ruta activa: {dropzone_manager.root_path}",
            font=("Consolas", 9),
            foreground="#94A3B8"
        )
        self.path_lbl.pack(anchor="w", pady=(0, 4))

        self.provider_lbl = ttk.Label(
            body,
            text=f"☁️ Proveedor detectado: {dropzone_manager.cloud_provider} (Nube: {'Sí' if dropzone_manager.is_cloud_synced else 'Local'})",
            font=("Segoe UI", 9, "italic"),
            foreground="#10B981" if dropzone_manager.is_cloud_synced else "#F59E0B"
        )
        self.provider_lbl.pack(anchor="w", pady=(0, 8))

        btn_row = ttk.Frame(body, style="Card.TFrame")
        btn_row.pack(fill="x")

        open_folder_btn = tk.Button(
            btn_row,
            text="📁 Abrir Carpeta en Windows",
            font=("Segoe UI", 8, "bold"),
            bg="#0284C7",
            fg="white",
            relief="flat",
            command=self._open_dropzone_folder,
            padx=10,
            pady=4
        )
        open_folder_btn.pack(side="left", padx=(0, 8))

        change_path_btn = tk.Button(
            btn_row,
            text="⚙️ Cambiar Ubicación...",
            font=("Segoe UI", 8),
            bg="#334155",
            fg="white",
            relief="flat",
            command=self._change_dropzone_path,
            padx=8,
            pady=4
        )
        change_path_btn.pack(side="left")

    def _open_dropzone_folder(self):
        try:
            os.startfile(dropzone_manager.root_path)
        except Exception as e:
            messagebox.showerror("Error", f"No se pudo abrir la carpeta: {e}")

    def _change_dropzone_path(self):
        new_dir = filedialog.askdirectory(
            initialdir=dropzone_manager.root_path,
            title="Selecciona la nueva carpeta raíz para el Dropzone"
        )
        if new_dir:
            ok = dropzone_manager.set_dropzone_path(new_dir)
            if ok:
                self.path_lbl.configure(text=f"Ruta activa: {dropzone_manager.root_path}")
                self.provider_lbl.configure(
                    text=f"☁️ Proveedor detectado: {dropzone_manager.cloud_provider}",
                    foreground="#10B981" if dropzone_manager.is_cloud_synced else "#F59E0B"
                )
                self.refresh_files()
                messagebox.showinfo("Ruta Actualizada", f"Dropzone reconfigurado en:\n{new_dir}")
            else:
                messagebox.showerror("Error", "No se pudo cambiar la ruta del Dropzone.")

    def _build_actions_card(self):
        act_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        act_card.pack(fill="x", pady=(0, 12))

        title = ttk.Label(
            act_card,
            text="📤 Compartir Archivos hacia el Teléfono Móvil",
            font=("Segoe UI", 11, "bold"),
            foreground="#10B981"
        )
        title.pack(anchor="w", padx=16, pady=(12, 6))

        body = ttk.Frame(act_card, style="Card.TFrame")
        body.pack(fill="x", padx=16, pady=(0, 12))

        desc = ttk.Label(
            body,
            text="Los archivos importados se guardan en el Dropzone y disparan una notificación inmediata al celular:",
            font=("Segoe UI", 9),
            foreground="#94A3B8"
        )
        desc.pack(anchor="w", pady=(0, 8))

        btn_row = ttk.Frame(body, style="Card.TFrame")
        btn_row.pack(fill="x", pady=(0, 6))

        browse_btn = tk.Button(
            btn_row,
            text="📂 Seleccionar Archivo(s)...",
            font=("Segoe UI", 9, "bold"),
            bg="#10B981",
            fg="black",
            relief="flat",
            command=self._select_and_import_files,
            padx=12,
            pady=6
        )
        browse_btn.pack(side="left", padx=(0, 8))

        paste_explorer_btn = tk.Button(
            btn_row,
            text="📋 Pegar Archivo Copiado (Explorer)",
            font=("Segoe UI", 9),
            bg="#8B5CF6",
            fg="white",
            relief="flat",
            command=self._paste_clipboard_files,
            padx=12,
            pady=6
        )
        paste_explorer_btn.pack(side="left")

        self.import_status_lbl = ttk.Label(
            body,
            text="",
            font=("Segoe UI", 8, "italic"),
            foreground="#38BDF8"
        )
        self.import_status_lbl.pack(anchor="w", pady=(4, 0))

    def _select_and_import_files(self):
        files = filedialog.askopenfilenames(
            title="Seleccionar archivos para enviar al móvil"
        )
        if files:
            count = 0
            for f in files:
                res = dropzone_manager.import_external_file(f)
                if res:
                    count += 1
            self.refresh_files()
            self.import_status_lbl.configure(text=f"✅ {count} archivo(s) importados y notificados al móvil.")

    def _paste_clipboard_files(self):
        files = dropzone_manager.get_clipboard_files()
        if not files:
            messagebox.showinfo("Portapapeles", "No hay archivos copiados en el portapapeles de Windows.\nCopia un archivo en el Explorador con Ctrl+C y vuelve a presionar este botón.")
            return

        count = 0
        for f in files:
            res = dropzone_manager.import_external_file(f)
            if res:
                count += 1
        self.refresh_files()
        self.import_status_lbl.configure(text=f"✅ {count} archivo(s) pegados desde el portapapeles al Dropzone.")

    def _build_recent_files_card(self):
        rec_card = ttk.Frame(self.scroll_content, style="Card.TFrame")
        rec_card.pack(fill="x", pady=(0, 16))

        header_row = ttk.Frame(rec_card, style="Card.TFrame")
        header_row.pack(fill="x", padx=16, pady=(12, 6))

        title = ttk.Label(
            header_row,
            text="🕒 Entregables & Archivos Recientes",
            font=("Segoe UI", 11, "bold"),
            foreground="#38BDF8"
        )
        title.pack(side="left")

        refresh_btn = tk.Button(
            header_row,
            text="🔄 Actualizar",
            font=("Segoe UI", 8),
            bg="#334155",
            fg="white",
            relief="flat",
            command=self.refresh_files,
            padx=8,
            pady=2
        )
        refresh_btn.pack(side="right")

        list_frame = tk.Frame(rec_card, background="#0F172A")
        list_frame.pack(fill="x", padx=16, pady=(0, 8))

        self.files_listbox = tk.Listbox(
            list_frame,
            height=7,
            font=("Segoe UI", 9),
            bg="#0F172A",
            fg="#E2E8F0",
            selectbackground="#0284C7",
            selectforeground="#FFFFFF",
            relief="flat",
            highlightthickness=0
        )
        self.files_listbox.pack(side="left", fill="both", expand=True, padx=4, pady=4)
        self.files_listbox.bind("<<ListboxSelect>>", self._on_file_select)

        scroll = ttk.Scrollbar(list_frame, orient="vertical", command=self.files_listbox.yview)
        scroll.pack(side="right", fill="y")
        self.files_listbox.config(yscrollcommand=scroll.set)

        btn_row = ttk.Frame(rec_card, style="Card.TFrame")
        btn_row.pack(fill="x", padx=16, pady=(0, 12))

        open_btn = tk.Button(
            btn_row,
            text="Abrir Archivo Seleccionado",
            font=("Segoe UI", 8, "bold"),
            bg="#0284C7",
            fg="white",
            relief="flat",
            command=self._open_selected_file,
            padx=10,
            pady=4
        )
        open_btn.pack(side="left")

        self.refresh_files()

    def refresh_files(self):
        self.recent_files = dropzone_manager.scan_recent_files(limit=25)
        self.files_listbox.delete(0, tk.END)

        if not self.recent_files:
            self.files_listbox.insert(tk.END, "  (Buzón vacío - No se han recibido entregables aún)")
            return

        for f in self.recent_files:
            name = f.get("fileName", "archivo")
            size_kb = f.get("sizeBytes", 0) // 1024
            cat = f.get("category", "general")
            t_str = time.strftime("%d/%m %H:%M", time.localtime(f.get("timestamp", 0) / 1000))
            self.files_listbox.insert(tk.END, f"[{t_str}] ({size_kb} KB) {name} ({cat})")

    def _on_file_select(self, event):
        sel = self.files_listbox.curselection()
        if sel and self.recent_files:
            idx = sel[0]
            if idx < len(self.recent_files):
                f = self.recent_files[idx]
                rel = f.get("relativePath", "")
                full = os.path.join(dropzone_manager.root_path, rel)
                self.selected_file_path = full

    def _open_selected_file(self):
        if self.selected_file_path and os.path.exists(self.selected_file_path):
            try:
                os.startfile(self.selected_file_path)
            except Exception as e:
                messagebox.showerror("Error", f"No se pudo abrir el archivo: {e}")
        else:
            messagebox.showinfo("Selección", "Por favor selecciona un archivo de la lista.")
