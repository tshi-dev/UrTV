#!/usr/bin/env python3
"""Builds and publishes the picked photo packs, in CI (.github/workflows/photo-packs.yml).

For each photos/packs-src/<id>.json:
  {"id", "name", "name_en", "source", "source_en", "reviewed": true,
   "photos": [{"title", "author", "licence", "link", "url"}, ...]}   (url: the original file on Commons)
the originals are downloaded into work/<id>/ with manifest.json, make_pack.py builds packs-built/<id>.zip, the cover and the index line, and
the zip is uploaded as the asset <id>.zip of the release photos-<id> (created, or its asset replaced). Out: packs-built/index.json (the lines
for photos/packs.json), packs-built/covers/<id>.jpg, packs-built/<id>-sheet.jpg (what went in, for one more look).

    python3 .github/tools/pack_build.py
"""
import io
import json
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "photos/packs-src"
OUT = ROOT / "packs-built"
UA = {"User-Agent": "UrTV-photo-packs/1.0 (https://github.com/tshi-dev/UrTV)"}


def get(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r:
                return r.read()
        except Exception:  # noqa: BLE001
            time.sleep(5 * (attempt + 1))
    raise SystemExit(f"could not download {url}")


def scaled(url):
    """A Commons original as Commons' own 3840-px-wide rendering (Special:FilePath?width=): plenty for 1920x1080, and a 200-megapixel Hubble
    original is never downloaded or opened (Pillow refuses it as a decompression bomb). Any other address as it is."""
    m = re.match(r"https://upload\.wikimedia\.org/wikipedia/commons/[0-9a-f]/[0-9a-f]{2}/([^/?#]+)$", url)
    return f"https://commons.wikimedia.org/wiki/Special:FilePath/{m.group(1)}?width=3840" if m else url


def focus_crop(data, fx, fy):
    """The source cut to 16:9 around (fx, fy) (0..1 of its width and height), kept inside the frame; JPEG quality 95."""
    from PIL import Image
    im = Image.open(io.BytesIO(data)).convert("RGB")
    w, h = im.size
    if w * 9 > h * 16:   # wider than 16:9: choose where the 16:9 window sits across
        cw, ch = h * 16 // 9, h
    else:                # taller: choose where it sits down
        cw, ch = w, w * 9 // 16
    left = min(max(0, int(fx * w - cw / 2)), w - cw)
    top = min(max(0, int(fy * h - ch / 2)), h - ch)
    buf = io.BytesIO()
    im.crop((left, top, left + cw, top + ch)).save(buf, "JPEG", quality=95)
    return buf.getvalue()


def sheet(zpath, path):
    import zipfile
    from PIL import Image, ImageDraw
    with zipfile.ZipFile(zpath) as z:
        names = sorted([n for n in z.namelist() if n.endswith(".jpg")], key=lambda n: int(n.split(".")[0]))
        W, H, cols = 384, 216, 5
        rows = (len(names) + cols - 1) // cols
        im = Image.new("RGB", (cols * W, rows * H), (20, 20, 20))
        d = ImageDraw.Draw(im)
        for i, n in enumerate(names):
            t = Image.open(io.BytesIO(z.read(n))).convert("RGB").resize((W, H))
            im.paste(t, ((i % cols) * W, (i // cols) * H))
            d.rectangle([(i % cols) * W, (i // cols) * H, (i % cols) * W + 46, (i // cols) * H + 26], fill=(0, 0, 0))
            d.text(((i % cols) * W + 6, (i // cols) * H + 6), n.split(".")[0], fill=(255, 255, 0))
    im.save(path, "JPEG", quality=82)


def main():
    picks = sorted(SRC.glob("*.json")) if SRC.is_dir() else []
    if not picks:
        print("no picked packs: nothing to build")
        return 0
    OUT.mkdir(parents=True, exist_ok=True)
    index = []
    for p in picks:
        pk = json.loads(p.read_text(encoding="utf-8"))
        pid = pk["id"]
        work = ROOT / "work" / pid
        work.mkdir(parents=True, exist_ok=True)
        photos = []
        for i, ph in enumerate(pk["photos"], 1):
            name = f"src{i:03d}.img"   # Pillow reads the format from the bytes
            data = get(scaled(ph["url"]))
            if ph.get("focus"):   # the subject away from the clock: cut 16:9 around this point first (make_pack.py then only resizes)
                data = focus_crop(data, *ph["focus"])
            (work / name).write_bytes(data)
            e = {"file": name, "title": ph["title"], "author": ph["author"], "licence": ph["licence"], "link": ph["link"]}
            for k in ("licence_url", "changes"):   # a CC BY / BY-SA photo's licence address and what was changed (make_pack.py requires them)
                if ph.get(k):
                    e[k] = ph[k]
            photos.append(e)
        (work / "manifest.json").write_text(json.dumps({"reviewed": pk.get("reviewed") is True, "photos": photos}, ensure_ascii=False, indent=1),
                                            encoding="utf-8")
        idx = OUT / "index.json"
        if not idx.exists():
            idx.write_text('{"version": 1, "packs": []}\n', encoding="utf-8")
        subprocess.run([sys.executable, str(ROOT / ".github/tools/make_pack.py"), "build", "--id", pid, "--name", pk["name"], "--name-en", pk["name_en"],
                        "--source", pk["source"], "--source-en", pk["source_en"], "--src", str(work), "--out", str(OUT),
                        "--index", str(idx), "--covers", str(OUT / "covers")] + (["--allow-attribution"] if pk.get("allow_attribution") is True else []),
                       check=True)
        zpath = OUT / f"{pid}.zip"
        sheet(zpath, OUT / f"{pid}-sheet.jpg")
        tag = f"photos-{pid}"
        have = subprocess.run(["gh", "release", "view", tag], capture_output=True).returncode == 0
        if have:
            subprocess.run(["gh", "release", "upload", tag, str(zpath), "--clobber"], check=True)
        else:
            subprocess.run(["gh", "release", "create", tag, str(zpath), "--title", f"Photo pack: {pk['name_en']}",
                            "--notes", f"Screensaver photo pack «{pk['name']}» ({len(photos)} photos). Credits are inside the zip (credits.json).",
                            "--latest=false"], check=True)
        print(f"{pid}: {len(photos)} photos, released as {tag}", flush=True)
        zpath.unlink()   # the release has it; the data branch keeps the index line, the cover and the sheet
    return 0


if __name__ == "__main__":
    sys.exit(main())
