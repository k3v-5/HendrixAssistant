import json
import logging
import socket
import threading
import time
import uuid

logger = logging.getLogger("DiscoveryServer")

DISCOVERY_PORT = 8764
PING_MESSAGE = b"HENDRIX_DISCOVERY_PING"
PONG_PREFIX = "HENDRIX_DISCOVERY_PONG:"


def get_mac_address() -> str:
    """Obtiene la dirección MAC física del adaptador de red principal en formato estándar."""
    try:
        node = uuid.getnode()
        mac = ":".join(f"{(node >> ele) & 0xff:02X}" for ele in range(40, -1, -8))
        return mac
    except Exception as e:
        logger.warning(f"Error resolviendo dirección MAC: {e}")
        return "00:00:00:00:00:00"


def get_local_ip() -> str:
    """Determina la dirección IP local de la interfaz de red activa hacia la LAN."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # No necesita ser alcanzable realmente; se usa para seleccionar la interfaz de salida
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        try:
            ip = socket.gethostbyname(socket.gethostname())
        except Exception:
            ip = "127.0.0.1"
    finally:
        s.close()
    return ip


class DiscoveryServer:
    """
    Servidor UDP de descubrimiento Zero-Config para Hendrix Desktop.
    
    1. Responde inmediatamente a pings de sondeo broadcast desde la app móvil.
    2. Emite balizas periódicas (beacons) hacia la subred local (255.255.255.255:8764).
    3. Informa la IP, puerto WebSocket, puerto AirSync y dirección MAC para sincronía con Wake-on-LAN.
    """

    def __init__(
        self,
        ws_port: int = 8765,
        airsync_port: int = 8766,
        discovery_port: int = DISCOVERY_PORT,
        beacon_interval: float = 3.0
    ):
        self.ws_port = ws_port
        self.airsync_port = airsync_port
        self.discovery_port = discovery_port
        self.beacon_interval = beacon_interval
        self._running = False
        self._thread = None
        self._beacon_thread = None
        self._sock = None

    def get_payload_dict(self) -> dict:
        return {
            "service": "hendrix-workspace",
            "version": "1.0",
            "hostname": socket.gethostname(),
            "ip": get_local_ip(),
            "ws_port": self.ws_port,
            "airsync_port": self.airsync_port,
            "mac": get_mac_address()
        }

    def start(self):
        if self._running:
            return
        self._running = True

        try:
            self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._sock.bind(("", self.discovery_port))
            self._sock.settimeout(1.0)
        except Exception as e:
            logger.error(f"Fallo al enlazar socket de descubrimiento UDP en puerto {self.discovery_port}: {e}")
            self._running = False
            return

        self._thread = threading.Thread(target=self._listen_loop, daemon=True, name="HendrixDiscoveryListener")
        self._thread.start()

        self._beacon_thread = threading.Thread(target=self._beacon_loop, daemon=True, name="HendrixDiscoveryBeacon")
        self._beacon_thread.start()

        logger.info(f"Servidor de descubrimiento Zero-Config iniciado en puerto UDP {self.discovery_port}")

    def _listen_loop(self):
        while self._running:
            try:
                data, addr = self._sock.recvfrom(1024)
                if PING_MESSAGE in data:
                    payload = self.get_payload_dict()
                    response = f"{PONG_PREFIX}{json.dumps(payload)}".encode("utf-8")
                    self._sock.sendto(response, addr)
                    logger.debug(f"Respuesta de descubrimiento enviada a {addr}")
            except socket.timeout:
                continue
            except Exception as e:
                if self._running:
                    logger.warning(f"Error en bucle de escucha de descubrimiento: {e}")

    def _beacon_loop(self):
        broadcast_addr = ("255.255.255.255", self.discovery_port)
        while self._running:
            try:
                payload = self.get_payload_dict()
                msg = f"{PONG_PREFIX}{json.dumps(payload)}".encode("utf-8")
                self._sock.sendto(msg, broadcast_addr)
            except Exception as e:
                logger.debug(f"Aviso al enviar baliza de descubrimiento broadcast: {e}")
            time.sleep(self.beacon_interval)

    def stop(self):
        self._running = False
        if self._sock:
            try:
                self._sock.close()
            except Exception:
                pass
        logger.info("Servidor de descubrimiento detenido.")
