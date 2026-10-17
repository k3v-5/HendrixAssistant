import os
import tempfile
import unittest
from unittest.mock import patch, MagicMock

from core.platform.linux.linux_system_ops import LinuxSystemOps
from core.platform.linux.linux_window_manager import LinuxWindowManager
from core.platform.linux.linux_credential_vault import LinuxCredentialVault
from core.platform.linux.linux_network_diagnostic import LinuxNetworkDiagnostic

class TestLinuxAdapters(unittest.TestCase):

    def test_linux_credential_vault_crud(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            vault_file = os.path.join(tmpdir, "test_vault.enc")
            vault = LinuxCredentialVault(vault_path=vault_file)

            # 1. Guardar
            saved = vault.store_credential("gemini:antigravity", "user@test.com", "secret_jwt_token_123")
            self.assertTrue(saved)
            self.assertTrue(os.path.exists(vault_file))

            # 2. Leer
            cred = vault.read_credential("gemini:antigravity")
            self.assertIsNotNone(cred)
            self.assertEqual(cred[0], "user@test.com")
            self.assertEqual(cred[1], "secret_jwt_token_123")

            # 3. Leer inexistente
            none_cred = vault.read_credential("target_fantasma")
            self.assertIsNone(none_cred)

            # 4. Eliminar
            deleted = vault.delete_credential("gemini:antigravity")
            self.assertTrue(deleted)
            self.assertIsNone(vault.read_credential("gemini:antigravity"))

    def test_linux_system_ops_volume_bounds(self):
        ops = LinuxSystemOps()
        # Verificación con mocks para no alterar volumen real del host durante tests
        with patch("subprocess.run") as mock_run:
            mock_run.return_value = MagicMock(returncode=0)
            self.assertTrue(ops.set_volume(50))
            self.assertTrue(ops.set_volume(150)) # Clamp a 100
            self.assertTrue(ops.set_volume(-20)) # Clamp a 0
            self.assertTrue(ops.mute(True))
            self.assertTrue(ops.mute(False))

    def test_linux_window_manager_enumeration(self):
        wm = LinuxWindowManager()
        windows = wm.get_taskbar_windows()
        self.assertIsInstance(windows, list)
        if windows:
            w = windows[0]
            self.assertIn("hwnd", w)
            self.assertIn("title", w)
            self.assertIn("process", w)
            self.assertIn("isForeground", w)
            self.assertIsInstance(w["hwnd"], int)

    def test_linux_network_diagnostic(self):
        diag = LinuxNetworkDiagnostic()
        res = diag.check_firewall_rule("HendrixCompanion", 8765)
        self.assertIn("exists", res)
        self.assertIn("enabled", res)
        self.assertIn("firewall_type", res)

if __name__ == "__main__":
    unittest.main()
