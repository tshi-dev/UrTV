#!/usr/bin/env python3
"""4.3.3 (6) seasonal photo packs (urtv-data seasonal/seasonal.json, Seasonal): only between their days, before the index's packs, the same
checks as every pack, a license required, at most PACKS_MAX, a kept one still named after its days. Runs Seasonal on a plain JVM."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


ps = (APP / "PackStore.java").read_text(encoding="utf-8")
check("the picker's list: today's seasonal packs merged before the index's, the kept copy read, fetched in the background",
      "Seasonal.merge(Seasonal.packs(ContentFile.read(c, Seasonal.NAME), f.format(new java.util.Date())), indexOnly(c))" in ps
      and "ContentFile.refresh(c, Seasonal.PATH, Seasonal.NAME, 12 * 3600_000L)" in ps)
check("a seasonal pack kept on the TV keeps its name after its days", "Seasonal.packs(ContentFile.read(c, Seasonal.NAME), null)" in ps)
check("a seasonal pack is downloaded like any other (the same download, sha256 and checks)", "static String download(Context c, PhotoPacks.Pack p)" in ps
      and "if (!PhotoPacks.valid(p)) return \"bad\";" in ps and "PhotoPacks.valid(k)" in (APP / "Seasonal.java").read_text(encoding="utf-8"))

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, *[str(APP / n) for n in ("Seasonal.java", "PhotoPacks.java", "MiniJson.java")],
                            str(ROOT / "tests/SeasonalRun.java")], capture_output=True, text=True)
        check("Seasonal compiles on a plain JVM", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.SeasonalRun"], capture_output=True, text=True).stdout
            got = dict(l.split(" ", 1) for l in out.splitlines() if " " in l)
            check("in Ramadan: the Ramadan pack only; one without a license or with a bad id never", got.get("RAMADAN") == "ramadan", out)
            check("its first and last days count", got.get("FIRSTDAY") == "ramadan LASTDAY ramadan", out)
            check("after its days: not offered", got.get("AFTER") == "[]", out)
            check("for the names: every valid one, whatever the day", got.get("ALL") == "ramadan,national-day", out)
            check("no file: nothing", got.get("NODOC") == "[]", out)
            check("merged: the seasonal first, an id that is in both once", got.get("MERGE") == "ramadan,space,sea", out)
            check("never more than PACKS_MAX, the seasonal one kept", got.get("CAP") == "12 ramadan", out)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
