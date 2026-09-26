import os
import time
import subprocess
import ctypes
import pyautogui
import json
from typing import Optional
from core.security_guard import SecurityGuard

KNOWN_APPS = {
    "chrome": "chrome",
    "navegador": "chrome",
    "vs code": "code",
    "visual studio code": "code",
    "codigo": "code",
    "spotify": "spotify",
    "musica": "spotify",
    "calculadora": "calc",
    "bloc de notas": "notepad",
    "notas": "notepad",
    "word": "winword",
    "excel": "excel",
    "terminal": "wt",
    "powershell": "powershell",
    "cmd": "cmd",
    "steam": "steam",
    "explorador": "explorer",
    "blender": "blender",
    "unreal": "unreal",
    "unreal engine": "unreal",
    "unreal engine 5": "unreal",
    "ue5": "unreal",
    "epic games": "epic games"
}


def find_unreal_engine_path() -> Optional[str]:
    """
    Localiza de manera inteligente el ejecutable UnrealEditor.exe inspeccionando
    los manifiestos de Epic Games y las rutas habituales de instalación en Windows.
    """
    # 1. Inspeccionar manifiestos de Epic Games Launcher
    manifest_dir = r"C:\ProgramData\Epic\EpicGamesLauncher\Data\Manifests"
    if os.path.isdir(manifest_dir):
        try:
            for item_file in os.listdir(manifest_dir):
                if item_file.endswith(".item"):
                    full_item = os.path.join(manifest_dir, item_file)
                    try:
                        with open(full_item, "r", encoding="utf-8") as fp:
                            data = json.load(fp)
                            if data.get("DisplayName") == "Unreal Engine" or "engines/ue5" in data.get("AppCategories", []):
                                install_loc = data.get("InstallLocation", "")
                                launch_exe = data.get("LaunchExecutable", "Engine/Binaries/Win64/UnrealEditor.exe")
                                candidate = os.path.normpath(os.path.join(install_loc, launch_exe))
                                if os.path.isfile(candidate):
                                    return candidate
                    except Exception:
                        continue
        except Exception:
            pass

    # 2. Búsqueda en rutas estándar a través de los discos disponibles
    for drive in ["E", "D", "C", "F"]:
        for version in ["5.8", "5.7", "5.6", "5.5", "5.4", "5.3", "5.2", "5.1", "5.0"]:
            p1 = rf"{drive}:\UE\UE_{version}\Engine\Binaries\Win64\UnrealEditor.exe"
            if os.path.isfile(p1):
                return p1
            p2 = rf"{drive}:\Program Files\Epic Games\UE_{version}\Engine\Binaries\Win64\UnrealEditor.exe"
            if os.path.isfile(p2):
                return p2

    return None

class UiaManager:
    def execute_quick_command(self, command: str) -> bool:
        if not SecurityGuard.can_execute_commands():
            return False

        cmd = command.lower().strip()

        if cmd == "shutdown":
            subprocess.run(["shutdown", "/s", "/t", "10"], shell=True)
            return True
        elif cmd == "restart":
            subprocess.run(["shutdown", "/r", "/t", "10"], shell=True)
            return True
        elif cmd == "sleep":
            # Poner Windows en suspensión usando SetSuspendState
            ctypes.windll.PowrProf.SetSuspendState(0, 1, 0)
            return True
        elif cmd == "lock":
            ctypes.windll.user32.LockWorkStation()
            return True
        elif cmd.startswith("unlock:"):
            pin_or_pwd = command.split("unlock:", 1)[1].strip()
            return self.unlock_workstation(pin_or_pwd)
        elif cmd == "wake":
            pyautogui.press("shift")
            return True
        elif cmd == "volume_up":
            pyautogui.press("volumeup")
            return True
        elif cmd == "volume_down":
            pyautogui.press("volumedown")
            return True
        elif cmd == "volume_mute":
            pyautogui.press("volumemute")
            return True
        elif cmd == "media_play_pause":
            pyautogui.press("playpause")
            return True
        elif cmd == "media_next":
            pyautogui.press("nexttrack")
            return True
        elif cmd == "media_prev":
            pyautogui.press("prevtrack")
            return True
        elif cmd.startswith("launch:"):
            app_name = cmd.split("launch:", 1)[1].strip()
            return self.launch_app(app_name)

        return False

    def launch_app(self, app_query: str) -> bool:
        clean_query = app_query.lower().strip()

        # Caso especial: Unreal Engine
        if clean_query in ["unreal", "unreal engine", "unreal engine 5", "ue5"]:
            ue_path = find_unreal_engine_path()
            if ue_path and os.path.isfile(ue_path):
                try:
                    subprocess.Popen([ue_path])
                    return True
                except Exception:
                    pass
            # Intentar protocolo de Epic Games Launcher
            try:
                os.startfile("com.epicgames.launcher://apps/UE_5.8?action=launch&silent=true")
                return True
            except Exception:
                try:
                    os.startfile("com.epicgames.launcher://")
                    return True
                except Exception:
                    pass

        # Caso especial: Blender
        if clean_query in ["blender", "blender 3d"]:
            for drive in ["C", "D", "E", "F"]:
                bp = rf"{drive}:\Program Files\Blender Foundation"
                if os.path.isdir(bp):
                    for folder in os.listdir(bp):
                        b_exe = os.path.join(bp, folder, "blender.exe")
                        if os.path.isfile(b_exe):
                            try:
                                subprocess.Popen([b_exe])
                                return True
                            except Exception:
                                pass

        app_target = KNOWN_APPS.get(clean_query, app_query)
        try:
            os.startfile(app_target)
            return True
        except Exception:
            try:
                subprocess.Popen(app_target, shell=True)
                return True
            except Exception:
                return False

    def unlock_workstation(self, pin_or_pwd: str) -> bool:
        """
        Envía eventos de teclado para descartar la pantalla de bloqueo de Windows,
        enfocar el campo de credenciales e ingresar el PIN del usuario.
        """
        try:
            # 1. Despertar pantalla si el monitor está en reposo
            ctypes.windll.user32.keybd_event(0x20, 0, 0, 0) # Space down
            ctypes.windll.user32.keybd_event(0x20, 0, 2, 0) # Space up
            time.sleep(0.3)

            # 2. Levantar la pantalla de bloqueo
            pyautogui.press("enter")
            time.sleep(0.5)

            # 3. Escribir el PIN
            if pin_or_pwd:
                pyautogui.write(pin_or_pwd, interval=0.03)
                time.sleep(0.2)
                pyautogui.press("enter")
            return True
        except Exception as e:
            return False

    def plan_goal(self, goal_prompt: str) -> dict:
        lower = goal_prompt.lower()
        steps = []

        # Detección de patrones comunes de automatización
        for name, exe in KNOWN_APPS.items():
            if name in lower:
                steps.append({
                    "stepId": "step_1",
                    "description": f"Abrir la aplicación {name.title()}",
                    "actionType": "LAUNCH",
                    "targetAppOrElement": exe,
                    "isDestructive": False
                })
                break

        if "escribe" in lower or "escribir" in lower:
            text = goal_prompt.split("escribe", 1)[-1].strip()
            steps.append({
                "stepId": f"step_{len(steps)+1}",
                "description": f"Escribir texto: '{text}'",
                "actionType": "TYPE",
                "textArgument": text,
                "isDestructive": False
            })

        if not steps:
            steps.append({
                "stepId": "step_1",
                "description": f"Ejecutar acción: {goal_prompt}",
                "actionType": "CUSTOM",
                "textArgument": goal_prompt,
                "isDestructive": False
            })

        return {
            "planId": os.urandom(4).hex(),
            "goal": goal_prompt,
            "steps": steps,
            "requiresApproval": any(s.get("isDestructive", False) for s in steps),
            "summary": f"Plan estructurado con {len(steps)} pasos listos para ejecución."
        }

    def execute_plan(self, plan_data: dict) -> bool:
        steps = plan_data.get("steps", [])
        for step in steps:
            action = step.get("actionType")
            if action == "LAUNCH":
                target = step.get("targetAppOrElement", "")
                self.launch_app(target)
            elif action == "TYPE":
                text = step.get("textArgument", "")
                if text:
                    pyautogui.sleep(0.5)
                    pyautogui.write(text, interval=0.01)
        return True

uia_manager = UiaManager()
