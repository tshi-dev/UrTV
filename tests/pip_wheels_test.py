"""optimize/setup.sh never makes pip build cryptography from source: a pin with wheels for every computer, and --prefer-binary (R-372).

On Windows ARM64, 32-bit Windows and Intel Macs, pip found no wheel of cryptography 50.0.2 (it ships only win_amd64 and
macOS 11 arm64) and tried to build it, which fails without Rust and a compiler. 46.0.3 is the newest with wheels for all of
them (win_arm64, win32, win_amd64, macosx_10_9_universal2, manylinux, musllinux); every pip install in setup.sh prefers a
wheel over a newer sdist. No network (--online also asks PyPI that the pinned files are there).

    python3 tests/pip_wheels_test.py [--online]
"""
import json, re, sys, urllib.request
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

# the cryptography versions checked to carry a wheel for each computer the setup runs on (PyPI's file lists, Nov 2026)
WHEELS = {"46.0.0", "46.0.1", "46.0.2", "46.0.3"}
NEEDED = ("win_arm64", "win32", "win_amd64", "macosx_10_9_universal2", "manylinux", "musllinux_1_2_aarch64")
req = (ROOT / "optimize/requirements.txt").read_text(encoding="utf-8")
ver = re.search(r"^cryptography==(\S+)", req, re.M).group(1)
check(f"cryptography {ver} has wheels for Windows ARM64, 32-bit Windows and older Macs", ver in WHEELS, ver)
setup = (ROOT / "optimize/setup.sh").read_text(encoding="utf-8")
pips = re.findall(r"-m pip install[^\n]*", setup)
check("every pip install in setup.sh prefers a wheel (--prefer-binary)", pips and all("--prefer-binary" in p for p in pips), pips)
if "--online" in sys.argv:
    files = [u["filename"] for u in json.load(urllib.request.urlopen(f"https://pypi.org/pypi/cryptography/{ver}/json", timeout=30))["urls"]]
    missing = [n for n in NEEDED if not any(n in f for f in files)]
    check(f"PyPI has the wheels of cryptography {ver}", not missing, missing)
sys.exit(1 if bad else 0)
