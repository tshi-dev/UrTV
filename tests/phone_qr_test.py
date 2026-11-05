#!/usr/bin/env python3
"""The QR code the TV shows for «تحكم من الجوال» (QrCode.java, our own encoder), read back by a real decoder (zxing-cpp): every version from 1 to 10,
the pairing address, Arabic text; and the large ones (encodeLarge, level L up to version 40) the setup's done screen shows for its report link. Needs javac and `pip install zxing-cpp pillow`; skipped with a note without either (the Java side of the
code is tested in phone_test.py without a decoder).   Run: python3 tests/phone_qr_test.py"""
import random
import shutil
import string
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PHONE = ROOT / "android/launcher/src/main/java/sa/urtv/home/phone"
if not shutil.which("javac"):
    print("SKIP phone QR: no javac here")
    sys.exit(0)
try:
    import zxingcpp
    from PIL import Image
except ImportError:
    print("SKIP phone QR: zxing-cpp or pillow is not installed (pip install zxing-cpp pillow)")
    sys.exit(0)

CAP = [14, 26, 42, 62, 84, 106, 122, 152, 180, 213]   # bytes per version at level M
random.seed(7)
alphabet = string.ascii_letters + string.digits + ":/#.-_"
cases = [("http://" + "192" + "." + "168" + "." + "1" + "." + "20" + ":50000/p#" + "0123456789abcdef" * 2)]
for v, cap in enumerate(CAP, 1):
    cases.append("".join(random.choice(alphabet) for _ in range(cap)))       # a full version
    cases.append("".join(random.choice(alphabet) for _ in range(max(1, cap - 9))))
cases.append("بسم الله الرحمن الرحيم")
CAP_L = {1: 17, 7: 154, 11: 321, 20: 858, 30: 1732, 31: 1840, 32: 1952, 33: 2068, 40: 2953}   # bytes at level L (the standard's table)
FULL_L = {}
for v, cap in CAP_L.items():
    t = "".join(random.choice(alphabet) for _ in range(cap))
    FULL_L[t] = v
    cases.append("L:" + t)
cases.append("L:https://tshi-dev.github.io/UrTV/share/#" + "".join(random.choice(alphabet + "%") for _ in range(2953 - 41)))
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(PHONE / "QrCode.java"), str(ROOT / "tests/QrDumpRun.java")], check=True)
    out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.home.phone.QrDumpRun"] + cases, check=True, capture_output=True).stdout.decode("utf-8").split("\n")
failed = 0
i = 0
for text in cases:
    version, size = map(int, out[i].split())
    rows = out[i + 1:i + 1 + size]
    i += 1 + size
    scale, quiet = 6, 4
    img = Image.new("L", ((size + 2 * quiet) * scale,) * 2, 255)
    px = img.load()
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c == "1":
                for dy in range(scale):
                    for dx in range(scale):
                        px[(x + quiet) * scale + dx, (y + quiet) * scale + dy] = 0
    got = zxingcpp.read_barcodes(img)
    want = text[2:] if text.startswith("L:") else text
    ok = len(got) == 1 and got[0].text == want
    if want in FULL_L:   # a version filled to the byte: exactly that version (the smallest that holds it)
        ok = ok and version == FULL_L[want]
    failed += not ok
    print(("PASS " if ok else "FAIL ") + f"QR version {version}{' level L' if text.startswith('L:') else ''} ({len(want.encode())} bytes) is read back as written" + ("" if ok else f"  -> {[g.text[:60] for g in got]}"))
sys.exit(1 if failed else 0)
