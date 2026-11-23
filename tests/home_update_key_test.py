#!/usr/bin/env python3
"""4.4 beta 2 (the tester, build 525): after an update of the home (pm install -r over 516, the add-on not updated) the FIRST Home press opened the stock
home although the HOME role was ours and the add-on's Home key catch on; after UrTV ran once, 3 of 3 ours. While the home was being replaced the add-on
judged its catch with our home not installed for a moment, turned it off, and judged it again only when our home came to the front.
- the home (works with the add-on as it is): at MY_PACKAGE_REPLACED, and again after the 8 s settle, the person's choice the add-on holds is told to it
  again as it is (WANTED or UNWANTED, never changed), on its own thread: the add-on judges the catch again with our home installed;
- the add-on (when it is updated too): our home's ADDED or REPLACED lets the watch's pause go and judges the catch again 1.5 s later; and a stock home's
  window while the catch should be on but is off turns it on for the next press.
- the tester's answer: on the owner's TV Google TV's home stays on and Android's HOME resolver names it the default while our home holds the role, so the
  add-on's «ours is chosen» (the resolver) was false there; the add-on now also asks our home whether it holds the HOME role.
Read from the sources; the watch's reset and the fake-resolver cases run in tests/HomeWatchRun.java (tests/extras_test.py).
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
      and "private final Runnable rearm = () -> { filterKeys(); refreshRole(); };" in svc)
check("registered when the service connects, let go when it unbinds", "watchReplaced();" in svc.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0]
      and "unregisterReceiver(replaced)" in svc.split("public boolean onUnbind(Intent i) {")[1].split("\n    }\n")[0])
ev = svc.split("public void onAccessibilityEvent(AccessibilityEvent e) {")[1].split("\n    }\n")[0]
check("a stock home's window while the catch should be on but is off: on again for the next press (other apps' windows untouched)",
      "if (!homeKeyFiltering() && homeKeyActive()) filterKeys();" in ev and ev.index("if (!stockPackages.contains(pkg)) return;") < ev.index("homeKeyFiltering()"))
check("the watch's reset lets the pause and the counted bursts go", "void reset() {" in hw and "pausedUntil = 0;" in hw.split("void reset() {")[1] and "java.util.Arrays.fill(brings, 0);" in hw.split("void reset() {")[1])
br = (H / "ExtrasBridge.java").read_text(encoding="utf-8")
hr = ext.split("static String homeRole(Context c) {")[1].split("\n    }\n")[0]
check("the home answers the add-on whether it holds the HOME role (RoleManager.isRoleHeld, Android 10+; unknown below or on an error)",
      "case ExtrasWords.HOME_ROLE:" in br and "Extras.homeRole(c)" in br and "Build.VERSION.SDK_INT < 29) return ExtrasWords.ROLE_UNKNOWN" in hr and "getMethod(\"isRoleHeld\", String.class).invoke(rm, ROLE_HOME)" in hr and 'ROLE_HOME = "android.app.role.HOME"' in ext)
oc = svc.split("private boolean oursChosen() {")[1].split("\n    }\n")[0]
check("the add-on's «ours is chosen» is the resolver OR the role our home holds (HomeWatch.oursChosen, the fake-resolver cases in HomeWatchRun)",
      "HomeWatch.oursChosen(" in oc and "roleAnswer" in oc)
rr = svc.split("private void refreshRole() {")[1].split("\n    }\n")[0]
check("the role is asked on a thread of its own (one at a time), at connect, after our home's install and again at a stock home's window; a change judges the catch on the main thread",
      '"extras-role").start();' in rr and "roleAsking.compareAndSet(false, true)" in rr and "main.post(this::filterKeys);" in rr
      and "refreshRole();" in svc.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0]
      and "private final Runnable rearm = () -> { filterKeys(); refreshRole(); };" in svc and "if (android.os.SystemClock.uptimeMillis() - roleAskedAt > ROLE_REASK_MS) refreshRole();" in ev)
check("no answer (an older home) keeps the last answer: never a guess", "if (r == null) return;" in rr)
# build 574 (the owner's TV): home, screensaver and add-on installed over 570 with adb; the add-on's own update unbound its service until the home healed it at its start
rep = (X / "ExtrasReplaced.java").read_text(encoding="utf-8")
xm = (ROOT / "android/extras/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("build 574: the add-on's own update (MY_PACKAGE_REPLACED, a manifest receiver) asks the home to heal its service at once, on a thread",
      'android:name="sa.urtv.home.ExtrasReplaced"' in xm and "android.intent.action.MY_PACKAGE_REPLACED" in xm.split('android:name="sa.urtv.home.ExtrasReplaced"')[1].split("</receiver>")[0]
      and "askHome(app, ExtrasWords.HEAL, null, null)" in rep and '"extras-replaced").start();' in rep and "pending.finish();" in rep)
check("the home answers HEAL at once and heals after its settle on its own thread (a «fix-…» why: not taken for its own update)",
      "case ExtrasWords.HEAL:" in br and 'A11yHeal.healSoon(c, "fix-addon-replaced");' in br
      and '"a11y-heal-addon").start();' in heal and "Thread.sleep(SETTLE_MS);" in heal.split("static void healSoon(")[1].split("\n    }\n")[0])
con = svc.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0]
check("the add-on's service arms the catch at its connect with the last role answer it kept (no wait for the home), and keeps each new answer",
      'roleAnswer = getSharedPreferences("extras", MODE_PRIVATE).getString(ROLE_SEEN, null);' in con and con.index("roleAnswer = getSharedPreferences") < con.index("filterKeys();")
      and '.putString(ROLE_SEEN, r).apply();' in svc)
print("FAILED" if failed else "all passed")
# beta 3, build 608 (the owner's TCL, Android 11: the first Home after installing the three over 581 opened the stock home again; 577 passed): the Xiaomi
# fix had gated the Home catch on filterSafe, false on a TCL with Android 11. The catch is off on Xiaomi ONLY; the three hold together:
# the Xiaomi filter-off, the TCL's YouTube key, the first-Home catch after the add-on's own update.
import re, shutil, subprocess, tempfile
ha = svc.split("private boolean homeKeyActive() {")[1].split("\n")[0]
check("608: the Home catch is gated by homeCatchSafe (Xiaomi only), never by filterSafe (false on a TCL's Android 11)",
      "KeyButton.homeCatchSafe()" in ha and "filterSafe" not in ha)
fk = svc.split("private void filterKeys() {")[1].split("\n    }\n")[0]
check("608: filterKeys asks for the Home catch as homeKeyActive alone (no filterSafe beside it); the TCL YouTube key and the chosen buttons keep theirs",
      "|| homeKeyActive());" in fk and "homeKeyActive() && KeyButton.filterSafe()" not in fk
      and "KeyApps.globalWanted(KeyButton.TV, wanted(this) || oursChosen(), KeyButton.filterSafe())" in fk
      and "KeyApps.filterWanted(front, KeyApps.anyChosen(keyChoices), KeyButton.filterSafe())" in fk)
con = svc.split("protected void onServiceConnected() {")[1].split("\n    }\n")[0]
check("608: the add-on replaced: the last role answer is read at connect and the filter judged then (the catch armed before the home answers)",
      "getString(ROLE_SEEN, null)" in con and "filterKeys()" in con and con.index("getString(ROLE_SEEN") < con.index("filterKeys()"))
check("608: a stock home's window while the catch should be on but is off turns it on for the next press",
      "if (!homeKeyFiltering() && homeKeyActive()) filterKeys();" in svc)
rb = [ROOT / "android/launcher/src/main/java/sa/urtv/home/RemoteButtons.java", ROOT / "android/app/src/main/java/sa/urtv/screensaver/RemoteButtons.java"]
bodies = [re.sub(r"^package [^;]+;", "", f.read_text(encoding="utf-8")) for f in rb]
check("608: the two RemoteButtons copies say the same", bodies[0] == bodies[1])
if shutil.which("javac") and shutil.which("java"):
    tmp = tempfile.mkdtemp()
    (Path(tmp) / "CatchRun.java").write_text("""package sa.urtv.home;
public final class CatchRun { public static void main(String[] a) {
  int[][] tv = {{RemoteButtons.TCL, 30}, {RemoteButtons.TCL, 31}, {RemoteButtons.XIAOMI_AFKR0, 30}, {RemoteButtons.XIAOMI, 28}, {RemoteButtons.XIAOMI_PATCHWALL_ONLY, 30}, {RemoteButtons.NONE, 30}};
  for (int[] t : tv) System.out.println(RemoteButtons.homeCatchSafe(t[0]) + " " + RemoteButtons.filterSafe(t[0], t[1]));
} }""", encoding="utf-8")
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(rb[0]), str(Path(tmp) / "CatchRun.java")], capture_output=True, text=True)
    check("608: RemoteButtons compiles alone", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        got = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.CatchRun"], capture_output=True, text=True).stdout.strip().split("\n")
        want = ["true false", "true true", "false false", "false false", "false false", "true true"]
        for name, g, w in zip(["TCL Android 11: Home catch on (remote-buttons filter off)", "TCL Android 12: both on", "Xiaomi AFKR0: both off",
                               "Xiaomi MiTV-M: both off", "Xiaomi PatchWall: both off", "another TV: both on"], got + [""] * 6, want):
            check("608: " + name, g == w, g)

sys.exit(1 if failed else 0)
