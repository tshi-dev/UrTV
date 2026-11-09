#!/usr/bin/env python3
"""4.3 A2: a live station does not stay silent. StallWatch (run for real, tests/StallWatchRun.java) restarts a station that ran dry for 12 s or stands
still for 15 s, quietly, three times in two minutes at most; MusicService asks it every second and restarts from the station's own address; a live
HLS station plays through Media3 ExoPlayer (Playback/ExoPlayback: a 15-60 s buffer, 8 retries of a segment, back to the live edge when it fell out of
the window), everything else through the TV's MediaPlayer as before. Needs javac for the run.   python3 tests/stall_watch_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
EXO = ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

svc, pb, exo = (T / "MusicService.java").read_text(encoding="utf-8"), (T / "Playback.java").read_text(encoding="utf-8"), EXO.read_text(encoding="utf-8")
gradle, props = (ROOT / "android/launcher/build.gradle").read_text(encoding="utf-8"), (ROOT / "android/gradle.properties").read_text(encoding="utf-8")
check("the service plays through Playback (MediaPlayer, or ExoPlayer for a live HLS station), never a MediaPlayer of its own",
      "private Playback player;" in svc and "Playback.create(this, url, source == Prefs.SOURCE_RADIO)" in svc and "new MediaPlayer()" not in svc)
check("Playback.create: ExoPlayer only for a station's .m3u8, loaded by name, MediaPlayer when it cannot be",
      "if (radio && StallWatch.isHls(url))" in pb and 'Class.forName(EXO)' in pb and "return new Platform();" in pb and 'EXO = "sa.urtv.home.tilawah.ExoPlayback"' in pb)
check("ExoPlayback: a 15-60 s buffer starting after 2.5 s, 8 retries of a failed segment, behind-the-window back to the live edge, its own focus off",
      "MIN_BUFFER_MS = 15_000, MAX_BUFFER_MS = 60_000, START_MS = 2_500" in exo and "new DefaultLoadErrorHandlingPolicy(RETRIES)" in exo and "RETRIES = 8" in exo
      and "ERROR_CODE_BEHIND_LIVE_WINDOW" in exo and "exo.seekToDefaultPosition();" in exo and ".build(), false);" in exo)
check("ExoPlayback keeps https on https, keeps the CPU and Wi-Fi up, and gives MediaPlayer's buffering callbacks",
      "setAllowCrossProtocolRedirects(false)" in exo and "C.WAKE_MODE_NETWORK" in exo and "MEDIA_INFO_BUFFERING_START" in exo and "MEDIA_INFO_BUFFERING_END" in exo)
check("Gradle: Media3 exoplayer and hls 1.4.1, the exo sources added, AndroidX on",
      "implementation 'androidx.media3:media3-exoplayer:1.4.1'" in gradle and "implementation 'androidx.media3:media3-exoplayer-hls:1.4.1'" in gradle
      and "main.java.srcDirs += ['src/exo/java']" in gradle and "android.useAndroidX=true" in props)
check("the 1 s tick asks the watchdog; a restart is the station's own address again, quietly; the fourth is said as a failure",
      "if (watchStall()) return;" in svc and "startRadio(radioAddress, 0);\n            return true;" in svc and "onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, -110);" in svc)
check("a pick of a station clears the count; a new player keeps it", "stall.reset();   // the person's pick" in svc and "stall.newPlayer();" in svc)
check("music.sh status says which player and how many quiet restarts", "|player=" in (T / "MusicReceiver.java").read_text(encoding="utf-8") and "|stall_restarts=" in (T / "MusicReceiver.java").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP the watchdog's run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(T / "StallWatch.java"), str(ROOT / "tests/StallWatchRun.java")], capture_output=True, text=True)
        check("StallWatch compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.StallWatchRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every watchdog case ran", len(out.splitlines()) == 12, len(out.splitlines()))
sys.exit(1 if failed else 0)
