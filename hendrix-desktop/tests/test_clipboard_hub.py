import os
import shutil
import tempfile
import unittest
from storage.clipboard_hub import ClipboardHub

class TestClipboardHub(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp(prefix="hendrix_test_clipboard_")
        self.hub = ClipboardHub(max_history=5)
        # Limpiar historial inicial para tests
        self.hub.history.clear()

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_push_history_item(self):
        self.hub.push_history_item("Texto de prueba 1")
        self.assertEqual(len(self.hub.history), 1)
        self.assertEqual(self.hub.history[0]["text"], "Texto de prueba 1")
        self.assertEqual(self.hub.history[0]["char_count"], len("Texto de prueba 1"))
        self.assertIsNotNone(self.hub.history[0]["sha256"])

        # Evitar duplicados consecutivos
        self.hub.push_history_item("Texto de prueba 1")
        self.assertEqual(len(self.hub.history), 1)

        # Agregar otro texto
        self.hub.push_history_item("Texto de prueba 2")
        self.assertEqual(len(self.hub.history), 2)
        self.assertEqual(self.hub.history[0]["text"], "Texto de prueba 2")

    def test_history_max_capacity(self):
        for i in range(10):
            self.hub.push_history_item(f"Item {i}")
        self.assertLessEqual(len(self.hub.history), 5)
        self.assertEqual(self.hub.history[0]["text"], "Item 9")

    def test_clear_history(self):
        self.hub.push_history_item("Temporal")
        self.assertEqual(len(self.hub.history), 1)
        self.hub.clear_history()
        self.assertEqual(len(self.hub.history), 0)

    def test_snippets_crud(self):
        initial_count = len(self.hub.get_all_snippets())
        new_snip = self.hub.add_snippet(
            title="Mi Snippet Test",
            content="git status",
            category="TestCategory"
        )
        self.assertIsNotNone(new_snip.get("id"))
        self.assertEqual(len(self.hub.get_all_snippets()), initial_count + 1)

        # Filtrar por categoría
        filtered = self.hub.get_all_snippets("TestCategory")
        self.assertEqual(len(filtered), 1)
        self.assertEqual(filtered[0]["title"], "Mi Snippet Test")

        # Eliminar snippet
        deleted = self.hub.delete_snippet(new_snip["id"])
        self.assertTrue(deleted)
        self.assertEqual(len(self.hub.get_all_snippets()), initial_count)

if __name__ == "__main__":
    unittest.main()
