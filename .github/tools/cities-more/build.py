#!/usr/bin/env python3
"""Build the offline city search list: android/app/src/main/assets/cities-more.txt.

The places a person may type that the built-in list (Cities.java, untouched) lacks: every GeoNames place of the Saudi and Gulf lists,
the towns of the Arab countries, Turkey, and the cities of the countries with large Arab and Muslim communities, each with an Arabic
name, the GeoNames English name, its coordinates and time zone. The app loads it only when a search needs it (CitiesMore.java).

Sources (both credited in THIRD_PARTY_NOTICES.md):
  GeoNames cities500, CC BY 4.0, through the pinned geonamescache package (requirements.txt of cities-world)
  Wikidata (CC0): the Arabic label and aliases of each GeoNames id, and the seats of the Saudi governorates
    (sources/*.tsv, written by fetch_wikidata.py, so this script runs offline and gives the same file every time)
  overrides.json: reviewed spellings, drops and additions (the only hand-made input)

usage: python3 .github/tools/cities-more/build.py [--check]   (--check: fail when the asset differs from a fresh build)
"""
import json
import math
import re
import sys
import unicodedata
import zoneinfo
from collections import defaultdict
from pathlib import Path

import geonamescache

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
GEO = Path(geonamescache.__file__).parent / "data/cities500.json"
JAVA = ROOT / "android/app/src/main/java/sa/urtv/screensaver/Cities.java"
WORLD = ROOT / ".github/tools/cities-world/cities-merged.json"
OUT = ROOT / "android/app/src/main/assets/cities-more.txt"

# country -> the smallest GeoNames population taken (cities500 holds places of 500 and more, and seats)
TIERS = {}
for _cc in "SA AE QA KW BH OM JO LB SY YE LY IQ".split():
    TIERS[_cc] = 0
for _cc in "EG SD MA DZ TN PS".split():
    TIERS[_cc] = 5000
for _cc in "TR DE GB FR NL BE SE DK NO AT CH IT ES IE FI".split():
    TIERS[_cc] = 15000
for _cc in "US CA AU NZ".split():
    TIERS[_cc] = 30000
for _cc in "MY ID PK IN SG BN".split():
    TIERS[_cc] = 50000
ARAB = set("SA AE QA KW BH OM JO LB SY YE LY IQ EG SD MA DZ TN PS".split())

# the country of a legacy (pre-world) row of Cities.java, by its zone; a row without a zone is Saudi
LEGACY_TZ = {"Asia/Riyadh": "SA", "Asia/Bahrain": "BH", "Asia/Kuwait": "KW", "Asia/Qatar": "QA", "Asia/Dubai": "AE",
             "Asia/Muscat": "OM", "Africa/Cairo": "EG", "Asia/Amman": "JO", "Asia/Damascus": "SY", "Asia/Beirut": "LB",
             "Asia/Hebron": "PS", "Asia/Gaza": "PS", "Africa/Casablanca": "MA", "Africa/Algiers": "DZ", "Africa/Tunis": "TN"}

NON_ARABIC = set("پچژگکیۀٹڈڑںےھ")


def arabic(s):
    """Arabic letters and spaces only (no Persian/Urdu letters, no digits or Latin), at least two letters."""
    if not s:
        return False
    n = 0
    for ch in s:
        if ch == " ":
            continue
        if ch in NON_ARABIC or not ("ء" <= ch <= "ي" or "ً" <= ch <= "ْ" or ch == "ٰ"):
            return False
        n += ch >= "ء" and ch <= "ي"
    return n >= 2


_FOLD = str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ئ": "ي", "ؤ": "و", "ة": "ه"})


def fold(s):
    """The app's search key (TeamSearch.normalize, simplified): no marks, folded letters, lower case, no «ال» or al-/el-/ar-."""
    s = unicodedata.normalize("NFKD", s.replace("ı", "i").replace("ß", "ss"))
    s = "".join(c for c in s if unicodedata.category(c) != "Mn" and c != "ـ").lower().translate(_FOLD)
    words = re.findall(r"[a-z0-9]+|[ء-ي]+", s)
    out = []
    for w in words:
        if len(w) > 3 and w.startswith("ال"):
            w = w[2:]
        out.append(w)
    while len(out) > 1 and out[0] in ("al", "el", "ar", "as", "ad", "an", "az", "at", "ash", "adh", "ath"):
        out = out[1:]
    return " ".join(out)


def latin(s):
    return bool(re.fullmatch(r"[A-Za-z][A-Za-z '\-.]{1,40}", unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode())) and \
        not any("؀" <= c <= "ۿ" for c in s) and all(unicodedata.category(c)[0] in "LZPM" for c in s)


def plain(s):
    """an Arabic name to show: Arabic letters only, no harakat"""
    return arabic(s) and not any("\u064b" <= ch <= "\u0652" or ch == "\u0670" for ch in s)


def km(a, b, c, d):
    p = math.pi / 180
    x = (d - b) * p * math.cos((a + c) / 2 * p)
    y = (c - a) * p
    return 6371 * math.hypot(x, y)


def strip_label(s):
    s = re.sub(r"\s*\([^)]*\)\s*$", "", s or "").strip()   # «الرس (السعودية)»: the bracket is Wikipedia's, not the name
    s = re.sub(r"\s*،.*$", "", s).strip()
    s = re.sub(r"^(?:ولاية|بلدة)\s+", "", s)   # Oman's wilayat: the item is the wilaya, the place is its town («ولاية صحم» -> «صحم»)
    return " ".join(s.replace("ـ", "").split())


def geonames():
    return json.loads(GEO.read_text(encoding="utf-8"))


def candidates(data=None):
    data = data or geonames()
    return sorted((c for c in data.values() if c["countrycode"] in TIERS and c["population"] >= TIERS[c["countrycode"]]),
                  key=lambda c: c["geonameid"])


def tsv(name):
    path = HERE / "sources" / name
    if not path.exists():
        return []
    lines = path.read_text(encoding="utf-8").splitlines()
    head = lines[0].split("\t")
    return [dict(zip(head, l.split("\t"))) for l in lines[1:] if l]


def builtin():
    """Every row of Cities.java, as the app decodes it: (country, ar, lat, lon, tz, en or '', geonameid or None, population or None)."""
    src = JAVA.read_text(encoding="utf-8")
    rows = []
    for m in re.finditer(r'new Region\("([^"]+)", new String\[\]\[\] \{(.*?)\}\)', src, re.S):
        for name, lat, lon, tz in re.findall(r'\{"([^"]+)", "([-\d.]+)", "([-\d.]+)"(?:, "([^"]+)")?\}', m.group(2)):
            tz = tz or "Asia/Riyadh"
            rows.append([LEGACY_TZ[tz], name, lat, lon, tz, "", None, None])
    for r in json.loads(WORLD.read_text(encoding="utf-8")):
        rows.append([r["country"], r["name_ar"], f"{r['lat']:.4f}", f"{r['lon']:.4f}", r["tz"], r["name_en"], r["geonameid"], r["population"]])
    return rows


def zone_countries():
    out = defaultdict(set)
    for f in ("/usr/share/zoneinfo/zone.tab", "/usr/share/zoneinfo/zone1970.tab"):
        try:
            for line in open(f, encoding="utf-8"):
                if line.startswith("#") or not line.strip():
                    continue
                p = line.split("\t")
                for cc in p[0].split(","):
                    out[p[2].strip()].add(cc)
        except OSError:
            pass
    return out


def build():
    data = geonames()
    over = json.loads((HERE / "overrides.json").read_text(encoding="utf-8"))
    wd = {int(r["geonameid"]): r for r in tsv("wikidata-ar.tsv")}
    by_cc = defaultdict(list)
    for c in data.values():
        by_cc[c["countrycode"]].append(c)

    def ar_names(c):
        """The Arabic names of a GeoNames place: the label of the Wikidata item checked to be that place (its GeoNames id or its name,
        within 25 km), then its aliases; in the Arab countries also GeoNames' own Arabic names (the usual spelling, with hamza and ة, first)."""
        names = []
        w = wd.get(c["geonameid"])
        if w:
            names.append(strip_label(w["ar"]))
            names += [strip_label(a) for a in w["ar_aliases"].split("|")]
        if c["countrycode"] in ARAB:
            own = [strip_label(a) for a in c["alternatenames"] if plain(strip_label(a))]
            names += sorted(own, key=lambda a: (not any(ch in a for ch in "أإآة"), a))
        seen, out = set(), []
        for n in names:
            if plain(n) and n not in seen:
                seen.add(n)
                out.append(n)
        return out

    # 1. the built-in rows: the GeoNames place each one is (for its English name, aliases and population; and so it is not added twice)
    rows = builtin()
    used = set()
    info = []   # (lat, lon, population, en, aliases) for each built-in row
    for country, ar, lat, lon, tz, en, gid, pop in rows:
        la, lo = float(lat), float(lon)
        c = None
        if gid:
            c = next((x for x in by_cc[country] if x["geonameid"] == gid), None)
        else:
            near = sorted((km(la, lo, x["latitude"], x["longitude"]), x) for x in by_cc[country]
                          if abs(x["latitude"] - la) < 0.3 and abs(x["longitude"] - lo) < 0.3)
            key = fold(ar)
            same = [(d, x) for d, x in near if d < 15 and key in {fold(n) for n in ar_names(x)}]
            pick = same or [(d, x) for d, x in near if d < 3]
            if pick:
                c = max(pick, key=lambda dx: (dx[1]["population"] if dx[0] < 15 else 0, -dx[0]))[1] if same else pick[0][1]
        fix = over["builtin"].get(f"{ar}|{lat}|{lon}", {})
        if fix.get("nomatch"):   # the nearest GeoNames place is another one: no English, aliases or population from it
            c = None
        if fix.get("geonameid"):
            c = next(x for x in by_cc[country] if x["geonameid"] == fix["geonameid"])
        if c:
            used.add(c["geonameid"])
        en = en or fix.get("en") or (c["name"] if c else "")
        pop = pop if pop is not None else (c["population"] if c else 0)
        aliases = aliases_of(ar, en, c, ar_names(c) if c else [], fix.get("aliases", []))
        info.append((lat, lon, pop, en if not rows_have_en(gid) else "", aliases))

    # 2. the new places
    drop = {str(x) for x in over["drop"]}
    names_fix = {int(k): v for k, v in over["names"].items()}
    out = []
    for c in candidates(data):
        gid = c["geonameid"]
        if gid in used or str(gid) in drop:
            continue
        if c["countrycode"] == "PS" and c["timezone"] not in ("Asia/Hebron", "Asia/Gaza"):
            continue   # GeoNames puts the settlements on Jerusalem's zone: never listed as Palestine's towns
        if c["timezone"] == "Asia/Jerusalem":
            continue
        names = ar_names(c)
        ar = names_fix.get(gid) or (names[0] if names else "")
        if not ar:
            continue
        la, lo = c["latitude"], c["longitude"]
        # the same place under another GeoNames id as a built-in city: same folded name, a few km apart
        if any(r[0] == c["countrycode"] and km(la, lo, float(r[2]), float(r[3])) < 8 and fold(r[1]) == fold(ar) for r in rows):
            continue
        out.append({"id": str(gid), "cc": c["countrycode"], "tz": c["timezone"], "lat": la, "lon": lo, "pop": c["population"],
                    "ar": ar, "en": c["name"], "aliases": aliases_of(ar, c["name"], c, names, over["aliases"].get(str(gid), []))})
    for g in tsv("sa-governorates.tsv"):   # the seat of every Saudi governorate (Wikidata), unless GeoNames has it
        m = re.match(r"Point\(([-\d.]+) ([-\d.]+)\)", g["coord"])
        if not m or not arabic(strip_label(g["seat_ar"])):
            continue
        ar, lo, la = strip_label(g["seat_ar"]), float(m.group(1)), float(m.group(2))
        if g["geonameid"] and int(g["geonameid"]) in {int(x["id"]) for x in out if x["id"].isdigit()} | used:
            continue
        # a seat the built-in list has under its name (its coordinates there may be the town's, Wikidata's the governorate's: up to 60 km)
        if g["seat"] in drop or any(r[0] == "SA" and km(la, lo, float(r[2]), float(r[3])) < 60 and fold(r[1]) == fold(ar) for r in rows) or \
                any(km(la, lo, x["lat"], x["lon"]) < 8 and fold(x["ar"]) == fold(ar) for x in out):
            continue
        out.append({"id": g["seat"], "cc": "SA", "tz": "Asia/Riyadh", "lat": la, "lon": lo, "pop": 0, "ar": ar, "en": "",
                    "aliases": []})
    for a in over["add"]:
        out.append({"id": a["id"], "cc": a["cc"], "tz": a["tz"], "lat": a["lat"], "lon": a["lon"], "pop": a.get("pop", 0),
                    "ar": a["ar"], "en": a["en"], "aliases": a.get("aliases", [])})
    # one row for one place: GeoNames sometimes lists a town and its quarter under one name a few km apart
    out.sort(key=lambda x: (x["cc"], -x["pop"], x["id"]))
    kept = []
    for x in out:
        if any(y["cc"] == x["cc"] and fold(y["ar"]) == fold(x["ar"]) and km(x["lat"], x["lon"], y["lat"], y["lon"]) < 8 for y in kept):
            continue
        kept.append(x)
    # an other spelling that is another place's name (GeoNames gives Dammam «الاحساء») would find the wrong place: only the reviewed ones stay
    reviewed = {a for f in over["builtin"].values() for a in f.get("aliases", [])} | {a for v in over["aliases"].values() for a in v}
    taken = {fold(r[1]) for r in rows} | {fold(x["ar"]) for x in kept} | {fold(a) for a in reviewed}

    def own(aliases, *names):
        mine = {fold(n) for n in names}
        return [a for a in aliases if a in reviewed or fold(a) in mine or fold(a) not in taken]
    info = [(lat, lon, pop, en, own(al, r[1])) for (lat, lon, pop, en, al), r in zip(info, rows)]
    for x in kept:
        x["aliases"] = own(x["aliases"], x["ar"])
    return info, kept


def rows_have_en(gid):
    return gid is not None   # a world row has its English name in Cities.java already


def aliases_of(ar, en, c, ar_names, extra):
    """Other spellings a person may type: other Arabic names, and Latin spellings (Arab countries and Turkey: GeoNames' Latin forms)."""
    keys = {fold(ar), fold(en)}
    out = []
    names = [k for k in keys if k]
    for n in list(extra) + ar_names[1:3]:
        if n not in extra and any(k.startswith(fold(n)) for k in names):
            continue   # a cut name («خميس» for «خميس الخشنة»): the search finds the start of a name already, and it would rank as the whole name
        if fold(n) and fold(n) not in keys:
            keys.add(fold(n))
            out.append(n)
    if c is not None:
        cap = 3 if c["countrycode"] in ARAB or c["countrycode"] == "TR" else 0   # elsewhere the English name is GeoNames' own
        n = 0
        for a in c["alternatenames"]:
            if n >= cap:
                break
            if latin(a) and 3 <= len(a) <= 30:
                k = fold(a)
                if k and k not in keys and not any(k.startswith(x) or x.startswith(k) for x in keys if len(x) > 3 and len(k) > 3):
                    keys.add(k)
                    out.append(a)
                    n += 1
    return [a.replace("\t", " ").replace("|", " ") for a in out]


def render(info, cities):
    tzs = sorted({c["tz"] for c in cities})
    lines = ["# UrTV offline city search. GeoNames (geonames.org, CC BY 4.0) and Wikidata (CC0). Generated by .github/tools/cities-more/build.py: do not edit.",
             "@" + "\t".join(tzs)]
    for lat, lon, pop, en, aliases in info:
        lines.append("=" + "\t".join([lat, lon, str(pop), en, "|".join(aliases)]))
    for c in cities:
        lines.append("\t".join([c["id"], c["cc"], str(tzs.index(c["tz"])), f"{c['lat']:.4f}", f"{c['lon']:.4f}", str(c["pop"]), c["ar"], c["en"], "|".join(c["aliases"])]))
    return "\n".join(lines) + "\n"


def validate(info, cities):
    zc = zone_countries()
    bad = []
    ids = set()
    for c in cities:
        if c["id"] in ids:
            bad.append(f"duplicate id {c['id']}")
        ids.add(c["id"])
        try:
            zoneinfo.ZoneInfo(c["tz"])
        except Exception:
            bad.append(f"bad zone {c['tz']} for {c['id']}")
        if zc and c["cc"] not in zc.get(c["tz"], {c["cc"]}):
            bad.append(f"zone {c['tz']} is not {c['cc']}'s ({c['id']} {c['en']})")
        if not (-90 <= c["lat"] <= 90 and -180 <= c["lon"] <= 180):
            bad.append(f"coordinates of {c['id']}")
        if not arabic(c["ar"]):
            bad.append(f"Arabic name of {c['id']}: {c['ar']!r}")
        if c["cc"] == "PS" and c["tz"] not in ("Asia/Hebron", "Asia/Gaza"):
            bad.append(f"Palestine's zone for {c['id']}")
        if c["cc"] in ("IL", "EH"):
            bad.append(f"never listed: {c['cc']}")
    return bad


def main():
    info, cities = build()
    bad = validate(info, cities)
    if bad:
        print("\n".join(bad[:40]), file=sys.stderr)
        sys.exit(1)
    text = render(info, cities)
    if "--check" in sys.argv:
        same = OUT.exists() and OUT.read_text(encoding="utf-8") == text
        print("cities-more.txt is " + ("up to date" if same else "STALE: run .github/tools/cities-more/build.py"))
        sys.exit(0 if same else 1)
    lock_file = HERE / "ids.lock"
    lock = set(lock_file.read_text(encoding="utf-8").split()) if lock_file.exists() else set()
    gone = lock - {c["id"] for c in cities}
    if gone:   # the data only grows: a place once shipped stays (a drop or a fix keeps its id)
        print("ids of ids.lock that would go away: " + " ".join(sorted(gone)[:20]), file=sys.stderr)
        sys.exit(1)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8")
    lock_file.write_text("\n".join(sorted(lock | {c["id"] for c in cities}, key=lambda i: (i[0] == "Q", int(i.lstrip("Q"))))) + "\n", encoding="utf-8")
    counts = defaultdict(int)
    for c in cities:
        counts[c["cc"]] += 1
    print(f"{OUT.relative_to(ROOT)}: {len(cities)} places, {len(text.encode())} bytes; " + " ".join(f"{k}:{v}" for k, v in sorted(counts.items())))


if __name__ == "__main__":
    main()
