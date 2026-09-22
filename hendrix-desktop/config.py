import os
import json
import random
import socket

CONFIG_FILE = os.path.join(os.path.dirname(__file__), "hendrix_config.json")

DEFAULT_PORT = 8899
DEFAULT_AIRSYNC_PORT = 8900
DEFAULT_QUALITY = 75
DEFAULT_CHUNK_SIZE = 256 * 1024

def get_local_ip() -> str:
    """Obtiene la IP local de la máquina en la red LAN."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

class DesktopConfig:
    def __init__(self):
        self.port = DEFAULT_PORT
        self.airsync_port = DEFAULT_AIRSYNC_PORT
        self.pin = str(random.randint(100000, 999999))
        self.local_ip = get_local_ip()
        self.allow_mouse_control = True
        self.allow_keyboard_control = True
        self.allow_screen_capture = True
        self.allow_terminal_commands = True
        self.paired_tokens = []
        self.paired_devices = []
        self.remote_tunnel_url = None
        self.dropzone_path = None
        self.quality = DEFAULT_QUALITY
        self.airsync_chunk_size = DEFAULT_CHUNK_SIZE
        self.load()

    def load(self):
        if os.path.exists(CONFIG_FILE):
            try:
                with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.port = data.get("port", DEFAULT_PORT)
                    self.airsync_port = data.get("airsync_port", DEFAULT_AIRSYNC_PORT)
                    self.pin = str(data.get("pin", self.pin))
                    self.dropzone_path = data.get("dropzone_path", None)
                    self.quality = data.get("quality", DEFAULT_QUALITY)
                    self.airsync_chunk_size = data.get("airsync_chunk_size", DEFAULT_CHUNK_SIZE)
                    self.allow_mouse_control = data.get("allow_mouse_control", True)
                    self.allow_keyboard_control = data.get("allow_keyboard_control", True)
                    self.allow_screen_capture = data.get("allow_screen_capture", True)
                    self.allow_terminal_commands = data.get("allow_terminal_commands", True)
                    self.paired_tokens = data.get("paired_tokens", [])
                    self.paired_devices = data.get("paired_devices", [])
                    # Sincronizar paired_tokens si hay devices
                    if self.paired_devices:
                        for d in self.paired_devices:
                            tok = d.get("token")
                            if tok and tok not in self.paired_tokens:
                                self.paired_tokens.append(tok)
                    self.remote_tunnel_url = data.get("remote_tunnel_url", None)
            except Exception:
                pass
        else:
            self.save()

    def save(self):
        try:
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump({
                    "port": self.port,
                    "airsync_port": self.airsync_port,
                    "pin": self.pin,
                    "airsync_chunk_size": self.airsync_chunk_size,
                    "dropzone_path": self.dropzone_path,
                    "quality": self.quality,
                    "allow_mouse_control": self.allow_mouse_control,
                    "allow_keyboard_control": self.allow_keyboard_control,
                    "allow_screen_capture": self.allow_screen_capture,
                    "allow_terminal_commands": self.allow_terminal_commands,
                    "paired_tokens": self.paired_tokens,
                    "paired_devices": self.paired_devices,
                    "remote_tunnel_url": self.remote_tunnel_url
                }, f, indent=2)
        except Exception:
            pass

config = DesktopConfig()
