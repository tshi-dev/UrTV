#!/usr/bin/env python3
"""4.4 «الإقامة»: a sound at the iqama (off by default; our own short bell or a sound of «صوتياتي», at its own volume, played the dua's way: never
over the adhan, another app's sound or in the quiet hours) and «تنبيه الإقامة» (a notice where the person is; over another app the kick-off
notice's rule: an overlay with the TV's permission, else a text notice, nothing over the screensaver or another app in kids mode). The iqama is
the adhan plus the home bar's minutes (none on a Friday's Dhuhr); its alarm is set when the adhan's goes off. IqamaPlan runs for real.
    python3 tests/iqama_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = H / "tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
plan, rec, sch, act, prefs, adhan = (read(T / f) for f in ("IqamaPlan.java", "IqamaReceiver.java", "IqamaScheduler.java", "IqamaActivity.java", "Prefs.java", "AdhanReceiver.java"))
man, main, mr = read(ROOT / "android/launcher/src/main/AndroidManifest.xml"), read(T / "MainActivity.java"), read(T / "MusicReceiver.java")

bar = re.search(r"IQAMA = \{([^}]*)\}", read(H / "AdhanBarText.java")).group(1).replace(" ", "").split(",")
mine = re.search(r"MINUTES = \{([^}]*)\}", plan).group(1).replace(" ", "").split(",")
check("the iqama's minutes are the home bar's (Fajr, Dhuhr, Asr, Maghrib, Isha; Sunrise none)", [mine[0]] + mine[2:] == bar and mine[1] == "0", (mine, bar))
check("off by default: no sound, no notice; volume 30", 'sp.getString("iqama_sound", "")' in prefs and 'sp.getBoolean("iqama_notice", false)' in prefs and 'sp.getInt("iqama_volume", 30)' in prefs)
check("the adhan's alarm is wanted for the iqama alone too", "|| IqamaPlan.wanted(iqamaSound(), iqamaNotice())" in prefs)
check("the adhan's alarm sets the iqama's, from its own time, not when it came late; a Friday's Dhuhr in the city's zone",
      "if (late == null && due > 0) iqama(context, prefs, intent.getIntExtra(AdhanScheduler.EXTRA_PRAYER, -1), due);" in adhan
      and "IqamaPlan.at(prayer, due, cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY)" in adhan and "ZoneFix.zone(Place.get(c).tz)" in adhan)
check("its own alarm (request 53), exact when allowed", "REQUEST = 53;" in sch and "setExactAndAllowWhileIdle" in sch and "AdhanScheduler.exactAllowed(c)" in sch)
others = [m for f in H.rglob("*.java") if f.name != "IqamaScheduler.java" for m in re.findall(r"REQUEST = 53;|getBroadcast\(c, 53\b", read(f))]
check("request 53 is the iqama's alone", not others, others)
check("the receiver: on time only, the screen on, the sound the dua's way, the notice", "IqamaPlan.onTime(due, now)" in rec and "if (!screenOn) return;" in rec
      and 'MusicService.dua(app, path, S.n("الإقامة"), prefs.iqamaVolume());' in rec and "IqamaPlan.sound(sound, true, MusicService.carryAdhanActive(), prefs.quietNow(app), others)" in rec)
check("the notice: the kick-off's rule (kids mode: the home only), an overlay with the permission, else a text notice",
      "KickoffPlan.where(front == AdhanFront.AT_HOME ? 0 : front == AdhanFront.AT_SAVER ? 1 : 2, NoticeOverlay.kids(app), true, Settings.canDrawOverlays(app))" in rec
      and "if (where == KickoffPlan.NONE) return;" in rec and "NoticeOverlay.show(app," in rec and "Toast.makeText(app, line, Toast.LENGTH_LONG)" in rec)
ov = read(H / "NoticeOverlay.java")
check("the overlay takes no focus and no key, 8 s, the kick-off's look; kids mode not known = as kids mode",
      "FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE" in ov and "KickoffOverlay.SHOW_MS" in ov and "0xE60C0E12" in ov and "catch (RuntimeException e) { return true; }" in ov)
check("receiver and page in the manifest (not exported)", '<receiver android:name=".tilawah.IqamaReceiver" android:exported="false" />' in man and ".tilawah.IqamaActivity" in man)
check("the adhan tab: a row «الإقامة» opens its page", "KIND_IQAMA = 20" in main and "startActivity(new Intent(this, IqamaActivity.class));" in main and 'S.t("الإقامة"), IqamaActivity.summary(' in main)
check("the page: the sound (none, our bell, «صوتياتي»), its volume (left/right, heard), the notice", 'S.t("صوت الإقامة")' in act and 'S.t("نغمة قصيرة")' in act
      and "AudioStore.list(IqamaActivity.this)" in act and 'S.t("مستوى صوت الإقامة")' in act and 'S.t("تنبيه الإقامة")' in act)
sh = read(ROOT / "optimize/music.sh")
check("commands: music.sh adhan iqama / iqama-volume / iqama-notice; iqama= and audio_ids= in the status", '"sa.urtv.home.IQAMA"' in mr and '"|iqama=" + iqamaText(prefs)' in mr
      and '"|audio_ids=" + audioIds(context)' in mr and "iqama|iqama-volume|iqama-notice)" in sh and "`adhan iqama <off|chime|aN>`" in read(ROOT / "docs/SETTINGS.md"))
ogg = ROOT / "android/launcher/src/main/res/raw/iqama_chime.ogg"
check("the chime: our own (make_iqama_chime.py, CC0), an Ogg under 30 KB", ogg.exists() and ogg.read_bytes()[:4] == b"OggS" and ogg.stat().st_size < 30_000
      and "CC0" in read(ROOT / ".github/tools/make_iqama_chime.py") and "iqama_chime.ogg" in read(ROOT / "THIRD_PARTY_NOTICES.md"))
if shutil.which("ffprobe"):
    d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(ogg)], capture_output=True, text=True).stdout.strip()
    check("the chime is short (under 3 s)", d and 0.5 < float(d) < 3, d)

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class IqRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  long t = 1_700_000_000_000L;
  out("each prayer's minutes after its adhan", IqamaPlan.at(PrayerTimes.FAJR, t, false) == t + 25 * 60_000L && IqamaPlan.at(PrayerTimes.MAGHRIB, t, false) == t + 10 * 60_000L
      && IqamaPlan.at(PrayerTimes.ISHA, t, false) == t + 20 * 60_000L && IqamaPlan.at(PrayerTimes.ASR, t, true) == t + 20 * 60_000L);
  out("none: a Friday's Dhuhr, Sunrise, no prayer, no time", IqamaPlan.at(PrayerTimes.DHUHR, t, true) == -1 && IqamaPlan.at(PrayerTimes.DHUHR, t, false) == t + 20 * 60_000L
      && IqamaPlan.at(PrayerTimes.SUNRISE, t, false) == -1 && IqamaPlan.at(-1, t, false) == -1 && IqamaPlan.at(9, t, false) == -1 && IqamaPlan.at(PrayerTimes.ASR, 0, false) == -1);
  out("the sounds kept", IqamaPlan.validSound("") && IqamaPlan.validSound("chime") && IqamaPlan.validSound("a12") && !IqamaPlan.validSound("a") && !IqamaPlan.validSound("bell") && !IqamaPlan.validSound(null));
  out("wanted: a sound or the notice", !IqamaPlan.wanted("", false) && IqamaPlan.wanted("chime", false) && IqamaPlan.wanted("", true) && !IqamaPlan.wanted("x", false));
  out("on time: up to 2 min late, a minute early", IqamaPlan.onTime(t, t + 119_000) && !IqamaPlan.onTime(t, t + 121_000) && IqamaPlan.onTime(t, t - 59_000) && !IqamaPlan.onTime(t, t - 61_000) && !IqamaPlan.onTime(0, t));
  out("the sound: chosen, screen on, no adhan, no quiet hours, no other app's sound", IqamaPlan.sound("chime", true, false, false, false)
      && !IqamaPlan.sound("", true, false, false, false) && !IqamaPlan.sound("chime", false, false, false, false) && !IqamaPlan.sound("chime", true, true, false, false)
      && !IqamaPlan.sound("chime", true, false, true, false) && !IqamaPlan.sound("chime", true, false, false, true));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        stub = Path(tmp, "PrayerTimes.java")
        stub.write_text("package sa.urtv.home.tilawah;\nfinal class PrayerTimes { static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5; }\n")
        consts = re.search(r"static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;", read(T / "PrayerTimes.java"))
        check("the prayers' numbers the run uses are PrayerTimes' own", consts is not None)
        Path(tmp, "IqRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "IqamaPlan.java"), str(stub), str(Path(tmp, "IqRun.java"))], capture_output=True, text=True)
        check("IqamaPlan compiles on its own (no Android)", r.returncode == 0 and "import android" not in plan, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.IqRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 6, len(out.splitlines()))
sys.exit(1 if failed else 0)
