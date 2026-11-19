#!/usr/bin/env python3
"""4.4 «تنبيه بداية المباراة»: the kick-off alert of the followed clubs. The rules (KickoffPlan: which games, when, where, with or without the
whistle) run for real on a JVM (tests/KickoffRun.java); the whistle file, the defaults, the plumbing (the page's hand-off, the alarm, the
screensaver's settings, the provider, saver.sh, dock.sh overlay) are pinned.

    python3 tests/kickoff_test.py
"""
import re, shutil, subprocess, sys, tempfile, wave
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
xman, xnote, xprov, xwords, xact, ovn = (read(ROOT / "android/extras/src/main/AndroidManifest.xml"), read(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasNotice.java"),
    read(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasCallProvider.java"), read(HOME / "ExtrasWords.java"), read(HOME / "ExtrasActivity.java"), read(HOME / "OverlayNotice.java"))
check("«الظهور فوق التطبيقات» is the ADD-ON's (granted only by the person or dock.sh overlay on): the home has no SYSTEM_ALERT_WINDOW (Play Protect), the add-on has",
      "SYSTEM_ALERT_WINDOW" not in re.sub(r"<!--.*?-->", "", man, flags=re.S) and '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />' in xman)
check("defaults when the screensaver app says nothing: on, the whistle, not over another app", "boolean[] s = {true, true, false};" in rcv)
check("before showing: the game is looked up once; no answer (no network) = nothing", "lookupevent.php?id=" in rcv and "KickoffPlan.confirmed(check != null" in rcv)
check("never with the adhan, never with the screen off, each game once", "NowPlaying.adhanLive()" in rcv and "screenOn" in rcv and "KickoffStore.markDone(app, g.id)" in rcv)
check("kids mode and quiet hours reach the rules", "Parental.state(app).kids" in rcv and "Whistle.quietNow(app)" in rcv)
check("over another app: only with «فوق تطبيق آخر» and the permission; the overlay is never focused or touched",
      "canDrawOverlays" not in rcv and "set[2] && !kids && OverlayNotice.ready()" in rcv and "OverlayNotice.kick(g)" in rcv and "TYPE_APPLICATION_OVERLAY" not in over
      and "TYPE_APPLICATION_OVERLAY" in xnote and "FLAG_NOT_FOCUSABLE" in xnote and "FLAG_NOT_TOUCHABLE" in xnote and "Settings.canDrawOverlays(c)" in xnote)
check("the add-on side: the provider answers «notice» (draws it) and says CAN_DRAW in its status; both ends are signature-guarded (EXTRAS); the words are shared",
      "case ExtrasWords.NOTICE:" in xprov and "ExtrasNotice.show(getContext(), extras)" in xprov and "ExtrasWords.CAN_DRAW" in xprov
      and 'android:permission="sa.urtv.permission.EXTRAS"' in xman and '"notice"' in xwords and '"can_draw"' in xwords
      and "Extras.notice(b)" in ovn and "Extras.canDraw()" in ovn)
check("the home's notice in its own window stays (no permission needed), and no TYPE_APPLICATION_OVERLAY is left anywhere in the home",
      "static boolean showIn(" in over and not [p.name for p in HOME.rglob("*.java") if "TYPE_APPLICATION_OVERLAY" in read(p)])
check("the notice over another app: cream text on dark glass, top right (the add-on's window), 8 s", "CREAM = 0xFFFFF8EC" in xnote and "0xE60C0E12" in xnote and "Gravity.TOP | Gravity.RIGHT" in xnote and "SHOW_MS = 8_000L" in xnote, "colour")
check("the whistle is a notification sound that ducks the TV (never a music stream)", "USAGE_NOTIFICATION_EVENT" in whistle and "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK" in whistle)
check("the home's card asks the page for its list (no JavaScript interface, only while in front)",
      "addJavascriptInterface" not in card and "spKickList()" in card and "idle.removeCallbacks(kickPull);   // 4.4" in card)
check("the home draws the kick-off notice itself at the screen's top right in its own window (the card's page is only the card's size); a toast if it cannot",
      "KickoffReceiver.SHOW" in main and "KickoffOverlay.showIn(MainActivity.this, g.get(0))" in main and 'putExtra("game", g.json())' in rcv
      and "static boolean showIn(android.app.Activity a, KickoffPlan.Game g)" in over and "getDecorView()" in over.split("showIn(", 1)[1][:400] and "spKick(" not in card)

prefs, prov, srcv, act, hub, sh, dock, rst = (read(APP / "Prefs.java"), read(APP / "ConfigProvider.java"), read(APP / "SaverReceiver.java"), read(APP / "SettingsActivity.java"),
    read(APP / "SettingsHub.java"), read(ROOT / "optimize/saver.sh"), read(ROOT / "optimize/dock.sh"), read(ROOT / "optimize/reset.sh"))
check("the screensaver's defaults: on, the whistle, goal off, the final whistle off, over another app off",
      'getBoolean("kickoff", true)' in prefs and 'getBoolean("kickoffWhistle", true)' in prefs and 'getBoolean("kickoffGoal", false)' in prefs
      and 'getBoolean("kickoffEnd", false)' in prefs and 'getBoolean("kickoffOver", false)' in prefs)
check("the page is told only what is not the default (&ko=0, &kogoal=1, &koend=1)", '(kickoff() ? "" : "&ko=0")' in prefs and '"&kogoal=1"' in prefs and '"&koend=1"' in prefs)
check("the provider gives the column «kickoff»", '"kickoff", "greeting_name"});' in prov and "p.kickoffColumn(), p.greetingName()});" in prov)
rows = ["تنبيه بداية المباراة", "صوت التنبيه", "عند الهدف", "صافرة النهاية", "فوق تطبيق آخر"]
check("the five rows in the settings, in both layouts (the hub's tables)", all(f'row("{r}"' in act for r in rows) and all(hub.count(f'"{r}"') >= 4 for r in rows),
      [r for r in rows if hub.count(f'"{r}"') < 4])
check("«فوق تطبيق آخر» opens the TV's own permission screen for the ADD-ON (the home has none)", "MANAGE_OVERLAY_PERMISSION" in act and '"package:sa.urtv.extras"' in act and '"package:sa.urtv.home"' not in act)
check("without the add-on the person is told on screen (Arabic, with its English entry) and the add-on's offer opens; the row says it needs the add-on",
      "if (Boolean.FALSE.equals(extrasHere)) { offerExtras(); return; }" in act and '"sa.urtv.home.ExtrasActivity"' in act and '"sa.urtv.home.EXTRAS_FOR", "overlay"' in act
      and "«فوق تطبيق آخر» يحتاج «إضافات تلفزيونك»: نفتح لك تثبيتها" in act and "يعمل، لكنه يحتاج «إضافات تلفزيونك»" in act
      and all("\t" in l for l in read(ROOT / "i18n/en/saver.tsv").splitlines() if "فوق تطبيق آخر» يحتاج" in l)
      and "«فوق تطبيق آخر» يحتاج «إضافات تلفزيونك» وهي غير مثبّتة" in xact and '"overlay".equals(' in xact and "allowOverlay()" in xact
      and '"package:" + ExtrasWords.PACKAGE' in xact and 'android:name="sa.urtv.home.OPEN_EXTRAS"' in man)
check("agents: saver.sh kickoff/kickoffgoal/kickoffend/kickoffover, and show says them", all(f"set1 {k} " in sh for k in ("kickoff", "kickoffgoal", "kickoffend", "kickoffover"))
      and all(f'"\\n{k}: "' in srcv for k in ("kickoff", "kickoffgoal", "kickoffend", "kickoffover")))
check("dock.sh overlay on|off|status sets the ADD-ON's appop (nothing changed when it is not installed); reset.sh puts it back",
      "appops set sa.urtv.extras SYSTEM_ALERT_WINDOW allow" in dock and "appops set sa.urtv.home SYSTEM_ALERT_WINDOW allow" not in dock
      and "pm list packages sa.urtv.extras" in dock.split("overlay:2)")[1].split("overlay:*)")[0] and "appops set sa.urtv.extras SYSTEM_ALERT_WINDOW default" in rst)
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
# 4.4 beta 2, the UI review (the owner's no-decoration rule): no colour square or accent bar for the clubs on any of the three notices; the
# page's shows a club's real crest only when it has one (CRESTS), the apps keep none (the names on the flat fill); the layout and timing as they were
check("the clubs on the notice: no colour square anywhere (home, add-on, the page); the page's a real crest or nothing, by the club's id",
      "swatch(" not in over and "swatch(" not in xnote and "swatch(" not in tryj and "spsw" not in js.split("function spKick(", 1)[1].split("\n}", 1)[0]
      and "const sw = id => id ? spCrest(id, 'kcr') : '';" in js and "hid: String(e2.idHomeTeam), aid: String(e2.idAwayTeam)" in js
      and "hid: String(ev.idHomeTeam), aid: String(ev.idAwayTeam)" in js and "const spCrestOf = id =>" in js and 'onerror="this.remove()"' in js)
check("…the notices' look and timing unchanged: 8 s, the top right, the slide of 450 ms, the dark glass with the gold edge, the names at 38 and the head at 24",
      over.count("SHOW_MS = 8_000L") == 1 and "Gravity.TOP | Dir.start(true)" in over and over.count("setDuration(450)") == 2 and "bg.setColor(0xE60C0E12)" in over
      and "bg.setStroke(Math.max(1, (int) (2 * u)), 0x99E9C46A)" in over and 'text(c, "  " + g.h + "   ×   " + g.a + "  ", CREAM, 38 * u, true)' in over
      and 'text(c, S.t("بدأت المباراة"), GOLD, 24 * u, false)' in over and "(int) (dm.widthPixels * 0.36f)" in over
      and "mid.addView(text(c, line, CREAM, 38 * u, true));" in xnote and "SHOW_MS = 8_000L" in xnote
      and "SHOW_MS = 8000" in tryj and "setDuration(450)" in tryj and "}, 8000);" in js.split("function spKick(", 1)[1].split("\n}", 1)[0])
_kr = (ROOT / "android/launcher/src/main/java/sa/urtv/home/KickoffReceiver.java").read_text(encoding="utf-8") if "ROOT" in globals() else (Path(__file__).resolve().parent.parent / "android/launcher/src/main/java/sa/urtv/home/KickoffReceiver.java").read_text(encoding="utf-8")
_ea = (Path(__file__).resolve().parent.parent / "android/launcher/src/main/java/sa/urtv/home/ExtrasActivity.java").read_text(encoding="utf-8")
_run = _kr.split("private static void run(Context app) {")[1].split("private static void show(")[0]
check("4.4 review: the kick-off thread knows the TV's language before it builds the notice (S.init in run, not only in show)", "S.init(app);" in _run.split("boolean[] set")[0])
_act = _ea.split("private void act() {")[1][:1200]
check("4.4 review: granting the overlay is behind the same PIN lock as the add-on's other settings", _act.index("Parental.needsPin") < _act.index("allowOverlay();"))
sys.exit(1 if failed else 0)
