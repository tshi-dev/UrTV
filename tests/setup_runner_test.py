"""SetupRunner (android/launcher/.../setup/SetupRunner.java): the one setup run of the process, used by the TV's own button and later by the phone.
One run at a time, a step model with reasons, a bounded ring of numbered lines («the lines since n»), the TV button's five steps and sentences
unchanged, and a full run that resumes from its step pointer after a restart without running a step that is not safe to repeat. On a plain JVM
against the fake TV of tests/SetupEngineRun.java; no Android, no TV.

    python3 tests/setup_runner_test.py
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

runner = (SETUP / "SetupRunner.java").read_text(encoding="utf-8")
act = (SETUP / "SetupActivity.java").read_text(encoding="utf-8")
boot = (SETUP / "SetupBootReceiver.java").read_text(encoding="utf-8")
session = (SETUP / "SetupSession.java").read_text(encoding="utf-8")
service = (SETUP / "SetupService.java").read_text(encoding="utf-8")
manifest = (ROOT / "android/launcher/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("the runner is plain Java (no Android): the tests run it on a JVM", "import android." not in runner)
check("the setup screen only starts and reads through the runner: no session, no engine step, no thread of its own for a run",
      "SetupRunner.get().start(" in act and "SetupSession.open" not in act and not re.search(r"\.(precheck|grants|debloat|screensaver|home|undo)\(\)", act)
      and '"setup-run"' not in act)
check("the screen keeps the PIN gate and the buttons' guard against overlays", "SetupGate.guard" in act and act.count("setFilterTouchesWhenObscured(true)") >= 2)
check("a run lives in a foreground service (it outlives the screen), not exported", 'android:name=".setup.SetupService" android:exported="false"' in manifest
      and "startForeground(" in service and "startForegroundService" in service)
check("the TV's start resumes a full run only when it restarted the TV on purpose, and the ADB key stays only then",
      "SetupRunner.awaitingRestart(new FileStore(app))" in boot and "SetupRunner.get().resume(" in boot and "SetupRunner.awaitingRestart(new FileStore(c))" in session)
check("the step pointer is written BEFORE a step runs", runner.index('writeState(host.store(), o, d.id, "started")') < runner.index("SetupEngine.Result r = d.body.run(c);"))
check("the home switch is the one step never run again by itself after a cut", '"home", false' in runner and runner.count(", false, c ->") == 2)

if not shutil.which("javac"):
    print("SKIP the runner on a JVM: no javac on this machine")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    pure = [p for p in sorted(SETUP.glob("*.java")) if "import android." not in p.read_text(encoding="utf-8")]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, pure), str(ROOT / "tests/SetupEngineRun.java"), str(ROOT / "tests/SetupRunnerRun.java")], capture_output=True, text=True)
    check("the pure classes and the runner's harness compile", r.returncode == 0, r.stderr)
    if r.returncode:
        sys.exit(1)
    run = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.SetupRunnerRun"], capture_output=True, text=True, timeout=300, encoding="utf-8")
    got = {l.split()[0]: l for l in run.stdout.splitlines() if l.strip()}
    if run.returncode:
        print(run.stderr[-2000:])

FIVE = "precheck:DONE,grants:DONE,safe:DONE,screensaver:DONE,home:DONE"
check("R1: the TV's button runs its five steps in order, all done; the run is over; the key is let go once; the screen is told of changes",
      got.get("R1") == f"R1 STARTED {FIVE} running=false finished=1 bad=false listened=true", got.get("R1"))
check("R1: the end is the setup screen's own sentence", (got.get("R1end") or "").startswith("R1end انتهى الإعداد. اضغط زر الرئيسية في الريموت."), got.get("R1end"))
check("R1: numbered lines, in order, «since n» gives the ones after n; the prompt line comes first; the button's run leaves no resume pointer",
      got.get("R1lines") == "R1lines true true true 0 prompt=true" and got.get("R1nostate") == "R1nostate null", (got.get("R1lines"), got.get("R1nostate")))
check("R2: a second start (undo, or the full run) while one runs is BUSY and is not queued; once it ends a new one starts",
      got.get("R2") == "R2 STARTED BUSY BUSY queued=1 kind=SETUP after=STARTED", got.get("R2"))
check("R3: the ring keeps the newest 400 lines with their numbers", got.get("R3") == "R3 400 51 l50 450 3", got.get("R3"))
check("R4: a TV that answers nothing: the precheck fails, the rest is skipped, not one change, «توقّفنا قبل أي تغيير.» in red",
      got.get("R4") == "R4 precheck:FAILED,grants:SKIPPED,safe:SKIPPED,screensaver:SKIPPED,home:SKIPPED changes=0 bad=true توقّفنا قبل أي تغيير.", got.get("R4"))
check("R5: debugging off / the prompt refused: the screen's old sentences, in red, the key let go, not running", got.get("R5") == "R5 true 1 true true running=false", got.get("R5"))
check("R6: the undo is one step through the same runner, with its old sentence", got.get("R6") == "R6 undo:DONE com.google.android.apps.tv.launcherx true", got.get("R6"))
check("R7: a full run cut in the middle of the home switch: after the restart the switch is NOT run again (said as failed), the pointer is cleared at the end",
      got.get("R7") == "R7 true STARTED precheck:DONE,grants:DONE,safe:DONE,screensaver:DONE,home:FAILED roleAdds=0 disables=0 extras=false cleared=true resumableAfter=false opens=1", got.get("R7"))
check("R8: a full run cut in the middle of the safe list runs that step again (its ledger was written first): the same ledger, done",
      got.get("R8") == f"R8 {FIVE} sameLedger=true", got.get("R8"))
check("R9: a resumed run waits while the home switch is still «switching» (the boot path puts the stock home back first)", (got.get("R9") or "").startswith("R9 waited=true"), got.get("R9"))
check("R10: the TV does not come back after the restart: ten minutes of tries, then said in red; the pointer stays for «ابدأ», nothing resumes by itself",
      got.get("R10") == "R10 opens=40 true true kept=true auto=false", got.get("R10"))
check("R11: a pointer older than a day is not resumed", got.get("R11") == "R11 false false", got.get("R11"))
sys.exit(1 if failed else 0)
