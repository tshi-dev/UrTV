#!/usr/bin/env python3
"""The «regressions» group: the gate's own tests (tests/regressions/*_test.py) and every guard docs/dev/REGRESSIONS.md names, each once.
A .py runs with python3, a .mjs with node (Playwright, as tests/run.sh), a .sh with bash. Prints one line per file and a summary;
exits 1 when any failed.
Run: bash tests/run.sh regressions       (the same as: python3 tests/regressions/run.py)
     python3 tests/regressions/run.py --list    (the files only)
     python3 tests/regressions/run.py --no-browser   (skips the .mjs: for a quick local pass; CI never uses it)
     python3 tests/regressions/run.py --rest   (what the full tests/run.sh does not run by name itself: the gate's tests and any
                                               guard missing from it; the full run calls this, so nothing runs twice)"""
import re, subprocess, sys, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / "docs/dev/REGRESSIONS.md"
GUARD = re.compile(r"tests/[\w./-]+?\.(?:py|mjs|sh)\b")


def rows():
    """[(id, cells)] of the register's table rows"""
    out = []
    for ln in DOC.read_text(encoding="utf-8").splitlines():
        m = re.match(r"\|\s*(R-\d{3,})\s*\|", ln)
        if m:
            out.append((m.group(1), [c.strip() for c in ln.strip().strip("|").split("|")]))
    return out


def guards(cells):
    return sorted(set(GUARD.findall(cells[4]))) if len(cells) > 4 else []


def files():
    own = sorted(str(p.relative_to(ROOT)) for p in (ROOT / "tests/regressions").glob("*_test.py"))
    named = sorted({g for _, c in rows() for g in guards(c)} - set(own))
    return own + named


def main():
    fs = files()
    if "--rest" in sys.argv:
        sh = (ROOT / "tests/run.sh").read_text(encoding="utf-8")
        fs = [f for f in fs if f.startswith("tests/regressions/") or f not in sh]
    if "--list" in sys.argv:
        print("\n".join(fs))
        return 0
    skip_mjs = "--no-browser" in sys.argv
    bad, t0 = [], time.time()
    for f in fs:
        if f.endswith(".mjs") and skip_mjs:
            print(f"SKIP {f} (--no-browser)")
            continue
        cmd = {"py": ["python3"], "mjs": ["node"], "sh": ["bash"]}[f.rsplit(".", 1)[1]] + [f]
        t = time.time()
        r = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
        ok = r.returncode == 0
        print(f"{'PASS' if ok else 'FAIL'} {f} ({time.time() - t:.0f} s)")
        if not ok:
            bad.append(f)
            tail = (r.stdout + r.stderr).strip().splitlines()
            print("\n".join("     " + x for x in [x for x in tail if x.startswith("FAIL")][:15] or tail[-15:]))
    print(f"regressions: {len(fs) - len(bad)}/{len(fs)} files passed in {time.time() - t0:.0f} s" + (f"; failed: {' '.join(bad)}" if bad else ""))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
