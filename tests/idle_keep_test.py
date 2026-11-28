#!/usr/bin/env python3
"""R-400: some firmware (a Skyworth, Android 11) writes «never» (Integer.MAX) into the TV's idle delay at each boot, so the screensaver the person
chose never started again after a power cycle. The minutes chosen («بدء شاشة التوقف بعد», saver.sh idle N, the phone page) are kept in a file of
their own, and at BOOT_COMPLETED they go back ONLY when the TV says «never» now. The rule runs in tests/IdleRuleRun.java; the wiring is read here.
Run: python3 tests/idle_keep_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return p.read_text(encoding="utf-8")
keep = read(SAVER / "IdleKeep.java") if (SAVER / "IdleKeep.java").exists() else ""
boot, act, recv, rule = read(SAVER / "BootReceiver.java"), read(SAVER / "SettingsActivity.java"), read(SAVER / "SaverReceiver.java"), read(SAVER / "IdleRule.java")
sh = read(ROOT / "optimize/saver.sh")

check("the kept minutes have a file of their own (never the screensaver's settings, R-305)", 'FILE = "idle_keep"' in keep and '"settings"' not in keep)
check("the kept minutes are written with commit() (they must outlive a power cut), on a thread of its own", ".putString(KEY, minutes).commit()" in keep
      and keep.index("new Thread(") < keep.index(".putString(KEY, minutes).commit()"))
check("BootReceiver puts the delay back at a real boot only, off the main thread", "IdleKeep.bootAction(i.getAction())" in boot and "goAsync()" in boot
      and "IdleKeep.atBoot(app)" in boot and "pr.finish()" in boot and "UpdateJob.schedule(c);" in boot)
ba = keep.split("static boolean bootAction(String action) {")[1].split("\n    }\n")[0] if keep else ""
check("not at LOCKED_BOOT (the kept file opens later) nor at an update", "BOOT_COMPLETED" in ba and "LOCKED" not in ba and "REPLACED" not in ba)
at = keep.split("static void atBoot(Context c) {")[1].split("\n    }\n")[0] if keep else ""
check("at boot: the rule first, then our own write where we may, then the home's adb link", "IdleRule.reapplyMs(kept(c), Prefs.idleMs(c), Prefs.IDLE_MINUTES)" in at
      and at.index("reapplyMs") < at.index("canWriteSettings") < at.index("NowPlayingBridge.setIdle("))
logs = [l for l in at.splitlines() if "Log.i(" in l]
check("the boot log has fixed words only (no value joined in)", len(logs) == 3 and not any(" + " in l.split("Log.i(")[1] for l in logs), logs)
st = act.split("private void setIdle(final String minutes) {")[1].split("\n    static final String IDLE_NO_LINK")[0]
check("the row keeps the minutes once they were written (both ways)", st.count("IdleKeep.keep(") == 2
      and st.index("putInt") < st.index("IdleKeep.keep(") and 'if ("ok".equals(r)) { IdleKeep.keep(' in st)
check("SET idle keeps the minutes after the TV took them", recv.index("the TV did not take the change") < recv.index("IdleKeep.keep(c, idle)"))
check("SET idlekept takes only our minutes", 'return "error: idlekept is one of "' in recv and "if (idleKept != null) IdleKeep.keep(c, idleKept);" in recv)
idle = sh.split("\n  idle)\n")[1].split(";;\n  tasks)")[0]
check("saver.sh's shell fallback tells the app the minutes it set", idle.index("screen_off_timeout") < idle.index('send SET "--es idlekept $v"'))
check("the rule has no Android", "import android" not in rule and "static long reapplyMs(String kept, long nowMs, String[] offered)" in rule)

if not shutil.which("javac"):
    print("SKIP idle rules: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(SAVER / "IdleRule.java"), str(ROOT / "tests/IdleKeepRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.IdleKeepRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of IdleKeepRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
