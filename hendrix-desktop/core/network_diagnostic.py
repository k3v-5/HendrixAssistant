import socket
import subprocess
import logging

logger = logging.getLogger("hendrix_desktop.network_diagnostic")

class NetworkDiagnostic:
    @staticmethod
    def check_port_listening(port: int, host: str = "127.0.0.1") -> bool:
        """Comprueba si el servidor WebSocket de Hendrix está escuchando activamente en el puerto."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(1.0)
                result = s.connect_ex((host, port))
                return result == 0
        except Exception as e:
            logger.warning(f"Error comprobando puerto {port}: {e}")
            return False

    @staticmethod
    def get_lan_info(config_obj) -> dict:
        """Recupera la información de red local y WAN."""
        return {
            "hostname": socket.gethostname(),
            "local_ip": getattr(config_obj, "local_ip", "127.0.0.1"),
            "port": getattr(config_obj, "port", 8899),
            "tunnel_url": getattr(config_obj, "remote_tunnel_url", None)
        }

    @staticmethod
    def check_windows_firewall_rule(rule_name: str = "Hendrix Desktop Companion") -> dict:
        """
        Consulta las reglas del Firewall de Windows mediante netsh.
        Retorna un dict con el estado de la regla.
        """
        try:
            cmd = ["netsh", "advfirewall", "firewall", "show", "rule", f"name={rule_name}"]
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                encoding="cp850",
                errors="replace",
                timeout=5
            )
            out = result.stdout
            if result.returncode != 0 or "No se encontraron reglas" in out or "No rules match" in out:
                return {
                    "exists": False,
                    "enabled": False,
                    "message": "No se encontró regla de entrada en el Firewall de Windows",
                    "raw": out
                }

            # Analizar si está habilitada
            lower_out = out.lower()
            is_enabled = "sí" in lower_out or "yes" in lower_out or "habilitada:\t\tsí" in lower_out or "enabled:\t\tyes" in lower_out

            return {
                "exists": True,
                "enabled": is_enabled,
                "message": "Regla de entrada configurada y activa" if is_enabled else "Regla encontrada pero deshabilitada",
                "raw": out
            }
        except Exception as e:
            return {
                "exists": False,
                "enabled": False,
                "message": f"Error al consultar el Firewall: {e}",
                "raw": ""
            }

    @staticmethod
    def repair_windows_firewall_rule(port: int = 8899, rule_name: str = "Hendrix Desktop Companion") -> tuple[bool, str]:
        """
        Invoca la creación/reparación de la regla de entrada en Windows Defender Firewall
        utilizando PowerShell con elevación de permisos (UAC RunAs).
        """
        try:
            # Primero intentar eliminar regla obsoleta si existiera duplicada
            del_cmd = f"netsh advfirewall firewall delete rule name='{rule_name}'"
            add_cmd = f"netsh advfirewall firewall add rule name='{rule_name}' dir=in action=allow protocol=TCP localport={port}"

            ps_script = (
                f"Start-Process cmd.exe -ArgumentList '/c {del_cmd} & {add_cmd}' -Verb RunAs -Wait"
            )

            res = subprocess.run(
                ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ps_script],
                capture_output=True,
                text=True,
                timeout=20
            )

            # Verificar si se aplicó
            check = NetworkDiagnostic.check_windows_firewall_rule(rule_name)
            if check.get("exists") and check.get("enabled"):
                return True, f"Regla '{rule_name}' en puerto {port} TCP creada y activada con éxito."
            elif check.get("exists"):
                return True, f"Regla creada pero verificar estado: {check.get('message')}"
            else:
                return False, "La regla no pudo ser verificada tras solicitar elevación."
        except subprocess.TimeoutExpired:
            return False, "Tiempo de espera agotado esperando la confirmación de elevación UAC."
        except Exception as e:
            return False, f"Error al ejecutar reparación de firewall: {e}"

network_diagnostic = NetworkDiagnostic()
