import sys
import threading
import asyncio

try:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

from config import config
from core import ws_server
from core.session_manager import session_manager
from core.discovery_server import DiscoveryServer
from core.ota_server import ota_server
from ui.app_window import AppWindow

server_loop = None

def free_port_if_occupied(port: int):
    try:
        import psutil
        import os
        current_pid = os.getpid()
        for conn in psutil.net_connections(kind="inet"):
            if conn.laddr and conn.laddr.port == port and conn.status == "LISTEN":
                pid = conn.pid
                if pid and pid != current_pid:
                    try:
                        proc = psutil.Process(pid)
                        proc_name = proc.name().lower()
                        if "python" in proc_name:
                            print(f"[Hendrix] 🔄 Liberando puerto {port} ocupado por instancia previa (PID {pid} - {proc_name})...")
                            proc.kill()
                            proc.wait(timeout=2.0)
                    except Exception:
                        pass
    except Exception:
        pass

def start_background_server():
    global server_loop
    server_loop = asyncio.new_event_loop()
    asyncio.set_event_loop(server_loop)
    try:
        server_loop.run_until_complete(ws_server.run_server(config.port))
    except Exception as e:
        import traceback
        traceback.print_exc()
        ws_server.log_activity(f"❌ Error crítico en WebSocket: {e}")

def main():
    # Asegurar que el puerto WebSocket no esté ocupado por un zombi previo
    free_port_if_occupied(config.port)

    # Iniciar servidor WebSocket en hilo secundario de fondo
    server_thread = threading.Thread(target=start_background_server, daemon=True)
    server_thread.start()

    # Iniciar servidor de descubrimiento UDP Zero-Config
    discovery_server = DiscoveryServer(ws_port=config.port, airsync_port=config.airsync_port)
    discovery_server.start()

    # Iniciar servidor HTTP OTA en segundo plano
    ota_server.start()

    def on_close():
        discovery_server.stop()
        ota_server.stop()
        if server_loop and server_loop.is_running():
            server_loop.call_soon_threadsafe(server_loop.stop)
        sys.exit(0)

    # Iniciar interfaz gráfica en el hilo principal
    app = AppWindow(on_close_callback=on_close)

    # Conectar callbacks de logging y estado hacia la UI
    def ui_logger(text: str):
        app.after(0, lambda: app.activity_log.append_log(text))

    def on_device_change(name: str | None):
        app.after(0, lambda: app.pairing_card.set_connected_device(name))

    ws_server.activity_logger = ui_logger
    session_manager.on_device_connected = on_device_change
    session_manager.on_device_disconnected = lambda _: on_device_change(None)

    ui_logger("Hendrix Desktop Companion iniciado correctamente")
    ui_logger(f"Escuchando en ws://{config.local_ip}:{config.port}/ws")

    app.mainloop()

if __name__ == "__main__":
    main()
