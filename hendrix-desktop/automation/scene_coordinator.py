import time
import logging
import asyncio
import ctypes
import pyautogui
from typing import Dict, Any
from core.audio_mixer import audio_mixer
from core.hardware_watchdog import hardware_watchdog
from automation.uia_manager import uia_manager
from automation.ableton_manager import ableton_manager

logger = logging.getLogger("hendrix_desktop.scene_coordinator")

class SceneCoordinator:
    """
    Coordinador de macros encadenadas y escenas de estudio para Windows.
    Permite ejecutar flujos multi-paso orquestando energía, audio, programas y centinela.
    """

    async def execute_scene(self, scene_id: str, emit_callback=None) -> Dict[str, Any]:
        logger.info(f"🎬 Iniciando ejecución de escena de estudio: {scene_id}")

        if scene_id == "STUDIO_MUSIC_MODE":
            # 1. Lanzar DAW (Ableton / FL Studio)
            ableton_manager.execute_action("LAUNCH_OR_FOCUS")
            await asyncio.sleep(1.0)
            # 2. Configurar volumen de estudio al 80%
            audio_mixer.set_master_volume(80)
            audio_mixer.set_app_volume("Ableton", 90)
            audio_mixer.set_app_mute("Discord", True)
            audio_mixer.set_app_mute("chrome", True)

            return {
                "sceneId": scene_id,
                "success": True,
                "stepsExecuted": 4,
                "totalSteps": 4,
                "message": "Entorno de Producción Musical listo. DAW enfocado y audio balanceado al 80%."
            }

        elif scene_id == "STUDIO_RENDER_NIGHT_MODE":
            # 1. Asegurar guardado en Blender
            pyautogui.hotkey("ctrl", "s")
            await asyncio.sleep(0.5)
            # 2. Iniciar vigilancia con auto-suspensión
            hardware_watchdog.start_render_watchdog(
                process_name="blender",
                auto_suspend=True,
                on_completed=emit_callback
            )

            return {
                "sceneId": scene_id,
                "success": True,
                "stepsExecuted": 2,
                "totalSteps": 2,
                "message": "Render nocturno activado. El centinela suspenderá la PC tras terminar y sincronizar con Drive."
            }

        elif scene_id == "STUDIO_CLOSE_MODE":
            # 1. Salvar cambios
            pyautogui.hotkey("ctrl", "s")
            await asyncio.sleep(2.5)
            # 2. Silenciar audio
            audio_mixer.set_master_mute(True)
            # 3. Poner en suspensión
            ctypes.windll.PowrProf.SetSuspendState(0, 1, 0)

            return {
                "sceneId": scene_id,
                "success": True,
                "stepsExecuted": 3,
                "totalSteps": 3,
                "message": "Estudio cerrado de forma segura. Cambios guardados y PC suspendida."
            }

        elif scene_id == "STUDIO_STREAMING_MODE":
            audio_mixer.set_master_volume(75)
            audio_mixer.set_app_volume("Spotify", 40)
            uia_manager.execute_quick_command("open:chrome")

            return {
                "sceneId": scene_id,
                "success": True,
                "stepsExecuted": 2,
                "totalSteps": 2,
                "message": "Modo Streaming configurado con éxito."
            }

        else:
            return {
                "sceneId": scene_id,
                "success": False,
                "stepsExecuted": 0,
                "totalSteps": 1,
                "message": f"Escena desconocida: {scene_id}"
            }

scene_coordinator = SceneCoordinator()
