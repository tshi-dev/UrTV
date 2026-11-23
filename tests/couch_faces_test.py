#!/usr/bin/env python3
"""4.4 beta 4 (the owner: «كنبة» as couch-tv's own design, «take all the icons»): couch-tv's app faces for the «أيقونات» way. One table
(CouchFaces.FACES: package, face); every face it names is a file in assets/couch-faces and every file is named (so one line and one file take
a brand away); a face is used only for an installed app (appOf runs for the apps on the TV), before TileArt, drawn full-bleed; the MIT notice
and the marks are in THIRD_PARTY_NOTICES.md.   python3 tests/couch_faces_test.py"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = ROOT / "android/launcher/src/main/java/sa/urtv/home"
D = ROOT / "android/launcher/src/main/assets/couch-faces"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

src = (H / "CouchFaces.java").read_text(encoding="utf-8")
main = (H / "MainActivity.java").read_text(encoding="utf-8")
rows = re.findall(r'\{"([a-zA-Z0-9_.]+)", "([a-z0-9]+)"\}', src)
files = {f.stem for f in D.glob("*.png")}
named = {f for _, f in rows}
check("one table: package and face, each package once", len(rows) > 20 and len({p for p, _ in rows}) == len(rows), len(rows))
check("every face the table names is a file, and every file is named (one line and one file take a brand away)", named == files, (named ^ files))
check("the faces are small (each under 40 KB, all under 1 MB)", all(f.stat().st_size < 40_000 for f in D.glob("*.png")) and sum(f.stat().st_size for f in D.glob("*.png")) < 1_000_000)
icons = main[main.index('Drawable q = artCache.get("q:" + a.pkg);'):main.index("if (q != null) { a.icon = q; a.iconComposed = true; }")]
check("«أيقونات»: an installed app's face first (appOf runs only for the apps on the TV), else TileArt of its own icon; drawn full-bleed as a composed square",
      "String face = q == null ? CouchFaces.face(a.pkg) : null;" in icons and "getAssets().open(CouchFaces.asset(face))" in icons
      and icons.index("CouchFaces.face(") < icons.index("composeTile(sharpIcon(ri, pm), side, side)") and "if (q == null) {" in icons)
notice = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
check("the notice: couch-tv's MIT licence covers the faces, the marks are their owners', shown only to launch the person's installed app",
      "assets/couch-faces/" in notice and "Copyright (c) 2026 Seagey" in notice and "trademarks" in notice and "CouchFaces" in notice)
sys.exit(1 if failed else 0)
