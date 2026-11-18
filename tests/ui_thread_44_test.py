#!/usr/bin/env python3
"""4.4 (QA-6..QA-10, rule R1: the 4.3.3 TCL freeze's shape): no call into another app's process, and no reading of a file, on a path the UI
thread takes for a key, the minute tick, onCreate or a return, for the five paths QA found in 4.4. One section per finding; the whole graph is
QA's tests/regressions/ui_thread_ipc_test.py (its QA-6..10 baseline lines go when this is merged).
Run: python3 tests/ui_thread_44_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def src(p): return re.sub(r"//[^\n]*", "", p.read_text(encoding="utf-8"))
def body(s, head):
    return s.split(head)[1].split("\n    }\n")[0] if head in s else ""

# QA-6: Extras.enabled
ext = src(HOME / "Extras.java")
known = body(ext, "public static Boolean enabledKnown() {")
check("QA-6: Extras.enabledKnown never calls the add-on (the kept answer, a refresh on a thread)",
      "call(" not in known and "askNow(" not in known and "refresh();" in known and "return known;" in known)
check("QA-6: enabled() is the kept answer («not known yet» is off for a screen's line, never stored as an answer)",
      "public static boolean enabled() { return Boolean.TRUE.equals(enabledKnown()); }" in ext)
ref = body(ext, "static void refresh() {")
check("QA-6: refresh asks on a thread, once at a time, only when the answer is older than 2 s",
      "FRESH_MS" in ref and "asking.compareAndSet(false, true)" in ref and '"extras-status").start();' in ref)
ask = body(ext, "static Boolean askNow() {")
check("QA-6: a failed call keeps the last real answer; not installed is a real «off»",
      "if (b != null) known = b.getBoolean(ExtrasWords.ON, false);" in ask and "else if (!installed(c)) known = false;" in ask)
heal = src(HOME / "A11yHeal.java")
check("QA-6: «stuck» and «bound» need a real answer (unknown is neither)",
      "Boolean.FALSE.equals(Extras.enabledKnown())" in heal and "Boolean.TRUE.equals(Extras.enabledKnown())" in heal and "Extras.enabled()" not in heal)
phone = src(HOME / "PhoneApps.java")
check("QA-6: the phone page's server threads ask now (enabledNow), the only callers that may wait",
      "return Extras.enabledNow();" in phone and [f.name for f in HOME.rglob("*.java") if "Extras.enabledNow()" in src(f)] == ["PhoneApps.java"])
sleep = src(HOME / "SleepTimer.java")
off = body(sleep, "static String turnOff(Context c) {")
check("QA-6: «أطفئ الآن» and the timer's end ask the add-on on a thread; the screensaver comes up from the main thread without it",
      '"sleep-off").start();' in off and off.index("new Thread(") < off.index("Extras.sleepNow()") and "Looper.getMainLooper()).post(" in off)
carry = src(HOME / "UpdateCarry.java")
check("QA-6: the carry's Home press goes to the add-on on a thread", 'Extras.goHome()' in carry.split('"carry-front"')[0].split("new Thread(")[-1])
main = src(HOME / "MainActivity.java")
check("QA-6: the home refreshes the add-on's state at its return and on the tick's thread",
      "Extras.refresh();" in body(main, "protected void onResume() {") and "Extras.askNow();" in main.split("clockReads.execute(() -> {")[1].split("runOnUiThread")[0])

# QA-7: the screensaver settings' home_key
sa = src(APP / "SettingsActivity.java")
check("QA-7: the settings' render reads the kept answer; the home is asked on a thread, once per return, and told on a thread",
      "NowPlayingBridge.homeKey(this" not in sa and "final Boolean homeKey = homeKeySeen();" in sa and '"home-key").start();' in sa
      and '"home-key-set").start();' in sa and "homeKeyAsked = false;" in body(sa, "protected void onResume() {"))

# QA-8: the control centre's adhan preview
prev = src(HOME / "tilawah/AdhanPreview.java")
play = body(prev, "public static void play(Context c, int prayer) {")
check("QA-8: the preview's voice (it reads the place) is picked on a thread; the sound starts from the main thread",
      play.index("new Thread(") < play.index("playFor(") and '"adhan-preview").start();' in play and "Looper.getMainLooper()).post(() -> MusicService.adhan(" in play)

# QA-9: «المباريات» hands over the kept follows
ma = src(HOME / "MatchesActivity.java")
check("QA-9: «المباريات» hands the kept follows over on a thread", 'new Thread(() -> HomeFollows.sync(syncApp), "follows-sync").start();' in ma
      and "HomeFollows.sync(this)" not in ma)

# QA-10: the notice bar's file
due = body(main, "private NoticeRule.Notice dueNotice() {")
check("QA-10: the notice bar reads the kept copy, never the file", "notices.value()" in due and "ContentFile.read(" not in due)
check("QA-10: the copy is loaded on the tick's thread and at each return (off the UI thread)",
      "notices.load(app);" in main.split("clockReads.execute(() -> {")[1].split("runOnUiThread")[0]
      and "clockReads.execute(() -> notices.load(app));" in body(main, "protected void onResume() {"))
cf = src(HOME / "ContentFile.java")
load = body(cf, "synchronized void load(Context c) {").split("\n        }")[0] if "synchronized void load(Context c) {" in cf else ""
check("QA-10: the file is parsed again only when its time or size changed", "if (t == at && n == size) return;" in cf and "read(c, name)" in cf.split("synchronized void load(Context c) {")[1])
# 4.4 review: two reads of the screensaver's settings left on the UI thread by their own code
gr = src(HOME / "Greeting.java")
check("review: the greeting's weather answer reads the settings on its worker thread; the posted code only draws",
      "final ConfigSnapshot again = ConfigSnapshot.read(context);" in gr and "main.post(() -> { fetching = false; update(" in gr and "ConfigSnapshot.read(context)); });" not in gr)
rcv = main.split("private final BroadcastReceiver nowPlayingChanged = new BroadcastReceiver() {")[1].split("private final BroadcastReceiver sleepChanged")[0]
check("review: the adhan receiver takes the known answer, and reads only off the UI thread when there is none",
      "ConfigSnapshot.known()" in rcv and "clockReads.execute(" in rcv and "updateAdhanBar(ConfigSnapshot.read(" not in rcv)

print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
