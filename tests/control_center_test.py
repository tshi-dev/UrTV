#!/usr/bin/env python3
"""«مركز التحكم» (the control center): the text and the rules on a plain JVM (tests/ControlTextRun.java: the speed test's answer, the network and the sound output tiles with the real product names, and the
tiles' layout arithmetic on 1080p, 720p and 540p screens at several densities), the speed test against a server of its own on this machine
(tests/SpeedTestRun.java: one host constant, a bounded read, cancel at once), and what can only be read from the source: the pill and the panel are
behind the family lock's «@center», every tile that opens something keeps its own lock, no network is touched before the person presses the speed
test, the panel closes with the home (and stops the test), and the manifest asks only for the normal permission.
Run: python3 tests/control_center_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def src(n): return (HOME / n).read_text(encoding="utf-8")
main, center, text, speed, policy = src("MainActivity.java"), src("ControlCenter.java"), src("ControlText.java"), src("SpeedTest.java"), src("ParentalPolicy.java")
keys = src("RemoteKeys.java")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

# ---- the wiring
check("the pill opens the panel only through the family lock, with the @center path", "guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(v))" in main)
check("@center is a lock target: a pill, a path, a valid name, and it is on the lock screen's list",
      'CENTER = "@center"' in policy and "SLEEP, CENTER}" in policy and "PILL_CENTER" in policy and "case ParentalPolicy.CENTER" in src("ParentalActivity.java"))
check("the sleep timer, the Wi-Fi, the screensaver and the settings keep their own lock paths",
      all(p in main + center for p in ("ParentalPolicy.PILL_SLEEP", "ParentalPolicy.PILL_WIFI", "ParentalPolicy.PILL_SAVER", "ParentalPolicy.PILL_SAVER_SETTINGS", "ParentalPolicy.PILL_TV_SETTINGS")))
check("the panel starts nothing by itself: every launch goes through the host's launchGuarded/guarded paths (no startActivity in it)",
      "startActivity(" not in center and "host.launchGuarded(" in center)
check("the sound output opens an intent that resolves, falling back to the TV's settings",
      "resolveActivity(sound, 0) != null ? sound : new Intent(Settings.ACTION_SETTINGS)" in center)
check("the sleep timer's tile and «أطفئ الآن» end the same way (SleepTimer.turnOff: the service, else sound off and the screensaver)",
      "SleepTimer.turnOff(act)" in center and "static String turnOff(Context c)" in src("SleepTimer.java") and "String done = turnOff(c);" in src("SleepTimer.java")
      and "SleepService.sleepNow()" in src("SleepTimer.java").split("static String turnOff")[1])
check("«أطفئ الآن» with the service off shows the notice first, then does the fallback after a moment (not if the panel closed meanwhile); a long OK opens the service's screen behind the sleep lock",
      "SleepService.enabled()" in center and "ParentalPolicy.SLEEP_ACCESSIBILITY" in center and "title.setText(ControlText.SLEEP_OFF_NOTICE)" in center
      and center.index("ControlText.SLEEP_OFF_NOTICE") < center.index("SleepTimer.turnOff(act);", center.index("ui.postDelayed")) and "ui.postDelayed(" in center
      and "if (!showing()) return;" in center.split("ui.postDelayed(")[1].split("ControlText.NOTICE_MS")[0] and "ui.removeCallbacksAndMessages(null)" in center
      and "مو شغّالة" in text and "اضغط مطوّلًا" in text)
check("«إعدادات تلفزيونك» opens our settings (the screensaver app's SettingsActivity) through «قفل الإعدادات» (@urtv), the tile only when that app is there",
      "host.openOurSettings()" in center and "ParentalPolicy.OUR_SETTINGS, null" in main and 'SAVER_PKG + ".SettingsActivity"' in main
      and "case OUR_SETTINGS:" in policy and "t.add(OWN_SETTINGS)" in policy.split("case OUR_SETTINGS:")[1][:200]
      and center.index("if (host.saverInstalled())") < center.index('"إعدادات تلفزيونك"') < center.index('"tv", "الإعدادات"'))
check("the tiles' size comes from ControlLayout (width and height), the popup gets its size, and what still does not fit scrolls",
      "ControlLayout.plan(dm.widthPixels, dm.heightPixels" in center and "new PopupWindow(panel, plan.panelW, Math.min(plan.panelH, ControlLayout.maxHeight(dm.heightPixels)), true)" in center
      and "new ScrollView(act)" in center and "dp(104)" not in center)
check("the panel opens on the tile used last while the home is in front (the first tile of the reading order otherwise), and it is forgotten when the home comes back",
      "lastKey = key" in center and "t.key.equals(lastKey)" in center and "Tile start = tiles.get(0)" in center and "void forgetLast()" in center
      and "control.forgetLast()" in main.split("protected void onResume()")[1].split("\n    }\n")[0]
      and "control.forgetLast()" in main.split("protected void onNewIntent")[1].split("\n    }\n")[0])
check("«حرّر الذاكرة» is gone: no tile, no kill call, no permission (Android 14 gives a normal app nothing useful to do there)",
      "killBackgroundProcesses" not in center and "freeMemory" not in center and "ic_memory" not in center and "killable" not in text
      and "KILL_BACKGROUND_PROCESSES" not in manifest and not (ROOT / "android/launcher/src/main/res/drawable/ic_memory.xml").exists())
check("the sound tile reads the real devices with their product names", "d[i].getProductName()" in center and "ControlText.outputs(types, products)" in center)
check("the remote's Settings key opens our settings with a fallback to the TV's own", "OPEN_OUR_SETTINGS" in keys and "case RemoteKeys.OPEN_OUR_SETTINGS:" in main and "openOurSettings(true)" in main
      and "orTvSettings" in main and "ParentalPolicy.REMOTE_SETTINGS" in main.split("private void openOurSettings")[1].split("\n    }\n")[0])
check("kids mode: no pill and no panel (the row's own check, and the panel closes when it goes)", "private boolean centerWanted() { return !Parental.state(this).kids; }" in main
      and "control.dismiss();" in main.split("private void applyCenterPill")[1].split("wireFocus();")[0])
check("the pill follows the row's walk: it is in `pills` and in the row's children together, at the same place",
      "pills.add(at, centerPill);" in main and "pillRow.addView(centerPill, at + 1," in main)
check("the home closing or coming back closes the panel (its test stops with it)", main.count("control.dismiss()") >= 3 and "speed.cancel()" in center.split("setOnDismissListener")[1])
check("the panel is for the remote: no touch handling beyond the outside dismiss", "setOnTouchListener" not in center and "setOutsideTouchable(true)" in center and "new PopupWindow(panel, plan.panelW," in center)
check("RTL: the panel's own direction is set, and the tiles walk right to left with explicit focus",
      "LAYOUT_DIRECTION_RTL" in center and "setNextFocusRightId(col == 0" in center and "setNextFocusLeftId(" in center)
check("focus goes back to the pill when it closes, and a tile has it when it opens", "anchor.requestFocus()" in center and "focusFirst.requestFocus()" in center)

# ---- «انعكاس الشاشة»: the TV's own mirroring app, only when one is installed
mirror_block = text[text.index("static final String[] MIRROR_APPS = {"):text.index("};", text.index("static final String[] MIRROR_APPS = {"))]
mirror_apps = re.findall(r'"([A-Za-z0-9_.]+)"', mirror_block)
lists = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "optimize/lists").rglob("*.txt"))
check("the mirroring apps: TCL's com.tcl.miracast first (keep.txt keeps it on, names-ar.txt names it), a short list",
      mirror_apps[:1] == ["com.tcl.miracast"] and 2 <= len(mirror_apps) <= 6 and len(set(mirror_apps)) == len(mirror_apps)
      and "com.tcl.miracast" in (ROOT / "optimize/lists/keep.txt").read_text(encoding="utf-8")
      and "com.tcl.miracast" in (ROOT / "optimize/lists/names-ar.txt").read_text(encoding="utf-8"), mirror_apps)
check("…every one of them is a package our lists name (none made up)", all(re.search(r"(?m)^" + re.escape(p) + r"\b", lists) for p in mirror_apps),
      [p for p in mirror_apps if not re.search(r"(?m)^" + re.escape(p) + r"\b", lists)])
check("…and every one is in the manifest's <queries>, so Android 11+ shows it to the home",
      all(f'<package android:name="{p}" />' in manifest.split("<queries>")[1].split("</queries>")[0] for p in mirror_apps))
check("the tile is there only when the TV can open one of them (an app missing, disabled or with no screen gives no tile)",
      "final Intent mirror = mirrorIntent();" in center and "if (mirror != null)" in center
      and "ControlText.mirrorApp(p -> launchIntent(pm, p) != null)" in center
      and "pm.getLeanbackLaunchIntentForPackage(pkg)" in center and "pm.getLaunchIntentForPackage(pkg)" in center)
check("its name «انعكاس الشاشة», its one line «من الجوال: شارك الشاشة واختر التلفزيون», its own icon",
      'MIRROR = "انعكاس الشاشة"' in text and 'MIRROR_HINT = "من الجوال: شارك الشاشة واختر التلفزيون"' in text
      and 'tile("mirror", ControlText.MIRROR, ControlText.MIRROR_HINT, R.drawable.ic_cast,' in center
      and (ROOT / "android/launcher/src/main/res/drawable/ic_cast.xml").exists())
check("OK opens it through the host, locked as that app's own tile is (ParentalPolicy.TILE: the app's package)",
      "host.launchGuarded(ParentalPolicy.TILE, mirror, ControlText.MIRROR)" in center and "case TILE:" in policy)
check("the tile comes after «مخرج الصوت» and before the screensaver's, so the panel's first tiles stay where they were",
      center.index('tile("sound"') < center.index('tile("mirror"') < center.index('tile("saver"'))
check("nine tiles may go five to a row; eight or fewer are laid out as before", "static final int FIVE_AFTER = 8;" in src("ControlLayout.java")
      and "for (int cols : columns(n))" in src("ControlLayout.java"))

# ---- no traffic before OK
check("the only network code is the speed test's, and it is started by the tile's OK on a worker thread",
      "new SpeedTest()" in center and "HttpURLConnection" not in center and "new Thread(" in center.split("private void speedTest")[1]
      and "test.downloadMbps()" in center.split("new Thread(")[1])
check("nothing in the panel's opening reads the network: no SpeedTest, no thread before a press",
      "new SpeedTest()" not in center.split("void open(View anchor)")[1].split("// ----")[0] and "new Thread(" not in center.split("void open(View anchor)")[1].split("// ----")[0])
check("the speed test's host is named once, in SpeedTest, and the panel does not name a URL", speed.count("speed.cloudflare.com") == 1 and "cloudflare" not in center + main + text and "://" not in center.split("*/", 1)[0].split("//")[-1] + center.split("*/", 1)[1])
check("the speed test is bounded: a few MB, a measuring time, connect and read timeouts, no cache",
      "BYTES = 6_000_000" in speed and "MEASURE_NANOS" in speed and "setConnectTimeout" in speed and "setReadTimeout" in speed and "setUseCaches(false)" in speed)
check("nothing is uploaded: no output stream, no POST", "getOutputStream" not in speed and "setDoOutput" not in speed and "POST" not in speed)
check("the talkies credit headers are in the files that adapt it", all("rajupp/talkies" in s.split("*/")[0] for s in (speed, center)))
check("the manifest asks for no QUERY_ALL_PACKAGES and adds no accessibility capability",
      "QUERY_ALL_PACKAGES" not in manifest and manifest.count("android.accessibilityservice.AccessibilityService") == 2)   # the sleep service and the phone remote's typing service; the Control Center adds none
check("every number goes through Digits.of (no String.valueOf of a number into a label), every format has Locale.ROOT",
      "String.valueOf" not in center and all("Locale.ROOT" in l for l in (text + speed).split("\n") if "String.format" in l))
check("no new receiver, service or alarm for it; the one delay is the notice before «أطفئ الآن» acts", not re.search(r"registerReceiver|startService|AlarmManager|JobScheduler", center) and center.count("postDelayed") == 1)
check("the phone remote and the accessibility service are untouched by it", "PhoneApi" not in center and "sleep_service" not in center)

# ---- the logic
if not shutil.which("javac"):
    print("SKIP control center logic: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/control_center_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "ControlText.java"), str(HOME / "ControlLayout.java"), str(HOME / "PillText.java"), str(HOME / "DigitsText.java"),
                    str(HOME / "SpeedTest.java"), str(ROOT / "tests/ControlTextRun.java"), str(ROOT / "tests/SpeedTestRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.ControlTextRun", str(ROOT / "tests/control_center_cases.tsv")],
                         check=True, capture_output=True).stdout
    got = out.decode("utf-8").split("\n")
    for c, g in zip(cases, got):
        want = c[5]
        # the digits of the answers in the table are written hindi for reading; the class writes western ones
        g_hindi = g.translate(str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩"))
        check(f"control center {c[0]}: {c[6] if len(c) > 6 else ''}", g == want or (c[0] == "mbps" and g_hindi.replace(".", "٫") == want), f"{g!r}, wanted {want!r}")
    for c, g in zip(cases, got):   # the layout, whatever the numbers: nothing wider or taller than the screen allows, every tile at least readable
        if c[0] != "layout": continue
        W, H, d, n = int(c[1]), int(c[2]), float(c[3]), int(c[4])
        cols, rows, tw, th, pw, ph = (int(x) for x in g.split()[:6])
        check(f"layout {W}x{H} @{d}: {n} tiles in {cols}x{rows}, the panel inside the screen",
              cols * rows >= n and pw <= W * 0.82 + 1 and ph <= H * 0.90 + 1 and th >= 54 * d - 1, g)
    check("the layout cases cover nine tiles («انعكاس الشاشة») on 1080p, 720p and 540p", {(c[1], c[2]) for c in cases if c[0] == "layout" and c[4] == "9"} >= {("1920", "1080"), ("1280", "720"), ("960", "540")})
    check("every case answered", len(got) > len(cases), (len(got), len(cases)))
    r = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.SpeedTestRun"], capture_output=True, text=True)
    for l in r.stdout.splitlines():
        check("speed test: " + l.split(" ", 1)[1], l.startswith("ok "))
    check("speed test run ended well", r.returncode == 0, r.stderr[-300:])
sys.exit(1 if failed else 0)
