#!/usr/bin/env python3
"""4.4 beta 4 (Xiaomi A9): a surah stuttered or sounded slowed while the stations played well.
1. The buffer: a surah file read ahead 60 to 240 s (ExoPlayback FILE_*), so a TV whose Wi-Fi drops for a minute and more ran dry again and again
   in a long surah. Now 120 to 300 s, and it starts (and restarts) with 5 s (10 s) in, as a station does. The long-playback check below plays
   two hours of a surah (al-Baqarah is about that) at 128 kbps over a weak connection, second by second, by the rule ExoPlayer's
   DefaultLoadControl follows (it reads until MAX is ahead, then waits until less than MIN is), with the numbers in ExoPlayback.java: no
   stop at all; the old numbers stop on the same connection (so the check can tell them apart).
2. The sample rate: a TV maker's MP3 decoder that reports the wrong output rate plays a 44.1 kHz file slowed and in bursts. A surah's MP3 now
   goes to the platform's own software decoder first (DecoderOrder through ExoPlayback.SURAH_DECODERS); every other type keeps the TV's order.
Run: python3 tests/surah_buffer_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TIL = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
EXO = ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

exo = EXO.read_text(encoding="utf-8")
num = lambda k: int(re.search(k + r" = ([\d_]+)", exo).group(1).replace("_", ""))
NEW = dict(min=num("FILE_MIN_BUFFER_MS"), max=num("FILE_MAX_BUFFER_MS"), start=num("FILE_START_MS"), restart=num("FILE_RESTART_MS"))
OLD = dict(min=60_000, max=240_000, start=2_500, restart=5_000)
check("the surah's buffer is used for a surah (DefaultLoadControl with the FILE_ numbers, time over size)",
      "setBufferDurationsMs(FILE_MIN_BUFFER_MS, FILE_MAX_BUFFER_MS, FILE_START_MS, FILE_RESTART_MS)" in exo and ".setPrioritizeTimeOverSizeThresholds(true)" in exo)
check("a surah's player is built with SURAH_DECODERS (a station's as before)",
      "(live ? new ExoPlayer.Builder(c) : new ExoPlayer.Builder(c, new androidx.media3.exoplayer.DefaultRenderersFactory(c).setMediaCodecSelector(SURAH_DECODERS)))" in exo
      and "DecoderOrder.order(mime, names)" in exo and "MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)" in exo)
check("five minutes ahead at most: under 5 MB at 128 kbps", NEW["max"] <= 300_000 and NEW["max"] / 1000 * 128_000 / 8 < 5 * 1024 * 1024)

def play(p, link, seconds=2 * 3600):
    """A surah of `seconds` at 1x; link(t) is how many seconds of audio the connection brings in second t. DefaultLoadControl: reads while
    less than max is ahead once it fell under min; plays once start (first) or restart (after running dry) is in. Returns (stops, silent s)."""
    ahead = got = 0.0
    played, playing, loading, stops, silent, t, first = 0.0, False, True, 0, 0, 0, True
    while played < seconds and t < seconds * 3:
        if ahead * 1000 < p["min"]: loading = True
        if ahead * 1000 >= p["max"]: loading = False
        if loading and got < seconds:
            n = min(link(t), seconds - got, p["max"] / 1000 - ahead + link(t))
            ahead += n; got += n
        if not playing and (ahead * 1000 >= (p["start"] if first else p["restart"]) or got >= seconds):
            playing, first = True, False
        if playing:
            step = min(1.0, ahead)
            ahead -= step; played += step
            if step < 1.0 and played < seconds:
                playing = False; stops += 1
        else:
            silent += 1
        t += 1
    return stops, silent

# a weak Wi-Fi (the reports' TV beside a busy router): 2.5 times the bitrate for 1 to 4 minutes, then nothing for 70 to 110 s, at random but
# the same on every run (seven of them); the drops meet every state of the buffer within two hours
def weak(seed, up=2.5):
    x, t, spans = seed, 0, []
    while t < 3 * 7200:
        x = (x * 1103515245 + 12345) & 0x7fffffff; on = 60 + x % 181
        x = (x * 1103515245 + 12345) & 0x7fffffff; off = 70 + x % 41
        spans.append((t, t + on)); t += on + off
    return lambda s: up if any(a <= s < b for a, b in spans) else 0.0
good = lambda t: 4.0
for seed in range(1, 8):
    link = weak(seed)
    s_new, q_new = play(NEW, link)
    s_old, q_old = play(OLD, link)
    check(f"connection {seed}: two hours of a surah with ExoPlayback's numbers never stop (silent {q_new} s, the start); the old numbers stopped {s_old} times",
          s_new == 0 and s_old > 0, ((s_new, q_new), (s_old, q_old)))
st, q = play(NEW, good)
check("a good connection: never stops, and it starts within the start's 5 s", st == 0 and q <= 5, (st, q))
check("the start waits for START_MS (5 s, a station's), the restart for RESTART_MS (10 s)", NEW["start"] == 5_000 and NEW["restart"] == 10_000, NEW)

if not shutil.which("javac"):
    print("SKIP decoder order: no javac here")
    sys.exit(1 if failed else 0)
CASES = [
    ("MP3: the platform's software decoder first, then the maker's in their order", ["audio/mpeg", "OMX.MTK.AUDIO.DECODER.MP3", "c2.android.mp3.decoder", "OMX.vendor.mp3"], "c2.android.mp3.decoder,OMX.MTK.AUDIO.DECODER.MP3,OMX.vendor.mp3"),
    ("MP3 on an older TV: OMX.google first", ["audio/mpeg", "OMX.amlogic.mp3.decoder.awesome", "OMX.google.mp3.decoder"], "OMX.google.mp3.decoder,OMX.amlogic.mp3.decoder.awesome"),
    ("AAC (a station): the TV's order", ["audio/mp4a-latm", "OMX.MTK.AUDIO.DECODER.AAC", "c2.android.aac.decoder"], "OMX.MTK.AUDIO.DECODER.AAC,c2.android.aac.decoder"),
    ("MP3 with the software one already first: as it was", ["audio/mpeg", "c2.android.mp3.decoder", "OMX.MTK.AUDIO.DECODER.MP3"], "c2.android.mp3.decoder,OMX.MTK.AUDIO.DECODER.MP3"),
]
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(TIL / "DecoderOrder.java"), str(ROOT / "tests/DecoderOrderRun.java")], capture_output=True, text=True)
    check("DecoderOrder compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        for name, args, want in CASES:
            got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.DecoderOrderRun", *args], capture_output=True, text=True).stdout.strip()
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
