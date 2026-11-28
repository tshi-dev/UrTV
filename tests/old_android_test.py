"""The setup refuses a TV older than Android 8 at the precheck, before anything is changed, and keeps each install's answer (R-376).

The home app and the add-on need Android 8 (minSdk 26). An Android 7 box went through the backup, the lists and the tweaks,
then stopped at the install with INSTALL_FAILED_OLDER_SDK, and the install's own answer was shown only on screen.
precheck.py now stops with exit 6 (and the Arabic words) from the TV's SDK; run.sh says why; install.sh writes every install's
answer to the run folder's install.log. Canned answers; no TV needed.

    python3 tests/old_android_test.py
"""
import re, subprocess, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

def answers(sdk, release):
    return ("$ pm list features\nfeature:android.software.leanback\n$ getprop ro.product.manufacturer\nTCL\n"
            "$ dumpsys webviewupdate\nCurrent WebView package (name, version): (com.google.android.webview, 120.0.1)\n"
            f"$ getprop ro.build.version.sdk\n{sdk}\n$ getprop ro.build.version.release\n{release}\n")
def pre(text):
    r = subprocess.run([sys.executable, "-I", str(ROOT / "optimize/precheck.py"), "check"], input=text, capture_output=True, text=True)
    return r.returncode, r.stdout
rc, out = pre(answers(25, "7.1.2"))
check("Android 7.1 (SDK 25): stops at the precheck with exit 6, says nothing was changed, and the Arabic words",
      rc == 6 and "Android 7.1.2 (SDK 25)" in out and "nothing was changed" in out and "أندرويد ٨" in out, out)
rc, out = pre(answers(26, "8.0"))
check("Android 8 (SDK 26): goes on", rc == 0 and "!!" not in out, out)
rc, out = pre(answers("", ""))
check("the SDK not said: goes on as before (the install says it if it must)", rc == 0, out)
sh = (ROOT / "optimize/precheck.sh").read_text(encoding="utf-8")
check("precheck.sh asks the TV for its SDK and release", '"getprop ro.build.version.sdk"' in sh and '"getprop ro.build.version.release"' in sh)
run = (ROOT / "optimize/run.sh").read_text(encoding="utf-8")
check("run.sh: exit 6 at the precheck is said as «older than 8, nothing was changed»", re.search(r'"\$rc" -eq 6 \] && \[ "\$current" = precheck \].*older than 8', run) is not None)
inst = (ROOT / "optimize/install.sh").read_text(encoding="utf-8")
fn = re.search(r"^install_log\(\) \{.*?^\}\n", inst, re.S | re.M)
check("install.sh logs the answer of each install (ours and the extras)", fn is not None and inst.count('install_log "$1" "$out"') == 1 and inst.count('install_log "$name" "$out"') == 1)
if fn:
    import tempfile
    d = tempfile.mkdtemp()
    subprocess.run(["bash", "-c", f'URTV_RUN_DIR="{d}"\n{fn.group(0)}install_log urtv-home.apk $\'Performing Streamed Install\\r\\nFailure [INSTALL_FAILED_OLDER_SDK: Requires newer sdk version #26 (current version is #25)]\''])
    log = Path(d, "install.log").read_text()
    check("install.log: one line per install, with the TV's whole answer", log.count("\n") == 1 and "urtv-home.apk: Performing Streamed Install Failure [INSTALL_FAILED_OLDER_SDK" in log, log)
sys.exit(1 if bad else 0)
