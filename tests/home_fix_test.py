#!/usr/bin/env python3
"""4.4 the owner's TV, step 86 B («HOME DOES NOT WORK»): the five fixes, read from the source (the rules run in SetupEngineRun, SetupRunnerRun and
HomeWatchRun). 1. a working switch is never undone (confirmed when our home is drawn; Home still opening the stock home keeps ours the TV's home); the
role the TV takes back by itself is given back once. 2. the add-on's Home watch acts on Google TV's real home screen, also when the role went back,
and logs every decision. 3. one ADB key for good. 4. «إلغاء» on «السماح بتصحيح USB؟» is said, with «جهّز كل شي» again; the page never closes by
itself. 5. the setup page is a small window in the middle, its content scrolls inside it, the QR on the left.
    python3 tests/home_fix_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
HOME = SETUP.parent
EXTRAS = ROOT / "android/extras/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


eng, hook, boot, runner, act, adb, host = (text(SETUP / f) for f in ("SetupEngine.java", "SetupHook.java", "SetupBootReceiver.java", "SetupRunner.java", "SetupActivity.java", "AdbClient.java", "SetupHost.java"))
svc, prov, watch = text(EXTRAS / "ExtrasA11yService.java"), text(EXTRAS / "ExtrasCallProvider.java"), text(HOME / "HomeWatch.java")

# 1. the switch survives
fw = eng[eng.index("Result finishWatch(boolean watchOk)"):eng.index("static final String WATCH_FAILED")]
check("1. Home still opening the stock home after the add-on: nothing is put back (no abort), the switch is confirmed and the person told plainly",
      "abort(" not in fw and "confirmOurs();" in fw and "r = failed(WATCH_FAILED);" in fw and "ما قدرنا نخلي زر الرئيسية يفتح تلفزيونك" in eng)
co = eng[eng.index("private void confirmOurs()"):eng.index("static final String HOME_WANTED")]
check("1. a confirmed switch: the safety switch off, the deadline gone, the journal «done», HOME_WANTED on", all(w in co for w in ("deadman.disarm();", "clearDeadline();", 'journalState("done");', 'store.put(HOME_WANTED, "1");')))
cd = eng[eng.index("static boolean confirmDrawn("):eng.index("static boolean confirmDrawn(") + 900]
check("1. our home drawn as the TV's default home while «switching» (the person left the page, the run died): confirmed with no adb, and only then",
      '"  \\"state\\": \\"switching\\","' in cd and "deadman.disarm();" in cd and 'store.put("home-deadline.txt", "");' in cd
      and "SetupEngine.confirmDrawn(new FileStore(app), new SetupDeadman(app))" in hook and "!SetupRecovery.pending(app) || SetupRunner.get().running() || !oursIsDefault(app)" in hook
      and "confirmIfDrawn(c);" in hook[hook.index("public static void homeUp"):])
ra = hook[hook.index("public static void reapplyIfLost"):hook.index("private static volatile Boolean toldWatch")]
check("1. the TV took the role back by itself: given back ONCE (the time kept), logged; a second loss logged and left; never during a run or a pending switch",
      '"1".equals(trim(store.get(SetupEngine.HOME_WANTED)))' in ra and "oursIsDefault(app)) return;" in ra and "s.engine.reapplyHomeRole()" in ra
      and "lost again after one re-apply; left as it is" in ra and "re-applied once" in ra and "10 * 60_000L" in ra
      and "store.put(HOME_REAPPLIED, String.valueOf(now()));" in eng and "SetupHook.reapplyIfLost(app);" in boot and "reapplyIfLost(c);" in hook)
rr = eng[eng.index("boolean reapplyHomeRole()"):eng.index("boolean reapplyHomeRole()") + 500]
check("1. the re-apply only gives the role and the home activity to ours; nothing is turned off", "add-role-holder" in rr and "set-home-activity" in rr and "disable" not in rr)
check("1. «تراجع» and a stock home put back clear HOME_WANTED (nothing brings ours back after the person's own choice)",
      "clearFile(HOME_WANTED);" in eng[eng.index("Result undo()"):] and eng.count('store.put(HOME_WANTED, "");') >= 2)
# 2. the watch
ev = svc[svc.index("public void onAccessibilityEvent(AccessibilityEvent e)"):svc.index("private static void watchLog(int what)")]
check("2. the watch acts on the stock home's home screen also when the role went back (the setup's WANTED word), and logs each decision on a stock home's window",
      "watch.decide(on, pkg, cls, stockHomes, chosen || wantedNow, now)" in ev and ev.count("watchLog(") == 3
      and all(w in svc for w in ('"stock window its home activity"', '"decision bring ours"', '"decision not its home screen"', '"decision watch off"', '"decision ours not chosen"', '"decision paused"', '"role stock wanted yes"'))
      and "if (!stockPackages.contains(pkg)) return;" in ev)
check("2. the alias's real activity and launcherx's …HomeActivity count as its home screen; the service logs what it knows when it starts",
      "ri.activityInfo.targetActivity" in svc and 'simple.endsWith("HomeActivity")' in watch and '"service on watch on wanted yes"' in svc)
check("2. the home tells the add-on WANTED / UNWANTED (on its start and at boot, again until the add-on answers); the provider keeps it",
      "sa.urtv.home.Extras.homeWanted(app, wanted)" in hook and "SetupHook.tellWatch(app);" in boot and "tellWatch(c);" in hook
      and "ExtrasA11yService.setWanted(getContext(), ExtrasWords.WANTED.equals(arg))" in prov)
# 3. the key
check("3. one ADB key for good: no delete anywhere in the setup, the log says it is kept",
      "AdbKeys.delete" not in "".join(text(p) for p in SETUP.glob("*.java")) and "adb key: kept (one key for good)" in text(SETUP / "SetupSession.java"))
# 4. Cancel
check("4. AdbClient waits for the answer half a second at a time (Android 11+ too), ends at once on «إلغاء» or at the time limit",
      adb.count("if (approval != null && approval.cancelled())") == 2 and "throw new IOException(CANCELLED);" in adb and "Thread.sleep(500);" in adb
      and "@Override public boolean cancelled() { return approval != null && approval.cancelled(); }" in host)
check("4. the screen sees the prompt close (its focus back) and the runner counts «إلغاء» after 6 s with no answer; the words are the owner's; «جهّز كل شي» again",
      "r.promptClosed(System.currentTimeMillis())" in act and "CANCEL_GRACE_MS = 6000" in runner and "@Override public boolean cancelled() { return promptCancelled(host.now()); }" in runner
      and 'CANCELLED_TEXT = "ضغطت «إلغاء». اضغط «جهّز كل شي» مرة ثانية واختر «السماح دائماً».' in runner and "if (e.retry)" in act)
check("4. the setup page never closes by itself: its only finish() is «تم»", act.count("finish();") == 1 and act.index("finish();") > act.index('button(S.t("تم"))'))
# 5. the window
oc = act[act.index("protected void onCreate(Bundle b)"):act.index("setContentView(frame);") + 30]
check("5. a small window in the middle: 560 dp for every page (4.4 compact, Compact.WINDOW_DP), at most 80 % of the screen's height, centred in the 5 % safe area",
      "WINDOW_DP = sa.urtv.home.Compact.WINDOW_DP" in act and "WINDOW_HEIGHT = 0.80f" in act and "new WindowColumn(this, windowMaxHeight(sh))" in oc
      and "Gravity.CENTER" in oc and "Math.round(sw * 0.05f), Math.round(sh * 0.05f)" in oc and "MeasureSpec.AT_MOST" in act)
check("5. the title stays at the top of the window and only what is under it scrolls, inside the window (no page-wide scroll that cut the card's top off)",
      "setContentView(frame);" in oc and "setContentView(scroll)" not in act and oc.index('S.t("إعداد تلفزيوني")') < oc.index("new ScrollView(this)") and "inner.addView(body" in oc)
qb = act[act.index("private void qrBeside("):act.index("private void qrBeside(") + 1600]
check("5. the QR on the LEFT of the screen in both languages, beside its words (the rescue block and the computer's way)",
      "row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);" in qb and qb.index("row.addView(card") < qb.index("row.addView(words")
      and "qrBeside(words, RescueInfo.DOC_URL, sa.urtv.home.Compact.QR_DP);" in act and "qrBeside(words, HomeFails.GUIDE_URL, sa.urtv.home.Compact.QR_DP);" in act)
pv = text(SETUP / "SetupProgressView.java")
check("5. the steps are plain words (no pills): the step now in the accent and bold, the others dim; no «·» between the counts and the time",
      "p.state == NOW ? look.accent : p.state == FAILED ? look.warn : look.sub" in pv and '" · "' not in pv)
check("5. the mockup the owner sees is in docs/mockups/", (ROOT / "docs/mockups/setup-window-44.png").is_file())
sys.exit(1 if failed else 0)
