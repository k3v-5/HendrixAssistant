"""
Hendrix Desktop Companion - Linux Window Manager
Implementación de IWindowManager para entornos Linux (X11 / Wayland) mediante EWMH, wmctrl, xdotool o psutil.
"""
import shutil
import subprocess
import os
from typing import List, Dict, Any, Optional
import psutil
from core.platform.interfaces import IWindowManager

class LinuxWindowManager(IWindowManager):
    def __init__(self):
        self._has_wmctrl = shutil.which("wmctrl") is not None
        self._has_xdotool = shutil.which("xdotool") is not None

    def get_taskbar_windows(self) -> List[Dict[str, Any]]:
        windows: List[Dict[str, Any]] = []
        fg_id = self.get_foreground_window()

        # Estrategia 1: wmctrl -l -p (ID, Desktop, PID, Host, Title)
        if self._has_wmctrl:
            try:
                res = subprocess.run(
                    ["wmctrl", "-l", "-p"],
                    capture_output=True,
                    text=True,
                    timeout=2
                )
                if res.returncode == 0:
                    for line in res.stdout.strip().splitlines():
                        parts = line.split(maxsplit=4)
                        if len(parts) >= 5:
                            win_id_hex, desktop, pid_str, _, title = parts
                            title = title.strip()
                            if not title or desktop == "-1":  # Desktop -1 son barras/fondos
                                continue
                            
                            process_name = "unknown"
                            try:
                                pid = int(pid_str)
                                if pid > 0 and psutil.pid_exists(pid):
                                    proc = psutil.Process(pid)
                                    process_name = proc.name()
                            except Exception:
                                pass

                            is_foreground = (win_id_hex == fg_id) if fg_id else False
                            try:
                                parsed_hwnd = int(win_id_hex, 16)
                            except Exception:
                                parsed_hwnd = int(pid_str) if pid_str.isdigit() else 1

                            windows.append({
                                "hwnd": parsed_hwnd,
                                "raw_id": win_id_hex,
                                "title": title,
                                "process": process_name,
                                "isForeground": is_foreground,
                                "platform": "linux",
                            })
                    if windows:
                        windows.sort(key=lambda w: (not w["isForeground"], w["process"].lower(), w["title"].lower()))
                        return windows
            except Exception:
                pass

        # Estrategia 2: Si no hay wmctrl, enumerar procesos interactivos vía psutil
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                try:
                    name = proc.info.get('name') or ''
                    # Filtrar hilos y procesos del sistema comunes
                    if name and not name.startswith(('kworker', 'systemd', 'dbus')):
                        windows.append({
                            "hwnd": int(proc.info['pid']),
                            "title": name,
                            "process": name,
                            "isForeground": False,
                            "platform": "linux",
                        })
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    pass
        except Exception:
            pass

        return windows

    def focus_window(self, hwnd_or_id: Any) -> bool:
        if not hwnd_or_id:
            return False
        win_id = hex(hwnd_or_id) if isinstance(hwnd_or_id, int) and hwnd_or_id > 65535 else str(hwnd_or_id)
        try:
            if self._has_wmctrl and win_id.startswith("0x"):
                res = subprocess.run(["wmctrl", "-i", "-a", win_id], capture_output=True, timeout=2)
                return res.returncode == 0
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "windowactivate", str(hwnd_or_id)], capture_output=True, timeout=2)
                return res.returncode == 0
        except Exception:
            pass
        return False

    def minimize_window(self, hwnd_or_id: Any) -> bool:
        if not hwnd_or_id:
            return False
        win_id = hex(hwnd_or_id) if isinstance(hwnd_or_id, int) and hwnd_or_id > 65535 else str(hwnd_or_id)
        try:
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "windowminimize", str(hwnd_or_id)], capture_output=True, timeout=2)
                return res.returncode == 0
            if self._has_wmctrl and win_id.startswith("0x"):
                res = subprocess.run(["wmctrl", "-i", "-r", win_id, "-b", "add,hidden"], capture_output=True, timeout=2)
                return res.returncode == 0
        except Exception:
            pass
        return False

    def restore_window(self, hwnd_or_id: Any) -> bool:
        return self.focus_window(hwnd_or_id)

    def close_window(self, hwnd_or_id: Any) -> bool:
        win_id = str(hwnd_or_id)
        try:
            if self._has_wmctrl and win_id.startswith("0x"):
                res = subprocess.run(["wmctrl", "-i", "-c", win_id], capture_output=True, timeout=2)
                return res.returncode == 0
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "windowclose", win_id], capture_output=True, timeout=2)
                return res.returncode == 0
        except Exception:
            pass
        return False

    def get_foreground_window(self) -> Any:
        try:
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "getactivewindow"], capture_output=True, text=True, timeout=2)
                if res.returncode == 0 and res.stdout.strip():
                    dec_id = int(res.stdout.strip())
                    return f"0x{dec_id:08x}"
        except Exception:
            pass
        return None

    def get_window_title(self, hwnd_or_id: Any) -> str:
        win_id = str(hwnd_or_id)
        try:
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "getwindowname", win_id], capture_output=True, text=True, timeout=2)
                if res.returncode == 0:
                    return res.stdout.strip()
        except Exception:
            pass
        return f"Window {win_id}"

    def get_window_rect(self, hwnd_or_id: Any) -> Dict[str, int]:
        win_id = str(hwnd_or_id)
        try:
            if self._has_xdotool:
                res = subprocess.run(["xdotool", "getwindowgeometry", "--shell", win_id], capture_output=True, text=True, timeout=2)
                if res.returncode == 0:
                    data = {}
                    for line in res.stdout.splitlines():
                        if "=" in line:
                            k, v = line.split("=", 1)
                            data[k.strip()] = int(v.strip())
                    x = data.get("X", 0)
                    y = data.get("Y", 0)
                    w = data.get("WIDTH", 800)
                    h = data.get("HEIGHT", 600)
                    return {"left": x, "top": y, "right": x + w, "bottom": y + h}
        except Exception:
            pass
        return {"left": 0, "top": 0, "right": 1920, "bottom": 1080}
