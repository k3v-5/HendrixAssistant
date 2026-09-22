import os
import shutil
import time
from core.plugin_engine import HendrixPlugin

class BackupProjectsPlugin(HendrixPlugin):
    id = "backup_projects"
    name = "Backup de Proyectos"
    description = "Copia de seguridad rápida con timestamp de proyectos de audio, 3D o código."
    version = "1.0.0"
    author = "Hendrix Team"
    icon_emoji = "💾"
    category = "Almacenamiento & Backup"

    def get_actions(self):
        return [
            {
                "id": "backup_dropzone",
                "name": "Respaldar Buzón Dropzone",
                "description": "Copia todos los archivos de Dropzone a una carpeta de archivo fechada.",
                "icon_emoji": "📦"
            },
            {
                "id": "backup_summary",
                "name": "Resumen de Respaldos",
                "description": "Consulta cuántos respaldos existen y el espacio ocupado.",
                "icon_emoji": "📊"
            }
        ]

    def execute_action(self, action_id, params=None):
        backup_root = os.path.join(os.path.expanduser("~"), "HendrixStudio", "Backups")
        os.makedirs(backup_root, exist_ok=True)

        if action_id == "backup_dropzone":
            from storage.dropzone_manager import dropzone_manager
            src = dropzone_manager.root_path
            if not os.path.exists(src):
                return {"success": False, "message": "No se encontró el directorio de Dropzone."}

            timestamp = time.strftime("%Y%m%d_%H%M%S")
            dest = os.path.join(backup_root, f"Dropzone_Backup_{timestamp}")

            copied_count = 0
            for root, _, files in os.walk(src):
                for f in files:
                    rel = os.path.relpath(os.path.join(root, f), src)
                    target = os.path.join(dest, rel)
                    os.makedirs(os.path.dirname(target), exist_ok=True)
                    shutil.copy2(os.path.join(root, f), target)
                    copied_count += 1

            return {
                "success": True,
                "message": f"Respaldo creado con éxito en '{os.path.basename(dest)}' ({copied_count} archivos).",
                "data": {"backupPath": dest, "fileCount": copied_count}
            }

        elif action_id == "backup_summary":
            backups = [d for d in os.listdir(backup_root) if os.path.isdir(os.path.join(backup_root, d))]
            total_size = 0
            for root, _, files in os.walk(backup_root):
                for f in files:
                    total_size += os.path.getsize(os.path.join(root, f))
            size_mb = round(total_size / (1024 * 1024), 2)
            return {
                "success": True,
                "message": f"Se encontraron {len(backups)} respaldos ({size_mb} MB totales).",
                "data": {"count": len(backups), "totalMb": size_mb}
            }

        return {"success": False, "message": f"Acción desconocida: '{action_id}'"}
