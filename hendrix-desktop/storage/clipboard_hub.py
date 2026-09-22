import os
import json
import time
import uuid
import hashlib
import threading
import logging
import pyperclip

logger = logging.getLogger("hendrix_desktop.clipboard_hub")

HISTORY_FILE = os.path.join(os.path.dirname(__file__), "hendrix_clipboard_history.json")
SNIPPETS_FILE = os.path.join(os.path.dirname(__file__), "hendrix_snippets.json")

DEFAULT_SNIPPETS = [
    {
        "id": "snip_git_status",
        "title": "Git: Estado del Repo",
        "category": "Git & Terminal",
        "content": "git status -s"
    },
    {
        "id": "snip_git_pull",
        "title": "Git: Pull con Rebase",
        "category": "Git & Terminal",
        "content": "git pull --rebase"
    },
    {
        "id": "snip_py_venv",
        "title": "Python: Crear Entorno Virtual",
        "category": "Python & Dev",
        "content": "python -m venv .venv && .venv\\Scripts\\activate"
    },
    {
        "id": "snip_adb_devices",
        "title": "ADB: Listar Dispositivos",
        "category": "Android & ADB",
        "content": "adb devices -l"
    },
    {
        "id": "snip_ai_prompt",
        "title": "Prompt IA: Arquitectura Limpia",
        "category": "Prompts IA",
        "content": "Revisa este código aplicando principios SOLID, OCP y arquitectura por capas desacopladas:"
    }
]

class ClipboardHub:
    def __init__(self, max_history: int = 40):
        self.max_history = max_history
        self.history: list[dict] = []
        self.snippets: list[dict] = []
        self._last_copied_text: str = ""
        self._monitoring = False
        self._monitor_thread = None

        # Callbacks para la GUI y la red
        self.on_history_updated = None
        self.on_snippets_updated = None
        self.broadcast_callback = None

        self._load_storage()
        self.start_monitoring()

    def _load_storage(self):
        # Cargar historial
        if os.path.exists(HISTORY_FILE):
            try:
                with open(HISTORY_FILE, "r", encoding="utf-8") as f:
                    self.history = json.load(f)
            except Exception as e:
                logger.warning(f"Error cargando historial de portapapeles: {e}")
                self.history = []

        # Cargar snippets
        if os.path.exists(SNIPPETS_FILE):
            try:
                with open(SNIPPETS_FILE, "r", encoding="utf-8") as f:
                    self.snippets = json.load(f)
            except Exception as e:
                logger.warning(f"Error cargando snippets: {e}")
                self.snippets = list(DEFAULT_SNIPPETS)
        else:
            self.snippets = list(DEFAULT_SNIPPETS)
            self._save_snippets()

    def _save_history(self):
        try:
            with open(HISTORY_FILE, "w", encoding="utf-8") as f:
                json.dump(self.history[:self.max_history], f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.warning(f"Error guardando historial: {e}")

    def _save_snippets(self):
        try:
            with open(SNIPPETS_FILE, "w", encoding="utf-8") as f:
                json.dump(self.snippets, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.warning(f"Error guardando snippets: {e}")

    def start_monitoring(self):
        """Inicia el hilo centinela de monitoreo del portapapeles de Windows."""
        if self._monitoring:
            return
        self._monitoring = True

        try:
            current = pyperclip.paste()
            if current:
                self._last_copied_text = current
        except Exception:
            pass

        self._monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._monitor_thread.start()

    def _monitor_loop(self):
        while self._monitoring:
            try:
                text = pyperclip.paste()
                if text and text != self._last_copied_text:
                    stripped = text.strip()
                    if stripped and stripped != self._last_copied_text.strip():
                        self._last_copied_text = text
                        self.push_history_item(text)
            except Exception:
                pass
            time.sleep(0.5)

    def push_history_item(self, text: str):
        """Registra un nuevo item en el historial de portapapeles."""
        sha = hashlib.sha256(text.encode("utf-8")).hexdigest()
        # Evitar duplicados consecutivos
        if self.history and self.history[0].get("sha256") == sha:
            return

        preview = text.strip().replace("\n", " ")
        if len(preview) > 80:
            preview = preview[:77] + "..."

        item = {
            "id": str(uuid.uuid4())[:8],
            "text": text,
            "preview": preview,
            "char_count": len(text),
            "timestamp": time.time(),
            "sha256": sha
        }

        self.history.insert(0, item)
        if len(self.history) > self.max_history:
            self.history = self.history[:self.max_history]

        self._save_history()

        if self.on_history_updated:
            self.on_history_updated()

    def copy_to_clipboard(self, text: str):
        """Copia el texto al portapapeles de Windows."""
        self._last_copied_text = text
        pyperclip.copy(text)
        self.push_history_item(text)

    def clear_history(self):
        """Limpia todo el historial de portapapeles."""
        self.history.clear()
        self._save_history()
        if self.on_history_updated:
            self.on_history_updated()

    # --- Gestión de Snippets ---
    def get_all_snippets(self, category: str | None = None) -> list[dict]:
        if not category or category == "Todos":
            return list(self.snippets)
        return [s for s in self.snippets if s.get("category") == category]

    def get_categories(self) -> list[str]:
        cats = sorted(list(set(s.get("category", "General") for s in self.snippets)))
        return ["Todos"] + cats

    def add_snippet(self, title: str, content: str, category: str = "General") -> dict:
        snip_id = "snip_" + str(uuid.uuid4())[:8]
        new_snippet = {
            "id": snip_id,
            "title": title.strip(),
            "category": category.strip() or "General",
            "content": content
        }
        self.snippets.append(new_snippet)
        self._save_snippets()
        if self.on_snippets_updated:
            self.on_snippets_updated()
        return new_snippet

    def delete_snippet(self, snippet_id: str) -> bool:
        initial_len = len(self.snippets)
        self.snippets = [s for s in self.snippets if s.get("id") != snippet_id]
        if len(self.snippets) != initial_len:
            self._save_snippets()
            if self.on_snippets_updated:
                self.on_snippets_updated()
            return True
        return False

    def broadcast_to_mobile(self, text: str):
        """Dispara el broadcast hacia los móviles conectados."""
        if self.broadcast_callback:
            self.broadcast_callback(text)

clipboard_hub = ClipboardHub()
