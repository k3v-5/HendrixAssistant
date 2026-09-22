import unittest
import time
import struct
from core.audio_stream_server import AudioStreamServer

class TestAudioStreamServer(unittest.TestCase):

    def setUp(self):
        self.audio_server = AudioStreamServer()

    def tearDown(self):
        self.audio_server.stop_stream()

    def test_start_and_stop_stream(self):
        self.assertFalse(self.audio_server.is_streaming())
        started = self.audio_server.start_stream(sample_rate=24000, channels=1)
        self.assertTrue(started)
        self.assertTrue(self.audio_server.is_streaming())

        stopped = self.audio_server.stop_stream()
        self.assertTrue(stopped)
        self.assertFalse(self.audio_server.is_streaming())

    def test_audio_packet_structure_and_streaming(self):
        received_packets = []
        self.audio_server.register_callback(lambda pkt: received_packets.append(pkt))

        self.audio_server.chunk_ms = 20 # 20ms chunks para test rápido
        self.audio_server.start_stream(sample_rate=24000, channels=1)

        # Esperar que se generen al menos 3 paquetes
        time.sleep(0.12)
        self.audio_server.stop_stream()

        self.assertGreaterEqual(len(received_packets), 2)

        first_pkt = received_packets[0]
        # Header: 1 byte tipo (0x02) + 4 bytes número de secuencia Big Endian = 5 bytes
        self.assertGreater(len(first_pkt), 5)
        pkt_type, seq_num = struct.unpack(">BI", first_pkt[:5])
        self.assertEqual(pkt_type, AudioStreamServer.PACKET_TYPE_AUDIO)
        self.assertEqual(seq_num, 0)

        # Segundo paquete debe tener seq_num 1
        _, seq_num_2 = struct.unpack(">BI", received_packets[1][:5])
        self.assertEqual(seq_num_2, 1)

    def test_telemetry(self):
        self.audio_server.start_stream(sample_rate=24000, channels=1)
        time.sleep(0.06)
        telemetry = self.audio_server.get_telemetry()

        self.assertTrue(telemetry["isStreaming"])
        self.assertEqual(telemetry["sampleRate"], 24000)
        self.assertEqual(telemetry["channels"], 1)
        self.assertIn("currentRms", telemetry)
        self.assertIn("currentPeak", telemetry)
        self.assertGreaterEqual(telemetry["currentRms"], 0.0)

if __name__ == "__main__":
    unittest.main()
