#!/usr/bin/env python3
"""4.3: photo packs on the HOME too, shuffled with «صورنا»: the home's rotation order (WallShuffle, in Java), the packs the home rotates (PhotoPacks.homeSet), and the wiring:
the screensaver app's provider answers .../homepacks and serves .../homepack/ID/N read only; the home reads them and mixes them into its wallpaper.

    python3 tests/home_packs_test.py
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "WallShuffle.java"), str(ROOT / "tests/WallShuffleRun.java")], capture_output=True, text=True)
        check("it compiles", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.WallShuffleRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                if line.startswith(("PASS ", "FAIL ")):
                    print(line)
                    failed += line.startswith("FAIL ")
            check("every shuffle case ran", "ALL PASS" in out, out[-200:])
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
prov = (SAVER / "ConfigProvider.java").read_text(encoding="utf-8")
prefs = (SAVER / "Prefs.java").read_text(encoding="utf-8")
packs = (SAVER / "PhotoPacks.java").read_text(encoding="utf-8")
check("the provider answers .../homepacks (id, n, ours, rev) and opens .../homepack/ID/N read only, only for a pack the home rotates",
      '"homepacks".equals(uri.getPathSegments().get(0))' in prov and 'new String[] {"id", "n", "ours", "rev"}' in prov and '"homepack".equals(seg.get(0)) && "r".equals(mode)' in prov and "new Prefs(c).homePacks().get(id)" in prov)
check("the home's set is the screensaver's («مثل شاشة التوقف», the default) or its own, only packs that are on the TV, and «صورنا» is never the only thing left out",
      "static Map<String, Integer> homeSet(boolean same" in packs and 'sp.getBoolean("homeSame", true)' in prefs and "return on || homePacks().isEmpty();" in prefs)
check("the home's wallpaper mixes ours, the owner's and the packs' photos in a shuffled order, and chooses again when the set changes",
      "WallShuffle.of(total, wallIndex / total)[wallIndex % total]" in main and "HOME_PACK.buildUpon()" in main and "homePacksRev" in main and "packBase" in main)
sys.exit(1 if failed else 0)
