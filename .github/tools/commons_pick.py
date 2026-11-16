#!/usr/bin/env python3
"""4.4 a photo for a horizon, from Wikimedia Commons, with its licence checked (run in CI: GitHub Actions can reach Commons).

    python3 .github/tools/commons_pick.py search OUT "<search words>" [Category:... ...]
        every bitmap in the categories and the search that is CC BY or CC BY-SA (any version), landscape (1.4 to 2.0 wide), at least 1920 px
        wide: OUT/candidates.json (title, licence, author, size, page) and OUT/sheet.jpg (numbered 640 px thumbnails) to choose from by eye.
    python3 .github/tools/commons_pick.py fetch OUT "File:<title>" <id>
        the original, its licence checked again, cropped to 16:9 about its centre and made 1920x1080: OUT/<id>.jpg and OUT/<id>.credit.txt
        (title, author, licence, page). Untouched otherwise: no colour change.
Needs Pillow. A clear client name and no personal data in the requests (Wikimedia's API policy)."""
import io, json, re, sys, urllib.parse, urllib.request
from pathlib import Path

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "UrTV-horizons/1.0 (https://github.com/tshi-dev/UrTV)"}
OK = re.compile(r"^CC BY(-SA)? \d\.\d$")


def get(params):
    q = urllib.request.Request(API + "?" + urllib.parse.urlencode({**params, "format": "json"}), headers=UA)
    return json.load(urllib.request.urlopen(q, timeout=60))


def info(titles):
    out = []
    for i in range(0, len(titles), 40):
        d = get({"action": "query", "titles": "|".join(titles[i:i + 40]), "prop": "imageinfo", "iiprop": "url|size|extmetadata|mime",
                 "iiurlwidth": 640})
        for p in d.get("query", {}).get("pages", {}).values():
            ii = (p.get("imageinfo") or [{}])[0]
            md = ii.get("extmetadata", {})
            lic = md.get("LicenseShortName", {}).get("value", "")
            w, h = ii.get("width", 0), ii.get("height", 0)
            if ii.get("mime") != "image/jpeg" or not OK.match(lic) or w < 1920 or not (1.4 <= w / max(h, 1) <= 2.0): continue
            author = re.sub("<[^>]+>", "", md.get("Artist", {}).get("value", "")).strip()
            out.append({"title": p["title"], "licence": lic, "author": author, "size": [w, h], "url": ii["url"], "thumb": ii.get("thumburl", ""),
                        "page": ii.get("descriptionurl", "")})
    return out


def search(out, words, cats):
    titles = set()
    for c in cats:
        cont = {}
        while True:
            d = get({"action": "query", "list": "categorymembers", "cmtitle": c, "cmtype": "file", "cmlimit": 200, **cont})
            titles |= {m["title"] for m in d["query"]["categorymembers"]}
            if "continue" not in d: break
            cont = d["continue"]
    d = get({"action": "query", "list": "search", "srsearch": words + " filetype:bitmap", "srnamespace": 6, "srlimit": 100})
    titles |= {m["title"] for m in d["query"]["search"]}
    cands = info(sorted(titles))
    out.mkdir(parents=True, exist_ok=True)
    (out / "candidates.json").write_text(json.dumps(cands, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    from PIL import Image, ImageDraw
    tiles = []
    for n, c in enumerate(cands):
        try: im = Image.open(io.BytesIO(urllib.request.urlopen(urllib.request.Request(c["thumb"], headers=UA), timeout=60).read())).convert("RGB")
        except Exception: continue
        im.thumbnail((640, 360)); t = Image.new("RGB", (640, 380), (20, 20, 20)); t.paste(im, (0, 20))
        ImageDraw.Draw(t).text((6, 4), f"{n}  {c['licence']}  {c['size'][0]}x{c['size'][1]}", fill=(255, 255, 0)); tiles.append(t)
    if tiles:
        sheet = Image.new("RGB", (640 * 3, 380 * ((len(tiles) + 2) // 3)))
        for i, t in enumerate(tiles): sheet.paste(t, ((i % 3) * 640, (i // 3) * 380))
        sheet.save(out / "sheet.jpg", quality=80)
    print(f"{len(cands)} candidates of {len(titles)} files")


def fetch(out, title, sid):
    c = info([title])
    if not c: sys.exit(f"{title}: not CC BY / CC BY-SA, too small or not landscape")
    c = c[0]
    from PIL import Image
    im = Image.open(io.BytesIO(urllib.request.urlopen(urllib.request.Request(c["url"], headers=UA), timeout=120).read())).convert("RGB")
    w, h = im.size
    cw, ch = (w, round(w * 9 / 16)) if w * 9 <= h * 16 else (round(h * 16 / 9), h)
    im = im.crop(((w - cw) // 2, (h - ch) // 2, (w - cw) // 2 + cw, (h - ch) // 2 + ch)).resize((1920, 1080), Image.LANCZOS)
    out.mkdir(parents=True, exist_ok=True)
    im.save(out / f"{sid}.jpg", quality=90)
    (out / f"{sid}.credit.txt").write_text(f"{c['title']}\n{c['author']}\n{c['licence']}\n{c['page']}\n", encoding="utf-8")
    print(json.dumps(c, ensure_ascii=False))


if __name__ == "__main__":
    a = sys.argv[1:]
    if len(a) >= 3 and a[0] == "search": search(Path(a[1]), a[2], a[3:])
    elif len(a) == 4 and a[0] == "fetch": fetch(Path(a[1]), a[2], a[3])
    else: sys.exit(__doc__)
