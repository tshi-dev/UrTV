#!/usr/bin/env python3
"""The four adhan options of 4.2, all off by default: «الأذان على شاشة التوقف فقط», «تنبيه عند الأذان», «تكبير فقط» and «شاشة التوقف وقت الأذان».
The decisions run for real (tests/AdhanOptionsRun.java, needs javac); how they are wired in the Android code and the screensaver app is read from
the source.   Run: python3 tests/adhan_options_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
T = HOME / "tilawah"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

text = lambda p: p.read_text(encoding="utf-8")
prefs, receiver, music, tmain = text(T / "Prefs.java"), text(T / "AdhanReceiver.java"), text(T / "MusicReceiver.java"), text(T / "MainActivity.java")
strings = text(ROOT / "android/launcher/src/main/res/values/strings.xml")
manifest = text(ROOT / "android/launcher/src/main/AndroidManifest.xml")

# ---- «الأذان على شاشة التوقف فقط»
check("saver-only is off until it is turned on", 'getBoolean("adhan_saver_only", false)' in prefs)
check("saver-only: the alarm asks AdhanFront and AdhanPlan before it plays", "AdhanPlan.mayPlay(prefs.adhanSaverOnly(), front)" in receiver and "AdhanFront.now(context)" in receiver)
check("saver-only: a skipped adhan is still the alarm's own (it is marked played, never made up) and logs why",
      receiver.index("prefs.setLastDue(due)") < receiver.index("AdhanPlan.mayPlay") and "adhan skipped" in receiver)
front = text(T / "AdhanFront.java")
check("our home tells AdhanFront when it is in front and when it leaves", "AdhanFront.home(true)" in text(HOME / "MainActivity.java") and "AdhanFront.home(false)" in text(HOME / "MainActivity.java"))
check("the screensaver app is asked through its provider, the answer's column checked (an older app answers any path with its settings)",
      'config/dreaming' in front and '"dreaming".equals(cols[0])' in front and "cols.length != 1" in front)
check("an unreadable answer is not known, not «another app»", "return -1;" in front)
prov = text(APP / "ConfigProvider.java")
check("the screensaver's provider answers .../dreaming before anything else", '"dreaming".equals(uri.getLastPathSegment())' in prov and prov.index('"dreaming"') < prov.index("new Prefs(getContext())"))
check("the dream counts itself in and out, the preview too", "Showing.enter()" in text(APP / "ScreenDream.java") and "Showing.leave()" in text(APP / "ScreenDream.java")
      and "Showing.enter()" in text(APP / "MainActivity.java") and "Showing.leave()" in text(APP / "MainActivity.java"))
check("the adhan tab has the row, with the one plain label", "KIND_SAVER_ONLY" in tmain and "<string name=\"adhan_saver_only\">الأذان على شاشة التوقف فقط</string>" in strings)
check("the agent command is declared and answered", '"sa.urtv.home.ADHAN_SAVERONLY"' in manifest and 'case "sa.urtv.home.ADHAN_SAVERONLY"' in music and "adhan_saver_only=" in music)
music_sh = text(ROOT / "optimize/music.sh")
check("music.sh has adhan saver-only and shows it in the state", "saver-only)" in music_sh and "adhan_saver_only=" in music_sh)

# ---- «تنبيه عند الأذان»
notice = text(T / "AdhanNotice.java")
sched = text(T / "AdhanScheduler.java")
check("notice is off until it is turned on", 'getBoolean("adhan_notice", false)' in prefs)
check("the alarm is set for the notice alone (adhan off), and cancelled only when neither is wanted",
      "boolean alarmWanted() { return adhanOn() || adhanNotice(); }" in prefs and "alarmWanted()" in sched and sched.count("adhanOn()") == 1)
check("the adhan itself still needs its own switch: the notice never plays it", "boolean play = prefs.adhanOn() && AdhanPlan.mayPlay(" in receiver)
check("the notice is shown whether the adhan plays or not, when the option is on", "if (notice) AdhanNotice.show(context, prayer);" in receiver and "boolean notice = prefs.adhanNotice();" in receiver)
check("the notice is a text toast (no permission, no focus), never throws, logs", "Toast.makeText(" in notice and "catch (RuntimeException" in notice and "UrTVAdhan" in notice)
check("no overlay permission was added for it", "SYSTEM_ALERT_WINDOW" not in manifest and "TYPE_APPLICATION_OVERLAY" not in notice)
check("the last alarm is written down for the status", "setLastAlarmNote(AdhanPlan.note(" in receiver and "adhan_alarm_last=" in music)
check("the tab has the row with one plain label", "KIND_NOTICE" in tmain and '<string name="adhan_notice">تنبيه عند الأذان</string>' in strings)
check("the switch re-sets the alarm (screen and agent)", tmain.split("row.kind == KIND_NOTICE")[1].split("KIND_EXACT")[0].count("AdhanScheduler.schedule(this)") == 1
      and music.split('"sa.urtv.home.ADHAN_NOTICE"')[1].split("return;")[0].count("AdhanScheduler.schedule(context)") == 1)
check("the agent command is declared and in music.sh", '"sa.urtv.home.ADHAN_NOTICE"' in manifest and "notice)" in music_sh and "adhan_notice=" in music)

# ---- «تكبير فقط»
svc = text(T / "MusicService.java")
check("takbir is off until it is turned on", 'getBoolean("adhan_takbir", false)' in prefs)
check("takbir and short switch each other off, in Prefs (so the screen, the agent and the phone all do)",
      'if (on) e.putBoolean("adhan_takbir", false);' in prefs and 'if (on) e.putBoolean("adhan_short", false);' in prefs)
check("the service cuts by the length chosen, for a real adhan only, through the same tick as the short one",
      "AdhanVoices.length(prefs.adhanTakbir(), prefs.adhanShort()) : AdhanVoices.FULL" in svc and "AdhanVoices.gain(elapsed, adhanCutMs, adhanFadeMs)" in svc
      and "adhanCutMs - adhanFadeMs" in svc and "adhanShortNow" not in svc)
check("every end of an adhan clears the cut", svc.count("adhanCutMs = 0;") >= 3)
check("the tab has the row with one plain label", "KIND_TAKBIR" in tmain and '<string name="adhan_takbir">تكبير فقط</string>' in strings)
check("the agent command is declared and in music.sh", '"sa.urtv.home.ADHAN_TAKBIR"' in manifest and "takbir)" in music_sh and "adhan_takbir=" in music)

if not shutil.which("javac"):
    print("SKIP adhan options: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp] + [str(T / n) for n in ("AdhanPlan.java", "AdhanNoticeText.java", "AdhanVoices.java")] + [str(ROOT / "tests/AdhanOptionsRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.tilawah.AdhanOptionsRun"], check=True, capture_output=True).stdout.decode("utf-8")
rows = [l.split("\t") for l in out.split("\n") if l]
for name, res in rows:
    check(name, res == "ok")
check("every check ran", len(rows) >= 24, len(rows))
sys.exit(1 if failed else 0)
