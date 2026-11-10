#!/usr/bin/env python3
"""Every vector drawable's pathData must be one the Android Gradle plugin's own vector parser reads (the release build pre-renders vectors in :app:mergeReleaseResources
and fails with «Index N out of bounds» on a path it cannot parse: build 215 and 216 had none because of arcs written with compact flags such as `a1.6 1.6 0 110 3.2`).
The runtime parser reads compact flags; the build tool does not. So: in an arc (a/A) the two flags are the single tokens 0 or 1, written apart from their neighbours;
every command has the argument count it needs (M, L, T 2; H, V 1; C 6; S, Q 4; A 7; Z 0); the XML is well-formed. A real Gradle run is the final check (CI), this one runs anywhere."""
import re, sys, xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID = "{http://schemas.android.com/apk/res/android}"
NUM = re.compile(r"[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?")
ARITY = {"M": 2, "L": 2, "T": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "A": 7, "Z": 0}
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1

def problems(d):
    """what the build tool's parser would trip on in one pathData string"""
    out = []
    pos = 0
    for m in re.finditer(r"([A-Za-z])([^A-Za-z]*)", d):
        cmd, body = m.group(1), m.group(2)
        if re.fullmatch(r"\s*", d[pos:m.start()]) is None: out.append(f"stray text {d[pos:m.start()]!r}")
        pos = m.end()
        n = ARITY.get(cmd.upper())
        if n is None: out.append(f"unknown command {cmd}"); continue
        leftover = NUM.sub(" ", body)
        if leftover.replace(",", "").strip(): out.append(f"{cmd}: odd text {leftover.strip()!r}")
        nums = NUM.findall(body)
        if n == 0:
            if nums: out.append(f"{cmd} takes no numbers")
            continue
        if not nums or len(nums) % n: out.append(f"{cmd}: {len(nums)} numbers, not a multiple of {n}"); continue
        if cmd.upper() == "A":
            for k in range(0, len(nums), 7):
                if nums[k + 3] not in ("0", "1") or nums[k + 4] not in ("0", "1"): out.append(f"A: flags {nums[k + 3]!r} {nums[k + 4]!r} must be the single tokens 0 or 1, written apart (a compact `0 110 3.2` is read as one number)")
    return out

# the checker itself, on the two paths that broke the build and on good ones
check("the checker flags compact arc flags (the two broken paths of build 215)", problems("M6 3a3 3 0 013 3z") and problems("M12 2a10 10 0 100 20 10 10 0 000-20z"))
check("the checker accepts the same arcs with the flags apart, and the other commands", not problems("M6 3a3 3 0 0 1 3 3z M1 1h2v3H0V1zM0 0c1 1 2 2 3 3 1 1 2 2 3 3z") and not problems("M0 0 1 1 2 2"))
check("the checker flags a wrong argument count and stray text", problems("M1 2 3") and problems("M0 0 L1") and problems("M0 0 x 4"))

files = sorted(p for p in ROOT.glob("android/*/src/**/res/drawable*/*.xml"))
bad, seen = [], 0
for f in files:
    try: root = ET.parse(f).getroot()
    except ET.ParseError as e: bad.append(f"{f.relative_to(ROOT)}: not well-formed ({e})"); continue
    if root.tag != "vector": continue
    for el in root.iter():
        d = el.get(ANDROID + "pathData")
        if d is None: continue
        seen += 1
        for p in problems(d): bad.append(f"{f.relative_to(ROOT)}: {p}")
check(f"every vector drawable's pathData reads in the build tool's parser ({seen} paths in {len(files)} drawables)", not bad and seen > 0, bad[:6])
sys.exit(1 if failed else 0)
