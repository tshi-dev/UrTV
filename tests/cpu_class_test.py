#!/usr/bin/env python3
"""4.4 beta 4: on weak TVs our screensaver used more processor and memory than the TV's own. «الوضع الخفيف» on «تلقائي» came on by the memory (under
2000 MB, the low-RAM flag) and two cores or fewer only, so a four-core TV with slow cores stayed on the full look. Now the processor's class counts
too (CpuClass: four cores or fewer whose fastest runs at 1.4 GHz or less, read from the kernel) in both apps' tier (the home's Ram.tier and the
screensaver's Prefs.ramTier, before the person's choice, so «إيقاف» still wins); the screensaver logs its share of a core every 10 minutes; and the
page's lite mode is guarded in a browser (tests/lite_cpu.test.mjs). CpuClass (no Android) on cases, then the wiring.
Run: python3 tests/cpu_class_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

a, b = (HOME / "CpuClass.java").read_text(encoding="utf-8"), (APP / "CpuClass.java").read_text(encoding="utf-8")
norm = lambda s: re.sub(r"package [\w.]+;|\(Ram\.tier\)|\(Prefs\.ramTier\)", "", s)
check("the two apps carry the same CpuClass", norm(a) == norm(b))
ram, pf = (HOME / "Ram.java").read_text(encoding="utf-8"), (APP / "Prefs.java").read_text(encoding="utf-8")
for name, src, low, lite in (("home Ram.tier", ram, "LOW", "t = liteTier(t, sp.getString"), ("screensaver Prefs.ramTier", pf, "RAM_LOW", "t = liteTier(t, s.getString")):
    line = f"if (t != {low} && CpuClass.weak(Runtime.getRuntime().availableProcessors(), CpuClass.maxKhz())) t = {low};"
    check(f"{name}: a slow processor is LOW, before the person's «الوضع الخفيف» (so «إيقاف» still wins) and the tester's switch",
          line in src and src.index(line) < src.index(lite))
check("the add-on (extras) builds with the home's Ram.java, so it copies CpuClass.java too", "'CpuClass.java'" in (ROOT / "android/extras/build.gradle").read_text(encoding="utf-8"))
dream = (APP / "ScreenDream.java").read_text(encoding="utf-8")
check("the screensaver logs its share of one core every 10 minutes while it shows, and stops with it", "CPU_EVERY_MS = 10 * 60_000L" in dream
      and "snapHandler.postDelayed(cpuTick, CPU_EVERY_MS);" in dream and "snapHandler.removeCallbacks(cpuTick);" in dream and "CpuClass.percent(cpu - cpuMs, now - cpuAt)" in dream)
check("tests/run.sh runs the browser guard", "node tests/lite_cpu.test.mjs" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP cpu class: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)
    (t / "f1").write_text("1416000\n"); (t / "f2").write_text("garbage\n"); (t / "f3").write_text("")
    r = subprocess.run(["javac", "-d", str(t / "c"), str(HOME / "CpuClass.java"), str(ROOT / "tests/CpuClassRun.java")], capture_output=True, text=True)
    check("CpuClass compiles alone", r.returncode == 0, r.stderr[-500:])
    if r.returncode == 0:
        CASES = [
            ("4 cores at 1.2 GHz (a weak TV): weak", ["weak", "4", "1200000"], "1"),
            ("4 cores at 1.4 GHz: weak", ["weak", "4", "1400000"], "1"),
            ("4 cores at 1.5 GHz (an S905X): not", ["weak", "4", "1512000"], "0"),
            ("8 cores at 1.3 GHz: not (many cores share the work)", ["weak", "8", "1300000"], "0"),
            ("the clock not known: not (nothing changes)", ["weak", "4", "0"], "0"),
            ("the cores not known: not", ["weak", "0", "1200000"], "0"),
            ("30 s of processor in 10 min: 5 %", ["percent", "30000", "600000"], "5"),
            ("no time passed: not told", ["percent", "10", "0"], "-1"),
            ("the kernel's number is read", ["parse", str(t / "f1")], "1416000"),
            ("a file that is not a number: 0", ["parse", str(t / "f2")], "0"),
            ("an empty file: 0", ["parse", str(t / "f3")], "0"),
        ]
        args = [x for c in CASES for x in c[1]]
        out = subprocess.run(["java", "-cp", str(t / "c"), "sa.urtv.home.CpuClassRun", *args], capture_output=True, text=True).stdout.split()
        for (name, _, want), got in zip(CASES, out + [""] * len(CASES)):
            check(name, got == want, f"{got!r}, wanted {want!r}")
sys.exit(1 if failed else 0)
