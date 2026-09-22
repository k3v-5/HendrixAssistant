import sys
import threading
import asyncio
from config import config
from core import ws_server
from core.session_manager import session_manager
from core.discovery_server import DiscoveryServer
from core.ota_server import ota_server
from ui.app_window import AppWindow

server_loop = None

def start_background_server():
    global server_loop
    server_loop = asyncio.new_event_loop()
    asyncio.set_event_loop(server_loop)
    try:
        server_loop.run_until_complete(ws_server.run_server(config.port))
    except Exception as e:
        print(f"Error en servidor WebSocket: {e}")

def main():
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
