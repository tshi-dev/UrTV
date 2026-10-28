#!/usr/bin/env python3
"""The owner's TV test of 3.147 (fix7): the AppSearch cases (YouTube's own search for typing, the class name logged), then source checks of the
wiring: the screen stays on only while the pairing / «سماح» / update / setup screens are shown, the cards saved from the phone reach the
home in front («بدون» included), the typing answers are explained on the page, and the Settings key's two numbers.
Needs javac for the Java cases. Run: python3 tests/fix7_test.py"""
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
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / HOME / "AppSearch.java"), str(ROOT / "tests/AppSearchRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.AppSearchRun"], capture_output=True, text=True).stdout
    for line in out.splitlines():
        if line.startswith(("PASS ", "FAIL ")):
            print(line)
            failed += line.startswith("FAIL ")
    check("every AppSearch case ran", "ALL PASS" in out, out[-200:])
else:
    print("SKIP the Java cases: no javac")

# 1. the screen stays on only while these screens are shown
KEEP = "FLAG_KEEP_SCREEN_ON"
pa, ua, su = text(HOME + "PhoneActivity.java"), text(HOME + "UpdateActivity.java"), text(HOME + "setup/SetupActivity.java")
check("the phone screen keeps the screen on while it shows the pairing code or the «سماح/رفض» prompt, and lets go when it pauses",
      "keepAwake(control.isOn());" in pa and re.search(r"void onPause\(\)[^}]*keepAwake\(false\)", pa, re.S) and KEEP in pa and "clearFlags" in pa)
check("the update screen keeps it on from onResume and clears it in onPause",
      re.search(r"void onResume\(\).*?addFlags\([^)]*" + KEEP, ua, re.S) and re.search(r"void onPause\(\).*?clearFlags\([^)]*" + KEEP, ua, re.S))
check("setup keeps it on only while it is running (not from onCreate)", su.count("addFlags(WindowManager.LayoutParams." + KEEP + ")") == 1
      and su.index("running = true;") < su.index(KEEP) and "clearFlags" in su and su.index("running = false;") < su.index("clearFlags"))
others = [f.name for f in (ROOT / HOME).rglob("*.java") if f.name not in ("PhoneActivity.java", "UpdateActivity.java", "SetupActivity.java", "PlayerActivity.java") and KEEP in f.read_text(encoding="utf-8")]
# (the Quran player screen kept it on before this change; it is not one of the screens of the owner's report)
check("no other screen of the launcher keeps the screen awake", not others, others)

# 3. the cards saved from the phone reach the home that is in front
act = text(HOME + "phone/PhoneActions.java")
ma = text(HOME + "MainActivity.java")
check("a saved set of cards (empty list = «بدون» too) sends the home a message", "PhoneApps.cardsChanged(app);" in act
      and 'extras.putString("home_panel", cards.length() == 0 ? "none" : cards.toString())' in act)
check("the home follows it at once when it is in front, with its own (not exported) receiver",
      "InternalReceivers.register(this, cardsChanged, new IntentFilter(HomeCard.CHANGED))" in ma and "if (resumed) { homeCard.refresh(); wireFocus(); }" in ma
      and "unregisterReceiver(cardsChanged)" in ma)
check("the message stays inside the app", "setPackage(app.getPackageName())" in text(HOME + "PhoneApps.java") and 'CHANGED = "sa.urtv.home.CARDS_CHANGED"' in text(HOME + "HomeCard.java"))
sr = text("android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java")
check("the receiver takes none / an empty list as «no cards»", 'if (h.isEmpty() || "none".equals(h)) continue;' in sr and "if (homeCards != null) p.setHomePanels(homeCards);" in sr)

# 2. typing where the app has no field
ts = text(HOME + "TypeService.java")
check("YouTube TV in front: its own search first, on any screen and whatever the field, by ACTION_SEARCH then its web link, both bound to the package",
      "if (AppSearch.supports(pkg)) {" in ts and "new Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(SearchManager.QUERY, q)" in ts
      and "new Intent(Intent.ACTION_VIEW, Uri.parse(AppSearch.webUrl(q))).setPackage(pkg)" in ts and ts.index("AppSearch.supports(pkg)") < ts.index("root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)")
      and 'return done(SEARCHED)' in ts)
check("the search does not ask resolveActivity (Android 11+ package visibility made it always null): it starts the intent and goes to the next way on a refusal",
      "resolveActivity" not in ts[ts.index("private static String search("):ts.index("private static String done(")] and "catch (RuntimeException e)" in ts and 'log("search=" + how);' in ts)
check("when there is no field only the focused node's class name (AppSearch.safeClass) is logged", 'log("focus_class=" + AppSearch.safeClass(f == null ? null : f.getClassName()));' in ts)
check("the search comes after the lock checks (never into a locked app)", ts.index("TypeInto.allowed(") < ts.index("AppSearch.supports(pkg)"))
page = text(HOME.replace("java/sa/urtv/home/", "assets/phone/") + "index.html")
check("the phone page explains «searched» and «no field in the app in front»", "searched: '" in page and "copied_other: '" in page and "j.result === 'copied_other'" in page)
check("copied_other is what the phone says when another app was in front and nothing could be typed", '"copied_other"' in act and "otherNoField[0] && \"copied\".equals(r)" in act)

check("the cards save is asked back of the screensaver: what it holds is what was sent, else the page is told «not_saved»",
      'saver("phone_state", null, null)' in act and 'Result.fail("not_saved")' in act and '"home_cards sent="' in act and "not_saved: '" in page)

# 4. the Settings key
rk = text(HOME + "RemoteKeys.java")
check("176 and 83 both open our settings; 176 is kept for TVs that pass it on", "case KEYCODE_SETTINGS:\n            case KEYCODE_NOTIFICATION: return new Decision(OPEN_OUR_SETTINGS, null);" in rk
      and "The system itself takes 176" in rk)

print("ALL PASS" if not failed else f"{failed} FAILED")
sys.exit(1 if failed else 0)
