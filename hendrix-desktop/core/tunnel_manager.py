import os
import re
import shutil
import subprocess
import threading
from typing import Callable, Optional
from config import config

class TunnelManager:
    """
    Gestor de túneles remotos seguros (WAN) para Hendrix Desktop.
    Permite acceder a la computadora desde redes móviles (4G/5G) o fuera del hogar.
    Soporta:
      1. Cloudflare Quick Tunnels automáticos (sin configuración previa ni apertura de puertos).
      2. URLs de túneles personalizados (ngrok, Tailscale, Cloudflare Named Tunnel, o DNS dinámico).
    """

    def __init__(self):
        self._process: Optional[subprocess.Popen] = None
        self._tunnel_url: Optional[str] = getattr(config, "remote_tunnel_url", None)
        self._is_active: bool = False
        self._on_status_change_callbacks: list[Callable[[bool, Optional[str]], None]] = []
        self._lock = threading.Lock()

    def add_listener(self, callback: Callable[[bool, Optional[str]], None]):
        """Registra un observador para cambios en el estado del túnel."""
        with self._lock:
            if callback not in self._on_status_change_callbacks:
                self._on_status_change_callbacks.append(callback)

    def remove_listener(self, callback: Callable[[bool, Optional[str]], None]):
        with self._lock:
            if callback in self._on_status_change_callbacks:
                self._on_status_change_callbacks.remove(callback)

    def _notify(self):
        with self._lock:
            active = self._is_active
            url = self._tunnel_url
            callbacks = list(self._on_status_change_callbacks)

        for cb in callbacks:
            try:
                cb(active, url)
            except Exception:
                pass

    def get_tunnel_url(self) -> Optional[str]:
        return self._tunnel_url

    def is_active(self) -> bool:
        return self._is_active

    def find_cloudflared_binary(self) -> Optional[str]:
        """Busca el ejecutable de cloudflared en el sistema o en la carpeta bin local."""
        bin_path = shutil.which("cloudflared")
        if bin_path:
            return bin_path

        local_bin = os.path.join(os.path.dirname(os.path.dirname(__file__)), "bin", "cloudflared.exe")
        if os.path.exists(local_bin):
            return local_bin

        return None

    def set_custom_tunnel_url(self, url: str):
        """Establece una URL de túnel personalizada o existente y notifica a los clientes."""
        clean_url = url.strip()
        if clean_url:
            if not (clean_url.startswith("http://") or clean_url.startswith("https://") or clean_url.startswith("ws://") or clean_url.startswith("wss://")):
                clean_url = f"https://{clean_url}"

        self._tunnel_url = clean_url if clean_url else None
        self._is_active = bool(clean_url)
        config.remote_tunnel_url = self._tunnel_url
        config.save()
        self._notify()

    def start_quick_tunnel(self, port: int = config.port) -> bool:
        """
        Inicia un Quick Tunnel automático mediante Cloudflare si está instalado.
        Si no se detecta la utilidad, mantiene la URL personalizada si existe.
        """
        binary = self.find_cloudflared_binary()
        if not binary:
            if self._tunnel_url:
                self._is_active = True
                self._notify()
                return True
            return False

        if self._process is not None:
            return True

        def run_tunnel():
            cmd = [binary, "tunnel", "--url", f"http://127.0.0.1:{port}"]
            try:
                self._process = subprocess.Popen(
                    cmd,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
                )

                # Monitorear la salida de error estándar donde cloudflared imprime la URL pública
                url_pattern = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")
                for line in self._process.stderr:
                    match = url_pattern.search(line)
                    if match:
                        found_url = match.group(0)
                        self._tunnel_url = found_url
                        self._is_active = True
                        config.remote_tunnel_url = found_url
                        config.save()
                        self._notify()
                        break

                self._process.wait()
            except Exception:
                pass
            finally:
                self._is_active = False
                self._process = None
                self._notify()

        thread = threading.Thread(target=run_tunnel, daemon=True)
        thread.start()
        return True

    def stop_tunnel(self):
        """Detiene el proceso del túnel y restablece el estado."""
        if self._process is not None:
            try:
                self._process.terminate()
            except Exception:
                pass
            self._process = None

        self._is_active = False
        self._notify()

tunnel_manager = TunnelManager()
