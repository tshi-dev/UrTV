#!/usr/bin/env python3
"""4.4 «شكل التطبيقات», the apps row's three ways (the owner approved them: docs/mockups/couch-apps/ on work/433-design): A «بنرات» (each
app's TV banner, 256×144 at 1080, radius 16), B «أيقونات» (each app's icon in a rounded square, 112, 40 apart), C «مربعات ملونة» (today's
tray). Every way centred; A and B with no tray, «تكبير وظل» and the name under the focused one; an app with no banner shows its icon on its
colour; nothing bundled (the installed apps' own art, read and cached at runtime). The geometry runs on a JVM (tests/AppsLookRun.java),
the wiring is read from the code.
    python3 tests/apps_look_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


read = lambda p: p.read_text(encoding="utf-8")
main, snap, prefs, st, recv = read(HOME / "MainActivity.java"), read(HOME / "ConfigSnapshot.java"), read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "SaverReceiver.java")

if not shutil.which("javac"):
    print("SKIP the geometry: no javac here")
else:
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "FavBar.java"), str(ROOT / "tests/AppsLookRun.java")], check=True, capture_output=True)
        out = subprocess.run(["java", "-cp", tmp, "sa.urtv.home.AppsLookRun"], check=True, capture_output=True).stdout.decode().split("\n")
    look = {l.split()[1]: l.split()[2:] for l in out if l.startswith("look ")}
    check("the three ways stored as they are; 4.4's first values (frame, zoom) and anything else are today's tiles",
          look["banners"][0] == "banners" and look["icons"][0] == "icons" and all(look[k][0] == "tiles" for k in ("tiles", "frame", "zoom", "-", "nonsense")), look)
    check("«تكبير وظل» for A and B always; for the tiles only where it was chosen (zoom)",
          look["banners"][1] == look["icons"][1] == look["zoom"][1] == "1" and look["frame"][1] == look["tiles"][1] == look["-"][1] == "0", look)
    geo = {}
    for l in out:
        m = re.match(r"(\d+) (\w+) (.*)", l)
        if m and m.group(2) in ("banners", "icons", "tiles"):
            t = m.group(3).split()
            geo[(int(m.group(1)), m.group(2))] = {t[i]: float(t[i + 1]) for i in range(0, len(t) - 1, 2)}
    g = geo[(1080, "banners")]
    check("A at 1080: a 256×144 banner (16:9), radius 16, 24 apart", g["cell"] == 256 and abs(g["aspect"] - 9 / 16) < 1e-6 and g["radius"] == 16 and g["gap"] == 24, g)
    g = geo[(1080, "icons")]
    check("B at 1080: a 112 square, radius 24, 40 apart", g["cell"] == 112 and g["aspect"] == 1 and g["radius"] == 24 and g["gap"] == 40, g)
    g = geo[(1080, "tiles")]
    check("C at 1080: today's tray, 304×170 tiles, 226 high (4.4 beta 2: room for the ×1.14 focus)", g["cell"] == 304 and g["row"] == 226, g)
    for h in (1080, 720):
        for way in ("banners", "icons", "tiles"):
            g = geo[(h, way)]
            check(f"{h} {way}: a short row is centred (3 apps start well inside), a long one starts at its edge and scrolls (14)", g["start3"] > 0 and g["start14"] == 0, g)
            check(f"{h} {way}: the row leaves room for the focused one to grow (×1.14) inside its height", g["row"] >= g["cell"] * g["aspect"] * 1.14 - 1, g)
    check("A: six banners fit whole across the tray's width, centred (no tray: no side inset)", geo[(1080, "banners")]["start6"] > 0 and geo[(720, "banners")]["start6"] >= 0, geo[(1080, "banners")])
    check("B: nine icons fit whole, centred", geo[(1080, "icons")]["start9"] > 0 and geo[(720, "icons")]["start9"] > 0)
    check("A and B smaller than the tray's row; C as tall as always", geo[(1080, "banners")]["row"] < 218 and geo[(1080, "icons")]["row"] < 218)

# ---- the launcher
check("the row is centred in every way (CENTER, in a tray that fills the screen's tray width)", "dockRow.setGravity(Gravity.CENTER);" in main and "tray.setFillViewport(true);" in main)
check("A and B have no tray, no side inset; C wears the home's material as always",
      "tray.setBackground(FavBar.TILES.equals(appsLook) ? dockBand : null);" in main and "FavBar.padSide(appsLook" in main)
check("each way its own cell, height and corner (FavBar.cellW, rowH, radius); the row under it («كمّل المشاهدة») keeps the tray's cells",
      "FavBar.cellW(way, h, FavBar.shown(barSetting, favs), i)" in main and "FavBar.rowH(appsLook" in main and "o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), corner);" in main
      and "return dockCell(i, n, favs, FavBar.TILES); }" in main and "return dockCell(i, n, n, appsLook); }" in main)
check("B: the launcher icon in its square (AspectFrame's ratio 1), read only for that way, cached and sized (artCache, fitted) for a weak TV",
      "card.ratio = FavBar.aspect(FavBar.ICONS);" in main and "float ratio = 9f / 16f;" in main and 'if (FavBar.ICONS.equals(appsLook)) {' in main
      and 'a.icon = artCache.get("i:" + a.pkg);' in main and 'artCache.put("i:" + a.pkg, a.icon = fitted(ri.loadIcon(pm), false));' in main)
check("A: the TV banner (loadBanner); an app with none its own icon on a neutral tile (4.4 beta 2, the owner: never a dark tint of its colour; tests/tile_art_test.py)",
      "banner = ai.loadBanner(pm);" in main and "app.composed && !icons || icons && app.iconComposed ? 0 : TileArt.LIGHT" in main and "darken(" not in main)
check("a new way makes the row again (its art and cells); the focus «تكبير وظل» per FavBar.zoom, the name under the row",
      "if (!wayNow.equals(appsLook)) {" in main and "applyAppsLook();\n            buildApps();" in main and "appsZoom = FavBar.zoom(cfg.appsStyle);" in main
      and "placeZoomName(appsZoom && inBar ? f : null);" in main)
check("the launcher reads the three ways (and the first two) from the screensaver; an older one says nothing: today's tray",
      'java.util.Arrays.asList("zoom", "banners", "icons", "tiles").contains(v)' in snap and 'appsStyle = "frame";' in snap)
check("nothing bundled: no app's banner or icon in the app's resources", not any(re.search(r"(banner|logo)_", p.name) for p in (ROOT / "android/launcher/src/main/res").rglob("*.png")))

# ---- the setting
check("the setting: «بنرات» / «أيقونات» / «مربعات ملونة», stored banners, icons, or the tiles' first values (frame the default, zoom kept)",
      'HOME_APPS = {"banners", "icons", "tiles", "glass"};' in prefs and '{"بنرات", "أيقونات", "مربعات ملونة", "tvOS"}' in prefs and 'APPS_STORED = {"banners", "icons", "frame", "zoom", "glass"};' in prefs
      and 'sp.getString("appsStyle", "frame")' in prefs and "Prefs.appsWay(prefs.appsStyle())" in st)
check("«مربعات ملونة» keeps the tiles' own focus when they are already the way, else the ring",
      'if ("tiles".equals(v)) { if (!"tiles".equals(appsWay(appsStyle()))) sp.edit().putString("appsStyle", "frame").apply(); return; }' in prefs)
check("the presets: «شكل كنبة» → «أيقونات», «شكل تلفزيونك» → «مربعات ملونة»; a TV set to Couch before («تكبير وظل») still reads Couch",
      '{"couch", "شكل كنبة", "couch", "scenes", "glass", "icons"}' in prefs and '{"urtv", "شكل تلفزيونك", "classic", "photos", "plain", "frame"}' in prefs
      and 'presetApps(l[5], appsStyle())' in prefs)
check("saver.sh appsstyle tiles|banners|icons, and the receiver accepts them (and the first two)",
      "appsstyle tiles|banners|icons" in read(ROOT / "optimize/saver.sh") and "!Arrays.asList(Prefs.APPS_STORED).contains(appsStyle)" in recv)
sys.exit(1 if failed else 0)
