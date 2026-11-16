#!/usr/bin/env python3
"""4.4 the owner: on a scene or one of our masked photos the clock stays where it is and the picture comes down a little (a zoom from the screen's top
centre, the cut-out and the glass in the digits with it, never an empty band) so more of the digits are above the scenery: the visible share goes up by
ScenePlan.MORE (0.2), up to ScenePlan.CAP (0.9); one already at CAP or more stays as it is. ScenePlan runs on a JVM over the bundled scene's real
cut-out (and, with URTV_SCENES=dir1:dir2, over downloaded packs' folders too), and over made-up skylines; the wiring is read from the source.

    python3 tests/scene_zoom_test.py      (needs javac and Pillow)"""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p): return p.read_text(encoding="utf-8")


plan = read(HOME / "ScenePlan.java")
m = re.search(r"static final float MORE = ([0-9.]+)f, CAP = ([0-9.]+)f, MAX_ZOOM = ([0-9.]+)f;", plan)
MORE, CAP, MAX = map(float, m.groups())
if not shutil.which("javac"):
    print("SKIP scene zoom: no javac here")
else:
    from PIL import Image
    dirs = [ROOT / "android/launcher/src/main/assets/scenes/saudi"] + [Path(d) for d in os.environ.get("URTV_SCENES", "").split(":") if d]
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "ScenePlan.java"), str(ROOT / "tests/SceneZoomRun.java")], check=True, capture_output=True)
        run = lambda *a: subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SceneZoomRun", *a], check=True, capture_output=True).stdout.decode("utf-8").splitlines()
        rows = []
        for d in dirs:
            for line in read(d / "scenes.tsv").splitlines():
                c = line.split("\t")
                if line.startswith("#") or len(c) < 6 or c[5] == "-":
                    continue
                fg = Image.open(d / f"{c[0]}-fg.webp").convert("RGBA")
                sw, sh = 96, max(1, round(96 * fg.height / fg.width))
                small = fg.resize((sw, sh), Image.NEAREST)
                raw = small.tobytes()
                px = [(raw[i + 3] << 24) | (raw[i] << 16) | (raw[i + 1] << 8) | raw[i + 2] for i in range(0, len(raw), 4)]
                f = Path(tmp) / f"{c[0]}.txt"
                f.write_text(f"{sw} {sh} " + " ".join(str(v) for v in px))
                rows += [l.split("\t") for l in run("scene", str(d / "scenes.tsv"), c[0], str(f))]
        bad = []
        for r in rows:
            v0, k, v1 = float(r[3]), float(r[4]), float(r[5])
            target = min(v0 + MORE, max(v0, CAP))
            if not (1 <= k <= MAX and v1 >= target - 0.005):
                bad.append(r)
        check(f"every scene with a cut-out: the visible share of the digits reaches its target (+{MORE}, up to {CAP}) with a zoom of 1 to {MAX}, cards or not",
              rows and not bad, bad or "no scene")
        print("     " + "; ".join(f"{r[1]}{' (cards)' if r[2] == 'true' else ''} {float(r[3]):.2f}->{float(r[5]):.2f} x{float(r[4]):.2f}" for r in rows))
        flat = {r[1]: (float(r[2]), float(r[3]), float(r[4])) for r in (l.split("\t") for y in ("450", "300", "700", "1080") for l in run("flat", y))}
        check("made up: scenery at the digits' middle, 50 % visible: zoomed to 70 %", abs(flat["450"][0] - 0.5) < 0.01 and flat["450"][2] >= 0.695 and flat["450"][1] > 1, flat["450"])
        check("…scenery over the whole digits, none visible: zoomed to 20 % at most up to MAX_ZOOM", flat["300"][0] == 0 and (flat["300"][2] >= 0.195 or flat["300"][1] == MAX), flat["300"])
        check("…scenery under the digits (all visible) and no scenery at all: no zoom", flat["700"][1] == 1 and flat["1080"][1] == 1, [flat["700"], flat["1080"]])

home, glass, clock, main = read(HOME / "SceneHome.java"), read(HOME / "PhotoGlass.java"), read(HOME / "SceneClock.java"), read(HOME / "MainActivity.java")
check("the scenes: the zoom from the cut-out, on the scene's thread; the photo and the cut-out zoom about the screen's top centre, the glass in the digits too",
      "final float zoom = mask == null ? 1f : zoomOf(mask, sc);" in home and "back.photo.setPivotY(0);" in home and "back.mask.setPivotY(-lp.topMargin);" in home
      and "back.clock.setZoom(zoom);" in home and "m.postScale(zoom, zoom, w / 2f, 0);" in clock)
check("the clock stays where it is: its box is ScenePlan.clockBox / PhotoGlassPlan.box, the same with or without the zoom",
      "ScenePlan.clockBox(sc, w, h, cardsShown, rtl)" in home and "clock.setScene(shown, digits, false);" in glass)
check("our photos: the same rule from the cut-out's skyline; the home's photo layer follows, the one fading out keeps its own, a layer never comes back zoomed",
      "float z = ScenePlan.zoom(line, digits, w, h);" in glass and "zoomTo.zoom(z);" in glass and "photoGlass.onZoom(this::zoomWall);" in main
      and main.index("wallFront.setScaleX(1f);") < main.index("if (photoGlass != null) photoGlass.show(bm, key,"))
check("a zoom of 1 or more about the top: never an empty band", "while (visible(sky, box, w, h, k) < target - 1e-4f && k < MAX_ZOOM)" in plan and "float k = 1f;" in plan)
sys.exit(1 if failed else 0)
