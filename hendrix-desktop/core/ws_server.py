import asyncio
import json
import socket
import hashlib
import psutil
import websockets
import pyperclip
import pyautogui
from config import config
from core.session_manager import session_manager
from core.security_guard import SecurityGuard
from capture.screen_engine import screen_engine
from input.input_controller import input_controller
from automation.uia_manager import uia_manager
from automation.antigravity_manager import antigravity_manager
from automation.ableton_manager import ableton_manager
from automation.module_automations import execute_module_action
from core.tunnel_manager import tunnel_manager
from storage.dropzone_manager import dropzone_manager
from core.audio_mixer import audio_mixer
from core.project_browser import project_browser
from core.hardware_watchdog import hardware_watchdog
from automation.scene_coordinator import scene_coordinator
from core.plugin_engine import plugin_engine
from core.sentinel_monitor import SentinelMonitor
from core.audio_stream_server import AudioStreamServer
from core.foreground_observer import foreground_observer
from core.workspace_git_provider import workspace_git_provider
from core.terminal_watchdog import terminal_watchdog
from core.airsync_server import airsync_server
from core.system_ops import system_ops

sentinel_monitor = SentinelMonitor(hardware_watchdog=hardware_watchdog)
audio_stream_server = AudioStreamServer()

activity_logger = None
active_websockets = set()
main_event_loop = None

def broadcast_render_watchdog_event(payload: dict):
    if not active_websockets:
        return
    msg = json.dumps({"type": "RENDER_WATCHDOG_EVENT", "event": payload})
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass
    sentinel_monitor.emit_render_completed_alert(payload)

def broadcast_proactive_alert(payload: dict):
    if not active_websockets:
        return
    msg = json.dumps(payload)
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass

def broadcast_audio_frame(packet: bytes):
    if not active_websockets:
        return
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(packet), main_event_loop)
        except Exception:
            pass

sentinel_monitor.register_callback(broadcast_proactive_alert)
audio_stream_server.register_callback(broadcast_audio_frame)

def broadcast_foreground_change(payload: dict):
    if not active_websockets:
        return
    msg = json.dumps(payload)
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass

def broadcast_terminal_error(payload: dict):
    if not active_websockets:
        return
    msg = json.dumps(payload)
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass

foreground_observer.register_callback(broadcast_foreground_change)
terminal_watchdog.register_callback(broadcast_terminal_error)

def broadcast_dropzone_file_ready(payload: dict):
    if not active_websockets:
        return
    msg = json.dumps({"type": "DROPZONE_FILE_READY", "file": payload})
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass

def broadcast_clipboard_push(text: str):
    if not active_websockets:
        return
    sha = hashlib.sha256(text.encode("utf-8")).hexdigest()
    msg = json.dumps({
        "type": "CLIPBOARD_SET_ACK",
        "text": text,
        "charCount": len(text),
        "sha256": sha,
        "status": "ok"
    })
    for ws in list(active_websockets):
        try:
            if main_event_loop and main_event_loop.is_running():
                asyncio.run_coroutine_threadsafe(ws.send(msg), main_event_loop)
        except Exception:
            pass

dropzone_manager.add_file_ready_listener(broadcast_dropzone_file_ready)
dropzone_manager.start_watcher()

from storage.clipboard_hub import clipboard_hub
clipboard_hub.broadcast_callback = broadcast_clipboard_push

def log_activity(text: str):
    if activity_logger:
        activity_logger(text)
    else:
        print(f"[Hendrix] {text}")

def get_primary_mac_address():
    try:
        for iface, addrs in psutil.net_if_addrs().items():
            has_ipv4 = False
            mac = None
            for addr in addrs:
                if getattr(addr, 'family', None) in (-1, getattr(psutil, 'AF_LINK', -1)):
                    mac = addr.address
                elif getattr(addr, 'family', None) == 2:
                    if not addr.address.startswith("127."):
                        has_ipv4 = True
            if has_ipv4 and mac:
                return mac.replace("-", ":").upper()
    except Exception:
        pass
    try:
        import uuid
        mac_num = uuid.getnode()
        return ':'.join(['{:02X}'.format((mac_num >> elements) & 0xff) for elements in range(0, 8*6, 8)][::-1])
    except Exception:
        return None

def get_telemetry_dict() -> dict:
    try:
        cpu = psutil.cpu_percent(interval=None)
        mem = psutil.virtual_memory()
        active_title = ""
        rect = screen_engine.get_active_window_rect()
        if rect:
            active_title = f"Ventana activa ({rect['width']}x{rect['height']})"

        battery = psutil.sensors_battery()
        bat_pct = int(battery.percent) if battery else None
        is_charging = battery.power_plugged if battery else False

        return {
            "cpuPercent": cpu,
            "ramPercent": mem.percent,
            "ramUsedGb": round(mem.used / (1024**3), 1),
            "ramTotalGb": round(mem.total / (1024**3), 1),
            "volumePercent": 60,
            "isMuted": False,
            "activeWindowTitle": active_title,
            "activeProcessName": "Windows",
            "isBatteryPresent": battery is not None,
            "batteryPercent": bat_pct,
            "isBatteryCharging": is_charging,
            "hostname": socket.gethostname(),
            "osName": "Windows 11",
            "isSessionLocked": screen_engine.is_session_locked(),
            "macAddress": get_primary_mac_address()
        }
    except Exception as e:
        return {
            "cpuPercent": 10.0,
            "ramPercent": 40.0,
            "ramUsedGb": 6.0,
            "ramTotalGb": 16.0,
            "volumePercent": 50,
            "isMuted": False,
            "activeWindowTitle": "Windows Desktop",
            "activeProcessName": "Explorer.exe",
            "isBatteryPresent": False,
            "hostname": socket.gethostname(),
            "osName": "Windows"
        }

async def handle_client(websocket):
    client_ip = websocket.remote_address[0]
    log_activity(f"Nueva conexión entrante desde {client_ip}")
    active_websockets.add(websocket)

    try:
        async for message in websocket:
            if isinstance(message, str):
                try:
                    data = json.loads(message)
                except Exception:
                    continue

                msg_type = data.get("type", "")

                # 1. Emparejamiento inicial
                if msg_type == "HELLO_PAIR":
                    pin_attempt = data.get("pin", "")
                    token = data.get("token", "")
                    dev_name = data.get("deviceName", f"Móvil ({client_ip})")

                    auth_token = session_manager.authenticate_or_pair(
                        pin_attempt=pin_attempt,
                        client_token=token,
                        device_name=dev_name,
                        client_ip=client_ip,
                        websocket=websocket
                    )
                    if auth_token:
                        log_activity(f"✅ Dispositivo emparejado con éxito: {dev_name}")
                        ack = {
                            "type": "HELLO_ACK",
                            "status": "AUTHORIZED",
                            "token": auth_token,
                            "hostname": socket.gethostname(),
                            "remoteTunnelUrl": tunnel_manager.get_tunnel_url(),
                            "macAddress": get_primary_mac_address()
                        }
                        await websocket.send(json.dumps(ack))
                    else:
                        log_activity(f"❌ Intento de conexión con PIN incorrecto desde {client_ip}")
                        await websocket.send(json.dumps({
                            "type": "HELLO_ACK",
                            "status": "UNAUTHORIZED",
                            "message": "PIN incorrecto"
                        }))

                # 2. Solicitud de Snapshot WebP
                elif msg_type == "SNAPSHOT_REQUEST":
                    if not SecurityGuard.can_capture_screen():
                        continue
                    crop_to_active = data.get("cropToActiveWindow", False)
                    quality = data.get("quality", 75)
                    try:
                        webp_bytes = screen_engine.capture_webp(quality=quality, crop_to_active=crop_to_active)
                        await websocket.send(webp_bytes)
                        log_activity(f"📸 Snapshot WebP enviado ({len(webp_bytes) // 1024} KB)")
                    except Exception as e:
                        log_activity(f"Error al capturar pantalla: {e}")

                # 3. Inyección de acción física (ratón / teclado / atajos)
                elif msg_type == "INPUT_ACTION":
                    action_type = data.get("actionType", "")
                    success = input_controller.process_action(data)
                    log_activity(f"🖱️ Acción inyectada: {action_type} -> {'Éxito' if success else 'Denegado'}")

                # 4. Solicitud de telemetría en tiempo real
                elif msg_type == "TELEMETRY_REQUEST":
                    resp = {
                        "type": "TELEMETRY_DATA",
                        "telemetry": get_telemetry_dict()
                    }
                    await websocket.send(json.dumps(resp))

                # 5. Comandos rápidos de sistema (energía, volumen, apps)
                elif msg_type == "QUICK_COMMAND":
                    cmd = data.get("command", "")
                    success = uia_manager.execute_quick_command(cmd)
                    log_activity(f"⚡ Comando rápido ejecutado: {cmd} ({success})")
                    # Enviar telemetría actualizada
                    await websocket.send(json.dumps({
                        "type": "TELEMETRY_DATA",
                        "telemetry": get_telemetry_dict()
                    }))

                # 6. Generación de Plan Autónomo (RPA)
                elif msg_type == "RPA_PLAN":
                    goal = data.get("goalPrompt", "")
                    req_id = data.get("requestId", "")
                    plan = uia_manager.plan_goal(goal)
                    plan["type"] = "RPA_PLAN_RESULT"
                    plan["requestId"] = req_id
                    await websocket.send(json.dumps(plan))
                    log_activity(f"🤖 Plan RPA generado para meta: '{goal}'")

                # 7. Ejecución de Plan Aprobado (RPA)
                elif msg_type == "RPA_EXECUTE":
                    plan_data = data.get("plan", {})
                    success = uia_manager.execute_plan(plan_data)
                    log_activity(f"🚀 Plan RPA ejecutado exitosamente ({success})")

                # 8. Antigravity: Listar proyectos
                elif msg_type == "AG_LIST_PROJECTS":
                    req_id = data.get("requestId", "")
                    projects = antigravity_manager.get_projects()
                    resp = {
                        "type": "AG_LIST_PROJECTS_RESP",
                        "requestId": req_id,
                        "projects": projects
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🪐 Proyectos de Antigravity enviados ({len(projects)})")

                # 9. Antigravity: Listar chats recientes
                elif msg_type == "AG_LIST_CHATS":
                    req_id = data.get("requestId", "")
                    project_id = data.get("projectId")
                    chats = antigravity_manager.get_recent_chats(project_id)
                    resp = {
                        "type": "AG_LIST_CHATS_RESP",
                        "requestId": req_id,
                        "chats": chats
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"💬 Chats de Antigravity enviados ({len(chats)})")

                # 10. Antigravity: Ejecutar acción (Nuevo chat / Chat existente / Switch)
                elif msg_type == "AG_ACTION":
                    req_id = data.get("requestId", "")
                    mode = data.get("mode", "LAUNCH_OR_FOCUS")
                    proj_uri = data.get("workspaceUri")
                    conv_id = data.get("conversationId")
                    prompt = data.get("prompt")
                    success = antigravity_manager.execute_action(mode, proj_uri, conv_id, prompt)
                    resp = {
                        "type": "AG_ACTION_RESP",
                        "requestId": req_id,
                        "success": success,
                        "message": f"Acción Antigravity ({mode}) ejecutada."
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"⚡ Acción Antigravity ejecutada: {mode} (Éxito: {success})")

                # 11. DAW / Ableton Live: Ejecutar acción de transporte o proyecto
                elif msg_type == "DAW_ACTION":
                    req_id = data.get("requestId", "")
                    action = data.get("action", "LAUNCH_OR_FOCUS")
                    target_project = data.get("targetProject")
                    export_preset = data.get("exportPreset")
                    save_current_first = data.get("saveCurrentFirst")
                    result = ableton_manager.execute_action(action, target_project, export_preset, save_current_first)
                    resp = {
                        "type": "DAW_ACTION_RESP",
                        "requestId": req_id,
                        "success": result.get("success", True),
                        "requiresConfirmation": result.get("requiresConfirmation", False),
                        "confirmationTitle": result.get("confirmationTitle"),
                        "message": result.get("message", f"Acción DAW ({action}) ejecutada.")
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🎹 Acción DAW ejecutada: {action} (Requiere confirmación: {result.get('requiresConfirmation', False)})")

                # 12. DAW / Ableton Live: Listar proyectos recientes (.als)
                elif msg_type == "DAW_LIST_PROJECTS":
                    req_id = data.get("requestId", "")
                    projects = ableton_manager.scan_recent_projects()
                    resp = {
                        "type": "DAW_LIST_PROJECTS_RESP",
                        "requestId": req_id,
                        "projects": projects
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🎼 Proyectos DAW enviados ({len(projects)})")

                # 13. Módulos & Plugins PC: Ejecutar acción de comando rápido
                elif msg_type == "MODULE_ACTION":
                    req_id = data.get("requestId", "")
                    module_id = data.get("moduleId", "")
                    action_id = data.get("actionId", "")
                    params = data.get("params", {})
                    result = execute_module_action(module_id, action_id, params)
                    resp = {
                        "type": "MODULE_ACTION_RESP",
                        "requestId": req_id,
                        "success": result.get("success", True),
                        "message": result.get("message", f"Acción {action_id} de {module_id} ejecutada.")
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🚀 Acción de Módulo PC ejecutada: {module_id} -> {action_id} (Éxito: {result.get('success', True)})")

                # 14. Dropzone & Cloud Sync: Consultar estado y archivos recientes
                elif msg_type == "DROPZONE_GET_INFO":
                    req_id = data.get("requestId", "")
                    info = dropzone_manager.get_status_info()
                    resp = {
                        "type": "DROPZONE_INFO_RESP",
                        "requestId": req_id,
                        "info": info
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📁 Estado de Dropzone enviado a {client_ip} ({info.get('cloudProvider')})")

                # 15. Dropzone & Cloud Sync: Configurar ruta personalizada
                elif msg_type == "DROPZONE_SET_PATH":
                    req_id = data.get("requestId", "")
                    new_path = data.get("path", "")
                    success = dropzone_manager.set_dropzone_path(new_path)
                    resp = {
                        "type": "DROPZONE_SET_PATH_RESP",
                        "requestId": req_id,
                        "success": success,
                        "info": dropzone_manager.get_status_info()
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📁 Ruta de Dropzone actualizada: {new_path} (Éxito: {success})")

                # 16. Desbloqueo Remoto de Sesión Windows (Winlogon)
                elif msg_type == "SESSION_UNLOCK":
                    req_id = data.get("requestId", "")
                    pin = data.get("pin", "")
                    uia_manager.unlock_workstation(pin)
                    await asyncio.sleep(1.2)
                    is_locked = screen_engine.is_session_locked()
                    resp = {
                        "type": "SESSION_UNLOCK_ACK",
                        "requestId": req_id,
                        "success": not is_locked,
                        "isSessionLocked": is_locked
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🔓 Intento de desbloqueo de sesión ejecutado. Bloqueado: {is_locked}")

                # 17. Portapapeles: Escribir texto con verificación de hash SHA-256
                elif msg_type == "CLIPBOARD_SET":
                    req_id = data.get("requestId", "")
                    text = data.get("text", "")
                    paste_now = data.get("pasteImmediately", False)
                    try:
                        pyperclip.copy(text)
                        sha = hashlib.sha256(text.encode("utf-8")).hexdigest()
                        if paste_now:
                            pyautogui.hotkey("ctrl", "v")
                        resp = {
                            "type": "CLIPBOARD_SET_ACK",
                            "requestId": req_id,
                            "status": "ok",
                            "charCount": len(text),
                            "sha256": sha,
                            "pasted": paste_now
                        }
                    except Exception as e:
                        resp = {
                            "type": "CLIPBOARD_SET_ACK",
                            "requestId": req_id,
                            "status": "error",
                            "message": str(e)
                        }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📋 Texto copiado al portapapeles ({len(text)} caracteres, Pegar: {paste_now})")

                # 18. Portapapeles: Leer contenido actual de la PC
                elif msg_type == "CLIPBOARD_GET":
                    req_id = data.get("requestId", "")
                    try:
                        content = pyperclip.paste() or ""
                        sha = hashlib.sha256(content.encode("utf-8")).hexdigest()
                        resp = {
                            "type": "CLIPBOARD_GET_ACK",
                            "requestId": req_id,
                            "status": "ok",
                            "text": content,
                            "charCount": len(content),
                            "sha256": sha
                        }
                    except Exception as e:
                        resp = {
                            "type": "CLIPBOARD_GET_ACK",
                            "requestId": req_id,
                            "status": "error",
                            "text": "",
                            "message": str(e)
                        }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📋 Contenido del portapapeles enviado a {client_ip} ({len(resp.get('text', ''))} caracteres)")

                # 19. Mezclador de Audio: Consultar sesiones activas
                elif msg_type == "AUDIO_MIXER_GET":
                    req_id = data.get("requestId", "")
                    mixer_state = audio_mixer.get_mixer_state()
                    resp = {
                        "type": "AUDIO_MIXER_SESSIONS",
                        "requestId": req_id,
                        "mixer": mixer_state
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🎚️ Estado del mezclador de audio enviado ({len(mixer_state.get('sessions', []))} apps)")

                # 20. Mezclador de Audio: Fijar volumen (App o Maestro)
                elif msg_type == "AUDIO_MIXER_SET_VOLUME":
                    req_id = data.get("requestId", "")
                    proc_name = data.get("processName", "")
                    vol = data.get("volumePercent", 50)
                    is_master = data.get("isMaster", False)

                    if is_master:
                        audio_mixer.set_master_volume(vol)
                    else:
                        audio_mixer.set_app_volume(proc_name, vol)

                    mixer_state = audio_mixer.get_mixer_state()
                    resp = {
                        "type": "AUDIO_MIXER_SESSIONS",
                        "requestId": req_id,
                        "mixer": mixer_state
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🔊 Volumen ajustado: {proc_name or 'Maestro'} -> {vol}%")

                # 21. Mezclador de Audio: Silenciar / Activar Mute
                elif msg_type == "AUDIO_MIXER_SET_MUTE":
                    req_id = data.get("requestId", "")
                    proc_name = data.get("processName", "")
                    is_muted = data.get("isMuted", False)
                    is_master = data.get("isMaster", False)

                    if is_master:
                        audio_mixer.set_master_mute(is_muted)
                    else:
                        audio_mixer.set_app_mute(proc_name, is_muted)

                    mixer_state = audio_mixer.get_mixer_state()
                    resp = {
                        "type": "AUDIO_MIXER_SESSIONS",
                        "requestId": req_id,
                        "mixer": mixer_state
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🔇 Mute ajustado: {proc_name or 'Maestro'} -> {'Silenciado' if is_muted else 'Activo'}")

                # 22. Escenas de Estudio y Macros Encadenadas
                elif msg_type == "STUDIO_SCENE_EXECUTE":
                    req_id = data.get("requestId", "")
                    scene_id = data.get("sceneId", "")
                    result = await scene_coordinator.execute_scene(
                        scene_id,
                        emit_callback=broadcast_render_watchdog_event
                    )
                    resp = {
                        "type": "STUDIO_SCENE_RESP",
                        "requestId": req_id,
                        "sceneResult": result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🎬 Escena ejecutada: {scene_id} (Éxito: {result.get('success', False)})")

                # 23. Explorador de Proyectos: Consultar proyectos en discos
                elif msg_type == "PROJECTS_QUERY":
                    req_id = data.get("requestId", "")
                    cat = data.get("category")
                    query = data.get("query")
                    projects = project_browser.query_projects(category=cat, search_query=query)
                    resp = {
                        "type": "PROJECTS_QUERY_RESP",
                        "requestId": req_id,
                        "projects": projects
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📁 Proyectos encontrados: {len(projects)} (Filtro: {cat or 'Todos'})")

                # 24. Explorador de Proyectos: Lanzar proyecto en Windows
                elif msg_type == "PROJECT_LAUNCH":
                    req_id = data.get("requestId", "")
                    proj_path = data.get("projectPath", "")
                    ok = project_browser.launch_project(proj_path)
                    resp = {
                        "type": "PROJECT_LAUNCH_RESP",
                        "requestId": req_id,
                        "success": ok,
                        "projectPath": proj_path
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🚀 Solicitud de apertura de proyecto: {proj_path} (Éxito: {ok})")

                # 25. Telemetría de Hardware (GPU, VRAM, CPU, Temperatura)
                elif msg_type == "HARDWARE_TELEMETRY_GET":
                    req_id = data.get("requestId", "")
                    hw_data = hardware_watchdog.get_hardware_telemetry()
                    resp = {
                        "type": "HARDWARE_TELEMETRY_RESP",
                        "requestId": req_id,
                        "telemetry": hw_data
                    }
                    await websocket.send(json.dumps(resp))

                # 26. Watchdog de Renderizado
                elif msg_type == "WATCHDOG_START":
                    req_id = data.get("requestId", "")
                    proc = data.get("processName", "blender")
                    auto_susp = data.get("autoSuspend", True)
                    ok = hardware_watchdog.start_render_watchdog(
                        process_name=proc,
                        auto_suspend=auto_susp,
                        on_completed=broadcast_render_watchdog_event
                    )
                    resp = {
                        "type": "WATCHDOG_START_RESP",
                        "requestId": req_id,
                        "success": ok,
                        "processName": proc
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"Centinela de render activado para: {proc} (Auto-suspensión: {auto_susp})")

                # 27. Consulta de Plugins y Scripts de Usuario
                elif msg_type == "PLUGINS_QUERY":
                    req_id = data.get("requestId", "")
                    plugins_list = plugin_engine.get_all_plugins_metadata()
                    resp = {
                        "type": "PLUGINS_QUERY_RESP",
                        "requestId": req_id,
                        "plugins": plugins_list
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🧩 Catálogo de plugins enviado ({len(plugins_list)} plugins activos)")

                # 28. Ejecución de Acción de Plugin
                elif msg_type == "PLUGIN_EXECUTE":
                    req_id = data.get("requestId", "")
                    plugin_id = data.get("pluginId", "")
                    action_id = data.get("actionId", "")
                    params = data.get("params", {})
                    result = plugin_engine.execute_action(plugin_id, action_id, params)
                    resp = {
                        "type": "PLUGIN_EXECUTE_RESP",
                        "requestId": req_id,
                        "result": result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🧩 Acción de plugin ejecutada: [{plugin_id}.{action_id}] (Éxito: {result.get('success', False)})")

                # 29. Monitoreo de Audio Inalámbrico: Iniciar Streaming
                elif msg_type == "AUDIO_STREAM_START":
                    req_id = data.get("requestId", "")
                    sample_rate = data.get("sampleRate", 24000)
                    channels = data.get("channels", 1)
                    ok = audio_stream_server.start_stream(sample_rate=sample_rate, channels=channels)
                    resp = {
                        "type": "AUDIO_STREAM_START_RESP",
                        "requestId": req_id,
                        "success": ok,
                        "telemetry": audio_stream_server.get_telemetry()
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🎙️ Monitor de audio inalámbrico activado ({sample_rate}Hz, {channels}ch)")

                # 30. Monitoreo de Audio Inalámbrico: Detener Streaming
                elif msg_type == "AUDIO_STREAM_STOP":
                    req_id = data.get("requestId", "")
                    ok = audio_stream_server.stop_stream()
                    resp = {
                        "type": "AUDIO_STREAM_STOP_RESP",
                        "requestId": req_id,
                        "success": ok,
                        "telemetry": audio_stream_server.get_telemetry()
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity("⏹️ Monitor de audio inalámbrico detenido")

                # 31. Monitoreo de Audio Inalámbrico: Telemetría / VU-Meter
                elif msg_type == "AUDIO_STREAM_TELEMETRY_GET":
                    req_id = data.get("requestId", "")
                    resp = {
                        "type": "AUDIO_STREAM_TELEMETRY_RESP",
                        "requestId": req_id,
                        "telemetry": audio_stream_server.get_telemetry()
                    }
                    await websocket.send(json.dumps(resp))

                # 32. Alerta Proactiva de Prueba
                elif msg_type == "TRIGGER_TEST_ALERT":
                    req_id = data.get("requestId", "")
                    cat = data.get("category", "GPU_OVERHEAT")
                    alert = sentinel_monitor.emit_alert(
                        category=cat,
                        title="⚠️ Alerta de Prueba Simulada",
                        message="Esta es una prueba de alerta proactiva con botones de acción desde Hendrix Desktop.",
                        severity="WARNING",
                        actions=[
                            {"id": "suspend_pc", "label": "Suspender PC", "dangerous": False},
                            {"id": "view_copilot", "label": "Ver Pantalla", "dangerous": False},
                            {"id": "dismiss", "label": "Descartar", "dangerous": False}
                        ]
                    )
                    resp = {
                        "type": "TRIGGER_TEST_ALERT_RESP",
                        "requestId": req_id,
                        "success": True,
                        "alert": alert
                    }
                    await websocket.send(json.dumps(resp))

                # 33. Consulta de Contexto de Espacio de Trabajo & Git
                elif msg_type == "WORKSPACE_CONTEXT_GET":
                    req_id = data.get("requestId", "")
                    target_dir = data.get("directory", None)
                    ctx = workspace_git_provider.get_workspace_context(target_dir)
                    resp = {
                        "type": "WORKSPACE_CONTEXT_RESP",
                        "requestId": req_id,
                        "context": ctx
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity("🧠 Contexto de Git y espacio de trabajo enviado al móvil")

                # 34. Consulta de Ventana y Proceso en Primer Plano (Foreground)
                elif msg_type == "FOREGROUND_GET":
                    req_id = data.get("requestId", "")
                    resp = {
                        "type": "FOREGROUND_RESP",
                        "requestId": req_id,
                        "app": foreground_observer.get_current_foreground_info()
                    }
                    await websocket.send(json.dumps(resp))

                # 35. Compartir archivo para transferencia AirSync (PC -> Móvil)
                elif msg_type == "AIRSYNC_SHARE_FILE":
                    req_id = data.get("requestId", "")
                    f_id = data.get("fileId", "")
                    f_path = data.get("filePath", "")
                    ok = airsync_server.register_file(f_id, f_path)
                    resp = {
                        "type": "AIRSYNC_SHARE_FILE_RESP",
                        "requestId": req_id,
                        "success": ok,
                        "fileId": f_id,
                        "airsyncPort": getattr(config, "airsync_port", 8900)
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"⚡ Archivo registrado en AirSync: {f_id} -> {f_path} (Éxito: {ok})")

                # 36. Listar archivos compartibles por AirSync
                elif msg_type == "AIRSYNC_LIST_FILES":
                    req_id = data.get("requestId", "")
                    shared = airsync_server.get_shared_files()
                    resp = {
                        "type": "AIRSYNC_LIST_FILES_RESP",
                        "requestId": req_id,
                        "files": shared,
                        "airsyncPort": getattr(config, "airsync_port", 8900)
                    }
                    await websocket.send(json.dumps(resp))

                # 37. Simulación o reporte de error de terminal
                elif msg_type == "TERMINAL_SIMULATE_ERROR":
                    req_id = data.get("requestId", "")
                    sample_log = data.get("logText", "BUILD FAILED in 14s\nCompilation error in MainActivity.kt:42")
                    err = terminal_watchdog.analyze_log_content(sample_log, source_name="Terminal Test")
                    resp = {
                        "type": "TERMINAL_SIMULATE_ERROR_RESP",
                        "requestId": req_id,
                        "success": err is not None,
                        "error": err
                    }
                    await websocket.send(json.dumps(resp))

                # 38. Gestión de Procesos PC: Terminar / Forzar Cierre
                elif msg_type == "PC_KILL_PROCESS":
                    req_id = data.get("requestId", "")
                    proc_query = data.get("processName", "")
                    result = system_ops.kill_processes_by_name(proc_query)
                    resp = {
                        "type": "PC_KILL_PROCESS_RESP",
                        "requestId": req_id,
                        **result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🔪 Proceso terminado: {proc_query} (Éxito: {result.get('success', False)})")

                # 39. Gestión de Ventanas y Disposición Espacial
                elif msg_type == "PC_WINDOW_COMMAND":
                    req_id = data.get("requestId", "")
                    action = data.get("action", "")
                    result = system_ops.execute_window_command(action)
                    resp = {
                        "type": "PC_WINDOW_COMMAND_RESP",
                        "requestId": req_id,
                        **result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"🪟 Comando de ventana: {action} (Éxito: {result.get('success', False)})")

                # 40. Telemetría de Salud de Hardware (CPU, RAM, GPU, VRAM)
                elif msg_type == "PC_HARDWARE_HEALTH":
                    req_id = data.get("requestId", "")
                    telemetry = hardware_watchdog.get_hardware_telemetry()
                    resp = {
                        "type": "PC_HARDWARE_HEALTH_RESP",
                        "requestId": req_id,
                        "telemetry": telemetry
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity("🩺 Telemetría de salud de hardware enviada al móvil")

                # 41. Hendrix Vault: Guardar Copia de Respaldo en Dropzone PC
                elif msg_type == "VAULT_BACKUP_PUSH":
                    req_id = data.get("requestId", "")
                    vault_data = data.get("vaultData")
                    result = system_ops.save_vault_backup(vault_data)
                    resp = {
                        "type": "VAULT_BACKUP_PUSH_RESP",
                        "requestId": req_id,
                        **result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📦 Respaldo de Bóveda Hendrix guardado en PC: {result.get('filename')}")

                # 42. Hendrix Vault: Listar o Recuperar Copia de Respaldo
                elif msg_type == "VAULT_BACKUP_LIST":
                    req_id = data.get("requestId", "")
                    backups = system_ops.list_vault_backups()
                    resp = {
                        "type": "VAULT_BACKUP_LIST_RESP",
                        "requestId": req_id,
                        "backups": backups
                    }
                    await websocket.send(json.dumps(resp))

                elif msg_type == "VAULT_BACKUP_PULL":
                    req_id = data.get("requestId", "")
                    target_file = data.get("filename")
                    result = system_ops.read_vault_backup(target_file)
                    resp = {
                        "type": "VAULT_BACKUP_PULL_RESP",
                        "requestId": req_id,
                        **result
                    }
                    await websocket.send(json.dumps(resp))
                    log_activity(f"📦 Respaldo de Bóveda enviado al móvil: {result.get('filename')}")

    except websockets.exceptions.ConnectionClosed:
        log_activity(f"Dispositivo desconectado ({client_ip})")
    finally:
        active_websockets.discard(websocket)
        session_manager.unregister_connection(websocket)

async def run_server(port: int = config.port):
    global main_event_loop
    main_event_loop = asyncio.get_running_loop()
    sentinel_monitor.start()
    foreground_observer.start()

    def on_airsync_received(info):
        if main_event_loop and main_event_loop.is_running():
            asyncio.run_coroutine_threadsafe(
                broadcast({
                    "type": "AIRSYNC_FILE_RECEIVED",
                    "file": info
                }),
                main_event_loop
            )

    airsync_server.on_file_received = on_airsync_received
    airsync_server.start()
    log_activity(f"Iniciando Hendrix PC Bridge en ws://0.0.0.0:{port}/ws")
    async with websockets.serve(handle_client, "0.0.0.0", port):
        await asyncio.Future() # Mantener corriendo indefinidamente
