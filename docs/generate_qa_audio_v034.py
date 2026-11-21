from pathlib import Path
import math,struct,wave
base=Path(r"E:\ProTech\ProTechMusic\qa\fixtures")
base.mkdir(parents=True,exist_ok=True)
for name,seconds,freq in [
    ("qa_v034_auto_01.wav",2,440),
    ("qa_v034_auto_02.wav",10,523),
    ("qa_v034_delete_only.wav",1,659),
]:
    with wave.open(str(base/name),"wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(8000)
        for i in range(seconds*8000):
            sample=int(3200*math.sin(2*math.pi*freq*i/8000))
            out.writeframesraw(struct.pack("<h",sample))
    print(name,(base/name).stat().st_size)
