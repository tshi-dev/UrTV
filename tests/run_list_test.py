#!/usr/bin/env python3
"""CI runs tests/run.sh only, so a test file it does not name is never run (4.4: 30 such files were found, all passing, none run). Every
tests/*_test.py and tests/*.test.mjs is named in tests/run.sh, or in SLOW below (run only with URTV_SLOW_TESTS=1, each with its reason).
Run: python3 tests/run_list_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SLOW = {}   # file name -> why it is slow (none today: every test runs in CI)
run = (ROOT / "tests/run.sh").read_text(encoding="utf-8")
named = set(re.findall(r"(?:^|\s)tests/([\w.-]+\.(?:py|mjs))\b", run, re.M))
files = sorted(p.name for p in (ROOT / "tests").iterdir() if p.name.endswith("_test.py") or p.name.endswith(".test.mjs"))
missing = [f for f in files if f not in named and f not in SLOW]
gone = sorted(f for f in named if (f.endswith("_test.py") or f.endswith(".test.mjs")) and not (ROOT / "tests" / f).exists())
print(("PASS" if not missing else "FAIL") + " every test file is run by tests/run.sh (or listed as slow)" + ("" if not missing else "  -> " + ", ".join(missing)))
print(("PASS" if not gone else "FAIL") + " tests/run.sh names no test file that is gone" + ("" if not gone else "  -> " + ", ".join(gone)))
sys.exit(1 if missing or gone else 0)
