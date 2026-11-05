#!/usr/bin/env python3
"""Candidates for the photo packs (docs/PHOTO-PACKS.md), from Wikimedia Commons, in CI (the dev container cannot reach Commons).

Only files whose own metadata says Public domain or CC0 (extmetadata LicenseShortName), with no «Restrictions» (personality rights,
trademarks, insignia), at least 1920x1080 and landscape, and none whose title, description or categories name a place or a thing the
content rule leaves out (Israel and its cities, maps, people, shrines and other sacred buildings but mosques, statues, flags, logos). What
is left is shown on numbered contact sheets for a person to look at; nothing is published from here.

Out (OUT, default packs-candidates/): <pack>.json (every candidate: n, title, author, licence, link, url, width, height) and
<pack>-<k>.jpg (sheets of 20, 5 x 4, 384x216 each, the number in a corner).

    python3 .github/tools/pack_candidates.py [pack ...]
"""
import html
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "UrTV-photo-packs/1.0 (https://github.com/tshi-dev/UrTV)"}
OUT = Path(os.environ.get("OUT", "packs-candidates"))
FREE = {"public domain": "Public domain", "cc0": "CC0", "pdm-owner": "Public domain", "pd": "Public domain"}
# the owner allowed attribution licences for «مدن السعودية» only (never NC or ND): their exact names, as make_pack.py's ATTRIBUTION
BY = {f"cc {k} {v}": f"CC {k.upper()} {v}" for k in ("by", "by-sa") for v in ("2.0", "3.0", "4.0")}
ATTRIBUTION_PACKS = {"ksa-cities"}
MAX_PER_PACK = 220   # looked at for calm; the SHEET_N calmest go on the sheets
SHEET_N = 60

# searches per pack (CirrusSearch: incategory: / deepcat: and words); the licence is checked on each file, not trusted from the search
PACKS = {
    "space": ['Earth limb ISS filetype:bitmap', '"Earth horizon" ISS filetype:bitmap', 'aurora seen from the International Space Station filetype:bitmap',
              'Earth at night from ISS filetype:bitmap', 'sunrise from ISS orbit filetype:bitmap', 'Mars Curiosity panorama filetype:bitmap',
              'Mars Perseverance panorama filetype:bitmap', 'Mars sunset Curiosity filetype:bitmap', 'Carina nebula hubble filetype:bitmap',
              'Orion nebula hubble filetype:bitmap', 'Pillars of Creation filetype:bitmap', 'Heart and Soul nebulae', 'Cassiopeia A hubble filetype:bitmap',
              'Earth from space Apollo filetype:bitmap', 'Moon limb from ISS filetype:bitmap'],
    "nature": ['incategory:"CC-Zero" lake mountains', 'incategory:"CC-Zero" fog forest', 'incategory:"CC-Zero" snow mountains', 'incategory:"CC-Zero" meadow',
               'incategory:"CC-Zero" lake reflection', 'incategory:"CC-Zero" sand dunes', 'incategory:"CC-Zero" sunset landscape', 'incategory:"CC-Zero" valley',
               'Grand Teton National Park NPS filetype:bitmap', 'Yellowstone landscape NPS filetype:bitmap', 'Glacier National Park landscape NPS filetype:bitmap',
               'Great Sand Dunes National Park NPS filetype:bitmap', 'Death Valley dunes NPS filetype:bitmap', 'incategory:"CC-Zero" mountains'],
    "sea": ['incategory:"CC-Zero" sea horizon', 'incategory:"CC-Zero" calm sea', 'incategory:"CC-Zero" beach', 'incategory:"CC-Zero" ocean',
            'incategory:"CC-Zero" sea sunset', 'incategory:"CC-Zero" coast cliffs', 'incategory:"CC-Zero" lagoon', 'aerial beach CC0 filetype:bitmap',
            'coral reef NOAA filetype:bitmap', 'Papahanaumokuakea NOAA filetype:bitmap', 'ocean horizon public domain filetype:bitmap'],
    "ksa-cities": ['"Kingdom Centre" Riyadh filetype:bitmap', '"King Abdullah Financial District" filetype:bitmap', 'KAFD Riyadh filetype:bitmap',
                   'Diriyah At-Turaif filetype:bitmap', 'Riyadh skyline night filetype:bitmap', 'Al Faisaliyah Center filetype:bitmap',
                   'Jeddah corniche filetype:bitmap', 'Al-Balad Jeddah filetype:bitmap', 'King Fahd Fountain Jeddah filetype:bitmap',
                   'Abraj Al Bait filetype:bitmap', 'Masjid al-Haram exterior filetype:bitmap', 'Mecca skyline filetype:bitmap',
                   "Prophet's Mosque Medina filetype:bitmap", 'Quba Mosque filetype:bitmap', 'AlUla Elephant Rock filetype:bitmap',
                   'Hegra Mada\'in Salih tomb Qasr al-Farid filetype:bitmap', 'Maraya AlUla filetype:bitmap', 'AlUla landscape filetype:bitmap',
                   'Abha filetype:bitmap', 'Habala Asir filetype:bitmap', 'Khobar corniche filetype:bitmap', 'Khobar water tower filetype:bitmap',
                   'Taif Al Hada filetype:bitmap', 'Taif mountains filetype:bitmap', 'Tabuk mountains filetype:bitmap', 'Wadi Disah filetype:bitmap',
                   'Edge of the World Riyadh filetype:bitmap'],
}

# leave out, whatever the licence (the content rule; docs/PHOTO-PACKS.md and the owner's rules: no Israel locations, no Western Sahara maps)
BAD = re.compile(
    r"\b(israel|israeli|jerusalem|tel aviv|haifa|eilat|golan|galilee|tiberias|negev|dead sea|jaffa|akko|nazareth|western sahara|map|maps|people|person|man|woman|men|women|boy|girl|child|children|"
    r"crowd|crowds|pilgrim|pilgrims|hajj|worshippers?|astronaut|astronauts|cosmonaut|crew|portrait|selfie|face|statue|statues|sculpture|idol|"
    r"buddha|church|cathedral|chapel|temple|synagogue|shrine|imamzadeh|mausoleum|tomb|karbala|najaf|mashhad|qom|husayn|hussain|ashura|"
    r"nude|naked|bikini|swimsuit|flag|flags|logo|insignia|patch|wedding|party|festival|concert|military|soldier|weapon|bomb|explosion|"
    r"diagram|chart|graph|illustration|artist's impression|artist concept|rendering|render)\b", re.I)


def api(params):
    q = dict(params, format="json", formatversion=2)
    req = urllib.request.Request(API + "?" + urllib.parse.urlencode(q), headers=UA)
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r)
        except Exception:  # noqa: BLE001 - a candidates list, retried
            time.sleep(3 * (attempt + 1))
    return {}


def text(v):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", str(v or "")))).strip()


def search(query, limit=200):
    d = api({"action": "query", "generator": "search", "gsrsearch": query, "gsrnamespace": 6, "gsrlimit": 50, "prop": "imageinfo|categories",
             "iiprop": "url|size|extmetadata|mime", "iiurlwidth": 640, "cllimit": 50})
    pages = (d.get("query") or {}).get("pages") or []
    out = []
    for p in pages:
        ii = (p.get("imageinfo") or [{}])[0]
        md = ii.get("extmetadata") or {}
        out.append({"title": p.get("title", ""), "ii": ii, "md": md, "cats": [c.get("title", "") for c in p.get("categories") or []]})
    return out[:limit]


def accept(c, pack=""):
    ii, md = c["ii"], c["md"]
    lic = text((md.get("LicenseShortName") or {}).get("value")).lower()
    licence = FREE.get(lic) or ("Public domain" if lic.startswith("public domain") else None)
    licence_url = ""
    if not licence and pack in ATTRIBUTION_PACKS and lic in BY:
        licence = BY[lic]
        licence_url = text((md.get("LicenseUrl") or {}).get("value")).replace("http://", "https://")
        if not re.fullmatch(r"https://creativecommons\.org/licenses/by(-sa)?/[234]\.0/?", licence_url):
            return None, "licence url " + licence_url
    if not licence:
        return None, "licence " + lic
    if text((md.get("Restrictions") or {}).get("value")):
        return None, "restrictions"
    if ii.get("mime") not in ("image/jpeg", "image/png", "image/tiff"):
        return None, "mime"
    w, h = ii.get("width") or 0, ii.get("height") or 0
    if w < 1920 or h < 1080 or w < h * 1.3:
        return None, "size"
    blob = " ".join([c["title"], text((md.get("ImageDescription") or {}).get("value")), text((md.get("ObjectName") or {}).get("value"))] + c["cats"])
    m = BAD.search(blob)
    if m:
        return None, "word " + m.group(0)
    title = text((md.get("ObjectName") or {}).get("value")) or re.sub(r"\.\w+$", "", c["title"].replace("File:", ""))
    author = text((md.get("Artist") or {}).get("value")) or text((md.get("Credit") or {}).get("value")) or "Unknown"
    return {"title": title[:120], "author": author[:120], "licence": licence, "licence_url": licence_url, "link": ii.get("descriptionurl", ""),
            "url": ii.get("url", ""), "thumb": ii.get("thumburl", ""), "width": w, "height": h, "file": c["title"]}, ""


def calm(c):
    """How busy the clock's zone is (the middle 40% across, 20-62% down) against the whole frame: edge density there over the mean; lower is
    calmer. None when the thumbnail cannot be read."""
    from PIL import Image, ImageFilter, ImageStat
    try:
        req = urllib.request.Request(c["thumb"], headers=UA)
        with urllib.request.urlopen(req, timeout=60) as r:
            im = Image.open(io.BytesIO(r.read())).convert("L")
        im = im.resize((480, 270))
        e = im.filter(ImageFilter.FIND_EDGES)
        whole = ImageStat.Stat(e).mean[0] + 1
        mid = ImageStat.Stat(e.crop((144, 54, 336, 167))).mean[0]
        return round(mid / whole, 2)
    except Exception:  # noqa: BLE001
        return None


def sheet(cands, path):
    from PIL import Image, ImageDraw
    W, H, cols = 384, 216, 5
    rows = (len(cands) + cols - 1) // cols
    im = Image.new("RGB", (cols * W, rows * H), (20, 20, 20))
    d = ImageDraw.Draw(im)
    for i, c in enumerate(cands):
        try:
            req = urllib.request.Request(c["thumb"], headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r:
                t = Image.open(io.BytesIO(r.read())).convert("RGB")
            t.thumbnail((W, H))
            x, y = (i % cols) * W, (i // cols) * H
            im.paste(t, (x + (W - t.width) // 2, y + (H - t.height) // 2))
            d.rectangle([x, y, x + 46, y + 26], fill=(0, 0, 0))
            d.text((x + 6, y + 6), str(c["n"]), fill=(255, 255, 0))
            d.text((x + 6, y + 190), f'{c["width"]}w calm {c.get("calm")}', fill=(255, 255, 0))
        except Exception:  # noqa: BLE001
            pass
    im.save(path, "JPEG", quality=82)


def main(which):
    OUT.mkdir(parents=True, exist_ok=True)
    summary = {}
    for pack in which:
        seen, cands, why = set(), [], {}
        for q in PACKS[pack]:
            for c in search(q):
                if c["title"] in seen:
                    continue
                seen.add(c["title"])
                ok, reason = accept(c, pack)
                if ok:
                    cands.append(ok)
                else:
                    k = reason.split(" ")[0]
                    why[k] = why.get(k, 0) + 1
                if len(cands) >= MAX_PER_PACK:
                    break
            if len(cands) >= MAX_PER_PACK:
                break
        for c in cands:
            c["calm"] = calm(c)
        # the calmest first, and a wider source before a narrower one of the same calm (the owner: ≥2560 wide preferred)
        cands.sort(key=lambda c: (c["calm"] if c["calm"] is not None else 9, -min(c["width"], 4000)))
        for i, c in enumerate(cands, 1):
            c["n"] = i
        (OUT / f"{pack}.json").write_text(json.dumps(cands, ensure_ascii=False, indent=1), encoding="utf-8")
        for k in range(0, min(len(cands), SHEET_N), 20):
            sheet(cands[k:k + 20], OUT / f"{pack}-{k // 20 + 1}.jpg")
        summary[pack] = {"looked_at": len(seen), "candidates": len(cands), "left_out": why}
        print(pack, summary[pack], flush=True)
    (OUT / "summary.json").write_text(json.dumps(summary, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main(sys.argv[1:] or list(PACKS))
