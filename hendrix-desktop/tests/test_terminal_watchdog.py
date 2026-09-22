import unittest
from core.terminal_watchdog import TerminalWatchdog

class TestTerminalWatchdog(unittest.TestCase):

    def setUp(self):
        self.watchdog = TerminalWatchdog()

    def test_detect_gradle_failure(self):
        sample_log = """
        > Task :app:compileDebugKotlin FAILED
        e: /app/MainActivity.kt: (42, 15): Unresolved reference: pcBridge
        BUILD FAILED in 12s
        84 actionable tasks: 12 executed
        """
        dispatched = []
        self.watchdog.register_callback(lambda p: dispatched.append(p))

        error = self.watchdog.analyze_log_content(sample_log, source_name="Android Studio")
        self.assertIsNotNone(error)
        self.assertEqual("Gradle", error["detectedTool"])
        self.assertIn("MainActivity.kt", error["logSnippet"])
        self.assertEqual(1, len(dispatched))
        self.assertEqual("TERMINAL_BUILD_ERROR", dispatched[0]["type"])

    def test_detect_python_traceback(self):
        sample_log = """
        Traceback (most recent call last):
          File "main.py", line 15, in <module>
            import non_existent_package
        ModuleNotFoundError: No module named 'non_existent_package'
        """
        error = self.watchdog.analyze_log_content(sample_log, source_name="Python Script")
        self.assertIsNotNone(error)
        self.assertEqual("Python", error["detectedTool"])

    def test_clean_log_ignored(self):
        sample_log = "BUILD SUCCESSFUL in 52s\n100 actionable tasks: 100 executed\n"
        error = self.watchdog.analyze_log_content(sample_log)
        self.assertIsNone(error)

    def test_recent_errors_history(self):
        self.watchdog.analyze_log_content("BUILD FAILED in 1s")
        self.watchdog.analyze_log_content("npm ERR! code E404")
        recent = self.watchdog.get_recent_errors()
        self.assertEqual(2, len(recent))

        self.watchdog.clear_errors()
        self.assertEqual(0, len(self.watchdog.get_recent_errors()))

if __name__ == "__main__":
    unittest.main()
