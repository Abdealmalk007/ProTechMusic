from pathlib import Path
import wave, math, struct

ROOT = Path(r"E:\ProTech\ProTechMusic\qa\fixtures")
ROOT.mkdir(parents=True, exist_ok=True)
for i, name in enumerate(("music.wav", "download.wav", "whatsapp.wav", "record.wav", "telegram.wav", "bluetooth.wav")):
    path = ROOT / name
    frequency = 360 + i * 55
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(16000)
        frames = bytearray()
        for j in range(16000):
            sample = int(8000 * math.sin(2 * math.pi * frequency * j / 16000))
            frames.extend(struct.pack("<h", sample))
        wav.writeframes(frames)
    print(path)
