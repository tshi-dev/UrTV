#!/usr/bin/env python3
"""4.4 the gate every cut-out passes before it is published (.github/tools/photo_masks.py), on made-up cut-outs (no model, no photo): a light fringe
and a blown-up stair-stepped edge are refused, a clean soft one passes; the digits' fit follows the TV's own rules (PhotoGlassPlan.box, ScenePlan.zoom);
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
b, crossing = T.box([1080] * 48, cards=True)
check("the digits as the home places them: no scenery, the start half (right in Arabic) at their usual height, not crossing",
      not crossing and b[0] >= 960 and b[1] == round(H * 0.16), b)
b, crossing = T.box([500] * 48, cards=True)
check("…a skyline at mid-height: the digits' bottom 30 % under it", crossing and b[3] == 500 + round(H * 0.26 * 0.3), b)
check("the zoom rule: a skyline covering half the digits zooms toward 75 % visible, at most 1.4",
      1.0 < T.zoom([440] * 48, [1000, 300, 1800, 580]) <= 1.4 and T.zoom([1080] * 48, [1000, 300, 1800, 580]) == 1.0)

T.people_cut = lambda rgb, alpha: False   # the finders are OpenCV's own; a made-up picture has no person
v = lambda r, a: T.judge(r, a, T.scenery_colour(r, a))[:2]
check("a clean edge that crosses the digits' lower part: FIT", v(*scene(560, 0.05))[0] == "FIT", v(*scene(560, 0.05)))
check("…a skyline too low to reach the digits: FRONT-ONLY", v(*scene(900))[0] == "FRONT-ONLY", v(*scene(900)))
tower = ((yy > 900) | ((xx > 1400) & (xx < 1440) & (yy > 420))).astype(np.float32)   # low ground and one thin tower through the digits
check("…only a sliver of the digits crossed (one thin tower): FRONT-ONLY", v(rgb, tower)[0] == "FRONT-ONLY", v(rgb, tower))
check("…no sky along the top: REJECT", v(rgb, np.ones((H, W), np.float32))[0] == "REJECT")
T.people_cut = lambda rgb, alpha: True
check("…the edge through a face or a person: REJECT", v(*scene(560)) == ("REJECT", "the edge cuts through a face or a person"))

tool = (ROOT / ".github/tools/photo_masks.py").read_text(encoding="utf-8")
check("only FIT cut-outs, reviewed by eye, are published; the gate goes with them (gate.json); a reviewed one that fails stops the run",
      'if verdict != "FIT": continue' in tool and 'if ok is not None and f"{name}/{stem}" not in ok: continue' in tool and '"gate.json"' in tool
      and "reviewed cut-out(s) that no longer pass the gate" in tool)
check("the depth model is pinned by its sha256 and never run on a TV", 'MODEL_SHA256 = "' in tool and "MODEL_SHA256" in tool.split("def depth")[0])
sys.exit(1 if failed else 0)
