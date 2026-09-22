import os
import glob
import logging
from typing import List, Dict, Any, Optional

logger = logging.getLogger("hendrix_desktop.project_browser")

EXT_CATEGORIES = {
    # Audio DAWs
    ".als": "AUDIO_DAW",
    ".flp": "AUDIO_DAW",
    ".cpr": "AUDIO_DAW",
    ".rpp": "AUDIO_DAW",
    # 3D & VFX
    ".blend": "THREE_D_VFX",
    ".uproject": "THREE_D_VFX",
    ".c4d": "THREE_D_VFX",
    # Video & Design
    ".prproj": "VIDEO_DESIGN",
    ".aep": "VIDEO_DESIGN",
    ".psd": "VIDEO_DESIGN",
    ".ai": "VIDEO_DESIGN",
    ".drp": "VIDEO_DESIGN",
    # Code & Workspaces
    ".py": "CODE_DEV",
    ".kt": "CODE_DEV",
    ".workspace": "CODE_DEV"
}

class ProjectBrowser:
    """
    Indexador y lanzador remoto de proyectos de trabajo (.als, .flp, .blend, .prproj, etc.)
    en Windows para control instantáneo desde el asistente móvil.
    """

    def __init__(self, custom_roots: Optional[List[str]] = None):
        self.custom_roots = custom_roots or []

    def _get_search_roots(self) -> List[str]:
        roots = []
        user_profile = os.environ.get("USERPROFILE", "")
        if user_profile:
            for sub in ["Documents", "Music", "Desktop", "Downloads", "Videos", "OneDrive"]:
                p = os.path.join(user_profile, sub)
                if os.path.isdir(p):
                    roots.append(p)

        # Buscar en unidades secundarias comunes (D:, E:, etc.)
        for drive in ["D:\\", "E:\\", "C:\\Proyectos", "D:\\Proyectos", "D:\\Musica", "D:\\Blender"]:
            if os.path.isdir(drive) and drive not in roots:
                roots.append(drive)

        for cr in self.custom_roots:
            if os.path.isdir(cr) and cr not in roots:
                roots.append(cr)

        return roots

    def query_projects(self, category: Optional[str] = None, search_query: Optional[str] = None, max_results: int = 40) -> List[Dict[str, Any]]:
        results = []
        seen_paths = set()
        roots = self._get_search_roots()
        query_lower = (search_query or "").strip().lower()

        for root in roots:
            try:
                # Recorrer hasta 3 niveles de profundidad para mantener velocidad ultra-rápida
                for dirpath, dirnames, filenames in os.walk(root):
                    # Excluir carpetas temporales / pesadas
                    dirnames[:] = [d for d in dirnames if not d.startswith(".") and d.lower() not in {"node_modules", "target", "build", "cache", "temp", "appdata", "windows", "program files"}]

                    rel_depth = os.path.relpath(dirpath, root).count(os.sep)
                    if rel_depth > 3:
                        dirnames.clear()
                        continue

                    for fname in filenames:
                        base, ext = os.path.splitext(fname)
                        ext_lower = ext.lower()

                        if ext_lower not in EXT_CATEGORIES:
                            continue

                        cat = EXT_CATEGORIES[ext_lower]
                        if category and category.upper() != cat:
                            continue

                        if query_lower and query_lower not in base.lower():
                            continue

                        full_path = os.path.join(dirpath, fname)
                        if full_path in seen_paths:
                            continue
                        seen_paths.add(full_path)

                        try:
                            st = os.stat(full_path)
                            size_bytes = st.st_size
                            mtime_epoch = int(st.st_mtime * 1000)
                        except Exception:
                            size_bytes = 0
                            mtime_epoch = 0

                        results.append({
                            "id": f"{base}_{len(results)}",
                            "name": base,
                            "path": full_path,
                            "category": cat,
                            "extension": ext_lower.removeprefix("."),
                            "sizeBytes": size_bytes,
                            "lastModifiedEpoch": mtime_epoch
                        })
            except Exception as e:
                logger.debug(f"Error explorando ruta {root}: {e}")

        # Ordenar por fecha de última modificación descendente
        results.sort(key=lambda x: x["lastModifiedEpoch"], reverse=True)
        return results[:max_results]

    def launch_project(self, project_path: str) -> bool:
        if not os.path.isfile(project_path):
            logger.error(f"El archivo de proyecto no existe: {project_path}")
            return False

        try:
            os.startfile(project_path)
            logger.info(f"🚀 Proyecto lanzado con éxito: {project_path}")
            return True
        except Exception as e:
            logger.error(f"Error al lanzar proyecto {project_path}: {e}")
            return False

project_browser = ProjectBrowser()
