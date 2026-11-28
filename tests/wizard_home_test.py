#!/usr/bin/env python3
"""R-409 (about 40 reports): on some TCL and Xiaomi TVs (Android 11 and 14) the TV's setup wizard keeps the Home key (it owns the saved Wi-Fi, so it is
never turned off), and the add-on's window watch only looks at stock homes. Now the home step, when the wizard keeps Home after the add-on is on, turns
the add-on's Home key catch on by default, says plainly that Home works through it and the «تلفزيونك» tile, and the report has a line for it (never
«turn the wizard off»). tests/WizardHomeRun.java runs the rules. Run: python3 tests/wizard_home_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def text(p): return p.read_text(encoding="utf-8") if p.exists() else ""

runner, host, eng, rep = text(SETUP / "SetupRunner.java"), text(SETUP / "SetupHost.java"), text(SETUP / "SetupEngine.java"), text(SETUP / "SetupReport.java")
home = runner.split('p.add(new Def("home", false, c -> {')[1].split("}));")[0]
check("the home step: after finishWatch, the wizard keeps Home -> the catch on and the plain result", home.index("c.engine.finishWatch(") <
      home.index("c.engine.wizardHoldsHome()") and "r = c.engine.wizardHome(c.host.homeCatchOn());" in home and "on.status == SetupEngine.Status.DONE && r.status != SetupEngine.Status.DONE" in home)
check("the host turns the add-on's Home key catch on", 'Extras.homeKey(app, "on")' in host.split("public boolean homeCatchOn()")[1].split("\n")[0])
check("the engine remembers what the Home key opened, and asks WizardHome", "homeKeyFront = front;" in eng and "WizardHome.holds(homeKeyFront," in eng)
wh = eng.split("Result wizardHome(boolean catchOn) {")[1].split("\n    }\n")[0]
check("the result: DONE with the catch on, the plain words either way, the report's word kept", "store.put(WizardHome.FILE" in wh and "Status.DONE" in wh and "out.line(said);" in wh)
check("the report adds its line after «Home:» and the Google TV line", "WizardHome.reportLine(opens, Boolean.TRUE.equals(homeCatch()))" in rep
      and rep.index('out.add("Home: " + opens);') < rep.index("out.add(googleTvLine(watchBound));") < rep.index("WizardHome.reportLine("))
check("the wizard is never turned off for this (KEEP_STOCK_ON stays)", "static boolean KEEP_STOCK_ON = true;" in eng and "disable" not in wh)

if not shutil.which("javac"):
    print("SKIP WizardHomeRun: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    stub = Path(tmp) / "src/sa/urtv/home/setup/SetupEngine.java"; stub.parent.mkdir(parents=True)
    stub.write_text('package sa.urtv.home.setup;\nfinal class SetupEngine { static final String WRAITH = "com.google.android.tungsten.setupwraith"; }\n')
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(stub), str(SETUP / "WizardHome.java"), str(ROOT / "tests/WizardHomeRun.java")], capture_output=True, text=True)
    check("compiles", r.returncode == 0, r.stderr[-300:])
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.setup.WizardHomeRun"], capture_output=True, text=True).stdout
    print(out.strip())
    check("every case of WizardHomeRun ran", "ALL PASS" in out, out[-200:])
print("failed" if failed else "ok")
raise SystemExit(1 if failed else 0)
