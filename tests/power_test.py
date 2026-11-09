"""optimize/power.sh (optimize/measure.sh power): the summary of what UrTV holds, from recorded answers, and that it only reads.
No TV needed.

    python3 tests/power_test.py
"""
import os, subprocess, sys, tempfile, types
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FIX = ROOT / "tests" / "fixtures" / "power"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + detail))
    failed += not ok

def summary(name):
    r = subprocess.run(["bash", str(ROOT / "optimize" / "power.sh"), "--from", str(FIX / name)], capture_output=True, text=True, cwd=ROOT)
    return r.returncode, r.stdout + r.stderr

rc, out = summary("quiet.txt")
check("a quiet TV: Doze, no allowlist, no UrTV lock, alarm or job", rc == 0 and "Doze: deep IDLE, light IDLE" in out and "no UrTV app is on the Doze allowlist" in out
      and "Wake locks: 2 held by anything, 0 owned by UrTV" in out and "Wi-Fi locks: none owned by UrTV" in out and "Alarms: none seen naming UrTV" in out and "Jobs: none seen naming UrTV" in out, out)
rc, out = summary("busy.txt")
check("a TV with UrTV holds: the wake lock, the two alarms (one wakes the TV) and the update job are listed", rc == 0 and "Wake locks: 3 held by anything, 2 owned by UrTV" in out
      and "MediaPlayer-WakeLock" in out and "AudioMix" in out and "RILJ_ACK_WL" not in out and "Wi-Fi locks: UrTV holds one" in out and "Alarms: 2 line(s) naming UrTV (1 of them wake the TV up)" in out
      and "Jobs: 1 line(s) naming UrTV (sa.urtv.screensaver/.UpdateJob)" in out and "Doze allowlist (sa.urtv.home)" in out and "deep ACTIVE" in out, out)
rc, out = summary("empty.txt")
check("a TV that shows nothing: it says it did not say, and never claims nothing is held", rc == 0 and "Doze: the TV did not say" in out and "Wake locks: ? held" in out
      and all(f"- {k}: not seen" in out for k in ("Battery optimisation", "Wi-Fi locks")) and "Alarms: none seen" in out and "none naming UrTV" not in out and "no UrTV app is on" not in out, out)
check("it says what it is: a moment's reading, not watts", "not a power measurement" in out)

# measure.sh power is the same thing, and everything it sends the TV only reads
for name in ("optimize/measure.sh",):
    src = (ROOT / name).read_text(encoding="utf-8")
    check("measure.sh power hands over to power.sh before it does anything to the TV", src.index("optimize/power.sh") < src.index("tv --reboot"))
for n in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[n] = types.ModuleType(n)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = object
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "192.0.2.5"
sys.path.insert(0, str(ROOT / "optimize"))
import tv
t = Path(tempfile.mkdtemp()); (t / "py").write_text('#!/usr/bin/env bash\n[ "$3" = --batch ] && cat > "$T/sent"\ncat "$FIXTURE"\n'); (t / "py").chmod(0o755)
env = dict(os.environ, T=str(t), PY=str(t / "py"), FIXTURE=str(FIX / "busy.txt"), URTV_RUN_DIR=str(t / "run"))
r = subprocess.run(["bash", str(ROOT / "optimize" / "measure.sh"), "power"], env=env, capture_output=True, text=True, cwd=ROOT)
sent = (t / "sent").read_text().splitlines() if (t / "sent").exists() else []
check("asked live (through measure.sh power), it prints the summary", r.returncode == 0 and "What UrTV holds right now" in r.stdout and "Jobs: 1 line(s)" in r.stdout, r.stdout + r.stderr)
check("and every line it sent the TV only reads (no settings put, no am, no restart)", len(sent) == 6 and all(tv.is_read_only(l) for l in sent), str(sent))
sys.exit(1 if failed else 0)
