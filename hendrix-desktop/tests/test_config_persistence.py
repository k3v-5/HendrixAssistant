import os
import json
import tempfile
import shutil
import unittest
from config import DesktopConfig, DEFAULT_PORT, DEFAULT_AIRSYNC_PORT, DEFAULT_CHUNK_SIZE

class TestConfigPersistence(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.config_path = os.path.join(self.temp_dir, "test_hendrix_config.json")

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_pin_and_chunk_size_persistence(self):
        # 1. Simular primera carga y guardar un PIN manual específico
        import config as cfg_module
        old_config_file = cfg_module.CONFIG_FILE
        cfg_module.CONFIG_FILE = self.config_path

        try:
            cfg = DesktopConfig()
            cfg.pin = "998877"
            cfg.airsync_chunk_size = 512 * 1024
            cfg.dropzone_path = os.path.join(self.temp_dir, "CustomDropzone")
            cfg.save()

            # Verificar que el archivo JSON contiene los valores
            with open(self.config_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                self.assertEqual(data["pin"], "998877")
                self.assertEqual(data["airsync_chunk_size"], 512 * 1024)
                self.assertEqual(data["dropzone_path"], os.path.join(self.temp_dir, "CustomDropzone"))

            # 2. Simular un nuevo reinicio del proceso y verificar que no genera otro PIN
            cfg2 = DesktopConfig()
            self.assertEqual(cfg2.pin, "998877")
            self.assertEqual(cfg2.airsync_chunk_size, 512 * 1024)
            self.assertEqual(cfg2.dropzone_path, os.path.join(self.temp_dir, "CustomDropzone"))

        finally:
            cfg_module.CONFIG_FILE = old_config_file

if __name__ == "__main__":
    unittest.main()
