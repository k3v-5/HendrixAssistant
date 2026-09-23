import os
import json
import logging
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn
from typing import Optional

from config import config
from core.system_ops import system_ops

logger = logging.getLogger("hendrix_desktop.ota_server")

DEFAULT_OTA_PORT = 8901

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True

class OtaRequestHandler(BaseHTTPRequestHandler):
    """
    Controlador HTTP para distribución de actualizaciones OTA en red local.
    """

    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Range")

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_cors_headers()
        self.end_headers()

    def do_HEAD(self):
        clean_path = self.path.split("?")[0].rstrip("/")
        if clean_path == "/api/update/download":
            apk_path = system_ops.get_app_apk_path()
            if not os.path.exists(apk_path):
                self.send_response(404)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_cors_headers()
                self.end_headers()
                return
            file_size = os.path.getsize(apk_path)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="hendrix-assistant-update.apk"')
            self.send_header("Content-Length", str(file_size))
            self.send_header("Accept-Ranges", "bytes")
            self.send_cors_headers()
            self.end_headers()
            return
        self.do_GET()

    def do_GET(self):
        clean_path = self.path.split("?")[0].rstrip("/")

        if clean_path in ("", "/health", "/api/health"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_cors_headers()
            self.end_headers()
            resp = {"status": "OK", "service": "Hendrix OTA Server", "version": "1.0"}
            self.wfile.write(json.dumps(resp).encode("utf-8"))
            return

        if clean_path in ("/api/update/latest", "/api/update/check"):
            apk_info = system_ops.get_app_apk_info()
            port = getattr(config, "ota_port", DEFAULT_OTA_PORT)
            apk_info["downloadUrl"] = f"http://{config.local_ip}:{port}/api/update/download"
            
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(json.dumps(apk_info).encode("utf-8"))
            return

        if clean_path == "/api/update/download":
            apk_path = system_ops.get_app_apk_path()
            if not os.path.exists(apk_path):
                self.send_response(404)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_cors_headers()
                self.end_headers()
                self.wfile.write(json.dumps({"error": "APK no encontrado en la PC"}).encode("utf-8"))
                return

            try:
                file_size = os.path.getsize(apk_path)
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", 'attachment; filename="app-debug.apk"')
                self.send_header("Content-Length", str(file_size))
                self.send_header("Accept-Ranges", "bytes")
                self.send_cors_headers()
                self.end_headers()

                with open(apk_path, "rb") as f:
                    while True:
                        chunk = f.read(64 * 1024)
                        if not chunk:
                            break
                        self.wfile.write(chunk)
                logger.info(f"✅ APK entregado exitosamente vía OTA a {self.client_address[0]} ({file_size // 1024} KB)")
            except (ConnectionResetError, BrokenPipeError):
                logger.warning(f"Descarga de APK cancelada o interrumpida por el cliente {self.client_address[0]}")
            except Exception as e:
                logger.error(f"Error sirviendo descarga OTA de APK: {e}")
            return

        self.send_response(404)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps({"error": "Ruta no encontrada"}).encode("utf-8"))

    def log_message(self, format, *args):
        # Suprimir logs verbosos de cada request HTTP
        pass

class OtaServer:
    """
    Servidor HTTP embebido en segundo plano para entrega de actualizaciones OTA a dispositivos Android.
    """
    def __init__(self, port: Optional[int] = None):
        self.port = port or getattr(config, "ota_port", DEFAULT_OTA_PORT)
        self._server: Optional[ThreadedHTTPServer] = None
        self._thread: Optional[threading.Thread] = None
        self._running = False

    def start(self):
        if self._running:
            return
        try:
            self._server = ThreadedHTTPServer(("0.0.0.0", self.port), OtaRequestHandler)
            self._running = True
            self._thread = threading.Thread(target=self._server.serve_forever, daemon=True, name="HendrixOtaServer")
            self._thread.start()
            logger.info(f"🚀 Servidor OTA escuchando en http://0.0.0.0:{self.port}")
        except Exception as e:
            logger.error(f"Error al iniciar servidor OTA en puerto {self.port}: {e}")

    def stop(self):
        if not self._running or not self._server:
            return
        self._running = False
        try:
            self._server.shutdown()
            self._server.server_close()
        except Exception:
            pass

ota_server = OtaServer()
