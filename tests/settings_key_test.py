#!/usr/bin/env python3
"""The remote's Settings key OUTSIDE our home (SettingsKey.java, TypeService, tests/settings_key_cases.tsv): on a TCL the key's handler lives in
the stock launcher, which our setup turns off, so on HDMI the key did nothing. Our accessibility service TypeService (already on for the phone
remote) answers it there: our home in front -> the home's own path (a short press the TV's settings, a long one OUR settings); another app -> the TV's settings; any other key -> passed on
untouched. Then the wiring and the privacy rules, read from the source: the service only ASKS for key events in its xml (the flag is set at run
time, only while the home is not in front and no stock home is enabled), logs fixed words only, and opens through the family lock.
Run: python3 tests/settings_key_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def text(p): return (ROOT / p).read_text(encoding="utf-8")

sk = (HOME / "SettingsKey.java").read_text(encoding="utf-8")
ts = text("android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java")   # 4.3.3: the service is the add-on's («إضافات تلفزيونك»)
bridge = (HOME / "ExtrasBridge.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
xml = text("android/extras/src/main/res/xml/extras_service.xml")
xml_code = re.sub(r"<!--.*?-->", "", xml, flags=re.S)
man = text("android/launcher/src/main/AndroidManifest.xml")
xman = text("android/extras/src/main/AndroidManifest.xml")

check("SettingsKey has no Android in it (key numbers written out)", "import android" not in sk and "KeyEvent." not in re.sub(r"//.*|/\*.*?\*/", "", sk, flags=re.S))
check("SettingsKey: the same two key numbers as the home's RemoteKeys (176 and TCL's 83)",
      "KEYCODE_SETTINGS = 176" in sk and "KEYCODE_NOTIFICATION = 83" in sk and "KEYCODE_SETTINGS = 176" in text("android/launcher/src/main/java/sa/urtv/home/RemoteKeys.java"))
check("SettingsKey: TCL G05's vendor Settings key 4077 is in both SettingsKey and RemoteKeys",
      "KEYCODE_TCL_SETTINGS = 4077" in sk and "TCL_SETTINGS = 4077" in text("android/launcher/src/main/java/sa/urtv/home/RemoteKeys.java"))
dec = sk.split("int decide(")[1].split("\n    }\n")[0]
check("any other key is passed on first, before anything is remembered", dec.split("\n")[1].strip() == "if (!isSettingsKey(keyCode)) return PASS;   // every other key: untouched, nothing remembered", dec[:200])
acts = re.findall(r'"([a-z.]+\.[A-Z_]+|android\.[a-z.]+)"', sk.split("TV_SETTINGS_ACTIONS = {")[1].split("};")[0])
check("what the key opens is one list, Android TV's settings for now (a better intent is one more line in front)", acts == ["android.settings.SETTINGS"], acts)

# the xml: may ask, does not set
check("extras_service.xml declares canRequestFilterKeyEvents (the right to ask)", 'android:canRequestFilterKeyEvents="true"' in xml_code)
check("the xml does not set flagRequestFilterKeyEvents: the flag is set at run time only", "flagRequestFilterKeyEvents" not in xml_code and "accessibilityFlags" not in xml_code)
check("the xml comment says why", "canRequestFilterKeyEvents:" in xml and "set at run" in xml)
check("the service is declared once, in the add-on, with its xml, behind BIND_ACCESSIBILITY_SERVICE; the home has none",
      xman.count('android:name="sa.urtv.home.ExtrasA11yService"') == 1 and "@xml/extras_service" in xman and "BIND_ACCESSIBILITY_SERVICE" not in man
      and 'android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"' in xman.split('android:name="sa.urtv.home.ExtrasA11yService"')[1][:300])
check("no new permission for this (the launcher declares the same ones, and our own EXTRAS one)", man.count("<uses-permission") == 25)   # 25 since 4.4 beta 2 «تغيير الأيقونة» (READ_MEDIA_IMAGES, asked only when the person picks an image); 24 since 4.4 the overlay permission moved to the add-on (was 25 with SYSTEM_ALERT_WINDOW);   # 25 since 4.4 the kick-off alert (SYSTEM_ALERT_WINDOW, «فوق تطبيق آخر», off by default); 19 since REQUEST_DELETE_PACKAGES («حذف التطبيق» in the app menu); 21 since targetSdk 34: FOREGROUND_SERVICE_SPECIAL_USE (SetupService must name its type) and USE_EXACT_ALARM (the adhan stays exact on a new install)
check("the TV's accessibility settings tell the person about the Settings key", "زر الإعدادات في الريموت يفتح إعدادات التلفزيون" in text("android/extras/src/main/res/values/strings.xml"))

# the service
fk = ts.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("the flag follows SettingsKey.filterWanted, set and cleared with setServiceInfo",
      "Build.VERSION.SDK_INT >= SettingsKey.MIN_FILTER_SDK && (" in fk and "SettingsKey.filterWanted(front, stockHome, lowRam, homeKeyLost, Build.VERSION.SDK_INT)" in fk and "FLAG_REQUEST_FILTER_KEY_EVENTS" in fk and "setServiceInfo(info)" in fk and "~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS" in fk)
check("the filter is set when the service connects", "filterKeys();" in ts.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0])
ok = ts.split("private boolean keyEvent(KeyEvent e) {")[1].split("\n    }\n")[0]   # (4.3.6: onKeyEvent wraps it in a catch-all)
check("onKeyEvent: SettingsKey decides; PASS returns false at once; nothing is logged or kept there",
      "settingsKey.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), homeFront, stockHome, lost != null && lost)" in ok and "if (d == SettingsKey.PASS) return false;" in ok
      and "log(" not in ok and "Log." not in ok and "getScanCode" not in ok and "getMetaState" not in ok and ok.index("return false") < ok.index("new Thread"), ok)
check("the TV's settings open after the key is answered (another thread), never inside onKeyEvent", 'new Thread(this::openTvSettings, "extras-settings").start();' in ok)
check("Haier MatrixTV (4.1 reports): where the SystemUI action resolves to nothing, the key is handed to the home's own path (posted, a copy of the event); "
      "the check is once per service, and the manifest may see that action",
      "askHome(this, ExtrasWords.KEY, null, b)" in ok and "MainActivity.keyFromService(e)" in bridge and "SettingsKey.isSettingsKey(e.getKeyCode())" in bridge
      and "if (homeKeyLost == null) homeKeyLost = homeKeyLost(this);" in fk and "queryIntentActivities(new Intent(SettingsKey.NOTIFICATION_PANEL_ACTION), 0).isEmpty()" in ts
      and '"android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL"' in sk and '<action android:name="android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL" />' in xman
      and "if (a != null && a.resumed && !a.isDestroyed()) a.dispatchKeyEvent(e);" in main)
op = ts.split("private void openTvSettings() {")[1].split("\n    }\n")[0]
check("the TV's settings go through the family lock: the home says whether the PIN is wanted (fail closed: no answer = the home asks it, EXTRA_SETTINGS_KEY)",
      "askHome(this, ExtrasWords.SETTINGS_PIN," in op and 'r == null || r.getBoolean(ExtrasWords.NEEDS_PIN, true)' in op and '"sa.urtv.home.SETTINGS_KEY"' in op
      and 'EXTRA_SETTINGS_KEY = "sa.urtv.home.SETTINGS_KEY"' in main and "Parental.needsPin(c, ParentalPolicy.targets(ParentalPolicy.REMOTE_SETTINGS, arg))" in bridge
      and op.index("NEEDS_PIN") < op.index("startActivity(i."))
skr = main.split("private void settingsKeyRequested(Intent i) {")[1].split("\n    }\n")[0]
check("the home answers EXTRA_SETTINGS_KEY once, through launchGuarded(REMOTE_SETTINGS)",
      "i.removeExtra(EXTRA_SETTINGS_KEY);" in skr and "launchGuarded(ParentalPolicy.REMOTE_SETTINGS, tv," in skr
      and main.count("settingsKeyRequested(getIntent());") == 1 and main.count("settingsKeyRequested(intent);") == 1)
logs = re.findall(r'\blog\(([^;]*)\);', ts.split("// ---------------------------------------------------------------- the remote's Settings key")[1].split("// ---------------------------------------------------------------- typing from the phone")[0])
check("the Settings key's and the remote buttons' («أزرار الريموت») log lines are fixed words only (no key code, no package)", logs and all(re.fullmatch(r'"(settings key|key app)=[a-z]+"', l) for l in logs), logs)
ev = ts.split("public void onAccessibilityEvent(AccessibilityEvent e) {")[1].split("\n    }\n")[0]
check("the service tells from the window that comes to the front whether our home is in front (its package only), and sets the filter again when it changes",
      "boolean front = pkg.equals(HOME_PKG);" in ev and "if (front != homeFront) {" in ev and "homeFront = front;" in ev and ev.index("homeFront = front;") < ev.index("filterKeys();"))
check("a stock home is found by an ordinary HOME query (enabled packages only)", "queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)" in ts
      and "SettingsKey.stockHomeOn(java.util.Collections.singletonList(ri.activityInfo.packageName), HOME_PKG)" in ts)
check("our home's own Settings key path stays the home's (RemoteKeys: a long press OUR settings, a short one the TV's)", "case RemoteKeys.OPEN_OUR_SETTINGS:" in main and "case RemoteKeys.OPEN_SETTINGS:" in main)

# docs
feat, sett, tvs = text("docs/FEATURES.md"), text("docs/SETTINGS.md"), text("docs/TVS.md")
check("FEATURES.md, SETTINGS.md and TVS.md say what the Settings key does outside our home",
      "SettingsKey" in tvs and "زر الإعدادات" in feat and "outside our home" in sett, "")
check("the receiver-mode design is written down (docs/TVS.md)", "«وضع الرسيفر»" in tvs and "Shell cannot change component state" in tvs)

check("4.3.6: homeKeyLost is asked only on an Android TV (FEATURE_LEANBACK) of 11 or newer", "SettingsKey.homeKeyLostApplies(Build.VERSION.SDK_INT, c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK))" in ts)
okc = re.search(r"protected boolean onKeyEvent\(KeyEvent e\) \{\s*try \{[^}]*return keyEvent\(e\);\s*\} catch \(Throwable t\) \{\s*(?:log\(.settings key=error.\);[^\n]*\s*)?return false;", ts)
check("4.3.6: onKeyEvent never throws into Android's key dispatch (catches everything and gives the key back)", bool(okc))
check("4.3.6 review F8: the catch-all logs one fixed word, never the key or the exception", 'log("settings key=error");' in ts and "t.getMessage" not in ts)
if not shutil.which("javac"):
    print("SKIP settings key cases: no javac here")
    sys.exit(1 if failed else 0)
cases = [l.split("\t") for l in (ROOT / "tests/settings_key_cases.tsv").read_text(encoding="utf-8").split("\n") if l and not l.startswith("#")]
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "SettingsKey.java"), str(HOME / "RemoteKeys.java"), str(ROOT / "tests/SettingsKeyRun.java")], check=True)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SettingsKeyRun", str(ROOT / "tests/settings_key_cases.tsv")],
                         check=True, capture_output=True).stdout.decode("utf-8").split("\n")
for c, g in zip(cases, out):
    if c[0] == "reset": continue
    check(f"settings key: {c[7]}", g == c[6], f"{g!r}, wanted {c[6]!r}")
check("an answer for every case", len(out) - 1 >= len(cases), (len(out), len(cases)))
sys.exit(1 if failed else 0)
