import time
import re
import logging
from typing import Dict, Any, Optional, Callable, List

logger = logging.getLogger("hendrix_desktop.terminal_watchdog")

class TerminalWatchdog:
    """
    Vigilante proactivo de terminales y procesos de compilación en Windows.
    Detecta fallos en Gradle, npm, Python, Cargo, Vite o scripts de usuario,
    capturando el error exacto y alertando al móvil con diagnóstico y solución rápida.
    """

    ERROR_PATTERNS = [
        (r"BUILD FAILED in (.*)", "Gradle"),
        (r"npm ERR! (.*)", "npm"),
        (r"Traceback \(most recent call last\):", "Python"),
        (r"SyntaxError: (.*)", "JavaScript / Python"),
        (r"error\[E\d+\]: (.*)", "Rust Cargo"),
        (r"fatal error: (.*)", "C/C++ Compiler"),
        (r"NullPointerException", "Java / Kotlin"),
        (r"Unresolved reference: (.*)", "Kotlin Compiler"),
        (r"ModuleNotFoundError: (.*)", "Python Runtime")
    ]

    def __init__(self):
        self._callbacks: List[Callable[[Dict[str, Any]], None]] = []
        self._recent_errors: List[Dict[str, Any]] = []

    def register_callback(self, callback: Callable[[Dict[str, Any]], None]):
        if callback not in self._callbacks:
            self._callbacks.append(callback)

    def unregister_callback(self, callback: Callable[[Dict[str, Any]], None]):
        if callback in self._callbacks:
            self._callbacks.remove(callback)

    def analyze_log_content(self, log_text: str, source_name: str = "Terminal") -> Optional[Dict[str, Any]]:
        """Analiza un texto o log de salida buscando errores críticos de compilación o ejecución."""
        detected_tool = None
        matched_snippet = None

        for pattern, tool in self.ERROR_PATTERNS:
            match = re.search(pattern, log_text, re.IGNORECASE)
            if match:
                detected_tool = tool
                # Extraer hasta 15 líneas alrededor de la coincidencia
                lines = log_text.splitlines()
                match_pos = 0
                for idx, l in enumerate(lines):
                    if re.search(pattern, l, re.IGNORECASE):
                        match_pos = idx
                        break
                start_l = max(0, match_pos - 2)
                end_l = min(len(lines), match_pos + 12)
                matched_snippet = "\n".join(lines[start_l:end_l])
                break

        if not detected_tool:
            return None

        error_data = {
            "errorId": f"err_{int(time.time() * 1000)}",
            "source": source_name,
            "detectedTool": detected_tool,
            "logSnippet": matched_snippet or log_text[:500],
            "timestampEpoch": int(time.time() * 1000),
            "suggestedAction": f"Diagnosticar fallo de {detected_tool} con IA en Hendrix"
        }

        self._recent_errors.append(error_data)
        if len(self._recent_errors) > 20:
            self._recent_errors.pop(0)

        self._dispatch_alert(error_data)
        return error_data

    def get_recent_errors(self) -> List[Dict[str, Any]]:
        return list(self._recent_errors)

    def clear_errors(self):
        self._recent_errors.clear()

    def _dispatch_alert(self, error_data: Dict[str, Any]):
        payload = {
            "type": "TERMINAL_BUILD_ERROR",
            "error": error_data,
            "timestampEpoch": int(time.time() * 1000)
        }
        for cb in list(self._callbacks):
            try:
                cb(payload)
            except Exception as e:
                logger.debug(f"Error despachando error de terminal: {e}")

terminal_watchdog = TerminalWatchdog()
