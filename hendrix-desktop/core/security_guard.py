import time
import hmac
import hashlib
import threading
from config import config

class HmacSecurityValidator:
    """
    Validador criptográfico HMAC-SHA256 con protección Anti-Replay para
    acciones y comandos entrantes desde el cliente móvil Hendrix.
    """
    FIELD_TIMESTAMP = "_ts"
    FIELD_NONCE = "_nonce"
    FIELD_SIGNATURE = "_sig"

    def __init__(self, time_provider=None):
        self._time_provider = time_provider or (lambda: int(time.time() * 1000))
        self._seen_nonces = {} # nonce -> timestamp_ms
        self._lock = threading.Lock()

    def build_canonical_string(self, payload: dict, ts: int, nonce: str) -> str:
        msg_type = str(payload.get("type", ""))
        sorted_keys = sorted(
            [k for k in payload.keys() if k not in (self.FIELD_TIMESTAMP, self.FIELD_NONCE, self.FIELD_SIGNATURE)]
        )
        sb = []
        for k in sorted_keys:
            val = payload.get(k)
            if val is None:
                formatted_val = ""
            elif isinstance(val, bool):
                formatted_val = "true" if val else "false"
            elif isinstance(val, float) and val.is_integer():
                formatted_val = str(int(val))
            else:
                formatted_val = str(val)
            sb.append(f"{k}={formatted_val};")
        body_str = "".join(sb)
        body_digest = hashlib.sha256(body_str.encode("utf-8")).hexdigest()
        return f"{msg_type}|{nonce}|{ts}|{body_digest}"

    def compute_signature(self, canonical_data: str, secret_key: str) -> str:
        return hmac.new(
            secret_key.encode("utf-8"),
            canonical_data.encode("utf-8"),
            hashlib.sha256
        ).hexdigest()

    def sign_payload(self, payload: dict, secret_key: str) -> dict:
        ts = self._time_provider()
        import uuid
        nonce = str(uuid.uuid4())
        payload[self.FIELD_TIMESTAMP] = ts
        payload[self.FIELD_NONCE] = nonce
        canonical = self.build_canonical_string(payload, ts, nonce)
        payload[self.FIELD_SIGNATURE] = self.compute_signature(canonical, secret_key)
        return payload

    def verify_payload(self, payload: dict, secret_key: str, tolerance_ms: int = 60000) -> tuple[bool, str]:
        """
        Retorna (is_valid, reason).
        """
        if self.FIELD_TIMESTAMP not in payload:
            return False, f"Falta metadato {self.FIELD_TIMESTAMP}"
        if self.FIELD_NONCE not in payload:
            return False, f"Falta metadato {self.FIELD_NONCE}"
        if self.FIELD_SIGNATURE not in payload:
            return False, f"Falta metadato {self.FIELD_SIGNATURE}"

        try:
            ts = int(payload[self.FIELD_TIMESTAMP])
        except (ValueError, TypeError):
            return False, "Timestamp inválido"

        nonce = str(payload[self.FIELD_NONCE])
        signature = str(payload[self.FIELD_SIGNATURE])

        if not nonce or not signature:
            return False, "Nonce o firma vacíos"

        now = self._time_provider()
        age = abs(now - ts)
        if age > tolerance_ms:
            return False, f"Petición expirada (edad: {age}ms, tolerancia: {tolerance_ms}ms)"

        with self._lock:
            # Poda periódica
            cutoff = now - (tolerance_ms * 2)
            expired = [n for n, t in self._seen_nonces.items() if t < cutoff]
            for n in expired:
                del self._seen_nonces[n]

            # Verificación Anti-Replay
            if nonce in self._seen_nonces:
                return False, f"Ataque de repetición detectado (Nonce ya procesado: {nonce})"

            self._seen_nonces[nonce] = now

        canonical = self.build_canonical_string(payload, ts, nonce)
        expected_sig = self.compute_signature(canonical, secret_key)

        if hmac.compare_digest(expected_sig, signature):
            return True, "OK"
        else:
            return False, "Firma digital HMAC-SHA256 no coincide"


hmac_validator = HmacSecurityValidator()


class SecurityGuard:
    @staticmethod
    def can_capture_screen() -> bool:
        return config.allow_screen_capture

    @staticmethod
    def can_inject_mouse() -> bool:
        return config.allow_mouse_control

    @staticmethod
    def can_inject_keyboard() -> bool:
        return config.allow_keyboard_control

    @staticmethod
    def can_execute_commands() -> bool:
        return config.allow_terminal_commands

    @staticmethod
    def verify_action_signature(payload: dict, secret_key: str, tolerance_ms: int = 60000) -> tuple[bool, str]:
        return hmac_validator.verify_payload(payload, secret_key, tolerance_ms)

