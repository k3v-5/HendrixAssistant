import os
import tempfile
from core.plugin_engine import HendrixPlugin

class TempCleanerPlugin(HendrixPlugin):
    id = "temp_cleaner"
    name = "Limpiador de Temporales"
    description = "Escanea y purga archivos residuales temporales de Windows y cachés de render."
    version = "1.0.0"
    author = "Hendrix Team"
    icon_emoji = "🧹"
    category = "Mantenimiento del Sistema"

    def get_actions(self):
        return [
            {
                "id": "scan_temp",
                "name": "Escanear Temporales",
                "description": "Calcula cuántos archivos temporales y espacio pueden liberarse.",
                "icon_emoji": "🔍"
            },
            {
                "id": "clean_temp",
                "name": "Purgar Temporales de Usuario",
                "description": "Elimina archivos no bloqueados en la carpeta temporal de Windows.",
                "icon_emoji": "🗑️"
            }
        ]

    def execute_action(self, action_id, params=None):
        temp_dir = tempfile.gettempdir()

        if action_id == "scan_temp":
            file_count = 0
            total_bytes = 0
            try:
                for entry in os.scandir(temp_dir):
                    if entry.is_file(follow_symlinks=False):
                        file_count += 1
                        total_bytes += entry.stat().st_size
            except Exception:
                pass

            size_mb = round(total_bytes / (1024 * 1024), 1)
            return {
                "success": True,
                "message": f"Archivos temporales: {file_count} ({size_mb} MB listos para limpiar).",
                "data": {"fileCount": file_count, "sizeMb": size_mb}
            }

        elif action_id == "clean_temp":
            deleted_count = 0
            freed_bytes = 0
            try:
                for entry in os.scandir(temp_dir):
                    if entry.is_file(follow_symlinks=False):
                        try:
                            s = entry.stat().st_size
                            os.remove(entry.path)
                            deleted_count += 1
                            freed_bytes += s
                        except Exception:
                            # Ignorar archivos en uso o protegidos
                            pass
            except Exception:
                pass

            freed_mb = round(freed_bytes / (1024 * 1024), 1)
            return {
                "success": True,
                "message": f"Limpieza completada: {deleted_count} archivos eliminados ({freed_mb} MB liberados).",
                "data": {"deletedCount": deleted_count, "freedMb": freed_mb}
            }

        return {"success": False, "message": f"Acción desconocida: '{action_id}'"}
