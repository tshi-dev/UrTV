"""optimize/setup.sh: pip prefers wheels, and only a computer with no wheel of the pinned cryptography falls back to one that has (R-372).

On Windows ARM64, 32-bit Windows and Intel Macs pip found no wheel of cryptography 50.0.2 (it ships only win_amd64 and macOS 11
arm64) and tried to build it, which fails without Rust and a compiler. Every computer keeps requirements.txt's pins; every pip
install prefers a wheel (--prefer-binary); when requirements.txt fails, setup.sh says so in one line and installs
requirements-fallback.txt: the same pins with cryptography 46.0.3, the newest with wheels for those computers (win_arm64,
win32, macosx_10_9_universal2). No network (--online also asks PyPI that the fallback's wheels are there).

    python3 tests/pip_wheels_test.py [--online]
"""
import json, re, subprocess, sys, tempfile, urllib.request
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

FALLBACK = "46.0.3"   # checked on PyPI (Nov 2026): wheels for win_arm64, win32, win_amd64, macosx_10_9_universal2, manylinux, musllinux
def pins(text):
    logical = [l for l in re.sub(r"\\\n\s*", " ", text).splitlines() if l.strip() and not l.lstrip().startswith("#")]
    return {l.split("==")[0]: l.split("==")[1].split()[0] for l in logical}, logical
main, _ = pins((ROOT / "optimize/requirements.txt").read_text(encoding="utf-8"))
fb, fb_lines = pins((ROOT / "optimize/requirements-fallback.txt").read_text(encoding="utf-8"))
check(f"the fallback pins cryptography {FALLBACK}", fb.get("cryptography") == FALLBACK, fb.get("cryptography"))
check("the fallback has the same packages and versions as requirements.txt but cryptography",
      {k: v for k, v in fb.items() if k != "cryptography"} == {k: v for k, v in main.items() if k != "cryptography"} and set(fb) == set(main), (fb, main))
check("every line of the fallback carries sha256 hashes", all(re.search(r"--hash=sha256:[0-9a-f]{64}", l) for l in fb_lines))

setup = (ROOT / "optimize/setup.sh").read_text(encoding="utf-8")
pips = re.findall(r"-m pip install[^\n]*", setup)
check("every pip install in setup.sh prefers a wheel (--prefer-binary)", pips and all("--prefer-binary" in p for p in pips), pips)
m = re.search(r"^installed=yes\n.*?^if \[ \"\$installed\" != yes \]; then\n", setup, re.S | re.M)
check("setup.sh has the fallback block", m is not None)

def run(first_ok, fallback_ok, reqs="requirements.txt"):
    d = Path(tempfile.mkdtemp()); (d / "venv/bin").mkdir(parents=True)
    (d / "requirements-fallback.txt").write_text(f"cryptography=={FALLBACK} \\\n    --hash=sha256:{'0' * 64}\n")
    (d / "venv/bin/python").write_text(f'#!/bin/bash\necho "$*" >> "{d}/log"\ncase "$*" in *requirements-fallback.txt*) exit {0 if fallback_ok else 1} ;; *) exit {0 if first_ok else 1} ;; esac\n')
    (d / "venv/bin/python").chmod(0o755)
    r = subprocess.run(["bash", "-c", f'hashes=--require-hashes; reqs={reqs}\n' + (m.group(0) if m else "if false; then\n") + '  echo FAILED\nfi\n'],
                       cwd=d, capture_output=True, text=True)
    log = (d / "log").read_text() if (d / "log").exists() else ""
    return r.stdout, log
out, log = run(True, True)
check("a computer with a wheel: requirements.txt only, the fallback never asked", "FAILED" not in out and "fallback" not in log and "--prefer-binary" in log, (out, log))
out, log = run(False, True)
check("no wheel: one line that says so, then the fallback with its hashes, and the setup goes on",
      "FAILED" not in out and f"installing the newest that has one, {FALLBACK}" in out and "--require-hashes -r requirements-fallback.txt" in log, (out, log))
out, log = run(False, False)
check("the fallback fails too: the !! of setup.sh as before", "FAILED" in out, (out, log))
out, log = run(False, True, reqs="/tmp/names-only")
check("a phone (the names only): no fallback (its packages come from the system)", "FAILED" in out and "fallback" not in log, (out, log))
if "--online" in sys.argv:
    files = [u["filename"] for u in json.load(urllib.request.urlopen(f"https://pypi.org/pypi/cryptography/{FALLBACK}/json", timeout=30))["urls"]]
    missing = [n for n in ("win_arm64", "win32", "win_amd64", "macosx_10_9_universal2", "manylinux", "musllinux_1_2_aarch64") if not any(n in f for f in files)]
    check(f"PyPI has the wheels of cryptography {FALLBACK}", not missing, missing)
sys.exit(1 if bad else 0)
