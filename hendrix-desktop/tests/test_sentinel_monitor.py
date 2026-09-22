import unittest
from unittest.mock import MagicMock
import time
from core.sentinel_monitor import SentinelMonitor

class TestSentinelMonitor(unittest.TestCase):

    def setUp(self):
        self.mock_watchdog = MagicMock()
        self.sentinel = SentinelMonitor(hardware_watchdog=self.mock_watchdog)

    def tearDown(self):
        self.sentinel.stop()

    def test_emit_alert_and_callback(self):
        received_alerts = []
        self.sentinel.register_callback(lambda alert: received_alerts.append(alert))

        alert = self.sentinel.emit_alert(
            category="TEST_ALERT",
            title="Prueba",
            message="Mensaje de prueba",
            severity="INFO",
            actions=[{"id": "test_action", "label": "Probar"}]
        )

        self.assertEqual(len(received_alerts), 1)
        self.assertEqual(received_alerts[0]["category"], "TEST_ALERT")
        self.assertEqual(received_alerts[0]["title"], "Prueba")
        self.assertEqual(len(received_alerts[0]["actions"]), 1)
        self.assertEqual(received_alerts[0]["actions"][0]["id"], "test_action")

    def test_render_completed_alert(self):
        received_alerts = []
        self.sentinel.register_callback(lambda alert: received_alerts.append(alert))

        event = {
            "processName": "blender.exe",
            "durationSeconds": 145,
            "peakGpuTemp": 78,
            "autoSuspendTriggered": True
        }
        self.sentinel.emit_render_completed_alert(event)

        self.assertEqual(len(received_alerts), 1)
        self.assertEqual(received_alerts[0]["category"], "RENDER_COMPLETED")
        self.assertIn("blender.exe", received_alerts[0]["title"])
        self.assertIn("Auto-suspensión", received_alerts[0]["message"])
        action_ids = [a["id"] for a in received_alerts[0]["actions"]]
        self.assertIn("suspend_pc", action_ids)
        self.assertIn("view_copilot", action_ids)

    def test_overheat_detection(self):
        received_alerts = []
        self.sentinel.register_callback(lambda alert: received_alerts.append(alert))
        self.sentinel.temp_threshold = 80
        self.sentinel.TEMP_TRIGGER_DURATION = 0.05 # Reducir para prueba rápida

        # 1. Telemetría normal
        self.mock_watchdog.get_hardware_telemetry.return_value = {
            "gpuTempCelsius": 70,
            "gpuName": "RTX 3080",
            "activeHeavyProcess": None
        }
        self.sentinel._check_thermal_state()
        self.assertEqual(len(received_alerts), 0)

        # 2. Telemetría sobrecalentada (inicio)
        self.mock_watchdog.get_hardware_telemetry.return_value = {
            "gpuTempCelsius": 86,
            "gpuName": "RTX 3080",
            "activeHeavyProcess": "blender.exe"
        }
        self.sentinel._check_thermal_state()
        self.assertEqual(len(received_alerts), 0) # Aún no pasa el tiempo mínimo

        # 3. Esperar duración mínima
        time.sleep(0.08)
        self.sentinel._check_thermal_state()
        self.assertEqual(len(received_alerts), 1)
        self.assertEqual(received_alerts[0]["category"], "GPU_OVERHEAT")
        self.assertIn("86°C", received_alerts[0]["title"])
        self.assertIn("blender.exe", received_alerts[0]["message"])

if __name__ == "__main__":
    unittest.main()
