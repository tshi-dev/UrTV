#!/usr/bin/env python3
"""4.4 the kick-off whistle (android/launcher/src/main/res/raw/kickoff_whistle.ogg): made here, from nothing but numbers, so it is our own
and given to the public domain (CC0 1.0), like the rest of this file. A referee's pea whistle: a 2.9 kHz tone with the pea's fast trill
(about 34 Hz, a slight bend of pitch with it), a short breath of noise, a soft start and end; two short blasts then a long one, 1.3 s in all,
mono 22.05 kHz, Ogg Vorbis about 40 kb/s (under 30 KB). Needs numpy and ffmpeg (libvorbis).

    python3 .github/tools/make_whistle.py
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "android/launcher/src/main/res/raw/kickoff_whistle.ogg"
SR = 22050


def blast(seconds, rng):
    t = np.arange(int(seconds * SR)) / SR
    trill = 0.5 + 0.5 * np.sin(2 * np.pi * 34 * t)                     # the pea rolling in the chamber
    f = 2900 + 90 * np.sin(2 * np.pi * 34 * t + 0.6)                   # its bend of pitch
    phase = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(phase) + 0.25 * np.sin(2 * phase) + 0.08 * np.sin(3 * phase)
    breath = rng.normal(0, 1, t.size) * 0.06
    env = np.minimum(1, t / 0.025) * np.minimum(1, (seconds - t) / 0.06)
    return (tone * (0.55 + 0.45 * trill) + breath) * env


def main():
    rng = np.random.default_rng(1)
    gap = np.zeros(int(0.07 * SR))
    sig = np.concatenate([blast(0.16, rng), gap, blast(0.16, rng), gap, blast(0.78, rng)])
    sig = sig / np.max(np.abs(sig)) * 0.7
    pcm = (sig * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "w.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-b:a", "40k", "-map_metadata", "-1", str(OUT)], check=True)
    secs = sig.size / SR
    print(f"wrote {OUT.relative_to(ROOT)}: {secs:.2f} s, {OUT.stat().st_size} bytes")
    return 0 if secs < 1.5 and OUT.stat().st_size < 30_000 else 1


if __name__ == "__main__":
    sys.exit(main())
