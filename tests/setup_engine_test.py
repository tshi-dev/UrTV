"""«إعداد تلفزيوني» (android/launcher/.../setup/SetupEngine.java): the TV sets itself up over its own local ADB. The engine runs on a plain JVM against a
fake TV, and its choices are compared with the shell scripts' own (the same lists, the same ledger, the same journal fields). No Android, no TV needed.

    python3 tests/setup_engine_test.py
"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SETUP = ROOT / "android/launcher/src/main/java/sa/urtv/home/setup"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

# the generated lists are the very lists of optimize/
gen = subprocess.run([sys.executable, str(ROOT / "optimize/gen_setup_lists.py"), "--check"], capture_output=True, text=True)
check("SetupLists.java is what optimize/lists say (python3 optimize/gen_setup_lists.py rewrites it)", gen.returncode == 0, gen.stdout + gen.stderr)

if not shutil.which("javac"):
    print("SKIP setup engine: no javac on this machine")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    # (the Android-facing classes of the folder are compiled by tests/android_compile.sh; here only the pure ones)
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")] + [SETUP.parent / "phone/UpdateRules.java"]   # (read only: its rule for updates)
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java")], capture_output=True, text=True)
    check("the pure classes compile", r.returncode == 0, r.stderr)
    if r.returncode:
        sys.exit(1)
    out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.SetupEngineRun"], capture_output=True, text=True, timeout=300, encoding="utf-8").stdout.splitlines()
    got = {l.split()[0]: l for l in out}

safe = [l.split()[0] for l in (ROOT / "optimize/lists/tcl-safe.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]
keep = [l.split()[0] for l in (ROOT / "optimize/lists/keep.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]
check("A: the whole run on a healthy TV: checks, grants, the safe list, the screensaver and the home switch are all done",
      got.get("A_steps") == "A_steps DONE DONE DONE DONE DONE", got.get("A_steps"))
ledger = (got.get("A_ledger") or "").split(" ", 1)[-1].split(",")
check("A: the ledger holds the safe list minus what the TV had off before (com.tcl.bi), once each", sorted(ledger) == sorted(p for p in safe if p != "com.tcl.bi"), ledger)
check("A: the journal has the fields of optimize/home-journal.sh and says done", all(f'"{k}"' in (got.get("A_journal") or "") for k in ("nonce", "state", "stock", "role_holder_before", "wizard_before", "stock_before", "safety_switch", "written"))
      and '"state": "done"' in got.get("A_journal", "") and '"role_holder_before": "com.google.android.apps.tv.launcherx"' in got.get("A_journal", ""), got.get("A_journal"))
check("A: ours is home, the safety switch was armed once and disarmed, the stock home's helpers are off", got.get("A_home") == "A_home sa.urtv.home sa.urtv.home/.MainActivity armed=1 disarmed=1 helpersOff=true", got.get("A_home"))
check("A: ours is the screensaver", got.get("A_screensaver") == "A_screensaver sa.urtv.screensaver/.ScreenDream", got.get("A_screensaver"))
check("A: the undo gives back the stock home, the role, the screensaver the TV had and empties the ledger",
      got.get("A_undo") == "A_undo DONE com.google.android.apps.tv.launcherx com.google.android.apps.tv.launcherx/.home.HomeActivity com.google.android.apps.tv.dreamx/.service.Dream ledger=[]", got.get("A_undo"))
check("A: after the undo the TV is as it was (only what was already off stays off), and the journal says restored", got.get("A_same") == "A_same true [com.tcl.bi]" and got.get("A_journal_state") == "A_journal_state restored", (got.get("A_same"), got.get("A_journal_state")))
check("A: a second run changes nothing more (the ledger is the same, Home is already ours)", (got.get("A2") or "").startswith("A2 true DONE true"), got.get("A2"))
check("B: the TV does not end its list of disabled packages: nothing turned off, nothing remembered", got.get("B") == "B FAILED disables=0 ledger=null", got.get("B"))
check("C: a TV that answers nothing: no step counts as done, and not one change is sent, no file written, no safety switch armed",
      got.get("C") == "C FAILED FAILED FAILED FAILED FAILED changes=0 files=[] armed=0", got.get("C"))
check("D: Home does not open ours: the stock home and the role are back, the setup app is not left off, the switch is disarmed, the journal says restored",
      got.get("D") == "D FAILED com.google.android.apps.tv.launcherx stockOff=false wizOff=false armed=1 disarmed=1 state=restored", got.get("D"))
check("E: a TV that answers «illegal» to disabling its own home: skipped, put back as it was", got.get("E") == "E SKIPPED com.google.android.apps.tv.launcherx stockOff=false state=restored disarmed=1", got.get("E"))
check("F: WebView too old, unknown, or no screensaver installed: skipped, not one setting written", got.get("F") == "F SKIPPED SKIPPED SKIPPED puts=0", got.get("F"))
check("G: the setup wizard owns the Wi-Fi: left on; another owner: turned off", got.get("G") == "G DONE wizardDisabled=0" and got.get("G2") == "G2 wizardDisabled=1", (got.get("G"), got.get("G2")))
check("H: a switch cut off (journal «switching», Home not ours) is undone; nothing to do when there is no cut-off switch", got.get("H") == "H DONE restored com.google.android.apps.tv.launcherx" and got.get("H2") == "H2 true", (got.get("H"), got.get("H2")))
check("L: no alarm could be set: the home switch does not start (journal «aborted»), not one change on the TV, no deadline left behind", got.get("L") == "L FAILED state=aborted changes=0 deadline=[]", got.get("L"))
check("M: the TV rejects the role or the home activity: the stock home is never disabled, it is put back and CHECKED, then «restored»",
      got.get("M") == "M FAILED stockDisabled=0 com.google.android.apps.tv.launcherx state=restored disarmed=1" and got.get("M2") == "M2 FAILED stockDisabled=0 com.google.android.apps.tv.launcherx", (got.get("M"), got.get("M2")))
check("N: the stock home cannot be confirmed back (stuck on the wizard's recovery screen): NOT «restored», the safety switch stays armed, the deadline stays on disk; a later recovery finishes it",
      got.get("N") == "N FAILED state=switching disarmed=0 deadlineSet=true" and got.get("N2") == "N2 DONE state=restored disarmed=1 deadline=0 com.google.android.apps.tv.launcherx", (got.get("N"), got.get("N2")))
check("O: a journal value is never pasted into a shell command as it is (a holder that is not a package name is replaced by the stock home); a component with $Inner is single-quoted",
      got.get("O") == "O holderOk=true com.google.android.apps.tv.launcherx" and got.get("O2") == "O2 quoted=true", (got.get("O"), got.get("O2")))
check("Q: the permissions are read back (a refused appop is FAILED, said); the exact-alarm mode is given back as it was, and untouched when it was already allowed",
      got.get("Q") == "Q FAILED alreadyAllow=allow/0 ignore->allow->ignore", got.get("Q"))
check("R: Android 12+ without the exact-alarm permission: the screen says the safety switch can be late", got.get("R") == "R late=true", got.get("R"))
check("S: the safety switch retries with a growing wait, 30 s doubling, ten minutes at most", got.get("S") == "S 30 60 240 600 600", got.get("S"))
check("T: putStockBack checks Home once before its first wait (a quick TV costs no sleep)", got.get("T") == "T sleeps=0", got.get("T"))
check("H3: the journal says switching but ours is Home: confirmed done, the alarm cancelled", got.get("H3") == "H3 DONE done disarmed=2", got.get("H3"))
check("I: the app that answers Home is the «before» only when it is the stock home; nothing readable at all stops before any change", got.get("I") == "I com.google.android.apps.tv.launcherx FAILED changes=0 files=[]", got.get("I"))
check("J: the safe list holds nothing from keep.txt or core.txt", got.get("J") == f"J 0 {len(safe)} {len(safe)}" and not set(safe) & set(keep), got.get("J"))
check("K: lists need their end marker; WebView and ledger parsing", got.get("K") == "K true 1 true 120 -1 [a.b, e.f]", got.get("K"))

# the shell's own answer on the same TV: optimize/disable.sh with the same installed and already-off sets writes the same ledger
with tempfile.TemporaryDirectory() as d:
    d = Path(d); tr = d / "tree"; (tr / "optimize").mkdir(parents=True)
    shutil.copytree(ROOT / "optimize/lists", tr / "optimize/lists")
    for f in ("disable.sh", "offlist.py", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    installed = safe + ["com.google.android.apps.tv.launcherx", "com.android.vending", "com.tcl.airplay2", "sa.urtv.home", "sa.urtv.screensaver", "com.google.android.tungsten.setupwraith",
                        "com.google.android.leanbacklauncher.recommendations", "com.tcl.tv.tclhome_passive"]
    (d / "state").write_text("\n".join(["-com.tcl.bi"] + [p for p in installed if p != "com.tcl.bi"]) + "\n")
    (d / "py").write_text('''#!/usr/bin/env python3
import os, sys
a = sys.argv[3:]
st = open(os.environ["STATE"]).read().split()
if a[0] == "--batch":
    for l in sys.stdin:
        w = l.split()
        if w[:3] == ["pm", "disable-user", "--user"] and w[4] in [s.lstrip("-") for s in st]:
            st = [("-" + s.lstrip("-")) if s.lstrip("-") == w[4] else s for s in st]
    open(os.environ["STATE"], "w").write("\\n".join(st) + "\\n")
elif a[0].startswith("pm list packages -d"):
    print("".join("package:%s\\n" % s[1:] for s in st if s.startswith("-")) + "END_OF_LIST")
elif a[0].startswith("pm list packages"):
    print("package:android\\n" + "".join("package:%s\\n" % s.lstrip("-") for s in st) + "END_OF_LIST")
''')
    (d / "py").chmod(0o755)
    import os
    env = dict(os.environ, STATE=str(d / "state"), PY=str(d / "py"), URTV_RUN_DIR=str(tr / "optimize/run"), URTV_HOME=str(d / "home"), PACKAGE_SETTLE_S="0")
    # the shell would run offlist.py with PY; give it a real python
    (d / "pyreal").write_text('#!/bin/bash\nif [ "$2" = optimize/tv.py ]; then exec "' + str(d / "py") + '" "$@"; else exec python3 "$@"; fi\n'); (d / "pyreal").chmod(0o755)
    env["PY"] = str(d / "pyreal")
    r = subprocess.run(["bash", str(tr / "optimize/disable.sh"), "optimize/lists/tcl-safe.txt"], env=env, capture_output=True, text=True, cwd=tr)
    shell_ledger = sorted((tr / "optimize/run/disabled-by-us.txt").read_text().split()) if (tr / "optimize/run/disabled-by-us.txt").exists() else None
    check("PARITY: optimize/disable.sh on the same TV writes the very ledger the app writes", r.returncode == 0 and shell_ledger == sorted(ledger), (r.returncode, r.stderr[-300:], shell_ledger, sorted(ledger)))

# the home journal's field names: the app writes the ones optimize/home-journal.sh reads
hj = (ROOT / "optimize/home-journal.sh").read_text(encoding="utf-8")
sh_fields = re.findall(r'"([a-z_]+)": "', hj[hj.index("jwrite()"):hj.index("jstate()")])[:8]
src_ = (SETUP / "SetupEngine.java").read_text(encoding="utf-8")
app_fields = re.findall(r'\\"([a-z_]+)\\": \\"', src_[src_.index("private void journalWrite"):src_.index("private void journalState")])
check("PARITY: the journal fields of the app are the shell's, in the same order", app_fields == sh_fields == ["nonce", "state", "stock", "role_holder_before", "wizard_before", "stock_before", "safety_switch", "written"], (app_fields, sh_fields))
engine_src = (SETUP / "SetupEngine.java").read_text(encoding="utf-8")
adb_src = (SETUP / "AdbClient.java").read_text(encoding="utf-8")
act_src = (SETUP / "SetupActivity.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the ADB client and the engine are package-private (nothing outside the setup package can run a command)", "\nfinal class AdbClient" in adb_src and "\nfinal class SetupEngine" in engine_src and "public final class AdbClient" not in adb_src, "")
check("the ADB client says the truth about port 5555 (adbd listens on the LAN; a device there can raise the prompt, not get in)", "listens on TCP 5555 on the whole LAN" in adb_src and "cannot get in unless the person accepts" in adb_src, "")
check("the safety switch survives a restart: a BOOT_COMPLETED receiver (system only) re-arms it from the saved deadline and tries at once; the setup screen does the same at its start",
      ".setup.SetupBootReceiver" in manifest and "BOOT_COMPLETED" in manifest[manifest.index(".setup.SetupBootReceiver"):][:500] and "SetupRecovery.rearmFromDisk" in act_src and "SetupRecovery.attempt" in act_src, "")
check("the buttons ask for the family PIN when one is set, and ignore touches through another window", "SetupGate.guard" in act_src and act_src.count("setFilterTouchesWhenObscured(true)") >= 2, "")
runner_src = (SETUP / "SetupRunner.java").read_text(encoding="utf-8")
host_src = (SETUP / "SetupHost.java").read_text(encoding="utf-8")
check("the ADB key is deleted when the job is finished (setup done, or undone) and nothing is pending (the runner's host, after every end)",
      "SetupSession.deleteKeyIfFinished(app)" in host_src and "AdbKeys.delete" in (SETUP / "SetupSession.java").read_text(encoding="utf-8"), "")
main_src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/MainActivity.java").read_text(encoding="utf-8")
hook_src = (SETUP / "SetupHook.java").read_text(encoding="utf-8")
check("the home screen re-arms a cut-off switch at its start with ONE call that never throws (a Force-stop, then Home)", main_src.count("SetupHook.atHomeStart(this)") == 1 and "catch (RuntimeException ignored)" in hook_src and "rearmFromDisk" in hook_src and "SetupRecovery.attempt" in hook_src, "")
fin = runner_src[runner_src.index("private void run(Kind k"):runner_src.index("private End runSteps")]
check("the key is deleted after EVERY end of a run (it refuses by itself while a switch is pending): host.finished() after the try, outside every catch",
      "host.finished()" in fin and fin.index("host.finished()") > fin.rindex("catch (RuntimeException ex)") and "if (finished)" not in fin
      and "SetupRunner.awaitingRestart" in (SETUP / "SetupSession.java").read_text(encoding="utf-8"), "")
check("after the automatic attempts run out the person is told at every start (the home and the setup screen)", "GAVE_UP_NOTICE" in hook_src and "GAVE_UP_NOTICE" in act_src, "")
check("the setup screen is not in the home screen's own app grid (Dock.catalogue skips this package)", "p.equals(c.getPackageName())" in (ROOT / "android/launcher/src/main/java/sa/urtv/home/Dock.java").read_text(encoding="utf-8"), "")
check("the engine runs only fixed commands: none is built from text the TV sent, except package names that matched [A-Za-z0-9_.]+",
      "PKG.matcher" in engine_src and "COMPONENT.matcher" in engine_src, "")
check("the engine's lines name no network, serial or address (it never prints what dumpsys wifi or getprop said)", not re.search(r'out\.line\([^;]*(wifi|ssid|serial)', engine_src, re.I), "")
sys.exit(1 if failed else 0)
