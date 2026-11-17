#!/usr/bin/env python3
"""4.4 the owner's Google TV, steps 87-88 (the Home doctor and R1-R3): on this TV the Home KEY is the TV's own whatever the HOME role says, so the
add-on's watch is what brings UrTV. Read from the source (the rules run in HomeWatchRun and SetupRunnerRun R5e, R5g, R35w):
the watch's guard counts one bring per Home press; the add-on hears «wanted» at the end of a setup; the report and the success sentence say how Home
works here; a dialog that never came is said after 30 s; the dialog's hint is the four keys; the language, the count and the end sentence; the setup page
comes back after a check; R3 behind a switch that is off.
    python3 tests/home_fix2_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
HOME = SETUP.parent
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


def text(p):
    return Path(p).read_text(encoding="utf-8")


watch, host, hook, eng, rep, runner, act, pv = (text(p) for p in (HOME / "HomeWatch.java", SETUP / "SetupHost.java", SETUP / "SetupHook.java", SETUP / "SetupEngine.java",
                                                                   SETUP / "SetupReport.java", SETUP / "SetupRunner.java", SETUP / "SetupActivity.java", SETUP / "SetupProgressView.java"))
svc = text(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasA11yService.java")
check("1. the watch (4.4 R2g, step 88): a press over another app always brought, never counted; over ours one count per 0.7 s burst, the 6th in 4 s pauses 30 s (build 413 T2); the burst never extends itself",
      "if (!fight) return true;" in watch and "BURST_MS = 700" in watch and "WINDOW_MS = 4_000, PAUSE_MS = 30_000" in watch
      and "if (lastBurst > 0 && now - lastBurst < BURST_MS) return true;" in watch and watch.count("lastBurst = now;") == 1 and "recent >= MAX_BRINGS - 1" in watch)
check("2. «wanted» reaches the add-on: before the Home check once it is bound, after every end of a run, and at the home's start",
      "if (on.status == SetupEngine.Status.DONE) c.host.homeWanted(true);" in runner and runner.index("c.host.homeWanted(true);") < runner.index("r = c.engine.finishWatch(")
      and "SetupHook.tellWatchNow(app);" in host and "sa.urtv.home.Extras.homeWanted(app, wanted)" in host and "tellWatch(c);" in hook)
check("3. on a Google TV the success sentence says the add-on is what brings UrTV and must stay on; the report says the role and whether the watch is bound",
      "finishHome(null, WATCH_OK)" in eng and "خلّها مفعّلة" in eng and 'out.add(homeHow());' in rep and '"Home role: "' in rep and 'Home watch («إضافات تلفزيونك»): " + (watch ? "bound" : "not bound")' in rep)
check("4. no dialog for 30 s (nothing covered the setup screen): «ما ظهرت رسالة السماح؟ أعد تشغيل التلفزيون ثم اضغط «جهّز كل شي» مرة ثانية», with the button; the screen's pause or lost focus counts as covered",
      "NO_DIALOG_MS = 30_000" in runner and 'NO_DIALOG_TEXT = "ما ظهرت رسالة السماح؟ أعد تشغيل التلفزيون ثم اضغط «جهّز كل شي» مرة ثانية.' in runner
      and "NO_DIALOG_TEXT.equals(text)" in runner and act.count("promptCoveredNow()") == 2)
check("5. the dialog's hint is the four keys (اختيار ← تحت ← يسار ← اختيار), the drawing shows the box and «السماح» on the left with the four keys under it",
      'S.t("على الريموت: اختيار (يعلّم «السماح دائماً») ← تحت ← يسار ← اختيار (على «السماح»)")' in act and 'String[] keys = {ok, "▼", "◀", ok};' in pv
      and "RectF a = new RectF(box.left + dp(24)" in pv and 'm.artAlways = S.t("السماح دائماً من هذا الكمبيوتر");' in act)
check("6. the setup page asks the screensaver's language before its configuration; an app already on the TV is not counted «chosen»; no «التفاصيل فوق» anywhere",
      act.index("S.refresh(b);") < act.index("super.attachBaseContext(S.wrap(b));") and "public static void refresh(Context c)" in text(HOME / "S.java")
      and "chosenCount(picks, haveIds)" in act and "if (!have.contains(id)) n++;" in act
      and not [p.name for p in SETUP.glob("*.java") if "التفاصيل فوق" in text(p) or "ملاحظات (فوق)" in text(p)] and "static String because(List<Step> steps)" in runner)
check("7. after a Home check the setup page is brought back at once (the TV's settings had stayed over it), in the engine and the report",
      "backToSetup();" in eng and 'SETUP_ACTIVITY = SELF + "/.setup.SetupActivity"' in eng and "am start --activity-reorder-to-front -n " in rep)
hk = svc[svc.index("static boolean homeKey(Context c)"):svc.index("/** 4.4: the setup's word")]
check("8. R3 (the Home key caught by the add-on) is a switch that is OFF by default, set only through the home's adb-only receiver; fixed-word logs",
      'getBoolean(ExtrasWords.HOME_KEY, false)' in hk and "if (e.getKeyCode() == KeyEvent.KEYCODE_HOME && homeKey(this)) return homeKeyCaught(e);" in svc
      and "|| homeKey(this);" in svc and 'case "sa.urtv.home.HOME_KEY":' in text(HOME / "DockReceiver.java")
      and 'android:name="sa.urtv.home.HOME_KEY"' in text(ROOT / "android/launcher/src/main/AndroidManifest.xml") and "HOME_KEY --es mode on" in text(ROOT / "docs/HOMEDOCTOR.md"))
sys.exit(1 if failed else 0)
