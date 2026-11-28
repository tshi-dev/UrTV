"""An empty answer about the TV is read again on a new connection: the probe's properties and the precheck (R-374).

The probe and the precheck sometimes read empty properties (a half-open ADB connection, a TV still starting its services):
the report said «? ? (?)», or the precheck stopped with «did not say what it is». tv.py now reads a property every TV has
again on a new connection (3 reads in all), and precheck.sh asks again (twice) when the features or the maker are empty.
A fake TV; no TV needed.

    python3 tests/props_retry_test.py
"""
import contextlib, io, os, shutil, subprocess, sys, tempfile, types
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

st = {}
class Dev:
    def __init__(self, *a, **k): pass
    def connect(self, **k): st["connects"] += 1
    def close(self): pass
    def shell(self, cmd, read_timeout_s=0):
        st["asked"][cmd] = st["asked"].get(cmd, 0) + 1
        if cmd.startswith("getprop "):
            if st["asked"][cmd] <= st["empty_for"]:
                return "\n"
            return {"getprop ro.product.cpu.abilist32": ""}.get(cmd, "TCL\n")
        return ""
for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = Dev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"   # a documentation address: nothing is reached
os.environ["URTV_RUN_DIR"] = tempfile.mkdtemp()
sys.path.insert(0, str(ROOT / "optimize"))
import tv
tv.signer = lambda: None
tv.new_dev = lambda timeout: Dev()
tv.time = types.SimpleNamespace(time=__import__("time").time, sleep=lambda s: None)

def shell(cmd, empty_for):
    st.update(connects=0, asked={}, empty_for=empty_for)
    t = tv.TV()
    return t.shell(cmd), st["asked"].get(cmd, 0), st["connects"]

out, asked, connects = shell("getprop ro.product.manufacturer", 2)
check("an empty maker is read again on a new connection, and the third read counts", out.strip() == "TCL" and asked == 3 and connects == 3, (out, asked, connects))
out, asked, connects = shell("getprop ro.build.version.sdk", 0)
check("an answer at once: read once", out.strip() == "TCL" and asked == 1 and connects == 1, (asked, connects))
out, asked, connects = shell("getprop ro.product.cpu.abilist32", 0)
check("a property that may be empty (abilist32) is not read again", out == "" and asked == 1, (asked,))
out, asked, connects = shell("getprop ro.product.model", 5)
check("still empty after 3 reads: empty, no endless loop", out.strip() == "" and asked == 3, (asked,))

# precheck.sh: empty features are asked again
FEATS = "$ pm list features\nfeature:android.software.leanback\n$ getprop ro.product.manufacturer\nTCL\n$ dumpsys webviewupdate\nCurrent WebView package (name, version): (com.google.android.webview, 120.0.1)\n$ for p in x\n"
EMPTY = "$ pm list features\n\n$ getprop ro.product.manufacturer\n\n$ dumpsys webviewupdate\n\n$ for p in x\n"
t = Path(tempfile.mkdtemp()); (t / "optimize").mkdir()
for f in ("precheck.sh", "precheck.py"):
    shutil.copy(ROOT / "optimize" / f, t / "optimize")
(t / "py").write_text('#!/bin/bash\nif [ "$2" = optimize/tv.py ]; then cat > /dev/null; n=$(( $(cat "$T/n" 2>/dev/null || echo 0) + 1 )); echo $n > "$T/n"\n'
                      '  if [ "$n" -le "$EMPTY_FOR" ]; then cat "$T/empty"; else cat "$T/good"; fi\nelse exec python3 "$@"; fi\n')
(t / "py").chmod(0o755); (t / "empty").write_text(EMPTY); (t / "good").write_text(FEATS)
def pre(empty_for):
    (t / "n").unlink(missing_ok=True)
    r = subprocess.run(["bash", str(t / "optimize/precheck.sh")], env=dict(os.environ, PY=str(t / "py"), T=str(t), EMPTY_FOR=str(empty_for), PRECHECK_WAIT_S="0"),
                       capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr, int((t / "n").read_text())
rc, out, n = pre(2)
check("precheck: empty twice, then the answer: an Android TV, asked 3 times", rc == 0 and n == 3 and "asking again" in out, (rc, n, out))
rc, out, n = pre(9)
check("precheck: empty every time: asked 3 times, then «did not say what it is» (exit 5) as before", rc == 5 and n == 3, (rc, n, out))
rc, out, n = pre(0)
check("precheck: an answer at once: asked once", rc == 0 and n == 1, (rc, n))
sys.exit(1 if bad else 0)
