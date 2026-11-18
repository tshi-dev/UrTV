#!/usr/bin/env python3
"""What the LG (ports/webos/build.py) and Samsung (ports/tizen/build.py) packages share: the page, its files and the data the settings screen needs.

BETA: not tested on a real LG or Samsung TV. See docs/LG-SAMSUNG.md.

The page is dist/tv.html, the one the Android screensaver shows, unchanged except for two things done here and nowhere else:
  - the fonts and the five bundled photos, which that page asks GitHub for, are packaged with the app and asked for by their own path;
  - ports/tvports/shell.js (and the data it needs) is the first script of <head>: the settings, the remote's keys and the adhan.
The private Thmanyah fonts are read from assets/fonts if they are there (a machine that has them); otherwise the page falls back to the
system's Arabic font, exactly as dist/urtv.html does on a machine without them. They are never committed.

Packing the archives (ar, tar.gz) is adapted from Free My TV (https://github.com/simbasjungles/free-my-tv, MIT), see THIRD_PARTY_NOTICES.md.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPO = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/"
FONT_NAMES = ["Thmanyah-Light.woff2", "Thmanyah-Regular.woff2", "Thmanyah-Medium.woff2", "ThmanyahDisplay-Bold.woff2", "UthmanicHafs-v30.woff2"]
VERSION = "0.1.0"   # the apps' own version (appinfo.json, config.xml); not the page's

# Limits the packages are tested against (tests/tvports_test.py): a TV's app storage is small and its browser slow to parse.
MAX_PACKAGE_BYTES = 8 * 1024 * 1024
MAX_PAGE_BYTES = 1536 * 1024
MAX_FILE_BYTES = 2 * 1024 * 1024
MAX_FILES = 40

CARDS = [  # the cards of the page that need nothing from the Android apps (not the matches, stocks, tasks, shows: those need their pickers)
    ("verse", "آية اليوم وحديث اليوم"), ("wisdom", "حكمة اليوم"), ("adhkar", "أذكار الصباح والمساء"),
    ("prayers", "مواقيت الصلاة اليوم"), ("prayerrow", "الصلوات في صف"), ("forecast", "طقس الأيام القادمة"),
    ("hourly", "طقس الساعات القادمة"), ("year", "السنة"), ("none", "بدون بطاقة"),
]


def page():
    """dist/tv.html as text; built first (python3 src/build.py) when it is not there."""
    path = ROOT / "dist" / "tv.html"
    if not path.is_file():
        subprocess.run([sys.executable, str(ROOT / "src" / "build.py")], check=True, cwd=ROOT)
    return path.read_text(encoding="utf-8")


def cities():
    """The regions and cities of the screensaver's own list (android/app/.../Cities.java, generated from GeoNames): Saudi Arabia's
    regions, then the other countries, each city as {name, lat, lon, tz}. A city without a time zone there is a Saudi one."""
    text = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Cities.java").read_text(encoding="utf-8")
    out = []
    for block in re.split(r'new Region\(', text)[1:]:
        name = re.match(r'"([^"]+)"', block).group(1)
        rows = re.findall(r'\{"([^"]+)",\s*"(-?[\d.]+)",\s*"(-?[\d.]+)"(?:,\s*"([A-Za-z_]+/[A-Za-z_\-]+)")?\}', block)
        out.append({"name": name, "cities": [{"name": n, "lat": float(a), "lon": float(o), "tz": z or "Asia/Riyadh"} for n, a, o, z in rows]})
    return [r for r in out if r["cities"]]


def voices():
    """The adhan recordings of the Android app (android/launcher/.../tilawah/Adhan.java, the Archive.org «Azaan of World Mosques»
    collection, Public Domain Mark): ({n, name, place, url} for the normal voices, the same for the Fajr ones)."""
    base = ROOT / "android/launcher/src/main/java/sa/urtv/home/tilawah"
    text = (base / "Adhan.java").read_text(encoding="utf-8")

    def strings(name):
        body = re.search(name + r'\s*=\s*\{(.*?)\};', text, re.S).group(1)
        return re.findall(r'"([^"]*)"', body)

    names, places, urls = strings("NAMES"), strings("PLACES"), strings("URLS")
    fajr = [int(x) for x in re.search(r'FAJR\s*=\s*\{([\d,\s]+)\}', (base / "AdhanVoices.java").read_text(encoding="utf-8")).group(1).split(",")]
    every = [{"n": i + 1, "name": names[i], "place": places[i], "url": urls[i]} for i in range(len(urls))]
    return [v for v in every if v["n"] not in fajr], [v for v in every if v["n"] in fajr]


def data_js():
    normal, fajr = voices()
    data = {"regions": cities(), "voices": normal, "fajrVoices": fajr, "cards": [{"id": i, "name": n} for i, n in CARDS]}
    return "window.URTV_TV_DATA=" + json.dumps(data, ensure_ascii=False, separators=(",", ":")) + ";\n"


def files(extra=None):
    """The app's files, {path in the app: bytes}, without the manifest and icons of the platform (its build.py adds them)."""
    html = page()
    out = {}
    for name in FONT_NAMES:   # the page's fonts, from GitHub there, from the app here; a font this machine does not have: none, the system's
        src = ROOT / "assets" / "fonts" / name
        url = REPO + "assets/fonts/" + name
        if src.is_file():
            out["fonts/" + name] = src.read_bytes()
            html = html.replace(url, "fonts/" + name)
        else:
            html = html.replace(url, "data:,")
    for src in sorted((ROOT / "assets" / "backgrounds").glob("*.jpg")):
        url = REPO + "assets/backgrounds/" + src.name
        if url in html:
            out["bg/" + src.name] = src.read_bytes()
            html = html.replace(url, "bg/" + src.name)
    head = '<meta charset="utf-8">'
    if head not in html:
        raise SystemExit("dist/tv.html has no <meta charset> to put the app's scripts after: was the page changed? ports/tvports/common.py needs updating.")
    html = html.replace(head, head + '\n<script src="data.js"></script>\n<script src="shell.js"></script>', 1)
    out["index.html"] = html.encode("utf-8")
    out["data.js"] = data_js().encode("utf-8")
    out["shell.js"] = (ROOT / "ports" / "tvports" / "shell.js").read_bytes()
    out.update(extra or {})
    return out


def preview(app, out_dir):
    """The app's files in out_dir, to open index.html in a browser."""
    out_dir = Path(out_dir)
    for path, data in app.items():
        target = out_dir / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
