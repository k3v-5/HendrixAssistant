import os
import time
import json
import hashlib
import logging
import psutil
import pyautogui
from typing import Dict, Any, List, Optional
from storage.dropzone_manager import dropzone_manager

logger = logging.getLogger("hendrix_desktop.system_ops")

PROTECTED_PROCESSES = {
    "system", "system idle process", "registry", "smss.exe", "csrss.exe", 
    "wininit.exe", "services.exe", "lsass.exe", "winlogon.exe", "explorer.exe",
    "svchost.exe", "dwm.exe", "taskmgr.exe", "spoolsv.exe"
}

class SystemOps:
    """
    Operaciones avanzadas del sistema Windows:
    - Cierre forzado / terminación de procesos por nombre o coincidencia
    - Gestión de ventanas y disposición multimonitor
    - Respaldo y restauración de la Bóveda Hendrix (Vault)
    """

    @staticmethod
    def kill_processes_by_name(query: str) -> Dict[str, Any]:
        """
        Busca y termina procesos que coincidan con el nombre dado (ej: 'chrome', 'blender', 'spotify').
        Protege procesos críticos del sistema operativo.
        """
        clean_query = query.strip().lower()
        if not clean_query:
            return {"success": False, "message": "Nombre de proceso vacío", "killedCount": 0, "memoryFreedMb": 0}

        killed_names = []
        total_freed_bytes = 0
        current_pid = os.getpid()

        for proc in psutil.process_iter(['pid', 'name', 'memory_info']):
            try:
                pinfo = proc.info
                pid = pinfo['pid']
                pname = (pinfo['name'] or "").lower()

                if pid == current_pid or pname in PROTECTED_PROCESSES:
                    continue

                if clean_query in pname or clean_query == pname.replace(".exe", ""):
                    mem = (pinfo.get('memory_info').rss if pinfo.get('memory_info') else 0)
                    total_freed_bytes += mem
                    killed_names.append(pinfo['name'])
                    proc.terminate()
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                continue
            except Exception as e:
                logger.warning(f"Error al terminar proceso {proc}: {e}")

        freed_mb = total_freed_bytes // (1024 * 1024)
        count = len(killed_names)

        if count > 0:
            msg = f"Se cerraron {count} procesos coincidentes con '{query}', liberando {freed_mb} MB de RAM."
            logger.info(msg)
            return {
                "success": True,
                "message": msg,
                "killedCount": count,
                "memoryFreedMb": freed_mb,
                "processes": killed_names[:10]
            }
        else:
            return {
                "success": False,
                "message": f"No se encontró ningún proceso activo coincidente con '{query}'.",
                "killedCount": 0,
                "memoryFreedMb": 0,
                "processes": []
            }

    @staticmethod
    def execute_window_command(action: str) -> Dict[str, Any]:
        """
        Ejecuta comandos de gestión de ventanas y disposición espacial.
        """
        action_upper = action.strip().upper()
        try:
            if action_upper == "MINIMIZE_ALL":
                pyautogui.hotkey('win', 'd')
                return {"success": True, "action": action_upper, "message": "Todas las ventanas minimizadas (Mostrar Escritorio)."}
            elif action_upper == "TOGGLE_MAXIMIZE":
                pyautogui.hotkey('win', 'up')
                return {"success": True, "action": action_upper, "message": "Ventana maximizada."}
            elif action_upper == "MOVE_NEXT_MONITOR":
                pyautogui.hotkey('win', 'shift', 'right')
                return {"success": True, "action": action_upper, "message": "Ventana movida al siguiente monitor."}
            elif action_upper == "SNAP_LEFT":
                pyautogui.hotkey('win', 'left')
                return {"success": True, "action": action_upper, "message": "Ventana acoplada a la izquierda."}
            elif action_upper == "SNAP_RIGHT":
                pyautogui.hotkey('win', 'right')
                return {"success": True, "action": action_upper, "message": "Ventana acoplada a la derecha."}
            elif action_upper == "CLOSE_ACTIVE":
                pyautogui.hotkey('alt', 'f4')
                return {"success": True, "action": action_upper, "message": "Ventana activa cerrada."}
            else:
                return {"success": False, "action": action_upper, "message": f"Acción de ventana desconocida: {action}"}
        except Exception as e:
            logger.error(f"Error al ejecutar comando de ventana {action}: {e}")
            return {"success": False, "action": action_upper, "message": f"Error al ejecutar: {e}"}

    @staticmethod
    def save_vault_backup(vault_data: Any) -> Dict[str, Any]:
        """
        Almacena una copia de respaldo de la Bóveda Hendrix en el Dropzone bajo la carpeta Backups.
        """
        try:
            root = dropzone_manager.root_path
            backup_dir = os.path.join(root, "Backups")
            os.makedirs(backup_dir, exist_ok=True)

            ts = int(time.time())
            filename = f"hendrix_vault_{ts}.json"
            target_path = os.path.join(backup_dir, filename)

            if isinstance(vault_data, str):
                try:
                    parsed = json.loads(vault_data)
                    with open(target_path, "w", encoding="utf-8") as f:
                        json.dump(parsed, f, indent=2, ensure_ascii=False)
                except Exception:
                    with open(target_path, "w", encoding="utf-8") as f:
                        f.write(vault_data)
            else:
                with open(target_path, "w", encoding="utf-8") as f:
                    json.dump(vault_data, f, indent=2, ensure_ascii=False)

            file_size = os.path.getsize(target_path)
            logger.info(f"Bóveda respaldada con éxito en {target_path} ({file_size} bytes)")
            return {
                "success": True,
                "filename": filename,
                "path": target_path,
                "timestamp": ts,
                "sizeBytes": file_size,
                "message": f"Respaldo guardado exitosamente en la PC: {filename}"
            }
        except Exception as e:
            logger.error(f"Error al guardar respaldo de bóveda: {e}")
            return {"success": False, "message": f"Error al guardar respaldo: {e}"}

    @staticmethod
    def list_vault_backups() -> List[Dict[str, Any]]:
        """
        Lista los respaldos disponibles en la carpeta Backups del Dropzone.
        """
        try:
            root = dropzone_manager.root_path
            backup_dir = os.path.join(root, "Backups")
            if not os.path.exists(backup_dir):
                return []

            results = []
            for fname in os.listdir(backup_dir):
                if fname.startswith("hendrix_vault_") and fname.endswith(".json"):
                    fpath = os.path.join(backup_dir, fname)
                    stat = os.stat(fpath)
                    results.append({
                        "filename": fname,
                        "sizeBytes": stat.st_size,
                        "modified": int(stat.st_mtime),
                        "path": fpath
                    })
            results.sort(key=lambda x: x["modified"], reverse=True)
            return results
        except Exception as e:
            logger.error(f"Error al listar respaldos: {e}")
            return []

    @staticmethod
    def read_vault_backup(filename: Optional[str] = None) -> Dict[str, Any]:
        """
        Lee el contenido de un respaldo específico o del más reciente si no se especifica.
        """
        try:
            backups = SystemOps.list_vault_backups()
            if not backups:
                return {"success": False, "message": "No hay respaldos disponibles en la PC."}

            target = None
            if filename:
                for b in backups:
                    if b["filename"] == filename:
                        target = b
                        break
            if not target:
                target = backups[0]

            with open(target["path"], "r", encoding="utf-8") as f:
                content = json.load(f)

            return {
                "success": True,
                "filename": target["filename"],
                "timestamp": target["modified"],
                "vault": content
            }
        except Exception as e:
            logger.error(f"Error al leer respaldo de bóveda: {e}")
            return {"success": False, "message": f"Error al leer respaldo: {e}"}

    @staticmethod
    def get_app_apk_path() -> str:
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        return os.path.abspath(os.path.join(base_dir, "..", "app", "build", "outputs", "apk", "debug", "app-debug.apk"))

    @staticmethod
    def get_app_apk_info() -> Dict[str, Any]:
        """
        Inspecciona el estado del APK compilado de la app móvil en la PC
        para entrega OTA instantánea.
        """
        apk_path = SystemOps.get_app_apk_path()
        if not os.path.exists(apk_path):
            return {
                "available": False,
                "message": "APK no compilado aún en la PC.",
                "path": apk_path
            }

        try:
            stat = os.stat(apk_path)
            size_bytes = stat.st_size
            mtime_ms = int(stat.st_mtime * 1000)

            # Cálculo de hash SHA-256 para validación de integridad OTA
            h = hashlib.sha256()
            with open(apk_path, "rb") as f:
                for chunk in iter(lambda: f.read(65536), b""):
                    h.update(chunk)
            sha256_hash = h.hexdigest()

            return {
                "available": True,
                "fileName": "app-debug.apk",
                "apkSizeBytes": size_bytes,
                "lastModifiedEpoch": mtime_ms,
                "sha256": sha256_hash,
                "path": apk_path,
                "airsyncFileId": "hendrix_latest_apk"
            }
        except Exception as e:
            logger.error(f"Error al inspeccionar APK para OTA: {e}")
            return {
                "available": False,
                "message": f"Error al inspeccionar APK: {e}",
                "path": apk_path
            }

system_ops = SystemOps()
