#!/usr/bin/env python3
"""Build the worldwide city dataset for the prayer-times app.

Outputs (next to this script):
  cities-world.json   list of city rows
  missing-arabic.csv  included cities that have no usable Arabic name
  STATS.md            counts, drops, spot-check sample, suspicious names

Requirements: pip install geonamescache babel
Nothing is typed from memory except the ARABIC_OVERRIDES dict, whose spellings
were supplied by the requester.  Country names in Arabic come from Babel's CLDR
'ar' locale because geonamescache has no Arabic country names.
"""
import csv
import json
import os
import random
import re
import sys
import unicodedata
import zoneinfo
from collections import Counter, defaultdict

from babel import Locale
from geonamescache import GeonamesCache

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- country sets (ISO 3166-1 alpha-2) -------------------------------------
MUSLIM_MAJORITY = [
    "TR", "IR", "IQ", "PK", "AF", "BD", "ID", "MY", "BN", "MV", "AZ", "KZ",
    "UZ", "KG", "TJ", "TM", "LY", "SD", "MR", "SO", "DJ", "KM", "YE", "SN",
    "NE", "ML", "GN", "GM", "TD", "BF", "SL", "AL", "XK", "BA",
]
LARGE_MUSLIM_SHARE = ["IN", "NG", "ET", "TZ", "RU", "CN", "PH", "TH"]
SKIP = [  # hand-checked lists already exist in the app
    "SA", "BH", "KW", "QA", "AE", "OM", "EG", "JO", "SY", "LB", "PS",
    "MA", "DZ", "TN",
]
PAD_TO_100K = set(MUSLIM_MAJORITY) | set(LARGE_MUSLIM_SHARE)
# Arab League members not already in the app: every city >= 50,000 (owner's change).
ARAB_NEW = ["IQ", "YE", "LY", "SD", "MR", "SO", "DJ", "KM"]

# Approximate Muslim population per country (Pew-style estimates, rounded, from
# memory). It ONLY sets the country order in the output; it is not used for
# selecting cities. Countries missing from this table go last, by total population.
MUSLIMS = {
    "ID": 210e6, "PK": 175e6, "IN": 177e6, "BD": 145e6, "NG": 78e6,
    "IR": 74e6, "TR": 74e6, "CN": 21e6, "ET": 31e6, "AF": 31e6,
    "RU": 16e6, "UZ": 27e6, "NE": 19e6, "ML": 14e6, "TZ": 13e6,
    "SN": 13e6, "MY": 14e6, "SD": 33e6, "IQ": 32e6, "YE": 24e6,
    "SO": 14e6, "LY": 6e6, "MR": 3.6e6, "DJ": 0.8e6, "KM": 0.8e6,
    "KZ": 10e6, "AZ": 8e6, "TJ": 7e6, "TM": 5e6, "KG": 5e6,
    "GN": 9e6, "BF": 8.5e6, "TD": 9e6, "SL": 2.5e6, "GM": 2.5e6,
    "CI": 9e6, "GH": 4.5e6, "CM": 4e6, "UG": 4e6, "KE": 4.5e6,
    "MZ": 5e6, "MM": 4.5e6, "PH": 5e6, "TH": 4.5e6, "AL": 2e6,
    "XK": 1.8e6, "BA": 1.6e6, "BN": 0.3e6, "MV": 0.3e6,
    # Muslim-minority countries with large populations (for ordering only)
    "FR": 4.7e6, "DE": 3.8e6, "GB": 2.7e6, "US": 2.6e6, "IT": 1.5e6,
    "CA": 1.0e6, "ES": 1.0e6, "NL": 0.9e6,
}

# ---- Arabic spellings supplied by the requester (keyed by country, GeoNames name)
ARABIC_OVERRIDES = {
    ("TR", "Istanbul"): "إسطنبول",
    ("IR", "Tehran"): "طهران",
    ("PK", "Karachi"): "كراتشي",
    ("ID", "Jakarta"): "جاكرتا",
    ("MY", "Kuala Lumpur"): "كوالالمبور",
    ("GB", "London"): "لندن",
    ("FR", "Paris"): "باريس",
    ("US", "New York City"): "نيويورك",
    ("RU", "Moscow"): "موسكو",
    ("CN", "Beijing"): "بكين",
    ("IN", "Delhi"): "دلهي",
    ("PK", "Lahore"): "لاهور",
    ("BD", "Dhaka"): "داكا",
    ("IQ", "Baghdad"): "بغداد",
    ("SD", "Khartoum"): "الخرطوم",
    ("LY", "Tripoli"): "طرابلس",
    ("YE", "Sanaa"): "صنعاء",
    ("MR", "Nouakchott"): "نواكشوط",
    ("SO", "Mogadishu"): "مقديشو",
}

# ---- Arabic-script test -----------------------------------------------------
# Letters that are Persian / Urdu / other and must not be used (Arabic uses ي and ك).
NON_ARABIC_LETTERS = set("پچژگکیۀٹڈڑںےھ")
# Unicode-name fragments of Persian / Urdu letters (matched on whole words).
NON_ARABIC_NAME_WORDS = [
    "FARSI", "PERSIAN", "URDU", "KEHEH", "GAF", "PEH", "TCHEH", "JEH",
    "TTEH", "DDAL", "RREH", "NOON GHUNNA", "YEH BARREE",
    "HEH DOACHASHMEE", "HEH WITH YEH ABOVE", "HEH GOAL",
]
# Punctuation that may appear inside a name without making it non-Arabic.
ALLOWED_PUNCT = set("-'’،؛؟٪٫٬٭۔")
SPACE_LIKE = set("  ")


def _is_presentation_form(cp):
    return 0xFB50 <= cp <= 0xFDFF or 0xFE70 <= cp <= 0xFEFF


def is_arabic_name(s):
    """True if s is written only in the Arabic script (no Latin, no digits,
    no Persian/Urdu letters)."""
    if not s or not s.strip():
        return False
    letters = 0
    for ch in s:
        if ch in SPACE_LIKE or ch.isspace():
            continue
        if ch in ALLOWED_PUNCT:
            continue
        if ch in NON_ARABIC_LETTERS:
            return False
        cp = ord(ch)
        cat = unicodedata.category(ch)
        name = unicodedata.name(ch, "")
        if "ARABIC" not in name or _is_presentation_form(cp):
            return False
        if any(f" {w} " in f" {name} " for w in NON_ARABIC_NAME_WORDS):
            return False
        if cat in ("Lo", "Lm"):
            letters += 1
        elif cat == "Mn":          # harakat / diacritics
            pass
        else:                      # digits, Po/Pd other than allowed, etc.
            return False
    return letters >= 2


def core(s):
    return "".join(ch for ch in s if not (ch.isspace() or ch == "-"))


def has_harakat(s):
    return any(0x064B <= ord(c) <= 0x065F or ord(c) == 0x0670 for c in s)


_SKEL = str.maketrans({
    "أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ئ": "ي",
    "ؤ": "و", "ة": "ه",
})


def skeleton(s):
    """Spelling key that ignores harakat, tatweel, hamza/alef/ya/ta-marbuta variants."""
    out = []
    for ch in core(s):
        if 0x064B <= ord(ch) <= 0x065F or ord(ch) == 0x0670 or ch == "ـ":
            continue
        out.append(ch)
    return "".join(out).translate(_SKEL)


def pick_arabic(alternatenames):
    """Most common spelling (cluster of agreeing variants), then the shortest."""
    cands = [s for s in alternatenames if is_arabic_name(s) and len(core(s)) >= 2]
    if not cands:
        return None
    cluster = Counter(skeleton(s) for s in cands)
    return sorted(cands, key=lambda s: (-cluster[skeleton(s)], has_harakat(s),
                                        len(core(s)), s))[0]


# ---- load data --------------------------------------------------------------
gc15 = GeonamesCache(min_city_population=15000)
cities15 = gc15.get_cities()
countries = gc15.get_countries()
cities500 = GeonamesCache(min_city_population=500).get_cities()   # for capitals under 15k

by_cc_500 = defaultdict(list)
for rec in cities500.values():
    by_cc_500[rec["countrycode"]].append(rec)


def find_capital(cc, capname):
    if not capname:
        return None
    key = capname.casefold().strip()
    pool = by_cc_500.get(cc, [])
    hits = [r for r in pool if r["name"].casefold().strip() == key]
    if not hits:
        hits = [r for r in pool
                if any(a.casefold().strip() == key for a in r["alternatenames"])]
    return max(hits, key=lambda r: r["population"]) if hits else None


# ---- choose cities ----------------------------------------------------------
selected = {}          # geonameid -> record
capital_ids = set()
capital_unmatched = []

for gid, rec in cities15.items():
    cc = rec["countrycode"]
    if cc in SKIP:
        continue
    pop = rec["population"]
    if (pop >= 500_000
            or (cc in PAD_TO_100K and pop >= 100_000)
            or (cc in ARAB_NEW and pop >= 50_000)):
        selected[int(rec["geonameid"])] = rec

for cc, country in sorted(countries.items()):
    if cc in SKIP:
        continue
    rec = find_capital(cc, country.get("capital"))
    if rec is None:
        capital_unmatched.append((cc, country.get("capital")))
        continue
    gid = int(rec["geonameid"])
    selected[gid] = rec
    capital_ids.add(gid)

# ---- Arabic country names (CLDR via Babel) -----------------------------------
ar_territories = Locale.parse("ar").territories
country_ar = {}
for cc in {r["countrycode"] for r in selected.values()}:
    name = ar_territories.get(cc)
    if name:
        name = re.sub(r"\s*\(.*?\)", "", name).strip()   # "ميانمار (بورما)" -> "ميانمار"
    if not name or not is_arabic_name(name):
        raise SystemExit(f"no usable Arabic country name for {cc}: {name!r}")
    country_ar[cc] = name

# ---- build rows -------------------------------------------------------------
rows, missing = [], []
capitals_no_arabic = []
country_name_en = {k: v["name"] for k, v in countries.items()}
used_overrides = set()
for gid, rec in selected.items():
    cc = rec["countrycode"]
    en = rec["name"]
    key = (cc, en)
    if key in ARABIC_OVERRIDES:
        name_ar = ARABIC_OVERRIDES[key]
        used_overrides.add(key)
        if not is_arabic_name(name_ar):
            raise SystemExit(f"override for {key} is not pure Arabic")
    else:
        name_ar = pick_arabic(rec["alternatenames"])
    if name_ar is None:
        missing.append({"name": en, "country": cc, "population": rec["population"]})
        if gid in capital_ids:
            capitals_no_arabic.append(f"{cc} {country_name_en.get(cc, cc)}: {en}")
        continue
    rows.append({
        "country": cc,
        "country_ar": country_ar[cc],
        "name_ar": name_ar,
        "name_en": en,
        "lat": round(rec["latitude"], 4),
        "lon": round(rec["longitude"], 4),
        "tz": rec["timezone"],
        "population": rec["population"],
        "capital": gid in capital_ids,
        "geonameid": gid,
    })

unused_overrides = set(ARABIC_OVERRIDES) - used_overrides

# one row per (country, name_ar): keep the bigger city (defined below)
rows.sort(key=lambda r: (-r["population"], r["geonameid"]))
seen, deduped, dup_dropped = set(), [], []
for r in rows:
    k = (r["country"], r["name_ar"])
    if k in seen:
        dup_dropped.append(r)
        continue
    seen.add(k)
    deduped.append(r)
rows = deduped
# ---- country order -----------------------------------------------------------
# 1) Arab League members not in the app, by Muslim population
# 2) other countries in MUSLIMS, by Muslim population
# 3) everything else, by total population
def order_key(cc):
    if cc in ARAB_NEW:
        return (0, -MUSLIMS.get(cc, 0), cc)
    if cc in MUSLIMS:
        return (1, -MUSLIMS[cc], cc)
    return (2, -countries[cc]["population"], cc)


present = {r["country"] for r in rows}
ordered_ccs = sorted(present, key=order_key)
order_of = {cc: i + 1 for i, cc in enumerate(ordered_ccs)}
for r in rows:
    r["order"] = order_of[r["country"]]
rows.sort(key=lambda r: (r["order"], -r["population"], r["geonameid"]))

# ---- write outputs ----------------------------------------------------------
with open(os.path.join(HERE, "cities-world.json"), "w", encoding="utf-8") as f:
    json.dump(rows, f, ensure_ascii=False, indent=1)
    f.write("\n")

missing.sort(key=lambda r: (r["country"], -r["population"]))
with open(os.path.join(HERE, "missing-arabic.csv"), "w", encoding="utf-8", newline="") as f:
    w = csv.DictWriter(f, fieldnames=["name", "country", "population"])
    w.writeheader()
    w.writerows(missing)

# ---- self-checks ------------------------------------------------------------
problems = []
for r in rows:
    try:
        zoneinfo.ZoneInfo(r["tz"])
    except Exception:
        problems.append(f"bad tz {r['tz']} for {r['name_en']}")
    if not -90 <= r["lat"] <= 90 or not -180 <= r["lon"] <= 180:
        problems.append(f"bad coords {r['name_en']}")
cap_countries = {r["country"] for r in rows if r["capital"]}
countries_in = {r["country"] for r in rows}
no_capital = sorted(c for c in countries if c not in SKIP and c not in cap_countries)
print("countries without a capital row:", no_capital, file=sys.stderr)
if problems:
    print("CHECK PROBLEMS:", problems[:10], file=sys.stderr)
if unused_overrides:
    print("UNUSED OVERRIDES (no matching city):", sorted(unused_overrides), file=sys.stderr)

# ---- STATS.md ---------------------------------------------------------------
per_country = Counter((order_of[cc], country_ar[cc], cc) for cc in present)
ar_to_countries = defaultdict(set)
for r in rows:
    ar_to_countries[r["name_ar"]].add(r["country"])
suspicious = [r for r in rows if len(r["name_ar"]) > 25 or len(ar_to_countries[r["name_ar"]]) > 1]

rng = random.Random(20261029)
sample = rng.sample(rows, 30)

lines = []
lines.append("# Worldwide cities for the prayer-times app: stats\n")
lines.append(f"- Total cities: **{len(rows)}**")
lines.append(f"- Countries: **{len(countries_in)}**")
lines.append(f"- Capitals in output: **{len(cap_countries)}**")
lines.append(f"- Countries skipped (hand-checked in the app): {', '.join(SKIP)}")
lines.append(f"- Cities selected but dropped for lacking an Arabic name: **{len(missing)}** (see `missing-arabic.csv`)")
lines.append(f"- Cities dropped as duplicate (country, name_ar), bigger city kept: **{len(dup_dropped)}**")
for r in dup_dropped:
    lines.append(f"  - dropped {r['name_en']} ({r['geonameid']}, {r['country']}, {r['name_ar']}, pop {r['population']})")
lines.append(f"- Capitals with no matching GeoNames city: {capital_unmatched or 'none'}")
lines.append(f"- Capitals dropped for lacking an Arabic name (not transliterated): {len(capitals_no_arabic)}")
for x in capitals_no_arabic:
    lines.append(f"  - {x}")
lines.append(f"- Countries without any capital row: {no_capital or 'none'}")
lines.append(f"- Overrides used: {len(used_overrides)} of {len(ARABIC_OVERRIDES)}")
lines.append("")
lines.append("## Country order\n")
lines.append("Order is set by the `MUSLIMS` table in `build.py`: Arab League members first, then the other countries in the table by Muslim population, then the rest by total population. The Muslim-population figures are approximate (Pew-style, rounded, from memory) and only set the order; they do not select cities.\n")
lines.append("| order | country_ar | ISO | count |")
lines.append("|---|---|---|---|")
for (o, ar, cc), n in sorted(per_country.items()):
    lines.append(f"| {o} | {ar} | {cc} | {n} |")
lines.append("")
lines.append("## 30 random rows (seed 20261029)\n")
lines.append("| country | name_ar | name_en | population | capital | tz | geonameid |")
lines.append("|---|---|---|---|---|---|---|")
for r in sample:
    lines.append(f"| {r['country_ar']} | {r['name_ar']} | {r['name_en']} | {r['population']} | {'yes' if r['capital'] else ''} | {r['tz']} | {r['geonameid']} |")
lines.append("")
lines.append("## Suspicious name_ar (longer than 25 characters, or the same as another country's city)\n")
if suspicious:
    lines.append("| country | name_ar | name_en | length | reason |")
    lines.append("|---|---|---|---|---|")
    for r in suspicious:
        reason = []
        if len(r["name_ar"]) > 25:
            reason.append("long")
        if len(ar_to_countries[r["name_ar"]]) > 1:
            reason.append("same as other country: " + ", ".join(sorted(ar_to_countries[r["name_ar"]] - {r["country"]})))
        lines.append(f"| {r['country']} | {r['name_ar']} | {r['name_en']} | {len(r['name_ar'])} | {'; '.join(reason)} |")
else:
    lines.append("None.")
lines.append("")

with open(os.path.join(HERE, "STATS.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print(f"cities={len(rows)} countries={len(countries_in)} capitals={len(cap_countries)} "
      f"missing_arabic={len(missing)} dup_dropped={len(dup_dropped)} suspicious={len(suspicious)}")
