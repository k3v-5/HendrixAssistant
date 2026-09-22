import logging
import comtypes

logger = logging.getLogger("hendrix_desktop.audio_mixer")

class WindowsAudioMixer:
    """
    Controlador nativo del Mezclador de Audio de Windows (Windows Core Audio APIs)
    Permite gestionar el volumen maestro y faders por aplicación individual
    (Ableton Live, FL Studio, Chrome, Spotify, Discord, etc.).
    """

    def __init__(self):
        pass

    def _ensure_com(self):
        try:
            comtypes.CoInitialize()
        except Exception:
            pass

    def get_mixer_state(self) -> dict:
        self._ensure_com()
        try:
            from pycaw.pycaw import AudioUtilities
            speakers = AudioUtilities.GetSpeakers()
            endpoint = speakers.EndpointVolume
            master_vol = int(round(endpoint.GetMasterVolumeLevelScalar() * 100))
            is_master_muted = bool(endpoint.GetMute())

            sessions = AudioUtilities.GetAllSessions()
            app_sessions = []
            seen_processes = set()

            for s in sessions:
                try:
                    proc = s.Process
                    if proc is None:
                        continue
                    proc_name = proc.name()
                    if not proc_name or proc_name in seen_processes:
                        continue
                    seen_processes.add(proc_name)

                    vol_control = s.SimpleAudioVolume
                    vol_pct = int(round(vol_control.GetMasterVolume() * 100))
                    is_muted = bool(vol_control.GetMute())

                    # Nombre amigable para mostrar
                    display_name = s.DisplayName or proc_name
                    if display_name.lower().endswith(".exe"):
                        display_name = display_name[:-4]

                    app_sessions.append({
                        "id": f"{proc_name}_{proc.pid}",
                        "processName": proc_name,
                        "displayName": display_name,
                        "volumePercent": max(0, min(100, vol_pct)),
                        "isMuted": is_muted
                    })
                except Exception as e:
                    logger.debug(f"Error leyendo sesión de audio: {e}")
                    continue

            return {
                "masterVolumePercent": max(0, min(100, master_vol)),
                "isMasterMuted": is_master_muted,
                "sessions": app_sessions
            }
        except Exception as e:
            logger.error(f"Error al obtener estado del mezclador: {e}")
            return {
                "masterVolumePercent": 50,
                "isMasterMuted": False,
                "sessions": []
            }

    def set_app_volume(self, process_name: str, volume_percent: int) -> bool:
        self._ensure_com()
        try:
            from pycaw.pycaw import AudioUtilities
            target = process_name.lower()
            scalar = max(0.0, min(1.0, volume_percent / 100.0))
            sessions = AudioUtilities.GetAllSessions()
            found = False

            for s in sessions:
                try:
                    proc = s.Process
                    if proc and (target in proc.name().lower() or proc.name().lower() in target):
                        s.SimpleAudioVolume.SetMasterVolume(scalar, None)
                        found = True
                except Exception:
                    continue
            return found
        except Exception as e:
            logger.error(f"Error al fijar volumen de {process_name}: {e}")
            return False

    def set_app_mute(self, process_name: str, is_muted: bool) -> bool:
        self._ensure_com()
        try:
            from pycaw.pycaw import AudioUtilities
            target = process_name.lower()
            sessions = AudioUtilities.GetAllSessions()
            found = False

            for s in sessions:
                try:
                    proc = s.Process
                    if proc and (target in proc.name().lower() or proc.name().lower() in target):
                        s.SimpleAudioVolume.SetMute(1 if is_muted else 0, None)
                        found = True
                except Exception:
                    continue
            return found
        except Exception as e:
            logger.error(f"Error al mutear {process_name}: {e}")
            return False

    def set_master_volume(self, volume_percent: int) -> bool:
        self._ensure_com()
        try:
            from pycaw.pycaw import AudioUtilities
            speakers = AudioUtilities.GetSpeakers()
            scalar = max(0.0, min(1.0, volume_percent / 100.0))
            speakers.EndpointVolume.SetMasterVolumeLevelScalar(scalar, None)
            return True
        except Exception as e:
            logger.error(f"Error al fijar volumen maestro: {e}")
            return False

    def set_master_mute(self, is_muted: bool) -> bool:
        self._ensure_com()
        try:
            from pycaw.pycaw import AudioUtilities
            speakers = AudioUtilities.GetSpeakers()
            speakers.EndpointVolume.SetMute(1 if is_muted else 0, None)
            return True
        except Exception as e:
            logger.error(f"Error al mutear volumen maestro: {e}")
            return False

audio_mixer = WindowsAudioMixer()
