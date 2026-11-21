#!/usr/bin/env python3
"""An input as a favourite (InputFav.java): «أسماء المصادر» adds it to or takes it off the favourites, the home draws it as a tile with the
input's name and OK switches to it behind the «المصادر» lock; an input the TV no longer reports keeps its place and draws nothing.
Run: python3 tests/input_fav_test.py"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

src = lambda n: (HOME / n).read_text(encoding="utf-8")
fav, main, names = src("InputFav.java"), src("MainActivity.java"), src("InputNamesActivity.java")
check("no Android in InputFav", "import android" not in fav)
check("the home builds a tile for each favourite input, from the TV's reported inputs", "InputFav.isInput(w) ? inputApp(w) : null" in main and "InputLabels.reported(this)" in main.split("private App inputApp(String entry)")[1][:600])
check("OK on it switches to that input behind the «المصادر» lock (the tuner through openLiveTv)", "openFavouriteInput(app)" in main and "guarded(ParentalPolicy.PILL_INPUTS, null, app.label" in main and "openPassthroughInput(t)" in main.split("private void openFavouriteInput")[1][:700])
check("no app menu on an input tile", "if (InputFav.isInput(app.pkg)) return true;" in main)
check("saving the favourites keeps a favourite input (it is not a package)", "isInstalled(p) || InputFav.isInput(p)" in main)
check("«أسماء المصادر» adds or takes it off from the name box, at most Dock.MAX, and tells the home", "InputFav.toggle(before, id, Dock.MAX)" in names and "new android.content.Intent(Dock.CHANGED)" in names)
en = (ROOT / "i18n/en/home.tsv").read_text(encoding="utf-8")
check("the new texts have their English", all(t in en for t in ("أضفه للمفضلة", "شيله من المفضلة", "انضاف للمفضلة", "انشال من المفضلة", "التلفزيون ما عاد يعرض هذا المصدر")))
check("FEATURES.md says where", "أضفه للمفضلة" in (ROOT / "docs/FEATURES.md").read_text(encoding="utf-8"))
if not shutil.which("javac"):
    print("SKIP input fav: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "InputFav.java"), str(ROOT / "tests/InputFavRun.java")], check=True)
    got = dict(l.split("\t") for l in subprocess.run(["java", "-cp", tmp, "sa.urtv.home.InputFavRun"], check=True, capture_output=True).stdout.decode("utf-8").splitlines())
iid = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128"
want = {"token": "@input:" + iid, "isInput token": "true", "isInput app": "false", "isInput bare prefix": "false", "idOf": iid,
        "added": "com.netflix.ninja,net.mbc.shahidTV,@input:" + iid, "has": "true", "removed": "com.netflix.ninja,net.mbc.shahidTV",
        "full": "2", "comma refused": "2", "original untouched": "2"}
for k, v in want.items():
    check(f"input fav: {k}", got.get(k) == v, f"{got.get(k)!r}, wanted {v!r}")
sys.exit(1 if failed else 0)
