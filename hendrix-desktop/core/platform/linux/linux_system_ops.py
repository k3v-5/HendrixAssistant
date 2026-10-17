"""
Hendrix Desktop Companion - Linux System Operations
Implementación de ISystemOps para distribuciones Linux mediante systemctl, loginctl y pactl/amixer.
"""
import shutil
import subprocess
import re
from core.platform.interfaces import ISystemOps

class LinuxSystemOps(ISystemOps):
    def __init__(self):
        self._has_systemctl = shutil.which("systemctl") is not None
        self._has_loginctl = shutil.which("loginctl") is not None
        self._has_pactl = shutil.which("pactl") is not None
        self._has_amixer = shutil.which("amixer") is not None

    def shutdown(self) -> bool:
        try:
            if self._has_systemctl:
                res = subprocess.run(["systemctl", "poweroff"], capture_output=True, timeout=5)
                return res.returncode == 0
            res = subprocess.run(["shutdown", "-h", "now"], capture_output=True, timeout=5)
            return res.returncode == 0
        except Exception:
            return False

    def reboot(self) -> bool:
        try:
            if self._has_systemctl:
                res = subprocess.run(["systemctl", "reboot"], capture_output=True, timeout=5)
                return res.returncode == 0
            res = subprocess.run(["shutdown", "-r", "now"], capture_output=True, timeout=5)
            return res.returncode == 0
        except Exception:
            return False

    def suspend(self) -> bool:
        try:
            if self._has_systemctl:
                res = subprocess.run(["systemctl", "suspend"], capture_output=True, timeout=5)
                return res.returncode == 0
            return False
        except Exception:
            return False

    def lock(self) -> bool:
        try:
            if self._has_loginctl:
                res = subprocess.run(["loginctl", "lock-session"], capture_output=True, timeout=5)
                if res.returncode == 0:
                    return True
            if shutil.which("xdg-screensaver"):
                res = subprocess.run(["xdg-screensaver", "lock"], capture_output=True, timeout=5)
                return res.returncode == 0
            return False
        except Exception:
            return False

    def set_volume(self, level: int) -> bool:
        level = max(0, min(100, int(level)))
        try:
            if self._has_pactl:
                res = subprocess.run(
                    ["pactl", "set-sink-volume", "@DEFAULT_SINK@", f"{level}%"],
                    capture_output=True,
                    timeout=3
                )
                if res.returncode == 0:
                    return True
            if self._has_amixer:
                res = subprocess.run(
                    ["amixer", "-q", "sset", "Master", f"{level}%"],
                    capture_output=True,
                    timeout=3
                )
                return res.returncode == 0
            return False
        except Exception:
            return False

    def get_volume(self) -> int:
        try:
            if self._has_pactl:
                res = subprocess.run(
                    ["pactl", "get-sink-volume", "@DEFAULT_SINK@"],
                    capture_output=True,
                    text=True,
                    timeout=3
                )
                if res.returncode == 0:
                    match = re.search(r"(\d+)%", res.stdout)
                    if match:
                        return int(match.group(1))
            if self._has_amixer:
                res = subprocess.run(
                    ["amixer", "sget", "Master"],
                    capture_output=True,
                    text=True,
                    timeout=3
                )
                if res.returncode == 0:
                    match = re.search(r"\[(\d+)%\]", res.stdout)
                    if match:
                        return int(match.group(1))
        except Exception:
            pass
        return 50

    def mute(self, state: bool) -> bool:
        try:
            val = "1" if state else "0"
            if self._has_pactl:
                res = subprocess.run(
                    ["pactl", "set-sink-mute", "@DEFAULT_SINK@", val],
                    capture_output=True,
                    timeout=3
                )
                if res.returncode == 0:
                    return True
            if self._has_amixer:
                cmd = "mute" if state else "unmute"
                res = subprocess.run(
                    ["amixer", "-q", "sset", "Master", cmd],
                    capture_output=True,
                    timeout=3
                )
                return res.returncode == 0
            return False
        except Exception:
            return False
