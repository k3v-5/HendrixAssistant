import unittest
from core.security_guard import HmacSecurityValidator, SecurityGuard

class TestSecurityGuard(unittest.TestCase):
    def setUp(self):
        self.secret_key = "test_super_secret_key_hendrix_12345"

    def test_sign_and_verify_valid_payload(self):
        simulated_time = 1000000
        validator = HmacSecurityValidator(time_provider=lambda: simulated_time)

        payload = {
            "type": "INPUT_ACTION",
            "actionType": "CLICK",
            "xRatio": 0.5,
            "yRatio": 0.8
        }

        signed = validator.sign_payload(payload, self.secret_key)
        self.assertIn("_ts", signed)
        self.assertIn("_nonce", signed)
        self.assertIn("_sig", signed)

        is_valid, reason = validator.verify_payload(signed, self.secret_key)
        self.assertTrue(is_valid, f"Expected valid, got: {reason}")
        self.assertEqual("OK", reason)

    def test_verify_detects_tampered_payload(self):
        simulated_time = 1000000
        validator = HmacSecurityValidator(time_provider=lambda: simulated_time)

        payload = {
            "type": "QUICK_COMMAND",
            "command": "LOCK_WORKSTATION"
        }

        signed = validator.sign_payload(payload, self.secret_key)
        # Modificar payload tras firmar
        signed["command"] = "SHUTDOWN_FORCE"

        is_valid, reason = validator.verify_payload(signed, self.secret_key)
        self.assertFalse(is_valid)
        self.assertIn("no coincide", reason)

    def test_verify_rejects_expired_timestamp(self):
        time_holder = [1000000]
        validator = HmacSecurityValidator(time_provider=lambda: time_holder[0])

        payload = {"type": "TELEMETRY_REQUEST"}
        signed = validator.sign_payload(payload, self.secret_key)

        # Avanzar el tiempo 10 segundos (> tolerancia de 5000ms)
        time_holder[0] += 10000

        is_valid, reason = validator.verify_payload(signed, self.secret_key, tolerance_ms=5000)
        self.assertFalse(is_valid)
        self.assertIn("expirada", reason)

    def test_verify_rejects_replayed_nonce(self):
        simulated_time = 1000000
        validator = HmacSecurityValidator(time_provider=lambda: simulated_time)

        payload = {
            "type": "INPUT_ACTION",
            "actionType": "KEY_PRESS",
            "keyCode": 13
        }

        signed = validator.sign_payload(payload, self.secret_key)

        # Primer pase debe ser válido
        is_valid1, _ = validator.verify_payload(signed, self.secret_key)
        self.assertTrue(is_valid1)

        # Segundo pase con el mismo paquete debe ser detectado como repetición
        is_valid2, reason2 = validator.verify_payload(signed, self.secret_key)
        self.assertFalse(is_valid2)
        self.assertIn("repetición", reason2)

    def test_verify_rejects_wrong_secret_key(self):
        simulated_time = 1000000
        validator = HmacSecurityValidator(time_provider=lambda: simulated_time)

        payload = {"type": "KILL_PROCESS", "pid": 1234}
        signed = validator.sign_payload(payload, "secret_key_alpha")

        is_valid, reason = validator.verify_payload(signed, "secret_key_beta")
        self.assertFalse(is_valid)
        self.assertIn("no coincide", reason)

    def test_verify_missing_metadata(self):
        validator = HmacSecurityValidator()
        payload = {"type": "TEST"}
        is_valid, reason = validator.verify_payload(payload, self.secret_key)
        self.assertFalse(is_valid)
        self.assertIn("Falta metadato", reason)

if __name__ == "__main__":
    unittest.main()
