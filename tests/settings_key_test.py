#!/usr/bin/env python3
"""The remote's Settings key OUTSIDE our home (SettingsKey.java, TypeService, tests/settings_key_cases.tsv): on a TCL the key's handler lives in
the stock launcher, which our setup turns off, so on HDMI the key did nothing. Our accessibility service TypeService (already on for the phone
remote) answers it there: our home in front -> the home's own path, OUR settings; another app -> the TV's settings; any other key -> passed on
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
ts = (HOME / "TypeService.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
xml = text("android/launcher/src/main/res/xml/type_service.xml")
xml_code = re.sub(r"<!--.*?-->", "", xml, flags=re.S)
man = text("android/launcher/src/main/AndroidManifest.xml")

check("SettingsKey has no Android in it (key numbers written out)", "import android" not in sk and "KeyEvent." not in re.sub(r"//.*|/\*.*?\*/", "", sk, flags=re.S))
check("SettingsKey: the same two key numbers as the home's RemoteKeys (176 and TCL's 83)",
      "KEYCODE_SETTINGS = 176" in sk and "KEYCODE_NOTIFICATION = 83" in sk and "KEYCODE_SETTINGS = 176" in text("android/launcher/src/main/java/sa/urtv/home/RemoteKeys.java"))
dec = sk.split("int decide(")[1].split("\n    }\n")[0]
check("any other key is passed on first, before anything is remembered", dec.split("\n")[1].strip() == "if (!isSettingsKey(keyCode)) return PASS;   // every other key: untouched, nothing remembered", dec[:200])
acts = re.findall(r'"([a-z.]+\.[A-Z_]+|android\.[a-z.]+)"', sk.split("TV_SETTINGS_ACTIONS = {")[1].split("};")[0])
check("what the key opens is one list, Android TV's settings for now (a better intent is one more line in front)", acts == ["android.settings.SETTINGS"], acts)

# the xml: may ask, does not set
check("type_service.xml declares canRequestFilterKeyEvents (the right to ask)", 'android:canRequestFilterKeyEvents="true"' in xml_code)
check("the xml does not set flagRequestFilterKeyEvents: the flag is set at run time only", "flagRequestFilterKeyEvents" not in xml_code and "accessibilityFlags" not in xml_code)
check("the sleep service may not ask for keys", "FilterKeyEvents" not in re.sub(r"<!--.*?-->", "", text("android/launcher/src/main/res/xml/sleep_service.xml"), flags=re.S))
check("the xml comment says why, and that an enabled service keeps its place on update", "canRequestFilterKeyEvents:" in xml and "without being turned on again" in xml)
check("the service is still declared once, with its xml, behind BIND_ACCESSIBILITY_SERVICE",
      man.count('android:name=".TypeService"') == 1 and "@xml/type_service" in man
      and 'android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"' in man.split('android:name=".TypeService"')[1][:300])
check("no new permission for this (the launcher declares the same ones)", man.count("<uses-permission") == 18)
check("the TV's accessibility settings tell the person about the Settings key", "زر الإعدادات في الريموت يفتح إعدادات التلفزيون" in text("android/launcher/src/main/res/values/strings.xml"))

# the service
fk = ts.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("the flag follows SettingsKey.filterWanted, set and cleared with setServiceInfo",
      "SettingsKey.filterWanted(front, stockHome)" in fk and "FLAG_REQUEST_FILTER_KEY_EVENTS" in fk and "setServiceInfo(info)" in fk and "~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS" in fk)
check("the filter is set when the service connects", "onServiceConnected() { running = this; filterKeys(); }" in ts)
ok = ts.split("protected boolean onKeyEvent(KeyEvent e) {")[1].split("\n    }\n")[0]
check("onKeyEvent: SettingsKey decides; PASS returns false at once; nothing is logged or kept there",
      "settingsKey.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), homeFront, stockHome)" in ok and "if (d == SettingsKey.PASS) return false;" in ok
      and "log(" not in ok and "Log." not in ok and "getScanCode" not in ok and ok.index("return false") < ok.index("main.post"), ok)
check("the TV's settings open after the key is answered (posted), never inside onKeyEvent", "main.post(this::openTvSettings)" in ok)
op = ts.split("private void openTvSettings() {")[1].split("\n    }\n")[0]
check("the TV's settings go through the family lock: with the PIN wanted, the home asks it (EXTRA_SETTINGS_KEY)",
      "Parental.needsPin(this, ParentalPolicy.targets(ParentalPolicy.REMOTE_SETTINGS," in op and "MainActivity.EXTRA_SETTINGS_KEY" in op
      and op.index("needsPin") < op.index("startActivity(i."))
skr = main.split("private void settingsKeyRequested(Intent i) {")[1].split("\n    }\n")[0]
check("the home answers EXTRA_SETTINGS_KEY once, through launchGuarded(REMOTE_SETTINGS)",
      "i.removeExtra(EXTRA_SETTINGS_KEY);" in skr and "launchGuarded(ParentalPolicy.REMOTE_SETTINGS, tv," in skr
      and main.count("settingsKeyRequested(getIntent());") == 1 and main.count("settingsKeyRequested(intent);") == 1)
logs = re.findall(r'\blog\(([^;]*)\);', ts.split("// ---------------------------------------------------------------- the Settings key")[1].split("/** One node of the real screen")[0])
check("the Settings key's log lines are fixed words only (no key code, no package)", logs and all(re.fullmatch(r'"settings key=[a-z]+"', l) for l in logs), logs)
check("the home tells the service when it comes to the front and when it leaves",
      "TypeService.homeFront(true);" in main.split("protected void onResume() {")[1].split("\n    }\n")[0]
      and "TypeService.homeFront(false);" in main.split("protected void onPause() {")[1].split("\n    }\n")[0])
check("a stock home is found by an ordinary HOME query (enabled packages only)", "queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)" in ts
      and "SettingsKey.stockHomeOn(pkgs, c.getPackageName())" in ts)
check("our home's own Settings key path is unchanged (RemoteKeys -> OUR settings)", "case RemoteKeys.OPEN_OUR_SETTINGS:" in main)

# docs
feat, sett, tvs = text("docs/FEATURES.md"), text("docs/SETTINGS.md"), text("docs/TVS.md")
check("FEATURES.md, SETTINGS.md and TVS.md say what the Settings key does outside our home",
      "SettingsKey" in tvs and "زر الإعدادات" in feat and "outside our home" in sett, "")
check("the receiver-mode design is written down (docs/TVS.md)", "«وضع الرسيفر»" in tvs and "Shell cannot change component state" in tvs)

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
