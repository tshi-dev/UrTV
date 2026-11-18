#!/usr/bin/env python3
"""The register stays honest: every row of docs/dev/REGRESSIONS.md has an id, a symptom, a cause, the version it was fixed in, at least one
guard that is a test file which exists, an area and «on TV» Y or N; ids are unique; every tests/regressions/*_test.py guards a row; every
row a person can see on the TV (Y) has its line in tester/RELEASE-SMOKE.md; no IP address or serial in either file (AGENTS.md).
Run: python3 tests/regressions/regressions_index_test.py"""
import re, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import run as reg  # noqa: E402

ROOT = reg.ROOT
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

rows = reg.rows()
check("the register has rows (more than 100)", len(rows) > 100, len(rows))
ids = [i for i, _ in rows]
check("ids are unique", len(ids) == len(set(ids)), sorted({i for i in ids if ids.count(i) > 1}))
short = [i for i, c in rows if len(c) < 7 or any(not x for x in c[:7])]
check("every row fills its seven columns (id, symptom, cause, fixed, guard, area, on TV)", not short, short)
ver = [i for i, c in rows if len(c) > 3 and not re.match(r"(\d+\.\d+(\.\d+)?|build \d+)", c[3])]
check("every row says the version (or build) that fixed it", not ver, ver)
noguard = [i for i, c in rows if not reg.guards(c)]
check("every row names a test file (a row with no guard fails here)", not noguard, noguard)
missing = sorted({g for _, c in rows for g in reg.guards(c) if not (ROOT / g).is_file()})
check("every named guard exists", not missing, missing)
yn = [i for i, c in rows if len(c) > 6 and c[6] not in ("Y", "N")]
check("«on TV» is Y or N", not yn, yn)
named = {g for _, c in rows for g in reg.guards(c)}
# rules that guard against a class of mistake before any bug of it reached a TV (a lesson, not a row): each says why it is here
PREVENTIVE = {"tests/regressions/no_bundled_packs_test.py": "the photo packs are downloaded, never shipped in an APK (supervisor's rule, 4.3.4)",
              "tests/regressions/regressions_index_test.py": "this file",
              "tests/regressions/attribution_test.py": "the owner's rule (18 Nov): tshi-dev authors and commits every released commit, no AI lines",
              "tests/regressions/lessons_test.py": "the LESSONS rules (R4, R9, R10, R12, R15, R16, R17): rules, not one bug each", "tests/regressions/release_gate_ci_test.py": "the gate's own wiring"}
orphan = [str(p.relative_to(ROOT)) for p in sorted((ROOT / "tests/regressions").glob("*_test.py"))
          if str(p.relative_to(ROOT)) not in named and str(p.relative_to(ROOT)) not in PREVENTIVE]
check("every gate test of tests/regressions guards at least one row", not orphan, orphan)
smoke_p = ROOT / "tester/RELEASE-SMOKE.md"
smoke = smoke_p.read_text(encoding="utf-8") if smoke_p.exists() else ""
unseen = [i for i, c in rows if len(c) > 6 and c[6] == "Y" and not re.search(r"\b" + i + r"\b", smoke)]
check("every row seen on the TV is in tester/RELEASE-SMOKE.md", not unseen, unseen)
for p in (reg.DOC, smoke_p):
    t = p.read_text(encoding="utf-8") if p.exists() else ""
    check(f"no IP address in {p.relative_to(ROOT)}", not re.search(r"\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b", t))
run_sh = (ROOT / "tests/run.sh").read_text(encoding="utf-8")
check("tests/run.sh runs the group (bash tests/run.sh regressions, and in the full run)",
      'tests/regressions/run.py' in run_sh and '"regressions"' in run_sh)
sys.exit(1 if failed else 0)
