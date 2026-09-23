import os
import tempfile
import shutil
import unittest
import config as cfg_module
from config import config
from core.session_manager import SessionManager

class TestSessionDevices(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.old_config_file = cfg_module.CONFIG_FILE
        cfg_module.CONFIG_FILE = os.path.join(self.temp_dir, "test_config.json")

        self.mgr = SessionManager()
        self.orig_tokens = list(config.paired_tokens)
        self.orig_devices = list(config.paired_devices)
        config.paired_tokens.clear()
        config.paired_devices.clear()
        config.pin = "123456"

    def tearDown(self):
        config.paired_tokens = self.orig_tokens
        config.paired_devices = self.orig_devices
        cfg_module.CONFIG_FILE = self.old_config_file
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_pairing_with_pin(self):
        # Intento con PIN erróneo
        token = self.mgr.authenticate_or_pair("999999", "", "Pixel 8 Pro", client_ip="192.168.1.50")
        self.assertIsNone(token)

        # Intento con PIN correcto
        token = self.mgr.authenticate_or_pair("123456", "", "Pixel 8 Pro", client_ip="192.168.1.50")
        self.assertIsNotNone(token)
        self.assertIn(token, config.paired_tokens)
        self.assertEqual(len(config.paired_devices), 1)
        self.assertEqual(config.paired_devices[0]["name"], "Pixel 8 Pro")

        # Re-autenticación con token previo
        token2 = self.mgr.authenticate_or_pair("", token, "Pixel 8 Pro Modificado", client_ip="192.168.1.51")
        self.assertEqual(token, token2)

    def test_get_all_paired_devices(self):
        tok1 = self.mgr.authenticate_or_pair("123456", "", "Dispositivo 1", client_ip="192.168.1.10")
        devices = self.mgr.get_all_paired_devices()
        self.assertEqual(len(devices), 1)
        self.assertEqual(devices[0]["name"], "Dispositivo 1")
        self.assertEqual(devices[0]["ip"], "192.168.1.10")

    def test_revoke_device(self):
        tok = self.mgr.authenticate_or_pair("123456", "", "Galaxy S24", client_ip="192.168.1.20")
        self.assertIn(tok, config.paired_tokens)

        revoked = self.mgr.revoke_device(tok)
        self.assertTrue(revoked)
        self.assertNotIn(tok, config.paired_tokens)
        self.assertEqual(len(config.paired_devices), 0)

if __name__ == "__main__":
    unittest.main()
