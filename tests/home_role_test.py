#!/usr/bin/env python3
"""4.5 (the owner's «with a protective way»): where `cmd role add-role-holder` alone does not make our home the HOME role holder (Android 10+), the stock holder is
taken off the role first, only under six guards (HomeRole.java): 1. the holder written OUTSIDE the app's data and read back first; 2. the safety switch armed
first; 3. nothing disabled or uninstalled on this path; 4. a failed add or Home press puts the holder back at once, said; 5. every restore (the safety switch,
«تراجع», «رجّع واجهة المصنع», the rescue app) gives the role back to the recorded holder; 6. never the setup wizard or Settings. The brand list of stock homes
is data only (tvOS Launcher, Apache-2.0). Source pins and the engine on SetupEngineRun's fake TV (tests/HomeRoleRun.java). Run: python3 tests/home_role_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def text(p): return Path(p).read_text(encoding="utf-8")

eng = text(SETUP / "SetupEngine.java")
hr = text(SETUP / "HomeRole.java")
sw = eng[eng.index("private Result homeSwitch()"):eng.index("private Result protectedRole(")]
pr = eng[eng.index("private Result protectedRole("):eng.index("private Result putRoleBack(")]
pb = eng[eng.index("private Result putRoleBack("):]
pb = pb[:pb.index("\n    }\n")]
check("the protected way is taken only after add alone failed (Android 10+), on the keep-the-stock-home switch",
      "if (KEEP_STOCK_ON && HomeRole.addAloneFailed(sdkNow(), role.code, roleHolder(), SELF)) return protectedRole(" in sw
      and sw.index('sh.run("cmd role add-role-holder --user 0 android.app.role.HOME " + SELF);') < sw.index("HomeRole.addAloneFailed(")
      and "static final int MIN_SDK = 29;" in hr and "return sdk >= MIN_SDK && (addCode != 0 || !self.equals(holderAfter));" in hr)
check("rule 2: the safety switch is armed (and its deadline on disk) before the switch reaches the protected way", sw.index("deadman.arm(SAFETY_SECONDS)") < sw.index("protectedRole(stock"))
check("rule 1: the holder is written outside the app's data (/data/local/tmp) and read back BEFORE it is taken off; a record that does not read back: nothing taken off",
      'static final String JOURNAL = "/data/local/tmp/urtv-home-role.txt";' in hr and pr.index("sh.run(HomeRole.record(prev));") < pr.index("HomeRole.parse(text(HomeRole.READ))") < pr.index("sh.run(HomeRole.remove(prev));")
      and "return abort(stock, stockPkg, holder, HomeRole.NOT_RECORDED, false);" in pr)
check("rule 1: only a package name is ever written (record() refuses anything else)", 'if (!pkg(holder)) throw new IllegalArgumentException' in hr)
check("rule 3: nothing on this path disables, hides or uninstalls (the stock home stays installed and on)",
      not re.search(r"pm (disable|hide|uninstall)|cmd package uninstall", pr + pb))
check("rule 4: a failed add, or a Home press that does not open ours, gives the role back at once (and the stock home's activity), and says so",
      "if (add.code != 0 || !SELF.equals(roleHolder())) return putRoleBack(prev, stock, stockPkg, HomeRole.ADD_FAILED);" in pr
      and "if (!homeKeyOpensOurs()) return putRoleBack(prev, stock, stockPkg, HomeRole.VERIFY_FAILED);" in pr
      and pb.index("sh.run(HomeRole.add(prev));") < pb.index("out.line(said);") and 'if (prev.equals(stockPkg)) sh.run("cmd package set-home-activity --user 0 " + q(stock));' in pb)
psb = eng[eng.index("private boolean putStockBack("):]
psb = psb[:psb.index("\n    }\n")]
check("rule 5: the shared restore (the safety switch, «تراجع», «رجّع واجهة المصنع») gives the role to the recorded holder, and lets the record go only once Home is confirmed",
      "String recorded = HomeRole.parse(text(HomeRole.READ));" in psb and "String back = !recorded.isEmpty() ? recorded :" in psb and "if (!recorded.isEmpty()) sh.run(HomeRole.CLEAR); return true;" in psb)
resc = text(ROOT / "android/rescue/src/main/java/sa/urtv/home/setup/RescueActivity.java")
check("rule 5: the rescue app reads the record over its adb and gives the role back to it (RescuePlan), with HomeRole shared into its build",
      "String recorded = adb.run(HomeRole.READ).output;" in resc and "RescuePlan.plan(installed, off, ledger, a11y, recorded)" in resc
      and "'HomeRole.java'" in text(ROOT / "android/rescue/build.gradle"))
check("rule 6: the setup wizard and the TV's Settings are never taken off the role (nor ours)",
      '"com.google.android.tungsten.setupwraith", "com.android.tv.settings", "com.android.settings"' in hr and "if (!HomeRole.removable(prev)) return abort(" in pr)
check("the brand list of stock homes is data only, credited to tvOS Launcher (Apache-2.0)", "tvOS Launcher" in hr and "Apache-2.0" in hr and "static final String[][] STOCK_HOMES" in hr)

if not shutil.which("javac"):
    print("SKIP the home role scenarios: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in text(p)] + [HOME / "phone/UpdateRules.java", HOME / "SuggestedApps.java", HOME / "ExtrasWords.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/HomeRoleRun.java")], capture_output=True, text=True)
    check("the engine, the fake TV and the runner compile", r.returncode == 0, r.stderr[-1500:])
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.HomeRoleRun"], capture_output=True, text=True)
    got = {l.split(" ", 1)[0]: l for l in out.stdout.splitlines()}
L = "com.google.android.apps.tv.launcherx"
check("brandOf(): Google, TCL, Philips by their stock home; an app is not a stock home", got.get("BRAND") == "BRAND google tcl philips true true", got.get("BRAND"))
check("removable(): a stock home yes; the setup wizard, Settings, ours, a bad name, nothing: no", got.get("REMOVABLE") == "REMOVABLE true false false false false false", got.get("REMOVABLE"))
check("record() writes a package name only, read back by parse(); a record naming the wizard or junk reads as nothing",
      got.get("RECORD") == f"RECORD true rejects=true parse={L} wizard=[] junk=[]", got.get("RECORD"))
check("addAloneFailed(): Android 10+ and ours not the holder; Android 9 never", got.get("ALONE") == "ALONE true false false true", got.get("ALONE"))
check("H1 add alone does not take: recorded, then taken off, ours added, Home opens ours; the safety switch armed then let go; nothing disabled, the stock home on",
      got.get("H1") == f"H1 DONE role=sa.urtv.home file={L} order=true armed=1 disarmed=true disables=0 uninstalls=0 stockOn=true said=true", (got.get("H1"), out.stderr[-800:]))
check("H1u «تراجع» gives the role back to the recorded holder and lets the record go", got.get("H1u") == f"H1u DONE role={L} file=null", got.get("H1u"))
check("H2 the record cannot be written: nothing taken off", got.get("H2") == f"H2 FAILED removes=0 role={L} disables=0 uninstalls=0", got.get("H2"))
check("H3 ours refused after the take-off: the holder back at once, said, the record let go, the safety switch let go",
      got.get("H3") == f"H3 FAILED role={L} file=null said=true disarmed=true disables=0 uninstalls=0", got.get("H3"))
check("H4 the Home press does not open ours: the holder back at once, said", got.get("H4") == f"H4 FAILED role={L} file=null said=true disables=0 uninstalls=0", got.get("H4"))
check("H5 the TV ignores the take-off: the holder keeps the role, said", got.get("H5") == f"H5 FAILED role={L} said=true", got.get("H5"))
check("H6 the holder is the setup wizard: never taken off, nothing recorded", got.get("H6") == "H6 FAILED removes=0 records=0", got.get("H6"))
check("H7 Android 9 (no role): the protected way never runs", got.get("H7") == "H7 removes=0 records=0", got.get("H7"))
check("H8 add alone works (most TVs): nothing recorded or taken off", got.get("H8") == "H8 removes=0 records=0", got.get("H8"))
check("the rescue: the recorded holder gets the role (another maker's home, or Google's), then the record is let go; none recorded: untouched",
      got.get("RESCUE") == f"RESCUE com.tcl.cyberui true clear=true google={L} clear=true none=true", got.get("RESCUE"))
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
