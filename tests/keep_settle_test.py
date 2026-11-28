#!/usr/bin/env python3
"""Protected features stay on across the setup's restarts (R-380).

keep.sh turned back on what was off before UrTV with `pm enable`, and the next step (measure before) restarted the TV within about
10 s, before Android had written the package state: the enable was lost and the features were off again after the reboot. keep.sh
now marks the change for optimize/settle.sh (measure.sh waits on it before its restart), and run.sh runs keep once more after the
last restart, before the report. A package the TV itself keeps off (a TCL's com.tcl.smartalexa outside Alexa's regions) is said as
the TV's own, not a failure. A fake tv.py; no TV needed.

    python3 tests/keep_settle_test.py
"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

FAKE = r'''#!/usr/bin/env bash
# $PY -I optimize/tv.py <cmd> | --batch: $T/off holds the disabled packages; $T/stuck the ones pm enable cannot turn on
[ "$2" = optimize/tv.py ] || exec python3 "$@"
shift 2
tvdo() { case "$1" in
  "pm list packages -d") sed 's/^/package:/' "$T/off" ;;
  "pm enable "*) p=${1##* }; grep -qx "$p" "$T/stuck" 2> /dev/null || { grep -vx "$p" "$T/off" > "$T/off.n" || true; mv "$T/off.n" "$T/off"; } ;;
esac; }
if [ "$1" = --batch ]; then while read -r l; do [ -n "$l" ] && tvdo "$l"; done; else tvdo "$1"; fi
exit 0
'''
def keep(off, stuck=(), before_off=()):
    t = Path(tempfile.mkdtemp()); tr = t / "tree"; (tr / "optimize/lists").mkdir(parents=True); (tr / "optimize/run").mkdir()
    for f in ("keep.sh", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    shutil.copy(ROOT / "optimize/lists/keep.txt", tr / "optimize/lists")
    (t / "py").write_text(FAKE); (t / "py").chmod(0o755)
    (t / "off").write_text("".join(p + "\n" for p in off)); (t / "stuck").write_text("".join(p + "\n" for p in stuck))
    (tr / "optimize/run/backup.txt").write_text("== disabled packages\n" + "".join(f"package:{p}\n" for p in before_off) + "== end\n")
    r = subprocess.run(["bash", str(tr / "optimize/keep.sh")], cwd=tr, capture_output=True, text=True,
                       env=dict(os.environ, PY=str(t / "py"), T=str(t), URTV_RUN_DIR=str(tr / "optimize/run")))
    return r.returncode, r.stdout + r.stderr, (tr / "optimize/run/.package-change").exists()

kept = [l.split()[0] for l in (ROOT / "optimize/lists/keep.txt").read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]
pkg = next(p for p in kept if p == "com.android.vending") if "com.android.vending" in kept else kept[0]
rc, out, marked = keep([pkg])
check("a protected feature turned back on: the change is marked for settle.sh (the next restart waits for Android to write it)",
      rc == 0 and "all on" in out and marked, (rc, out, marked))
rc, out, marked = keep([])
check("nothing to turn on: nothing marked", rc == 0 and not marked, (rc, out, marked))
if "com.tcl.smartalexa" in kept:
    rc, out, marked = keep(["com.tcl.smartalexa"], stuck=["com.tcl.smartalexa"], before_off=["com.tcl.smartalexa"])
    check("the TV keeps Alexa off itself (off before UrTV, pm enable refused): said as the TV's own, not a failure, no !!",
          rc == 0 and "the TV's own, not a failure" in out and "com.tcl.smartalexa" in out and "!!" not in out, out)

run = (ROOT / "optimize/run.sh").read_text(encoding="utf-8")
steps = re.search(r"^STEPS=\(([^)]*)\)", run, re.M).group(1).split()
check("run.sh runs keep once more after the last restart (after the after-measures), before the report",
      "keep-again" in steps and steps.index("keep-again") > max(steps.index("after"), steps.index("after-saver")) and steps.index("keep-again") == steps.index("report") - 1, steps)
check("the keep-again step runs keep.sh", re.search(r"^\s*keep-again\)\s+optimize/keep\.sh", run, re.M) is not None)
sys.exit(1 if bad else 0)
