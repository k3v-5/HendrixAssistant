import unittest
from core.foreground_observer import ForegroundObserver

class TestForegroundObserver(unittest.TestCase):

    def setUp(self):
        self.observer = ForegroundObserver()

    def test_current_foreground_info(self):
        info = self.observer.get_current_foreground_info()
        self.assertIn("title", info)
        self.assertIn("process", info)
        self.assertIn("pid", info)

    def test_dispatch_change_callback(self):
        events = []
        self.observer.register_callback(lambda p: events.append(p))

        mock_info = {
            "title": "Blender [MyScene.blend]",
            "process": "blender.exe",
            "pid": 4321
        }
        self.observer._dispatch_change(mock_info)

        self.assertEqual(1, len(events))
        self.assertEqual("FOREGROUND_APP_CHANGED", events[0]["type"])
        self.assertEqual("blender.exe", events[0]["app"]["process"])

        # Unregister
        self.observer.unregister_callback(self.observer._callbacks[0])
        self.assertEqual(0, len(self.observer._callbacks))

if __name__ == "__main__":
    unittest.main()
