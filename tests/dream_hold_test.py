#!/usr/bin/env python3
"""R-452 (a report: «the screensaver app stopped answering»). The dream asked the launcher on its main thread, in onAttachedToWindow and
onDreamingStarted, whether «إعداد تلفزيوني» holds it (setupWaiting, setupRunning). With the launcher's process not running, or busy, the query waits
for it to start (seconds, up to Android's 10 s), with the dream's window up: a key then is an ANR. Now both are asked on a thread; the dream waits at
most DreamHold.WAIT_MS on its dark ground, and a late «held» still ends it. The UI-thread gate (tests/regressions/ui_thread_ipc_test.py) now walks
the dream's onAttachedToWindow and onDreamingStarted too, so a query there fails it. Needs javac for the JVM part.

    python3 tests/dream_hold_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

dream = (APP / "ScreenDream.java").read_text(encoding="utf-8")
def body(sig):
    b = dream.split(sig)[1]
    return b[:b.index("\n    }\n")]
att, started = body("public void onAttachedToWindow() {"), body("public void onDreamingStarted() {")
def outside_threads(b):
    out, depth = [], 0
    for part in b.split("new Thread(")[0:1]:
        out.append(part)
    rest = b.split("new Thread(")[1:]
    for r in rest:   # what follows the thread's body (its closing «}, "name");»)
        out.append(r.split('}, "', 1)[1] if '}, "' in r else "")
    return "".join(out)
check("onAttachedToWindow asks setupWaiting only inside a thread, never on the main thread",
      "NowPlayingBridge.setupWaiting(app)" in att and "setupWaiting" not in outside_threads(att), att)
check("onDreamingStarted asks setupRunning only inside a thread", "NowPlayingBridge.setupRunning(app)" in started and "setupRunning" not in outside_threads(started), started)
check("the answer comes back to the main thread (snapHandler.post) and is judged by DreamHold, with a wait of DreamHold.WAIT_MS",
      "snapHandler.post(() -> holdAnswer(true, held));" in att and "snapHandler.postDelayed(holdWait, DreamHold.WAIT_MS);" in att
      and "DreamHold.decide(attached, started, answered, held)" in dream)
ans = body("private void holdAnswer(boolean answered, boolean held) {")
check("«held»: the dream ends before anything is shown or counted; the start shows, counts and listens once",
      ans.index("DreamHold.END") < ans.index("finish();") < ans.index("Showing.enter();") < ans.index("show(true);") and "started = true;" in ans)
det = body("public void onDetachedFromWindow() {")
check("the count is left only when it was entered, and the wait is cancelled", "if (entered) Showing.leave();" in det and "snapHandler.removeCallbacks(holdWait);" in det)
check("a setup run still wakes the dream (Android 10+) or ends it", "wakeUp(); else finish();" in started and "if (!attached) return;" in started)
graph = (ROOT / "tests/regressions/javagraph.py").read_text(encoding="utf-8")
check("the UI-thread gate walks the dream's onAttachedToWindow and onDreamingStarted", '"onAttachedToWindow"' in graph and '"onDreamingStarted"' in graph)

if not shutil.which("javac"):
    print("SKIP the JVM part: no javac"); sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "DreamHold.java"), str(ROOT / "tests/DreamHoldRun.java")], capture_output=True, text=True)
    check("DreamHold compiles on a plain JVM", r.returncode == 0, r.stderr)
    if r.returncode == 0:
        r = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.DreamHoldRun"], capture_output=True, text=True)
        print(r.stdout, end="")
        failed += r.returncode != 0 or r.stdout.count("PASS ") != 7
sys.exit(1 if failed else 0)
