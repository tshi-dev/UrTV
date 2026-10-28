#!/usr/bin/env python3
"""The phone remote's keys into other apps through our accessibility service (RemoteNav.java on a plain JVM), then source checks of the wiring:
our own screens keep dispatchKeyEvent, PrivateScreen stays refused, the adhan's any-key rule is asked first, no new permission or service
capability, the log (UrTVRemote) has the key, the route and the answer only, and the page tells the person how to turn the service on.
Needs javac for the Java cases; the constants are also read from the cached android-all jar when it is there.
Run: python3 tests/remote_nav_test.py"""
import os
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
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / HOME / "RemoteNav.java"), str(ROOT / "tests/RemoteNavRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.RemoteNavRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every RemoteNav case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

# the numbers written out in RemoteNav are the framework's own
cache = Path(os.environ.get("URTV_CACHE") or Path(os.environ.get("XDG_CACHE_HOME") or Path.home() / ".cache") / "urtv")
jars = sorted(cache.glob("android-all-*.jar")) if cache.is_dir() else []
if shutil.which("javap") and (os.environ.get("URTV_ANDROID_JAR") or jars):
    jar = os.environ.get("URTV_ANDROID_JAR") or str(jars[-1])
    acc = subprocess.run(["javap", "-constants", "-cp", jar, "android.accessibilityservice.AccessibilityService"], capture_output=True, text=True).stdout
    view = subprocess.run(["javap", "-constants", "-cp", jar, "android.view.View"], capture_output=True, text=True).stdout
    want = {"GLOBAL_ACTION_BACK": 1, "GLOBAL_ACTION_HOME": 2, "GLOBAL_ACTION_DPAD_UP": 16, "GLOBAL_ACTION_DPAD_DOWN": 17, "GLOBAL_ACTION_DPAD_LEFT": 18, "GLOBAL_ACTION_DPAD_RIGHT": 19, "GLOBAL_ACTION_DPAD_CENTER": 20}
    got = {k: int(m.group(1)) for k in want if (m := re.search(rf"\b{k} = (\d+);", acc))}
    check("RemoteNav's GLOBAL_ACTION numbers are AccessibilityService's own (the real framework jar)", got == want, got)
    fv = {k: int(m.group(1)) for k in ("FOCUS_UP", "FOCUS_DOWN", "FOCUS_LEFT", "FOCUS_RIGHT") if (m := re.search(rf"\b{k} = (\d+);", view))}
    check("RemoteNav's FOCUS numbers are View's own", fv == {"FOCUS_UP": 33, "FOCUS_DOWN": 130, "FOCUS_LEFT": 17, "FOCUS_RIGHT": 66}, fv)
else:
    print("SKIP the framework constants: no javap or no cached android-all jar")

nav = text(HOME + "RemoteNav.java")
rn = {k: int(v) for k, v in re.findall(r"(GLOBAL_\w+) = (\d+)", nav)}
check("RemoteNav writes the same global action numbers the plan uses", rn.get("GLOBAL_DPAD_CENTER") == 20 and rn.get("GLOBAL_BACK") == 1 and rn.get("GLOBAL_HOME") == 2, rn)

act = text(HOME + "phone/PhoneActions.java")
key = act[act.index("@Override public Result key("):act.index("// ---- text")]
check("the adhan's any-key rule is asked first, before any key does anything",
      "RemoteNav.stopsAdhan(key, NowPlaying.adhanLive(), NowPlaying.adhanStopKey(app))" in key and key.index("stopsAdhan") < key.index("AudioManager audio") and "NowPlaying.stopAdhan(app)" in key)
check("our own screens keep the exact dispatchKeyEvent path, and the PIN keypad's private screen stays refused",
      "a.dispatchKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))" in key and "if (a instanceof PrivateScreen) return Result.fail(\"private_screen\")" in key
      and key.index("PrivateScreen") > key.index("if (a == null)") and key.index("a.dispatchKeyEvent") > key.index("PrivateScreen"))
check("another app in front: the service answers (done, limited, failed), service off is the old other_app",
      "PhoneApps.pressKey(key)" in key and 'Result.fail("other_app_limited")' in key and 'Result.fail(RemoteNav.FAILED.equals(r) ? "failed" : "other_app")' in key and "if (a == null)" in key)
check("Home goes through the service when one is on (right from any app), else the intent as before",
      'PhoneApps.keysEverywhere() && RemoteNav.DONE.equals(PhoneApps.pressKey("home"))' in key and "Intent.CATEGORY_HOME" in key)
check("the state tells the page whether the arrows reach other apps too", '"dpad_everywhere", PhoneApps.dpadEverywhere()' in act and '"keys_everywhere", PhoneApps.keysEverywhere()' in act)

ts, ss, pa = text(HOME + "TypeService.java"), text(HOME + "SleepService.java"), text(HOME + "PhoneApps.java")
check("the typing/remote service presses a key: a global action, else a focus move or a click on the window in front",
      "static String press(String key)" in ts and "s.performGlobalAction(st.code)" in ts and "cur.focusSearch(direction)" in ts and "AccessibilityNodeInfo.ACTION_FOCUS" in ts and "AccessibilityNodeInfo.ACTION_CLICK" in ts
      and "RemoteNav.plan(key, Build.VERSION.SDK_INT, true)" in ts)
check("the sleep service presses only global actions (it may not read the window): no node is touched there",
      "static String press(String key)" in ss and "RemoteNav.plan(key, Build.VERSION.SDK_INT, false)" in ss and "AccessibilityNodeInfo" not in ss and "getRootInActiveWindow" not in ss)
check("whichever of our services is connected is used: the typing/remote one first, then the sleep one",
      "String r = TypeService.press(key);" in pa and "SleepService.press(key)" in pa and "SleepService.enabled() || TypeService.enabled()" in pa)
check("the node walks are bounded (a count and a depth)", "new int[] {500}" in ts and "depth >= 40" in ts and "up < 6" in ts)
# privacy: the log
trace = text(HOME + "RemoteTrace.java")
check("the remote's log (tag UrTVRemote) is one line with the key's word, the route and the answer, written in one place",
      'Log.i("UrTVRemote", "key=" + key + " route=" + route.name().toLowerCase(Locale.ROOT) + " result=" + result)' in trace
      and "UrTVRemote" not in ts + ss + act + pa and ts.count("RemoteTrace.line(key, st.route, r);") == 1 and ss.count("RemoteTrace.line(key, st.route, r);") == 1)
code = ts[ts.index("static String press(String key)"):ts.index("private static String done(")]
check("no window content is read into a string, logged or returned by a key press", not re.search(r"getText\(|getContentDescription\(|getPackageName\(|getClassName\(|toString\(\)", code))
# no new capability
man = text("android/launcher/src/main/AndroidManifest.xml")
check("no permission was added (the launcher declares the same ones)", man.count("<uses-permission") == 18)
for xml in ("android/launcher/src/main/res/xml/type_service.xml", "android/launcher/src/main/res/xml/sleep_service.xml"):
    x = re.sub(r"<!--.*?-->", "", text(xml), flags=re.S)
    check(f"{Path(xml).name}: no gestures, no key-event filter, no interactive windows, no touch exploration",
          not re.search(r"canPerformGestures|canRequestFilterKeyEvents|flagRequestFilterKeyEvents|flagRetrieveInteractiveWindows|canRequestTouchExplorationMode|canTakeScreenshot|accessibilityEventTypes=\"[^\"]*(Content|Text|Click|Focus)", x), x)
check("the typing service still declares one event type", len(re.findall(r'accessibilityEventTypes="typeWindowStateChanged"', text("android/launcher/src/main/res/xml/type_service.xml"))) == 1)
strings = text("android/launcher/src/main/res/values/strings.xml")
check("the typing service is now «تلفزيونك: التحكم والكتابة من الجوال» and its description says arrows, OK, Back and Home",
      '<string name="type_service_label">تلفزيونك: التحكم والكتابة من الجوال</string>' in strings and "الأسهم والاختيار والرجوع والرئيسية" in strings)
for d in ("docs/FEATURES.md", "docs/SETTINGS.md"):
    t = text(d)
    check(f"{d} says the service presses the keys too, the log, and the adhan rule", "UrTVRemote" in t and "التحكم والكتابة من الجوال" in t)
check("dock.sh type-setup says what the service is for now", "remote and typing" in text("optimize/dock.sh") and "press the arrows, OK, Back and Home" in text("optimize/dock.sh"))
page = text("android/launcher/src/main/assets/phone/index.html")
check("the page: a note with the service's name when it is off, a short one when it reaches everything, and a clear word when nothing could be moved",
      "s.dpad_everywhere ?" in page and "فعّل خدمة «تلفزيونك: التحكم والكتابة من الجوال»" in page and "other_app_limited:" in page and "تحتاج أندرويد ١٣" in page)
print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
