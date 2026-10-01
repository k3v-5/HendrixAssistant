import os
import sys
import json
import shutil
import time
import subprocess
from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional
from automation.windows_credential_vault import windows_credential_vault

class AntigravityProfileManager:
    """
    Gestor de perfiles y sesiones de autenticación para Google Antigravity.
    Permite respaldar, alternar y rotar múltiples cuentas de Gemini Pro
    almacenando los datos esenciales de sesión (%APPDATA%\\Antigravity)
    y las credenciales OAuth en Windows Credential Vault ('gemini:antigravity').
    """

    ESSENTIAL_ITEMS = [
        "app_storage.json",
        "Preferences",
        "Local State",
        os.path.join("Network", "Cookies"),
        os.path.join("Network", "Cookies-journal"),
        os.path.join("Network", "Network Persistent State"),
        os.path.join("Network", "Trust Tokens"),
        os.path.join("Local Storage", "leveldb"),
        "Session Storage",
    ]

    def __init__(
        self,
        base_profiles_dir: Optional[str] = None,
        appdata_dir: Optional[str] = None,
        vault=None
    ):
        self.profiles_dir = base_profiles_dir or os.path.expanduser(r"~\.antigravity-profiles")
        self.appdata_dir = appdata_dir or os.path.expandvars(r"%APPDATA%\Antigravity")
        self.vault = vault or windows_credential_vault
        self.manifest_file = os.path.join(self.profiles_dir, "profiles_manifest.json")
        os.makedirs(self.profiles_dir, exist_ok=True)
        self._ensure_manifest()

    def _ensure_manifest(self):
        if not os.path.exists(self.manifest_file):
            default_manifest = {
                "active_profile": None,
                "profiles": {}
            }
            self._save_manifest(default_manifest)

    def _load_manifest(self) -> Dict[str, Any]:
        try:
            with open(self.manifest_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {"active_profile": None, "profiles": {}}

    def _save_manifest(self, data: Dict[str, Any]):
        try:
            with open(self.manifest_file, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"[AntigravityProfileManager] Error guardando manifiesto: {e}")

    def list_profiles(self) -> List[Dict[str, Any]]:
        """
        Devuelve la lista detallada de perfiles registrados con su estado de cuota y último uso.
        """
        manifest = self._load_manifest()
        active = manifest.get("active_profile")
        now = datetime.now(timezone.utc)
        result = []

        for name, info in manifest.get("profiles", {}).items():
            cooldown_until_str = info.get("cooldown_until")
            in_cooldown = False
            remaining_seconds = 0.0

            if cooldown_until_str:
                try:
                    dt = datetime.fromisoformat(cooldown_until_str)
                    if dt > now:
                        in_cooldown = True
                        remaining_seconds = (dt - now).total_seconds()
                except Exception:
                    pass

            has_cred = os.path.exists(os.path.join(self.profiles_dir, name, "credential.json"))

            result.append({
                "name": name,
                "email": info.get("email", ""),
                "isActive": name == active,
                "hasCredential": has_cred,
                "inCooldown": in_cooldown,
                "cooldownRemainingSeconds": int(remaining_seconds),
                "cooldownUntil": cooldown_until_str,
                "lastUsed": info.get("last_used"),
                "usageCount": info.get("usage_count", 0),
                "path": os.path.join(self.profiles_dir, name)
            })

        return sorted(result, key=lambda x: (x["inCooldown"], -(x["usageCount"] or 0)))

    def get_active_profile(self) -> Optional[str]:
        manifest = self._load_manifest()
        return manifest.get("active_profile")

    def _extract_email_from_appdata(self, source_dir: str) -> str:
        """Intenta extraer el correo electrónico del usuario desde app_storage.json."""
        storage_path = os.path.join(source_dir, "app_storage.json")
        if os.path.exists(storage_path):
            try:
                with open(storage_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    val = data.get("jetski.onboarding.lastLoginUsername")
                    if val and "@" in str(val):
                        return str(val)
            except Exception:
                pass
        return ""

    def is_antigravity_running(self) -> bool:
        """Comprueba si algún proceso de Antigravity.exe está en ejecución."""
        try:
            import psutil
            for p in psutil.process_iter(['name']):
                try:
                    pname = p.info.get('name') or ''
                    if pname.lower() == 'antigravity.exe':
                        return True
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
        except Exception:
            pass
        return False

    def capture_current_profile(self, name: str, email: Optional[str] = None, close_running: bool = False) -> bool:
        """
        Copia los archivos esenciales de sesión de %APPDATA%\\Antigravity
        hacia la carpeta del perfil identificado por `name`.
        Si `close_running` es True, cierra Antigravity para desbloquear Network\\Cookies.
        """
        if not os.path.exists(self.appdata_dir):
            print(f"[AntigravityProfileManager] Directorio AppData no encontrado: {self.appdata_dir}")
            return False

        if close_running and self.is_antigravity_running():
            print("[AntigravityProfileManager] Cerrando Antigravity temporalmente para desbloquear Network\\Cookies...")
            self.close_antigravity_processes()
            time.sleep(1.2)

        profile_path = os.path.join(self.profiles_dir, name)
        os.makedirs(profile_path, exist_ok=True)

        copied_count = 0
        for rel_path in self.ESSENTIAL_ITEMS:
            src = os.path.join(self.appdata_dir, rel_path)
            dst = os.path.join(profile_path, rel_path)

            if os.path.exists(src):
                try:
                    os.makedirs(os.path.dirname(dst), exist_ok=True)
                    if os.path.isdir(src):
                        if os.path.exists(dst):
                            shutil.rmtree(dst, ignore_errors=True)
                        shutil.copytree(src, dst)
                    else:
                        shutil.copy2(src, dst)
                    copied_count += 1
                except Exception as e:
                    print(f"[AntigravityProfileManager] Advertencia copiando {rel_path}: {e}")

        # Capturar credencial de Windows Credential Manager ('gemini:antigravity')
        cred_info = self.vault.read_credential("gemini:antigravity")
        cred_email = ""
        if cred_info:
            cred_file = os.path.join(profile_path, "credential.json")
            try:
                with open(cred_file, "w", encoding="utf-8") as f:
                    json.dump(cred_info, f, indent=2, ensure_ascii=False)
                cred_email = cred_info.get("email") or ""
                print(f"[AntigravityProfileManager] Credencial de Windows Vault capturada ({cred_email or 'OAuth Token'})")
            except Exception as e:
                print(f"[AntigravityProfileManager] Error guardando credential.json: {e}")

        detected_email = email or cred_email or self._extract_email_from_appdata(self.appdata_dir)
        now_str = datetime.now(timezone.utc).isoformat()

        manifest = self._load_manifest()
        if "profiles" not in manifest:
            manifest["profiles"] = {}

        existing = manifest["profiles"].get(name, {})
        manifest["profiles"][name] = {
            "email": detected_email or existing.get("email", ""),
            "last_used": now_str,
            "usage_count": existing.get("usage_count", 0),
            "cooldown_until": existing.get("cooldown_until", None)
        }
        manifest["active_profile"] = name
        self._save_manifest(manifest)

        print(f"[AntigravityProfileManager] Perfil '{name}' capturado ({copied_count} elementos copiados, email={detected_email})")
        return copied_count > 0 or bool(cred_info)

    def close_antigravity_processes(self, timeout_sec: float = 5.0) -> bool:
        """Termina los procesos de Antigravity para liberar bloqueos de archivos de sesión."""
        try:
            subprocess.run(
                ["taskkill", "/F", "/IM", "Antigravity.exe"],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False
            )
            # Esperar a que todos los procesos de Antigravity.exe hayan finalizado
            start_t = time.time()
            while time.time() - start_t < timeout_sec:
                if not self.is_antigravity_running():
                    break
                time.sleep(0.3)

            # Si aún quedase alguno, forzar árbol de procesos
            if self.is_antigravity_running():
                subprocess.run(
                    ["taskkill", "/F", "/T", "/IM", "Antigravity.exe"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=False
                )
                time.sleep(0.5)

            time.sleep(0.8)
            return True
        except Exception as e:
            print(f"[AntigravityProfileManager] Error cerrando procesos: {e}")
            return False

    def activate_profile(self, name: str, kill_running: bool = True) -> bool:
        """
        Restaura el perfil indicado en %APPDATA%\\Antigravity para convertirlo en la sesión activa.
        """
        profile_path = os.path.join(self.profiles_dir, name)
        if not os.path.exists(profile_path):
            print(f"[AntigravityProfileManager] Perfil no existe: {name}")
            return False

        if kill_running:
            self.close_antigravity_processes()

        # Copiar elementos esenciales del perfil hacia AppData
        os.makedirs(self.appdata_dir, exist_ok=True)
        restored_count = 0

        for rel_path in self.ESSENTIAL_ITEMS:
            src = os.path.join(profile_path, rel_path)
            dst = os.path.join(self.appdata_dir, rel_path)

            if os.path.exists(src):
                try:
                    os.makedirs(os.path.dirname(dst), exist_ok=True)
                    if os.path.isdir(src):
                        if os.path.exists(dst):
                            shutil.rmtree(dst, ignore_errors=True)
                        shutil.copytree(src, dst)
                    else:
                        shutil.copy2(src, dst)
                    restored_count += 1
                except Exception as e:
                    print(f"[AntigravityProfileManager] Error restaurando {rel_path}: {e}")

        # Restaurar credencial en Windows Credential Manager ('gemini:antigravity')
        cred_restored = False
        cred_file = os.path.join(profile_path, "credential.json")
        if os.path.exists(cred_file):
            try:
                with open(cred_file, "r", encoding="utf-8") as f:
                    cred_data = json.load(f)
                blob = cred_data.get("blob", "")
                user = cred_data.get("userName", "antigravity")
                target = cred_data.get("target", "gemini:antigravity")
                if blob:
                    cred_restored = self.vault.write_credential(target, blob, user_name=user)
                    if cred_restored:
                        print(f"[AntigravityProfileManager] Credencial OAuth de Windows Vault restaurada para '{name}'")
                    else:
                        print(f"[AntigravityProfileManager] Advertencia: No se pudo escribir en Windows Vault")
            except Exception as e:
                print(f"[AntigravityProfileManager] Error restaurando credencial de Windows: {e}")

        manifest = self._load_manifest()
        now_str = datetime.now(timezone.utc).isoformat()
        if name in manifest.get("profiles", {}):
            manifest["profiles"][name]["last_used"] = now_str
            manifest["profiles"][name]["usage_count"] = manifest["profiles"][name].get("usage_count", 0) + 1
        manifest["active_profile"] = name
        self._save_manifest(manifest)

        print(f"[AntigravityProfileManager] Perfil '{name}' activado con éxito ({restored_count} archivos aplicados, oauth={cred_restored})")
        return restored_count > 0 or cred_restored

    def mark_quota_exhausted(self, name: str, cooldown_hours: float = 4.0):
        """Marca un perfil como agotado de cuota con una hora de expiración de enfriamiento."""
        manifest = self._load_manifest()
        if name in manifest.get("profiles", {}):
            cooldown_until = datetime.now(timezone.utc) + timedelta(hours=cooldown_hours)
            manifest["profiles"][name]["cooldown_until"] = cooldown_until.isoformat()
            self._save_manifest(manifest)
            print(f"[AntigravityProfileManager] Perfil '{name}' marcado con cuota agotada hasta {cooldown_until.isoformat()}")

    def get_next_available_profile(self, exclude_current: bool = True) -> Optional[str]:
        """
        Busca el siguiente perfil que no esté en cooldown y que tenga menor uso reciente.
        """
        manifest = self._load_manifest()
        active = manifest.get("active_profile")
        now = datetime.now(timezone.utc)
        candidates = []

        for name, info in manifest.get("profiles", {}).items():
            if exclude_current and name == active:
                continue

            cooldown_until_str = info.get("cooldown_until")
            if cooldown_until_str:
                try:
                    dt = datetime.fromisoformat(cooldown_until_str)
                    if dt > now:
                        continue # En cooldown, descartar por ahora
                except Exception:
                    pass

            candidates.append((name, info.get("usage_count", 0), info.get("last_used") or ""))

        if not candidates:
            return None

        # Ordenar por el que tenga menor conteo de uso y uso más antiguo
        candidates.sort(key=lambda x: (x[1], x[2]))
        return candidates[0][0]

    def delete_profile(self, name: str) -> bool:
        """Elimina un perfil registrado."""
        profile_path = os.path.join(self.profiles_dir, name)
        if os.path.exists(profile_path):
            shutil.rmtree(profile_path, ignore_errors=True)

        manifest = self._load_manifest()
        if name in manifest.get("profiles", {}):
            del manifest["profiles"][name]
            if manifest.get("active_profile") == name:
                manifest["active_profile"] = None
            self._save_manifest(manifest)
            return True
        return False

antigravity_profile_manager = AntigravityProfileManager()
