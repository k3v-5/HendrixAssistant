import os
import shutil
import tempfile
import unittest
from storage.dropzone_manager import DropzoneManager

class TestDropzoneManager(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp(prefix="hendrix_test_dropzone_")
        self.config_dir = tempfile.mkdtemp(prefix="hendrix_test_config_")

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)
        shutil.rmtree(self.config_dir, ignore_errors=True)

    def test_custom_path_configuration(self):
        manager = DropzoneManager(config_dir=self.config_dir)
        success = manager.set_dropzone_path(self.temp_dir)
        self.assertTrue(success)
        self.assertEqual(os.path.abspath(manager.root_path), os.path.abspath(self.temp_dir))

        # Check subdirectories were created
        self.assertTrue(os.path.exists(os.path.join(self.temp_dir, "Blender", "Renders")))
        self.assertTrue(os.path.exists(os.path.join(self.temp_dir, "Audio", "Exports")))

        # Test scan files
        test_file = os.path.join(self.temp_dir, "Blender", "Renders", "render_test_001.png")
        with open(test_file, "wb") as f:
            f.write(b"PNG_FAKE_DATA")

        files = manager.scan_recent_files(limit=10)
        self.assertEqual(len(files), 1)
        self.assertEqual(files[0]["fileName"], "render_test_001.png")
        self.assertEqual(files[0]["category"], "blender_renders")

    def test_status_info_structure(self):
        manager = DropzoneManager(config_dir=self.config_dir)
        manager.set_dropzone_path(self.temp_dir)
        status = manager.get_status_info()

        self.assertIn("rootPath", status)
        self.assertIn("cloudProvider", status)
        self.assertIn("isCloudSynced", status)
        self.assertIn("categories", status)
        self.assertIn("recentFiles", status)

if __name__ == "__main__":
    unittest.main()
