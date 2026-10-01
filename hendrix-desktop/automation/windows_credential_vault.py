import sys
import json
import base64
from typing import Dict, Any, Optional

if sys.platform == "win32":
    try:
        import ctypes
        from ctypes import wintypes

        class _CREDENTIAL(ctypes.Structure):
            _fields_ = [
                ('Flags', wintypes.DWORD),
                ('Type', wintypes.DWORD),
                ('TargetName', wintypes.LPWSTR),
                ('Comment', wintypes.LPWSTR),
                ('LastWritten', wintypes.FILETIME),
                ('CredentialBlobSize', wintypes.DWORD),
                ('CredentialBlob', ctypes.POINTER(ctypes.c_byte)),
                ('Persist', wintypes.DWORD),
                ('AttributeCount', wintypes.DWORD),
                ('Attributes', ctypes.c_void_p),
                ('TargetAlias', wintypes.LPWSTR),
                ('UserName', wintypes.LPWSTR),
            ]
    except Exception:
        _CREDENTIAL = None
else:
    _CREDENTIAL = None

class WindowsCredentialVault:
    """
    Abstracción nativa de Windows Credential Manager (advapi32.dll).
    Permite respaldar, restaurar y alternar credenciales de autenticación
    de Google Antigravity ('gemini:antigravity') de forma instantánea.
    """

    CRED_TYPE_GENERIC = 1
    CRED_PERSIST_LOCAL_MACHINE = 2
    CREDENTIAL = _CREDENTIAL

    def __init__(self):
        self._advapi32 = None
        if sys.platform == "win32":
            try:
                import ctypes
                self._advapi32 = ctypes.windll.advapi32
            except Exception:
                self._advapi32 = None

    @staticmethod
    def extract_jwt_email(id_token: str) -> Optional[str]:
        """Extrae el email de un ID Token JWT de Google sin verificar firma (solo parseo de payload)."""
        if not id_token or "." not in id_token:
            return None
        try:
            parts = id_token.split(".")
            if len(parts) >= 2:
                payload_b64 = parts[1]
                payload_b64 += "=" * (-len(payload_b64) % 4)
                payload_json = base64.urlsafe_b64decode(payload_b64).decode("utf-8", errors="ignore")
                payload = json.loads(payload_json)
                return payload.get("email")
        except Exception:
            pass
        return None

    def read_credential(self, target_name: str) -> Optional[Dict[str, Any]]:
        """
        Lee una credencial genérica de Windows Credential Manager.
        Retorna diccionario con target, user, blob_str, json_data y email detectado.
        """
        if not self._advapi32 or not self.CREDENTIAL:
            return None

        import ctypes
        from ctypes import wintypes

        CREDENTIAL = self.CREDENTIAL
        CredReadW = self._advapi32.CredReadW
        CredReadW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD, ctypes.POINTER(ctypes.POINTER(CREDENTIAL))]
        CredReadW.restype = wintypes.BOOL

        CredFree = self._advapi32.CredFree
        CredFree.argtypes = [ctypes.c_void_p]

        p_cred = ctypes.POINTER(CREDENTIAL)()
        if CredReadW(target_name, self.CRED_TYPE_GENERIC, 0, ctypes.byref(p_cred)):
            try:
                cred = p_cred.contents
                size = cred.CredentialBlobSize
                blob_bytes = bytes(ctypes.cast(cred.CredentialBlob, ctypes.POINTER(ctypes.c_char * size)).contents)
                blob_str = blob_bytes.decode("utf-8", errors="replace")

                json_data = None
                detected_email = ""
                try:
                    json_data = json.loads(blob_str)
                    if isinstance(json_data, dict):
                        id_token = json_data.get("id_token", "")
                        detected_email = self.extract_jwt_email(id_token) or ""
                except Exception:
                    pass

                return {
                    "target": cred.TargetName,
                    "userName": cred.UserName or "",
                    "blob": blob_str,
                    "jsonData": json_data,
                    "email": detected_email
                }
            finally:
                CredFree(p_cred)

        return None

    def write_credential(
        self,
        target_name: str,
        blob: str,
        user_name: str = "antigravity",
        persist: int = CRED_PERSIST_LOCAL_MACHINE
    ) -> bool:
        """
        Escribe o actualiza una credencial en Windows Credential Manager.
        """
        if not self._advapi32 or not self.CREDENTIAL:
            return False

        import ctypes
        from ctypes import wintypes

        CREDENTIAL = self.CREDENTIAL
        CredWriteW = self._advapi32.CredWriteW
        CredWriteW.argtypes = [ctypes.POINTER(CREDENTIAL), wintypes.DWORD]
        CredWriteW.restype = wintypes.BOOL

        blob_bytes = blob.encode("utf-8")
        buf = (ctypes.c_byte * len(blob_bytes))(*blob_bytes)

        cred = CREDENTIAL()
        cred.Flags = 0
        cred.Type = self.CRED_TYPE_GENERIC
        cred.TargetName = target_name
        cred.Comment = None
        cred.CredentialBlobSize = len(blob_bytes)
        cred.CredentialBlob = ctypes.cast(buf, ctypes.POINTER(ctypes.c_byte))
        cred.Persist = persist
        cred.AttributeCount = 0
        cred.Attributes = None
        cred.TargetAlias = None
        cred.UserName = user_name

        return bool(CredWriteW(ctypes.byref(cred), 0))

    def delete_credential(self, target_name: str) -> bool:
        """Elimina una credencial de Windows Credential Manager."""
        if not self._advapi32:
            return False

        import ctypes
        from ctypes import wintypes

        CredDeleteW = self._advapi32.CredDeleteW
        CredDeleteW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD]
        CredDeleteW.restype = wintypes.BOOL

        return bool(CredDeleteW(target_name, self.CRED_TYPE_GENERIC, 0))

windows_credential_vault = WindowsCredentialVault()
