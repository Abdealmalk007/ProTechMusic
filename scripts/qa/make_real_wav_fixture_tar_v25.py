from __future__ import annotations
import argparse
import gzip
import io
from pathlib import Path
import struct
import tarfile
import hashlib

def create_wav() -> bytes:
    sample_rate = 8_000
    data = b"\x80" * 2048
    header = (
        b"RIFF" + struct.pack("<I", 36 + len(data)) +
        b"WAVEfmt " + struct.pack("<IHHIIHH", 16, 1, 1, sample_rate, sample_rate, 1, 8) +
        b"data" + struct.pack("<I", len(data))
    )
    assert len(header) == 44
    return header + data

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=50_000)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    if args.count not in (10_000, 50_000):
        parser.error("Only 10k or 50k QA data allowed")
    wav = create_wav()
    args.out.parent.mkdir(parents=True, exist_ok=True)
    prefix = f"ProTechQA_V22_{args.count}_"
    folder = f"ProTechMusicQA_F14_V22_{args.count}"
    with tarfile.open(args.out, mode="w:gz", compresslevel=1, format=tarfile.USTAR_FORMAT) as tar:
        for index in range(args.count):
            name = f"{folder}/{prefix}{index:05d}.wav"
            item = tarfile.TarInfo(name=name)
            item.size = len(wav)
            item.mode = 0o644
            item.mtime = 0
            tar.addfile(item, io.BytesIO(wav))
            if (index + 1) % 10000 == 0:
                print(f"CREATED {index+1}/{args.count}", flush=True)
    h = hashlib.sha256(args.out.read_bytes()).hexdigest()
    print(f"ARCHIVE={args.out} FILES={args.count} COMPRESSED_BYTES={args.out.stat().st_size} WAV_BYTES={len(wav)} SHA256={h}")

if __name__ == "__main__":
    main()
