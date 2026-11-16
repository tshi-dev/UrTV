#!/usr/bin/env python3
"""4.4 «تنبيه بداية المباراة»: the kick-off alert of the followed clubs. The rules (KickoffPlan: which games, when, where, with or without the
whistle) run for real on a JVM (tests/KickoffRun.java); the whistle file, the defaults, the plumbing (the page's hand-off, the alarm, the
screensaver's settings, the provider, saver.sh, dock.sh overlay) are pinned.

    python3 tests/kickoff_test.py
"""
import shutil, subprocess, sys, tempfile, wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "phone/Json.java"), str(HOME / "sports/KickoffPlan.java"), str(ROOT / "tests/KickoffRun.java")],
                           capture_output=True, text=True)
        check("KickoffPlan compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            o = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.sports.KickoffRun"], capture_output=True, text=True)
            bad = [l for l in o.stdout.splitlines() if l.startswith("FAIL")]
            check(f"the rules: the list, the time, late or early, called off, where it shows, the whistle ({o.stdout.count('PASS ')} cases)",
                  o.returncode == 0 and not bad and o.stdout.count("PASS ") >= 10, bad[:3] or o.stderr[-300:])
else:
    print("SKIP the JVM run (no javac here)")

ogg = ROOT / "android/launcher/src/main/res/raw/kickoff_whistle.ogg"
check("the whistle: our own (make_whistle.py), an Ogg under 30 KB", ogg.exists() and ogg.read_bytes()[:4] == b"OggS" and ogg.stat().st_size < 30_000, ogg.stat().st_size if ogg.exists() else "missing")
if ogg.exists() and shutil.which("ffprobe"):
    d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(ogg)], capture_output=True, text=True).stdout.strip()
    check("the whistle is shorter than 1.5 s", d and 0.3 < float(d) < 1.5, d)
mk = read(ROOT / ".github/tools/make_whistle.py")
check("its source says what it is (synthesised here, CC0) and makes the same file", "CC0" in mk and "kickoff_whistle.ogg" in mk)

plan, rcv, sched, over, man, main, card, whistle = (read(HOME / "sports/KickoffPlan.java"), read(HOME / "KickoffReceiver.java"), read(HOME / "sports/KickoffScheduler.java"),
    read(HOME / "KickoffOverlay.java"), read(ROOT / "android/launcher/src/main/AndroidManifest.xml"), read(HOME / "MainActivity.java"), read(HOME / "HomeCard.java"), read(HOME / "tilawah/Whistle.java"))
check("the launcher: the receiver, put back after a reboot and an update, only the system can send it (DUMP)",
      'android:name=".KickoffReceiver"' in man and "sa.urtv.home.KICKOFF_RESCHEDULE" in man and man.split('android:name=".KickoffReceiver"', 1)[1].split("</receiver>", 1)[0].count("android.permission.DUMP") == 1)
check("the launcher asks for «الظهور فوق التطبيقات» in its manifest (granted only by the person or dock.sh overlay on)", "android.permission.SYSTEM_ALERT_WINDOW" in man)
check("defaults when the screensaver app says nothing: on, the whistle, not over another app", "boolean[] s = {true, true, false};" in rcv)
check("before showing: the game is looked up once; no answer (no network) = nothing", "lookupevent.php?id=" in rcv and "KickoffPlan.confirmed(check != null" in rcv)
check("never with the adhan, never with the screen off, each game once", "NowPlaying.adhanLive()" in rcv and "screenOn" in rcv and "KickoffStore.markDone(app, g.id)" in rcv)
check("kids mode and quiet hours reach the rules", "Parental.state(app).kids" in rcv and "Whistle.quietNow(app)" in rcv)
check("over another app: only with «فوق تطبيق آخر» and the permission; the overlay is never focused or touched",
      "Settings.canDrawOverlays(app)" in rcv and "TYPE_APPLICATION_OVERLAY" in over and "FLAG_NOT_FOCUSABLE" in over and "FLAG_NOT_TOUCHABLE" in over)
check("the notice over another app: cream text on dark glass, top right", "CREAM = 0xFFFFF8EC" in over and "0xE60C0E12" in over and "Gravity.TOP | Dir.start(true)" in over, "colour")
check("the whistle is a notification sound that ducks the TV (never a music stream)", "USAGE_NOTIFICATION_EVENT" in whistle and "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK" in whistle)
check("the home's card asks the page for its list (no JavaScript interface, only while in front) and draws the notice",
      "addJavascriptInterface" not in card and "spKickList()" in card and "idle.removeCallbacks(kickPull);   // 4.4" in card and "spKick(" in card and "KickoffReceiver.SHOW" in main)

prefs, prov, srcv, act, hub, sh, dock, rst = (read(APP / "Prefs.java"), read(APP / "ConfigProvider.java"), read(APP / "SaverReceiver.java"), read(APP / "SettingsActivity.java"),
    read(APP / "SettingsHub.java"), read(ROOT / "optimize/saver.sh"), read(ROOT / "optimize/dock.sh"), read(ROOT / "optimize/reset.sh"))
check("the screensaver's defaults: on, the whistle, goal off, the final whistle off, over another app off",
      'getBoolean("kickoff", true)' in prefs and 'getBoolean("kickoffWhistle", true)' in prefs and 'getBoolean("kickoffGoal", false)' in prefs
      and 'getBoolean("kickoffEnd", false)' in prefs and 'getBoolean("kickoffOver", false)' in prefs)
check("the page is told only what is not the default (&ko=0, &kogoal=1, &koend=1)", '(kickoff() ? "" : "&ko=0")' in prefs and '"&kogoal=1"' in prefs and '"&koend=1"' in prefs)
check("the provider gives the column «kickoff»", '"kickoff"});' in prov and "p.kickoffColumn()});" in prov)
rows = ["تنبيه بداية المباراة", "صوت التنبيه", "عند الهدف", "صافرة النهاية", "فوق تطبيق آخر"]
check("the five rows in the settings, in both layouts (the hub's tables)", all(f'row("{r}"' in act for r in rows) and all(hub.count(f'"{r}"') >= 4 for r in rows),
      [r for r in rows if hub.count(f'"{r}"') < 4])
check("«فوق تطبيق آخر» opens the TV's own permission screen for the home app", "MANAGE_OVERLAY_PERMISSION" in act and '"package:sa.urtv.home"' in act)
check("agents: saver.sh kickoff/kickoffgoal/kickoffend/kickoffover, and show says them", all(f"set1 {k} " in sh for k in ("kickoff", "kickoffgoal", "kickoffend", "kickoffover"))
      and all(f'"\\n{k}: "' in srcv for k in ("kickoff", "kickoffgoal", "kickoffend", "kickoffover")))
check("dock.sh overlay on|off|status sets the appop; reset.sh puts it back", "appops set sa.urtv.home SYSTEM_ALERT_WINDOW allow" in dock and "appops set sa.urtv.home SYSTEM_ALERT_WINDOW default" in rst)
npp, bridge, tryj = read(HOME / "tilawah/NowPlayingProvider.java"), read(APP / "NowPlayingBridge.java"), read(APP / "KickoffTry.java")
check("kids mode reaches the screensaver's page: the launcher's read-only .../kids row, &kids=1 on the screensaver's address (only that, not the home's cards)",
      '"kids".equals(uri.getLastPathSegment())' in npp and "KidsNow.on(getContext())" in npp and 'content://sa.urtv.home.nowplaying/kids' in bridge
      and '(kickoff() && NowPlayingBridge.kidsOn(c) ? "&kids=1" : "")' in prefs and "&kids=1" not in prefs.split("String fragment() {", 1)[1].split("\n    }", 1)[0])
check("«جرّب التنبيه»: a row that shows the notice now in the settings' own window (no permission, never focused) and asks the launcher for the whistle (same key only, never over the adhan)",
      'row("جرّب التنبيه"' in act and "KickoffTry.show(SettingsActivity.this, prefs.kickoffWhistle())" in act and "getDecorView()" in tryj and "TYPE_APPLICATION_OVERLAY" not in tryj
      and "setFocusable(false)" in tryj and '"kickoff_whistle".equals(method)' in npp and 'checkCallingPermission("sa.urtv.home.READ_NOW_PLAYING")' in npp.split('"kickoff_whistle".equals(method)', 1)[1][:200]
      and "if (!MusicService.adhanLive()) Whistle.play(getContext());" in npp)
docs = read(ROOT / "docs/SETTINGS.md")
check("SETTINGS.md has the rows and the commands", all(w in docs for w in ("saver.sh kickoff off", "kickoffgoal on", "kickoffover on", "dock.sh overlay on")))
js = read(ROOT / "src/sports.js")
check("the page hands the launcher the time as digits in a string (13 digits are too long for the home's Json)", "t: String(fbKick(ev))" in js)
check("the home's page keeps the list for the launcher; the screensaver page draws its own notice, never with no network",
      "function spKickList(){ return spHanded; }" in js and "if(CONFIG.widget || _Q.get('ko') === '0'" in js and "navigator.onLine === false" in js)
sys.exit(1 if failed else 0)
