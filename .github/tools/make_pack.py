#!/usr/bin/env python3
"""Builds a screensaver photo pack (docs/PHOTO-PACKS.md) and puts it in the index.

  make_pack.py build --id space --name الفضاء --name-en Space --source "ناسا، ملكية عامة" --source-en "NASA, public domain" \
                     --src DIR [--out build/packs] [--index photos/packs.json] [--covers photos/covers]
  make_pack.py nasa  --query nebula --query "mars surface" --limit 12 --dir DIR      (candidates from images-api.nasa.gov, no key)

DIR holds the source images and manifest.json:
  {"reviewed": true,
   "photos": [{"file": "a.jpg", "title": "...", "author": "NASA/JPL", "licence": "Public domain", "link": "https://..."}, ...]}
`build` makes out/ID.zip (1920x1080 JPEGs 1.jpg..N.jpg and credits.json), its sha256, a cover, and writes the pack's line in the index.
The release that carries the zip is `photos-ID` on tshi-dev/UrTV, asset ID.zip (the address the app accepts, PhotoPacks.validUrl).

The content rule (docs/PROPOSALS-NOV2.md, Photo packs) as far as a script can hold it, and a refusal otherwise:
  - only CC0 or public domain is taken as it is; CC BY and CC BY-SA only with --allow-attribution (their credit is shown in the app);
    any other or unknown licence is refused, with the file named.
  - no people, no religious buildings other than mosques as architecture, no statues, no nudity: a title, tag or note that names any of
    these is refused, and nothing is accepted until a person has looked at every photo and set "reviewed": true in the manifest.
  - a photo under 1280x720 is refused (it would be stretched), a pack of more than 200 photos or more than 80 MB too.
The zip is the same bytes for the same photos (fixed times and order), so its sha256 does not change for nothing.
"""
import argparse
import hashlib
import io
import json
import re
import sys
import zipfile
from pathlib import Path

W, H = 1920, 1080
MIN_W, MIN_H = 1280, 720
MAX_PHOTOS, MAX_ZIP = 200, 80 << 20
PHOTO_MAX = 3 << 20
RELEASES = "https://github.com/tshi-dev/UrTV/releases/download/"
RAW = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/"

# taken as they are
FREE = {"cc0", "cc0 1.0", "cc-0", "public domain", "pd", "pdm", "pd-usgov-nasa", "public domain (nasa)", "public domain mark", "pd-old"}
# taken only with --allow-attribution
ATTRIBUTION = {"cc by", "cc by 2.0", "cc by 3.0", "cc by 4.0", "cc by-sa", "cc by-sa 2.0", "cc by-sa 3.0", "cc by-sa 4.0"}

# words in a title, a tag or a note that make a photo not ours (the rule: no people, no sacred buildings but mosques, no statues, no nudity)
REFUSED_WORDS = re.compile(
    r"\b(people|person|man|woman|men|women|boy|girl|child|children|kid|crowd|astronaut|astronauts|cosmonaut|crew|portrait|selfie|face|faces|"
    r"worshipp?ers?|pilgrims?|statue|statues|sculpture|idol|idols|buddha|deity|church|cathedral|chapel|temple|synagogue|shrine|monastery|"
    r"nude|nudity|naked|bikini|flag|logo|insignia|patch)\b", re.I)
PEOPLE_AR = re.compile("(شخص|أشخاص|رجل|امرأة|طفل|وجه|وجوه|تمثال|أصنام|كنيسة|معبد)")


def norm_licence(s):
    return re.sub(r"\s+", " ", (s or "").strip().lower())


def check_photo(entry, allow_attribution):
    """[] when the entry may go in, else the reasons."""
    why = []
    lic = norm_licence(entry.get("licence"))
    if lic in FREE:
        pass
    elif lic in ATTRIBUTION:
        if not allow_attribution:
            why.append(f"licence {entry.get('licence')!r} needs --allow-attribution")
    else:
        why.append(f"licence {entry.get('licence')!r} is not allowed (CC0 or public domain; CC BY/BY-SA with --allow-attribution)")
    for k in ("title", "author", "link"):
        if not str(entry.get(k) or "").strip():
            why.append(f"no {k}")
    if not str(entry.get("link") or "").startswith("https://"):
        why.append("the link must be https")
    text = " ".join(str(entry.get(k) or "") for k in ("title", "tags", "note", "description"))
    m = REFUSED_WORDS.search(text) or PEOPLE_AR.search(text)
    if m:
        why.append(f"the content rule: {m.group(0)!r} in its title or notes")
    if entry.get("people"):
        why.append("marked as showing people")
    if not re.fullmatch(r"[\w.-]+", str(entry.get("file") or "")) or "/" in str(entry.get("file")):
        why.append("the file name must be a plain name")
    return why


def to_jpeg(data):
    """1920x1080 JPEG bytes (cover-cropped, no metadata) and the source size, or raises ValueError."""
    from PIL import Image, ImageOps
    im = Image.open(io.BytesIO(data))
    im.load()
    if im.width < MIN_W or im.height < MIN_H:
        raise ValueError(f"{im.width}x{im.height} is under {MIN_W}x{MIN_H}")
    im = ImageOps.exif_transpose(im).convert("RGB")
    im = ImageOps.fit(im, (W, H), Image.LANCZOS)
    for q in (88, 84, 80, 75, 70, 65, 60):
        buf = io.BytesIO()
        im.save(buf, "JPEG", quality=q, optimize=True)   # a new file: no EXIF, no location
        if buf.tell() <= PHOTO_MAX:
            return buf.getvalue()
    raise ValueError("does not fit in 3 MB even at quality 60")


def cover_jpeg(photo):
    from PIL import Image
    im = Image.open(io.BytesIO(photo)).convert("RGB").resize((480, 270), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "JPEG", quality=80, optimize=True)
    return buf.getvalue()


def make_zip(photos, credits):
    """The zip's bytes: stored (a JPEG does not shrink), fixed times, in order, so the same photos give the same zip."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_STORED) as z:
        for i, data in enumerate(photos, 1):
            zi = zipfile.ZipInfo(f"{i}.jpg", (2026, 1, 1, 0, 0, 0))
            zi.external_attr = 0o644 << 16
            z.writestr(zi, data)
        zi = zipfile.ZipInfo("credits.json", (2026, 1, 1, 0, 0, 0))
        zi.external_attr = 0o644 << 16
        z.writestr(zi, json.dumps(credits, ensure_ascii=False, indent=1).encode("utf-8"))
    return buf.getvalue()


def build(args):
    """Returns the pack's index entry; raises SystemExit with the reasons when the rule is not met."""
    if not re.fullmatch(r"[a-z][a-z0-9-]{1,30}", args.id):
        sys.exit("the id is lower-case letters, digits and dashes, 2 to 31 long, starting with a letter")
    src = Path(args.src)
    mf = json.loads((src / "manifest.json").read_text(encoding="utf-8"))
    entries = mf.get("photos", [])
    problems = []
    if mf.get("reviewed") is not True:
        problems.append('a person has not reviewed the pack: look at every photo, then set "reviewed": true in manifest.json')
    if not 1 <= len(entries) <= MAX_PHOTOS:
        problems.append(f"{len(entries)} photos: 1 to {MAX_PHOTOS} are allowed")
    seen = set()
    for e in entries:
        if e.get("file") in seen:
            problems.append(f"{e.get('file')}: listed twice")
        seen.add(e.get("file"))
        for w in check_photo(e, args.allow_attribution):
            problems.append(f"{e.get('file')}: {w}")
    photos = []
    if not problems:
        for e in entries:
            try:
                photos.append(to_jpeg((src / e["file"]).read_bytes()))
            except (OSError, ValueError) as x:
                problems.append(f"{e['file']}: {x}")
    if problems:
        sys.exit("refused:\n  " + "\n  ".join(problems))
    credits = {"pack": args.id, "photos": [{"file": f"{i}.jpg", "title": e["title"], "author": e["author"], "licence": e["licence"], "link": e["link"]}
                                           for i, e in enumerate(entries, 1)]}
    zbytes = make_zip(photos, credits)
    if len(zbytes) > MAX_ZIP:
        sys.exit(f"the zip is {len(zbytes) >> 20} MB: over 80 MB")
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / f"{args.id}.zip").write_bytes(zbytes)
    entry = {"id": args.id, "name": args.name, "name_en": args.name_en, "source": args.source, "source_en": args.source_en,
             "cover": "", "url": f"{RELEASES}photos-{args.id}/{args.id}.zip", "sha256": hashlib.sha256(zbytes).hexdigest(),
             "count": len(photos), "bytes": len(zbytes)}
    if args.covers:
        cdir = Path(args.covers)
        cdir.mkdir(parents=True, exist_ok=True)
        (cdir / f"{args.id}.jpg").write_bytes(cover_jpeg(photos[0]))
        entry["cover"] = f"{RAW}photos/covers/{args.id}.jpg"
    if args.index:
        put_in_index(Path(args.index), entry)
    return entry


def put_in_index(path, entry):
    idx = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"version": 1, "packs": []}
    packs = [p for p in idx.get("packs", []) if p.get("id") != entry["id"]]
    packs.append(entry)
    idx["version"] = 1
    idx["packs"] = packs
    path.write_text(json.dumps(idx, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


# ---- NASA candidates: a start for a manifest, never a pack by itself (a person still reviews every photo) ----

def nasa_candidates(queries, limit, get_json):
    """[{nasa_id, title, tags, link}] for the queries; get_json(url) gives the parsed answer (injected, so a test needs no network)."""
    out, seen = [], set()
    for q in queries:
        url = "https://images-api.nasa.gov/search?media_type=image&page_size=100&q=" + q.replace(" ", "%20")
        for item in get_json(url).get("collection", {}).get("items", []):
            data = (item.get("data") or [{}])[0]
            nid = data.get("nasa_id", "")
            text = " ".join([data.get("title", ""), data.get("description", ""), " ".join(data.get("keywords", []) or [])])
            if not nid or nid in seen or REFUSED_WORDS.search(text) or data.get("secondary_creator"):
                continue
            seen.add(nid)
            out.append({"nasa_id": nid, "title": data.get("title", ""), "tags": ", ".join(data.get("keywords", []) or []),
                        "author": data.get("photographer") or data.get("center") or "NASA", "licence": "Public domain",
                        "link": f"https://images.nasa.gov/details/{nid}"})
            if len(out) >= limit:
                return out
    return out


def nasa(args):
    import urllib.request

    def get_json(u):
        with urllib.request.urlopen(u, timeout=30) as r:
            return json.loads(r.read())

    found = nasa_candidates(args.query, args.limit, get_json)
    d = Path(args.dir)
    d.mkdir(parents=True, exist_ok=True)
    photos = []
    for c in found:
        assets = get_json(f"https://images-api.nasa.gov/asset/{c['nasa_id']}").get("collection", {}).get("items", [])
        big = next((a["href"] for a in assets if a["href"].lower().endswith(".jpg") and "~orig" in a["href"]), None) or \
            next((a["href"] for a in assets if a["href"].lower().endswith(".jpg") and "~large" in a["href"]), None)
        if not big:
            continue
        name = c["nasa_id"].replace("/", "_") + ".jpg"
        with urllib.request.urlopen(big.replace("http://", "https://"), timeout=60) as r:
            (d / name).write_bytes(r.read())
        photos.append({"file": name, "title": c["title"], "tags": c["tags"], "author": c["author"], "licence": c["licence"], "link": c["link"]})
    (d / "manifest.json").write_text(json.dumps({"reviewed": False, "photos": photos}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"{len(photos)} candidates in {d}; look at each, remove any with people, logos or a third party's © , then set \"reviewed\": true")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    sub = ap.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build")
    b.add_argument("--id", required=True)
    b.add_argument("--name", required=True)
    b.add_argument("--name-en", required=True)
    b.add_argument("--source", required=True)
    b.add_argument("--source-en", required=True)
    b.add_argument("--src", required=True)
    b.add_argument("--out", default="build/packs")
    b.add_argument("--index", default="")
    b.add_argument("--covers", default="")
    b.add_argument("--allow-attribution", action="store_true")
    n = sub.add_parser("nasa")
    n.add_argument("--query", action="append", required=True)
    n.add_argument("--limit", type=int, default=12)
    n.add_argument("--dir", required=True)
    args = ap.parse_args(argv)
    if args.cmd == "build":
        e = build(args)
        print(f"{e['id']}: {e['count']} photos, {e['bytes']} bytes, sha256 {e['sha256']}")
        print(f"upload build/packs/{e['id']}.zip as the asset of release photos-{e['id']}")
    else:
        nasa(args)


if __name__ == "__main__":
    main()
