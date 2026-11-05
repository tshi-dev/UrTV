"""optimize/tweaks.sh apply trims every app's cache only when the TV's storage is low. A stub tv.py; no TV needed.

    python3 tests/tweaks_trim_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --batch) cat >> "$T/batch" ;;
  "df -k /data"*) printf '%s\n' "$DF" ;;
  "settings get global"*) echo 1.0 ;;        # readable, so nothing is skipped
  "device_config get"*) echo null ;;
esac
exit 0
'''
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

def run(df, mode="apply"):
    t = Path(tempfile.mkdtemp()); tree = t / "tree"; (tree / "optimize").mkdir(parents=True)
    shutil.copy(ROOT / "optimize" / "tweaks.sh", tree / "optimize")
    (t / "py").write_text(STUB); (t / "py").chmod(0o755)
    env = dict(os.environ, T=str(t), PY=str(t / "py"), DF=df)
    r = subprocess.run(["bash", str(tree / "optimize" / "tweaks.sh"), mode], env=env, capture_output=True, text=True)
    batch = (t / "batch").read_text() if (t / "batch").exists() else ""
    return r.returncode, r.stdout + r.stderr, batch

GB = 1024 * 1024
rc, out, batch = run(f"/dev/block/dm-5 {16 * GB} {4 * GB} {12 * GB} 25% /data")
check("plenty of room: caches are not trimmed, and it says so", rc == 0 and "trim-caches" not in batch and "left alone" in out and "cached_apps_freezer" not in batch, f"{rc} {out}")
rc, out, batch = run(f"/dev/block/dm-5 {16 * GB} {15 * GB} {600 * 1024} 98% /data")
check("under 1 GB free: trimmed, and it says so", rc == 0 and "pm trim-caches 999G" in batch and "storage is low" in out, f"{rc} {out}")
rc, out, batch = run(f"/dev/block/dm-5 {64 * GB} {59 * GB} {4 * GB} 94% /data")
check("under 10% free of a big disk: trimmed", "pm trim-caches 999G" in batch, out)
rc, out, batch = run("")
check("free space unreadable: not trimmed", rc == 0 and "trim-caches" not in batch and "unknown" in out, f"{rc} {out}")
rc, out, batch = run(f"/dev/x {GB} {GB} 0 100% /data", "restore")
check("restore never trims", "trim-caches" not in batch, batch)
sys.exit(1 if failed else 0)
