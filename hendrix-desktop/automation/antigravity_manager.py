import os
import sys
import time
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

    def get_active_conversations(self, limit: int = 10) -> List[Dict[str, Any]]:
        """
        Obtiene la lista de todas las conversaciones que estaban activas o en ejecución
        (not_fully_idle=1 o CASCADE_RUN_STATUS_RUNNING) ordenadas por última actividad.
        Permite la recuperación multi-chat escalable.
        """
        conn = self._get_readonly_conn()
        if not conn:
            return []

        active = []
        try:
            c = conn.cursor()
            query = """
                SELECT conversation_id, title, preview, last_modified_time, step_count, project_id, workspace_uris, status, not_fully_idle
                FROM conversation_summaries
                WHERE parent_conversation_id = '' AND killed = 0 AND (not_fully_idle = 1 OR status = 'CASCADE_RUN_STATUS_RUNNING')
                ORDER BY last_modified_time DESC
                LIMIT ?
            """
            rows = c.execute(query, (limit,)).fetchall()
            for cid, title, preview, last_mod, steps, pid, uris_json, status, not_idle in rows:
                display_title = title.strip() if title and title.strip() else (preview.strip() if preview and preview.strip() else "Conversación")
                if len(display_title) > 60:
                    display_title = display_title[:57] + "..."

                active.append({
                    "conversationId": cid,
                    "title": display_title,
                    "preview": preview or "",
                    "lastModified": str(last_mod),
                    "stepCount": steps or 0,
                    "projectId": pid or "",
                    "workspaceUri": uris_json or "",
                    "status": status or "",
                    "notFullyIdle": bool(not_idle)
                })

            # Si ninguna conversación tenía not_fully_idle=1, fallback al chat más reciente
            if not active:
                recents = self.get_recent_chats(limit=1)
                if recents:
                    active = recents

        except Exception as e:
            print(f"[AntigravityManager] Error buscando conversaciones activas: {e}")
        finally:
            conn.close()

        return active

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

    def wait_for_window(self, timeout_sec: float = 15.0) -> Optional[int]:
        """Espera de forma no bloqueante a que la ventana de Antigravity aparezca y sea visible."""
        start_time = time.time()
        while time.time() - start_time < timeout_sec:
            hwnd = self.find_antigravity_window()
            if hwnd:
                time.sleep(1.0)
                return hwnd
            time.sleep(0.5)
        return None

    def is_process_running(self) -> bool:
        """Comprueba si algún proceso de Antigravity.exe está corriendo."""
        try:
            import psutil
            for p in psutil.process_iter(['name']):
                try:
                    if (p.info.get('name') or '').lower() == 'antigravity.exe':
                        return True
                except Exception:
                    continue
        except Exception:
            pass
        return False

    def launch_or_focus(self, workspace_path: Optional[str] = None, force_relaunch: bool = False) -> bool:
        """
        Enfoca la ventana de Antigravity o la inicia si no está corriendo.
        Si force_relaunch es True, ignora cualquier HWND previo y lanza un proceso nuevo.
        """
        if not force_relaunch:
            hwnd = self.find_antigravity_window()
            if hwnd:
                ctypes.windll.user32.ShowWindow(hwnd, 9) # SW_RESTORE
                ctypes.windll.user32.SetForegroundWindow(hwnd)
                return True

        if os.path.exists(self.exe_path):
            try:
                # Si se solicitó force_relaunch, esperar a que los procesos previos mueran
                if force_relaunch:
                    start_wait = time.time()
                    while time.time() - start_wait < 3.0:
                        if not self.is_process_running():
                            break
                        time.sleep(0.3)

                launched = False
                if hasattr(os, "startfile"):
                    try:
                        if workspace_path and os.path.exists(workspace_path):
                            os.startfile(self.exe_path, "open", f'"{workspace_path}"')
                        else:
                            os.startfile(self.exe_path)
                        launched = True
                    except Exception as e_sf:
                        print(f"[AntigravityManager] Advertencia en startfile, recurriendo a Popen: {e_sf}")

                if not launched:
                    cmd = [self.exe_path]
                    if workspace_path and os.path.exists(workspace_path):
                        cmd.append(workspace_path)
                    creation_flags = 0
                    if sys.platform == "win32":
                        creation_flags = subprocess.CREATE_NEW_PROCESS_GROUP
                    subprocess.Popen(cmd, creationflags=creation_flags)

                hwnd = self.wait_for_window(timeout_sec=15.0)
                if hwnd:
                    ctypes.windll.user32.ShowWindow(hwnd, 9)
                    ctypes.windll.user32.SetForegroundWindow(hwnd)
                return True
            except Exception as e:
                print(f"[AntigravityManager] Error lanzando ejecutable: {e}")
                return False

        return False

    def force_foreground_window(self, hwnd: int) -> bool:
        """
        Garantiza que la ventana de Antigravity adquiera el foco del primer plano en Windows 10/11
        utilizando la combinación de restauración, bypass de tecla ALT y AttachThreadInput.
        """
        user32 = ctypes.windll.user32
        kernel32 = ctypes.windll.kernel32

        if not user32.IsWindow(hwnd):
            return False

        if user32.IsIconic(hwnd):
            user32.ShowWindow(hwnd, 9) # SW_RESTORE
        else:
            user32.ShowWindow(hwnd, 5) # SW_SHOW

        fg_hwnd = user32.GetForegroundWindow()
        if fg_hwnd == hwnd:
            return True

        # Método 1: Bypass de tecla ALT (estándar para Win10/11)
        VK_MENU = 0x12
        KEYEVENTF_KEYUP = 0x0002
        user32.keybd_event(VK_MENU, 0, 0, 0)
        user32.SetForegroundWindow(hwnd)
        user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
        user32.BringWindowToTop(hwnd)

        if user32.GetForegroundWindow() == hwnd:
            return True

        # Método 2: AttachThreadInput para heredar permisos de foco del hilo foreground
        fore_thread = user32.GetWindowThreadProcessId(fg_hwnd, None)
        app_thread = kernel32.GetCurrentThreadId()
        target_thread = user32.GetWindowThreadProcessId(hwnd, None)

        if fore_thread and target_thread and fore_thread != target_thread:
            try:
                user32.AttachThreadInput(fore_thread, app_thread, True)
                user32.AttachThreadInput(target_thread, app_thread, True)
                user32.BringWindowToTop(hwnd)
                user32.ShowWindow(hwnd, 9)
                user32.SetForegroundWindow(hwnd)
            finally:
                user32.AttachThreadInput(fore_thread, app_thread, False)
                user32.AttachThreadInput(target_thread, app_thread, False)

        return user32.GetForegroundWindow() == hwnd

    def _detect_input_box(self, img, w: int, h: int) -> Optional[int]:
        """
        Detecta dinámicamente la coordenada Y central de la caja de entrada de texto
        en Antigravity analizando el color característico del contenedor RGB=(28, 28, 28).
        Soporta tanto la vista centrada (home) como la vista inferior (conversación activa).
        """
        search_x = int(w * 0.60)
        matches = []
        for y in range(int(h * 0.30), int(h * 0.98)):
            r, g, b = img.getpixel((search_x, y))[:3]
            if 22 <= r <= 36 and 22 <= g <= 36 and 22 <= b <= 36:
                matches.append(y)

        if not matches:
            return None

        # Agrupar píxeles contiguos (la caja tiene al menos 15px de altura)
        groups = []
        curr = [matches[0]]
        for m in matches[1:]:
            if m == curr[-1] + 1:
                curr.append(m)
            else:
                if len(curr) >= 15:
                    groups.append(curr)
                curr = [m]
        if len(curr) >= 15:
            groups.append(curr)

        if not groups:
            return None

        # Preferir el grupo inferior si existe (caja inferior de chat)
        best_group = groups[-1]
        return (best_group[0] + best_group[-1]) // 2

    def get_chat_by_id(self, conversation_id: str) -> Optional[Dict[str, Any]]:
        """Obtiene información de una conversación específica por su conversation_id."""
        conn = self._get_readonly_conn()
        if not conn:
            return None
        try:
            c = conn.cursor()
            query = """
                SELECT conversation_id, title, preview, last_modified_time, step_count, project_id, workspace_uris
                FROM conversation_summaries
                WHERE conversation_id = ?
                LIMIT 1
            """
            row = c.execute(query, (conversation_id,)).fetchone()
            if row:
                cid, title, preview, last_mod, steps, pid, uris_json = row
                display_title = title.strip() if title and title.strip() else (preview.strip() if preview and preview.strip() else "Conversación")
                if len(display_title) > 60:
                    display_title = display_title[:57] + "..."
                return {
                    "conversationId": cid,
                    "title": display_title,
                    "preview": preview or "",
                    "lastModified": str(last_mod),
                    "stepCount": steps or 0,
                    "projectId": pid or "",
                    "workspaceUri": uris_json or ""
                }
        except Exception as e:
            print(f"[AntigravityManager] Error buscando chat {conversation_id}: {e}")
        finally:
            conn.close()
        return None

    def select_conversation(
        self,
        hwnd: int,
        title: Optional[str] = None,
        conversation_id: Optional[str] = None,
        workspace_name: Optional[str] = None
    ) -> bool:
        """
        Localiza y selecciona una conversación existente en la barra lateral de Antigravity
        utilizando Windows UI Automation (pywinauto) para evitar la creación de chats duplicados.
        """
        if not hwnd:
            return False

        target_title = title
        if not target_title and conversation_id:
            chat_info = self.get_chat_by_id(conversation_id)
            if chat_info:
                target_title = chat_info.get("title")

        try:
            from pywinauto import Desktop
            app = Desktop(backend='uia').window(handle=hwnd)

            # Si se proporcionó workspace_name y está colapsado, intentar expandirlo
            if workspace_name:
                try:
                    buttons = app.descendants(control_type='Button')
                    for b in buttons:
                        btn_text = b.window_text().strip()
                        if btn_text.lower() == workspace_name.lower():
                            links = app.descendants(control_type='Hyperlink')
                            chat_links_count = sum(1 for l in links if l.window_text().strip().lower() not in [
                                'new conversation', 'conversation history', 'new conversation in project'
                            ])
                            if chat_links_count == 0:
                                b.click_input()
                                pyautogui.sleep(0.5)
                            break
                except Exception:
                    pass

            links = app.descendants(control_type='Hyperlink')
            ignore = {'new conversation', 'conversation history', 'new conversation in project'}
            candidate_links = []
            for l in links:
                txt = l.window_text().strip()
                if txt and txt.lower() not in ignore:
                    candidate_links.append((txt, l))

            if not candidate_links:
                print(f"[AntigravityManager] No se encontraron enlaces de chat en la barra lateral.")
                return False

            best_link = None
            if target_title:
                norm_target = target_title.lower().rstrip(".").strip()
                # 1. Coincidencia exacta
                for txt, l in candidate_links:
                    if txt.lower().rstrip(".").strip() == norm_target:
                        best_link = (txt, l)
                        break

                # 2. Coincidencia por subcadena
                if not best_link:
                    for txt, l in candidate_links:
                        norm_txt = txt.lower().rstrip(".").strip()
                        if norm_target in norm_txt or norm_txt in norm_target:
                            best_link = (txt, l)
                            break

                # 3. Coincidencia por palabras clave significativas
                if not best_link:
                    target_words = set(w for w in norm_target.split() if len(w) > 3)
                    for txt, l in candidate_links:
                        txt_words = set(w for w in txt.lower().split() if len(w) > 3)
                        if target_words and len(target_words.intersection(txt_words)) >= 2:
                            best_link = (txt, l)
                            break

            # Si no hubo coincidencia con el título o no se especificó título,
            # seleccionar la primera conversación bajo el proyecto activo (la más reciente)
            if not best_link:
                best_link = candidate_links[0]
                print(f"[AntigravityManager] Seleccionando conversación más reciente por defecto: '{best_link[0]}'")

            txt, target_el = best_link
            rect = target_el.rectangle()
            cx = (rect.left + rect.right) // 2
            cy = (rect.top + rect.bottom) // 2
            print(f"[AntigravityManager] Haciendo clic en conversación '{txt}' en ({cx}, {cy})...")
            pyautogui.click(cx, cy)
            pyautogui.sleep(1.2)
            return True

        except Exception as e:
            print(f"[AntigravityManager] Advertencia en select_conversation vía UIA: {e}")
            try:
                rect = (ctypes.c_long * 4)()
                ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
                left, top, right, bottom = rect[0], rect[1], rect[2], rect[3]
                h = bottom - top
                fallback_x = left + 80
                fallback_y = top + int(h * 0.38)
                pyautogui.click(fallback_x, fallback_y)
                pyautogui.sleep(1.0)
                return True
            except Exception:
                pass

        return False

    def resume_multiple_conversations(
        self,
        chats: List[Dict[str, Any]],
        prompt: str = "Continúa con la tarea que estabas realizando"
    ) -> int:
        """
        Reanuda secuencialmente múltiples conversaciones activas en Antigravity:
        1. Selecciona la conversación en la barra lateral.
        2. Espera a que cargue el hilo de mensajes.
        3. Inyecta el prompt de continuación.
        4. Repite para cada conversación activa con un intervalo seguro.
        Retorna la cantidad de conversaciones reanudadas con éxito.
        """
        hwnd = self.find_antigravity_window()
        if not hwnd or not chats:
            return 0

        self.force_foreground_window(hwnd)
        resumed_count = 0

        for i, chat in enumerate(chats):
            title = chat.get("title", "")
            cid = chat.get("conversationId", "")
            ws = chat.get("workspaceUri", "")
            ws_name = os.path.basename(ws.replace("file:///", "").replace("file://", "").rstrip("/\\")) if ws else None

            print(f"[AntigravityManager] Reanudando conversación [{i+1}/{len(chats)}]: '{title}'...")
            self.select_conversation(
                hwnd=hwnd,
                title=title,
                conversation_id=cid,
                workspace_name=ws_name
            )

            pyautogui.sleep(1.0)
            if self._inject_prompt(prompt):
                resumed_count += 1
                print(f"[AntigravityManager] ✅ Prompt enviado a '{title}'.")
            else:
                print(f"[AntigravityManager] ⚠️ No se pudo inyectar el prompt en '{title}'.")

            # Pausa entre conversaciones para que Electron procese la orden
            if i < len(chats) - 1:
                pyautogui.sleep(1.5)

        return resumed_count

    def execute_action(
        self,
        mode: str,
        project_uri: Optional[str] = None,
        conversation_id: Optional[str] = None,
        conversation_title: Optional[str] = None,
        active_chats: Optional[List[Dict[str, Any]]] = None,
        prompt: Optional[str] = None,
        force_relaunch: bool = False
    ) -> bool:
        """
        Ejecuta la acción solicitada en Antigravity:
        - LAUNCH_OR_FOCUS: abrir/enfocar
        - NEW_CHAT: abrir nuevo chat y opcionalmente escribir prompt
        - EXISTING_CHAT: seleccionar conversación existente en la barra lateral y opcionalmente escribir prompt
        - RESUME_ALL: reanudar todas las conversaciones activas secuencialmente
        """
        workspace_clean = None
        if project_uri:
            workspace_clean = urllib.parse.unquote(project_uri.replace("file:///", "").replace("file://", ""))

        self.launch_or_focus(workspace_path=workspace_clean, force_relaunch=force_relaunch)
        hwnd = self.wait_for_window(timeout_sec=15.0)
        if hwnd:
            # Dar tiempo a Electron para renderizar el DOM del chat (6s en arranque en frío)
            pyautogui.sleep(6.0 if force_relaunch else 1.0)
            self.force_foreground_window(hwnd)
        else:
            pyautogui.sleep(2.0)

        if mode == "NEW_CHAT":
            # Nuevo chat en Antigravity (Ctrl+N o atajo de nueva conversación)
            pyautogui.hotkey("ctrl", "n")
            pyautogui.sleep(1.0)

            if prompt and prompt.strip():
                self._inject_prompt(prompt.strip())
            return True

        elif mode == "EXISTING_CHAT":
            if hwnd:
                self.force_foreground_window(hwnd)
                ws_name = os.path.basename(workspace_clean.rstrip("/\\")) if workspace_clean else None
                self.select_conversation(
                    hwnd=hwnd,
                    title=conversation_title,
                    conversation_id=conversation_id,
                    workspace_name=ws_name
                )
                pyautogui.sleep(1.0)

            if prompt and prompt.strip():
                self._inject_prompt(prompt.strip())
            return True

        elif mode == "RESUME_ALL":
            chats_to_resume = active_chats or []
            if not chats_to_resume and conversation_id:
                chats_to_resume = [{"conversationId": conversation_id, "title": conversation_title}]
            elif not chats_to_resume:
                chats_to_resume = self.get_active_conversations()

            if chats_to_resume:
                resumed = self.resume_multiple_conversations(
                    chats=chats_to_resume,
                    prompt=prompt or "Continúa con la tarea que estabas realizando"
                )
                return resumed > 0
            return True

        elif mode == "SWITCH_PROJECT_ONLY":
            if project_uri:
                clean_path = urllib.parse.unquote(project_uri.replace("file:///", "").replace("file://", ""))
                try:
                    if hasattr(os, "startfile"):
                        os.startfile(self.exe_path, "open", f'"{clean_path}"')
                    else:
                        subprocess.Popen([self.exe_path, clean_path])
                    return True
                except Exception:
                    pass
            return True

        elif mode == "LAUNCH_OR_FOCUS":
            if prompt and prompt.strip():
                self._inject_prompt(prompt.strip())
            return True

        return True

    def _inject_prompt(self, text: str) -> bool:
        """Inyecta el prompt en el área de entrada activa de Antigravity de forma 100% segura y dinámica."""
        hwnd = self.find_antigravity_window()
        if not hwnd:
            print("[AntigravityManager] No se encontró ventana de Antigravity para inyectar el prompt.")
            return False

        user32 = ctypes.windll.user32

        # 1. Asegurar foco en Antigravity
        focused = False
        for _ in range(5):
            if self.force_foreground_window(hwnd):
                focused = True
                break
            pyautogui.sleep(0.4)

        # 2. Protección estricta: comprobar si la ventana foreground es efectivamente Antigravity
        fg_hwnd = user32.GetForegroundWindow()
        if fg_hwnd != hwnd and not focused:
            fg_pid = ctypes.c_ulong()
            user32.GetWindowThreadProcessId(fg_hwnd, ctypes.byref(fg_pid))
            import psutil
            try:
                fg_pname = psutil.Process(fg_pid.value).name().lower()
            except Exception:
                fg_pname = ""

            if "antigravity" not in fg_pname:
                print(f"[AntigravityManager] ⚠️ Seguridad: La ventana activa es '{fg_pname}', no Antigravity. Se cancela el pegado para no afectar la consola.")
                return False

        # 3. Obtener dimensiones de la ventana
        rect = (ctypes.c_long * 4)()
        user32.GetWindowRect(hwnd, ctypes.byref(rect))
        left, top, right, bottom = rect[0], rect[1], rect[2], rect[3]
        w = right - left
        h = bottom - top

        # 4. Capturar pantalla de la ventana para análisis dinámico
        shot = None
        try:
            shot = pyautogui.screenshot(region=(left, top, w, h))
        except Exception:
            pass

        # 5. Localizar dinámicamente la caja de texto
        target_y = None
        if shot:
            box_y = self._detect_input_box(shot, w, h)
            if box_y is not None:
                target_y = top + box_y

        # Si no se encontró caja (ej. la ventana estaba en Scheduled Tasks o Settings),
        # abrir un nuevo chat en el proyecto actual con Ctrl+N
        if target_y is None:
            pyautogui.hotkey("ctrl", "n")
            pyautogui.sleep(1.0)
            try:
                shot = pyautogui.screenshot(region=(left, top, w, h))
                if shot:
                    box_y = self._detect_input_box(shot, w, h)
                    if box_y is not None:
                        target_y = top + box_y
            except Exception:
                pass

        if target_y is None:
            target_y = top + int(h * 0.92)

        click_x = left + int(w * 0.58)

        # 6. Clic en la caja de texto
        pyautogui.click(click_x, target_y)
        pyautogui.sleep(0.4)

        # 7. Pegar y enviar
        if pyperclip:
            pyperclip.copy(text)
            pyautogui.sleep(0.2)
            pyautogui.hotkey("ctrl", "v")
            pyautogui.sleep(0.2)
            pyautogui.press("enter")
        else:
            pyautogui.write(text, interval=0.01)
            pyautogui.press("enter")

        return True

antigravity_manager = AntigravityManager()
