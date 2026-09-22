import subprocess
import shutil
from core.plugin_engine import HendrixPlugin

class CommandRunnerPlugin(HendrixPlugin):
    id = "system_diagnostics"
    name = "Diagnóstico Rápido del Sistema"
    description = "Herramientas de red y sistema (Flush DNS, comprobación de espacio en discos)."
    version = "1.0.0"
    author = "Hendrix Team"
    icon_emoji = "⚡"
    category = "Sistema & Red"

    def get_actions(self):
        return [
            {
                "id": "flush_dns",
                "name": "Vaciar Caché DNS",
                "description": "Ejecuta ipconfig /flushdns para resolver problemas de red o túneles.",
                "icon_emoji": "🌐"
            },
            {
                "id": "disk_space",
                "name": "Espacio en Disco Principal",
                "description": "Comprueba el espacio libre y total en la unidad del sistema C:.",
                "icon_emoji": "💾"
            }
        ]

    def execute_action(self, action_id, params=None):
        if action_id == "flush_dns":
            try:
                res = subprocess.run(
                    ["ipconfig", "/flushdns"],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                if res.returncode == 0:
                    return {
                        "success": True,
                        "message": "Caché de resolución DNS de Windows vaciada correctamente."
                    }
                else:
                    return {
                        "success": False,
                        "message": f"Error ejecutando flushdns: {res.stderr}"
                    }
            except Exception as e:
                return {"success": False, "message": f"Error: {e}"}

        elif action_id == "disk_space":
            try:
                total, used, free = shutil.disk_usage("C:\\")
                total_gb = round(total / (1024**3), 1)
                free_gb = round(free / (1024**3), 1)
                used_pct = round((used / total) * 100, 1)

                return {
                    "success": True,
                    "message": f"Disco C: {free_gb} GB libres de {total_gb} GB ({used_pct}% en uso).",
                    "data": {"freeGb": free_gb, "totalGb": total_gb, "usedPercent": used_pct}
                }
            except Exception as e:
                return {"success": False, "message": f"Error consultando disco: {e}"}

        return {"success": False, "message": f"Acción desconocida: '{action_id}'"}
