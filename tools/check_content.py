#!/usr/bin/env python3
"""The hand-edited files of content/ (docs/SCHEMAS.md: notices, devices, seasonal, prayer): their form, checked before anything is published
(content.yml). Exit 1 with every problem listed. Python standard library only:  python3 tools/check_content.py [content dir]"""
import datetime as dt
import json
import re
import sys
from pathlib import Path

ARABIC = re.compile(r"[؀-ۿ]")
VERSION = re.compile(r"\d+(\.\d+){0,3}")
ISO_DAY = re.compile(r"\d{4}-\d{2}-\d{2}")
ID = re.compile(r"[a-z0-9][a-z0-9-]{1,47}")
HTTPS = re.compile(r"https://[^\s\"'<>]+")
problems = []


def bad(where, what):
    problems.append(f"{where}: {what}")


def day(where, s):
    if not (isinstance(s, str) and ISO_DAY.fullmatch(s)):
        bad(where, f"not a date YYYY-MM-DD: {s!r}")
        return None
    try:
        return dt.date.fromisoformat(s)
    except ValueError:
        bad(where, f"no such day: {s}")
        return None


def span(where, o):
    a, b = day(where + ".from", o.get("from")), day(where + ".to", o.get("to"))
    if a and b and b < a:
        bad(where, "ends before it starts")


def ids(where, rows):
    seen = set()
    for i, r in enumerate(rows):
        w = f"{where}[{i}]"
        if not isinstance(r, dict):
            bad(w, "not an object")
            continue
        if not (isinstance(r.get("id"), str) and ID.fullmatch(r["id"])):
            bad(w, f"id: lower-case letters, digits and dashes: {r.get('id')!r}")
        elif r["id"] in seen:
            bad(w, f"id twice: {r['id']}")
        seen.add(r.get("id"))
        yield w, r


def notices(d):
    for w, n in ids("announcements.notices", d.get("notices", [])):
        t = n.get("text") or {}
        if not (isinstance(t.get("ar"), str) and ARABIC.search(t["ar"]) and 0 < len(t["ar"]) <= 140):
            bad(w, "text.ar: Arabic, 1 to 140 characters")
        if not (isinstance(t.get("en"), str) and 0 < len(t["en"]) <= 140):
            bad(w, "text.en: 1 to 140 characters")
        span(w, n)
        for k in ("min_app", "max_app"):
            if k in n and not (isinstance(n[k], str) and VERSION.fullmatch(n[k])):
                bad(w, f"{k}: a version like 4.3.3")
        if not isinstance(n.get("dismissible", True), bool):
            bad(w, "dismissible: true or false")
        if HTTPS.search(json.dumps(t, ensure_ascii=False)) or "http" in json.dumps(t).lower():
            bad(w, "no links in a notice (a TV cannot open them, and a link is a way to mislead)")


def devices(d):
    for w, r in ids("devices.devices", d.get("devices", [])):
        m = r.get("match") or {}
        if not m or not set(m) <= {"brand", "model", "device", "sdk_min", "sdk_max"}:
            bad(w, "match: some of brand, model, device, sdk_min, sdk_max")
        if not any(isinstance(m.get(k), str) and m[k] for k in ("brand", "model", "device")):
            bad(w, "match: a brand, model or device (never every TV)")
        for k in ("sdk_min", "sdk_max"):
            if k in m and not (isinstance(m[k], int) and 19 <= m[k] <= 40):
                bad(w, f"match.{k}: an Android SDK level")
        if not (isinstance(r.get("defaults"), dict) and r["defaults"]):
            bad(w, "defaults: at least one")
        elif not all(re.fullmatch(r"[a-z][a-z0-9_]{1,47}", k) and isinstance(v, (bool, int, str)) for k, v in r["defaults"].items()):
            bad(w, "defaults: snake_case names, plain values")
        if not (isinstance(r.get("why"), str) and r["why"]):
            bad(w, "why: say why")


def seasonal(d):
    for w, p in ids("seasonal.packs", d.get("packs", [])):
        span(w, p)
        nm = p.get("name") or {}
        if not (isinstance(nm.get("ar"), str) and ARABIC.search(nm["ar"]) and isinstance(nm.get("en"), str) and nm["en"]):
            bad(w, "name: ar and en")
        pid = p.get("id") if isinstance(p.get("id"), str) else ""
        if not re.fullmatch(r"[a-z][a-z0-9-]{1,30}", pid):
            bad(w, "id: a pack id (a letter, then letters, digits, dashes; 31 at most): the app's PhotoPacks.validId")
        if p.get("url") != f"https://github.com/tshi-dev/UrTV/releases/download/photos-{pid}/{pid}.zip":
            bad(w, "url: https://github.com/tshi-dev/UrTV/releases/download/photos-<id>/<id>.zip, the address the app accepts")
        if len(nm.get("ar") or "") > 40 or len(nm.get("en") or "") > 40:
            bad(w, "name: 40 characters at most")
        src = p.get("source") or {}
        if not (isinstance(src.get("ar"), str) and isinstance(src.get("en"), str) and src["ar"] and len(src["ar"]) <= 80 and len(src["en"]) <= 80):
            bad(w, "source: ar and en, 80 characters at most (the picker's credit line)")
        if not (isinstance(p.get("count"), int) and 1 <= p["count"] <= 200 and isinstance(p.get("bytes"), int) and 1 <= p["bytes"] <= 80 << 20):
            bad(w, "count 1..200 photos and bytes up to 80 MB")
        if "cover" in p and not (isinstance(p["cover"], str) and p["cover"].startswith("https://")):
            bad(w, "cover: an https address")
        if not (isinstance(p.get("sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", p["sha256"])):
            bad(w, "sha256 of the zip")
        lic = p.get("license") or {}
        if lic.get("name") not in ("Unsplash License", "Pexels License", "CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0", "own"):
            bad(w, "license.name: a license that allows it (Unsplash, Pexels, CC0, CC BY, CC BY-SA, own)")
        if lic.get("name") in ("CC-BY-4.0", "CC-BY-SA-4.0") and not lic.get("credit"):
            bad(w, "license.credit: CC BY needs the author's credit")


def prayer(d):
    names = {"fajr", "dhuhr", "asr", "maghrib", "isha"}   # the sunrise is never moved by the apps
    for w, a in ids("prayer-adjustments.adjustments", d.get("adjustments", [])):
        where = a.get("where") or {}
        if not (isinstance(where.get("country"), str) and re.fullmatch(r"[A-Z]{2}", where["country"])):
            bad(w, "where.country: an ISO code like SA")
        if "city" in where and not (isinstance(where["city"], str) and where["city"]):
            bad(w, "where.city: the city's name as the app saves it")
        span(w, a)
        mins = a.get("minutes") or {}
        if not mins or not set(mins) <= names or not all(isinstance(v, int) and not isinstance(v, bool) and -20 <= v <= 20 and v for v in mins.values()):
            bad(w, "minutes: {prayer: -20..20, not 0} for fajr, dhuhr, asr, maghrib, isha (what the page and the home accept)")
        if not (isinstance(a.get("source"), str) and a["source"]):
            bad(w, "source: who announced it")


CHECKS = {"announcements.json": notices, "devices.json": devices, "seasonal.json": seasonal, "prayer-adjustments.json": prayer}


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else "content")
    for name, fn in CHECKS.items():
        p = root / name
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except (OSError, ValueError) as e:
            bad(name, f"not read: {e}")
            continue
        if d.get("v") != 1:
            bad(name, '"v": 1')
        fn(d)
    for p in problems:
        print("FAIL " + p)
    print(f"{len(problems)} problems" if problems else "content: all good")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
