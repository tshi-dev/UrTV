"""optimize/setup.sh on a phone or an iPad (Termux, Alpine / iSH): the venv sees the system's packages (cryptography and Pillow cannot be built there),
Pillow never stops the rest, and a computer is unchanged. A stand-in Python; no network, no venv is really made.

    python3 tests/setup_phone_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

STUB = r'''#!/usr/bin/env bash
# a stand-in for python3 and for venv/bin/python: logs what it was asked, makes the venv folder, fails PIL / pip on demand
echo "$*" >> "$T/log"
case "$*" in
  *"-m venv"*) d=${@: -1}; mkdir -p "$d/bin" "$d/lib/python3.12/site-packages"; cp "$0" "$d/bin/python" ;;
  *"import PIL"*) [ "$HAS_PIL" = yes ] || exit 1 ;;
  *"pip install"*pillow*) [ "$PIL_BUILD" = fail ] && exit 1 || exit 0 ;;
  *"pip install"*"-r "*) f=${@: -1}; echo "REQS $(tr '\n' ' ' < "$f")" >> "$T/log"; [ "$PIP_FAILS" = yes ] && exit 1 || exit 0 ;;
esac
exit 0
'''
def run(**env):
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize").mkdir(parents=True)
    for f in ("setup.sh", "python.sh", "requirements.txt"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (d / "bin").mkdir(); (d / "bin" / "python3").write_text(STUB); (d / "bin" / "python3").chmod(0o755)
    for tool in ("bash", "env", "dirname", "cat", "tr", "grep", "sed", "mktemp", "mkdir", "cp", "rm", "uname", "head", "mv", "chmod", "ls", "printf", "sort"):
        p = shutil.which(tool)
        if p: (d / "bin" / tool).symlink_to(p)    # only these: no other Python (and no uv) on the PATH, so the stand-in is the one picked
    e = dict(os.environ, T=str(d), PATH=str(d / "bin"), HAS_PIL="", PIL_BUILD="", PIP_FAILS="", URTV_SYSTEM_SITE="", PREFIX="")
    e.update(env)
    r = subprocess.run(["bash", str(tr / "optimize/setup.sh")], env=e, capture_output=True, text=True, cwd=tr)
    return r, (d / "log").read_text() if (d / "log").exists() else ""

r, log = run()
check("a computer: a plain venv (no system packages), the whole requirements.txt with its hashes (--require-hashes)", "-m venv venv" in log and "--system-site-packages" not in log
      and "--require-hashes -r requirements.txt" in log and all(f"{n}==" in log for n in ("adb-shell", "rsa", "cryptography", "certifi", "pillow")), log)
r, log = run(URTV_SYSTEM_SITE="1")
check("URTV_SYSTEM_SITE=1: the venv sees the system's packages, and Pillow is not in the list pip must satisfy", "--system-site-packages" in log and "REQS adb-shell rsa cryptography certifi" in log.replace("\n", " ")
      and "pillow" not in [l for l in log.splitlines() if l.startswith("REQS")][0].lower()
      and "--require-hashes" not in log and "==" not in [l for l in log.splitlines() if l.startswith("REQS")][0], log)
r, log = run(PREFIX="/data/data/com.termux/files/usr")
check("Termux is recognised by its PREFIX", "--system-site-packages" in log, log)
r, log = run(URTV_SYSTEM_SITE="1", PIL_BUILD="fail")
check("Pillow that cannot be built is a note, not a failure of the setup", "Pillow is not here" in r.stdout and "REQS" in log, r.stdout + r.stderr)
r, log = run(URTV_SYSTEM_SITE="1", PIP_FAILS="yes")
check("a package that cannot be installed on a phone: the message names the system packages (pkg / apk)", r.returncode == 1 and "pkg install python-cryptography" in r.stdout and "apk add py3-cryptography" in r.stdout, r.stdout + r.stderr)
src = (ROOT / "optimize/setup.sh").read_text(encoding="utf-8")
check("Alpine / iSH is recognised by /etc/alpine-release", "/etc/alpine-release" in src)
sys.exit(1 if failed else 0)
