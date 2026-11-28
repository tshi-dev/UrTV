"""R-402: optimize/reset.sh on a TV whose own home is in no list and not in the journal (a G-TEN on Android 15) stopped every time at
«The stock home is not on screen»: it looked only for launcherx, tvlauncher and the journal's home. Now, with no home in the journal, it reads
the TV's own HOME apps (system apps only; never ours, the setup wizard or a Settings stand-in). A fake TV; no TV needed.

    python3 tests/reset_home_read_test.py
"""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

# HOME is the vendor's own (a name in no list), shown on screen after the Home key; the person's own launcher is installed too (not a system app)
PYSTUB = r'''#!/usr/bin/env bash
shift 2
case "$1" in
  --batch) cat > /dev/null ;;
  *query-activities*) printf '%s\n' "  priority=0 preferredOrder=0 match=0x108000" "  com.vendor.tvhome/.HomeActivity" "  sa.urtv.home/.MainActivity" \
       "  com.android.tv.settings/.FallbackHome" "  com.google.android.tungsten.setupwraith/.MainActivity" "  com.example.mylauncher/.Main" ;;
  "pm list packages -s") printf 'package:%s\n' com.vendor.tvhome com.android.tv.settings com.google.android.tungsten.setupwraith android ;;
  *resolve-activity*) echo "$HOMEPKG/.HomeActivity" ;;
  *"dumpsys activity"*) echo "mResumedActivity: ActivityRecord{x u0 $HOMEPKG/.HomeActivity t3}" ;;
  "pm list packages; echo END_OF_LIST") echo package:android; echo END_OF_LIST ;;
  "getprop ro.build.version.sdk") echo 35 ;;
esac
exit 0
'''
def run(homepkg, journal=None):
    d = Path(tempfile.mkdtemp()); tr = d / "tree"; (tr / "optimize" / "lists").mkdir(parents=True); (tr / "optimize" / "run").mkdir()
    for f in ("reset.sh", "home-journal.sh"):
        shutil.copy(ROOT / "optimize" / f, tr / "optimize")
    for f in ("tcl-safe.txt", "tcl-optional.txt"): (tr / "optimize/lists" / f).write_text("# none\n")
    (tr / "optimize/restore.sh").write_text("#!/usr/bin/env bash\necho Restored.\nexit 0\n")
    for f in ("keep.sh", "tweaks.sh", "settle.sh", "familydns.sh"):
        (tr / "optimize" / f).write_text("#!/usr/bin/env bash\nexit 0\n")
    for f in tr.glob("optimize/*.sh"): f.chmod(0o755)
    (tr / "optimize/offlist.py").write_text("")
    if journal: (tr / "optimize/run/home-switch.json").write_text(journal)
    (d / "py").write_text(PYSTUB); (d / "py").chmod(0o755)
    (d / "bin").mkdir(); (d / "bin" / "sleep").write_text("#!/bin/sh\nexit 0\n"); (d / "bin" / "sleep").chmod(0o755)
    env = dict(os.environ, PY=str(d / "py"), PATH=f"{d / 'bin'}:{os.environ['PATH']}", URTV_RUN_DIR=str(tr / "optimize/run"), HOMEPKG=homepkg)
    r = subprocess.run(["bash", str(tr / "optimize/reset.sh")], env=env, capture_output=True, text=True, cwd=tr, timeout=60)
    return r.returncode, r.stdout + r.stderr

rc, out = run("com.vendor.tvhome")
check("a vendor home in no list and no journal (G-TEN A15): read from the TV, the reset goes on", rc == 0 and "Done." in out and "not on screen" not in out, out[-400:])
rc, out = run("com.example.mylauncher")
check("a launcher the person installed (not a system app) is never taken for the stock home: the reset stops as before", rc == 1 and "not on screen" in out, out[-300:])
rc, out = run("com.android.tv.settings")
check("a Settings stand-in is never the stock home", rc == 1 and "not on screen" in out, out[-300:])
rc, out = run("com.google.android.tungsten.setupwraith")
check("the setup wizard is never the stock home", rc == 1 and "not on screen" in out, out[-300:])
rc, out = run("com.google.android.apps.tv.launcherx")
check("Google TV's home still works as before", rc == 0 and "Done." in out, out[-300:])
src = (ROOT / "optimize/reset.sh").read_text(encoding="utf-8")
check("only when the journal names no home (the journal's home stays the rule there)", 'if [ -z "$stockpkg" ]; then' in src and src.index('if [ -z "$stockpkg" ]') < src.index("stock=no"))
sys.exit(1 if failed else 0)
