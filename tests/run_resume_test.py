#!/usr/bin/env python3
"""4.4 (reports: 0-4 ADB drops a run; a Skyworth stopped at «screensaver» and «launcher» and waited 12 and 30 minutes to be run again): run.sh's
step loop. A step that stopped ONLY because the TV stopped answering is run again once, after tv.py --wait; anything else stops as before (a !!
that asks for something, the Wi-Fi pause 5, a command that may or may not have run). Cuts the loop out of run.sh with stand-in steps; no TV needed.
Run: python3 tests/run_resume_test.py"""
import subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
src = (ROOT / "optimize/run.sh").read_text(encoding="utf-8")
loop = src[src.index("DROPPED="):src.index('rm -f "$RUN/.step-out"') + len('rm -f "$RUN/.step-out"')]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def run(behaviour, wait_ok=True):
    t = Path(tempfile.mkdtemp())
    script = f'''set -euo pipefail
RUN="{t}"; STEPS=(one two)
stamp() {{ echo "$*" >> "$RUN/steps.log"; }}
title() {{ echo "$1"; }}
tv() {{ echo "tv $*" >> "$RUN/calls"; [ "{'yes' if wait_ok else 'no'}" = yes ]; }}
do_step() {{
  n=$(( $(cat "$RUN/n-$1" 2>/dev/null || echo 0) + 1 )); echo $n > "$RUN/n-$1"
  if [ "$1" = one ]; then {behaviour}; fi
  echo "step $1 ran"
}}
{loop}
echo FINISHED'''
    r = subprocess.run(["bash", "-c", script], capture_output=True, text=True)
    rd = lambda f: (t / f).read_text() if (t / f).exists() else ""
    return r.returncode, r.stdout + r.stderr, rd("calls"), rd("n-one").strip(), rd("steps.log"), (t / ".done-one").exists()

rc, out, calls, n, log, done = run('[ "$n" = 1 ] && {{ echo "!! TV not answering after 6 tries (ConnectionError: x). Check the TV is on"; exit 1; }}'.replace("{{", "{").replace("}}", "}"))
check("a dropped connection: the TV is waited for, the same step once more, and the run goes on", rc == 0 and "tv --wait" in calls and n == "2" and done and "FINISHED" in out
      and "dropped one" in log, (rc, out, calls, n, log))
rc, out, calls, n, log, done = run('[ "$n" = 1 ] && { echo "!! The TV stopped answering while urtv-home.apk was installing (some TVs)"; exit 1; }')
check("an install the TV went silent on: the same", rc == 0 and n == "2" and done, (rc, out))
rc, out, calls, n, log, done = run('{ echo "!! TV not answering after 6 tries (x)"; exit 1; }')
check("dropped twice: stops (once more only), the step not marked done", rc == 1 and n == "2" and not done and "FINISHED" not in out, (rc, n))
rc, out, calls, n, log, done = run('{ echo "!! TV not answering after 6 tries (x)"; exit 1; }', wait_ok=False)
check("the TV never came back: stops after the wait, the step not run again", rc == 1 and n == "1" and not done, (rc, n, calls))
rc, out, calls, n, log, done = run('{ echo "!! Install of x failed: Failure [INSTALL_FAILED_OLDER_SDK]"; exit 1; }')
check("any other !! stops at once, no wait, no second run", rc == 1 and n == "1" and calls == "", (rc, n, calls))
rc, out, calls, n, log, done = run('{ echo "!! The TV stopped answering while it ran this command (x), so it may or may not have been done"; exit 1; }')
check("a command that may or may not have run is never repeated by itself", rc == 1 and n == "1" and calls == "", (rc, n, calls))
rc, out, calls, n, log, done = run('exit 5')
check("the Wi-Fi pause (5) passes through as it was", rc == 5 and n == "1" and calls == "", (rc, n))
rc, out, calls, n, log, done = run('true')
check("a step that worked: once, marked done", rc == 0 and n == "1" and done and calls == "", (rc, n))
sys.exit(1 if failed else 0)
