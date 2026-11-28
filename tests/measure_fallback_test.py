#!/usr/bin/env python3
"""optimize/measure.sh: the free RAM falls back to /proc/meminfo, and a reading it cannot make says «not measured» with the cause (R-369).

The free-RAM step failed on a TV whose kernel has no MemAvailable (and when the reading's batch stopped), with only
«memory read failed» or a stopped step. Now: MemAvailable, else MemFree + Buffers + Cached, else Android's «Free RAM»,
and /proc/meminfo is read alone when the full reading did not give it. A stand-in tv.py; no TV needed.
Run: python3 tests/measure_fallback_test.py"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    if not ok:
        bad.append(name)

STUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --batch) cat > /dev/null; [ "$BATCH" = fail ] && { echo "!! TV not answering after 3 tries" >&2; exit 1; }; printf '%b' "$BATCH" ;;
  "cat /proc/meminfo") printf '%b' "$MEMINFO" ;;
  "settings get system screen_off_timeout") echo 60000 ;;
esac
exit 0
'''
def measure(batch, meminfo=""):
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize/run").mkdir(parents=True)
    for f in ("measure.sh", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (d / "py").write_text(STUB); (d / "py").chmod(0o755)
    (d / "bin").mkdir(); (d / "bin" / "sleep").write_text("#!/bin/sh\n"); (d / "bin" / "sleep").chmod(0o755)
    env = dict(os.environ, T=str(d), PY=str(d / "py"), PATH=f"{d / 'bin'}:{os.environ['PATH']}", URTV_RUN_DIR=str(tr / "optimize/run"),
               PACKAGE_SETTLE_S="0", BATCH=batch, MEMINFO=meminfo)
    r = subprocess.run(["bash", str(tr / "optimize/measure.sh"), "before", "--quick"], env=env, capture_output=True, text=True, cwd=tr)
    summary = tr / "optimize/run/summary.md"
    return r.returncode, r.stdout + r.stderr, summary.read_text() if summary.exists() else ""

rc, out, summ = measure("== memory\\nMemTotal: 2048000 kB\\nMemAvailable: 1024000 kB\\n")
check("MemAvailable: read as before", rc == 0 and "| before | 1000 |" in summ, out)
rc, out, summ = measure("== memory\\nMemTotal: 2048000 kB\\n", "MemTotal: 2048000 kB\\nMemFree: 204800 kB\\nBuffers: 102400 kB\\nCached: 307200 kB\\nSwapCached: 999999 kB\\n")
check("no MemAvailable (an old kernel): MemFree + Buffers + Cached, said", rc == 0 and "| before | 600 |" in summ and "MemFree+Buffers+Cached" in out, (out, summ))
rc, out, summ = measure("== memory\\nMemTotal: 2048000 kB\\n== android\\n   Free RAM: 512,000K (  100,000K cached pss)\\n")
check("no free line in /proc/meminfo: Android's Free RAM", rc == 0 and "| before | 500 |" in summ, (out, summ))
rc, out, summ = measure("fail", "MemTotal: 2048000 kB\\nMemAvailable: 819200 kB\\n")
check("the reading's batch stopped: /proc/meminfo alone, the free RAM still read, the step ends well", rc == 0 and "| before | 800 |" in summ, (rc, out, summ))
rc, out, summ = measure("fail", "")
check("nothing answered: «not measured» with the cause, the step ends well",
      rc == 0 and "not measured (the TV did not answer the memory read)" in summ, (rc, out, summ))
rc, out, summ = measure("== memory\\nMemTotal: 2048000 kB\\n")
check("no free-memory line anywhere: «not measured» says which lines", "not measured (no free-memory line" in summ, (out, summ))
sys.exit(1 if bad else 0)
