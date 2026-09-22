import time
import math
import struct
import threading
import logging
from typing import Optional, Callable, Dict, Any

logger = logging.getLogger("hendrix_desktop.audio_stream")

class AudioStreamServer:
    """
    Servidor de transmisión de audio inalámbrico para Hendrix Desktop.
    Captura la salida del sistema (WASAPI Loopback en Windows) y emite tramas binarias
    PCM de latencia ultrabaja (< 25ms) hacia los clientes móviles conectados.
    Incluye un generador sintético de fallback para pruebas y entornos virtuales sin hardware de audio.
    """

    PACKET_TYPE_AUDIO = 0x02

    def __init__(self):
        self._is_streaming = False
        self._stream_thread: Optional[threading.Thread] = None
        self._frame_callbacks: list[Callable[[bytes], None]] = []

        self.sample_rate = 24000
        self.channels = 1
        self.chunk_ms = 40
        self.sequence_number = 0

        self._current_rms = 0.0
        self._current_peak = 0.0

        self._driver_name = "unknown"
        self._loopback_mic = None

    def register_callback(self, callback: Callable[[bytes], None]):
        """Registra un callback que recibe las tramas binarias de audio para enviarlas por WebSocket."""
        if callback not in self._frame_callbacks:
            self._frame_callbacks.append(callback)

    def unregister_callback(self, callback: Callable[[bytes], None]):
        if callback in self._frame_callbacks:
            self._frame_callbacks.remove(callback)

    def is_streaming(self) -> bool:
        return self._is_streaming

    def get_telemetry(self) -> Dict[str, Any]:
        """Retorna el estado y los niveles de audio en tiempo real para el vúmetro."""
        return {
            "isStreaming": self._is_streaming,
            "sampleRate": self.sample_rate,
            "channels": self.channels,
            "driver": self._driver_name,
            "currentRms": round(self._current_rms, 3),
            "currentPeak": round(self._current_peak, 3)
        }

    def start_stream(self, sample_rate: int = 24000, channels: int = 1) -> bool:
        """Inicia la captura y transmisión de audio."""
        if self._is_streaming:
            return True

        self.sample_rate = sample_rate
        self.channels = channels
        self.sequence_number = 0
        self._is_streaming = True

        # Inicializar driver de captura WASAPI o fallback
        self._init_driver()

        self._stream_thread = threading.Thread(
            target=self._capture_loop,
            daemon=True,
            name="AudioCaptureThread"
        )
        self._stream_thread.start()
        logger.info(f"🎙️ Transmisión de Audio Inalámbrico iniciada ({sample_rate}Hz, {channels}ch, driver: {self._driver_name})")
        return True

    def stop_stream(self) -> bool:
        """Detiene la captura de audio."""
        if not self._is_streaming:
            return True

        self._is_streaming = False
        if self._stream_thread and self._stream_thread.is_alive():
            self._stream_thread.join(timeout=1.0)

        self._current_rms = 0.0
        self._current_peak = 0.0
        logger.info("⏹️ Transmisión de Audio Inalámbrico detenida")
        return True

    def _init_driver(self):
        """Intenta inicializar soundcard para loopback WASAPI en Windows."""
        try:
            import soundcard as sc
            speaker = sc.default_speaker()
            self._loopback_mic = sc.get_microphone(id=str(speaker.name), include_loopback=True)
            self._driver_name = "soundcard_wasapi"
        except Exception as e:
            logger.info(f"Driver WASAPI loopback no disponible ({e}), utilizando generador sintético de baja latencia.")
            self._loopback_mic = None
            self._driver_name = "fallback_synth"

    def _capture_loop(self):
        samples_per_chunk = int(self.sample_rate * (self.chunk_ms / 1000.0))
        chunk_interval_sec = self.chunk_ms / 1000.0

        if self._driver_name == "soundcard_wasapi" and self._loopback_mic:
            try:
                with self._loopback_mic.recorder(samplerate=self.sample_rate, channels=self.channels) as recorder:
                    while self._is_streaming:
                        start_t = time.perf_counter()
                        data = recorder.record(numframes=samples_per_chunk)
                        # data es un array numpy de float32 entre -1.0 y 1.0
                        pcm_bytes = self._float_to_pcm16(data)
                        self._process_and_dispatch_frame(pcm_bytes)

                        elapsed = time.perf_counter() - start_t
                        sleep_time = chunk_interval_sec - elapsed
                        if sleep_time > 0:
                            time.sleep(sleep_time)
                return
            except Exception as e:
                logger.warning(f"Error en captura nativa: {e}, conmutando a fallback sintético.")
                self._driver_name = "fallback_synth"

        # Bucle de fallback sintético (genera silencio / tono de presencia imperceptible)
        t = 0.0
        dt = 1.0 / self.sample_rate
        while self._is_streaming:
            start_t = time.perf_counter()

            # Generar onda senoidal suave (220 Hz) a volumen muy bajo (-40dB) o silencio dinámico
            pcm_data = bytearray(samples_per_chunk * 2 * self.channels)
            sum_sq = 0.0
            peak = 0.0

            for i in range(samples_per_chunk):
                val_float = 0.08 * math.sin(2.0 * math.pi * 220.0 * t)
                val_int = int(max(-32768, min(32767, val_float * 32767)))
                sum_sq += val_float * val_float
                if abs(val_float) > peak:
                    peak = abs(val_float)
                # Empaquetar 16-bit little endian
                struct.pack_into("<h", pcm_data, i * 2, val_int)
                t += dt

            self._current_rms = math.sqrt(sum_sq / samples_per_chunk)
            self._current_peak = peak

            self._dispatch_packet(bytes(pcm_data))

            elapsed = time.perf_counter() - start_t
            sleep_time = chunk_interval_sec - elapsed
            if sleep_time > 0:
                time.sleep(sleep_time)

    def _float_to_pcm16(self, float_array) -> bytes:
        """Convierte datos flotantes [-1.0, 1.0] a PCM 16-bit Little Endian."""
        import numpy as np
        # Aplanar canales si es mono
        if float_array.ndim > 1 and self.channels == 1:
            mono_data = np.mean(float_array, axis=1)
        else:
            mono_data = float_array.flatten()

        # Calcular métricas RMS y pico
        self._current_peak = float(np.max(np.abs(mono_data))) if len(mono_data) > 0 else 0.0
        self._current_rms = float(np.sqrt(np.mean(mono_data ** 2))) if len(mono_data) > 0 else 0.0

        clipped = np.clip(mono_data, -1.0, 1.0)
        int_data = (clipped * 32767).astype(np.int16)
        return int_data.tobytes()

    def _process_and_dispatch_frame(self, pcm_bytes: bytes):
        self._dispatch_packet(pcm_bytes)

    def _dispatch_packet(self, pcm_bytes: bytes):
        """
        Construye el paquete binario:
        [1 byte PACKET_TYPE (0x02)][4 bytes seq_num BigEndian][PCM Payload]
        """
        header = struct.pack(">BI", self.PACKET_TYPE_AUDIO, self.sequence_number)
        packet = header + pcm_bytes
        self.sequence_number = (self.sequence_number + 1) & 0xFFFFFFFF

        for cb in list(self._frame_callbacks):
            try:
                cb(packet)
            except Exception as e:
                logger.debug(f"Error enviando trama de audio al suscriptor: {e}")
