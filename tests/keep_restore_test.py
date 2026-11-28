#!/usr/bin/env python3
"""After the cleanup: the list of what was turned off (by name), `keep.sh restore` turns one back on and remembers it, and a later
disable.sh (run.sh again) leaves it on. A fake tv.py keeps the TV's packages in a file; no TV needed.

    python3 tests/keep_restore_test.py
"""
import os, re, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
tmp = Path(tempfile.mkdtemp())
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

# the fake tv.py: $TVSTATE has one package per line, a leading «-» for a disabled one; «!» in front: pm enable can't turn it on
fake = tmp / "fakepy"
fake.write_text(r'''#!/usr/bin/env python3
import os, re, sys
a = sys.argv[3:]
st = open(os.environ["TVSTATE"]).read().split()
def save(): open(os.environ["TVSTATE"], "w").write("\n".join(st) + "\n")
def run(cmd):
    global st
    if cmd.endswith("; echo END_OF_LIST"):   # the end-marked list questions of disable.sh
        base = cmd[: -len("; echo END_OF_LIST")]
        body = run(base) if base != "pm list packages" else "package:android\n" + "".join("package:%s\n" % p.lstrip("-!") for p in st)
        return body + "END_OF_LIST\n"
    if cmd == "pm list packages -d":
        if os.path.exists(os.environ["TVSTATE"] + ".fail"): sys.exit(1)   # the list cannot be read
        return "".join("package:%s\n" % p[1:].lstrip("!") for p in st if p.startswith("-"))
    m = re.fullmatch(r"pm list packages (\S+)", cmd)
    if m:
        return "".join("package:%s\n" % p.lstrip("-!") for p in st if p.lstrip("-!") == m.group(1))
    m = re.fullmatch(r"pm enable (?:--user 0 )?(\S+)", cmd)
    if m:
        st = [p if (p.lstrip("-!") != m.group(1) or p.startswith("-!")) else p.lstrip("-") for p in st]; save(); return "Package enabled\n"
    m = re.fullmatch(r"pm disable-user --user 0 (\S+)", cmd)
    if m:
        st = [("-" + p if p.lstrip("-!") == m.group(1) and not p.startswith("-") else p) for p in st]; save(); return "disabled\n"
    return ""
if a[:1] == ["--batch"]:
    for line in sys.stdin:
        if line.strip():
            open(os.environ["LOG"], "a").write(line.strip() + "\n"); run(line.strip())
else:
    open(os.environ["LOG"], "a").write(a[0] + "\n"); sys.stdout.write(run(a[0]))
''')
fake.chmod(0o755)
state, log, home = tmp / "state", tmp / "log", tmp / "home"
def env(): return {**os.environ, "PY": str(fake), "TVSTATE": str(state), "LOG": str(log), "URTV_HOME": str(home)}
def sh(script, *args):
    r = subprocess.run(["bash", str(ROOT / "optimize" / script), *args], capture_output=True, text=True, env=env(), cwd=ROOT)
    return r.returncode, r.stdout + r.stderr
def tvstate(*lines): state.write_text("\n".join(lines) + "\n"); log.write_text("")

# the fake's python is the system one for offlist.py; keep.sh calls "$PY" -I optimize/offlist.py too, so give it a real python
real = tmp / "py"
real.write_text('#!/bin/bash\nif [ "$2" = optimize/tv.py ]; then exec ' + str(fake) + ' "$@"; else exec python3 "$@"; fi\n')
real.chmod(0o755)
def env():   # noqa: F811
    return {**os.environ, "PY": str(real), "TVSTATE": str(state), "LOG": str(log), "URTV_HOME": str(home)}

# ---- names
names = {}
for l in (ROOT / "optimize/lists/names-ar.txt").read_text(encoding="utf-8").splitlines():
    if l.strip() and not l.startswith("#"):
        k, _, v = l.strip().partition(" "); names[k] = v.strip()
for lst in ("tcl-safe", "tcl-optional"):
    pk = [l.split()[0] for l in (ROOT / f"optimize/lists/{lst}.txt").read_text().splitlines() if l.strip() and not l.lstrip().startswith("#")]
    missing = [p for p in pk if p not in names]
    check(f"every package of {lst}.txt has a name the person knows", not missing, str(missing))
check("names are short (30 characters at most)", all(len(v) <= 30 for v in names.values()), str([v for v in names.values() if len(v) > 30]))

# ---- the list of what was turned off
tvstate("-com.tcl.messagebox", "-com.tcl.bi", "com.tcl.gamebar", "-com.unknown.thing", "-com.google.android.apps.tv.launcherx", "-com.tcl.smartalexa",
        "com.google.android.youtube.tv")
rc, out = sh("keep.sh", "off")
rows = out.strip().splitlines()
check("off: the apps of our lists that are off, by name, with their list", "تنبيهات TCL | com.tcl.messagebox | tcl-safe" in rows and "تتبّع استخدام TCL | com.tcl.bi | tcl-safe" in rows, out)
check("off: an app still on is not listed; the stock home and a protected one (Alexa) are not ours to list",
      not any("gamebar" in r or "launcherx" in r or "smartalexa" in r or "unknown" in r for r in rows), out)

# ---- restore
rc, out = sh("keep.sh", "restore", "com.tcl.messagebox")
check("restore: turned on, named, remembered", rc == 0 and "on again: تنبيهات TCL (com.tcl.messagebox)" in out and "Remembered" in out
      and "-com.tcl.messagebox" not in state.read_text() and (home / "kept.txt").read_text().split().count("com.tcl.messagebox") == 1, out)
rc, out = sh("keep.sh", "off")
check("off: after that it is no longer listed", "messagebox" not in out and "com.tcl.bi" in out, out)
tvstate("-com.tcl.messagebox", "-com.tcl.bi", "-!com.tcl.stubborn")
rc, out = sh("disable.sh", "optimize/lists/tcl-safe.txt")
check("a later disable (run.sh again) leaves the kept app on and says so", "pm disable-user --user 0 com.tcl.messagebox" not in log.read_text()
      and "pm disable-user --user 0 com.tcl.bi" in log.read_text() and "Left on, as the person asked: com.tcl.messagebox" in out, out + log.read_text())
tvstate("-com.tcl.bi", "-!com.tcl.stubborn", "com.tcl.logkit")
rc, out = sh("keep.sh", "restore", "com.tcl.stubborn")
check("restore of one the TV will not turn on: says so, does not remember it", "!! com.tcl.stubborn is still off" in out
      and "com.tcl.stubborn" not in (home / "kept.txt").read_text(), out)
tvstate("-com.tcl.bi", "-com.tcl.logkit")
Path(str(state) + ".fail").write_text("")
rc, out = sh("keep.sh", "restore", "com.tcl.logkit")
Path(str(state) + ".fail").unlink()
check("restore when the list of what is off cannot be read: not «on again», not remembered, a !! line",
      "   on again: " not in out and "!! Could not ask the TV which packages are off, so «com.tcl.logkit on again» was not confirmed" in out
      and "com.tcl.logkit" not in (home / "kept.txt").read_text().split(), out)
rc, out = sh("keep.sh", "restore", "com.not.on.this.tv")
check("restore of a package this TV does not have: skipped", "not on this TV" in out and "com.not.on.this.tv" not in (home / "kept.txt").read_text(), out)
log.write_text("")
rc, out = sh("keep.sh", "restore", "com.google.android.apps.tv.launcherx")
check("restore of the stock home is refused (it belongs to the home switch)", "belongs to the home screen switch" in out and "pm enable" not in log.read_text(), out + log.read_text())
rc, out = sh("keep.sh", "restore", "bad pkg;rm")
check("restore of something that is not a package id: refused", rc == 2, out)
rc, out = sh("keep.sh", "restore")
check("restore with nothing: usage", rc == 2 and "usage" in out, out)
tvstate("com.tcl.messagebox", "-com.tcl.bi")
rc, out = sh("keep.sh", "unkeep", "com.tcl.messagebox")
check("unkeep: off again, forgotten", rc == 0 and "off again" in out and "-com.tcl.messagebox" in state.read_text()
      and "com.tcl.messagebox" not in (home / "kept.txt").read_text().split(), out)

# ---- R-362 (report row 765): an app an earlier run turned off that keep.txt names now is turned back on and leaves the ledger
runs = tmp / "run362"; runs.mkdir(exist_ok=True)
(runs / "disabled-by-us.txt").write_text("com.tcl.partnercustomizer\ncom.tcl.bi\n")
tvstate("-com.tcl.partnercustomizer", "-com.tcl.bi", "-com.maker.off", "com.tcl.messagebox")
r = subprocess.run(["bash", str(ROOT / "optimize/disable.sh"), "optimize/lists/tcl-safe.txt"], capture_output=True, text=True, env={**env(), "URTV_RUN_DIR": str(runs)}, cwd=ROOT)
left = (runs / "disabled-by-us.txt").read_text().split()
check("R-362: a kept app the ledger holds is turned back on and leaves the ledger; one the maker had off stays off; the rest of the ledger stays",
      "pm enable --user 0 com.tcl.partnercustomizer" in log.read_text() and "-com.tcl.partnercustomizer" not in state.read_text().split()
      and "-com.maker.off" in state.read_text().split() and "com.tcl.partnercustomizer" not in left and "com.tcl.bi" in left, r.stdout + r.stderr + log.read_text() + str(left))

# ---- offlist directly
r = lambda *a: subprocess.run([sys.executable, "-I", str(ROOT / "optimize/offlist.py"), *a], capture_output=True, text=True, env=env())
check("name of an unknown app is its last word", r("name", "com.some.brandapp").stdout.strip() == "brandapp")
check("allowed: a list without the kept ones", r("allowed", str(ROOT / "optimize/lists/tcl-safe.txt")).stdout.split().count("com.tcl.bi") == 1)
sh("keep.sh", "restore", "com.tcl.bi")
check("allowed after remember: the kept one is gone", "com.tcl.bi" not in r("allowed", str(ROOT / "optimize/lists/tcl-safe.txt")).stdout.split())
r("clear")
check("clear (reset.sh): nothing kept", r("kept").stdout.strip() == "")
check("reset.sh clears it", "offlist.py clear" in (ROOT / "optimize/reset.sh").read_text())
print("\nall keep/restore checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
