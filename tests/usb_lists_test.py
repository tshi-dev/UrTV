#!/usr/bin/env python3
"""4.4 hard case (docs/review/HARD-CASES-44.md, «USB drive not seen after the setup until a reboot», a TCL on Android 11 and a Sony on Android 12):
no list of ours, and no community list after our filters, turns off Android's storage, MTP or USB-host path, and the setup on the TV keeps them too.
A guard: it passed before the case was looked at (the cause is not a list), and fails if one is added.

    python3 tests/usb_lists_test.py
"""
import importlib.util, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
L = ROOT / "optimize/lists"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def words(p): return [l.split()[0] for l in p.read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]
KEEP, CORE, INP = set(words(L / "keep.txt")), words(L / "core.txt"), words(L / "inputs.txt")
def protected(p): return p in KEEP or any(re.fullmatch(r, p) for r in CORE + INP)

# Android's own storage and USB path, by the names it has had (Android 8 to 14, AOSP and Google's mainline builds)
STORAGE = ["com.android.externalstorage", "com.android.mtp", "com.android.storagemanager", "com.android.providers.media", "com.android.providers.media.module",
           "com.google.android.providers.media.module", "com.android.providers.downloads", "com.android.documentsui", "com.google.android.documentsui",
           "com.android.providers.downloads.ui", "com.android.defcontainer"]
for p in STORAGE:
    check(f"{p} can never be turned off (core.txt or keep.txt)", protected(p))
NAME = re.compile(r"(^|\.)(mtp|usbhost|usbmanager|usbstorage|externalstorage|storagemanager|vold|mediaprovider|media\.module|documentsui|udisk|mount)(\.|$)|providers\.media", re.I)
for n in ("tcl-safe.txt", "tcl-optional.txt", "home-replaced.txt"):
    hit = [p for p in words(L / n) if NAME.search(p)]
    check(f"our own {n} names no storage, MTP or USB-host package", not hit, hit)
src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
safe = re.findall(r'"([^"]+)"', re.search(r"SAFE = \{(.*?)\};", src, re.S).group(1))
check("the setup on the TV (SetupLists.SAFE) names none", not [p for p in safe if NAME.search(p)], [p for p in safe if NAME.search(p)])
spec = importlib.util.spec_from_file_location("community", ROOT / "optimize/community.py")
com = importlib.util.module_from_spec(spec); spec.loader.exec_module(com)
left = {}
for f in sorted(p for p in (L / "community").glob("*.txt") if not p.name.startswith("LICENSE")):
    out = [p for p in com.packages(str(f), KEEP) if NAME.search(p) and not protected(p)]
    if out: left[f.name] = out
check("no community list, after our filters, turns one off", not left, dict(list(left.items())[:3]))
sys.exit(1 if failed else 0)
