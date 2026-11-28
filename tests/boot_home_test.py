#!/usr/bin/env python3
"""R-410: after a power cut some Changhong and Skyworth TVs boot into Live TV. «ابدأ بالرئيسية بعد التشغيل»: at BOOT_COMPLETED, a few seconds after the
TV's boot input, our home is brought in front (through the add-on, else started directly); on by default on Changhong and Skyworth only, the person's
`dock.sh boot-home on|off` wins; never while our home is not the person's or a setup runs. The TV's own boot-input setting is documented first in
docs/SETTINGS.md. tests/BootHomeRun.java runs the rule. Run: python3 tests/boot_home_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def text(p): return p.read_text(encoding="utf-8") if p.exists() else ""

boot, bh, dock = text(HOME / "setup/SetupBootReceiver.java"), text(HOME / "BootHome.java"), text(ROOT / "optimize/dock.sh")
check("the boot receiver calls bootHome", "bootHome(app);" in boot)
b = boot.split("private void bootHome(final Context app) {")[1].split("\n    }\n")[0] if "bootHome(final Context app) {" in boot else ""
check("busy is read only (never resumeAtBoot, which counts a resume)", "resumeAtBoot" not in b and "SetupRunner.awaitingRestart(st) || SetupRunner.cutByRestart(st)" in b)
check("our home = the setup's word or Android's default home", "SetupEngine.HOME_WANTED" in b and "SetupHook.oursIsDefault(app)" in b)
check("off: nothing at all (no async, no thread)", b.index("if (!sa.urtv.home.BootHome.on(app)) return;") < b.index("goAsync()"))
check("after the TV's boot input, on a thread, the boot's pending result finished", "Thread.sleep(sa.urtv.home.BootHome.DELAY_MS)" in b and "pending.finish();" in b)
br = bh.split("public static void bring(Context c, boolean oursHome, boolean setupBusy) {")[1].split("\n    }\n")[0]
check("the add-on's Home first (Android 10+ lets no receiver start a screen), else a direct start", br.index("ExtrasWords.GO_HOME") < br.index("c.startActivity("))
check("the log has fixed words only", all(" + " not in l.split("Log.i(")[1] for l in br.splitlines() if "Log.i(" in l))
check("dock.sh boot-home on|off|status, DockReceiver and its manifest", 'send BOOT_HOME "--es mode $2"' in dock and 'case "sa.urtv.home.BOOT_HOME":' in text(HOME / "DockReceiver.java")
      and 'android:name="sa.urtv.home.BOOT_HOME"' in text(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
st = text(ROOT / "docs/SETTINGS.md")
check("docs/SETTINGS.md: the TV's own boot-input setting first, then the command", "**The TV's own setting first" in st and "`optimize/dock.sh boot-home on`" in st
      and st.index("**The TV's own setting first") < st.index("`optimize/dock.sh boot-home on`"))
if not shutil.which("javac"):
    print("SKIP BootHomeRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-d", tmp, str(HOME / "BootHomeRule.java"), str(ROOT / "tests/BootHomeRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BootHomeRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of BootHomeRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
