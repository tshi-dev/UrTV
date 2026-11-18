#!/usr/bin/env python3
"""The release gate stays wired (docs/dev/RELEASE.md): android.yml publishes to apk-latest only after checks AND the regressions gate pass;
promote-beta.yml copies to apk-beta only after the gate passed on the commit the source build was made from; the gate runs the
«regressions» group and the API surface diff, which fails on a new UI-thread path to a cross-process call.
Run: python3 tests/regressions/release_gate_ci_test.py"""
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
W = ROOT / ".github/workflows"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

try:
    import yaml
except ImportError:
    if os.environ.get("CI"):   # 4.4 review E-F1: in CI a missing module is a failure, never a pass
        print("FAIL no yaml module in CI (pip install pyyaml in checks.yml / regressions.yml)")
        sys.exit(1)
    print("SKIP no yaml module")
    sys.exit(0)
def load(n): return yaml.safe_load((W / n).read_text(encoding="utf-8"))
a, p, g = load("android.yml"), load("promote-beta.yml"), load("regressions.yml")
jobs = a["jobs"]
check("android.yml has the gate job, from regressions.yml, on master", jobs.get("regressions", {}).get("uses") == "./.github/workflows/regressions.yml"
      and "master" in jobs["regressions"].get("if", ""))
check("…as a release: the version's review must have no blocker or high open", jobs["regressions"].get("with", {}).get("release") is True
      and "review_gate.py" in " ".join(str(s.get("run", "")) for s in g["jobs"]["regressions"]["steps"]))
needs = jobs["apk"].get("needs")
check("the APK job (every publish step) waits for checks and the gate", isinstance(needs, list) and {"checks", "regressions"} <= set(needs), needs)
cond = jobs["apk"].get("if", "")
check("…and does not run when the gate failed (only success or skipped on next)", "needs.regressions.result == 'success'" in cond and "failure" not in cond, cond)
pj = p["jobs"]
check("promote-beta: gate from regressions.yml on the source build's commit, promote needs it",
      pj.get("gate", {}).get("uses") == "./.github/workflows/regressions.yml" and "needs.source.outputs.sha" in str(pj["gate"].get("with", {}))
      and pj.get("promote", {}).get("needs") == "gate")
steps = " ".join(str(s.get("run", "")) for s in g["jobs"]["regressions"]["steps"])
check("the gate runs the regressions group and the API surface diff", "tests/run.sh regressions" in steps and ".github/tools/api_surface_diff.py" in steps)
on = g.get("on") or g.get(True)
check("the gate can be called (workflow_call) and run by hand", "workflow_call" in on and "workflow_dispatch" in on)
check("it fetches the tags (the diff needs the previous release)", any(s.get("with", {}).get("fetch-depth") == 0 for s in g["jobs"]["regressions"]["steps"]))
sys.exit(1 if failed else 0)
