"""Writes optimize/requirements.txt: every package optimize/setup.sh installs, at an exact version, with the sha256 of every file PyPI has
for that version (wheels for every platform and Python, and the sdist), so `pip install -r` runs in --require-hashes mode and a changed or
substituted file on the index is refused. The pins work on Python 3.10-3.13 (optimize/python.sh). To move a pin: change PINS, run

    python3 .github/tools/pin_requirements.py

then python3 tests/requirements_pin_test.py and a fresh optimize/setup.sh on a computer.
"""
import json, sys, urllib.request
from pathlib import Path

# (name, version, environment marker): the direct ones first, then what they need (rsa -> pyasn1, cryptography -> cffi -> pycparser,
# cryptography on 3.10 -> typing-extensions)
PINS = [
    ("adb-shell", "0.4.4", ""),
    ("rsa", "4.9.1", ""),
    ("cryptography", "50.0.2", ""),
    ("certifi", "2026.10.23", ""),
    ("pillow", "12.2.0", ""),
    ("pyasn1", "0.6.5", ""),
    ("cffi", "2.1.1", 'platform_python_implementation != "PyPy"'),
    ("pycparser", "3.11", 'implementation_name != "PyPy"'),
    ("typing-extensions", "4.16.0", 'python_full_version < "3.11"'),
]


def hashes(name, version):
    with urllib.request.urlopen(f"https://pypi.org/pypi/{name}/{version}/json", timeout=30) as r:
        d = json.load(r)
    out = sorted({u["digests"]["sha256"] for u in d["urls"]})
    if not out:
        sys.exit(f"!! {name} {version}: PyPI lists no files")
    return out


def main():
    lines = ["# Written by .github/tools/pin_requirements.py: exact versions with every file's sha256 (pip's --require-hashes mode).",
             "# Do not edit by hand. A phone or an iPad (URTV_SYSTEM_SITE, Termux, Alpine / iSH) installs the names only: see optimize/setup.sh."]
    for name, version, marker in PINS:
        hs = hashes(name, version)
        lines.append(f"{name}=={version}" + (f" ; {marker}" if marker else "") + " \\")
        lines += [f"    --hash=sha256:{h}" + (" \\" if i + 1 < len(hs) else "") for i, h in enumerate(hs)]
    Path(__file__).resolve().parents[2].joinpath("optimize/requirements.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"optimize/requirements.txt: {len(PINS)} pins")


if __name__ == "__main__":
    main()
