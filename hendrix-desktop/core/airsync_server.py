import os
import socket
import struct
import hashlib
import json
import threading
import logging
import time
import uuid
from typing import Optional, Dict, Any
from config import config

logger = logging.getLogger("hendrix_desktop.airsync")

DEFAULT_CHUNK_SIZE = 256 * 1024 # 256 KB por bloque para equilibrio perfecto de latencia y throughput

class AirSyncServer:
    """
    Servidor de transferencia de archivos punto a punto (P2P) de ultra alta velocidad
    para Hendrix Studio (Hendrix AirSync).
    Opera en un socket TCP nativo dedicado (puerto 8900 por defecto) sin pasar por internet,
    con verificación de integridad SHA-256 por bloque y soporte para reanudación de descargas interrumpidas.
    """

    MAGIC = "HASY"

    def __init__(self, port: Optional[int] = None):
        self.port = port if port is not None else getattr(config, "airsync_port", 8900)
        self.default_chunk_size = getattr(config, "airsync_chunk_size", DEFAULT_CHUNK_SIZE)
        self._server_socket: Optional[socket.socket] = None
        self._running = False
        self._listen_thread: Optional[threading.Thread] = None

        # Almacén de archivos compartibles: file_id -> file_path
        self._shared_files: Dict[str, str] = {}
        self._active_transfers: Dict[str, Dict[str, Any]] = {}
        self.on_file_received = None

    def register_file(self, file_id: str, file_path: str) -> bool:
        """Registra un archivo local en el servidor AirSync para que pueda ser descargado por el móvil."""
        if os.path.exists(file_path):
            self._shared_files[file_id] = os.path.abspath(file_path)
            return True
        return False

    def unregister_file(self, file_id: str):
        self._shared_files.pop(file_id, None)

    def get_shared_files(self) -> Dict[str, str]:
        return dict(self._shared_files)

    def start(self):
        """Inicia el socket TCP de AirSync en segundo plano."""
        if self._running:
            return
        self._running = True

        self._server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # Habilitar buffer TCP grande para throughput máximo
        try:
            self._server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 1024 * 1024)
            self._server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 1024 * 1024)
        except Exception:
            pass

        self._server_socket.bind(("0.0.0.0", self.port))
        self._server_socket.listen(5)

        self._listen_thread = threading.Thread(target=self._accept_loop, daemon=True, name="AirSyncAcceptThread")
        self._listen_thread.start()
        logger.info(f"⚡ Hendrix AirSync TCP Server escuchando en 0.0.0.0:{self.port}")

    def stop(self):
        """Detiene el servidor TCP de AirSync."""
        self._running = False
        if self._server_socket:
            try:
                self._server_socket.close()
            except Exception:
                pass
            self._server_socket = None

        if self._listen_thread and self._listen_thread.is_alive():
            self._listen_thread.join(timeout=1.0)
        logger.info("⚡ Hendrix AirSync Server detenido")

    def _accept_loop(self):
        while self._running:
            try:
                if not self._server_socket:
                    break
                client_sock, client_addr = self._server_socket.accept()
                client_thread = threading.Thread(
                    target=self._handle_client,
                    args=(client_sock, client_addr),
                    daemon=True,
                    name=f"AirSyncClient-{client_addr[0]}"
                )
                client_thread.start()
            except Exception as e:
                if self._running:
                    logger.debug(f"Error aceptando conexión AirSync: {e}")
                break

    def _handle_client(self, sock: socket.socket, addr):
        try:
            # 1. Leer encabezado de comando en formato JSON (terminado en newline)
            sock_file = sock.makefile("rb")
            header_line = sock_file.readline()
            if not header_line:
                sock.close()
                return

            req = json.loads(header_line.decode("utf-8").strip())
            magic = req.get("magic", "")
            if magic != self.MAGIC:
                sock.sendall(b'{"status":"ERROR","message":"Invalid magic header"}\n')
                sock.close()
                return

            command = req.get("command", "")
            file_id = req.get("fileId", "")
            start_chunk = req.get("startChunk", 0)
            chunk_size = req.get("chunkSize", DEFAULT_CHUNK_SIZE)

            if command == "PULL_FILE":
                self._handle_pull_file(sock, file_id, start_chunk, chunk_size)
            elif command == "QUERY_FILE":
                self._handle_query_file(sock, file_id)
            elif command == "PUSH_FILE":
                file_name = req.get("fileName", "airsync_upload.dat")
                file_size = req.get("fileSize", 0)
                total_chunks = req.get("totalChunks", 1)
                copy_to_clipboard = req.get("copyToClipboard", False)
                auto_paste = req.get("autoPaste", False)
                target_dest = req.get("targetDestination", "dropzone")
                self._handle_push_file(sock, file_name, file_size, chunk_size, total_chunks, copy_to_clipboard, auto_paste, target_dest)
            else:
                sock.sendall(b'{"status":"ERROR","message":"Comando desconocido"}\n')
        except Exception as e:
            logger.debug(f"Error procesando cliente AirSync {addr}: {e}")
        finally:
            try:
                sock.close()
            except Exception:
                pass

    def _handle_query_file(self, sock: socket.socket, file_id: str):
        file_path = self._shared_files.get(file_id)
        if not file_path or not os.path.exists(file_path):
            sock.sendall(b'{"status":"NOT_FOUND"}\n')
            return

        size = os.path.getsize(file_path)
        name = os.path.basename(file_path)
        resp = {
            "status": "OK",
            "fileId": file_id,
            "fileName": name,
            "fileSize": size,
            "chunkSize": DEFAULT_CHUNK_SIZE,
            "totalChunks": (size + DEFAULT_CHUNK_SIZE - 1) // DEFAULT_CHUNK_SIZE
        }
        sock.sendall((json.dumps(resp) + "\n").encode("utf-8"))

    def _handle_pull_file(self, sock: socket.socket, file_id: str, start_chunk: int, chunk_size: int):
        file_path = self._shared_files.get(file_id)
        if not file_path or not os.path.exists(file_path):
            sock.sendall(b'{"status":"NOT_FOUND"}\n')
            return

        total_size = os.path.getsize(file_path)
        total_chunks = (total_size + chunk_size - 1) // chunk_size if total_size > 0 else 1

        # Enviar confirmación de inicio de streaming
        ack = {
            "status": "STREAMING",
            "fileId": file_id,
            "fileName": os.path.basename(file_path),
            "totalSize": total_size,
            "chunkSize": chunk_size,
            "totalChunks": total_chunks,
            "startChunk": start_chunk
        }
        sock.sendall((json.dumps(ack) + "\n").encode("utf-8"))

        with open(file_path, "rb") as f:
            if start_chunk > 0:
                f.seek(start_chunk * chunk_size)

            current_chunk = start_chunk
            while current_chunk < total_chunks:
                data = f.read(chunk_size)
                if not data:
                    break

                # Calcular SHA-256 del bloque
                chunk_hash = hashlib.sha256(data).digest() # 32 bytes

                # Empaquetar: [Index: 4B][Length: 4B][SHA256: 32B][DATA]
                header = struct.pack(">II32s", current_chunk, len(data), chunk_hash)
                sock.sendall(header + data)

                current_chunk += 1

    def _read_exact(self, sock: socket.socket, num_bytes: int) -> bytes:
        data = bytearray()
        while len(data) < num_bytes:
            packet = sock.recv(num_bytes - len(data))
            if not packet:
                return bytes(data)
            data.extend(packet)
        return bytes(data)

    def _handle_push_file(self, sock: socket.socket, file_name: str, file_size: int, chunk_size: int, total_chunks: int, copy_to_clipboard: bool, auto_paste: bool, target_dest: str):
        # Resolver directorio raíz de Dropzone (config.dropzone_path explícito, dropzone_manager.root_path, o ~/HendrixDropzone)
        dropzone_base = getattr(config, "dropzone_path", None)
        if not dropzone_base:
            try:
                from storage.dropzone_manager import dropzone_manager
                if dropzone_manager and getattr(dropzone_manager, "root_path", None):
                    dropzone_base = dropzone_manager.root_path
            except Exception:
                pass

        if not dropzone_base:
            dropzone_base = os.path.expanduser("~/HendrixDropzone")

        # Subdirectorio de destino según target_dest
        if target_dest and target_dest.lower() in ["root", "base", "dropzone"]:
            dest_dir = os.path.join(dropzone_base, "AirSync")
        elif target_dest:
            dest_dir = os.path.join(dropzone_base, target_dest)
        else:
            dest_dir = os.path.join(dropzone_base, "AirSync")

        os.makedirs(dest_dir, exist_ok=True)

        safe_name = os.path.basename(file_name) or f"upload_{int(time.time())}.dat"
        dest_path = os.path.join(dest_dir, safe_name)
        file_id = f"push_{uuid.uuid4().hex[:8]}"

        ack = {
            "status": "READY_TO_RECEIVE",
            "fileId": file_id,
            "fileName": safe_name,
            "chunkSize": chunk_size,
            "startChunk": 0
        }
        sock.sendall((json.dumps(ack) + "\n").encode("utf-8"))

        with open(dest_path, "wb") as f:
            for current_chunk in range(total_chunks):
                header_data = self._read_exact(sock, 40)
                if not header_data or len(header_data) < 40:
                    logger.warning(f"Conexión cerrada prematuramente al recibir chunk {current_chunk}")
                    return

                chunk_idx, chunk_len, expected_hash = struct.unpack(">II32s", header_data)
                data = self._read_exact(sock, chunk_len)
                if not data or len(data) != chunk_len:
                    logger.warning(f"Error leyendo payload del chunk {current_chunk}")
                    return

                computed_hash = hashlib.sha256(data).digest()
                if computed_hash != expected_hash:
                    logger.error(f"Falla de checksum SHA-256 en chunk {chunk_idx}")
                    err = {"status": "ERROR", "message": f"Fallo de integridad SHA-256 en bloque {chunk_idx}"}
                    sock.sendall((json.dumps(err) + "\n").encode("utf-8"))
                    return

                f.write(data)

        # Registrar archivo recibido
        self.register_file(file_id, dest_path)

        copied = False
        is_image = any(safe_name.lower().endswith(ext) for ext in [".png", ".jpg", ".jpeg", ".bmp", ".webp"])
        if copy_to_clipboard and is_image:
            copied = self.copy_image_to_clipboard(dest_path)
            if copied and auto_paste:
                self.simulate_ctrl_v()

        if self.on_file_received:
            try:
                self.on_file_received({
                    "fileId": file_id,
                    "fileName": safe_name,
                    "filePath": dest_path,
                    "fileSize": file_size,
                    "copiedToClipboard": copied,
                    "timestamp": time.time()
                })
            except Exception as e:
                logger.debug(f"Error en on_file_received callback: {e}")

        resp = {
            "status": "COMPLETED",
            "fileId": file_id,
            "fileName": safe_name,
            "totalBytes": os.path.getsize(dest_path),
            "copiedToClipboard": copied
        }
        sock.sendall((json.dumps(resp) + "\n").encode("utf-8"))

    @staticmethod
    def copy_image_to_clipboard(image_path: str) -> bool:
        """Copia una imagen directamente al portapapeles de Windows en formato DIB."""
        try:
            from PIL import Image
            import win32clipboard
            import io

            image = Image.open(image_path)
            output = io.BytesIO()
            image.convert("RGB").save(output, "BMP")
            data = output.getvalue()[14:] # Quitar el encabezado BMP de 14 bytes para obtener CF_DIB
            output.close()

            win32clipboard.OpenClipboard()
            win32clipboard.EmptyClipboard()
            win32clipboard.SetClipboardData(win32clipboard.CF_DIB, data)
            win32clipboard.CloseClipboard()
            logger.info(f"📋 Imagen copiada al portapapeles de Windows: {image_path}")
            return True
        except Exception as e:
            logger.warning(f"No se pudo copiar imagen al portapapeles: {e}")
            return False

    @staticmethod
    def simulate_ctrl_v():
        """Simula Ctrl+V en la ventana en primer plano de Windows."""
        try:
            import ctypes
            time.sleep(0.15)
            VK_CONTROL = 0x11
            VK_V = 0x56
            KEYEVENTF_KEYUP = 0x0002
            user32 = ctypes.windll.user32
            user32.keybd_event(VK_CONTROL, 0, 0, 0)
            user32.keybd_event(VK_V, 0, 0, 0)
            user32.keybd_event(VK_V, 0, KEYEVENTF_KEYUP, 0)
            user32.keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0)
            logger.info("⌨️ Atajo Ctrl+V simulado en Windows")
        except Exception as e:
            logger.debug(f"Error simulando Ctrl+V: {e}")

airsync_server = AirSyncServer()
