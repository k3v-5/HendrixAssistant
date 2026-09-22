import time
import uuid
from config import config

class SessionManager:
    def __init__(self):
        # token -> {"websocket": ws, "ip": str, "device_name": str, "connected_at": float}
        self.active_connections = {}
        self.connected_device_name = None
        self.on_device_connected = None
        self.on_device_disconnected = None
        self.on_devices_updated = None

    def authenticate_or_pair(
        self,
        pin_attempt: str,
        client_token: str,
        device_name: str,
        client_ip: str = "127.0.0.1",
        websocket = None
    ) -> str | None:
        now = time.time()
        auth_token = None

        # 1. Si ya tiene un token guardado previamente autorizado
        if client_token and client_token in config.paired_tokens:
            auth_token = client_token
            # Actualizar o insertar metadata del dispositivo
            existing = next((d for d in config.paired_devices if d.get("token") == auth_token), None)
            if existing:
                existing["name"] = device_name
                existing["ip"] = client_ip
                existing["last_seen"] = int(now)
            else:
                config.paired_devices.append({
                    "token": auth_token,
                    "name": device_name,
                    "ip": client_ip,
                    "paired_at": int(now),
                    "last_seen": int(now)
                })
            config.save()

        # 2. Si el PIN coincide con el generado en la app de escritorio
        elif pin_attempt and pin_attempt.strip() == config.pin:
            auth_token = str(uuid.uuid4())
            config.paired_tokens.append(auth_token)
            config.paired_devices.append({
                "token": auth_token,
                "name": device_name,
                "ip": client_ip,
                "paired_at": int(now),
                "last_seen": int(now)
            })
            config.save()

        if auth_token:
            self.connected_device_name = device_name
            if websocket:
                self.active_connections[auth_token] = {
                    "websocket": websocket,
                    "ip": client_ip,
                    "device_name": device_name,
                    "connected_at": now
                }
            if self.on_device_connected:
                self.on_device_connected(device_name)
            if self.on_devices_updated:
                self.on_devices_updated()
            return auth_token

        return None

    def unregister_connection(self, websocket):
        """Llamado cuando un websocket se desconecta."""
        disconnected_token = None
        for tok, info in list(self.active_connections.items()):
            if info.get("websocket") == websocket:
                disconnected_token = tok
                del self.active_connections[tok]
                break

        if not self.active_connections:
            old_device = self.connected_device_name
            self.connected_device_name = None
            if self.on_device_disconnected:
                self.on_device_disconnected(old_device)

        if self.on_devices_updated:
            self.on_devices_updated()

    def set_disconnected(self):
        old_device = self.connected_device_name
        self.connected_device_name = None
        self.active_connections.clear()
        if self.on_device_disconnected:
            self.on_device_disconnected(old_device)
        if self.on_devices_updated:
            self.on_devices_updated()

    def is_authenticated(self, websocket) -> bool:
        """Verifica si un objeto websocket corresponde a una sesión actualmente autenticada."""
        return any(info.get("websocket") == websocket for info in self.active_connections.values())

    def get_authenticated_websockets(self) -> list:
        """Devuelve la lista de sockets actualmente autenticados de forma segura."""
        return [info["websocket"] for info in list(self.active_connections.values()) if "websocket" in info and info["websocket"] is not None]

    def get_all_paired_devices(self) -> list[dict]:
        """Devuelve la lista completa de dispositivos emparejados con su estado en vivo."""
        devices = []
        now = time.time()
        for d in config.paired_devices:
            token = d.get("token")
            is_online = token in self.active_connections
            conn_info = self.active_connections.get(token, {})
            devices.append({
                "token": token,
                "name": conn_info.get("device_name", d.get("name", "Dispositivo Móvil")),
                "ip": conn_info.get("ip", d.get("ip", "Desconocida")),
                "paired_at": d.get("paired_at", int(now)),
                "last_seen": d.get("last_seen", int(now)),
                "is_online": is_online
            })
        return devices

    def disconnect_device(self, token: str) -> bool:
        """Fuerza la desconexión del websocket de un dispositivo específico."""
        conn = self.active_connections.pop(token, None)
        if conn:
            ws = conn.get("websocket")
            if ws:
                try:
                    import asyncio
                    asyncio.create_task(ws.close())
                except Exception:
                    pass
            if not self.active_connections:
                self.connected_device_name = None
                if self.on_device_disconnected:
                    self.on_device_disconnected(conn.get("device_name"))
            if self.on_devices_updated:
                self.on_devices_updated()
            return True
        return False

    def revoke_device(self, token: str) -> bool:
        """Desconecta y revoca permanentemente el token de emparejamiento."""
        self.disconnect_device(token)
        if token in config.paired_tokens:
            config.paired_tokens.remove(token)
        config.paired_devices = [d for d in config.paired_devices if d.get("token") != token]
        config.save()
        if self.on_devices_updated:
            self.on_devices_updated()
        return True

session_manager = SessionManager()
