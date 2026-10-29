"""After optimize/install.sh the report's «Versions:» line says what is on the TV now: the tail of install.sh against a fake
tv.py, no TV needed.

    python3 tests/install_versions_test.py
"""
import os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
src = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
tail = src[src.index("# The report's «Versions:»"):]
failed = 0

def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + detail))
    failed += not ok

def run(report, what="saver", answer="3.124", code=""):
    tmp = Path(tempfile.mkdtemp())
    stub = tmp / "py"
    stub.write_text('#!/usr/bin/env bash\n[ -z "$CODE" ] || echo "    versionCode=$CODE minSdk=21 targetSdk=34"\n[ -n "$ANSWER" ] && echo "    versionName=$ANSWER"\n[ -n "$ANSWER" ] || exit 1\n', encoding="utf-8")
    stub.chmod(0o755)
    if report is not None:
        (tmp / "report.md").write_text(report, encoding="utf-8")
    env = {**os.environ, "PY": str(stub), "URTV_RUN_DIR": str(tmp), "what": what, "ANSWER": answer, "CODE": code}
    r = subprocess.run(["bash", "-c", tail], env=env, capture_output=True, text=True, cwd=ROOT)
    out = (tmp / "report.md").read_text(encoding="utf-8") if report is not None else None
    return r.returncode, out

old = "Home: sa.urtv.home/.MainActivity\nVersions: screensaver 3.89, home 3.89\nExtras: TV Bro no\n"
code, out = run(old)
check("install: the report's old Versions line is replaced", code == 0 and out == old.replace("3.89, home 3.89", "3.124, home 3.124").replace("screensaver 3.89", "screensaver 3.124"), repr(out))
code, out = run("Home: x\n")
check("install: a report with no Versions line gets one", code == 0 and out == "Home: x\nVersions: screensaver 3.124, home 3.124\n", repr(out))
code, out = run(old, answer="")
check("install: the TV does not answer: the report stays as it was", code == 0 and out == old, repr(out))
code, out = run(old, what="extras")
check("install extras: the report is not touched", code == 0 and out == old, repr(out))
code, out = run(old, answer="4.0", code="151")   # the release named 4.0: versionName is «4.0» for every build, the build comes from versionCode
check("install: «4.0 (build 151)»: the name from versionName, the build from versionCode", code == 0 and out == old.replace("screensaver 3.89, home 3.89", "screensaver 4.0 (build 151), home 4.0 (build 151)"), repr(out))
code, out = run(old, answer="4.0")
check("install: a TV that does not say the versionCode gets the name alone", code == 0 and out == old.replace("screensaver 3.89, home 3.89", "screensaver 4.0, home 4.0"), repr(out))
code, out = run(None)
check("install: no report, nothing to do and no error", code == 0)
sys.exit(1 if failed else 0)
