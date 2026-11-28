#!/usr/bin/env python3
"""R-450 (a TCL on Android 12: the Assistant and the accounts page showed errors after the setup). No list of ours, and no community list after our
filters, turns off an account, sign-in, sync, partner-setup or Assistant app; the setup on the TV and disable.sh turn back on the ones an earlier run
turned off (SetupEngine.HEAL, the same names in disable.sh). No TV needed.

    python3 tests/accounts_keep_test.py
"""
import importlib.util, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LISTS = ROOT / "optimize/lists"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def words(path):
    return [l.split()[0] for l in path.read_text(encoding="utf-8").splitlines() if l.strip() and not l.lstrip().startswith("#")]

ACCOUNTS = re.compile(r"account|auth|syncadapter|partnersetup|katniss|assistant|usercenter|backuptransport", re.I)
PKG = re.compile(r"[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+")
KEEP = set(words(LISTS / "keep.txt"))
spec = importlib.util.spec_from_file_location("community", ROOT / "optimize/community.py")
com = importlib.util.module_from_spec(spec); spec.loader.exec_module(com)

for name in ("tcl-safe.txt", "tcl-optional.txt", "home-replaced.txt"):
    hit = [p for p in words(LISTS / name) if ACCOUNTS.search(p)]
    check(f"our own {name} names no account, sign-in, sync or Assistant app", not hit, hit)

src = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupLists.java").read_text(encoding="utf-8")
safe = re.search(r"SAFE = \{(.*?)\};", src, re.S).group(1)
hit = [p for p in re.findall(r'"([^"]+)"', safe) if ACCOUNTS.search(p)]
check("the setup on the TV (SetupLists.SAFE) turns off no account, sign-in, sync or Assistant app", not hit, hit)

named, left = set(), {}
for f in sorted(p for p in (LISTS / "community").glob("*.txt") if not p.name.startswith("LICENSE")):
    for p in words(f):
        if PKG.fullmatch(p) and ACCOUNTS.search(p):
            named.add(p)
    out = [p for p in com.packages(str(f), KEEP) if ACCOUNTS.search(p)]
    if out:
        left[f.name] = out
check("the community lists do name such apps (the case this guards)", "com.google.android.partnersetup" in named and "com.google.android.syncadapters.calendar" in named, sorted(named)[:5])
check("every account, sign-in, sync or Assistant app a community list names is on keep.txt", not (named - KEEP), sorted(named - KEEP))
check("no community list, after our filters, turns one off", not left, dict(list(left.items())[:3]))

eng = (ROOT / "android/launcher/src/main/java/sa/urtv/home/setup/SetupEngine.java").read_text(encoding="utf-8")
heal = re.findall(r'"([^"]+)"', re.search(r"HEAL = \{(.*?)\};", eng, re.S).group(1))
dis = (ROOT / "optimize/disable.sh").read_text(encoding="utf-8")
line = next(l for l in dis.splitlines() if l.lstrip().startswith("healed=$("))
sh_heal = re.findall(r"-e (\S+)", line)
for p in ("com.google.android.partnersetup", "com.tcl.usercenter", "com.google.android.syncadapters.calendar", "com.google.android.syncadapters.contacts"):
    check(f"{p} is turned back on where an earlier run turned it off (SetupEngine.HEAL and disable.sh)", p in heal and p in sh_heal)
check("SetupEngine.HEAL and disable.sh heal the same names, all on keep.txt", heal == sh_heal and set(heal) <= KEEP, (heal, sh_heal))

sys.exit(1 if failed else 0)
