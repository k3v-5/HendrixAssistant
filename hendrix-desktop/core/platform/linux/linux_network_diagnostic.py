"""
Hendrix Desktop Companion - Linux Network Diagnostic
Diagnóstico de red y cortafuegos para sistemas Linux (ufw, iptables, firewalld).
"""
import shutil
import subprocess
from typing import Dict, Any, Tuple
from core.platform.interfaces import INetworkDiagnostic

class LinuxNetworkDiagnostic(INetworkDiagnostic):
    def __init__(self):
        self._has_ufw = shutil.which("ufw") is not None
        self._has_iptables = shutil.which("iptables") is not None
        self._has_firewalld = shutil.which("firewall-cmd") is not None

    def check_firewall_rule(self, rule_name: str = "HendrixCompanion", port: int = 8765) -> Dict[str, Any]:
        """
        Comprueba el estado del firewall en Linux.
        Si no hay firewall activo (caso común en muchas estaciones de trabajo de desarrollo),
        se considera abierto y permitido por defecto.
        """
        # 1. Comprobar UFW
        if self._has_ufw:
            try:
                res = subprocess.run(["ufw", "status"], capture_output=True, text=True, timeout=3)
                if res.returncode == 0:
                    out = res.stdout.lower()
                    if "inactive" in out or "inactivo" in out:
                        return {
                            "exists": True,
                            "enabled": True,
                            "firewall_type": "ufw (inactivo - abierto)",
                            "message": "UFW está inactivo; el tráfico entrante está permitido"
                        }
                    # Si UFW está activo, buscar si el puerto está en la lista de reglas
                    port_str = str(port)
                    if port_str in res.stdout and ("allow" in out or "permitir" in out):
                        return {
                            "exists": True,
                            "enabled": True,
                            "firewall_type": "ufw",
                            "message": f"Regla UFW activa permitiendo el puerto {port}"
                        }
                    return {
                        "exists": False,
                        "enabled": False,
                        "firewall_type": "ufw",
                        "message": f"UFW está activo pero no tiene regla para el puerto {port}"
                    }
            except Exception:
                pass

        # 2. Comprobar firewalld
        if self._has_firewalld:
            try:
                res = subprocess.run(["firewall-cmd", "--state"], capture_output=True, text=True, timeout=3)
                if "running" in res.stdout.lower():
                    # Comprobar si el puerto está abierto
                    chk = subprocess.run(
                        ["firewall-cmd", f"--query-port={port}/tcp"],
                        capture_output=True,
                        text=True,
                        timeout=3
                    )
                    is_open = (chk.returncode == 0 and "yes" in chk.stdout.lower())
                    return {
                        "exists": is_open,
                        "enabled": is_open,
                        "firewall_type": "firewalld",
                        "message": f"Firewalld {'permite' if is_open else 'bloquea'} el puerto {port}"
                    }
            except Exception:
                pass

        # Por defecto en Linux sin cortafuegos bloqueante activo
        return {
            "exists": True,
            "enabled": True,
            "firewall_type": "none/open",
            "message": "No se detectó cortafuegos restrictivo activo; tráfico permitido"
        }

    def repair_firewall_rule(self, port: int = 8765) -> Tuple[bool, str]:
        if self._has_ufw:
            try:
                cmd = ["sudo", "-n", "ufw", "allow", f"{port}/tcp"]
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
                if res.returncode == 0:
                    return True, f"Puerto {port}/tcp habilitado exitosamente en UFW."
                return False, f"UFW requiere permisos de administrador: {res.stderr}"
            except Exception as e:
                return False, f"Error configurando UFW: {e}"

        if self._has_firewalld:
            try:
                cmd = ["sudo", "-n", "firewall-cmd", "--permanent", f"--add-port={port}/tcp"]
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
                if res.returncode == 0:
                    subprocess.run(["sudo", "-n", "firewall-cmd", "--reload"], timeout=5)
                    return True, f"Puerto {port}/tcp añadido a firewalld."
                return False, f"firewalld requiere sudo: {res.stderr}"
            except Exception as e:
                return False, f"Error en firewalld: {e}"

        return True, "El sistema no tiene un cortafuegos activo bloqueando este puerto."
