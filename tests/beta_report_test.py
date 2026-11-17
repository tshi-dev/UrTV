#!/usr/bin/env python3
"""4.3.3 beta reporting: a report says which channel the TV was on («channel: beta|latest|next», right after «Versions:»), share.py's allowlist keeps exactly
that line and nothing looser, and the TV's copy (Report.withChannel) only ever writes those words. Run: python3 tests/beta_report_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "optimize"))
import share  # noqa: E402

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
for c in ("beta", "latest", "next"):
    check(f"share.py keeps «channel: {c}»", share.restrict(f"channel: {c}\n") == f"channel: {c}\n")
for bad in ("channel: beta; rm", "channel: stable", "channel: beta extra", "channel:beta", "channel: "):
    check(f"share.py drops «{bad}»", share.restrict(bad + "\n").strip() == "", share.restrict(bad + "\n"))
act = (HOME / "ReportActivity.java").read_text(encoding="utf-8")
check("the report screen adds the line from the update channel", "Report.withChannel(lines, sa.urtv.home.phone.UpdateChannel.read(this))" in act)
if not shutil.which("javac"):
    print("SKIP rules: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(ROOT / "tests/i18n_home_stub/S.java"), str(HOME / "Tr.java"), str(HOME / "Report.java"), str(ROOT / "tests/BetaReportRun.java")], check=True, capture_output=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.BetaReportRun"], check=True, capture_output=True).stdout.decode("utf-8").split("\n")
rows = [r.split("|") for r in out if r]
check("beta, latest and next are written as they are, after «Versions:»", [r[2] for r in rows[:3]] == ["channel: beta", "channel: latest", "channel: next"] and rows[0][1].startswith("Versions:"), rows[:3])
check("nothing or any other word is the latest (never the word itself)", rows[3][2] == "channel: latest" and rows[4][2] == "channel: latest" and len(rows[4]) == 5, rows[3:])
check("every line of a beta report passes share.py's allowlist", share.restrict("\n".join(rows[0]) + "\n") == "\n".join(rows[0]) + "\n", rows[0])
sys.exit(1 if failed else 0)
