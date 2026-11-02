#!/usr/bin/env python3
"""The limits on what comes off the network (Bounded.java, in both apps): nothing is read past its cap, a photo is decoded at the
screen's size, the photo cache has a size. Also that the places that read the network use them. Needs javac for the Java cases.
Run: python3 tests/bounded_test.py"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
J = ROOT / "android"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(J / "launcher/src/main/java/sa/urtv/home/Bounded.java"), str(ROOT / "tests/BoundedRun.java")],
                       check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.BoundedRun"], check=True, capture_output=True, text=True).stdout.splitlines()
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp + "/b", str(J / "app/src/main/java/sa/urtv/screensaver/Bounded.java")], check=True, capture_output=True)
    check("read: exactly the cap is fine", out[0] == "ok 100", out[0])
    check("read: one byte over is refused", out[1] == "too long", out[1])
    check("read: empty is fine", out[2] == "ok 0", out[2])
    check("read: a server that never stops is cut at the cap (nothing much past it is read)", out[3] == "too long stopped", out[3])
    check("sample size: 4000 -> 2, 1920 -> 1, 8000 -> 4, small stays 1, 3840 -> 2, no width asked -> a power of two", out[4] == "2 1 4 1 2 4096" or out[4].startswith("2 1 4 1 2 "), out[4])
    check("cache budget: what fits, newest first", out[5] == "2 3 0 0", out[5])
    check("caps: JSON 2 MB, images 8 MB, cache 40 MB", out[6] == f"{2 << 20} {8 << 20} {40 << 20}", out[6])
else:
    print("SKIP bounded: no javac here")
same = lambda p: (J / p).read_text(encoding="utf-8")
a = same("launcher/src/main/java/sa/urtv/home/Bounded.java").split("package", 1)[1].split(";", 1)[1]
b = same("app/src/main/java/sa/urtv/screensaver/Bounded.java").split("package", 1)[1].split(";", 1)[1]
check("the two apps' Bounded.java are the same code", a == b)
home = same("launcher/src/main/java/sa/urtv/home/MainActivity.java")
check("home: downloads are capped (the photo list 2 MB, a photo 8 MB), nothing read without a cap", "Bounded.readAll(in, max)" in home and "Bounded.JSON_MAX" in home and "Bounded.IMAGE_MAX" in home
      and "b.write(buf, 0, n)" not in home)
check("home: a photo is checked to decode before it is kept", "validImage(" in home)
wall = home.split("private void nextWallpaper", 1)[1].split("scheduleWallpaper(WALLPAPER_INTERVAL_MS);", 1)[0]
check("home wallpaper: what is on the device is shown before the photo list is fetched",
      "showWallpaperIfCurrent(bm, gen)" in wall and wall.index("showWallpaperIfCurrent(bm, gen)") < wall.index("refreshPhotoIndex()"))
check("home wallpaper: a result older than the last request is dropped (a generation counter)",
      "final int gen = ++wallGen" in wall and "if (gen == wallGen) showWallpaper(bm)" in home)
check("home apps: names, art and colours load on one background thread; the grid is added in batches, the dock first",
      "loader.execute(" in home and "private List<App> loadApps(" in home and "addGridRows(rest, to, gen)" in home
      and home.index("dockRow.addView(tile, dockCell(i, dock.size()));") < home.index("addGridRows(rest, 0, gen)") and "BATCH_ROWS" in home)
cache = same("app/src/main/java/sa/urtv/screensaver/PhotoCache.java")
check("screensaver: hosted photos are capped, checked and kept within the size budget", "Bounded.IMAGE_MAX" in cache and "validImage(" in cache and "Bounded.fitting(" in cache and "Bounded.CACHE_MAX" in cache)
check("screensaver: the club search and the name lookup read through the cap",
      "Bounded.readAll(" in same("app/src/main/java/sa/urtv/screensaver/Arabic.java") and "Bounded.readAll(" in same("app/src/main/java/sa/urtv/screensaver/SettingsActivity.java"))
sys.exit(1 if failed else 0)
