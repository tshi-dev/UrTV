#!/usr/bin/env python3
"""4.4 (a user report on 4.3: «عند تشغيل القرآن من تبويب القراء يحدث تقطيع باستمرار بعد أقل من دقيقة، علمًا أن الإذاعات تعمل»): a surah plays through
ExoPlayer with a file's buffer (up to four minutes read ahead; a dropped read resumed from its byte, an HTTP Range request; an MP3 with no seek table
still sought), and a surah that fails goes on from its place after a growing wait, saying «يعيد الاتصال…», instead of the old loop of prepares
(each prepare forgave the failures). Only six failures in ten minutes are said. SurahRecover runs for real.
    python3 tests/surah_recover_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
EXO = ROOT / "android/launcher/src/exo/java/sa/urtv/home/tilawah/ExoPlayback.java"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, pb, exo = read(T / "MusicService.java"), read(T / "Playback.java"), read(EXO)
err = svc[svc.index("private void onPlaybackError"):svc.index("private void playSurahAgain")]

check("a surah goes to ExoPlayer (the adhan stays on MediaPlayer), MediaPlayer when the build has none",
      "player = Playback.create(this, url, source == Prefs.SOURCE_RADIO, source == Prefs.SOURCE_SURAH);" in svc
      and "if (radio || surah) {" in pb and "getConstructor(Context.class, boolean.class).newInstance(c, radio)" in pb and "return new Platform();" in pb)
check("the old one-argument constructor stays (a live station)", "public ExoPlayback(Context c) { this(c, true); }" in exo and "public ExoPlayback(Context c, boolean live) {" in exo)
check("a file's buffer: 60 s kept, up to 240 s read ahead, playing after 2.5 s, 5 s after a gap; time over size",
      "FILE_MIN_BUFFER_MS = 120_000, FILE_MAX_BUFFER_MS = 300_000, FILE_START_MS = 5_000, FILE_RESTART_MS = 10_000" in exo   # 4.4 beta 4: bigger (tests/surah_buffer_test.py)
      and "setBufferDurationsMs(FILE_MIN_BUFFER_MS, FILE_MAX_BUFFER_MS, FILE_START_MS, FILE_RESTART_MS)" in exo and ".setPrioritizeTimeOverSizeThresholds(true)" in exo)
check("a dropped read is retried 12 times (Media3 resumes it from its byte: a Range request)", "FILE_RETRIES = 12" in exo and "new DefaultLoadErrorHandlingPolicy(live ? RETRIES : FILE_RETRIES)" in exo)
check("an MP3 with no seek table can still be sought («متابعة» goes on from its place)", "setConstantBitrateSeekingEnabled(true).setConstantBitrateSeekingAlwaysEnabled(true)" in exo)
check("a surah's end is its completion (the next surah), a station's end a failure as before",
      "state == Player.STATE_ENDED && !live" in exo and "onCompletion.onCompletion(ExoPlayback.this);" in exo and "onError.onError(ExoPlayback.this, MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.IO)" in exo)
check("the live-edge handling and the live configuration are for a station only",
      "if (live && (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW || stuck)" in exo and "if (live) item.setLiveConfiguration(" in exo)
check("a failed surah: on from its place after SurahRecover's wait, «يعيد الاتصال…», only for the same surah while wanted",
      "long wait = surahRecover.after(SystemClock.elapsedRealtime());" in err and "error = RECONNECTING;" in err
      and "if (!wish.retry(gen, playing) || source != Prefs.SOURCE_SURAH || !failed.equals(currentUrl)) return;" in err and "startUrl(failed);   // pendingSeek" in err
      and "if (source == Prefs.SOURCE_SURAH && position > 0) pendingSeek = position;" in err)
check("the surah's way comes before the old two-tries one (no loop of prepares for a surah)", err.index("surahRecover.after(") < err.index("if (failed != null && retries < 2) {"))
check("a pick (and the network coming back: playSurah) starts with nothing held against it", "surahRecover.reset();" in svc.split("private void playSurah(int index, int n, int startAt)")[1].split("startUrl(")[0])
check("the network coming back still takes a failed surah on from its place (4.4 radio work)", "playSurah(reciter, surah, Math.max(position, pendingSeek));" in svc)

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class SrRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  SurahRecover r = new SurahRecover();
  long t = 1_000_000;
  boolean grow = r.after(t) == 1_500 && r.after(t + 60_000) == 3_000 && r.after(t + 120_000) == 6_000 && r.after(t + 180_000) == 12_000 && r.after(t + 240_000) == 20_000 && r.after(t + 300_000) == 30_000;
  out("the wait grows: 1.5, 3, 6, 12, 20, 30 s", grow);
  out("a seventh failure in ten minutes is said", r.after(t + 360_000) == -1 && r.count(t + 360_000) == 6);
  out("failures older than ten minutes are forgiven", r.after(t + SurahRecover.WINDOW_MS + 1) == 30_000 && r.count(t + SurahRecover.WINDOW_MS + 1) == 6);
  SurahRecover q = new SurahRecover();
  q.after(0); q.after(1); q.after(2);
  q.reset();
  out("a pick starts again from nothing", q.count(3) == 0 && q.after(3) == 1_500);
  SurahRecover m = new SurahRecover();
  long w = 0; int said = 0;
  for (int i = 0; i < 30; i++) { long x = m.after(i * 50_000L); if (x < 0) said++; }   // a drop every 50 s for 25 minutes (the report): never a loop for ever
  out("a drop every 50 s is said within ten minutes, not prepared again for ever", said > 0);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "SrRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "SurahRecover.java"), str(Path(tmp, "SrRun.java"))], capture_output=True, text=True)
        check("SurahRecover compiles on its own (no Android)", r.returncode == 0 and "import android" not in read(T / "SurahRecover.java"), r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.SrRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 5, len(out.splitlines()))
sys.exit(1 if failed else 0)
