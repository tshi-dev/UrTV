#!/usr/bin/env python3
"""4.5 «زجاجي» (the owner approved docs/mockups/glass/): the fourth way of the apps row, a liquid-glass bar of squircle icons, its size (50 to
150 %) and rows (1 to 3) as settings; «كل التطبيقات» as a glass window with a search and five columns; «تغيير الأيقونة». The geometry runs on a
JVM (tests/GlassDockRun.java); the rules are read from the code: the icons and the glass off the main thread, keys given back, the parts of
tvOS Launcher credited (Apache-2.0), nothing exported, Arabic first.    python3 tests/glass_dock_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
main, bar, art, grid, allapps, editor, store, opts = (read(HOME / f) for f in (
    "MainActivity.java", "GlassBar.java", "GlassArt.java", "DockGrid.java", "AllAppsActivity.java", "IconEditorActivity.java", "IconStore.java", "AppOptions.java"))
snap = read(HOME / "ConfigSnapshot.java")
manifest = read(ROOT / "android/launcher/src/main/AndroidManifest.xml")

if not shutil.which("javac"):
    print("SKIP the geometry: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FavBar.java"), str(ROOT / "tests/GlassDockRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.GlassDockRun"], check=True, capture_output=True).stdout.decode().split("\n")
    first = lambda tag: next(l.split()[1:] for l in out if l.startswith(tag + " "))
    check("«زجاجي» is its own way, with «تكبير وظل» (bigger, no ring, the name alone)", first("look") == ["glass", "1"], first("look"))
    k = {l.split()[1]: float(l.split()[2]) for l in out if l.startswith("k ")}
    check("the size: 50 to 150 % as a factor; anything else 100 %", k["50"] == 0.5 and k["70"] == 0.7 and k["100"] == 1 and k["150"] == 1.5 and k["40"] == k["160"] == k["x"] == k["-"] == 1, k)
    rows = {l.split()[1]: int(l.split()[2]) for l in out if l.startswith("rows ")}
    check("the rows: 1 to 3; anything else one", rows == {"1": 1, "2": 2, "3": 3, "0": 1, "4": 1, "x": 1}, rows)
    geo = {}
    for l in out:
        if l.startswith("geo "):
            t = l.split()
            geo[(int(t[1]), t[2], int(t[3]))] = {t[i]: int(t[i + 1]) for i in range(4, len(t) - 1, 2)}
    g = geo[(1080, "100", 1)]
    check("at 1080 and 100 %: a 96 squircle, 28 apart, 22 of glass round them, the corner about a quarter", g["cell"] == 96 and g["gap"] == 28 and g["pad"] == 22 and 20 <= g["radius"] <= 26, g)
    check("the owner's three: 70 % (two rows) smaller, 150 % bigger, in the same proportions",
          geo[(1080, "70", 2)]["cell"] == 67 and geo[(1080, "150", 1)]["cell"] == 144 and geo[(1080, "150", 1)]["gap"] == 42, (geo[(1080, "70", 2)], geo[(1080, "150", 1)]))
    check("at 100 % one row: ten favourites and «كل التطبيقات» fit whole in the tray (no scroll), centred", geo[(1080, "100", 1)]["w11"] <= geo[(1080, "100", 1)]["tray"]
          and geo[(720, "100", 1)]["w11"] <= geo[(720, "100", 1)]["tray"], geo[(1080, "100", 1)])
    check("two rows halve the bar's width, three a third (6 apps: 3 then 2 columns)", geo[(1080, "70", 2)]["w6"] < geo[(1080, "70", 1)]["w6"] and geo[(1080, "100", 3)]["w6"] < geo[(1080, "100", 2)]["w6"])
    tall = [(h, pct, r, g["row11"] + g["bottom"]) for (h, pct, r), g in geo.items() if g["row11"] + g["bottom"] > h * 0.62]
    check("every size and rows at 1080 and 720: the bar and the space under it stay under 62 % of the screen's height (the clock, the greeting and the cards keep their place)", not tall, tall)
    check("150 % in three rows would not fit: it shows two (FavBar.glassRowsFit); 100 % keeps its three", geo[(1080, "150", 3)]["row11"] == geo[(1080, "150", 2)]["row11"]
          and geo[(1080, "100", 3)]["row11"] > geo[(1080, "100", 2)]["row11"] and geo[(720, "150", 3)]["row11"] == geo[(720, "150", 2)]["row11"])
    check("the focused one grows inside the bar's room (ZOOM_SCALE about its centre: room kept above and below)", all(g["grow"] >= (g["cell"] * 0.14) / 2 for g in geo.values()))
    side = first("side")
    check("◀ ▶ in rows never wrap to the next row (left to right and right to left); one row is the ordinary step", side == ["-1", "2", "-1", "4", "5"], side)
    ud = first("updown")
    check("▲ ▼ inside the grid: the one under it, the nearest in a shorter last row, out of the grid -1 (the home's own up and down)", ud == ["4", "4", "1", "-1", "-1", "-1"], ud)
    cols = first("cols")
    check("the columns: row by row, the first row full (11 in 2 rows: 6 then 5), never an empty row", cols == ["11", "6", "4", "1", "2", "1", "1"], cols)

# ---- the setting reaches the home
check("the launcher reads «زجاجي», glass_size and glass_rows (an older screensaver: 100 %, one row)",
      'FavBar.GLASS.equals(v)' in snap and 'getColumnIndex("glass_size")' in snap and 'getColumnIndex("glass_rows")' in snap and 'glassSize = "100", glassRows = "1"' in snap)
check("a new size or rows makes the bar again (the rows as many as fit); the way's change as before",
      "FavBar.glassRowsFit(getResources().getDisplayMetrics().heightPixels, kNow, FavBar.glassRows(cfg.glassRows))" in main and
      "boolean glassNew = FavBar.GLASS.equals(wayNow)" in main and "glassK = kNow;" in main and "if (glassNew) {\n            applyAppsLook();\n            buildApps();" in main)
check("the bar: DockGrid draws GlassBar round the icons only and lays them out in rows; the tray has no band in «زجاجي»",
      "dockRow = new DockGrid(this);" in main and "dockRow.glass(glassBar, glassRowsN" in main and "glass.draw(c);" in grid and "FavBar.glassCellAt(i, n, rows)" in grid)

# ---- R1: off the main thread
loadapps = main[main.index("private List<App> loadApps("):main.index("/** One app's name, art and colour")]
check("R1: each icon's squircle is made only from loadApps (the loader's thread; appOf also runs on the main thread for one app), cached by package and the person's icon",
      "if (FavBar.GLASS.equals(appsLook)) glassIcon(app, ri, pm);" in loadapps and "GlassArt.appIcon(ri.loadIcon(pm), size)" in loadapps
      and '"g:" + a.pkg + ":" + IconStore.stamp(this, a.pkg)' in loadapps and main.count("glassIcon(") == 2)
check("R1: the two glass screens take the look from the home (no read of the screensaver's settings) and read the backdrop on their worker",
      all("getIntent().getStringExtra(LOOK)" in f and "Bitmap photo = GlassSource.backdrop(this);" in f and "GlassSource.light(" not in f for f in (allapps, editor)))
check("R1: the bar's picture is read on the loader (GlassSource's backdrop, blurred once when the photo changed), and again with a new photo",
      "loader.execute(() -> {\n            Bitmap b = GlassSource.backdrop(this);" in main and main.count("readGlassBackdrop();") >= 3)
check("no live blur anywhere in the glass (no RenderEffect, no blur filter): the backdrop is drawn", not re.search(r"RenderEffect|BlurMaskFilter|setRenderEffect", bar + art + grid + allapps + editor) and "drawBitmap(backdrop" in bar)
check("R1: «كل التطبيقات» and the editor read files, icons and renders on their own worker",
      "worker.execute(" in allapps and "GlassArt.appIcon(it.ri.loadIcon(pm), size)" in allapps and editor.count("worker.execute(") >= 4
      and "IconStore.write(this, pkg, GlassArt.render(" in editor)

# ---- R3: keys
mk = editor[editor.index("private boolean moveKey("):editor.index("private void setMoving(")]
check("R3: the editor's preview takes the arrows only while it moves the image (OK on it); every other key, and every key otherwise, goes on",
      "if (!moving) return false;" in mk and "if (!arrow) return false;" in mk)
check("«كل التطبيقات» in rows: ◀ ▶ by FavBar.glassSideStep, ▲ ▼ by glassUpDown, «كل التطبيقات» up and down as the last favourite",
      "FavBar.glassSideStep(index, row.getChildCount()" in main and "FavBar.glassUpDown(i, n, dockRow.rows(), false)" in main and "allAppsCard.setNextFocusUpId(topId);" in main)

# ---- «كل التطبيقات» and the editor
check("«كل التطبيقات»: the home's list (catalogue, kids mode's allowed apps, the given names), a search, five columns; the app opens through its own tile (the family lock asks there)",
      "Dock.catalogue(this)" in allapps and "ParentalPolicy.visible(family" in allapps and "AppNames.label(" in allapps and "static final int COLS = 5;" in allapps
      and "new EditText(this)" in allapps and "c.performClick();" in main and "data.getStringExtra(AllAppsActivity.PKG)" in main)
check("the editor: storage, USB or the app's own icon; «تلقائي» and 13 colours; square, rounded, circle; zoom and move; a preview; «حفظ» and «إلغاء»",
      all(w in editor for w in ('"من التخزين"', '"من USB"', '"الأيقونة الأصلية"', '"تلقائي"', '"مربع"', '"مربع بحواف دائرية"', '"دائرة"', '"تكبير"', '"تصغير"', '"حفظ"', '"إلغاء"'))
      and editor.count("0xFF") - editor.count("0xFF2C2C2E") >= 13 and len(re.search(r"COLOURS = \{(.*?)\};", editor, re.S).group(1).split(",")) == 14)
check("«تغيير الأيقونة» in the app's options: «زجاجي» only, never in kids mode; the icon is kept in the home's own files (IconStore), the app's own untouched",
      "if (!kids && glassWay()) o.icon = () -> editIcon(pkg);" in main and 'ICON_ROW = S.n("تغيير الأيقونة")' in opts and "getFilesDir(), \"icons\"" in store)
check("the images are asked for only when the person chooses them (READ_MEDIA_IMAGES on 13+, else the old read permission)",
      'android.permission.READ_MEDIA_IMAGES' in manifest and "requestPermissions(new String[] {readPermission()}" in editor)
check("the two screens are the home's only (not exported)", re.search(r'android:name="\.AllAppsActivity"\s+android:exported="false"', manifest) and re.search(r'android:name="\.IconEditorActivity"\s+android:exported="false"', manifest))

# ---- the credit (Apache-2.0)
notices = read(ROOT / "THIRD_PARTY_NOTICES.md")
check("tvOS Launcher credited: its copyright and the Apache-2.0 notice in each adapted file, what was changed, and a section in THIRD_PARTY_NOTICES.md",
      all("Copyright 2026 techgenius" in f and "Apache License, Version 2.0" in f and "Changed by UrTV" in f for f in (art, bar, editor))
      and "## tvOS Launcher" in notices and "Copyright 2026 techgenius" in notices)
# our own mockups of the «tvOS» manner (docs/mockups/settings-style, tvos-preset: rendered from our own pages, their READMEs say so) are named after it
check("nothing of tvOS Launcher's images in the apps or the pages (no file named after it outside our own mockups)",
      not any("tvos" in p.name.lower() for p in ROOT.rglob("*") if ".git" not in p.parts and "node_modules" not in p.parts and "mockups" not in p.parts))

# ---- the words
new = allapps + editor + art + bar + grid + store
check("no «·» or «•» separators in the new screens' words", "·" not in new and "•" not in new)
sys.exit(1 if failed else 0)
