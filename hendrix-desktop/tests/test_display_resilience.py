"""
Pruebas unitarias de resiliencia gráfica para Linux y modo headless.
Valida la detección de DISPLAY y la degradación elegante de PyAutoGUI.
"""
import os
import sys
import unittest
from unittest.mock import patch, MagicMock

from core.platform.linux.display_env import ensure_linux_display, _can_connect_display
from core.safe_pyautogui import ensure_safe_pyautogui, _DummyPyAutoGUI

class TestDisplayResilience(unittest.TestCase):
    def setUp(self):
        self._orig_display = os.environ.get("DISPLAY")

    def tearDown(self):
        if self._orig_display is not None:
            os.environ["DISPLAY"] = self._orig_display
        elif "DISPLAY" in os.environ:
            del os.environ["DISPLAY"]

    def test_ensure_linux_display_returns_string_or_none(self):
        if not sys.platform.startswith("linux"):
            self.skipTest("Solo aplicable en Linux")
        result = ensure_linux_display()
        if result is not None:
            self.assertTrue(result.startswith(":"))
            self.assertEqual(os.environ.get("DISPLAY"), result)

    def test_can_connect_display_with_invalid_display(self):
        # Un display inexistente debe retornar False sin lanzar excepción
        self.assertFalse(_can_connect_display(":999"))

    def test_safe_pyautogui_dummy_methods(self):
        dummy = _DummyPyAutoGUI()
        self.assertEqual(dummy.size().width, 1920)
        self.assertEqual(dummy.size().height, 1080)
        self.assertEqual(dummy.position().x, 0)
        self.assertEqual(dummy.position().y, 0)
        # Métodos no deben lanzar excepción
        dummy.moveTo(100, 200)
        dummy.moveRel(10, 10)
        dummy.click(100, 200)
        dummy.doubleClick(100, 200)
        dummy.rightClick(100, 200)
        dummy.mouseDown(100, 200)
        dummy.mouseUp(100, 200)
        dummy.press("enter")
        dummy.hotkey("ctrl", "c")
        dummy.write("test")
        dummy.scroll(5)
        self.assertIsNone(dummy.screenshot())

    def test_ensure_safe_pyautogui_returns_usable_instance(self):
        pag = ensure_safe_pyautogui()
        self.assertIsNotNone(pag)
        self.assertTrue(hasattr(pag, "size"))
        self.assertTrue(hasattr(pag, "click"))
        self.assertTrue(hasattr(pag, "hotkey"))

if __name__ == "__main__":
    unittest.main()
