"""optimize/dock.sh: the favourites' order is said as the TV shows it, the first app on the right (R-378).

The row reads like Arabic (docs/FEATURES.md: «أول تطبيق مفضّل على أقصى اليمين»), but dock.sh's help said «left to right»
for `show` and `set`, so an agent read the row back reversed and set it reversed. The help, `show` and docs/SETTINGS.md now
say the first is the row's start: the rightmost in Arabic. A fake tv.py; no TV needed.

    python3 tests/dock_order_test.py
"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
bad = []
def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        bad.append(name)

src = (ROOT / "optimize/dock.sh").read_text(encoding="utf-8")
helps = [l for l in src.splitlines()[:45] if re.match(r"#\s+optimize/dock\.sh (show|set)\s{2,}", l)]
check("the help of show and set never says «left to right»", helps and not any("left to right" in l for l in helps), helps)
check("the help says the first app is on the right (in Arabic)", any("FIRST app is on the RIGHT" in l for l in src.splitlines()[:45]))
check("docs/SETTINGS.md says the same", "the first at the row's start: the rightmost on the TV in Arabic" in (ROOT / "docs/SETTINGS.md").read_text(encoding="utf-8"))

t = Path(tempfile.mkdtemp()); (t / "optimize").mkdir()
shutil.copy(ROOT / "optimize/dock.sh", t / "optimize")
(t / "py").write_text('#!/bin/bash\necho \'Broadcast completed: result=0, data="dock: com.a,com.b,com.c"\'\n'); (t / "py").chmod(0o755)
r = subprocess.run(["bash", str(t / "optimize/dock.sh"), "show"], env=dict(os.environ, PY=str(t / "py")), capture_output=True, text=True)
lines = r.stdout.splitlines()
check("show: the order line as before (check-home.sh reads it), then which end is first",
      r.returncode == 0 and lines[0] == "dock: com.a,com.b,com.c" and "rightmost" in lines[1], r.stdout + r.stderr)
sys.exit(1 if bad else 0)
