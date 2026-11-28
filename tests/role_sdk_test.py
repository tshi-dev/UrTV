"""`cmd role` is not sent to a TV older than Android 10 (R-368).

Android 9 has no role service: every `cmd role add-role-holder …` of the home switch printed «Can't find service: role» in
the setup's output, which read as an error. tv.py now leaves such a line out below SDK 29 (said in a batch's output), and
the safety switch on the TV (deadman.sh) asks for the role only from SDK 29. A fake TV; no TV needed.

    python3 tests/role_sdk_test.py
"""
import io, os, subprocess, sys, tempfile, types
from contextlib import redirect_stdout
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)   # tv.py's ADB library is not needed: the TV is a fake
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = object
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"   # a documentation address: nothing is reached, the TV class is a fake
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
sys.path.insert(0, os.path.join(ROOT, "optimize"))
import tv as tvmod

bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + (f": {detail}" if detail and not ok else ""))
    if not ok:
        bad.append(name)

def fake_tv(sdk):
    sent = []
    class Fake:
        def __init__(self, *a, **k): pass
        def shell(self, cmd, timeout=120):
            sent.append(cmd)
            if cmd == "getprop ro.build.version.sdk":
                return f"{sdk}\n"
            if cmd.startswith("cmd role") and int(sdk) < 29:
                return "cmd: Can't find service: role\n"
            return "ok\n"
        def close(self): pass
    return Fake, sent

def run(sdk, argv, stdin=""):
    Fake, sent = fake_tv(sdk)
    real = (tvmod.TV, sys.argv, sys.stdin)
    tvmod.TV, sys.argv, sys.stdin = Fake, ["tv.py"] + argv, io.StringIO(stdin)
    out = io.StringIO()
    try:
        with redirect_stdout(out):
            tvmod.main()
    finally:
        tvmod.TV, sys.argv, sys.stdin = real
    return out.getvalue(), sent

BATCH = "cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home\ncmd package set-home-activity --user 0 sa.urtv.home/.MainActivity\n"
out, sent = run(28, ["--batch"], BATCH)
check("Android 9: the role line is not sent, the rest is", not any(c.startswith("cmd role") for c in sent)
      and any(c.startswith("cmd package set-home-activity") for c in sent), sent)
check("Android 9: no «Can't find service» in the output, and it says why the line was left out",
      "Can't find service" not in out and "not sent: no HOME role before Android 10" in out, out)
out, sent = run(29, ["--batch"], BATCH)
check("Android 10: the role line is sent", any(c.startswith("cmd role add-role-holder") for c in sent), sent)
out, sent = run(28, ["cmd role get-role-holders --user 0 android.app.role.HOME 2> /dev/null"])
check("Android 9: a role question answers nothing (no holder), no error", out.strip() == "" and not any(c.startswith("cmd role") for c in sent), (out, sent))

# deadman.sh on the TV: the role line only from SDK 29 (a fake getprop and cmd first in the PATH)
d = tempfile.mkdtemp()
for name, body in (("getprop", 'echo "$SDK"'), ("cmd", 'echo "cmd $*" >> "$LOG"'), ("pm", "true"), ("am", "true"), ("input", "true"), ("sleep", "true"), ("date", "echo 0")):
    p = os.path.join(d, name); open(p, "w").write(f"#!/bin/sh\n{body}\n"); os.chmod(p, 0o755)
src = open(os.path.join(ROOT, "optimize", "deadman.sh"), encoding="utf-8").read()
check("deadman.sh skips the role only below SDK 29", '[ "${sdk:-99}" -lt 29 ] 2> /dev/null || cmd role add-role-holder' in src)
for sdk, want in (("28", False), ("30", True), ("", True)):   # "": the TV did not say (tests/deadman_test.py has no getprop): asked, as before
    log = os.path.join(d, f"log{sdk}")
    tail = src[src.index('pm enable "${STOCK%%/*}"'):]
    subprocess.run(["sh", "-c", 'STOCK=com.stock/.Main; ' + tail], env={**os.environ, "PATH": d + ":" + os.environ["PATH"], "SDK": sdk, "LOG": log}, capture_output=True)
    got = open(log).read() if os.path.exists(log) else ""
    check(f"deadman.sh on SDK {sdk}: role {'asked' if want else 'not asked'}", ("cmd role" in got) == want, got)
sys.exit(1 if bad else 0)
