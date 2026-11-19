#!/usr/bin/env python3
"""4.4 beta 3, the paired route's security (the supervisor's hard condition; R-row from QA): the add-on's PAIR_* calls give an adb shell, so only our home, signed with our
certificate, may call them, and only the setup's own commands pass (by shape). The guard's cases on a plain JVM (tests/PairGuardRun.java): an unknown caller, another of our
apps, a shared uid, a wrong or unread certificate are refused; a command outside the list, an injected «;», a «$(…)», a second line are refused. Then the list itself is
exactly what tests/pair_commands_gen.py makes from the setup's scenarios (stale either way fails), and the checks are where they must be.
    python3 tests/pair_commands_test.py   (needs javac)"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

if not shutil.which("javac"):
    print("SKIP: no javac"); sys.exit(1)
with tempfile.TemporaryDirectory() as tmp:
    srcs = [HOME / "PairGuard.java", HOME / "PairCommandShapes.java", HOME / "setup/IdleSetRule.java", ROOT / "tests/PairGuardRun.java"]
    r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *map(str, srcs)], capture_output=True, text=True)
    check("PairGuard and the shape list compile on a plain JVM (no Android in them)", r.returncode == 0, r.stderr[-600:])
    if r.returncode == 0:
        got = dict(l.split("\t", 1) for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.setup.PairGuardRun", "cases"], capture_output=True, text=True, encoding="utf-8").stdout.splitlines() if "\t" in l)
        want = {
            "caller_home": "true", "caller_other_pkg": "false", "caller_screensaver": "false", "caller_shared_uid": "false", "caller_wrong_cert": "false",
            "caller_unread_cert": "false", "caller_none": "false",
            "cmd_known": "true", "cmd_known_other_pkg": "true", "cmd_install": "true",
            "cmd_unknown": "false", "cmd_injected": "false", "cmd_subst": "false", "cmd_newline": "false", "cmd_null": "false",
            "shape": "pm disable-user --user N P|for p in P+; do x; done",
        }
        for k, v in want.items():
            check("guard " + k, got.get(k) == v, got.get(k))

gen = subprocess.run([sys.executable, str(ROOT / "tests/pair_commands_gen.py"), "--print"], capture_output=True, text=True, encoding="utf-8")
check("the committed shape list is exactly what the setup's scenarios send (run tests/pair_commands_gen.py after a change to the setup's commands)",
      gen.returncode == 0 and gen.stdout == read(HOME / "PairCommandShapes.java"), (gen.stderr or "")[-400:] or "differs")
shapes = read(HOME / "PairCommandShapes.java")
check("no shape is a free shell: none is only a placeholder, and none starts with sh, su, eval, exec or a variable",
      not re.search(r'^\s*"(P|N|P\+)"', shapes, re.M) and not re.search(r'^\s*"(sh|su|eval|exec|\$)\b', shapes, re.M))

prov = read(ROOT / "android/extras/src/main/java/sa/urtv/home/ExtrasCallProvider.java")
pre = prov.split("switch (method) {")[0]
check("the add-on refuses every pair_* call that is not from our home with our certificate, before anything runs (a fixed log word)",
      'if (method.startsWith("pair_") && !SignDigest.callerIs(getContext(), PairGuard.HOME))' in pre and '"refused: not our home"' in pre)
check("the add-on runs a command or an install only when it is one of the setup's own", prov.count("if (!PairGuard.allowed(") >= 2 and '"not_allowed"' in prov)
route = read(HOME / "setup/PairRoute.java")
check("the home refuses before sending, too (never a generic shell, even for our own code)", route.count("PairGuard.allowed(command)") == 2)
files = read(HOME / "setup/PairFiles.java")
check("the home's one-time install files are read only by our add-on with our certificate", 'SignDigest.callerIs(getContext(), "sa.urtv.extras")' in files)
sd = read(HOME / "SignDigest.java")
check("the certificate is compared by its SHA-256 (the current signer; none, several or unreadable is refused)", 'MessageDigest.getInstance("SHA-256")' in sd and "hasMultipleSigners()" in sd and "return null;" in sd)
keys = read(ROOT / "android/extras/src/main/java/sa/urtv/home/pair/PairKeys.java")
check("the pairing key and certificate stay in the add-on's private files (getFilesDir), never shared storage", "c.getFilesDir()" in keys and "getExternal" not in keys and "MODE_WORLD" not in keys)
xg = read(ROOT / "android/extras/build.gradle")
check("the add-on builds with the same guard and list as the home (shared files)", all(f"'{f}'" in xg for f in ("PairGuard.java", "PairCommandShapes.java", "SignDigest.java")))
print("FAILED" if failed else "all passed")
sys.exit(1 if failed else 0)
