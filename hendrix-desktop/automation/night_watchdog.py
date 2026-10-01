import os
import sys
import json
import time
import threading
import subprocess
from datetime import datetime, timezone, timedelta
from typing import Dict, Any, Optional, Callable, List
from automation.antigravity_profiles import antigravity_profile_manager
from automation.antigravity_manager import antigravity_manager

class NightTaskWatchdog:
    """
    Supervisor autónomo de tareas nocturnas para Google Antigravity.
    Monitorea logs y transcripts en segundo plano, detecta agotamiento
    de cuota (429/Resource Exhausted) o bloqueos en bucle, rota automáticamente
    entre cuentas de Gemini Pro y auto-recupera la sesión sin intervención humana.
    """

    QUOTA_KEYWORDS = [
        "resource_exhausted",
        "quota exceeded",
        "rate limit",
        "http 429",
        "status 429",
        "code 429",
        "error 429",
        "too many requests",
        "daily limit",
        "credit limit"
    ]

    def __init__(
        self,
        profile_manager=None,
        ag_manager=None,
        log_path: Optional[str] = None,
        poll_interval: float = 3.0
    ):
        self.profile_mgr = profile_manager or antigravity_profile_manager
        self.ag_mgr = ag_manager or antigravity_manager
        self.log_path = log_path or os.path.expandvars(r"%APPDATA%\Antigravity\logs\language_server.log")
        self.poll_interval = poll_interval

        self.running = False
        self._thread: Optional[threading.Thread] = None
        self._lock = threading.Lock()

        # Callbacks para notificaciones a WebSocket (móvil y UI)
        self._event_callbacks: List[Callable[[str, Dict[str, Any]], None]] = []

        # Estado de la tarea activa
        self.task_state: Dict[str, Any] = {
            "status": "IDLE", # IDLE, RUNNING, ROTATING, RECOVERING, PAUSED, COMPLETED, FAILED
            "goal": "",
            "workspace": "",
            "activeProfile": None,
            "maxTurnsPerAccount": 40,
            "currentTurns": 0,
            "totalTurns": 0,
            "rotationsCount": 0,
            "loopRecoveriesCount": 0,
            "verificationCmd": None,
            "startedAt": None,
            "lastActivityAt": None,
            "lastIncident": None
        }

        self._last_log_size = 0
        self._last_recent_tools: List[str] = []

    def register_event_callback(self, cb: Callable[[str, Dict[str, Any]], None]):
        """Registra un listener para recibir eventos en tiempo real (WebSocket / Push)."""
        if cb not in self._event_callbacks:
            self._event_callbacks.append(cb)

    def _notify(self, event_type: str, details: Dict[str, Any]):
        payload = {
            "eventType": event_type,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "taskState": self.get_status(),
            "details": details
        }
        for cb in self._event_callbacks:
            try:
                cb(event_type, payload)
            except Exception as e:
                print(f"[NightWatchdog] Error en callback de notificación: {e}")

    def get_status(self) -> Dict[str, Any]:
        with self._lock:
            return dict(self.task_state)

    def _save_task_checkpoint(self):
        """Guarda el checkpoint en el directorio del proyecto (.agents/overnight_task.json)."""
        ws = self.task_state.get("workspace")
        if not ws or not os.path.exists(ws):
            return

        agents_dir = os.path.join(ws, ".agents")
        os.makedirs(agents_dir, exist_ok=True)
        checkpoint_path = os.path.join(agents_dir, "overnight_task.json")

        data = {
            **self.task_state,
            "checkpointUpdated": datetime.now(timezone.utc).isoformat()
        }
        try:
            with open(checkpoint_path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"[NightWatchdog] Error escribiendo checkpoint: {e}")

    def start_task(
        self,
        goal: str,
        workspace: str,
        max_turns_per_account: int = 40,
        verification_cmd: Optional[str] = None
    ) -> bool:
        """Inicia una nueva tarea nocturna supervisada."""
        with self._lock:
            if self.running:
                print("[NightWatchdog] Ya hay una tarea nocturna en ejecución.")
                return False

            active_profile = self.profile_mgr.get_active_profile()
            now_iso = datetime.now(timezone.utc).isoformat()

            self.task_state = {
                "status": "RUNNING",
                "goal": goal,
                "workspace": workspace,
                "activeProfile": active_profile,
                "maxTurnsPerAccount": max_turns_per_account,
                "currentTurns": 0,
                "totalTurns": 0,
                "rotationsCount": 0,
                "loopRecoveriesCount": 0,
                "verificationCmd": verification_cmd,
                "startedAt": now_iso,
                "lastActivityAt": now_iso,
                "lastIncident": None
            }
            self._save_task_checkpoint()

            if os.path.exists(self.log_path):
                self._last_log_size = os.path.getsize(self.log_path)
            else:
                self._last_log_size = 0

            self.running = True

        # Lanzar o enfocar Antigravity con el prompt inicial
        initial_prompt = (
            f"INSTRUCCIÓN DE TAREA NOCTURNA: {goal}\n"
            f"Trabaja en el espacio de trabajo: {workspace}\n"
            f"Por favor desglosa el objetivo en pasos lógicos, edita el código, ejecuta las herramientas necesarias "
            f"y valida cada cambio continuamente. Tienes el archivo .agents/overnight_task.json como checkpoint."
        )
        self.ag_mgr.execute_action("NEW_CHAT", project_uri=workspace, prompt=initial_prompt)

        self._thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self._thread.start()

        self._notify("NIGHT_TASK_STARTED", {
            "goal": goal,
            "workspace": workspace,
            "activeProfile": active_profile
        })
        print(f"[NightWatchdog] Tarea nocturna iniciada con perfil '{active_profile}'.")
        return True

    def stop_task(self, reason: str = "USER_REQUESTED") -> bool:
        """Detiene la supervisión de la tarea nocturna."""
        with self._lock:
            if not self.running:
                return False
            self.running = False
            self.task_state["status"] = "IDLE"
            self.task_state["lastIncident"] = reason
            self._save_task_checkpoint()

        self._notify("NIGHT_TASK_STOPPED", {"reason": reason})
        print(f"[NightWatchdog] Tarea nocturna detenida: {reason}")
        return True

    def check_log_for_quota(self) -> bool:
        """Lee las nuevas líneas agregadas a language_server.log para detectar 429 / cuota agotada."""
        if not os.path.exists(self.log_path):
            return False

        try:
            curr_size = os.path.getsize(self.log_path)
            if curr_size < self._last_log_size:
                # Log rotado o truncado
                self._last_log_size = 0

            if curr_size == self._last_log_size:
                return False

            with open(self.log_path, "r", encoding="utf-8", errors="ignore") as f:
                f.seek(self._last_log_size)
                new_lines = f.read()
                self._last_log_size = curr_size

                lower_chunk = new_lines.lower()
                for keyword in self.QUOTA_KEYWORDS:
                    if keyword in lower_chunk:
                        print(f"[NightWatchdog] Detectado límite de cuota en log: '{keyword}'")
                        return True
        except Exception as e:
            print(f"[NightWatchdog] Error leyendo log de lenguaje: {e}")

        return False

    def check_transcript_anomalies(self) -> Optional[str]:
        """
        Inspecciona el transcript activo más reciente en búsqueda de:
        - QUOTA_EXHAUSTED (errores de cuota explícitos)
        - TOOL_LOOP (misma herramienta llamada 3 veces consecutivas con iguales argumentos)
        - AGENT_ERROR (paso fallido no recuperado)
        """
        brain_dir = os.path.expanduser(r"~\.gemini\antigravity\brain")
        if not os.path.exists(brain_dir):
            return None

        # Encontrar el transcript modificado más recientemente
        latest_transcript = None
        latest_time = 0

        try:
            for entry in os.scandir(brain_dir):
                if entry.is_dir():
                    tpath = os.path.join(entry.path, ".system_generated", "logs", "transcript.jsonl")
                    if os.path.exists(tpath):
                        mtime = os.path.getmtime(tpath)
                        if mtime > latest_time:
                            latest_time = mtime
                            latest_transcript = tpath
        except Exception:
            return None

        if not latest_transcript:
            return None

        try:
            # Leer las últimas 15 líneas
            with open(latest_transcript, "r", encoding="utf-8", errors="ignore") as f:
                lines = f.readlines()[-15:]

            recent_tool_signatures = []
            for line in lines:
                try:
                    step = json.loads(line)
                    content = str(step.get("content", "")).lower()
                    status = step.get("status", "")

                    # Verificar si el contenido o error menciona cuota exclusivamente ante status de error
                    if status == "ERROR" or step.get("type") == "ERROR":
                        for kw in self.QUOTA_KEYWORDS:
                            if kw in content:
                                return "QUOTA_EXHAUSTED"

                    # Detección de bucles en llamadas de herramientas
                    calls = step.get("tool_calls", [])
                    if calls:
                        for call in calls:
                            name = call.get("name") or call.get("toolName", "")
                            args_str = json.dumps(call.get("args") or call.get("arguments", {}), sort_keys=True)
                            sig = f"{name}:{args_str}"
                            recent_tool_signatures.append(sig)

                except Exception:
                    continue

            # Si las últimas 3 herramientas son exactamente idénticas -> BUCLE
            if len(recent_tool_signatures) >= 3:
                last_3 = recent_tool_signatures[-3:]
                if last_3[0] == last_3[1] == last_3[2]:
                    return "TOOL_LOOP"

        except Exception as e:
            print(f"[NightWatchdog] Error inspeccionando transcript: {e}")

        return None

    def trigger_rotation(self, reason: str):
        """Ejecuta la rotación de cuenta hacia la siguiente disponible."""
        with self._lock:
            if not self.running:
                return
            self.task_state["status"] = "ROTATING"
            self.task_state["lastIncident"] = f"ROTATION_{reason}"
            curr_profile = self.task_state.get("activeProfile") or self.profile_mgr.get_active_profile()

        self._notify("NIGHT_TASK_ROTATING", {
            "reason": reason,
            "departingProfile": curr_profile
        })

        if reason == "QUOTA_EXHAUSTED" and curr_profile:
            self.profile_mgr.mark_quota_exhausted(curr_profile, cooldown_hours=4.0)

        # Buscar el siguiente perfil disponible
        next_profile = self.profile_mgr.get_next_available_profile(exclude_current=True)
        if not next_profile:
            print("[NightWatchdog] Todas las cuentas de Gemini Pro están en período de enfriamiento.")
            with self._lock:
                self.task_state["status"] = "PAUSED"
                self.task_state["lastIncident"] = "ALL_ACCOUNTS_IN_COOLDOWN"
                self._save_task_checkpoint()
            self._notify("NIGHT_TASK_PAUSED_COOLDOWN", {"message": "Esperando expiración de cooldown"})
            return

        # Detectar chats activos antes de rotar para reanudarlos
        active_chats = []
        try:
            active_chats = self.ag_mgr.get_active_conversations()
        except Exception:
            pass

        print(f"[NightWatchdog] Rotando de '{curr_profile}' a '{next_profile}'...")
        success = self.profile_mgr.activate_profile(next_profile, kill_running=True)

        if not success:
            print(f"[NightWatchdog] Falló la activación del perfil '{next_profile}'.")
            return

        with self._lock:
            self.task_state["activeProfile"] = next_profile
            self.task_state["rotationsCount"] += 1
            self.task_state["currentTurns"] = 0
            self.task_state["status"] = "RUNNING"
            self.task_state["lastActivityAt"] = datetime.now(timezone.utc).isoformat()
            self._save_task_checkpoint()

        # Inyectar prompt de continuación en la nueva sesión
        goal = self.task_state.get("goal", "")
        ws = self.task_state.get("workspace", "")
        continuation_prompt = (
            f"CONTINUACIÓN DE TAREA NOCTURNA TRAS ROTACIÓN DE CUENTA:\n"
            f"Objetivo: {goal}\n"
            f"Espacio de trabajo: {ws}\n"
            f"Por favor revisa el estado actual de los archivos y git, lee el checkpoint en .agents/overnight_task.json "
            f"y continúa ejecutando las tareas pendientes y ejecutando tests hasta completar el objetivo."
        )

        time.sleep(1.0)
        if len(active_chats) > 1:
            self.ag_mgr.execute_action(
                mode="RESUME_ALL",
                active_chats=active_chats,
                prompt=continuation_prompt,
                force_relaunch=True
            )
        else:
            cid = active_chats[0]["conversationId"] if active_chats else None
            title = active_chats[0].get("title") if active_chats else None
            self.ag_mgr.execute_action(
                mode="EXISTING_CHAT" if active_chats else "NEW_CHAT",
                project_uri=ws,
                conversation_id=cid,
                conversation_title=title,
                prompt=continuation_prompt,
                force_relaunch=True
            )

        self._notify("NIGHT_TASK_RESUMED", {
            "activeProfile": next_profile,
            "rotationsCount": self.task_state["rotationsCount"]
        })
        print(f"[NightWatchdog] Tarea nocturna reanudada exitosamente con cuenta '{next_profile}'.")

    def trigger_loop_recovery(self, anomaly_type: str):
        """Inyecta un prompt correctivo si el agente se atascó en un bucle o error."""
        with self._lock:
            if not self.running:
                return
            self.task_state["status"] = "RECOVERING"
            self.task_state["loopRecoveriesCount"] += 1
            recoveries = self.task_state["loopRecoveriesCount"]
            self.task_state["lastIncident"] = f"LOOP_{anomaly_type}"
            self._save_task_checkpoint()

        self._notify("NIGHT_TASK_LOOP_DETECTED", {
            "anomaly": anomaly_type,
            "recoveryAttempt": recoveries
        })

        if recoveries > 3:
            print("[NightWatchdog] Demasiados intentos de recuperación de bucle consecutivos. Forzando rotación de contexto/cuenta...")
            self.trigger_rotation("LOOP_MAX_RETRIES")
            return

        corrective_nudge = (
            "⚠️ [SUPERVISOR NOCTURNO AUTOMÁTICO]: Se ha detectado un bucle o estancamiento repetitivo en la ejecución.\n"
            "DIRECTRICES DE RECUPERACIÓN:\n"
            "1. NO intentes de nuevo la misma acción o comando que falló previamente.\n"
            "2. Analiza el error subyacente o ejecuta pruebas/diagnóstico.\n"
            "3. Aplica un enfoque alternativo y continúa con el siguiente paso de la meta establecida."
        )

        self.ag_mgr.execute_action("NEW_CHAT", prompt=corrective_nudge)
        time.sleep(1.0)

        with self._lock:
            self.task_state["status"] = "RUNNING"
            self.task_state["lastActivityAt"] = datetime.now(timezone.utc).isoformat()
            self._save_task_checkpoint()

        print(f"[NightWatchdog] Prompt de recuperación inyectado (intento {recoveries}).")

    def run_verification(self) -> bool:
        """Ejecuta el comando de verificación (ej. pytest o gradlew) si fue configurado."""
        cmd = self.task_state.get("verificationCmd")
        ws = self.task_state.get("workspace")
        if not cmd or not ws or not os.path.exists(ws):
            return False

        try:
            print(f"[NightWatchdog] Ejecutando verificación de meta: '{cmd}' en '{ws}'...")
            res = subprocess.run(cmd, shell=True, cwd=ws, capture_output=True, text=True, timeout=120)
            return res.returncode == 0
        except Exception as e:
            print(f"[NightWatchdog] Error en comando de verificación: {e}")
            return False

    def _monitor_loop(self):
        """Bucle principal de monitoreo en segundo plano."""
        print("[NightWatchdog] Iniciando bucle de supervisión continua...")
        while self.running:
            try:
                # 1. Verificar si la tarea fue pausada por cooldown general
                if self.task_state.get("status") == "PAUSED":
                    next_prof = self.profile_mgr.get_next_available_profile(exclude_current=False)
                    if next_prof:
                        print(f"[NightWatchdog] Perfil '{next_prof}' ha salido de cooldown. Reanudando tarea...")
                        self.trigger_rotation("COOLDOWN_EXPIRED")
                    time.sleep(self.poll_interval * 2)
                    continue

                # 2. Monitoreo de cuota en language_server.log
                if self.check_log_for_quota():
                    self.trigger_rotation("QUOTA_EXHAUSTED")
                    time.sleep(self.poll_interval * 2)
                    continue

                # 3. Monitoreo de anomalías en transcript (bucles, 429)
                anomaly = self.check_transcript_anomalies()
                if anomaly == "QUOTA_EXHAUSTED":
                    self.trigger_rotation("QUOTA_EXHAUSTED")
                    time.sleep(self.poll_interval * 2)
                    continue
                elif anomaly == "TOOL_LOOP":
                    self.trigger_loop_recovery("TOOL_LOOP")
                    time.sleep(self.poll_interval * 2)
                    continue

                # 4. Monitoreo proactivo de turnos por cuenta
                max_turns = self.task_state.get("maxTurnsPerAccount", 0)
                if max_turns > 0 and self.task_state.get("currentTurns", 0) >= max_turns:
                    print(f"[NightWatchdog] Límite proactivo de {max_turns} turnos alcanzado en cuenta actual.")
                    self.trigger_rotation("PROACTIVE_TURN_LIMIT")
                    time.sleep(self.poll_interval * 2)
                    continue

                # 5. Incrementar turnos si hubo actividad reciente
                with self._lock:
                    self.task_state["currentTurns"] += 1
                    self.task_state["totalTurns"] += 1
                    self.task_state["lastActivityAt"] = datetime.now(timezone.utc).isoformat()
                    self._save_task_checkpoint()

            except Exception as e:
                print(f"[NightWatchdog] Excepción en bucle de supervisión: {e}")

            time.sleep(self.poll_interval)

        print("[NightWatchdog] Bucle de supervisión finalizado.")

night_task_watchdog = NightTaskWatchdog()
