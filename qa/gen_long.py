from pathlib import Path
from math import sin, pi
import wave
import struct
path = Path(r"E:\ProTech\ProTechMusic\qa\fixtures\long30.wav")
path.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(path), "wb") as wav:
    wav.setnchannels(1)
    wav.setsampwidth(2)
    wav.setframerate(16000)
    sample_rate = 16000
    for seconds in range(30):
        block = bytearray()
        for i in range(sample_rate):
            val = int(2000*sin(2*pi*440*(seconds*sample_rate+i)/sample_rate))
            block.extend(struct.pack("<h", val))
        wav.writeframes(block)
print(path, path.stat().st_size)
