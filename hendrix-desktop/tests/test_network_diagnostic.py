import socket
import unittest
import threading
from core.network_diagnostic import NetworkDiagnostic

class TestNetworkDiagnostic(unittest.TestCase):

    def test_check_port_listening(self):
        # Crear un socket temporal para simular puerto abierto
        server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_sock.bind(("127.0.0.1", 0))
        server_sock.listen(1)
        test_port = server_sock.getsockname()[1]

        try:
            is_listening = NetworkDiagnostic.check_port_listening(test_port)
            self.assertTrue(is_listening)
        finally:
            server_sock.close()

        # Puerto que ya no está abierto
        is_closed = NetworkDiagnostic.check_port_listening(test_port)
        self.assertFalse(is_closed)

    def test_get_lan_info(self):
        class DummyConfig:
            local_ip = "192.168.1.100"
            port = 8899
            remote_tunnel_url = "https://tunnel.hendrix.org"

        info = NetworkDiagnostic.get_lan_info(DummyConfig())
        self.assertEqual(info["local_ip"], "192.168.1.100")
        self.assertEqual(info["port"], 8899)
        self.assertEqual(info["tunnel_url"], "https://tunnel.hendrix.org")
        self.assertIn("hostname", info)

    def test_check_windows_firewall_rule_structure(self):
        res = NetworkDiagnostic.check_windows_firewall_rule("ReglaInexistentePrueba123")
        self.assertIn("exists", res)
        self.assertIn("enabled", res)
        self.assertIn("message", res)
        self.assertFalse(res["exists"])

if __name__ == "__main__":
    unittest.main()
