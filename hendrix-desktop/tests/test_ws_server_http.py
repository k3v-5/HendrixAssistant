import os
import json
import unittest
import asyncio
from core.ws_server import _process_http_request
from core.tunnel_manager import tunnel_manager

class TestWsServerHttp(unittest.IsolatedAsyncioTestCase):

    async def test_health_check_endpoint(self):
        resp = await _process_http_request("/health", {})
        self.assertIsNotNone(resp)
        self.assertEqual(resp.status_code, 200)
        data = json.loads(resp.body.decode("utf-8"))
        self.assertEqual(data["status"], "OK")
        self.assertEqual(data["service"], "Hendrix PC Bridge")

    async def test_api_update_check_endpoint(self):
        resp = await _process_http_request("/api/update/latest", {})
        self.assertIsNotNone(resp)
        self.assertEqual(resp.status_code, 200)
        data = json.loads(resp.body.decode("utf-8"))
        self.assertIn("downloadUrl", data)

    async def test_ws_path_returns_none(self):
        # /ws should return None so that websockets upgrades to WebSocket connection
        resp = await _process_http_request("/ws", {})
        self.assertIsNone(resp)
