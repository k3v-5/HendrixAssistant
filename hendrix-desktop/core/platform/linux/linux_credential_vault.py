"""
Hendrix Desktop Companion - Linux Encrypted Credential Vault
Implementación de ICredentialVault para Linux mediante cifrado Fernet con derivación de clave local.
"""
import os
import json
import base64
import hashlib
from typing import Optional, Tuple, Dict
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes
from core.platform.interfaces import ICredentialVault

class LinuxCredentialVault(ICredentialVault):
    def __init__(self, vault_path: Optional[str] = None):
        if vault_path:
            self._vault_path = vault_path
        else:
            base_dir = os.path.expanduser("~/.hendrix")
            os.makedirs(base_dir, exist_ok=True)
            self._vault_path = os.path.join(base_dir, "vault.enc")
        
        self._fernet = self._init_fernet()

    def _init_fernet(self) -> Fernet:
        # Derivar una clave segura local basada en machine-id o identificadores del sistema
        seed = b"hendrix-default-salt"
        machine_id_paths = ["/etc/machine-id", "/var/lib/dbus/machine-id"]
        for p in machine_id_paths:
            if os.path.exists(p):
                try:
                    with open(p, "rb") as f:
                        seed = f.read().strip()
                        break
                except Exception:
                    pass

        user = os.environ.get("USER", "hendrix_user").encode()
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=hashlib.sha256(user + b"_salt").digest(),
            iterations=100000,
        )
        key = base64.urlsafe_b64encode(kdf.derive(seed))
        return Fernet(key)

    def _load_data(self) -> Dict[str, Dict[str, str]]:
        if not os.path.exists(self._vault_path):
            return {}
        try:
            with open(self._vault_path, "rb") as f:
                enc_data = f.read()
            if not enc_data:
                return {}
            dec_data = self._fernet.decrypt(enc_data)
            return json.loads(dec_data.decode("utf-8"))
        except Exception:
            return {}

    def _save_data(self, data: Dict[str, Dict[str, str]]) -> bool:
        try:
            raw = json.dumps(data).encode("utf-8")
            enc = self._fernet.encrypt(raw)
            with open(self._vault_path, "wb") as f:
                f.write(enc)
            # Asegurar permisos restringidos (solo usuario)
            os.chmod(self._vault_path, 0o600)
            return True
        except Exception:
            return False

    def store_credential(self, target_name: str, username: str, secret: str) -> bool:
        try:
            data = self._load_data()
            data[target_name] = {
                "username": username,
                "secret": secret
            }
            return self._save_data(data)
        except Exception:
            return False

    def read_credential(self, target_name: str) -> Optional[Tuple[str, str]]:
        try:
            data = self._load_data()
            if target_name in data:
                entry = data[target_name]
                return (entry["username"], entry["secret"])
        except Exception:
            pass
        return None

    def delete_credential(self, target_name: str) -> bool:
        try:
            data = self._load_data()
            if target_name in data:
                del data[target_name]
                return self._save_data(data)
        except Exception:
            pass
        return False
