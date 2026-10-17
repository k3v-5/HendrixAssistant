import sys
import unittest
from core.platform import platform_factory
from core.platform.interfaces import (
    IWindowManager,
    IInputController,
    ISystemOps,
    ICredentialVault,
    INetworkDiagnostic,
)

class TestPlatformFactory(unittest.TestCase):
    def test_platform_detection(self):
        self.assertEqual(platform_factory.is_linux, sys.platform.startswith("linux"))
        self.assertEqual(platform_factory.is_windows, sys.platform == "win32")

    def test_window_manager_instance(self):
        wm = platform_factory.window_manager
        self.assertIsInstance(wm, IWindowManager)

    def test_input_controller_instance(self):
        ic = platform_factory.input_controller
        self.assertIsInstance(ic, IInputController)
        self.assertGreater(ic.screen_size[0], 0)
        self.assertGreater(ic.screen_size[1], 0)

    def test_system_ops_instance(self):
        ops = platform_factory.system_ops
        self.assertIsInstance(ops, ISystemOps)

    def test_credential_vault_instance(self):
        vault = platform_factory.credential_vault
        self.assertIsInstance(vault, ICredentialVault)

    def test_network_diagnostic_instance(self):
        diag = platform_factory.network_diagnostic
        self.assertIsInstance(diag, INetworkDiagnostic)

    def test_dependency_injection(self):
        class DummyWindowManager(IWindowManager):
            def get_taskbar_windows(self): return []
            def focus_window(self, id): return True
            def restore_window(self, id): return True
            def close_window(self, id): return True
            def get_foreground_window(self): return None
            def get_window_title(self, id): return "Dummy"
            def get_window_rect(self, id): return {"left": 0, "top": 0, "right": 100, "bottom": 100}

        original = platform_factory.window_manager
        dummy = DummyWindowManager()
        platform_factory.set_window_manager(dummy)
        self.assertEqual(platform_factory.window_manager, dummy)
        platform_factory.set_window_manager(original)

if __name__ == "__main__":
    unittest.main()
