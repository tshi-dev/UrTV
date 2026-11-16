#!/usr/bin/env python3
"""4.4 «الساعة: زجاجية خلف الصورة»: the cut-out (the scenery in front of the clock, the sky transparent) of each of our photos and horizons, made
OFFLINE (here, in CI, or on a computer), never on the TV, and admitted only through a gate.

How a cut-out is made: a depth model (Depth-Anything-V2-Small, Apache-2.0, run with onnxruntime; its file is checked against MODEL_SHA256) finds
the sky (the farthest, reached from the top, or seen through an opening such as a tower's); a band along the edge is decided by colour, depth and
height (the nearest of thousands of sure-sky and sure-scenery samples); the matte is a guided filter on the photo at twice its size (it follows
branches and towers, no stair steps), made firm and feathered about 1 px; a piece floating in the sky (a cloud) is sky. The edge's colour is the
scenery's own, carried out from its solid inside, so no sky colour is left in it: no light or dark fringe on any background (the glass, a dimmed
photo). The TV draws it as 8-bit alpha, filtered, at the photo's size.

The gate, for every photo (OUT/gate.json, one line each, and the verdict):
  edge:  halo (the edge's light or dark fringe over a dark plate, against the scenery's own colour), jagged (the edge's length at 2x against the
         same edge smoothed), islands (scenery floating in the sky) and holes (small sky holes in the scenery);
  fit:   the owner: «the clock always has the same position across our images». The clock is a fixed box (CLOCK: the clean one is the owner's
         Riyadh place, digits 266 to 542, centred); the photo moves instead: a frame [x, y, w] (the part of the photo shown full-screen,
         h = w 9/16, zoom 1 to 1.4, the photo and its cut-out alike) is searched, the least zoom first, so that the scenery crosses the digits'
         lower part (not a sliver) and covers at most a quarter of any digit from its base, never the colon (the owner's rule);
  people: a face or a person the edge cuts through.
  FIT: clean and crossing well: published. FRONT-ONLY: clean, but the scenery does not cross the digits well (or never: «الحرمين»): the clock in
  front. REJECT: a fringe, a jagged or broken edge, a cut through a face or a person, or no sky found: the clock in front.
The owner's own pictures (scenes.tsv tag owner_ok, photos in photo_masks_owner_ok.txt) keep their depth at the fixed clock, through their own
frame if scenes.tsv gives one: only the edge, halo and alignment are checked there. «الحرمين» is never cut (the owner: no clock behind the Kaaba or the Prophet's Mosque), and the person's own photos are not ours to cut.

    python3 .github/tools/photo_masks.py OUT [--sets photos,nature,sea,space,ksa-cities] [--work DIR] [--all] [--only set/name,...]
    python3 .github/tools/photo_masks.py OUT --scenes DIR      (a horizons pack: DIR/scenes.tsv and DIR/<id>.jpg; writes OUT/<id>-fg.webp)

Only FIT cut-outs named in photo_masks_ok.txt (checked by eye) are written; --all writes every FIT one and OUT/sheet.jpg and OUT/edges/ to look at.
Writes OUT/<set>/<name>-fg.webp (cropped to its box, at most 180 KB at 1080), OUT/<set>/index.json:
{"v": 1, "set": ..., "files": {"<name>": {"sha256", "bytes", "box": [x, y, w, h], "skyline": [48 ints: the scenery's top in each 40 px column, px of 1080],
"frame": [x, y, w] (absent: the whole photo), "depth": [the layouts whose fixed clock it is drawn behind]}}}; a horizons pack's scenes.tsv gets
digits_top, digits_bottom and digits_x "-" (not read), and the columns "frame x,y,w" and "depth".
and OUT/gate.json: {"v": 1, "photos": {"<set>/<name>": {"verdict", "why", "scores"}}, "counts": {"<set>": {"FIT", "FRONT-ONLY", "REJECT"}}}.
The packs' zips come from this repository's releases (photos/packs.json, their sha256 checked); "photos" is the hosted set in photos/.
Needs numpy, scipy, opencv-python-headless<5, Pillow and onnxruntime; the model is downloaded once into --work."""
import argparse, hashlib, io, json, sys, urllib.request, zipfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter1d, label

ROOT = Path(__file__).resolve().parents[2]
NEVER = {"haramain"}            # the owner: never behind the Kaaba or the Prophet's Mosque
W, H, COLS = 1920, 1080, 48     # the work size (the TV's), and the skyline's columns (40 px each)
MAX_FG = 180 * 1024
REVIEWED = Path(__file__).with_name("photo_masks_ok.txt")
OWNER_OK = Path(__file__).with_name("photo_masks_owner_ok.txt")   # photos whose place the owner chose: the legibility rule is waived for them only
MODEL_URL = "https://github.com/fabio-sim/Depth-Anything-ONNX/releases/download/v2.0.0/depth_anything_v2_vits_dynamic.onnx"
MODEL_SHA256 = "46c4e8eeda3a27f34701831b6a2ec7753d7b38779b215acb5633424703deed8f"
# the gate's limits (looked at on every photo of our sets: docs/mockups/masks/)
HALO_MAX, JAGGED_MAX, ISLANDS_MAX, HOLES_MAX = 3.0, 0.33, 2, 6
CROSS_MIN, COLS_MIN, VISIBLE_MIN = 0.08, 0.25, 0.6
# the owner: «the clock always has the same position across our images»: fixed boxes (1920x1080 screen px), the photo is framed instead
CLOCK = {"clean": [601, 266, 1319, 542]}    # digits 266-542, 718 wide (2.6 x their height), centred: the owner's Riyadh place
FRAME_MAX = 1.4                             # a frame zooms 1.0 to 1.4, never smaller than the screen
_model = None


def reviewed():
    return {l.strip() for l in REVIEWED.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")}


# ---------------------------------------------------------------- the matte
def load_model(work):
    global _model
    if _model is None:
        import onnxruntime as ort
        f = Path(work) / "depth_anything_v2_vits_dynamic.onnx"
        if not f.is_file(): urllib.request.urlretrieve(MODEL_URL, f)
        if hashlib.sha256(f.read_bytes()).hexdigest() != MODEL_SHA256: sys.exit("the depth model is not the one this tool names (MODEL_SHA256)")
        _model = ort.InferenceSession(str(f), providers=["CPUExecutionProvider"])
    return _model


def depth(rgb, work):
    """inverse depth 0..1 (1 = nearest) at the photo's size"""
    m = load_model(work)
    h, w = rgb.shape[:2]
    mh, mw = 518, int(round(518 * w / h / 14)) * 14
    x = cv2.resize(rgb, (mw, mh), interpolation=cv2.INTER_AREA).astype(np.float32) / 255
    x = (x - [0.485, 0.456, 0.406]) / [0.229, 0.224, 0.225]
    d = m.run(None, {m.get_inputs()[0].name: x.transpose(2, 0, 1)[None].astype(np.float32)})[0][0]
    d = cv2.resize(d, (w, h), interpolation=cv2.INTER_CUBIC)
    lo, hi = np.percentile(d, 0.5), np.percentile(d, 99.5)
    return np.clip((d - lo) / (hi - lo + 1e-6), 0, 1)


def guided(I, p, r, eps):
    """the guided filter (He et al.): p made to follow the edges of the colour photo I (the 3x3 systems solved in closed form)"""
    box = lambda x: cv2.boxFilter(x, -1, (2 * r + 1, 2 * r + 1), borderType=cv2.BORDER_REFLECT)
    mI, mp = box(I), box(p)
    cov = box(I * p[..., None]) - mI * mp[..., None]
    v = lambda i, j: box(I[..., i] * I[..., j]) - mI[..., i] * mI[..., j] + (eps if i == j else 0)
    a11, a12, a13, a22, a23, a33 = v(0, 0), v(0, 1), v(0, 2), v(1, 1), v(1, 2), v(2, 2)
    c11, c12, c13 = a22 * a33 - a23 * a23, a13 * a23 - a12 * a33, a12 * a23 - a13 * a22
    c22, c23, c33 = a11 * a33 - a13 * a13, a12 * a13 - a11 * a23, a11 * a22 - a12 * a12
    det = a11 * c11 + a12 * c12 + a13 * c13
    x, y, z = cov[..., 0], cov[..., 1], cov[..., 2]
    a = np.dstack([c11 * x + c12 * y + c13 * z, c12 * x + c22 * y + c23 * z, c13 * x + c23 * y + c33 * z]) / det[..., None]
    b = mp - (a * mI).sum(-1)
    return (box(a) * I).sum(-1) + box(b)


def top_connected(m):
    lab, _ = label(m)
    keep = np.unique(lab[0][lab[0] > 0])
    return np.isin(lab, keep)


def sky_prob(rgb, d):
    """1 = sky: the farthest and reached from the top, or a big far opening; the band between decided by the nearest sure samples"""
    from scipy.spatial import cKDTree
    h, w = d.shape
    t = np.percentile(d[: h // 4], 50) + 0.008
    far_all = d < t
    far = top_connected(far_all)
    lab, n = label(far_all & ~far)
    if n:
        sizes = np.bincount(lab.ravel())
        ids = np.flatnonzero(sizes > h * w * 0.0004)
        lab_s = cv2.cvtColor(cv2.GaussianBlur(rgb, (0, 0), 1.0), cv2.COLOR_RGB2LAB).astype(np.float32)
        for i in ids[ids > 0]:          # the whole opening (a tower's arch): every pixel near the colour of the sky seen through it, joined to it,
            m = lab == i                # inside the opening's own box a little grown; its edge then follows the arch, not a blob
            ys, xs = np.nonzero(m)
            y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
            gy, gx = int((y1 - y0) * 0.25) + 4, int((x1 - x0) * 0.25) + 4
            y0, y1, x0, x1 = max(0, y0 - gy), min(h, y1 + gy), max(0, x0 - gx), min(w, x1 + gx)
            ref = np.median(lab_s[m], axis=0)
            de = np.sqrt(((lab_s[y0:y1, x0:x1] - ref) ** 2).sum(-1))
            near = de < 14
            lab_c, _ = label(near)
            seeds = np.unique(lab_c[m[y0:y1, x0:x1]]); seeds = seeds[seeds > 0]
            grown = np.zeros_like(m); grown[y0:y1, x0:x1] = np.isin(lab_c, seeds)
            if grown.sum() < m.sum() * 6: far |= grown | m
    far = cv2.erode(far.astype(np.uint8), np.ones((9, 9), np.uint8)) > 0                  # a band on the sky's side (thin spires)
    near = cv2.erode((d > t + 0.012).astype(np.uint8), np.ones((21, 21), np.uint8)) > 0    # and on the scenery's
    p = np.where(far, 1.0, np.where(near, 0.0, 0.5)).astype(np.float32)
    unk = ~far & ~near
    if unk.any() and far.any() and near.any():
        lab3 = cv2.cvtColor(rgb, cv2.COLOR_RGB2LAB).astype(np.float32)
        yy = np.mgrid[0:h, 0:w][0].astype(np.float32)
        feat = np.dstack([lab3, d[..., None] * 600, yy[..., None] / h * 60]).reshape(-1, 5)
        rng = np.random.default_rng(1)
        around = cv2.dilate(unk.astype(np.uint8), np.ones((61, 61), np.uint8)) > 0
        pick = lambda m, k: rng.choice(np.flatnonzero(m.ravel()), min(k, int(m.sum())), replace=False)
        fi, ni = pick(far & around, 9000), pick(near & around, 9000)
        if len(fi) < 50: fi = pick(far, 9000)
        if len(ni) < 50: ni = pick(near, 9000)
        y_ = np.r_[np.ones(len(fi)), np.zeros(len(ni))]
        u = np.flatnonzero(unk.ravel())
        _, idx = cKDTree(np.vstack([feat[fi], feat[ni]])).query(feat[u], 15)
        p = p.ravel(); p[u] = y_[idx].mean(1); p = p.reshape(h, w)
        lab2, _ = label((p > 0.5) | far)        # sky only where it joins the top, or a far opening
        keep = np.unique(np.r_[lab2[0], lab2[far]]); sky = np.isin(lab2, keep[keep > 0])
        p = np.where(unk & ~sky, np.minimum(p, 0.3), p)
        # a second look along the sky's edge, by colour and height only (the depth is blurred there): sky that glows between trees or rocks,
        # the colour of the sky seeds and joined to the sky, is sky
        skynow = p > 0.5
        band = (cv2.dilate(skynow.astype(np.uint8), np.ones((81, 81), np.uint8)) > 0) & ~skynow
        if band.any():
            f2 = np.dstack([lab3, yy[..., None] / h * 40]).reshape(-1, 4)
            u2 = np.flatnonzero(band.ravel())
            fi2, ni2 = pick(skynow & (cv2.dilate(band.astype(np.uint8), np.ones((61, 61), np.uint8)) > 0), 9000), pick(near & around | (band & (p < 0.1)), 9000)
            if len(fi2) >= 50 and len(ni2) >= 50:
                _, idx = cKDTree(np.vstack([f2[fi2], f2[ni2]])).query(f2[u2], 15)
                q = np.zeros(h * w, np.float32); q[u2] = np.r_[np.ones(len(fi2)), np.zeros(len(ni2))][idx].mean(1)
                glow = (q.reshape(h, w) > 0.85) & band
                lab4, _ = label(glow | skynow)
                keep = np.unique(lab4[skynow]); glow &= np.isin(lab4, keep[keep > 0])
                p = np.where(glow, 1.0, p)
    return p, far, near


def matte(rgb, work, scale=2):
    """alpha 0..1 at the photo's size: 1 = the scenery in front of the clock"""
    h, w = rgb.shape[:2]
    d = depth(rgb, work)
    p, far, near = sky_prob(rgb, d)
    rows = np.flatnonzero(((p > 0.02) & (p < 0.98)).any(axis=1) | (far.any(axis=1) & near.any(axis=1)))
    y0, y1 = (max(0, rows.min() - 24), min(h, rows.max() + 25)) if len(rows) else (0, 0)
    sky = p.copy()                                     # only the rows the edge crosses go through the filter, at twice the size
    if y1 > y0:
        R = cv2.resize(rgb[y0:y1], (w * scale, (y1 - y0) * scale), interpolation=cv2.INTER_CUBIC).astype(np.float32) / 255
        P = cv2.resize(p[y0:y1], (w * scale, (y1 - y0) * scale), interpolation=cv2.INTER_LINEAR)
        a = guided(R, P, r=2 * scale, eps=4e-5)
        a = guided(R, np.clip(a, 0, 1), r=2 * scale, eps=1e-5)
        sky[y0:y1] = cv2.resize(np.clip(a, 0, 1), (w, y1 - y0), interpolation=cv2.INTER_AREA)
    sky = np.clip((sky - 0.38) / 0.24, 0, 1)
    sky = sky * sky * (3 - 2 * sky)
    sky[near & (sky > 0.5)] = 0.5
    lab, n = label(sky < 0.5)                    # the scenery stands on the ground or the sides: a floating piece (a cloud) is sky
    edge = set(np.unique(lab[-1])) | set(np.unique(lab[:, 0])) | set(np.unique(lab[:, -1]))
    sizes = np.bincount(lab.ravel(), minlength=n + 1)
    for i in range(1, n + 1):
        if i not in edge and sizes[i] < h * w * 0.01:
            sky[cv2.dilate((lab == i).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0] = 1
    sky = cv2.GaussianBlur(sky, (0, 0), 0.5)
    return np.clip(1 - sky, 0, 1)


def scenery_colour(rgb, alpha):
    """the edge's colour carried out from the scenery's solid inside (2 px in: the photo's own blur has sky in it)"""
    I = rgb.astype(np.float32) / 255
    solid = cv2.erode((alpha > 0.96).astype(np.uint8), np.ones((5, 5), np.uint8)).astype(np.float32)
    F = I.copy()
    todo = solid < 0.5
    for sig in (1.5, 3, 6, 12, 24):
        num = cv2.GaussianBlur(I * solid[..., None], (0, 0), sig); den = cv2.GaussianBlur(solid, (0, 0), sig)
        ok = todo & (den > 0.05)
        F[ok] = num[ok] / den[ok][:, None]
        todo &= ~ok
    return (np.clip(F, 0, 1) * 255 + 0.5).astype(np.uint8)


def photo(path):
    """the photo at the TV's size, cropped to 16:9 as the TV does; None if it is far from 16:9 (the cut-out would not sit on it)"""
    src = Image.open(path)
    if abs(src.width * 9 - src.height * 16) > src.width * 0.02: return None
    return np.asarray(src.convert("RGB").resize((W, H), Image.LANCZOS))


# ---------------------------------------------------------------- the gate
def skyline(alpha):
    """the scenery's top in each of the 48 columns, px of 1080 (1080: none)"""
    s = alpha > 0.5
    out = []
    for c in range(COLS):
        col = s[:, c * (W // COLS):(c + 1) * (W // COLS)].any(axis=1)
        out.append(int(np.argmax(col)) if col.any() else H)
    return out


def frame_of(fr):
    """a frame [x, y, w] (the part of the 1920x1080 photo shown full-screen, h = w 9/16) or None (the whole photo): (x, y, w, h)"""
    if not fr: return 0, 0, W, H
    x, y, w = fr
    return x, y, w, round(w * 9 / 16)


def framed(img, fr):
    """the picture as the TV shows it through the frame (the photo and its cut-out the same way)"""
    x, y, w, h = frame_of(fr)
    if (x, y, w) == (0, 0, W): return img
    return cv2.resize(img[y:y + h, x:x + w], (W, H), interpolation=cv2.INTER_AREA if img.ndim == 3 else cv2.INTER_LINEAR)


def screen_tops(t, fr, x0=0, x1=W):
    """the scenery's top (tops()) in screen columns x0..x1 through the frame, px of 1080 (H or more: none)"""
    x, y, w, h = frame_of(fr)
    s = W / w
    src = np.clip((x + (np.arange(x0, x1) + 0.5) / s).astype(int), 0, W - 1)
    v = t[src].astype(np.float32)
    return np.where(v >= H, 2 * H, (v - y) * s)


def fit(t, b, fr=None):
    """how the scenery meets the fixed clock box b through the frame: the share of the digits visible, how much of their lower part it crosses,
    over how many of their columns (not a sliver)"""
    ts = screen_tops(t, fr, b[0], b[2])
    h = b[3] - b[1]
    if not (ts < b[3]).any(): return {"crossing": False, "visible": 1.0, "cross": 0.0, "cols": 0.0}
    cover = np.clip((b[3] - ts[::10]) / h, 0, 1)
    hit = ts[::20] < b[3] - h * 0.05
    return {"crossing": True, "visible": round(1 - float(cover.mean()), 3), "cross": round(float(cover.mean()), 3), "cols": round(float(hit.mean()), 3)}


def staircase(alpha):
    """how stair-stepped the edge is (a mask made small and blown up): where the edge runs shallow, a 1 px step of an edge made at full
    size is one slanted move of its outline; a blown-up one's steps are 2 px or more, so they add straight moves across the edge's way. The share of
    the moves across the edge that are straight, at the photo's size (0 smooth, about 0.5 for a mask blown up twice)."""
    m = (alpha > 0.5).astype(np.uint8)
    cs, _ = cv2.findContours(m, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)
    cross = straight = 0
    for c in cs:
        if len(c) < 60: continue
        p = c[:, 0, :].astype(np.float32)
        t = np.gradient(gaussian_filter1d(p, 8, axis=0, mode="wrap"), axis=0)
        d = np.diff(np.vstack([p, p[:1]]), axis=0)
        inside = (p[:, 0] > 4) & (p[:, 0] < W - 5) & (p[:, 1] > 4) & (p[:, 1] < H - 5)   # not the screen's own edge
        for along, across in ((0, 1),):             # a shallow edge (runs across the screen, steps down); not a steep one: a tree's spike is straight
            shallow = (np.abs(t[:, across]) < 0.45 * np.abs(t[:, along])) & inside
            moves = shallow & (d[:, across] != 0)
            st = moves & (d[:, along] == 0)
            lab, n = label(st)                      # straight steps in a row: 1 to 3 px is a blown-up stair; taller is the scenery's own (a tree)
            ids = np.flatnonzero(np.bincount(lab, minlength=n + 1) <= 3)
            short = np.isin(lab, ids[ids > 0]) & st
            cross += moves.sum(); straight += short.sum()
    return straight / cross if cross else 0.0


def edge_scores(rgb, alpha, F):
    """halo (luma units over a dark plate), jagged (the staircase share of the slanted edge), islands and holes"""
    a = alpha[..., None]
    plate = np.array([18, 22, 40], np.float32)
    comp = F.astype(np.float32) * a + plate * (1 - a)
    luma = lambda x: x @ np.array([0.299, 0.587, 0.114], np.float32)
    solid = (alpha > 0.96).astype(np.float32)
    inner = cv2.GaussianBlur(luma(F.astype(np.float32)) * solid, (0, 0), 4) / np.maximum(cv2.GaussianBlur(solid, (0, 0), 4), 1e-3)
    expect = inner * alpha + luma(plate) * (1 - alpha)
    ring = (alpha > 0.05) & (alpha < 0.95)
    halo = float(np.abs(luma(comp) - expect)[ring].mean()) if ring.any() else 0.0
    jag = staircase(alpha)
    lab, n = label(alpha > 0.5)
    edge = set(np.unique(lab[-1])) | set(np.unique(lab[:, 0])) | set(np.unique(lab[:, -1]))
    sizes = np.bincount(lab.ravel(), minlength=n + 1)
    islands = sum(1 for i in range(1, n + 1) if i not in edge and sizes[i] >= 60)
    lab, n = label(alpha <= 0.5)
    top = set(np.unique(lab[0]))
    sizes = np.bincount(lab.ravel(), minlength=n + 1)
    holes = sum(1 for i in range(1, n + 1) if i not in top and 30 <= sizes[i] < W * H * 0.0004)
    return {"halo": round(halo, 2), "jagged": round(float(jag), 4), "islands": islands, "holes": holes}


_cascades = None


def people_cut(rgb, alpha):
    """a face or a person the edge goes through (OpenCV's own cascades)"""
    global _cascades
    if _cascades is None:
        _cascades = [cv2.CascadeClassifier(cv2.data.haarcascades + f) for f in
                     ("haarcascade_frontalface_default.xml", "haarcascade_fullbody.xml", "haarcascade_eye.xml")]
        if any(c.empty() for c in _cascades): sys.exit("the face and person finders are missing: pip install \"opencv-python-headless<5\"")
    g = cv2.equalizeHist(cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY))
    for n, (cc, mn) in enumerate(zip(_cascades[:2], (40, 80))):
        for (x, y, w, h) in cc.detectMultiScale(g, 1.1, 6, minSize=(mn, mn)):
            if n == 0 and not len(_cascades[2].detectMultiScale(g[y:y + h // 2, x:x + w], 1.1, 4, minSize=(w // 8, w // 8))): continue   # a face has an eye: not a rock
            r = alpha[y:y + h, x:x + w]
            if (r > 0.8).mean() > 0.1 and (r < 0.2).mean() > 0.1: return True
    return False


COVER_MAX, COLON_MAX = 0.25, 0.25   # the owner: the scenery covers at most about a quarter of any digit, from its base, and never the colon
# (its lower dot starts about 30 % up in the app's font: a quarter keeps it clear)


def tops(alpha):
    """the scenery's top in every pixel column (H: none)"""
    s = alpha > 0.5
    return np.where(s.any(axis=0), np.argmax(s, axis=0), H)


COVER_MAX, COLON_MAX = 0.25, 0.25   # the owner: the scenery covers at most about a quarter of any digit, from its base, and never the colon


def glyphs(b):
    """where the digits and the colon stand in the box, for «١٢:٣٠» (4 digits) and «٤:٥٨» (3), the text centred, left to right: [(x0, x1, colon)]"""
    h = b[3] - b[1]; dw, cw = 0.56 * h, 0.24 * h; cx = (b[0] + b[2]) / 2
    out = []
    for n_left in (2, 1):
        width = (n_left + 2) * dw + cw
        x = cx - width / 2
        for kind in ["d"] * n_left + ["c"] + ["d", "d"]:
            w_ = cw if kind == "c" else dw
            out.append((int(x + w_ * 0.08), int(x + w_ * 0.92), kind == "c")); x += w_
    return out


def legible(t, b, fr=None):
    """the owner's rule at the clock's real digits, through the frame: the most any digit is covered from its base (a spike narrower than 3 px
    aside) and the most the colon is; ok when within COVER_MAX and COLON_MAX"""
    h = b[3] - b[1]
    def cover(x0, x1):
        x0, x1 = max(0, x0), min(W, x1)
        if x1 <= x0: return 0.0
        c = np.clip((b[3] - screen_tops(t, fr, x0, x1)) / h, 0, 1)
        return float(np.sort(c)[-3]) if len(c) >= 3 else float(c.max())
    dig = max(cover(x0, x1) for x0, x1, colon in glyphs(b) if not colon)
    col = max(cover(x0, x1) for x0, x1, colon in glyphs(b) if colon)
    return dig <= COVER_MAX and col <= COLON_MAX, round(dig, 3), round(col, 3)


def aligned(rgb, alpha, F):
    """the cut-out sits on the photo: where it is solid its pixels are the photo's (no offset, no doubled edge): the best shift of its edges
    against the photo's (phase correlation) and the mean difference"""
    solid = cv2.erode((alpha > 0.97).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    if solid.sum() < 1000: return True, 0.0, 0.0
    diff = float(np.abs(F.astype(np.float32) - rgb.astype(np.float32))[solid].mean())
    g1 = cv2.Canny(cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY), 60, 160).astype(np.float32) * solid
    g2 = cv2.Canny(cv2.cvtColor(F, cv2.COLOR_RGB2GRAY), 60, 160).astype(np.float32) * solid
    (dx, dy), _ = cv2.phaseCorrelate(g1, g2) if g1.any() and g2.any() else ((0.0, 0.0), 0)
    shift = float(np.hypot(dx, dy))
    return shift < 0.5 and diff < 2.0, round(shift, 2), round(diff, 2)


def place_ok(t, b, fr):
    """the fixed clock b through the frame fr: the scenery crosses its lower part well and the owner's legibility rule holds"""
    f = fit(t, b, fr)
    if not f["crossing"] or f["cross"] < CROSS_MIN or f["cols"] < COLS_MIN or f["visible"] < VISIBLE_MIN: return False
    return legible(t, b, fr)[0]


def frames():
    """every frame the gate may give a photo, the least zoom first, then the nearest the centre: zoom 1 to FRAME_MAX, x and y even"""
    out = []
    for k in np.arange(1.0, FRAME_MAX + 1e-6, 0.02):
        w = int(round(W / k / 2) * 2); h = round(w * 9 / 16)
        xs = sorted(range(0, W - w + 1, 16) if w < W else [0], key=lambda x: abs(x - (W - w) / 2))
        ys = sorted(range(0, H - h + 1, 8) if h < H else [0], key=lambda y: abs(y - (H - h) / 2))
        out += sorted(([x, y, w] for x in xs for y in ys), key=lambda f: abs(f[0] - (W - w) / 2) + abs(f[1] - (H - h) / 2))
    return [None if f == [0, 0, W] else f for f in out]


def search(t, b):
    """the frame that puts the scenery under the fixed clock by the owner's rule (the clock never moves: the photo does), or None"""
    return next((fr for fr in frames() if place_ok(t, b, fr)), False)


def judge(rgb, alpha, F, owner_ok=False, frame=None):
    """(verdict, why, scores, frame, depth): the edge checks, then a frame for the fixed clean clock (CLOCK); depth = the layouts drawn behind"""
    sky = float((alpha < 0.5).mean())
    e = edge_scores(rgb, alpha, F)
    t = tops(alpha)
    b = CLOCK["clean"]
    f = fit(t, b, frame)
    s = {**e, **f, "sky": round(sky, 3)}
    if sky < 0.05 or alpha[0].mean() > 0.5: return "REJECT", "no sky found along the top", s, None, []
    if people_cut(rgb, alpha): return "REJECT", "the edge cuts through a face or a person", s, None, []
    if e["halo"] > HALO_MAX: return "REJECT", f"a fringe along the edge ({e['halo']} > {HALO_MAX})", s, None, []
    if e["jagged"] > JAGGED_MAX: return "REJECT", f"a jagged edge ({e['jagged']} > {JAGGED_MAX})", s, None, []
    if e["islands"] > ISLANDS_MAX or e["holes"] > HOLES_MAX: return "REJECT", f"a broken edge ({e['islands']} islands, {e['holes']} holes)", s, None, []
    ok, s["shift"], s["diff"] = aligned(rgb, alpha, F)
    if not ok: return "REJECT", f"the cut-out does not sit on the photo (shift {s['shift']} px, difference {s['diff']})", s, None, []
    depth = lambda fr, first: first + [n for n, bb in CLOCK.items() if n not in first and place_ok(t, bb, fr)]
    if owner_ok:                     # the owner chose this picture at the clean place himself (his ad picture): the clean cut-out is all that is checked
        s["owner_ok"] = True
        if f["crossing"]: _, s["digit_cover"], s["colon_cover"] = legible(t, b, frame)
        return "FIT", "the owner's own place (owner_ok): only the edge, halo and alignment checked", s, frame, depth(frame, ["clean"])
    fr = search(t, b)
    if fr is False:
        if not f["crossing"] and not fit(t, b, [0, 0, int(round(W / FRAME_MAX / 2) * 2)])["crossing"]:
            return "FRONT-ONLY", "the scenery is too low to reach the clock at any frame", s, None, []
        return "FRONT-ONLY", "no frame puts the scenery under the clock by the owner's rule (a quarter of a digit, never the colon)", s, None, []
    f = fit(t, b, fr)
    s.update(f); _, s["digit_cover"], s["colon_cover"] = legible(t, b, fr)
    x, y, w, h = frame_of(fr); s["zoom"] = round(W / w, 3)
    return "FIT", "clean, and framed so the scenery crosses the clock's lower part" + (f" (frame {x},{y},{w})" if fr else ""), s, fr, depth(fr, ["clean"])


# ---------------------------------------------------------------- the files
def encode(F, alpha, fr=None):
    a8 = (alpha * 255 + 0.5).astype(np.uint8)
    rgba = Image.fromarray(np.dstack([F, a8]), "RGBA")
    bb = Image.fromarray(a8).point(lambda v: 255 if v > 8 else 0).getbbox()
    if bb is None: return None, None
    # the fixed clock's bottom is well above 72 % of the screen, so below 72 % of the frame the cut-out would only cover the same photo under it:
    # it stops there (smaller, the same picture)
    fx, fy, fw, fh = frame_of(fr)
    low = fy + round(fh * 0.72)
    if bb[3] > low:
        bb = (bb[0], bb[1], bb[2], low)
        a8 = a8.copy()                                   # the last 24 px fade out: no seam where it ends, whatever the TV scales
        ramp = np.linspace(1, 0, 24, dtype=np.float32)[:, None]
        a8[bb[3] - 24:bb[3]] = (a8[bb[3] - 24:bb[3]] * ramp + 0.5).astype(np.uint8)
        rgba = Image.fromarray(np.dstack([F, a8]), "RGBA")
    bb = (bb[0] - bb[0] % 2, bb[1] - bb[1] % 2, bb[2] + bb[2] % 2, bb[3] + bb[3] % 2)   # even: a 2x screen never puts it half a pixel off
    if bb[3] <= bb[1]: return None, None
    crop = rgba.crop(bb)
    for aq in (100, 90):
        q = 90
        while True:
            b = io.BytesIO(); crop.save(b, "WEBP", quality=q, method=6, alpha_quality=aq)
            if b.tell() <= MAX_FG or q <= 40: break
            q -= 8
        if b.tell() <= MAX_FG: return b.getvalue(), [bb[0], bb[1], bb[2] - bb[0], bb[3] - bb[1]]
    return None, None


def render(rgb, alpha, F, b, out, fr=None):
    """the check render through the frame: the glass digits at the fixed clock, the cut-out over them (1920x1080), and 2x crops of the edge
    across the digits"""
    from PIL import ImageDraw, ImageFilter, ImageFont
    rgb, alpha, F = framed(rgb, fr), framed(alpha, fr), framed(F, fr)
    base = Image.fromarray(rgb)
    if b:
        glass = base.filter(ImageFilter.GaussianBlur(28)).point(lambda v: min(255, int(v * 1.25 + 30)))
        try: font = ImageFont.load_default(size=round((b[3] - b[1]) * 1.25))
        except TypeError: font = ImageFont.load_default()
        m = Image.new("L", (W, H), 0); d = ImageDraw.Draw(m)
        tw = d.textlength("12:30", font=font)
        d.text(((b[0] + b[2]) / 2 - tw / 2, b[1] - (b[3] - b[1]) * 0.28), "12:30", fill=255, font=font)
        rim = m.filter(ImageFilter.FIND_EDGES).point(lambda v: 255 if v > 40 else 0)
        base = Image.composite(glass, base, m)
        base = Image.composite(Image.new("RGB", (W, H), (255, 255, 255)), base, rim.point(lambda v: v * 0.8))
    fg = Image.fromarray(np.dstack([F, (alpha * 255 + 0.5).astype(np.uint8)]), "RGBA")
    base.paste(fg, (0, 0), fg)
    base.save(out, quality=88)
    crops = []
    if b:
        y0 = max(0, b[3] - (b[3] - b[1]) // 2 - 100)
        for x0 in (b[0], (b[0] + b[2]) // 2 - 160, b[2] - 320):
            crops.append(base.crop((x0, y0, x0 + 320, y0 + 200)).resize((640, 400), Image.NEAREST))
    return base, crops


def sets(names, work):
    """{set: [(name, path)]}: the hosted photos, and each pack's photos from its release zip (sha256 checked)"""
    out = {}
    if "photos" in names:
        out["photos"] = [(p.stem, p) for p in sorted((ROOT / "photos").glob("*.jpg"))]
    index = json.loads((ROOT / "photos/packs.json").read_text(encoding="utf-8"))
    for p in index["packs"]:
        if p["id"] not in names or p["id"] in NEVER: continue
        z = work / f"{p['id']}.zip"
        if not z.is_file(): urllib.request.urlretrieve(p["url"], z)
        if hashlib.sha256(z.read_bytes()).hexdigest() != p["sha256"]: sys.exit(f"{p['id']}: the zip is not the one the index names")
        d = work / p["id"]
        d.mkdir(exist_ok=True)
        with zipfile.ZipFile(z) as f: f.extractall(d)
        out[p["id"]] = [(q.stem, q) for q in sorted(d.glob("*.jpg"), key=lambda q: int(q.stem))]
    return out


def scenes(o, work):
    """a horizons pack: each <id>.jpg of DIR/scenes.tsv gets <id>-fg.webp in OUT when FIT, and the tsv's mask box; the gate in OUT/gate.json"""
    src, out = Path(o.scenes), Path(o.out)
    out.mkdir(parents=True, exist_ok=True)
    rows = [l.split("\t") for l in (src / "scenes.tsv").read_text(encoding="utf-8").splitlines()]
    gate, sheet = {}, []
    for r in rows:
        if r[0].startswith("#") or len(r) < 6: continue
        rgb = photo(src / f"{r[0]}.jpg")
        alpha = matte(rgb, work); F = scenery_colour(rgb, alpha)
        owner = "owner_ok" in [t.strip() for t in (r[8] if len(r) > 8 else "").split(",")]   # scenes.tsv tags: the owner's own picture, kept
        given = r[10].strip() if len(r) > 10 else "-"
        fr0 = [int(v) for v in given.split(",")] if owner and given not in ("", "-") else None   # the owner's own frame, kept
        verdict, why, s, fr, depth = judge(rgb, alpha, F, owner, fr0)
        r += [""] * (9 - len(r))                     # the tags column may be empty
        while len(r) < 12: r.append("-")
        r[3] = r[4] = r[9] = "-"                     # the clock never moves (the owner): the old per-scene place is not read any more
        r[10] = ",".join(map(str, fr)) if fr else "-"
        r[11] = ",".join(depth) if verdict == "FIT" and depth else "-"
        gate[r[0]] = {"verdict": verdict, "why": why, "scores": s}
        print(f"{r[0]}: {verdict} ({why})")
        base, crops = render(rgb, alpha, F, CLOCK["clean"], out / f"{r[0]}-check.jpg", fr)
        sheet.append((r[0], verdict, base, crops))
        if verdict == "FIT":
            data, bb = encode(F, alpha, fr)
            if data:
                (out / f"{r[0]}-fg.webp").write_bytes(data); r[5] = ",".join(map(str, bb))
        else:
            r[5] = "-"
    rows = [(r + [""] * 9)[:9] + ["digits_x", "frame x,y,w", "depth"] if r[0].startswith("# id") else r for r in rows]
    (out / "scenes.tsv").write_text("\n".join("\t".join(r) for r in rows) + "\n", encoding="utf-8")
    (out / "gate.json").write_text(json.dumps({"v": 1, "photos": gate}, indent=1, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    review_sheet(sheet, out / "sheet.jpg")


def main():
    a = argparse.ArgumentParser()
    a.add_argument("out")
    a.add_argument("--sets", default="photos,nature,sea,space,ksa-cities")
    a.add_argument("--work", default="photo-masks-work")
    a.add_argument("--all", action="store_true", help="every FIT cut-out, the review sheet and the edge crops")
    a.add_argument("--only", default="", help="set/name,... (a quick look at a few)")
    a.add_argument("--scenes", default="", help="a horizons pack folder (scenes.tsv and its photos)")
    o = a.parse_args()
    work = Path(o.work); work.mkdir(parents=True, exist_ok=True)
    if o.scenes: return scenes(o, work)
    ok = None if o.all else reviewed()
    owner_list = {l.strip() for l in OWNER_OK.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")} if OWNER_OK.is_file() else set()
    only = {s for s in o.only.split(",") if s}
    sheet, gate, counts = [], {}, {}
    want = [s for s in o.sets.split(",") if s and s not in NEVER]
    total = cut_n = 0
    for name, photos in sets(want, work).items():
        d = Path(o.out) / name
        d.mkdir(parents=True, exist_ok=True)
        files, n = {}, counts.setdefault(name, {"FIT": 0, "FRONT-ONLY": 0, "REJECT": 0})
        for stem, path in photos:
            total += 1
            if only and f"{name}/{stem}" not in only: continue
            rgb = photo(path)
            if rgb is None:
                gate[f"{name}/{stem}"] = {"verdict": "FRONT-ONLY", "why": "not 16:9: the TV crops it", "scores": {}}; n["FRONT-ONLY"] += 1; continue
            alpha = matte(rgb, work); F = scenery_colour(rgb, alpha)
            owner = f"{name}/{stem}" in owner_list
            verdict, why, s, fr, depth = judge(rgb, alpha, F, owner)
            gate[f"{name}/{stem}"] = {"verdict": verdict, "why": why, "scores": s}; n[verdict] += 1
            print(f"{name}/{stem}: {verdict} ({why})", flush=True)
            if o.all:
                (Path(o.out) / "edges").mkdir(exist_ok=True)
                base, crops = render(rgb, alpha, F, CLOCK["clean"], Path(o.out) / "edges" / f"{name}-{stem}.jpg", fr)
                sheet.append((f"{name}/{stem}", verdict, base, crops))
            if verdict != "FIT": continue
            if ok is not None and f"{name}/{stem}" not in ok: continue
            data, bb = encode(F, alpha, fr)
            if not data: continue
            (d / f"{stem}-fg.webp").write_bytes(data)
            files[stem] = {"sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data), "box": bb, "skyline": skyline(alpha)}
            if fr: files[stem]["frame"] = fr                 # the part of the photo shown full-screen (the photo and its cut-out alike)
            files[stem]["depth"] = depth                     # the layouts that draw it behind the clock (their fixed boxes: CLOCK)
            if owner: files[stem]["owner_ok"] = True   # the app draws it without its own legibility drop (PhotoGlass)
            cut_n += 1
        (d / "index.json").write_text(json.dumps({"v": 1, "set": name, "files": files}, indent=0, sort_keys=True) + "\n", encoding="utf-8")
        print(f"{name}: {n['FIT']} FIT, {n['FRONT-ONLY']} FRONT-ONLY, {n['REJECT']} REJECT; {len(files)} published", flush=True)
    for s in NEVER: counts[s] = {"FIT": 0, "FRONT-ONLY": "all (never cut)", "REJECT": 0}
    (Path(o.out) / "gate.json").write_text(json.dumps({"v": 1, "photos": gate, "counts": counts}, indent=1, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"all: {cut_n} published of {total}")
    if ok is not None and not only:
        lost = sorted(k for k in ok if gate.get(k, {}).get("verdict") != "FIT")
        if lost: sys.exit(f"reviewed cut-out(s) that no longer pass the gate: {', '.join(lost)} (look at the sheet again: --all)")
    if sheet: review_sheet(sheet, Path(o.out) / "sheet.jpg")


def review_sheet(items, out):
    """each photo with its glass digits and the cut-out over them, its verdict written on it, 4 to a row"""
    from PIL import ImageDraw
    colour = {"FIT": (60, 220, 90), "FRONT-ONLY": (255, 200, 0), "REJECT": (255, 60, 60)}
    tiles = []
    for title, verdict, base, _ in items:
        t = base.resize((480, 270))
        dr = ImageDraw.Draw(t)
        dr.rectangle((0, 0, 479, 20), fill=(0, 0, 0)); dr.text((6, 4), f"{title}  {verdict}", fill=colour[verdict])
        tiles.append(t)
    sheet = Image.new("RGB", (480 * 4, 270 * ((len(tiles) + 3) // 4)))
    for i, t in enumerate(tiles): sheet.paste(t, ((i % 4) * 480, (i // 4) * 270))
    sheet.save(out, quality=82)


if __name__ == "__main__":
    main()
