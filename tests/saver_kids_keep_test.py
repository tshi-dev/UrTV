#!/usr/bin/env python3
"""4.4 review F3: the kids-mode copy is replaced only by a real answer of the launcher. kidsAsk gives null when the launcher did not answer (error, no cursor, no row);
KeptAnswer (runs on a JVM, tests/KeptAnswerRun.java) writes nothing for null or an unchanged answer; a changed answer is stored first and then broadcast (as CleanMask)."""
import subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
with tempfile.TemporaryDirectory() as tmp:
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "KeptAnswer.java"), str(ROOT / "tests/KeptAnswerRun.java")], capture_output=True, text=True)
    if r.returncode != 0: check("KeptAnswer compiles", False, r.stderr[-400:])
    else:
        o = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.KeptAnswerRun"], capture_output=True, text=True)
        print(o.stdout, end="")
        failed += o.stdout.count("FAIL ") + (1 if o.returncode not in (0, 1) else 0)
npb = (APP / "NowPlayingBridge.java").read_text(encoding="utf-8")
ask = npb.split("static Boolean kidsAsk(", 1)[1].split("\n    }\n", 1)[0]
check("F3: kidsAsk is a Boolean: null on an exception, no cursor, no column or no row; never false for 'did not answer'", ask.count("return null;") == 2 and "return false" not in ask, ask)
ko = npb.split("static boolean kidsOn(", 1)[1].split("\n    }\n", 1)[0]
check("F3: kidsOn's thread writes only what KeptAnswer.toStore lets through, and broadcasts after writing",
      "KeptAnswer.toStore(ans, sp.getBoolean(\"kids_on\", false))" in ko and "kidsAsk(app)" in ko and ko.index('putBoolean("kids_on"') < ko.index("sendBroadcast") and "Prefs.CHANGED" in ko, ko)
check("F3: kidsOn itself still queries nothing on the calling (UI) thread", "getContentResolver" not in ko)
check("F4: before the launcher has ever answered the copy is «kids» (no kick-off notice for children); a TV with no launcher is told «not kids» on the thread",
      'getBoolean("kids_on", true);' in ko and "launcherHere(app)" in ko and "ans = Boolean.FALSE" in ko and "private static boolean launcherHere(" in npb and "getContentResolver" not in ko)
sys.exit(1 if failed else 0)
