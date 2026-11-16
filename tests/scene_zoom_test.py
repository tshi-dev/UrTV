#!/usr/bin/env python3
"""4.4 the owner: on a scene or one of our masked photos the clock stays where it is and the picture comes down a little (a zoom from the screen's top
centre, the cut-out and the glass in the digits with it, never an empty band) so more of the digits are above the scenery, by the owner's bands: under
60 % visible to 75 %, 60 to 70 % to 80 %, 70 to 85 % to 85 %, 85 % or more as it is. ScenePlan runs on a JVM over the bundled scene's real
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
BANDS = [tuple(map(float, b)) for b in re.findall(r"\{([0-9.]+)f, ([0-9.]+)f\}", re.search(r"BANDS = \{(.*?)\};", plan).group(1))]
MAX = float(re.search(r"MAX_ZOOM = ([0-9.]+)f;", plan).group(1))
check("the owner's bands: under 60 % to 75 %, 60-70 % to 80 %, 70-85 % to 85 %, from 85 % as it is", BANDS == [(0.6, 0.75), (0.7, 0.8), (0.85, 0.85)], BANDS)


def target(v):
    for lo, t in BANDS:
        if v < lo: return t
    return v
if not shutil.which("javac"):
    print("SKIP scene zoom: no javac here")
else:
    from PIL import Image
    dirs = [ROOT / "android/launcher/src/main/assets/scenes/saudi"] + [Path(d) for d in os.environ.get("URTV_SCENES", "").split(":") if d]
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(HOME / "ScenePlan.java"), str(ROOT / "tests/SceneZoomRun.java")], check=True, capture_output=True)
        run = lambda *a: subprocess.run(["java", "-cp", tmp, "sa.urtv.home.SceneZoomRun", *a], check=True, capture_output=True).stdout.decode("utf-8").splitlines()
        rows = []
        # 4.4 the bundled Riyadh has no cut-out (the owner's legibility rule left it with the clock in front): a made-up scene stands in, a hilly
        # skyline across the digits' lower half, so the zoom bands are still checked on a real file read the app's way
        syn = Path(tmp) / "syn"; syn.mkdir()
        (syn / "scenes.tsv").write_text("# id\tname\tname_en\tdigits_top\tdigits_bottom\tmask x,y,w,h\tdark\tcredit\ttags\n"
                                        "hills\tتلال\tHills\t266\t542\t0,360,1920,720\t0\ttest\t\n", encoding="utf-8")
        import math
        fgi = Image.new("RGBA", (1920, 720), (0, 0, 0, 0))
        px = fgi.load()
        for x in range(1920):
            top = int(110 + 60 * math.sin(x / 140) + 30 * math.sin(x / 37))
            for y in range(max(0, top), 720): px[x, y] = (40, 50, 60, 255)
        fgi.save(syn / "hills-fg.webp", "WEBP", lossless=True)
        dirs.append(syn)
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
            want = target(v0)
            if not (1 <= k <= MAX and v1 >= want - 0.005):
                bad.append(r)
        check(f"every scene with a cut-out: the visible share of the digits reaches its band's target with a zoom of 1 to {MAX}, cards or not",
              rows and not bad, bad or "no scene")
        print("     " + "; ".join(f"{r[1]}{' (cards)' if r[2] == 'true' else ''} {float(r[3]):.2f}->{float(r[5]):.2f} x{float(r[4]):.2f}" for r in rows))
        # the digits' box is 300..600: scenery at y is (y - 300) / 300 visible
        ys = {"450": 0.5, "495": 0.65, "534": 0.78, "570": 0.9, "700": 1.0, "1080": 1.0}
        flat = {r[1]: (float(r[2]), float(r[3]), float(r[4])) for r in (l.split("\t") for y in ys for l in run("flat", y))}
        check("made up, each band: 50 % → 75 %, 65 % → 80 %, 78 % → 85 % (zoomed); 90 %, all visible and no scenery: as they are", all(
              abs(flat[y][0] - v) < 0.01 and (flat[y][2] >= target(v) - 0.005 and flat[y][1] > 1 if v < 0.85 else flat[y][1] == 1) for y, v in ys.items()), flat)

home, glass, clock, main = read(HOME / "SceneHome.java"), read(HOME / "PhotoGlass.java"), read(HOME / "SceneClock.java"), read(HOME / "MainActivity.java")
check("4.4 the scenes (the owner: the clock always in the same place): no zoom at runtime, the scene's frame is it (the masks gate chose it); the photo, "
      "its cut-out and the glass in the digits take that one framing",
      "final float zoom = 1f;" in home and "float[] f = ScenePlan.frame(sc.frame, w, h, zoom);" in home and "v.setTranslationX(f[1] + l * (f[0] - 1));" in home
      and "back.clock.setFrame(sc.frame, zoom);" in home and "float[] f = ScenePlan.frame(crop, w, h, zoom);" in clock and "ScenePlan.zoom(" not in home)
check("the clock stays where it is: its box is ScenePlan.clockBox / ScenePlan.fixedBox, the same on every scene and photo",
      "ScenePlan.clockBox(sc, w, h, cardsShown, rtl)" in home and "clock.setScene(glass, digits, pale(digits));" in glass and "int[] digits = ScenePlan.fixedBox(w, h, cardsShown, rtl);" in glass)
check("our photos: their index's frame (the photo, its cut-out, the glass, and the home's photo layer); the one fading out keeps its own, a layer never comes back framed",
      "float[] f = ScenePlan.frame(crop, w, h, 1f);" in glass and "zoomTo.frame(crop, 1f);" in glass and "ScenePlan.zoom(" not in glass
      and "float[] f = ScenePlan.frame(crop, dm.widthPixels, dm.heightPixels, k);" in main and "wallFront.setTranslationX(0);" in main and "photoGlass.onZoom(this::zoomWall);" in main
      and main.index("wallFront.setScaleX(1f);") < main.index("if (photoGlass != null) photoGlass.show(bm, key,"))
check("a zoom of 1 or more about the top: never an empty band", "while (visible(sky, box, w, h, k) < target - 1e-4f && k < MAX_ZOOM)" in plan and "float k = 1f;" in plan)
sys.exit(1 if failed else 0)
