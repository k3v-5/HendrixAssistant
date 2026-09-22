import os
import sys
import json
import time
import string
import threading
from typing import Dict, Any, List, Optional, Callable

CONFIG_FILENAME = "hendrix_config.json"

class DropzoneManager:
    """
    Gestor portátil de buzón de entregables (Dropzone) y sincronización con la nube.
    Autodetecta Google Drive, OneDrive o crea un espacio local organizado para Blender,
    Ableton Live, FL Studio, Adobe y Antigravity.
    """

    SUBDIRS = {
        "blender_renders": os.path.join("Blender", "Renders"),
        "blender_projects": os.path.join("Blender", "Projects"),
        "audio_exports": os.path.join("Audio", "Exports"),
        "video_adobe": os.path.join("Video_Adobe", "Exports"),
        "projects_general": os.path.join("Projects", "General")
    }

    ALLOWED_EXTENSIONS = {
        ".png", ".jpg", ".jpeg", ".exr", ".mp4", ".mov", ".avi",
        ".wav", ".mp3", ".flac", ".ogg", ".blend", ".als", ".flp", ".prproj", ".psd"
    }

    def __init__(self, config_dir: Optional[str] = None):
        self.config_dir = config_dir or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.config_path = os.path.join(self.config_dir, CONFIG_FILENAME)
        self.root_path = ""
        self.cloud_provider = "Local"
        self.is_cloud_synced = False
        self.categories: Dict[str, str] = {}

        self._file_ready_callbacks: List[Callable[[Dict[str, Any]], None]] = []
        self._watcher_thread: Optional[threading.Thread] = None
        self._is_watching = False
        self._known_files: Dict[str, float] = {}

        self.initialize()

    def initialize(self):
        """Inicializa la ruta del Dropzone según configuración o autodetección."""
        custom_path = self._load_custom_config()
        if custom_path and os.path.exists(custom_path):
            self.root_path = custom_path
        else:
            self.root_path, self.cloud_provider, self.is_cloud_synced = self.detect_best_dropzone_path()

        self._ensure_directories()
        self._update_provider_info()
        self._seed_known_files()

    def _load_custom_config(self) -> Optional[str]:
        if os.path.exists(self.config_path):
            try:
                with open(self.config_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    return data.get("dropzone_path")
            except Exception:
                pass
        return None

    def _save_custom_config(self, path: str):
        try:
            data = {}
            if os.path.exists(self.config_path):
                try:
                    with open(self.config_path, "r", encoding="utf-8") as f:
                        data = json.load(f)
                except Exception:
                    data = {}
            data["dropzone_path"] = path
            with open(self.config_path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"[DropzoneManager] Error al guardar config: {e}")

    @classmethod
    def detect_best_dropzone_path(cls) -> tuple[str, str, bool]:
        """
        Inspecciona el sistema en busca de Google Drive, OneDrive o directorio local.
        Retorna: (root_path, cloud_provider_name, is_cloud_synced)
        """
        home = os.path.expanduser("~")

        # 1. Buscar unidades virtuales de Google Drive (G:\, H:\, etc.)
        for letter in string.ascii_uppercase:
            drive_root = f"{letter}:\\"
            if os.path.exists(drive_root):
                for candidate_folder in ["My Drive", "Mi unidad", "Mi Unidad"]:
                    target = os.path.join(drive_root, candidate_folder)
                    if os.path.exists(target):
                        dropzone = os.path.join(target, "HendrixStudio")
                        return dropzone, "Google Drive", True

        # 2. Buscar Google Drive en el perfil de usuario
        for gdrive_local in ["Google Drive", "GoogleDrive"]:
            p = os.path.join(home, gdrive_local)
            if os.path.exists(p):
                return os.path.join(p, "HendrixStudio"), "Google Drive", True

        # 3. Buscar OneDrive
        onedrive = os.environ.get("OneDrive") or os.environ.get("OneDriveConsumer") or os.path.join(home, "OneDrive")
        if onedrive and os.path.exists(onedrive):
            return os.path.join(onedrive, "HendrixStudio"), "OneDrive", True

        # 4. Fallback local agnóstico
        fallback = os.path.join(home, "HendrixStudio", "Dropzone")
        return fallback, "Local", False

    def _update_provider_info(self):
        norm = self.root_path.lower()
        if "google drive" in norm or "mi unidad" in norm or "my drive" in norm:
            self.cloud_provider = "Google Drive"
            self.is_cloud_synced = True
        elif "onedrive" in norm:
            self.cloud_provider = "OneDrive"
            self.is_cloud_synced = True
        else:
            self.cloud_provider = "Local"
            self.is_cloud_synced = False

    def _ensure_directories(self):
        """Crea la estructura de carpetas si no existe."""
        try:
            os.makedirs(self.root_path, exist_ok=True)
            self.categories = {}
            for cat_key, rel_sub in self.SUBDIRS.items():
                full_cat_path = os.path.join(self.root_path, rel_sub)
                os.makedirs(full_cat_path, exist_ok=True)
                self.categories[cat_key] = full_cat_path
        except Exception as e:
            print(f"[DropzoneManager] Error al crear directorios: {e}")

    def set_dropzone_path(self, new_path: str) -> bool:
        """Actualiza la ruta raíz del Dropzone y persiste la elección."""
        try:
            expanded = os.path.abspath(os.path.expanduser(new_path))
            os.makedirs(expanded, exist_ok=True)
            self.root_path = expanded
            self._ensure_directories()
            self._update_provider_info()
            self._save_custom_config(expanded)
            self._seed_known_files()
            return True
        except Exception as e:
            print(f"[DropzoneManager] Error al cambiar ruta de Dropzone: {e}")
            return False

    def get_category_path(self, category_key: str) -> str:
        """Devuelve la ruta absoluta para una categoría específica."""
        return self.categories.get(category_key, self.root_path)

    def scan_recent_files(self, limit: int = 20) -> List[Dict[str, Any]]:
        """Escanea archivos recientes generados en las carpetas de Dropzone."""
        results = []
        if not os.path.exists(self.root_path):
            return results

        for cat_key, cat_path in self.categories.items():
            if not os.path.exists(cat_path):
                continue
            try:
                for root, _, files in os.walk(cat_path):
                    for f in files:
                        _, ext = os.path.splitext(f)
                        if ext.lower() in self.ALLOWED_EXTENSIONS:
                            full_file_path = os.path.join(root, f)
                            try:
                                stat = os.stat(full_file_path)
                                rel_path = os.path.relpath(full_file_path, self.root_path)
                                results.append({
                                    "fileName": f,
                                    "category": cat_key,
                                    "sizeBytes": stat.st_size,
                                    "relativePath": rel_path,
                                    "timestamp": int(stat.st_mtime * 1000)
                                })
                            except Exception:
                                pass
            except Exception:
                pass

        results.sort(key=lambda x: x["timestamp"], reverse=True)
        return results[:limit]

    def get_status_info(self) -> Dict[str, Any]:
        """Devuelve el estado estructurado del Dropzone para la app móvil."""
        return {
            "rootPath": self.root_path,
            "cloudProvider": self.cloud_provider,
            "isCloudSynced": self.is_cloud_synced,
            "categories": self.categories,
            "recentFiles": self.scan_recent_files(limit=15)
        }

    def _seed_known_files(self):
        """Indexa archivos existentes para no disparar alertas de archivos viejos."""
        self._known_files.clear()
        if not os.path.exists(self.root_path):
            return
        for root, _, files in os.walk(self.root_path):
            for f in files:
                p = os.path.join(root, f)
                try:
                    self._known_files[p] = os.path.getmtime(p)
                except Exception:
                    pass

    def add_file_ready_listener(self, callback: Callable[[Dict[str, Any]], None]):
        """Registra un callback para notificar cuando un nuevo archivo esté listo."""
        if callback not in self._file_ready_callbacks:
            self._file_ready_callbacks.append(callback)

    def start_watcher(self, interval_seconds: float = 2.0):
        """Inicia el observador en segundo plano."""
        if self._is_watching:
            return
        self._is_watching = True
        self._watcher_thread = threading.Thread(target=self._watch_loop, args=(interval_seconds,), daemon=True)
        self._watcher_thread.start()

    def stop_watcher(self):
        self._is_watching = False

    def _watch_loop(self, interval: float):
        """Ciclo del hilo vigilante de archivos."""
        while self._is_watching:
            time.sleep(interval)
            try:
                self._check_for_new_files()
            except Exception:
                pass

    def _check_for_new_files(self):
        if not os.path.exists(self.root_path):
            return

        current_files: Dict[str, float] = {}
        newly_found: List[str] = []

        for cat_key, cat_path in self.categories.items():
            if not os.path.exists(cat_path):
                continue
            for root, _, files in os.walk(cat_path):
                for f in files:
                    _, ext = os.path.splitext(f)
                    if ext.lower() in self.ALLOWED_EXTENSIONS:
                        full_path = os.path.join(root, f)
                        try:
                            mtime = os.path.getmtime(full_path)
                            current_files[full_path] = mtime
                            if full_path not in self._known_files:
                                newly_found.append(full_path)
                            elif mtime > self._known_files[full_path] + 1.0:
                                newly_found.append(full_path)
                        except Exception:
                            pass

        self._known_files.update(current_files)

        for file_path in newly_found:
            # Esperar a que el archivo termine de escribirse (tamaño estable)
            self._wait_and_notify_file(file_path)

    def _wait_and_notify_file(self, file_path: str):
        def _worker():
            try:
                prev_size = -1
                for _ in range(5):
                    if not os.path.exists(file_path):
                        return
                    cur_size = os.path.getsize(file_path)
                    if cur_size > 0 and cur_size == prev_size:
                        break
                    prev_size = cur_size
                    time.sleep(0.5)

                stat = os.stat(file_path)
                f_name = os.path.basename(file_path)
                rel_path = os.path.relpath(file_path, self.root_path)

                cat = "projects_general"
                for c_key, c_path in self.categories.items():
                    if file_path.startswith(c_path):
                        cat = c_key
                        break

                payload = {
                    "fileName": f_name,
                    "category": cat,
                    "sizeBytes": stat.st_size,
                    "relativePath": rel_path,
                    "timestamp": int(stat.st_mtime * 1000)
                }

                for cb in self._file_ready_callbacks:
                    try:
                        cb(payload)
                    except Exception as e:
                        print(f"[DropzoneManager] Error en callback: {e}")
            except Exception:
                pass

        threading.Thread(target=_worker, daemon=True).start()

    def import_external_file(self, source_path: str, target_category: str = "projects_general") -> dict | None:
        """
        Copia un archivo externo hacia el buzón Dropzone de Hendrix
        y dispara inmediatamente la notificación hacia la app móvil.
        """
        if not os.path.exists(source_path) or not os.path.isfile(source_path):
            return None

        ext = os.path.splitext(source_path)[1].lower()
        cat_folder = self.categories.get("projects_general")
        resolved_category = "projects_general"

        if ext in [".wav", ".mp3", ".flac", ".aif", ".m4a", ".aac"]:
            cat_folder = self.categories.get("audio_mixes", cat_folder)
            resolved_category = "audio_mixes"
        elif ext in [".mp4", ".mov", ".avi", ".mkv"]:
            cat_folder = self.categories.get("video_renders", cat_folder)
            resolved_category = "video_renders"
        elif ext in [".png", ".jpg", ".jpeg", ".webp", ".psd"]:
            cat_folder = self.categories.get("images_covers", cat_folder)
            resolved_category = "images_covers"

        dest_dir = cat_folder or self.root_path
        os.makedirs(dest_dir, exist_ok=True)

        f_name = os.path.basename(source_path)
        dest_path = os.path.join(dest_dir, f_name)

        base_name, file_ext = os.path.splitext(f_name)
        counter = 1
        while os.path.exists(dest_path):
            dest_path = os.path.join(dest_dir, f"{base_name}_{counter}{file_ext}")
            counter += 1

        import shutil
        shutil.copy2(source_path, dest_path)

        stat = os.stat(dest_path)
        final_name = os.path.basename(dest_path)
        rel_path = os.path.relpath(dest_path, self.root_path)

        payload = {
            "fileName": final_name,
            "category": resolved_category,
            "sizeBytes": stat.st_size,
            "relativePath": rel_path,
            "timestamp": int(stat.st_mtime * 1000)
        }

        for cb in self._file_ready_callbacks:
            try:
                cb(payload)
            except Exception as e:
                print(f"[DropzoneManager] Error en callback: {e}")

        return payload

    def get_clipboard_files(self) -> list[str]:
        """Extrae la lista de rutas de archivos copiados en el portapapeles de Windows (CF_HDROP)."""
        file_paths = []
        try:
            import ctypes
            user32 = ctypes.windll.user32
            shell32 = ctypes.windll.shell32
            CF_HDROP = 15

            if not user32.OpenClipboard(None):
                return []

            try:
                h_drop = user32.GetClipboardData(CF_HDROP)
                if not h_drop:
                    return []

                count = shell32.DragQueryFileW(h_drop, 0xFFFFFFFF, None, 0)
                for i in range(count):
                    buf = ctypes.create_unicode_buffer(512)
                    shell32.DragQueryFileW(h_drop, i, buf, 512)
                    path = buf.value
                    if os.path.exists(path):
                        file_paths.append(path)
            finally:
                user32.CloseClipboard()
        except Exception as e:
            print(f"[DropzoneManager] Error leyendo portapapeles CF_HDROP: {e}")
        return file_paths


dropzone_manager = DropzoneManager()

