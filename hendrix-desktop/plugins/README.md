# Motor de Plugins y Scripts de Usuario en Python — Hendrix Desktop

El **Motor de Plugins de Hendrix Desktop** permite extender las capacidades del sistema añadiendo scripts en Python dentro de esta carpeta (`hendrix-desktop/plugins/`).

Cada plugin que coloques aquí es descubierto automáticamente en tiempo de ejecución, permitiendo que sus acciones se expongan tanto en la **interfaz gráfica de Windows** como en los **botones táctiles y comandos de voz de la aplicación móvil de Hendrix**.

---

## 1. ¿Cómo Funciona?

1. **Descubrimiento Dinámico:** Al iniciar Hendrix Desktop (o al presionar **"🔄 Recargar Plugins (Hot Reload)"**), el motor escanea todos los archivos que coincidan con el patrón `plugin_*.py`.
2. **Carga Segura:** Se carga el módulo mediante `importlib` y se localiza la clase que herede de `HendrixPlugin`.
3. **Registro en el Ecosistema:**
   - La aplicación de escritorio registra las acciones en su panel.
   - El servidor WebSocket transmite el catálogo hacia el teléfono móvil (`PLUGINS_QUERY`).
   - El asistente móvil renderiza dinámicamente las tarjetas táctiles y habilita el reconocimiento de voz para invocar las acciones.

---

## 2. Cómo Crear un Plugin en 10 Líneas de Código

Crea un archivo con el prefijo `plugin_`, por ejemplo: `plugin_mi_script.py`:

```python
from core.plugin_engine import HendrixPlugin

class MiScriptPlugin(HendrixPlugin):
    id = "mi_script"                                   # Identificador único
    name = "Mi Script Personalizado"                    # Nombre visible
    description = "Automatización personalizada en PC"  # Descripción
    icon_emoji = "🚀"                                   # Icono
    category = "Productividad"                          # Categoría

    def get_actions(self):
        return [
            {
                "id": "saludo",
                "name": "Ejecutar Tarea",
                "description": "Realiza la acción principal",
                "icon_emoji": "⚡"
            }
        ]

    def execute_action(self, action_id, params=None):
        if action_id == "saludo":
            # Tu lógica en Python aquí:
            return {"success": True, "message": "¡Tarea ejecutada con éxito desde Hendrix!"}
        return {"success": False, "message": f"Acción desconocida: {action_id}"}
```

¡Eso es todo! Guarda el archivo, pulsa **"Recargar Plugins"** en Hendrix Desktop y la nueva macro aparecerá en tu teléfono y en tu PC.

---

## 3. Ciclo de Vida y Métodos Obligatorios

| Método | Descripción | Retorno Obligatorio |
|---|---|---|
| `on_load(self)` | Se ejecuta una sola vez cuando el plugin es cargado o recargado. Ideal para verificar dependencias o crear directorios necesarios. | `None` |
| `get_actions(self)` | Declara la lista de acciones disponibles que el usuario podrá invocar desde su celular o PC. | `List[Dict[str, Any]]` con campos `id`, `name`, `description`, `icon_emoji`. |
| `execute_action(self, action_id, params=None)` | Punto de entrada para ejecutar la lógica de la acción solicitada. | `Dict[str, Any]` con al menos `{"success": bool, "message": str}`. |

---

## 4. Usos Recomendados

- **Copias de Seguridad y Versionado:** Scripts para respaldar proyectos de audio (`.als`, `.flp`) o 3D (`.blend`) a un disco externo, NAS o nube secundaria.
- **Mantenimiento y Optimización de Windows:** Liberación de cachés de compilación, vaciado de carpetas temporales (`%TEMP%`), reinicio de interfaces de red o vaciado de DNS.
- **Domótica y Estudio Físico:** Integración con APIs de luces RGB (Razer Chroma, Elgato Key Light, Philips Hue) para encender las luces de grabación al empezar una sesión.
- **Renders y Exportaciones Desatendidas:** Invocar renderizadores por línea de comandos (ej. `blender -b escena.blend -a`) o compresión de video con FFmpeg.

---

## 5. Limitaciones Técnicas y Consideraciones de Seguridad

> [!WARNING]
> **Privilegios de Usuario:**
> Los plugins se ejecutan bajo el mismo nivel de permisos del proceso de Hendrix Desktop (usuario estándar de Windows). Si un script requiere permisos de Administrador, debe solicitar explícitamente elevación UAC (por ejemplo, mediante PowerShell con `-Verb RunAs`).

> [!IMPORTANT]
> **Aislamiento de Errores:**
> El motor envuelve todas las llamadas a `execute_action` en bloques de captura de excepciones. Si tu script falla o lanza una excepción no controlada (`Exception`), Hendrix **no se cerrará**; capturará el error y reportará al teléfono móvil un mensaje explicativo con el detalle del fallo.

> [!CAUTION]
> **Bloqueo del Hilo Principal y Timeouts:**
> - Las acciones deben retornar en un tiempo razonable. Si tu plugin realiza una tarea muy pesada (como exportar un video de 30 minutos), debes iniciarla en un hilo en segundo plano (`threading.Thread`) o proceso separado (`subprocess.Popen`), retornando de inmediato un mensaje de confirmación como: `"Render iniciado en segundo plano"`.
> - Evita bucles infinitos (`while True:`) dentro de `execute_action`.

> [!TIP]
> **Librerías Externas:**
> Puedes utilizar cualquier módulo estándar de Python (`os`, `sys`, `shutil`, `subprocess`, `urllib`, `socket`, `time`, `json`, `math`). Si tu plugin requiere librerías de terceros (como `requests` o `Pillow`), asegúrate de instalarlas en el entorno virtual de Python donde corre Hendrix Desktop.

---

## 6. Plugins de Ejemplo Incluidos

En esta misma carpeta encontrarás tres implementaciones de referencia completas:

1. [`plugin_backup_projects.py`](file:///d:/Proyectos/TEST/HendrixAssistant-main/hendrix-desktop/plugins/plugin_backup_projects.py): Respaldo fechado y resumen de espacio ocupado.
2. [`plugin_temp_cleaner.py`](file:///d:/Proyectos/TEST/HendrixAssistant-main/hendrix-desktop/plugins/plugin_temp_cleaner.py): Escaneo y purga de archivos temporales del sistema.
3. [`plugin_command_runner.py`](file:///d:/Proyectos/TEST/HendrixAssistant-main/hendrix-desktop/plugins/plugin_command_runner.py): Diagnóstico de red con vaciado de caché DNS e informe de almacenamiento en disco C:.
