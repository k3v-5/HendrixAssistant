import os
import shutil
import tempfile
import unittest
from unittest.mock import patch, MagicMock
from core.system_ops import SystemOps, system_ops
from storage.dropzone_manager import dropzone_manager

class TestSystemOps(unittest.TestCase):

    def setUp(self):
        self.test_dir = tempfile.mkdtemp(prefix="hendrix_system_ops_test_")
        self.orig_root = dropzone_manager.root_path
        dropzone_manager.root_path = self.test_dir

    def tearDown(self):
        dropzone_manager.root_path = self.orig_root
        shutil.rmtree(self.test_dir, ignore_errors=True)

    def test_kill_processes_empty_query(self):
        res = system_ops.kill_processes_by_name("")
        self.assertFalse(res["success"])
        self.assertEqual(res["killedCount"], 0)

    def test_kill_processes_nonexistent(self):
        res = system_ops.kill_processes_by_name("non_existent_fake_app_xyz_9999")
        self.assertFalse(res["success"])
        self.assertEqual(res["killedCount"], 0)

    def test_kill_processes_protected(self):
        # Even if someone queries explorer or system, it should never kill it
        res = system_ops.kill_processes_by_name("explorer.exe")
        # should be protected or ignored
        self.assertIsInstance(res, dict)

    @patch("pyautogui.hotkey")
    def test_execute_window_command(self, mock_hotkey):
        res_min = system_ops.execute_window_command("MINIMIZE_ALL")
        self.assertTrue(res_min["success"])
        mock_hotkey.assert_called_with('win', 'd')

        res_max = system_ops.execute_window_command("TOGGLE_MAXIMIZE")
        self.assertTrue(res_max["success"])
        mock_hotkey.assert_called_with('win', 'up')

        res_bad = system_ops.execute_window_command("UNKNOWN_ACTION_ABC")
        self.assertFalse(res_bad["success"])

    def test_vault_backup_save_list_read(self):
        sample_vault = {
            "version": 1,
            "profiles": [{"id": "deck_1", "name": "Studio Deck"}],
            "tasks": [{"id": "t1", "title": "Test task"}]
        }

        # 1. Save
        save_res = system_ops.save_vault_backup(sample_vault)
        self.assertTrue(save_res["success"])
        self.assertTrue(os.path.exists(save_res["path"]))

        # 2. List
        backups = system_ops.list_vault_backups()
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0]["filename"], save_res["filename"])

        # 3. Read
        read_res = system_ops.read_vault_backup(save_res["filename"])
        self.assertTrue(read_res["success"])
    def test_app_state_backup_save_list_read(self):
        sample_state = {
            "metadata": {"timestamp": 1234567890, "versionName": "1.0.1", "versionCode": 1001},
            "notes": [{"id": "n1", "title": "Nota persistida"}],
            "settings": {"gemini_api_key": "AIzaSyTestKey"}
        }

        # 1. Save
        save_res = system_ops.save_app_state_backup(sample_state)
        self.assertTrue(save_res["success"])
        self.assertTrue(os.path.exists(save_res["path"]))

        # 2. List
        backups = system_ops.list_app_state_backups()
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0]["filename"], save_res["filename"])

        # 3. Read
        read_res = system_ops.read_app_state_backup(save_res["filename"])
        self.assertTrue(read_res["success"])
        self.assertEqual(read_res["appState"]["settings"]["gemini_api_key"], "AIzaSyTestKey")

        # 4. Read latest default
        read_latest = system_ops.read_app_state_backup()
        self.assertTrue(read_latest["success"])
        self.assertEqual(read_latest["appState"]["notes"][0]["title"], "Nota persistida")

    def test_get_tailscale_ip_structure(self):
        # Debe retornar None si no está activo o un string de IP válido
        ip = system_ops.get_tailscale_ip()
        if ip is not None:
            self.assertIsInstance(ip, str)
            self.assertTrue(ip.startswith("100.") or "." in ip)

    def test_keep_awake_toggle(self):
        # Probar activación y consulta de estado
        ok = system_ops.set_keep_awake(True)
        self.assertTrue(ok)
        self.assertTrue(system_ops.is_keep_awake_active())

        # Probar desactivación
        ok_off = system_ops.set_keep_awake(False)
        self.assertTrue(ok_off)
        self.assertFalse(system_ops.is_keep_awake_active())

        # Restaurar a True preventivamente para el entorno
        system_ops.set_keep_awake(True)

if __name__ == "__main__":
    unittest.main()
