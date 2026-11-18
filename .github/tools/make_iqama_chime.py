#!/usr/bin/env python3
"""4.4 the iqama chime (android/launcher/src/main/res/raw/iqama_chime.ogg): made here, from nothing but numbers, so it is our own and given
to the public domain (CC0 1.0), like the rest of this file. A soft two-note bell (G5 then E5, a «ding-dong»), each note with a few partials
of a bell and a slow fall, 2.4 s in all, mono 22.05 kHz, Ogg Vorbis about 40 kb/s (under 30 KB). Needs numpy and ffmpeg (libvorbis).

    python3 .github/tools/make_iqama_chime.py
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "android/launcher/src/main/res/raw/iqama_chime.ogg"
SR = 22050


def bell(freq, seconds):
    t = np.arange(int(seconds * SR)) / SR
    partials = ((1.0, 1.0, 1.6), (2.0, 0.35, 2.4), (2.76, 0.18, 3.2), (5.4, 0.06, 5.0))   # (ratio, loudness, how fast it falls)
    s = sum(a * np.exp(-d * t) * np.sin(2 * np.pi * freq * r * t) for r, a, d in partials)
    return s * np.minimum(1, t / 0.008) * np.minimum(1, (seconds - t) / 0.25)   # no click at either end


def main():
    sig = np.zeros(int(2.4 * SR))
    a = bell(784.0, 1.5)                      # G5
    b = bell(659.3, 1.85)                     # E5, 0.55 s later
    sig[:a.size] += a
    start = int(0.55 * SR)
    sig[start:start + b.size] += b[: sig.size - start]
    sig = sig / np.max(np.abs(sig)) * 0.6
    pcm = (sig * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "c.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-b:a", "40k", "-map_metadata", "-1", str(OUT)], check=True)
    secs = sig.size / SR
    print(f"wrote {OUT.relative_to(ROOT)}: {secs:.2f} s, {OUT.stat().st_size} bytes")
    return 0 if secs < 3 and OUT.stat().st_size < 30_000 else 1


if __name__ == "__main__":
    sys.exit(main())
