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
check("«أطفئ الآن» with the service off: the tile says «يحتاج تفعيل، اضغط للتفعيل», OK opens the TV's accessibility settings behind the sleep lock (ParentalPolicy.SLEEP_ACCESSIBILITY) with a line «فعّل «تلفزيونك: إطفاء الشاشة»»; no silent fallback to sound off and the screensaver, no notice and no delay; a long OK does the same",
      "SleepService.enabled()" in center and "ParentalPolicy.SLEEP_ACCESSIBILITY" in center and 'S.t("يحتاج تفعيل، اضغط للتفعيل")' in center and 'S.t("فعّل «%s»", act.getString(R.string.sleep_service_label))' in center
      and "SLEEP_OFF_NOTICE" not in center and "SLEEP_OFF_NOTICE" not in text and "offPending" not in center and "ui.postDelayed(" not in center.split("private void turnOffNow()")[1].split("// ------")[0] and "مو شغّالة" not in text)
check("«إعدادات تلفزيونك» opens our settings (the screensaver app's SettingsActivity) through «قفل الإعدادات» (@urtv), the tile only when that app is there",
      "host.openOurSettings()" in center and "ParentalPolicy.OUR_SETTINGS, null" in main and 'SAVER_PKG + ".SettingsActivity"' in main
      and "case OUR_SETTINGS:" in policy and "t.add(OWN_SETTINGS)" in policy.split("case OUR_SETTINGS:")[1][:200]
      and center.index("if (host.saverInstalled())") < center.index('"إعدادات تلفزيونك"') < center.index('"tv", S.t("الإعدادات")'))
check("the side panel: 560 px of 1920 wide inside the 5 % safe area, a clear full-screen popup it slides across for 220 ms (ease-out), a press outside closes it, what does not fit scrolls",
      "PANEL_W = 560f / 1920f, SAFE = 0.05f" in center and "SLIDE_MS = 220" in center and "new PopupWindow(root, W, H, true)" in center and "root.setOnClickListener(v -> dismiss());" in center
      and "panel.animate().translationX(0).setDuration(SLIDE_MS).setInterpolator(new DecelerateInterpolator())" in center and "Dir.start() | Gravity.TOP" in center
      and "new ScrollView(act)" in center and "ControlLayout" not in center and not (HOME / "ControlLayout.java").exists())
check("four round toggles on top (Bluetooth, mirroring, the sleep timer, «أطفئ الآن»), then rows with a short value each (network, sound, speed, the TV's settings, ours); ellipsized one-line values",
      'TOGGLES = {"bluetooth", "mirror", "sleep", "off"}' in center and 'ROWS = {"net", "sound", "speed", "tv", "ours", "saver"}' in center and "GradientDrawable.OVAL" in center
      and "t.setSingleLine(true);" in center and "TruncateAt.END" in center and "ControlText.networkDetail" not in center)
check("the focus: a solid fill in the look's colours with its ink, coming in over 170 ms (ease-out)",
      "FOCUS_MS = 170" in center and "ValueAnimator.ofArgb(shown, target)" in center and "anim.setDuration(FOCUS_MS)" in center and "int target = f ? FOCUSED : " in center and "ON_SUB" in center)
check("«تخصيص»: a tick per item, the arrows move a row (Left/Right by the reading direction), at most six rows, «إعادة الضبط» (the defaults), «تم»; Back goes back to the panel's main view; kept on this TV",
      "MAX_ROWS = 6" in center and "tickedRows(now) >= MAX_ROWS" in center and "KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT" in center and "private void reset()" in center
      and 'getSharedPreferences("control"' in center and "if (code == KeyEvent.KEYCODE_BACK) { if (e.getAction() == KeyEvent.ACTION_UP) showMain(); return true; }" in center
      and all(f'"{w}"' in center for w in ("تخصيص", "تم", "إعادة الضبط", "اختر ما يظهر ورتّبه بالأسهم")))
check("«شاشة التوقف» is unticked until asked for (its pill is on the home); a toggle or a row that does not exist on this TV is not in the list",
      'DEFAULT_OFF = {"saver"}' in center and "for (String k : TOGGLES) { Item it = find(k); if (it != null) out.add(it); }" in center)
check("the Wi-Fi status is next to the greeting (a small icon on its line, no focus, no press), not in the quick-button bar",
      "greetRow.addView(netIcon, sideLlp(dp(NET_ICON), dp(NET_ICON), dp(8), 0, 0, 0));" in main and "NET_ICON = 22" in main and "right.member(netIcon)" not in main
      and "right.addView(netIcon" not in main and "int ink = online ? palette().soft : NET_OFFLINE;" in main and "netIcon.setColorFilter(ink);" in main and "setFocusable" not in main.split("netIcon = new ImageView(this);")[1].split("left.addView(greetRow")[0])
check("the panel opens on the tile used last while the home is in front (the first tile of the reading order otherwise), and it is forgotten when the home comes back",
      "lastKey = item.key" in center and "item.key.equals(lastKey)" in center and "private void focusStart(" in center and "void forgetLast()" in center
      and "control.forgetLast()" in main.split("protected void onResume()")[1].split("\n    }\n")[0]
      and "control.forgetLast()" in main.split("protected void onNewIntent")[1].split("\n    }\n")[0])
check("«حرّر الذاكرة» is gone: no tile, no kill call, no permission (Android 14 gives a normal app nothing useful to do there)",
      "killBackgroundProcesses" not in center and "freeMemory" not in center and "ic_memory" not in center and "killable" not in text
      and "KILL_BACKGROUND_PROCESSES" not in manifest and not (ROOT / "android/launcher/src/main/res/drawable/ic_memory.xml").exists())
check("the sound tile reads the real devices with their product names", "d[i].getProductName()" in center and "ControlText.outputs(types, products)" in center)
check("the remote's Settings key opens our settings with a fallback to the TV's own", "OPEN_OUR_SETTINGS" in keys and "case RemoteKeys.OPEN_OUR_SETTINGS:" in main and "openOurSettings(true)" in main
      and "orTvSettings" in main and "ParentalPolicy.REMOTE_SETTINGS" in main.split("private void openOurSettings")[1].split("\n    }\n")[0])
check("kids mode: no pill and no panel (the row's own check, and the panel closes when it goes)", "private boolean centerWanted() { return !Parental.state(this).kids && !hiddenQuick.contains(\"center\"); }" in main
      and "control.dismiss();" in main.split("private void applyCenterPill")[1].split("wireFocus();")[0])
check("the pill follows the row's walk: it is in `pills` and in the row's children together, at the same place",
      "pills.add(at, centerPill);" in main and "pillRow.addView(centerPill, at + 1," in main)
check("the home closing or coming back closes the panel (its test stops with it)", main.count("control.dismiss()") >= 3 and "speed.cancel()" in center.split("setOnDismissListener")[1])
check("the panel is for the remote: no touch handling beyond the press outside it", "setOnTouchListener" not in center and "root.setOnClickListener(v -> dismiss());" in center and "panel.setClickable(true);" in center)
check("RTL: the panel sits at the start edge (right in Arabic, left in English), its own direction is set (Dir), and the toggles walk right to left with explicit focus (English mirrored)",
      "root.setLayoutDirection(Dir.layout());" in center and "int before = i == 0 ? c.getId() : toggleCells.get(i - 1).getId()" in center
      and "c.setNextFocusRightId(Dir.rtl() ? before : after);" in center and "c.setNextFocusLeftId(Dir.rtl() ? after : before);" in center and "plp.setMarginStart(margin);" in center)
check("focus goes back to the pill when it closes, and a tile has it when it opens", "anchor.requestFocus()" in center and "focusFirst.requestFocus()" in center)

check("TCL G07: a return within 3.5 s of the sound screen opening is read as its crash; the row then opens the TV's general settings for good (no crash loop)",
      "SOUND_FAIL_MS = 3500" in center and 'putLong("sound_t"' in center and 'putBoolean("sound_bad", true)' in center and 'if (prefs().getBoolean("sound_bad", false)) return new Intent(Settings.ACTION_SETTINGS);' in center
      and "soundReturned();" in center.split("void forgetLast()")[1][:80])
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
      'MIRROR = S.n("انعكاس الشاشة")' in text and 'MIRROR_HINT = S.n("من الجوال: شارك الشاشة واختر التلفزيون")' in text
      and 'tile("mirror", ControlText.MIRROR, ControlText.MIRROR_HINT, R.drawable.ic_cast,' in center
      and (ROOT / "android/launcher/src/main/res/drawable/ic_cast.xml").exists())
check("OK opens it through the host, locked as that app's own tile is (ParentalPolicy.TILE: the app's package)",
      "host.launchGuarded(ParentalPolicy.TILE, mirror, ControlText.MIRROR)" in center and "case TILE:" in policy)
check("the tile comes after «مخرج الصوت» and before the screensaver's, so the panel's first tiles stay where they were",
      center.index('tile("sound"') < center.index('tile("mirror"') < center.index('tile("saver"'))
# ---- «البلوتوث»: the TV's remotes-and-accessories screen, only when one of its actions resolves
bt_block = text[text.index("static final String[] BLUETOOTH_ACTIONS = {"):text.index("};", text.index("static final String[] BLUETOOTH_ACTIONS = {"))]
check("the Bluetooth actions, in order: Android's BLUETOOTH_SETTINGS, then Android TV's accessories action",
      re.findall(r'"([A-Za-z0-9_.]+)"', bt_block) == ["android.settings.BLUETOOTH_SETTINGS", "com.google.android.intent.action.CONNECT_INPUT"], bt_block)
check("…each one in the manifest's <queries> (Android 11+ resolves only what it may see)",
      all(f'<action android:name="{a}" />' in manifest.split("<queries>")[1].split("</queries>")[0] for a in ("android.settings.BLUETOOTH_SETTINGS", "com.google.android.intent.action.CONNECT_INPUT")))
check("the tile is there only when one resolves, opens it behind the TV's settings' lock, and has its own icon",
      "final Intent bluetooth = bluetoothIntent();" in center and "if (bluetooth != null)" in center
      and "pm.resolveActivity(new Intent(a), 0) != null" in center
      and "host.launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, bluetooth, ControlText.BLUETOOTH)" in center
      and 'BLUETOOTH = S.n("البلوتوث")' in text and 'BLUETOOTH_HINT = S.n("الريموت والملحقات")' in text
      and (ROOT / "android/launcher/src/main/res/drawable/ic_bluetooth.xml").exists())
check("Wi-Fi and Bluetooth side by side, first (docs/UX-REVIEW.md): the network tile, then Bluetooth, then the rest",
      center.index('tile("net"') < center.index('tile("bluetooth"') < center.index('tile("sound"') < center.index('tile("speed"')
      < center.index('tile("sleep"') < center.index('tile("off"') < center.index('tile("mirror"') < center.index('tile("saver"'))
check("at most ten tiles: sleep, off, network, speed, sound, mirroring, Bluetooth, screensaver, ours, the TV's settings",
      len(re.findall(r'tile\("[a-z]+"', center)) == 10, re.findall(r'tile\("[a-z]+"', center))
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
check("no new receiver, service or alarm for it", not re.search(r"registerReceiver|startService|AlarmManager|JobScheduler", center))
check("the phone remote and the accessibility service are untouched by it", "PhoneApi" not in center and "sleep_service" not in center)

# ---- the logic
if not shutil.which("javac"):
    print("SKIP control center logic: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/control_center_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(ROOT / "android/launcher/src/main/java/sa/urtv/home/Tr.java"), str(HOME / "ControlText.java"), str(HOME / "PillText.java"), str(HOME / "DigitsText.java"),
                    str(HOME / "SpeedTest.java"), str(ROOT / "tests/ControlTextRun.java"), str(ROOT / "tests/SpeedTestRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.ControlTextRun", str(ROOT / "tests/control_center_cases.tsv")],
                         check=True, capture_output=True).stdout
    got = out.decode("utf-8").split("\n")
    for c, g in zip(cases, got):
        want = c[5]
        # the digits of the answers in the table are written hindi for reading; the class writes western ones
        g_hindi = g.translate(str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩"))
        check(f"control center {c[0]}: {c[6] if len(c) > 6 else ''}", g == want or (c[0] == "mbps" and g_hindi.replace(".", "٫") == want), f"{g!r}, wanted {want!r}")
    check("every case answered", len(got) > len(cases), (len(got), len(cases)))
    r = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.SpeedTestRun"], capture_output=True, text=True)
    for l in r.stdout.splitlines():
        check("speed test: " + l.split(" ", 1)[1], l.startswith("ok "))
    check("speed test run ended well", r.returncode == 0, r.stderr[-300:])
cc = (ROOT / "android/launcher/src/main/java/sa/urtv/home/ControlCenter.java").read_text(encoding="utf-8")
check("Back inside «تخصيص» goes back to the panel's list, not out of it (the popup's own decor takes Back first, so dismiss() is overridden), «تخصيص» opens at the top with its own focus key, titles shrink before they are cut",
      "pw = new PopupWindow(root, W, H, true) {" in cc and "if (customising && !closeNow && body != null) { backFromCustom = true; showMain(); return; }" in cc
      and "void dismiss() { if (pw != null) { closeNow = true;" in cc and "custom.setOnClickListener(v -> { customKey = null; showCustom(); });" in cc and "c.item.key.equals(customKey)" in cc
      and "scroller.scrollTo(0, 0)" in cc and cc.count("setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP)") == 2 and "PANEL_W * 0.30f" in cc)
main_src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
check("the tray's scroll does no avoidable work per step: the name row's fade starts only when it changes, the Wi-Fi mark is touched only when it changes",
      "if (nameShown != inBar) { nameShown = inBar;" in main_src and "if (netShown == (online ? 1 : 0) && netInk == ink) return;" in main_src)

sys.exit(1 if failed else 0)
