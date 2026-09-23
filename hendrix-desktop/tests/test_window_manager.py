import unittest
from automation.window_manager import WindowManager, window_manager

class TestWindowManager(unittest.TestCase):

    def setUp(self):
        self.wm = window_manager

    def test_get_taskbar_windows_structure(self):
        windows = self.wm.get_taskbar_windows()
        self.assertIsInstance(windows, list)
        for w in windows:
            self.assertIn("hwnd", w)
            self.assertIn("title", w)
            self.assertIn("process", w)
            self.assertIn("isForeground", w)
            self.assertIsInstance(w["hwnd"], int)
            self.assertIsInstance(w["title"], str)
            self.assertIsInstance(w["process"], str)
            self.assertIsInstance(w["isForeground"], bool)

    def test_focus_invalid_window_returns_false(self):
        result = self.wm.focus_window(0)
        self.assertFalse(result)

        result_negative = self.wm.focus_window(-99999)
        self.assertFalse(result_negative)

    def test_minimize_invalid_window_returns_false(self):
        result = self.wm.minimize_window(0)
        self.assertFalse(result)

    def test_close_invalid_window_returns_false(self):
        result = self.wm.close_window(0)
        self.assertFalse(result)

if __name__ == "__main__":
    unittest.main()
