#!/usr/bin/env python3
"""4.4 beta 2 (the tester, build 525): after an update of the home (pm install -r over 516, the add-on not updated) the FIRST Home press opened the stock
home although the HOME role was ours and the add-on's Home key catch on; after UrTV ran once, 3 of 3 ours. While the home was being replaced the add-on
judged its catch with our home not installed for a moment, turned it off, and judged it again only when our home came to the front.
- the home (works with the add-on as it is): at MY_PACKAGE_REPLACED, and again after the 8 s settle, the person's choice the add-on holds is told to it
  again as it is (WANTED or UNWANTED, never changed), on its own thread: the add-on judges the catch again with our home installed;
- the add-on (when it is updated too): our home's ADDED or REPLACED lets the watch's pause go and judges the catch again 1.5 s later; and a stock home's
  window while the catch should be on but is off turns it on for the next press.
Read from the sources; the watch's reset runs in tests/HomeWatchRun.java (tests/extras_test.py).
    python3 tests/home_update_key_test.py"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
X = ROOT / "android/extras/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


heal = (H / "A11yHeal.java").read_text(encoding="utf-8")
ext = (H / "Extras.java").read_text(encoding="utf-8")
svc = (X / "ExtrasA11yService.java").read_text(encoding="utf-8")
hw = (H / "HomeWatch.java").read_text(encoding="utf-8")
prov = (X / "ExtrasCallProvider.java").read_text(encoding="utf-8")
man = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

rec = heal.split("@Override public void onReceive(Context context, Intent intent) {")[1].split("\n    }\n")[0]
check("at our replace the add-on is told at once and again after the settle", 'if ("replaced".equals(why)) rearm(app, "replaced");' in rec and 'if ("replaced".equals(why)) rearm(app, "settled");' in rec
      and rec.index('rearm(app, "settled")') < rec.index("check(app, why)"))
check("A11yHeal is the MY_PACKAGE_REPLACED receiver (only the system sends it)", '<receiver\n            android:name=".A11yHeal"' in man and "MY_PACKAGE_REPLACED" in man.split('android:name=".A11yHeal"')[1].split("</receiver>")[0])
rm = heal.split("static void rearm(final Context app, final String when) {")[1].split("\n    }\n")[0]
check("the call into the add-on runs on its own thread, a fixed word logged", 'new Thread(() -> Log.i(TAG, "add-on home key " + when + ": " + Extras.rearmHomeKey(app)), "addon-rearm").start();' in rm)
rk = ext.split("static String rearmHomeKey(Context c) {")[1].split("\n    }\n")[0]
check("the person's choice is read first and told back as it is (never changed): WANTED stays WANTED, UNWANTED stays UNWANTED",
      "call(u, ExtrasWords.WATCH, null, null)" in rk and "now.getBoolean(ExtrasWords.WANTED) ? ExtrasWords.WANTED : ExtrasWords.UNWANTED" in rk and "HOME_KEY_ON" not in rk and "HOME_KEY_OFF" not in rk)
check("never a silent no-op: no add-on, no answer, a switched-off catch each say so", all(w in rk for w in ['"no-answer"', '"no-addon"', '"catch-off"', '"rearmed"']))
sw = svc.split("static void setWanted(Context c, boolean on) {")[1].split("\n    }\n")[0]
check("the add-on judges the catch again when told the person's choice (what the home's re-arm relies on, also in the add-on as shipped)", "s.main.post(s::filterKeys);" in sw
      and "ExtrasA11yService.setWanted(getContext(), ExtrasWords.WANTED.equals(arg))" in prov)
wr = svc.split("private void watchReplaced() {")[1].split("\n    }\n")[0]
check("the add-on: our home's ADDED or REPLACED (our package only) lets the watch's pause go and judges the catch 1.5 s later",
      "Intent.ACTION_PACKAGE_ADDED" in wr and "Intent.ACTION_PACKAGE_REPLACED" in wr and "addDataSchemeSpecificPart(HOME_PKG, android.os.PatternMatcher.PATTERN_LITERAL)" in wr
      and "HOME_PKG.equals(d.getSchemeSpecificPart())" in wr and "watch.reset();" in wr and "main.postDelayed(rearm, REARM_MS);" in wr and "REARM_MS = 1_500" in svc
      and "private final Runnable rearm = this::filterKeys;" in svc)
check("registered when the service connects, let go when it unbinds", "watchReplaced();" in svc.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0]
      and "unregisterReceiver(replaced)" in svc.split("public boolean onUnbind(Intent i) {")[1].split("\n    }\n")[0])
ev = svc.split("public void onAccessibilityEvent(AccessibilityEvent e) {")[1].split("\n    }\n")[0]
check("a stock home's window while the catch should be on but is off: on again for the next press (other apps' windows untouched)",
      "if (!homeKeyFiltering() && homeKeyActive()) filterKeys();" in ev and ev.index("if (!stockPackages.contains(pkg)) return;") < ev.index("homeKeyFiltering()"))
check("the watch's reset lets the pause and the counted bursts go", "void reset() {" in hw and "pausedUntil = 0;" in hw.split("void reset() {")[1] and "java.util.Arrays.fill(brings, 0);" in hw.split("void reset() {")[1])
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
