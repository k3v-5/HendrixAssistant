"""
Hendrix Desktop Companion - Linux Display Environment Resolver
Garantiza que la variable de entorno DISPLAY esté correctamente detectada y configurada
en entornos Linux (X11 / Xwayland / Wayland) antes de que cualquier módulo gráfico
o de inyección de entrada (pyautogui, mouseinfo, mss) intente conectarse.
"""
import os
import sys
import glob

def ensure_linux_display() -> str | None:
    """
    Detecta y configura de forma segura la variable de entorno DISPLAY para Linux.
    1. Si DISPLAY ya existe y es válida, la preserva.
    2. Si DISPLAY no existe o está vacía, busca sockets en /tmp/.X11-unix/X* dando
       prioridad a los sockets pertenecientes al UID del usuario actual (Xwayland / X11).
    3. Valida la conexión antes de asignarla a os.environ['DISPLAY'].
    Retorna el DISPLAY seleccionado (ej: ':1') o None si no hay servidor gráfico accesible.
    """
    if not sys.platform.startswith("linux"):
        return None

    current_disp = os.environ.get("DISPLAY", "").strip()
    if current_disp and _can_connect_display(current_disp):
        return current_disp

    current_uid = os.getuid() if hasattr(os, "getuid") else None
    user_displays: list[str] = []
    other_displays: list[str] = []

    try:
        for sock_path in sorted(glob.glob("/tmp/.X11-unix/X*")):
            base = os.path.basename(sock_path)
            if base.startswith("X") and len(base) > 1:
                dno = base[1:]
                disp_candidate = f":{dno}"
                try:
                    st = os.stat(sock_path)
                    if current_uid is not None and st.st_uid == current_uid:
                        user_displays.append(disp_candidate)
                    else:
                        other_displays.append(disp_candidate)
                except Exception:
                    other_displays.append(disp_candidate)
    except Exception:
        pass

    # Probar primero los sockets pertenecientes al usuario actual
    for disp in user_displays + other_displays:
        if _can_connect_display(disp):
            os.environ["DISPLAY"] = disp
            return disp

    # Fallback predeterminado seguro en caso de que no haya Xlib
    if user_displays:
        os.environ["DISPLAY"] = user_displays[0]
        return user_displays[0]

    return None

def _can_connect_display(display_name: str) -> bool:
    """
    Verifica si una pantalla X11/Xwayland es accesible y autorizada.
    """
    try:
        import Xlib.display
        d = Xlib.display.Display(display_name)
        d.close()
        return True
    except Exception:
        pass

    # Verificación alternativa a nivel de socket Unix si Xlib no está disponible
    try:
        import socket
        clean = display_name.lstrip(":").split(".")[0]
        sock_path = f"/tmp/.X11-unix/X{clean}"
        if not os.path.exists(sock_path):
            return False
        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        s.settimeout(0.2)
        s.connect(sock_path)
        s.close()
        return True
    except Exception:
        return False
