#!/usr/bin/env python3
"""4.4 setup speed: run.sh starts `install.sh prefetch` in the background while the first steps run, so «screensaver» and «launcher» find our APKs
downloaded; push_install takes a finished one of the same build tag (and this run's: under 2 hours) instead of downloading it again, and every
install says how long its download and install took («timing:» lines, for the reports). A fake curl; no TV needed.
Run: python3 tests/prefetch_test.py"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

def tree():
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize").mkdir(parents=True)
    for f in ("install.sh", "home-journal.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (d / "bin").mkdir()
    (d / "bin" / "curl").write_text('#!/bin/sh\necho "curl $*" >> "$T/calls"\nwhile [ $# -gt 0 ]; do [ "$1" = -o ] && { echo apk > "$2"; }; shift; done\nexit 0\n')
    (d / "bin" / "curl").chmod(0o755)
    return d, tr

d, tr = tree()
env = dict(os.environ, T=str(d), PATH=f"{d / 'bin'}:{os.environ['PATH']}", PY="/bin/false")
r = subprocess.run(["bash", str(tr / "optimize/install.sh"), "prefetch"], env=env, capture_output=True, text=True, cwd=tr)
pf = tr / "optimize/.prefetch"
check("prefetch: both APKs downloaded with their build tag, nothing asked of the TV (PY never runs)", r.returncode == 0 and (pf / "urtv-screensaver.apk").exists()
      and (pf / "urtv-home.apk").exists() and "apk-latest" in (pf / "urtv-home.apk.tag").read_text() and "timing: prefetch urtv-home.apk" in r.stdout, (r.returncode, r.stdout, r.stderr))
calls = (d / "calls").read_text()
r = subprocess.run(["bash", str(tr / "optimize/install.sh"), "prefetch"], env=env, capture_output=True, text=True, cwd=tr)
check("prefetch again: what is there is not downloaded twice", (d / "calls").read_text() == calls)

src = (tr / "optimize/install.sh").read_text(encoding="utf-8")
fa = src[src.index("fetch_apk() {"):src.index("\n}\n", src.index("fetch_apk() {")) + 3]
cg = src[src.index("curl_get() {"):src.index("\n}\n", src.index("curl_get() {")) + 3]
def fetch(tag="https://github.com/tshi-dev/UrTV/releases/download/apk-latest", old=False):
    if old:
        os.utime(pf / "urtv-home.apk", (1, 1))
    (pf / "urtv-home.apk.tag").write_text(tag + "\n") if (pf / "urtv-home.apk").exists() else None
    r = subprocess.run(["bash", "-c", f'cd "{tr}"; REL=https://github.com/tshi-dev/UrTV/releases/download/apk-latest; PREFETCH=optimize/.prefetch\n{cg}{fa}fetch_apk urtv-home.apk'],
                       env=env, capture_output=True, text=True)
    return r.stdout
before = (d / "calls").read_text().count("\n")
o = fetch()
check("push_install's fetch takes the prefetched file (no download)", "(prefetched)" in o and (tr / "optimize/urtv-home.apk").exists() and (d / "calls").read_text().count("\n") == before, o)
(pf / "urtv-home.apk").write_text("apk")
o = fetch(tag="https://github.com/tshi-dev/UrTV/releases/download/v4.4.1")
check("another build tag: downloaded as before", "(prefetched)" not in o and "timing: download urtv-home.apk" in o, o)
(pf / "urtv-home.apk").write_text("apk")
o = fetch(old=True)
check("an old prefetch (not this run's: apk-latest moves): downloaded as before", "(prefetched)" not in o, o)
run = (ROOT / "optimize/run.sh").read_text(encoding="utf-8")
check("run.sh starts the prefetch in the background while «screensaver» or «launcher» is to do", 'optimize/install.sh prefetch > "$RUN/prefetch.log" 2>&1 &' in run
      and run.index("install.sh prefetch") < run.index('for s in "${STEPS[@]}"; do\n  [ -f "$RUN/.done-$s" ] && continue'))
real = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
check("timing lines: download and install, ours and the extras", real.count('echo "timing: ') >= 6 and 'if ! fetch_apk "$1"; then' in real)
check("the prefetch folder is not committed", "optimize/.prefetch/" in (ROOT / ".gitignore").read_text())
sys.path.insert(0, str(ROOT / "optimize"))
import share
d2 = Path(tempfile.mkdtemp()); share.RUN = d2
(d2 / "run.log").write_text("timing: download urtv-home.apk 0 s (prefetched)\ntiming: install urtv-home.apk 42 s in all\nThe TV stopped answering during this step: waiting for it\n")
t = share.trouble()
check("the shared report says the install times and the steps run again by themselves", "Install times: download urtv-home.apk 0 s (prefetched); install urtv-home.apk 42 s in all" in t
      and "1 steps run again after the TV came back" in t[0], t)
sys.exit(1 if failed else 0)
