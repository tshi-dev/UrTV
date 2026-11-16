#!/usr/bin/env python3
"""4.4 «الساعة: زجاجية خلف الصورة» on our photos: a cut-out (the scenery, the sky transparent) for each photo of our sets, made OFFLINE (here, in CI),
never on the TV. A photo gets one only when its sky has a clean edge: three ways of finding the sky (the first strong edge from the top, a blue sky, a
warm sky) are tried, and the cut-out is kept only when a colour one (the sky is blue, or warm) agrees with the edge one, and no sky is taken below the scenery's top; else the photo has none and the TV's glass clock sits in front.
«الحرمين» is never cut (the owner: no clock behind the Kaaba or the Prophet's Mosque), and the person's own photos are not ours to cut.

    python3 .github/tools/photo_masks.py OUT [--sets photos,nature,sea,space,ksa-cities] [--work DIR] [--all]

Only the cut-outs named in photo_masks_ok.txt (checked by eye) are written; --all writes every one that passes and OUT/sheet.jpg to look at.

Writes OUT/<set>/<name>-fg.webp (cropped to its box, at most 180 KB at 1080) and OUT/<set>/index.json:
{"v": 1, "set": ..., "files": {"<name>": {"sha256", "bytes", "box": [x, y, w, h], "skyline": [48 ints: the scenery's top in each 40 px column, px of 1080]}}}
The packs' zips come from this repository's releases (photos/packs.json, their sha256 checked); "photos" is the hosted set in photos/.
Needs numpy, scipy, opencv-python-headless and Pillow."""
import argparse, hashlib, io, json, os, sys, urllib.request, zipfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy.ndimage import label, median_filter

ROOT = Path(__file__).resolve().parents[2]
NEVER = {"haramain"}            # the owner: never behind the Kaaba or the Prophet's Mosque
W, H, COLS = 960, 540, 48       # the work size, and the skyline's columns (40 px of 1920 each)
MAX_FG = 180 * 1024
REVIEWED = Path(__file__).with_name("photo_masks_ok.txt")


def reviewed():
    return {l.strip() for l in REVIEWED.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")}


def edge_sky(small, t):
    g = cv2.cvtColor(cv2.bilateralFilter(small, 9, 30, 7), cv2.COLOR_BGR2LAB).astype(np.float32)
    mag = sum(np.abs(cv2.Sobel(g[:, :, c], cv2.CV_32F, 0, 1, ksize=3)) + np.abs(cv2.Sobel(g[:, :, c], cv2.CV_32F, 1, 0, ksize=3)) for c in range(3))
    edge = mag > t
    end = np.full(W, H)
    for x in range(W):
        col = edge[:, x]
        hits = np.where(col[4:H - 3] & col[5:H - 2] & col[6:H - 1])[0]
        if len(hits): end[x] = hits[0] + 4
    end = median_filter(end, size=3)
    wide = median_filter(end, size=41)
    end = np.where(end < wide - 60, wide, end)
    return (np.arange(H)[:, None] < end[None, :])


def colour_sky(small, rule):
    im = cv2.GaussianBlur(small, (5, 5), 0).astype(int)
    b, r = im[:, :, 0], im[:, :, 2]
    skyish = (b >= r - 8) if rule == "blue" else (r > b + 25)
    lab, _ = label(skyish)
    top = set(np.unique(lab[0])) - {0}
    if not top: return np.zeros((H, W), bool)
    sky = np.isin(lab, list(top)).astype(np.uint8) * 255
    sky = cv2.morphologyEx(sky, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    sky = cv2.morphologyEx(sky, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    return sky > 127


def grounded(sky):
    """the scenery is what stands on the ground: a patch not joined to the bottom edge (a cloud, a bird) is sky"""
    sub, _ = label(~sky)
    keep = set(np.unique(sub[-1])) - {0}
    return ~np.isin(sub, list(keep)) if keep else np.ones_like(sky)


def skyline(sky):
    """the scenery's top per column of the work size (H where the column is all sky)"""
    sub = ~sky
    any_ = sub.any(axis=0)
    first = np.argmax(sub, axis=0)
    return np.where(any_, first, H)


def good(sky):
    """the gates a cut-out must pass: sky along the top, a fair share of sky, a skyline with no ragged jumps"""
    share = sky.mean()
    if not (0.12 <= share <= 0.75) or sky[0].mean() < 0.85: return False
    s = skyline(sky)
    jumps = np.abs(np.diff(s.astype(int))) > 30
    return jumps.mean() <= 0.06 and (s < H).mean() >= 0.9


def iou(a, b):
    u = (a | b).sum()
    return (a & b).sum() / u if u else 0


def cut(path):
    """the sky mask at 960x540, or None when no two ways agree on a clean one"""
    small = cv2.resize(cv2.imread(str(path)), (W, H), interpolation=cv2.INTER_AREA)
    cands = {"edge": grounded(edge_sky(small, 30)), "blue": grounded(colour_sky(small, "blue")), "warm": grounded(colour_sky(small, "warm"))}
    ok = {k: above(v) for k, v in cands.items() if good(v)}
    if "edge" not in ok: return None
    for k in ("warm", "blue"):   # a sky of one colour that covers what the edge one does: its mask (it follows an arch, a tower's crown)
        if k in ok and iou(ok[k], ok["edge"]) >= 0.8: return ok[k]
    return None


def above(sky):
    """sky only above the scenery's top in each column: no hole in the scenery (a shadow, a red cloth) is ever taken for sky"""
    return np.arange(H)[:, None] < skyline(sky)[None, :]


def write(sky, photo, out_dir, name):
    big = cv2.resize(sky.astype(np.uint8) * 255, (1920, 1080), interpolation=cv2.INTER_LINEAR)
    big = cv2.GaussianBlur(big, (5, 5), 1.2)
    alpha = 255 - big
    src = Image.open(photo)
    if abs(src.width * 9 - src.height * 16) > src.width * 0.02: return None   # not 16:9: the TV crops it, the cut-out would not sit on it
    rgb = src.convert("RGB").resize((1920, 1080))
    fg = rgb.copy(); fg.putalpha(Image.fromarray(alpha))
    box = Image.fromarray(alpha).point(lambda v: 255 if v > 8 else 0).getbbox()
    if box is None: return None
    crop = fg.crop(box)
    q = 86
    while True:
        b = io.BytesIO(); crop.save(b, "WEBP", quality=q, method=6, alpha_quality=90)
        if b.tell() <= MAX_FG or q <= 30: break
        q -= 8
    if b.tell() > MAX_FG: return None
    data = b.getvalue()
    (out_dir / f"{name}-fg.webp").write_bytes(data)
    line = skyline(sky)
    per = [int(min(line[c * (W // COLS):(c + 1) * (W // COLS)].min(), H) * 1080 / H) for c in range(COLS)]
    return {"sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data), "box": [box[0], box[1], box[2] - box[0], box[3] - box[1]], "skyline": per}


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


def main():
    a = argparse.ArgumentParser()
    a.add_argument("out")
    a.add_argument("--sets", default="photos,nature,sea,space,ksa-cities")
    a.add_argument("--work", default="/tmp/photo-masks")
    a.add_argument("--all", action="store_true", help="every cut-out that passes, and a review sheet")
    o = a.parse_args()
    ok = None if o.all else reviewed()
    sheet = []
    want = [s for s in o.sets.split(",") if s and s not in NEVER]
    work = Path(o.work); work.mkdir(parents=True, exist_ok=True)
    total = cut_n = 0
    for name, photos in sets(want, work).items():
        d = Path(o.out) / name
        d.mkdir(parents=True, exist_ok=True)
        files = {}
        for stem, path in photos:
            total += 1
            if ok is not None and f"{name}/{stem}" not in ok: continue
            sky = cut(path)
            if sky is None: continue
            rec = write(sky, path, d, stem)
            if rec:
                files[stem] = rec; cut_n += 1
                if o.all: sheet.append((f"{name}/{stem}", d / f"{stem}-fg.webp", rec["box"]))
        (d / "index.json").write_text(json.dumps({"v": 1, "set": name, "files": files}, indent=0, sort_keys=True) + "\n", encoding="utf-8")
        print(f"{name}: {len(files)} of {len(photos)} photos have a cut-out")
    print(f"all: {cut_n} of {total}")
    if ok is not None and cut_n != len(ok): sys.exit(f"{len(ok) - cut_n} reviewed cut-out(s) no longer made: look at the sheet again (--all)")
    if sheet: review_sheet(sheet, Path(o.out) / "sheet.jpg")


def review_sheet(items, out):
    """each cut-out over red, 4 to a row: red where the sky is taken, so a bite into the scenery shows"""
    from PIL import ImageDraw
    tiles = []
    for title, fg_path, box in items:
        fg = Image.open(fg_path)
        bg = Image.new("RGB", (1920, 1080), (255, 0, 0))
        bg.paste(fg, (box[0], box[1]), fg)
        t = bg.resize((480, 270))
        ImageDraw.Draw(t).text((6, 6), title, fill=(255, 255, 0))
        tiles.append(t)
    sheet = Image.new("RGB", (480 * 4, 270 * ((len(tiles) + 3) // 4)))
    for i, t in enumerate(tiles): sheet.paste(t, ((i % 4) * 480, (i // 4) * 270))
    sheet.save(out, quality=80)


if __name__ == "__main__":
    main()
