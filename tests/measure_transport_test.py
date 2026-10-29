"""optimize/measure.sh before: a TV connected by Wireless debugging (TLS and pairing: the port changes when it restarts) is NOT restarted; it is measured now and
the output says so. A TV on plain network adb is restarted as before. tv.py --transport tells which. Fake TV; no TV needed.

    python3 tests/measure_transport_test.py
"""
import contextlib, io, json, os, shutil, subprocess, sys, tempfile, types
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

# 1. tv.py --transport (the real function, with the adb library stubbed)
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = object
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"
os.environ.pop("TV_PORT", None)
tmp = Path(tempfile.mkdtemp())
os.environ["URTV_HOME"] = str(tmp / "home")
sys.path.insert(0, str(ROOT / "optimize"))
import tv, wireless
def transport(state):
    wireless.load_state = lambda: state
    tv.HOST = "192.0.2.5"
    out = io.StringIO(); sys.argv = ["tv.py", "--transport"]
    with contextlib.redirect_stdout(out):
        tv.main()
    return out.getvalue().strip()
check("Wireless debugging saved for this address: «wireless»", transport({"mode": "tls", "host": "192.0.2.5", "port": 38123}) == "wireless")
check("nothing saved: plain network adb", transport({}) == "adb")
check("Wireless debugging saved for ANOTHER address: plain network adb", transport({"mode": "tls", "host": "192.0.2.77", "port": 38123}) == "adb")

# 2. measure.sh with a stand-in tv.py
STUB = r'''#!/usr/bin/env bash
shift 2
echo "$1 $2" >> "$T/calls"
case "$1" in
  --transport) echo "$TRANSPORT" ;;
  --batch) cat > /dev/null; echo "== memory" ;;
  "settings get system screen_off_timeout") echo 60000 ;;
esac
exit 0
'''
def run(transport):
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize").mkdir(parents=True); (tr / "optimize/run").mkdir()
    for f in ("measure.sh", "settle.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    (tr / "optimize/power.sh").write_text("#!/usr/bin/env bash\nexit 0\n"); (tr / "optimize/power.sh").chmod(0o755)
    (d / "py").write_text(STUB); (d / "py").chmod(0o755)
    (d / "bin").mkdir(); (d / "bin" / "sleep").write_text("#!/bin/sh\nexit 0\n"); (d / "bin" / "sleep").chmod(0o755)
    env = dict(os.environ, T=str(d), PY=str(d / "py"), TRANSPORT=transport, PATH=f"{d / 'bin'}:{os.environ['PATH']}", URTV_RUN_DIR=str(tr / "optimize/run"), PACKAGE_SETTLE_S="0")
    r = subprocess.run(["bash", str(tr / "optimize/measure.sh"), "before"], env=env, capture_output=True, text=True, cwd=tr)
    return r, (d / "calls").read_text() if (d / "calls").exists() else ""
r, calls = run("wireless")
check("Wireless debugging: no restart (no --reboot), it says why, and the measurement is still made", r.returncode == 0 and "--reboot" not in calls and "Wireless debugging" in r.stdout and "without the restart" in r.stdout and "letting the TV settle" not in r.stdout and "--batch" in calls, f"{r.returncode} {r.stdout} {calls}")
r, calls = run("adb")
check("plain network adb: restarted as before", r.returncode == 0 and "--reboot" in calls and "letting the TV settle" in r.stdout, f"{r.returncode} {r.stdout} {calls}")
r, calls = run("")
check("--transport that says nothing counts as wireless (never a restart on a guess)", "--reboot" not in calls, calls)
src = (ROOT / "optimize/measure.sh").read_text(encoding="utf-8")
check("--now never asks (a read with no restart)", src.index("tv --transport") < src.index("tv --reboot") and 'if [ "$mode" != --now ]; then\n    transport=' in src)
sys.exit(1 if failed else 0)
