import os
import re
import json
import sqlite3
import subprocess
import urllib.parse
import ctypes
from typing import List, Dict, Any, Optional
import pyautogui

try:
    import pyperclip
except ImportError:
    pyperclip = None

class AntigravityManager:
    """
    Gestor de automatización e integración directa con Google Antigravity.
    Permite descubrir proyectos, explorar chats recientes y crear o continuar
    conversaciones directamente desde Hendrix Assistant.
    """

    def __init__(self):
        self.db_path = os.path.expanduser(r"~\.gemini\antigravity\conversation_summaries.db")
        self.exe_path = os.path.expandvars(r"%LOCALAPPDATA%\Programs\antigravity\Antigravity.exe")

    def _get_readonly_conn(self) -> Optional[sqlite3.Connection]:
        if not os.path.exists(self.db_path):
            return None
        try:
            # Modo solo lectura para evitar cualquier bloqueo con el proceso activo de Antigravity
            conn = sqlite3.connect(f"file:{self.db_path}?mode=ro", uri=True)
            return conn
        except Exception:
            try:
                return sqlite3.connect(self.db_path)
            except Exception:
                return None

    def get_projects(self) -> List[Dict[str, Any]]:
        """
        Obtiene la lista de proyectos y repositorios registrados en Antigravity
        ordenados por actividad reciente.
        """
        conn = self._get_readonly_conn()
        if not conn:
            return []

        projects = []
        try:
            c = conn.cursor()
            query = """
                SELECT DISTINCT workspace_uris, project_id, MAX(last_modified_time) as last_active, COUNT(*) as total_convs
                FROM conversation_summaries
                WHERE parent_conversation_id = '' AND killed = 0
                GROUP BY workspace_uris
                ORDER BY last_active DESC
                LIMIT 30
            """
            rows = c.execute(query).fetchall()

            for uris_json, pid, last_active, total_convs in rows:
                try:
                    uris = json.loads(uris_json)
                except Exception:
                    uris = [uris_json]

                if not uris:
                    continue

                primary_uri = uris[0]
                clean_path = urllib.parse.unquote(primary_uri.replace("file:///", "").replace("file://", ""))
                # Obtener nombre legible del proyecto
                folder_name = os.path.basename(clean_path.rstrip("/\\"))
                if not folder_name:
                    folder_name = clean_path

                projects.append({
                    "id": pid or primary_uri,
                    "name": folder_name,
                    "workspaceUri": primary_uri,
                    "lastActive": str(last_active),
                    "totalConversations": total_convs
                })
        except Exception as e:
            print(f"[AntigravityManager] Error listando proyectos: {e}")
        finally:
            conn.close()

        return projects

    def get_recent_chats(self, project_id_or_uri: Optional[str] = None, limit: int = 20) -> List[Dict[str, Any]]:
        """
        Obtiene la lista de conversaciones recientes en Antigravity.
        """
        conn = self._get_readonly_conn()
        if not conn:
            return []

        chats = []
        try:
            c = conn.cursor()
            if project_id_or_uri:
                query = """
                    SELECT conversation_id, title, preview, last_modified_time, step_count, project_id, workspace_uris
                    FROM conversation_summaries
                    WHERE parent_conversation_id = '' AND killed = 0 AND (project_id = ? OR workspace_uris LIKE ?)
                    ORDER BY last_modified_time DESC
                    LIMIT ?
                """
                pattern = f"%{project_id_or_uri}%"
                rows = c.execute(query, (project_id_or_uri, pattern, limit)).fetchall()
            else:
                query = """
                    SELECT conversation_id, title, preview, last_modified_time, step_count, project_id, workspace_uris
                    FROM conversation_summaries
                    WHERE parent_conversation_id = '' AND killed = 0
                    ORDER BY last_modified_time DESC
                    LIMIT ?
                """
                rows = c.execute(query, (limit,)).fetchall()

            for cid, title, preview, last_mod, steps, pid, uris_json in rows:
                display_title = title.strip() if title and title.strip() else (preview.strip() if preview and preview.strip() else "Conversación")
                if len(display_title) > 60:
                    display_title = display_title[:57] + "..."

                chats.append({
                    "conversationId": cid,
                    "title": display_title,
                    "preview": preview or "",
                    "lastModified": str(last_mod),
                    "stepCount": steps or 0,
                    "projectId": pid or "",
                    "workspaceUri": uris_json or ""
                })
        except Exception as e:
            print(f"[AntigravityManager] Error listando chats: {e}")
        finally:
            conn.close()

        return chats

    def find_antigravity_window(self) -> Optional[int]:
        """Busca el manejador de ventana (HWND) de Antigravity si está abierta."""
        user32 = ctypes.windll.user32
        hDesk = user32.OpenInputDesktop(0, False, 0x01FF)
        if hDesk:
            user32.SetThreadDesktop(hDesk)

        found_hwnd = None
        try:
            import psutil
            ag_pids = set()
            for p in psutil.process_iter(['pid', 'name']):
                if 'antigravity' in p.info['name'].lower():
                    ag_pids.add(p.info['pid'])

            def enum_proc(hwnd, lParam):
                nonlocal found_hwnd
                pid = ctypes.c_ulong()
                user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
                if pid.value in ag_pids:
                    length = user32.GetWindowTextLengthW(hwnd)
                    if length > 0:
                        buff = ctypes.create_unicode_buffer(length + 1)
                        user32.GetWindowTextW(hwnd, buff, length + 1)
                        title = buff.value
                        if title and title not in ['Default IME', 'MSCTFIME UI', 'DDE Server Window']:
                            found_hwnd = hwnd
                            return False
                return True

            WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
            user32.EnumDesktopWindows(hDesk, WNDENUMPROC(enum_proc), 0)
            if found_hwnd:
                return found_hwnd
        except Exception as e:
            print(f"[AntigravityManager] Error buscando ventana por PID: {e}")

        # Fallback estándar por título
        def enum_windows_proc(hwnd, extra):
            nonlocal found_hwnd
            if user32.IsWindowVisible(hwnd):
                length = user32.GetWindowTextLengthW(hwnd)
                if length > 0:
                    buff = ctypes.create_unicode_buffer(length + 1)
                    user32.GetWindowTextW(hwnd, buff, length + 1)
                    title = buff.value
                    if "antigravity" in title.lower():
                        found_hwnd = hwnd
                        return False
            return True

        WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
        user32.EnumWindows(WNDENUMPROC(enum_windows_proc), 0)
        return found_hwnd

    def launch_or_focus(self) -> bool:
        """Enfoca la ventana de Antigravity o la inicia si no está corriendo."""
        hwnd = self.find_antigravity_window()
        if hwnd:
            ctypes.windll.user32.ShowWindow(hwnd, 9) # SW_RESTORE
            ctypes.windll.user32.SetForegroundWindow(hwnd)
            return True

        if os.path.exists(self.exe_path):
            try:
                subprocess.Popen([self.exe_path])
                pyautogui.sleep(1.5)
                return True
            except Exception as e:
                print(f"[AntigravityManager] Error lanzando ejecutable: {e}")
                return False

        return False

    def execute_action(
        self,
        mode: str,
        project_uri: Optional[str] = None,
        conversation_id: Optional[str] = None,
        prompt: Optional[str] = None
    ) -> bool:
        """
        Ejecuta la acción solicitada en Antigravity:
        - LAUNCH_OR_FOCUS: abrir/enfocar
        - NEW_CHAT: abrir nuevo chat y opcionalmente escribir prompt
        - EXISTING_CHAT: enfocar conversación y opcionalmente escribir prompt
        """
        self.launch_or_focus()
        pyautogui.sleep(0.5)

        if mode == "NEW_CHAT":
            # Nuevo chat en Antigravity (Ctrl+N o atajo de nueva conversación)
            pyautogui.hotkey("ctrl", "n")
            pyautogui.sleep(0.6)

            if prompt and prompt.strip():
                self._inject_prompt(prompt.strip())
            return True

        elif mode == "EXISTING_CHAT":
            if conversation_id:
                try:
                    os.startfile(f"antigravity://conversation/{conversation_id}")
                    pyautogui.sleep(0.8)
                except Exception:
                    pass

            if prompt and prompt.strip():
                self._inject_prompt(prompt.strip())
            return True

        elif mode == "SWITCH_PROJECT_ONLY":
            if project_uri:
                clean_path = urllib.parse.unquote(project_uri.replace("file:///", "").replace("file://", ""))
                try:
                    subprocess.Popen([self.exe_path, clean_path])
                    return True
                except Exception:
                    pass
            return True

        return True

    def _inject_prompt(self, text: str):
        """Inyecta el prompt en el área de entrada activa de Antigravity."""
        hwnd = self.find_antigravity_window()
        if hwnd:
            rect = (ctypes.c_long * 4)()
            ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
            left, top, right, bottom = rect[0], rect[1], rect[2], rect[3]
            w = right - left
            h = bottom - top
            click_x = left + int(w * 0.59)
            click_y = top + int(h * 0.93)
            pyautogui.click(click_x, click_y)
            pyautogui.sleep(0.3)

        if pyperclip:
            pyperclip.copy(text)
            pyautogui.sleep(0.1)
            pyautogui.hotkey("ctrl", "v")
            pyautogui.sleep(0.1)
            pyautogui.press("enter")
        else:
            pyautogui.write(text, interval=0.01)
            pyautogui.press("enter")

antigravity_manager = AntigravityManager()
