#!/usr/bin/env python3
"""Typing from the phone into other apps (the accessibility service): the pure choice (TypeInto.java on a fake screen), then source checks that
it stays inside its limits: off until the owner turns the service on, the family lock and kids mode apply, no password fields, nothing of the
screen is logged, stored or sent back (the phone gets a fixed word), and it is written down plainly in the docs and the service's description.
Needs javac for the Java cases. Run: python3 tests/type_into_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def text(p):
    return (ROOT / p).read_text(encoding="utf-8")


HOME = "android/launcher/src/main/java/sa/urtv/home/"
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / HOME / "TypeInto.java"), str(ROOT / "tests/TypeIntoRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.TypeIntoRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every Java case ran", "ALL PASS" in out or "FAILED" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

svc = text(HOME + "TypeService.java")
sleep = text(HOME + "SleepService.java")
ti = text(HOME + "TypeInto.java")
act = text(HOME + "phone/PhoneActions.java")
res = "android/launcher/src/main/res/"
xml = text(res + "xml/type_service.xml")
body = svc.split("static String typeText(")[1].split("    /** The package an intent opens")[0]
check("typing is a service of its own, turned on on its own: it can read the screen, the sleep service cannot (no window content, no events)",
      'canRetrieveWindowContent="true"' in xml and 'android:accessibilityEventTypes="typeWindowStateChanged"' in xml and 'canRetrieveWindowContent="false"' in text(res + "xml/sleep_service.xml")
      and "android:accessibilityEventTypes" not in text(res + "xml/sleep_service.xml") and "getRootInActiveWindow" not in sleep and "AccessibilityNodeInfo" not in sleep
      and "onAccessibilityEvent(AccessibilityEvent e) { }" in svc)
man = text("android/launcher/src/main/AndroidManifest.xml")
check("both services are declared with the system's bind permission, each with its own xml and label",
      '<service\n            android:name=".TypeService"' in man and "@xml/type_service" in man and "@string/type_service_label" in man and "@xml/sleep_service" in man)
check("it only types when the service is on, the window names its package and is not ours, and the family lock and kids mode (also for the TV's settings and Wi-Fi screens) are asked first",
      "TypeInto.allowed(s != null, pkg, ours, pkg != null && Parental.needsPin(c, pkg), pkg != null && !ParentalPolicy.visible(st, pkg), systemScreen)" in body
      and body.index("TypeInto.allowed(") < body.index("TypeInto.pick(") and "ParentalPolicy.TV_SETTINGS" in body and "ParentalPolicy.WIFI" in body
      and 'pkg.equals("sa.urtv.screensaver")' in body and "pkg.equals(c.getPackageName())" in body)
check("a password field is never typed into", "TypeInto.fieldProblem(picked)" in body and "PASSWORD" in body)
check("nothing of the screen is stored (no preferences, no file, no broadcast in the typing code)", not re.search(r"SharedPreferences|getSharedPreferences|FileOutputStream|sendBroadcast|setPrimaryClip|toString\(\)\.getBytes", body + ti))
# TV6 row 10: window-state events are declared ONLY so Android tracks the active window (else getRootInActiveWindow stays null); none is read
xml_code = re.sub(r'<!--.*?-->', '', xml, flags=re.S)
ev = re.findall(r'android:accessibilityEventTypes="([^"]*)"', xml_code)
check("the typing service declares exactly one event type, typeWindowStateChanged (no content or text events), and reads none of them",
      ev == ["typeWindowStateChanged"] and "onAccessibilityEvent(AccessibilityEvent e) { }" in svc and "flagRetrieveInteractiveWindows" not in xml_code
      and "typeWindowContentChanged" not in xml_code and "typeViewTextChanged" not in xml_code and "canPerformGestures" not in xml_code)
# the log: tag UrTVType, fixed words and a length only. Every log line of the typing path is checked by pattern.
logs = re.findall(r'Log\.\w\(([^;]*)\);', svc + act) + re.findall(r'(?<!void )\blog\(([^;]*)\);', svc)
check("the typing path logs (tag UrTVType) at each hop", all(w in (svc + act) for w in ('"text request: mode="', '"foreground=other"', '"service=on"', '"service=off"', '"root=present"', '"root=null"', '"field=none"', '"field=password"', '"field=found"', '"focus_class="', '"search="', '"result="')) and len(logs) >= 6, len(logs))
okline = re.compile(r'^(?:Log\.\w\()?(?:"UrTVType", |"UrTVPhone", |TAG, )?("[a-z= ]*"|"text request: mode=" \+ \("append"\.equals\(mode\) \? "append" : "replace"\) \+ " search=" \+ search \+ " len=" \+ \(text == null \? 0 : text\.length\(\)\)|"result=" \+ word|"home_cards sent=" \+ want\.size\(\) \+ " saved=" \+ \(got == null \? -1 : got\.size\(\)\) \+ " same=" \+ same|[a-z]+ != null \? "service=on" : "service=off"|root != null \? "root=present" : "root=null"|r == TypeInto\.Refusal\.NO_FIELD \? "field=none" : r == TypeInto\.Refusal\.PASSWORD_FIELD \? "field=password" : "field=found"|"search=" \+ how|"focus_class=" \+ AppSearch\.safeClass\(f == null \? null : f\.getClassName\(\)\)|Foreground\.current\(\) == null \? "foreground=other" : "foreground=own"|line)$')
bad = [l for l in logs if not okline.match(l.strip())]
check("no log line carries the text, a package name or anything of a node: only fixed words and the length", not bad, bad)
check("a log line has its own tag and the helper is one line", 'static final String TAG = "UrTVType";' in svc and 'static void log(String line) { Log.i(TAG, line); }' in svc)
check("the focused input field (findFocus FOCUS_INPUT) is tried first, then the tree walk",
      "root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)" in body and body.index("findFocus(") < body.index("TypeInto.pick("))
check("a field is editable by the flag, an EditText class, or taking ACTION_SET_TEXT",
      "info.isEditable() || editText(info) || canSetText(info)" in svc and "AccessibilityAction.ACTION_SET_TEXT" in svc and 'endsWith("EditText")' in svc)
check("what the field holds is used only to build the new text, in one expression, and never returned", body.count("field.getText()") == 1 and "return now" not in body and "return out" not in body)
check("the answer is one of a few fixed words", re.search(r'TYPED = "typed_other", TYPED_NO_SEARCH = "typed_other_nosearch", OFF = "service_off", LOCKED = "locked", REFUSED = "refused",\s+NO_FIELD = "no_field", PASSWORD = "password_field", FAILED = "failed", SEARCHED = "searched";', svc) is not None
      and not re.search(r"return [a-z]+\.get(Text|ContentDescription|Package)", body))
check("the phone path: the service off is a failure the page explains (nothing is copied), locked, refused and password stay failures, only «no field» falls back to the clipboard",
      'default: return Result.fail(r);' in act and 'case "no_field": otherNoField[0] = true; break;' in act and 'if (Foreground.current() == null)' in act)
check("the node walk is bounded (depth and a small count)", "depth >= 40" in svc and "new int[] {500}" in svc)
check("search presses the keyboard's action only on Android 11+ and only when asked", "SDK_INT < 30" in body and "ACTION_IME_ENTER" in body and body.index("if (!search) return done(TYPED);") < body.index("ACTION_IME_ENTER"))
check("the state tells the page whether the typing service is on (its own field, apart from Back's keys_everywhere)", '"typing_service", PhoneApps.typingEnabled()' in act and '"keys_everywhere", PhoneApps.keysEverywhere()' in act)
for d in ("docs/FEATURES.md", "docs/SETTINGS.md"):
    t = text(d)
    check(f"{d} says plainly what the service sees and keeps", "الكتابة في التطبيقات الأخرى" in t or "typing into other apps" in t.lower())
    check(f"{d} names the separate service and how to turn it on", "type-setup" in t and "تلفزيونك: الكتابة من الجوال" in t)
strings = text(res + "values/strings.xml")
check("the typing service's own description (shown in the TV's accessibility settings) says what it sees and keeps",
      "لا ينظر في الشاشة إلا لحظة وصول نص" in strings and "لا يحفظ ولا يسجّل ولا يرسل" in strings and "ولا في تطبيق مقفول بالرقم السري" in strings)
check("the sleep service's description still says it reads nothing", "لا يقرأ شيئًا من الشاشة" in strings)
sys.exit(1 if failed else 0)
