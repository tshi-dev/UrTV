#!/usr/bin/env python3
"""4.4 the gate every cut-out passes before it is published (.github/tools/photo_masks.py), on made-up cut-outs (no model, no photo): a light fringe
and a blown-up stair-stepped edge are refused, a clean soft one passes; the clock is fixed (CLOCK) and the photo gets a frame that puts its scenery under it by the owner's rule;
floating pieces and a cut through a person are refused; only FIT is published, and the reviewed list is kept honest."""
import importlib.util, sys
from pathlib import Path

import cv2
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("photo_masks", ROOT / ".github/tools/photo_masks.py")
T = importlib.util.module_from_spec(spec); spec.loader.exec_module(T)
failed = 0


def check(what, ok, got=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else f"  -> {got}"))
    failed += 0 if ok else 1


W, H = T.W, T.H
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)


def scene(top_at, slope=0.35):
    """a slanted skyline (a hill) whose top is about top_at: alpha soft over 1.5 px, a sky-blue sky and a dark brown scenery"""
    line = top_at + (xx - W / 2) * slope
    alpha = np.clip((yy - line) / 1.5 + 0.5, 0, 1)
    rgb = np.where(alpha[..., None] > 0.5, np.array([60, 40, 30]), np.array([170, 200, 235])).astype(np.uint8)
    return rgb, alpha


rgb, alpha = scene(520)
F = T.scenery_colour(rgb, alpha)
clean = T.edge_scores(rgb, alpha, F)
check("a clean soft edge in the scenery's own colour: no fringe, no staircase, no islands or holes",
      clean["halo"] < 1 and clean["jagged"] < T.JAGGED_MAX and clean["islands"] == 0 and clean["holes"] == 0, clean)
fringe = T.edge_scores(rgb, np.clip((yy - (520 + (xx - W / 2) * 0.35)) / 4 + 0.5, 0, 1), rgb)   # the photo's colours kept at a wide edge: sky in it
check("…the photo's own colour along a soft edge (the old cut-outs): a light fringe on a dark background, refused", fringe["halo"] > T.HALO_MAX, fringe)
blocky = cv2.resize((alpha[::4, ::4] > 0.5).astype(np.float32), (W, H), interpolation=cv2.INTER_NEAREST)
st = T.staircase(blocky)
check("…a mask made small and blown up: a stair-stepped edge, refused", st > T.JAGGED_MAX and T.staircase(alpha) < T.JAGGED_MAX, (st, T.staircase(alpha)))
isl = alpha.copy(); isl[100:140, 300:360] = 1; isl[200:230, 900:950] = 1; isl[150:170, 1500:1540] = 1
check("…three pieces floating in the sky: a broken edge", T.edge_scores(rgb, isl, F)["islands"] == 3)

line = T.skyline(alpha)
check("the skyline: the scenery's top in each of the 48 columns, px of 1080", len(line) == 48 and abs(line[24] - 520) <= 8 and line[0] < line[47], line[:3])
tool = (ROOT / ".github/tools/photo_masks.py").read_text(encoding="utf-8")
B = T.CLOCK["clean"]
check("the owner: the clock always has the same place; the clean one is his Riyadh place (digits 266 to 542, 718 wide, centred)",
      B == [601, 266, 1319, 542] and 'r[3] = r[4] = r[9] = "-"' in tool, B)
fr = T.search(T.tops((yy > 440).astype(np.float32)), B)
check("the photo moves instead: a skyline over half the digits gets a frame (zoom 1 to 1.4) that puts it under them by the rule",
      fr and 1372 <= fr[2] < W and fr[2] % 2 == 0 and fr[0] % 2 == 0 and fr[1] % 2 == 0 and T.place_ok(T.tops((yy > 440).astype(np.float32)), B, fr), fr)
check("…one already under them keeps the whole photo; one the frame cannot lift to the digits finds none",
      T.search(T.tops((yy > 510).astype(np.float32)), B) is None and T.search(T.tops((yy > 1000).astype(np.float32)), B) is False)
x_, y_, w_, h_ = T.frame_of([100, 60, 1600])
check("…a frame is the part of the photo shown full-screen: h = w 9/16, the photo and its cut-out the same", (w_, h_) == (1600, 900)
      and T.framed(np.zeros((H, W), np.float32), [100, 60, 1600]).shape == (H, W) and T.screen_tops(np.full(W, 600), [100, 60, 1600], 0, 1)[0] == 648)

T.people_cut = lambda rgb, alpha: False   # the finders are OpenCV's own; a made-up picture has no person
v = lambda r, a: T.judge(r, a, T.scenery_colour(r, a))[:2]
check("a clean edge that crosses the digits' lower part: FIT", v(*scene(560, 0.05))[0] == "FIT", v(*scene(560, 0.05)))
check("…a skyline too low to reach the digits: FRONT-ONLY", v(*scene(900))[0] == "FRONT-ONLY", v(*scene(900)))
tower = ((yy > 900) | ((xx > 1400) & (xx < 1440) & (yy > 420))).astype(np.float32)   # low ground and one thin tower through the digits
check("…only a sliver of the digits crossed (one thin tower): FRONT-ONLY", v(rgb, tower)[0] == "FRONT-ONLY", v(rgb, tower))
check("…no sky along the top: REJECT", v(rgb, np.ones((H, W), np.float32))[0] == "REJECT")
T.people_cut = lambda rgb, alpha: True
T.people_cut = lambda rgb, alpha: False
tower = ((yy > 600) | ((xx > 1395) & (xx < 1415) & (yy > 300))).astype(np.float32)   # ground under the digits, a tower up through one of them
r, _ = scene(560, 0.05)
tower = ((yy > 560) | ((xx > 955) & (xx < 975) & (yy > 300))).astype(np.float32)   # ground under the digits, a tower up through the clock's middle
check("the owner's rule: a tower through a digit (more than a quarter of it) or up to the colon: not behind the clock",
      not T.legible(T.tops(tower), B)[0] and T.legible(T.tops(scene(600, 0.0)[1]), B)[0])
g = T.glyphs([1000, 300, 1720, 580])
check("…the colon is looked at where «١٢:٣٠» and «٤:٥٨» put it", sum(1 for x in g if x[2]) == 2 and len(g) == 9)
tex = rgb.copy(); tex[(alpha > 0.5) & ((xx.astype(int) // 7 + yy.astype(int) // 5) % 3 == 0)] = (200, 160, 120)   # a scenery with detail
Ft = T.scenery_colour(tex, alpha)
check("…a cut-out a few px off the photo (a doubled edge): refused", not T.aligned(tex, alpha, np.roll(Ft, 3, axis=1))[0] and T.aligned(tex, alpha, Ft)[0],
      (T.aligned(tex, alpha, np.roll(Ft, 3, axis=1)), T.aligned(tex, alpha, Ft)))
tall = ((yy > 1000) | ((np.abs(xx - 960) < 60) & (yy > 300))).astype(np.float32)   # low ground and a wide tower up through the clock
r2 = rgb.copy(); r2[tall > 0.5] = (60, 40, 30)
own = T.judge(r2, tall, T.scenery_colour(r2, tall), True)
check("owner_ok (the owner's own picture: scenes.tsv tag, photo_masks_owner_ok.txt): the legibility rule is waived at the clean place, nowhere else",
      own[0] == "FIT" and own[4] == ["clean"] and T.judge(r2, tall, T.scenery_colour(r2, tall), False)[0] == "FRONT-ONLY", (own[:2], own[4]))
check("…the alignment and edge checks still apply to it", not T.judge(tex, alpha, np.roll(Ft, 3, axis=1), True)[0] == "FIT")
T.people_cut = lambda rgb, alpha: True
check("…the edge through a face or a person: REJECT", v(*scene(560)) == ("REJECT", "the edge cuts through a face or a person"))

tool = (ROOT / ".github/tools/photo_masks.py").read_text(encoding="utf-8")
check("only FIT cut-outs, reviewed by eye, are published; the gate goes with them (gate.json); a reviewed one that fails stops the run",
      'if verdict != "FIT": continue' in tool and 'if ok is not None and f"{name}/{stem}" not in ok: continue' in tool and '"gate.json"' in tool
      and "reviewed cut-out(s) that no longer pass the gate" in tool)
check("the depth model is pinned by its sha256 and never run on a TV", 'MODEL_SHA256 = "' in tool and "MODEL_SHA256" in tool.split("def depth")[0])
sys.exit(1 if failed else 0)
