#!/usr/bin/env python3
"""R-455 (reports of lag and hangs switching apps on 4.4 beta). The way out to an app waits for the home's onPause, and there the card's last frame
(HomeCard.keepStill, 4.4 beta 3) was drawn into a full-size picture in software, a new one each time: now at half its size (StillSize), into the picture
kept from the last time. A debug build turns StrictMode on (the main thread's disk, network and slow calls logged), and tester/perf/switch.sh measures
the leave, the return and the frames after it (run before the soak on the emulator, and on a TV with the owner's go-ahead); its parser is fed real
outputs here.

    python3 tests/app_switch_test.py
"""
import importlib.util, tempfile
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
ks = card[card.index("    private void keepStill() {"):]
ks = ks[:ks.index("\n    }\n")]
check("the still is drawn at half size (StillSize) with the canvas scaled to it, not a full-size picture",
      "int w = StillSize.side(web.getWidth()), h = StillSize.side(web.getHeight());" in ks and "cv.scale(w / (float) web.getWidth(), h / (float) web.getHeight());" in ks
      and "createBitmap(web.getWidth(), web.getHeight()" not in ks)
check("the picture of the last pause is used again when it fits (no new one each time), cleared first",
      "stillBits != null && !stillBits.isRecycled() && stillBits.getWidth() == w && stillBits.getHeight() == h ? stillBits" in ks and "b.eraseColor(0);" in ks)
check("it still fills the card's box (FIT_XY) and comes before the page is paused (4.4 beta 3)",
      "ScaleType.FIT_XY" in ks and card.index("keepStill();   // 4.4 beta 3") < card.index("web.onPause(); web.pauseTimers();"))
ss = (HOME / "StillSize.java").read_text(encoding="utf-8")
check("StillSize: half, rounded up, never under 1 px", "return Math.max(1, (px + 1) / 2);" in ss)
app = (HOME / "HomeApp.java").read_text(encoding="utf-8")
check("StrictMode only in a debug build, only logged (never a crash or a dialog), after the crash counter",
      "if (BuildConfig.DEBUG) strict();" in app and app.count("penaltyLog()") == 2 and "penaltyDeath" not in app and "penaltyDialog" not in app
      and app.index("CrashLoopStore.hook(this);") < app.index("if (BuildConfig.DEBUG) strict();")
      and "detectDiskReads().detectDiskWrites()" in app and "detectCustomSlowCalls()" in app)

spec = importlib.util.spec_from_file_location("switch", ROOT / "tester/perf/switch.py")
sw = importlib.util.module_from_spec(spec); spec.loader.exec_module(sw)
START = "Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] }\nStatus: ok\nLaunchState: HOT\nActivity: sa.urtv.home/.MainActivity\nTotalTime: {t}\nWaitTime: {w}\nComplete\n"
GFX = ("Applications Graphics Acceleration Info:\n** Graphics info for pid 4242 [sa.urtv.home] **\n\nStats since: 1234ns\nTotal frames rendered: {f}\nJanky frames: {j} ({p}%)\n"
       "50th percentile: 9ms\n90th percentile: {p90}ms\n95th percentile: 30ms\n99th percentile: {p99}ms\nNumber Missed Vsync: 3\n")
with tempfile.TemporaryDirectory() as d:
    for i, (lw, bt, p99) in enumerate([(900, 400, 60), (1200, 520, 80), (700, 380, 50)], 1):
        Path(d, f"leave-{i}.txt").write_text(START.replace("{t}", "650").replace("{w}", str(lw)))
        Path(d, f"back-{i}.txt").write_text(START.replace("{t}", str(bt)).replace("{w}", str(bt + 20)))
        Path(d, f"gfx-{i}.txt").write_text(GFX.format(f=90, j=9, p="10.00", p90=24, p99=p99))
    rows = sw.read(d)
    check("the parser reads each round: the leave's WaitTime, the return's TotalTime, frames, jank and percentiles",
          [(r["leave_ms"], r["back_ms"], r["frames"], r["jank_pct"], r["p99_ms"]) for r in rows] == [(900, 400, 90, 10.0, 60), (1200, 520, 90, 10.0, 80), (700, 380, 90, 10.0, 50)], rows)
    red, out = sw.summary(rows, {})
    check("without limits it only reports the medians (the emulator)", not red and "INFO leave_ms: median 900" in out and "INFO back_ms: median 400" in out, out)
    red, out = sw.summary(rows, {"back_ms": 450, "p99_ms": 70})
    check("with limits a median within them is PASS", not red and "PASS back_ms: median 400 (limit 450)" in out, out)
    red, out = sw.summary(rows, {"leave_ms": 800})
    check("…and one over its limit is RED", red and "FAIL leave_ms: median 900 (limit 800)" in out, out)
    Path(d, "back-4.txt").write_text("Error: Activity not started, unable to resolve Intent\n")
    red, out = sw.summary(sw.read(d), {})
    check("a round the TV did not answer is a «-», not a number", any(l.startswith("4\t") and "\t-\t" in l for l in out), out)
red, out = sw.summary([], {})
check("nothing measured is RED", red, out)
sh = (ROOT / "tester/perf/switch.sh").read_text(encoding="utf-8")
check("switch.sh: the leave, the return and gfxinfo of the 3 s after it per round, then the parser has the last word",
      "am start -W -a android.settings.SETTINGS" in sh and "dumpsys gfxinfo sa.urtv.home reset" in sh and "-c android.intent.category.HOME" in sh
      and 'python3 -I tester/perf/switch.py "$OUT"' in sh and 'exit "${PIPESTATUS[0]}"' in sh)
check("the soak measures it first on the emulator (reported, not judged)", 'bash tester/perf/switch.sh "$OUT/switch" 5' in (ROOT / "tester/soak/soak.sh").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
