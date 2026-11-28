#!/usr/bin/env python3
"""R-401: the HOME role did not stick on some TVs (ignored, back to the stock home after a reboot, or a Chromecast's setup wizard took it); the home
gave it back once and then said nothing, and no log said who held Home. Now at BOOT_COMPLETED the holder is read again and logged (never an app
the person installed by name), the role is given back once over the kept adb link (no new key offered), and when that is spent or did not hold the
home says so ONCE at its next start, until the role is ours again. The rule runs in tests/RoleBootRun.java; the wiring is read here.
Run: python3 tests/role_boot_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def read(p): return p.read_text(encoding="utf-8") if p.exists() else ""
boot, hook, main = read(HOME / "setup/SetupBootReceiver.java"), read(HOME / "setup/SetupHook.java"), read(HOME / "MainActivity.java")
rule, notice, en = read(HOME / "RoleBootRule.java"), read(HOME / "RoleNotice.java"), read(ROOT / "i18n/en/home.tsv")

check("the boot reads the role again (SetupHook.roleAtBoot)", "SetupHook.roleAtBoot(app);" in boot)
rb = hook.split("public static void roleAtBoot(Context c) {")[1].split("\n    /** R-401: the home showed")[0] if "roleAtBoot(Context c)" in hook else ""
check("the holder is read from RoleManager and the resolver, and logged through holderWord only", "Extras.homeRole(app)" in rb and "resolveActivity(" in rb
      and 'Log.i(TAG, "home role at boot: holder " + sa.urtv.home.RoleBootRule.holderWord(pkg, cls, app.getPackageName())' in rb)
check("no log line joins the raw package", not any("+ pkg" in l for l in rb.splitlines() if "Log.i(" in l))
check("ours again clears the notice", 'if (ours) { if (!notice.isEmpty()) store.put(ROLE_NOTICE, ""); return; }' in rb)
check("the decision is RoleBootRule's, busy = an offer, a pending switch or a running setup", "RoleBootRule.decide(wanted, false, pkg," in rb
      and "stockBackOffered(app) || SetupRecovery.pending(app) || SetupRunner.get().running()" in rb)
check("given back over the kept key only (no approval wait: no new key offered, R-307), off the boot's thread",
      "SetupSession.openAny(app, 0, null," in rb and "s.engine.reapplyHomeRole()" in rb and 'new Thread(' in rb)
check("a give-back that did not hold leaves the one notice", "if (!ok) store.put(ROLE_NOTICE, sa.urtv.home.RoleBootRule.PENDING);" in rb)
check("the home's start reads the waiting notice", "RoleBootRule.PENDING.equals(trim(new FileStore(app).get(ROLE_NOTICE)))) sa.urtv.home.RoleNotice.waiting(app);" in hook)
check("the home shows it after its first frame, never over the adhan, a kick-off or the lite line, and marks it told",
      "if (RoleNotice.pending() != null) AfterFrame.run(getWindow().getDecorView(), this::roleNotice);" in main
      and "NowPlaying.adhanLive() || KickoffOverlay.showing() || LiteAutoStore.pending() != null" in main and "RoleNotice.shown();" in main)
txt = notice.split('TEXT = S.n("')[1].split('");')[0] if 'TEXT = S.n("' in notice else ""
check("the notice is plain Arabic with its English (no «·»)", txt and "·" not in txt and ("\n" + txt + "\t") in en, txt)
check("the rule has no Android", "import android" not in rule)

if not shutil.which("javac"):
    print("SKIP the rule: no javac here"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "RoleBootRule.java"), str(HOME / "StockHome.java"), str(ROOT / "tests/RoleBootRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.RoleBootRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of RoleBootRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
