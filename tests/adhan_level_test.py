#!/usr/bin/env python3
"""4.4 «مستوى صوت الأذان» (a user report: «صوت الأذان ثابت على ٢٥ ومرتفع ولا أستطيع تغييره»): the adhan's level is 0-100% of the TV's highest
volume (was 10-100 in steps of 10) or «مثل صوت التلفزيون» (the TV's volume left as it is), for all prayers and for each («مثل العام» by default).
OK on «صوت الأذان» opens the page; left/right step by 5. «كل الصلوات بصوت عدا الفجر» is one OK on «أذان كل صلاة». AdhanLevel runs for real.
    python3 tests/adhan_level_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
T = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")
svc, prefs, main, rec, act, ps = (read(T / f) for f in ("MusicService.java", "Prefs.java", "MainActivity.java", "MusicReceiver.java", "AdhanVolumeActivity.java", "PrayerSoundActivity.java"))
up = svc[svc.index("private void adhanVolumeUp()"):svc.index("/** The one way back for the TV's volume")]

check("the adhan takes the prayer's own level, else the general one (a dua keeps its own)", "volumePercent = duaUrl != null ? duaVolume : prefs.volumeFor(prayer);" in svc
      and "volumePercent = prefs.volumeFor(prayer);   // 4.4" in svc)
check("«مثل صوت التلفزيون»: the volume is not set (the TV's own saved, a muted TV unmuted, all put back after, as before)",
      "int want = AdhanLevel.index(volumePercent, max);" in up and "if (want >= 0) {" in up and "savedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);" in up
      and "adhanLevel = audio.getStreamVolume(AudioManager.STREAM_MUSIC);" in up)
check("kept in the tilawah prefs: adhan_volume 0-100 (an older app reads under 10 as 30), adhan_volume_tv, pv_<prayer>",
      'int v = sp.getInt("adhan_volume", 30); return v < 0 || v > 100 ? 30 : v;' in prefs and '"adhan_volume_tv"' in prefs and 'sp.getString("pv_" + prayer, "")' in prefs
      and 'putBoolean("adhan_volume_tv", false)' in prefs)
check("the row: left/right step it by 5 (AdhanLevel.step), OK opens «مستوى صوت الأذان»", "AdhanLevel.step(was, dir)" in main and "changeVolume(keyCode == sa.urtv.home.Dir.forwardKey() ? 1 : -1);" in main
      and "startActivity(new Intent(this, AdhanVolumeActivity.class));" in main and 'volumeText(prefs.adhanVolumeLevel())' in main)
check("the page: all prayers, then each, «مثل العام», «مثل صوت التلفزيون», 0 to 100; every change heard for two seconds",
      'S.t("لكل الصلوات")' in act and 'S.t("مثل العام")' in act and 'S.t("مثل صوت التلفزيون")' in act and "MusicService.adhan(this, voice, Adhan.name(voice), 2, prayer);" in act
      and "for (int v = AdhanLevel.MIN; v <= AdhanLevel.MAX; v += 10)" in act)
check("the page is in the manifest", ".tilawah.AdhanVolumeActivity" in read(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
check("«كل الصلوات بصوت عدا الفجر»: one OK (Fajr silent, the others heard, their muezzin kept), again: all five heard",
      'S.t("كل الصلوات بصوت عدا الفجر")' in ps and 'if (p == PrayerTimes.FAJR) prefs.setPrayerSound(p, on ? "silent" : "");' in ps and "static boolean allButFajr(Prefs prefs)" in ps)
check("commands: music.sh adhan volume <0-100|tv>, adhan prayer-volume; the status says them",
      '"sa.urtv.home.PRAYER_VOLUME"' in rec and 'intent.getBooleanExtra("tv", false)' in rec and '"|adhan_volume_tv="' in rec and '"|prayer_volume="' in rec
      and "prayer-volume)" in read(ROOT / "optimize/music.sh") and "--ez tv true" in read(ROOT / "optimize/music.sh"))
check("docs/SETTINGS.md has both commands", "`adhan volume <0-100|tv>`" in read(ROOT / "docs/SETTINGS.md") and "`adhan prayer-volume <fajr|dhuhr|asr|maghrib|isha> <general|tv|0-100>`" in read(ROOT / "docs/SETTINGS.md"))
check("settings export/import: 0-100", "ADHAN_VOLUME = (0, 100)" in read(ROOT / "optimize/settings_move.py"))

if shutil.which("javac"):
    run = r"""package sa.urtv.home.tilawah;
public class AlRun { static void out(String n, boolean ok) { System.out.println(n + "\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  out("general: TV or the percent", AdhanLevel.general(30, false) == 30 && AdhanLevel.general(30, true) == AdhanLevel.TV && AdhanLevel.general(150, false) == 100);
  out("a prayer: its own, else the general", AdhanLevel.forPrayer("", 30) == 30 && AdhanLevel.forPrayer("tv", 30) == AdhanLevel.TV && AdhanLevel.forPrayer("70", 30) == 70
      && AdhanLevel.forPrayer("101", 30) == 30 && AdhanLevel.forPrayer("x", 30) == 30 && AdhanLevel.forPrayer(null, AdhanLevel.TV) == AdhanLevel.TV);
  out("valid words", AdhanLevel.valid("") && AdhanLevel.valid("tv") && AdhanLevel.valid("0") && AdhanLevel.valid("100") && !AdhanLevel.valid("-1") && !AdhanLevel.valid("1000") && !AdhanLevel.valid(null));
  out("steps of 5, no wrapping: TV below 0, 100 the top", AdhanLevel.step(30, 1) == 35 && AdhanLevel.step(30, -1) == 25 && AdhanLevel.step(0, -1) == AdhanLevel.TV
      && AdhanLevel.step(AdhanLevel.TV, -1) == AdhanLevel.TV && AdhanLevel.step(AdhanLevel.TV, 1) == 0 && AdhanLevel.step(100, 1) == 100);
  out("an old level off the grid lands on it", AdhanLevel.step(33, 1) == 35 && AdhanLevel.step(33, -1) == 30);
  out("the TV's step: TV not set, 0 silent, any other at least 1", AdhanLevel.index(AdhanLevel.TV, 15) == -1 && AdhanLevel.index(0, 15) == 0 && AdhanLevel.index(5, 15) == 1
      && AdhanLevel.index(30, 100) == 30 && AdhanLevel.index(100, 15) == 15 && AdhanLevel.index(30, 15) == 5);
  out("the word kept", AdhanLevel.choice(AdhanLevel.TV).equals("tv") && AdhanLevel.choice(45).equals("45"));
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "AlRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(T / "AdhanLevel.java"), str(Path(tmp, "AlRun.java"))], capture_output=True, text=True)
        check("AdhanLevel compiles on its own (no Android)", r.returncode == 0 and "import android" not in read(T / "AdhanLevel.java"), r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.tilawah.AlRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 7, len(out.splitlines()))
sys.exit(1 if failed else 0)
