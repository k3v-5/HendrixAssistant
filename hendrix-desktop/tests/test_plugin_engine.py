import os
import unittest
from core.plugin_engine import PluginEngine, HendrixPlugin

class TestPluginEngine(unittest.TestCase):

    def setUp(self):
        self.engine = PluginEngine()

    def test_default_plugins_discovered(self):
        meta = self.engine.get_all_plugins_metadata()
        plugin_ids = [p["id"] for p in meta]

        # Verificar que los 3 plugins de fábrica fueron descubiertos
        self.assertIn("backup_projects", plugin_ids)
        self.assertIn("temp_cleaner", plugin_ids)
        self.assertIn("system_diagnostics", plugin_ids)

    def test_plugin_metadata_structure(self):
        meta = self.engine.get_all_plugins_metadata()
        for p in meta:
            self.assertIn("id", p)
            self.assertIn("name", p)
            self.assertIn("description", p)
            self.assertIn("version", p)
            self.assertIn("author", p)
            self.assertIn("iconEmoji", p)
            self.assertIn("category", p)
            self.assertIn("actions", p)
            self.assertTrue(isinstance(p["actions"], list))

    def test_execute_valid_action(self):
        res = self.engine.execute_action(
            plugin_id="system_diagnostics",
            action_id="disk_space"
        )
        self.assertTrue(res.get("success"))
        self.assertIn("Disco C:", res.get("message", ""))
        self.assertEqual("system_diagnostics", res.get("pluginId"))
        self.assertEqual("disk_space", res.get("actionId"))
        self.assertIn("elapsedMs", res)

    def test_execute_invalid_plugin(self):
        res = self.engine.execute_action(
            plugin_id="plugin_fantasma_inexistente",
            action_id="action_x"
        )
        self.assertFalse(res.get("success"))
        self.assertIn("no encontrado", res.get("message", ""))

    def test_execute_invalid_action(self):
        res = self.engine.execute_action(
            plugin_id="system_diagnostics",
            action_id="accion_que_no_existe"
        )
        self.assertFalse(res.get("success"))
        self.assertIn("desconocida", res.get("message", ""))

    def test_hot_reload(self):
        initial_count = len(self.engine.plugins)
        reloaded = self.engine.reload_plugins()
        self.assertEqual(len(reloaded), initial_count)

if __name__ == "__main__":
    unittest.main()
