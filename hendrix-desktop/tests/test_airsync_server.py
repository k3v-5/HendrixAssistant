import unittest
import os
import tempfile
import socket
import json
import struct
import hashlib
from core.airsync_server import AirSyncServer

class TestAirSyncServer(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.test_file_path = os.path.join(self.temp_dir, "sample_render.mp4")
        # Generar archivo de prueba de 600 KB (más de 2 chunks de 256KB)
        self.file_content = b"HENDRIX_AIRSYNC_DATA_" * 28000
        with open(self.test_file_path, "wb") as f:
            f.write(self.file_content)

        # Usar puerto efímero libre
        self.server = AirSyncServer(port=0)

    def tearDown(self):
        self.server.stop()
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_register_and_query_file(self):
        registered = self.server.register_file("file_123", self.test_file_path)
        self.assertTrue(registered)
        self.assertIn("file_123", self.server.get_shared_files())

        # Probar query
        self.server.start()
        port = self.server._server_socket.getsockname()[1]

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", port))

        req = {
            "magic": "HASY",
            "command": "QUERY_FILE",
            "fileId": "file_123"
        }
        sock.sendall((json.dumps(req) + "\n").encode("utf-8"))

        sock_file = sock.makefile("rb")
        resp_line = sock_file.readline().decode("utf-8")
        resp = json.loads(resp_line)

        self.assertEqual("OK", resp.get("status"))
        self.assertEqual("file_123", resp.get("fileId"))
        self.assertEqual(len(self.file_content), resp.get("fileSize"))
        self.assertEqual("sample_render.mp4", resp.get("fileName"))
        sock.close()

    def test_pull_file_streaming(self):
        self.server.register_file("file_pull", self.test_file_path)
        self.server.start()
        port = self.server._server_socket.getsockname()[1]

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", port))

        chunk_size = 128 * 1024 # 128 KB chunks
        req = {
            "magic": "HASY",
            "command": "PULL_FILE",
            "fileId": "file_pull",
            "startChunk": 0,
            "chunkSize": chunk_size
        }
        sock.sendall((json.dumps(req) + "\n").encode("utf-8"))

        sock_file = sock.makefile("rb")
        ack_line = sock_file.readline().decode("utf-8")
        ack = json.loads(ack_line)
        self.assertEqual("STREAMING", ack.get("status"))
        total_chunks = ack.get("totalChunks")
        self.assertGreater(total_chunks, 1)

        received_bytes = bytearray()
        for i in range(total_chunks):
            header = sock_file.read(40) # 4 + 4 + 32
            if not header or len(header) < 40:
                break
            idx, length, sha = struct.unpack(">II32s", header)
            self.assertEqual(i, idx)

            chunk_data = sock_file.read(length)
            self.assertEqual(length, len(chunk_data))
            # Verificar checksum por bloque
            self.assertEqual(hashlib.sha256(chunk_data).digest(), sha)
            received_bytes.extend(chunk_data)

        self.assertEqual(self.file_content, bytes(received_bytes))
        sock.close()

    def test_push_file_streaming(self):
        received_events = []
        self.server.on_file_received = lambda evt: received_events.append(evt)
        self.server.start()
        port = self.server._server_socket.getsockname()[1]

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", port))

        chunk_size = 64 * 1024 # 64 KB
        upload_content = b"HENDRIX_UPLOAD_STREAMING_DATA_" * 4000 # ~124 KB
        total_chunks = (len(upload_content) + chunk_size - 1) // chunk_size

        req = {
            "magic": "HASY",
            "command": "PUSH_FILE",
            "fileName": "lens_test.jpg",
            "fileSize": len(upload_content),
            "chunkSize": chunk_size,
            "totalChunks": total_chunks,
            "copyToClipboard": False,
            "autoPaste": False,
            "targetDestination": "dropzone"
        }
        sock.sendall((json.dumps(req) + "\n").encode("utf-8"))

        sock_file = sock.makefile("rb")
        ack_line = sock_file.readline().decode("utf-8")
        ack = json.loads(ack_line)
        self.assertEqual("READY_TO_RECEIVE", ack.get("status"))

        for i in range(total_chunks):
            start = i * chunk_size
            end = min(start + chunk_size, len(upload_content))
            chunk_data = upload_content[start:end]
            chunk_hash = hashlib.sha256(chunk_data).digest()
            header = struct.pack(">II32s", i, len(chunk_data), chunk_hash)
            sock.sendall(header + chunk_data)

        completion_line = sock_file.readline().decode("utf-8")
        completion = json.loads(completion_line)
        self.assertEqual("COMPLETED", completion.get("status"))
        self.assertEqual(len(upload_content), completion.get("totalBytes"))
        self.assertEqual(1, len(received_events))
        self.assertEqual("lens_test.jpg", received_events[0].get("fileName"))
        saved_path = received_events[0].get("filePath")
        self.assertTrue(os.path.exists(saved_path))
        with open(saved_path, "rb") as f:
            self.assertEqual(upload_content, f.read())

        # Cleanup saved file
        if os.path.exists(saved_path):
            os.remove(saved_path)
        sock.close()

    def test_dropzone_path_customization(self):
        from config import config
        original_path = config.dropzone_path
        try:
            custom_dropzone = os.path.join(self.temp_dir, "CustomStudioDropzone")
            config.dropzone_path = custom_dropzone

            self.server.start()
            port = self.server._server_socket.getsockname()[1]

            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect(("127.0.0.1", port))

            chunk_size = 32 * 1024
            upload_content = b"CUSTOM_DROPZONE_TEST_DATA"
            req = {
                "magic": "HASY",
                "command": "PUSH_FILE",
                "fileName": "custom_asset.png",
                "fileSize": len(upload_content),
                "chunkSize": chunk_size,
                "totalChunks": 1,
                "targetDestination": "Renders"
            }
            sock.sendall((json.dumps(req) + "\n").encode("utf-8"))

            sock_file = sock.makefile("rb")
            ack_line = sock_file.readline().decode("utf-8")
            ack = json.loads(ack_line)
            self.assertEqual("READY_TO_RECEIVE", ack.get("status"))

            chunk_hash = hashlib.sha256(upload_content).digest()
            header = struct.pack(">II32s", 0, len(upload_content), chunk_hash)
            sock.sendall(header + upload_content)

            completion_line = sock_file.readline().decode("utf-8")
            completion = json.loads(completion_line)
            self.assertEqual("COMPLETED", completion.get("status"))

            expected_path = os.path.join(custom_dropzone, "Renders", "custom_asset.png")
            self.assertTrue(os.path.exists(expected_path))
            with open(expected_path, "rb") as f:
                self.assertEqual(upload_content, f.read())

            sock.close()
        finally:
            config.dropzone_path = original_path

if __name__ == "__main__":
    unittest.main()
