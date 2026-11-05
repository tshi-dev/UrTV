#!/usr/bin/env python3
"""The pack builder (.github/tools/make_pack.py): a pack of CC0 photos with a person's review becomes a zip of 1920x1080 JPEGs 1..N and credits.json,
with its sha256 in the index; a licence that is not allowed, a title that names people, a pack nobody reviewed, a photo too small are each refused.
The zip is the one the app accepts (only N.jpg and credits.json; its sha256 is what the index says; the same photos give the same zip).
Photos here are drawn gradients (no real photo is needed for a rule). Run: python3 tests/make_pack_test.py   (needs Pillow)"""
import hashlib
import importlib.util
import json
import re
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("make_pack", ROOT / ".github/tools/make_pack.py")
mp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mp)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


try:
    from PIL import Image
except ImportError:
    print("SKIP make_pack: Pillow is not installed")
    sys.exit(0)


def draw(path, size, seed):
    im = Image.new("RGB", size)
    px = im.load()
    for x in range(0, size[0], 8):
        for y in range(0, size[1], 8):
            c = ((x * 255 // size[0] + seed * 40) % 256, (y * 255 // size[1]) % 256, (seed * 70) % 256)
            for i in range(8):
                for j in range(8):
                    if x + i < size[0] and y + j < size[1]:
                        px[x + i, y + j] = c
    im.save(path, "JPEG", quality=90)


def entry(f, **kw):
    e = {"file": f, "title": "A nebula in deep space", "author": "NASA/JPL", "licence": "Public domain", "link": "https://images.nasa.gov/details/x"}
    e.update(kw)
    return e


def args(src, out, index, covers, **kw):
    a = mp.argparse.Namespace(id="space", name="الفضاء", name_en="Space", source="ناسا، ملكية عامة", source_en="NASA, public domain",
                              src=str(src), out=str(out), index=str(index), covers=str(covers), allow_attribution=False)
    a.__dict__.update(kw)
    return a


def refused(a):
    try:
        mp.build(a)
    except SystemExit as e:
        return str(e)
    return None


with tempfile.TemporaryDirectory() as t:
    t = Path(t)
    src = t / "src"
    src.mkdir()
    draw(src / "a.jpg", (2400, 1350), 1)
    draw(src / "b.jpg", (1920, 1280), 2)      # not 16:9: cropped
    draw(src / "small.jpg", (800, 450), 3)
    out, idx, covers = t / "out", t / "index.json", t / "covers"
    (src / "manifest.json").write_text(json.dumps({"reviewed": True, "photos": [entry("a.jpg"), entry("b.jpg", licence="CC0 1.0", title="Rings")]}))
    e = mp.build(args(src, out, idx, covers))
    z = out / "space.zip"
    names = zipfile.ZipFile(z).namelist()
    check("the zip holds 1.jpg, 2.jpg and credits.json, nothing else", names == ["1.jpg", "2.jpg", "credits.json"], names)
    check("every name in it is one the app accepts", all(re.fullmatch(r"[1-9][0-9]{0,2}\.jpg|credits\.json", n) for n in names))
    with zipfile.ZipFile(z) as zf:
        sizes = [Image.open(zf.open(f"{i}.jpg")).size for i in (1, 2)]
        check("each photo is 1920x1080", sizes == [(1920, 1080)] * 2, sizes)
        check("a photo is under 3 MB and has no EXIF", all(zf.getinfo(f"{i}.jpg").file_size <= 3 << 20 for i in (1, 2))
              and not Image.open(zf.open("1.jpg")).getexif())
        cr = json.loads(zf.read("credits.json"))
    check("credits.json: author, licence, link for each photo", [p["file"] for p in cr["photos"]] == ["1.jpg", "2.jpg"]
          and all(p["author"] and p["licence"] and p["link"].startswith("https://") for p in cr["photos"]), cr)
    check("the index line's sha256 and size are the zip's", e["sha256"] == hashlib.sha256(z.read_bytes()).hexdigest() and e["bytes"] == z.stat().st_size and e["count"] == 2)
    check("its address is our release, photos-ID/ID.zip", e["url"] == "https://github.com/tshi-dev/UrTV/releases/download/photos-space/space.zip")
    i = json.loads(idx.read_text())
    check("the index has the pack, version 1; a rebuild replaces the line, not adds one", i["version"] == 1 and [p["id"] for p in i["packs"]] == ["space"]
          and mp.build(args(src, out, idx, covers)) and len(json.loads(idx.read_text())["packs"]) == 1)
    check("the same photos give the same zip (the sha256 does not change)", json.loads(idx.read_text())["packs"][0]["sha256"] == e["sha256"])
    check("a cover is made and the index points at it", (covers / "space.jpg").is_file() and Image.open(covers / "space.jpg").size == (480, 270) and e["cover"].endswith("/photos/covers/space.jpg"))

    def with_manifest(m):
        (src / "manifest.json").write_text(json.dumps(m))
        return refused(args(src, t / "o2", t / "i2.json", ""))

    check("a pack nobody reviewed is refused", "reviewed" in (with_manifest({"reviewed": False, "photos": [entry("a.jpg")]}) or ""))
    check("an unknown licence is refused, naming the file", "a.jpg" in (with_manifest({"reviewed": True, "photos": [entry("a.jpg", licence="All rights reserved")]}) or ""))
    check("CC BY is refused unless --allow-attribution", with_manifest({"reviewed": True, "photos": [entry("a.jpg", licence="CC BY 4.0")]}) is not None
          and refused(args(src, t / "o3", "", "", allow_attribution=True)) is None)
    for bad in ("An astronaut on a spacewalk", "Portrait of a man", "The statue at dawn", "St. Mary's cathedral", "Crowd at the market", "NASA logo on the wall"):
        check(f"the content rule: «{bad}» is refused", "content rule" in (with_manifest({"reviewed": True, "photos": [entry("a.jpg", title=bad)]}) or ""))
    check("a photo marked as showing people is refused", "people" in (with_manifest({"reviewed": True, "photos": [entry("a.jpg", people=True)]}) or ""))
    check("a photo under 1280x720 is refused", "small.jpg" in (with_manifest({"reviewed": True, "photos": [entry("small.jpg")]}) or ""))
    check("a file name with a path is refused", with_manifest({"reviewed": True, "photos": [entry("../a.jpg")]}) is not None)
    check("the same file twice is refused", "twice" in (with_manifest({"reviewed": True, "photos": [entry("a.jpg"), entry("a.jpg")]}) or ""))
    check("a bad id is refused", refused(args(src, t / "o4", "", "", id="Space")) is not None)

# NASA candidates: people, third-party credits and repeats are left out (no network: the answer is given)
def fake(url):
    def item(nid, title, **kw):
        d = {"nasa_id": nid, "title": title, "keywords": kw.get("kw", []), "description": kw.get("desc", "")}
        d.update({k: v for k, v in kw.items() if k in ("secondary_creator", "photographer")})
        return {"data": [d]}
    return {"collection": {"items": [item("N1", "Orion Nebula"), item("N2", "Astronaut on the station"), item("N3", "Galaxy", kw=["people"]),
                                     item("N4", "Mars dunes", photographer="NASA/JPL"), item("N1", "Orion Nebula"), item("N5", "Moon", secondary_creator="Someone")]}}


c = mp.nasa_candidates(["nebula"], 10, fake)
check("NASA candidates: people, a third-party creator and a repeat are left out", [x["nasa_id"] for x in c] == ["N1", "N4"], c)
check("NASA candidates: public domain, a link to the record, a photographer when known", c[1]["author"] == "NASA/JPL" and c[0]["licence"] == "Public domain" and c[0]["link"].endswith("/N1"))
check("NASA candidates stop at the limit", len(mp.nasa_candidates(["a"], 1, fake)) == 1)
sys.exit(1 if failed else 0)
