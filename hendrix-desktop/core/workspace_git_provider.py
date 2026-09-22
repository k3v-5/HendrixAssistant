import os
import subprocess
import logging
from typing import Dict, Any, List, Optional
import psutil

logger = logging.getLogger("hendrix_desktop.workspace_git")

class WorkspaceGitProvider:
    """
    Proveedor de contexto de desarrollo y repositorios Git activos en la PC.
    Permite a Hendrix responder consultas sobre el estado de trabajo del usuario
    y alimentar el diagnóstico multimodal.
    """

    KNOWN_DEV_PROCESSES = {
        "code.exe": "VS Code",
        "idea64.exe": "IntelliJ IDEA",
        "studio64.exe": "Android Studio",
        "pycharm64.exe": "PyCharm",
        "sublime_text.exe": "Sublime Text",
        "blender.exe": "Blender",
        "ableton live 11 suite.exe": "Ableton Live",
        "ableton live 12 suite.exe": "Ableton Live",
        "fl64.exe": "FL Studio",
        "adobe premiere pro.exe": "Adobe Premiere",
        "windowsterminal.exe": "Windows Terminal"
    }

    def __init__(self, default_workspace_dir: Optional[str] = None):
        self.default_workspace_dir = default_workspace_dir or os.path.abspath(
            os.path.join(os.path.dirname(__file__), "..", "..")
        )

    def get_workspace_context(self, target_dir: Optional[str] = None) -> Dict[str, Any]:
        """Obtiene el estado completo de desarrollo (Git + Procesos creativos/dev abiertos)."""
        scan_dir = target_dir or self.default_workspace_dir
        git_info = self.get_git_status(scan_dir)
        active_apps = self.get_active_creative_and_dev_apps()

        return {
            "workspaceDirectory": scan_dir,
            "git": git_info,
            "activeDevApps": active_apps,
            "hasUncommittedChanges": git_info.get("modifiedCount", 0) > 0,
            "summaryText": self._build_human_summary(git_info, active_apps)
        }

    def get_git_status(self, repo_path: str) -> Dict[str, Any]:
        """Extrae el estado de Git de un directorio específico de forma segura."""
        if not os.path.exists(os.path.join(repo_path, ".git")):
            # Buscar en subdirectorios o padre
            parent = os.path.dirname(repo_path)
            if os.path.exists(os.path.join(parent, ".git")):
                repo_path = parent
            else:
                return {
                    "isGitRepo": False,
                    "repoName": os.path.basename(repo_path),
                    "branch": "none",
                    "modifiedCount": 0,
                    "modifiedFiles": [],
                    "latestCommit": ""
                }

        repo_name = os.path.basename(repo_path)
        branch = "unknown"
        modified_files = []
        latest_commit = ""

        try:
            # 1. Obtener rama actual
            res_branch = subprocess.run(
                ["git", "rev-parse", "--abbrev-ref", "HEAD"],
                cwd=repo_path,
                capture_output=True,
                text=True,
                timeout=2.0
            )
            if res_branch.returncode == 0:
                branch = res_branch.stdout.strip()

            # 2. Archivos modificados o sin seguimiento
            res_status = subprocess.run(
                ["git", "status", "--porcelain"],
                cwd=repo_path,
                capture_output=True,
                text=True,
                timeout=2.5
            )
            if res_status.returncode == 0:
                lines = [line.strip() for line in res_status.stdout.splitlines() if line.strip()]
                modified_files = [line.split(maxsplit=1)[-1] for line in lines[:10]]
                total_modified = len(lines)
            else:
                total_modified = 0

            # 3. Último commit
            res_log = subprocess.run(
                ["git", "log", "-1", "--format=%s (%cr)"],
                cwd=repo_path,
                capture_output=True,
                text=True,
                timeout=2.0
            )
            if res_log.returncode == 0:
                latest_commit = res_log.stdout.strip()

            return {
                "isGitRepo": True,
                "repoName": repo_name,
                "branch": branch,
                "modifiedCount": total_modified,
                "modifiedFiles": modified_files,
                "latestCommit": latest_commit
            }
        except Exception as e:
            logger.debug(f"Error consultando Git: {e}")
            return {
                "isGitRepo": True,
                "repoName": repo_name,
                "branch": branch,
                "modifiedCount": len(modified_files),
                "modifiedFiles": modified_files,
                "latestCommit": latest_commit
            }

    def get_active_creative_and_dev_apps(self) -> List[str]:
        """Detecta qué programas de desarrollo o diseño están en ejecución en Windows."""
        active = []
        running_names = set()
        try:
            for p in psutil.process_iter(['name']):
                try:
                    name = p.info['name'].lower()
                    running_names.add(name)
                except Exception:
                    continue

            for proc_key, display_name in self.KNOWN_DEV_PROCESSES.items():
                if proc_key in running_names and display_name not in active:
                    active.append(display_name)
        except Exception:
            pass
        return active

    def _build_human_summary(self, git_info: Dict[str, Any], active_apps: List[str]) -> str:
        apps_str = ", ".join(active_apps) if active_apps else "el escritorio de Windows"
        if git_info.get("isGitRepo", False):
            branch = git_info.get("branch", "main")
            mod_count = git_info.get("modifiedCount", 0)
            repo = git_info.get("repoName", "proyecto")
            changes_str = f"con {mod_count} archivos modificados sin confirmar" if mod_count > 0 else "sin cambios pendientes"
            return f"Estás trabajando en '{repo}' (rama {branch}, {changes_str}). Tienes abierto {apps_str}."
        return f"Estás en la PC con {apps_str} activo."

workspace_git_provider = WorkspaceGitProvider()
