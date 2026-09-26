import os
import sys
import glob
import time
import inspect
import logging
import importlib.util
from typing import Dict, List, Any, Optional

logger = logging.getLogger("hendrix_desktop.plugin_engine")

PLUGINS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "plugins"))

class HendrixPlugin:
    """
    Clase base obligatoria para todos los plugins de Hendrix Desktop.
    Diseñada bajo el principio de Open/Closed (OCP) para admitir
    scripts de usuario y automatizaciones personalizadas.
    """
    id: str = "custom_plugin"
    name: str = "Plugin Personalizado"
    description: str = "Sin descripción"
    version: str = "1.0.0"
    author: str = "Usuario"
    icon_emoji: str = "🧩"
    category: str = "General"

    def on_load(self) -> None:
        """Llamado inmediatamente después de que el plugin es cargado o recargado."""
        pass

    def get_actions(self) -> List[Dict[str, Any]]:
        """
        Retorna la lista de acciones que este plugin expone hacia la PC y el móvil.
        Cada acción debe contener:
        - id: Identificador único de la acción (str)
        - name: Nombre visible (str)
        - description: Descripción (str)
        - icon_emoji: Emoji distintivo (str, opcional)
        """
        return []

    def execute_action(self, action_id: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        Ejecuta la acción solicitada.
        Debe retornar un diccionario con:
        - success: bool
        - message: str
        - data: dict (opcional)
        """
        raise NotImplementedError("El plugin debe implementar execute_action")


class PluginEngine:
    """
    Motor de descubrimiento dinámico, validación y ejecución de plugins.
    Soporta Hot-Reload para actualizar plugins sin reiniciar la aplicación.
    """
    def __init__(self, plugins_dir: str = PLUGINS_DIR):
        self.plugins_dir = plugins_dir
        self.plugins: Dict[str, HendrixPlugin] = {}
        self.on_plugins_changed = None
        os.makedirs(self.plugins_dir, exist_ok=True)
        self.reload_plugins()

    def reload_plugins(self) -> List[Dict[str, Any]]:
        """Escanea el directorio de plugins y recarga todos los módulos válidos."""
        logger.info(f"Escaneando plugins en: {self.plugins_dir}")
        loaded = {}

        pattern = os.path.join(self.plugins_dir, "plugin_*.py")
        plugin_files = glob.glob(pattern)

        for file_path in plugin_files:
            file_name = os.path.basename(file_path)
            module_name = f"hendrix_plugin_{os.path.splitext(file_name)[0]}"
            try:
                spec = importlib.util.spec_from_file_location(module_name, file_path)
                if not spec or not spec.loader:
                    continue
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)

                # Buscar clases que hereden de HendrixPlugin
                found = False
                for attr_name in dir(module):
                    attr = getattr(module, attr_name)
                    if (
                        inspect.isclass(attr)
                        and issubclass(attr, HendrixPlugin)
                        and attr is not HendrixPlugin
                    ):
                        plugin_instance: HendrixPlugin = attr()
                        plugin_instance.on_load()
                        loaded[plugin_instance.id] = plugin_instance
                        logger.info(f"✅ Plugin cargado: [{plugin_instance.id}] {plugin_instance.name} v{plugin_instance.version}")
                        found = True
                        break

                if not found:
                    logger.warning(f"⚠️ El archivo {file_name} no define ninguna clase que herede de HendrixPlugin.")
            except Exception as e:
                logger.error(f"❌ Error al cargar plugin {file_name}: {e}", exc_info=True)

        self.plugins = loaded
        if self.on_plugins_changed:
            try:
                self.on_plugins_changed()
            except Exception:
                pass
        return self.get_all_plugins_metadata()

    def get_all_plugins_metadata(self) -> List[Dict[str, Any]]:
        """Retorna la lista de plugins instalados con sus acciones disponibles serializables."""
        result = []
        for plugin in self.plugins.values():
            actions = plugin.get_actions()
            normalized_actions = []
            for act in actions:
                act_copy = dict(act)
                label = act.get("label") or act.get("name") or act.get("id", "Acción")
                act_copy["label"] = label
                act_copy["name"] = label
                normalized_actions.append(act_copy)
            result.append({
                "id": plugin.id,
                "name": plugin.name,
                "description": plugin.description,
                "version": plugin.version,
                "author": plugin.author,
                "iconEmoji": plugin.icon_emoji,
                "icon": plugin.icon_emoji or "code",
                "category": plugin.category,
                "actions": normalized_actions
            })
        return result

    def execute_action(
        self,
        plugin_id: str,
        action_id: str,
        params: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Ejecuta una acción con captura estricta de excepciones."""
        plugin = self.plugins.get(plugin_id)
        if not plugin:
            return {
                "success": False,
                "message": f"Plugin no encontrado: '{plugin_id}'",
                "pluginId": plugin_id,
                "actionId": action_id
            }

        start_time = time.time()
        try:
            res = plugin.execute_action(action_id, params or {})
            elapsed_ms = int((time.time() - start_time) * 1000)
            if not isinstance(res, dict):
                res = {"success": True, "message": str(res)}
            res.setdefault("pluginId", plugin_id)
            res.setdefault("actionId", action_id)
            res.setdefault("elapsedMs", elapsed_ms)
            return res
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            logger.error(f"Error ejecutando acción '{action_id}' en plugin '{plugin_id}': {e}", exc_info=True)
            return {
                "success": False,
                "message": f"Excepción en plugin: {str(e)}",
                "pluginId": plugin_id,
                "actionId": action_id,
                "elapsedMs": elapsed_ms
            }

plugin_engine = PluginEngine()
