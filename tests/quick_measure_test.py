#!/usr/bin/env python3
"""4.4 setup speed (22 reports: «before» 4:08-4:47 and «after» 4:11-5:05 on every computer run, plus up to 1:33 per screensaver reading): run.sh's
default is one quick reading before and one after (measure.sh --quick: no restart, 45 s on the home screen, kept awake) and the two screensaver readings
only on a weak TV (under 2.5 GB, where ours can be heavy: heavy.py needs both). `optimize/run.sh --measure` (or URTV_MEASURE=1) is the full measure as
before. Fake TV (a stand-in tv.py); no TV needed.
Run: python3 tests/quick_measure_test.py"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

STUB = r'''#!/usr/bin/env bash
shift 2
echo "$1 $2" >> "$T/calls"
case "$1" in
  --transport) echo adb ;;
  --batch) cat > /dev/null; printf '== memory\nMemTotal: 3000000 kB\nMemAvailable: 1500000 kB\n' ;;
  "settings get system screen_off_timeout") echo 60000 ;;
esac
exit 0
'''
def measure(*args):
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize/run").mkdir(parents=True)
    for f in ("measure.sh", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (tr / "optimize/power.sh").write_text("#!/usr/bin/env bash\nexit 0\n"); (tr / "optimize/power.sh").chmod(0o755)
    (d / "py").write_text(STUB); (d / "py").chmod(0o755)
    (d / "bin").mkdir(); (d / "bin" / "sleep").write_text('#!/bin/sh\necho "sleep $1" >> "$T/calls"\n'); (d / "bin" / "sleep").chmod(0o755)
    env = dict(os.environ, T=str(d), PY=str(d / "py"), PATH=f"{d / 'bin'}:{os.environ['PATH']}", URTV_RUN_DIR=str(tr / "optimize/run"), PACKAGE_SETTLE_S="0")
    r = subprocess.run(["bash", str(tr / "optimize/measure.sh"), *args], env=env, capture_output=True, text=True, cwd=tr)
    return r, (d / "calls").read_text() if (d / "calls").exists() else ""

r, calls = measure("before", "--quick")
check("--quick: no restart, never asks the transport, settles 45 s, reads, writes the row", r.returncode == 0 and "--reboot" not in calls and "--transport" not in calls
      and "sleep 45" in calls and "sleep 180" not in calls and "--batch" in calls and "| before | 1464 |" in r.stdout, f"{r.returncode} {r.stdout} {r.stderr} {calls}")
check("--quick says how to get the full measure", "optimize/run.sh --measure" in r.stdout)
r, calls = measure("before")
check("without --quick: the full measure as before (restart, 3 minutes)", "--reboot" in calls and "sleep 180" in calls, calls)

run = (ROOT / "optimize/run.sh").read_text(encoding="utf-8")
check("run.sh takes --measure out of its arguments anywhere (status and again still work), URTV_MEASURE=1 the same",
      'if [ "$a" = --measure ]; then URTV_MEASURE=1; else args+=("$a"); fi' in run and 'MEASURE="${URTV_MEASURE:-0}"' in run)
check("run.sh: before and after quick by default, full with --measure",
      'if [ "$MEASURE" = 1 ]; then optimize/measure.sh before; else optimize/measure.sh before --quick; fi' in run
      and 'if [ "$MEASURE" = 1 ]; then optimize/measure.sh after; else optimize/measure.sh after --quick; fi' in run)
check("run.sh: the screensavers' readings only when saver_measure says so", run.count("if saver_measure; then optimize/measure.sh") == 2)
fn = "saver_measure() {" + run.split("saver_measure() {")[1].split("\n}\n")[0] + "\n}\n"
def saver(measure, memtotal):
    d = Path(tempfile.mkdtemp())
    if memtotal is not None:
        (d / "ram-before.txt").write_text(f"== memory\nMemTotal:        {memtotal} kB\n")
    return subprocess.run(["bash", "-c", f'RUN="{d}"; MEASURE={measure}\n{fn}saver_measure && echo yes || echo no'], capture_output=True, text=True).stdout.strip()
check("a 2 GB TV: both screensaver readings (ours can be heavy there)", saver(0, 2_000_000) == "yes")
check("a 4 GB TV: skipped", saver(0, 4_000_000) == "no")
check("--measure: always", saver(1, 4_000_000) == "yes")
check("a «before» that did not say the memory: measured, as before", saver(0, None) == "yes")
check("the setup prompt says the time and --measure", "## 3. Run it (about 10–20 minutes; with `--measure`" in (ROOT / "docs/SETUP-PROMPT.md").read_text(encoding="utf-8"))
sys.exit(1 if failed else 0)
