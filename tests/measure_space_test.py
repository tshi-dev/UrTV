"""optimize/measure.sh with URTV_RUN_DIR an absolute path with a space in it (macOS «Application Support»): the summary is written inside
that folder, a repeated reading replaces its row, and nothing lands beside it. Also: no unquoted $RUN in any optimize/*.sh. Fake TV.

    python3 tests/measure_space_test.py
"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

STUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --transport) echo wireless ;;
  --batch) cat > /dev/null; echo "== memory" ;;
  "settings get system screen_off_timeout") echo 60000 ;;
esac
exit 0
'''
d = Path(tempfile.mkdtemp(prefix="urtv space "))
tr = d / "tree"; (tr / "optimize").mkdir(parents=True)
for f in ("measure.sh", "settle.sh"):
    shutil.copy(ROOT / "optimize" / f, tr / "optimize")
(tr / "optimize/power.sh").write_text("#!/usr/bin/env bash\nexit 0\n"); (tr / "optimize/power.sh").chmod(0o755)
(d / "py").write_text(STUB); (d / "py").chmod(0o755)
(d / "bin").mkdir(); (d / "bin" / "sleep").write_text("#!/bin/sh\nexit 0\n"); (d / "bin" / "sleep").chmod(0o755)
run_dir = d / "Library" / "Application Support" / "UrTV run"
env = dict(os.environ, PY=str(d / "py"), PATH=f"{d / 'bin'}:{os.environ['PATH']}", URTV_RUN_DIR=str(run_dir), PACKAGE_SETTLE_S="0")
results = [subprocess.run(["bash", str(tr / "optimize/measure.sh"), "before"], env=env, capture_output=True, text=True, cwd=tr) for _ in range(2)]
summary = run_dir / "summary.md"
check("measure.sh runs with a run folder that has spaces in its path", all(r.returncode == 0 for r in results),
      [(r.returncode, r.stderr[-300:]) for r in results])
check("the summary is written inside that folder", summary.is_file(), sorted(p.name for p in run_dir.iterdir()) if run_dir.exists() else "no folder")
rows = summary.read_text().split("\n") if summary.is_file() else []
check("a repeated reading replaces its row (one «before» row, one header)", sum(1 for l in rows if l.startswith("| before |")) == 1
      and sum(1 for l in rows if l.startswith("| state |")) == 1, rows)
check("no tmp file is left", not (run_dir / "summary.md.tmp").exists())
stray = [p.name for p in (d / "Library").iterdir() if p.name != "Application Support"] + [p.name for p in tr.iterdir() if p.name != "optimize"]
check("nothing lands beside the folder (a split path would write «/…/Application» or «Support/…»)", not stray and not (tr / "Support").exists(), stray)

def unquoted_run(code):
    code = re.sub(r'"(?:[^"\\]|\\.)*"', '""', re.sub(r"'[^']*'", "''", code))
    return re.search(r"\$\{?RUN\b", code) and not re.match(r"\s*(local\s+)?\w+=\$\{?RUN\S*\s*(#.*)?$", code)

def parts(line):
    """each $(...) on its own (innermost first: its quotes are its own), then the line with each of them as a plain word"""
    out = []
    while True:
        m = re.search(r"\$\(([^()]*)\)", line)
        if not m:
            return out + [line]
        out.append(m.group(1))
        line = line[:m.start()] + "CMD" + line[m.end():]

bad = []
for f in sorted((ROOT / "optimize").glob("*.sh")):
    for i, l in enumerate(f.read_text(encoding="utf-8").split("\n"), 1):
        if l.lstrip().startswith("#"):
            continue
        if any(unquoted_run(part) for part in parts(l)):
            bad.append(f"{f.name}:{i}: {l.strip()}")
check("no unquoted $RUN in optimize/*.sh", not bad, bad)
check("tests/run.sh runs this test", "tests/measure_space_test.py" in (ROOT / "tests/run.sh").read_text(encoding="utf-8"))
shutil.rmtree(d, ignore_errors=True)
sys.exit(1 if failed else 0)
