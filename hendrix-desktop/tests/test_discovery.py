import json
import re
import socket
import time
import unittest
from core.discovery_server import (
    DiscoveryServer,
    get_mac_address,
    get_local_ip,
    PING_MESSAGE,
    PONG_PREFIX
)


class TestDiscoveryServer(unittest.TestCase):

    def test_mac_address_format(self):
        mac = get_mac_address()
        self.assertIsInstance(mac, str)
        # MAC format XX:XX:XX:XX:XX:XX
        pattern = r"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
        self.assertTrue(re.match(pattern, mac), f"MAC '{mac}' no cumple con el formato estándar")

    def test_local_ip_resolution(self):
        ip = get_local_ip()
        self.assertIsInstance(ip, str)
        self.assertTrue(len(ip.split(".")) == 4, f"IP '{ip}' inválida")

    def test_payload_structure(self):
        server = DiscoveryServer(ws_port=9000, airsync_port=9001)
        payload = server.get_payload_dict()
        self.assertEqual(payload["service"], "hendrix-workspace")
        self.assertEqual(payload["version"], "1.0")
        self.assertEqual(payload["ws_port"], 9000)
        self.assertEqual(payload["airsync_port"], 9001)
        self.assertIn("hostname", payload)
        self.assertIn("ip", payload)
        self.assertIn("mac", payload)

    def test_discovery_ping_pong_response(self):
        # Use an ephemeral or dedicated test port
        test_port = 8799
        server = DiscoveryServer(ws_port=8765, discovery_port=test_port, beacon_interval=10.0)
        server.start()
        time.sleep(0.2)

        client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        client_sock.settimeout(2.0)
        try:
            client_sock.sendto(PING_MESSAGE, ("127.0.0.1", test_port))
            data, _ = client_sock.recvfrom(1024)
            msg = data.decode("utf-8")
            self.assertTrue(msg.startswith(PONG_PREFIX))
            json_str = msg[len(PONG_PREFIX):]
            payload = json.loads(json_str)
            self.assertEqual(payload["service"], "hendrix-workspace")
            self.assertEqual(payload["ws_port"], 8765)
            self.assertTrue(len(payload["mac"]) > 0)
        finally:
            client_sock.close()
            server.stop()


if __name__ == "__main__":
    unittest.main()
