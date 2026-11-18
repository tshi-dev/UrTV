#!/usr/bin/env python3
"""4.4 touch «يبدأ بهدوء»: a dua rises from silence to its level in three seconds, gently at first (FadeIn, run for real); on for a new one, a row on its
page turns it off; it is the dua's own (an adhan starts as before).   python3 tests/fade_in_test.py"""
import subprocess, sys, tempfile, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
svc, store, act = ((T / f).read_text(encoding="utf-8") for f in ("MusicService.java", "DuaStore.java", "DuaActivity.java"))
check("only a dua with the setting rises: an adhan starts as before", "boolean rise = source == Prefs.SOURCE_ADHAN && duaNow != null && duaFade;" in svc
      and svc.index("boolean rise") < svc.index("mp.start();", svc.index("boolean rise")))
check("it starts from silence and steps every 100 ms until full", "mp.setVolume(0f, 0f);" in svc and "if (g < 1f) handler.postDelayed(this, FadeIn.STEP_MS);" in svc)
check("on for a new dua (and for one kept before this setting)", "boolean fade = true;" in store and 'o.optBoolean("fade", true)' in store)
check("a row on the dua's page", 'S.t("يبدأ بهدوء")' in act and "edit.fade = !edit.fade;" in act)
check("the alarm and «استمع الآن» pass it", "MusicService.dua(c, source, chosen.name, chosen.volume, chosen.fade);" in (T / "DuaReceiver.java").read_text(encoding="utf-8")
      and "MusicService.dua(this, src, d.name, d.volume, d.fade);" in act)
if shutil.which("javac"):
    run = """package sa.urtv.home.tilawah;
public class FadeRun { public static void main(String[] a) {
  boolean ok = FadeIn.gain(0) == 0f && FadeIn.gain(-5) == 0f && FadeIn.gain(FadeIn.MS) == 1f && FadeIn.gain(99_999) == 1f
    && Math.abs(FadeIn.gain(FadeIn.MS / 2) - 0.25f) < 1e-6 && FadeIn.MS == 3000;
  float last = -1; for (long t = 0; t <= FadeIn.MS; t += FadeIn.STEP_MS) { float g = FadeIn.gain(t); ok &= g >= last; last = g; }
  System.out.println(ok ? "ok" : "bad");
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "FadeRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "FadeIn.java"), str(Path(tmp, "FadeRun.java"))], capture_output=True, text=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.FadeRun"], capture_output=True, text=True).stdout.strip() if r.returncode == 0 else r.stderr
        check("FadeIn: from 0 to 1 in 3 s, a quarter at half way, never going down", out == "ok", out)
sys.exit(1 if failed else 0)
