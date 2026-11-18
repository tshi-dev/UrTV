#!/usr/bin/env python3
"""Snapshot the Wikidata facts the offline city list uses (Wikidata is CC0), so build.py runs offline and reproducibly.

  sources/wikidata-ar.tsv    GeoNames id -> Wikidata item, its Arabic label and Arabic aliases (for the GeoNames candidates of build.py)
  sources/sa-governorates.tsv  every governorate of Saudi Arabia (Q66661665) with its seat, the seat's Arabic name and coordinates

usage: python3 .github/tools/cities-more/fetch_wikidata.py   (the query service allows about one query a minute: it waits)
"""
import json
import math
import re
import os
import ssl
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build  # noqa: E402  (the candidate list)

URL = "https://query.wikidata.org/sparql"
CA = os.environ.get("SSL_CERT_FILE")   # a proxy's own CA bundle, when there is one
CTX = ssl.create_default_context(cafile=CA) if CA else ssl.create_default_context()


def sparql(query):
    data = urllib.parse.urlencode({"query": query}).encode()
    for attempt in range(8):
        req = urllib.request.Request(URL, data=data, headers={
            "User-Agent": "urtv-cities-build/1.0", "Accept": "application/sparql-results+json"})
        try:
            with urllib.request.urlopen(req, timeout=180, context=CTX) as r:
                return json.load(r)["results"]["bindings"]
        except Exception as e:  # 429 / timeouts: wait and ask again
            print(f"  retry {attempt + 1}: {e}", file=sys.stderr)
            time.sleep(65)
    raise SystemExit("Wikidata did not answer")


def v(row, k):
    return row[k]["value"] if k in row else ""


LANGS = {"DE": "de", "AT": "de", "CH": "de fr it", "FR": "fr", "BE": "fr nl", "NL": "nl", "SE": "sv", "DK": "da", "NO": "nb", "FI": "fi",
         "IT": "it", "ES": "es", "CA": "fr", "TR": "tr", "MY": "ms", "ID": "id", "BN": "ms"}


def point(s):
    m = re.match(r"Point\(([-\d.eE]+) ([-\d.eE]+)\)", s or "")
    return (float(m.group(2)), float(m.group(1))) if m else None


def km(a, b):
    p = math.pi / 180
    return 6371 * math.hypot((b[1] - a[1]) * p * math.cos((a[0] + b[0]) / 2 * p), (b[0] - a[0]) * p)


def lit(s, lang):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"@' + lang


def labels(cands):
    """For each GeoNames place: Wikidata items that carry its GeoNames id, or whose label in English, the country's language or (Arab
    countries) one of GeoNames' Arabic names is its name; each with its Arabic label, Arabic aliases and coordinates."""
    found = []   # (geonameid, item, ar, aliases, lat, lon, via)
    ids = [c["geonameid"] for c in cands]
    for i in range(0, len(ids), 2500):
        part = ids[i:i + 2500]
        q = f"""SELECT ?gn ?item ?ar (GROUP_CONCAT(DISTINCT ?alt; separator="|") AS ?alts) (SAMPLE(?c) AS ?coord) WHERE {{
  VALUES ?gn {{ {" ".join(f'"{g}"' for g in part)} }} ?item wdt:P1566 ?gn.
  OPTIONAL {{ ?item rdfs:label ?ar FILTER(lang(?ar) = "ar") }} OPTIONAL {{ ?item skos:altLabel ?alt FILTER(lang(?alt) = "ar") }}
  OPTIONAL {{ ?item wdt:P625 ?c }} }} GROUP BY ?gn ?item ?ar"""
        for r in sparql(q):
            found.append((int(v(r, "gn")), v(r, "item"), v(r, "ar"), v(r, "alts"), v(r, "coord"), "id"))
        print(f"ids {i + len(part)}/{len(ids)}: {len(found)}", file=sys.stderr)
        time.sleep(62)
    names = {}
    for c in cands:
        langs = ["en"] + LANGS.get(c["countrycode"], "").split()
        for lang in langs:
            names.setdefault(lit(c["name"], lang), set()).add(c["geonameid"])
        if c["countrycode"] in build.ARAB:
            for a in c["alternatenames"]:
                if build.arabic(a):
                    names.setdefault(lit(a, "ar"), set()).add(c["geonameid"])
    keys = sorted(names)
    for i in range(0, len(keys), 3000):
        part = keys[i:i + 3000]
        q = f"""SELECT ?lab ?item ?ar (GROUP_CONCAT(DISTINCT ?alt; separator="|") AS ?alts) (SAMPLE(?c) AS ?coord) WHERE {{
  VALUES ?lab {{ {" ".join(part)} }} ?item rdfs:label ?lab; wdt:P625 ?c.
  ?item rdfs:label ?ar FILTER(lang(?ar) = "ar") OPTIONAL {{ ?item skos:altLabel ?alt FILTER(lang(?alt) = "ar") }} }} GROUP BY ?lab ?item ?ar"""
        for r in sparql(q):
            key = lit(r["lab"]["value"], r["lab"].get("xml:lang", ""))
            for g in names.get(key, ()):
                found.append((g, v(r, "item"), v(r, "ar"), v(r, "alts"), v(r, "coord"), "name"))
        print(f"names {i + len(part)}/{len(keys)}: {len(found)}", file=sys.stderr)
        time.sleep(62)
    return found


def governorates():
    q = """SELECT ?g ?gl ?seat ?sl ?coord ?gn WHERE { ?g wdt:P31 wd:Q66661665.
  OPTIONAL { ?g rdfs:label ?gl FILTER(lang(?gl) = "ar") }
  OPTIONAL { ?g wdt:P36 ?seat. OPTIONAL { ?seat rdfs:label ?sl FILTER(lang(?sl) = "ar") } OPTIONAL { ?seat wdt:P625 ?coord } OPTIONAL { ?seat wdt:P1566 ?gn } } }"""
    return sparql(q)


def clean(s):
    return " ".join(s.replace("\t", " ").replace("\n", " ").split())


def main():
    src = HERE / "sources"
    src.mkdir(exist_ok=True)
    gov = governorates()
    lines = ["governorate\tgovernorate_ar\tseat\tseat_ar\tcoord\tgeonameid"]
    for r in sorted(gov, key=lambda r: (int(v(r, "g").rsplit("/Q", 1)[-1]), v(r, "seat"), v(r, "coord"), v(r, "gn"))):
        lines.append("\t".join([v(r, "g").rsplit("/", 1)[-1], clean(v(r, "gl")), v(r, "seat").rsplit("/", 1)[-1], clean(v(r, "sl")), v(r, "coord"), v(r, "gn")]))
    (src / "sa-governorates.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8")
    time.sleep(62)
    seats = {int(l.split("\t")[5]) for l in lines[1:] if l.split("\t")[5]}
    data = build.geonames()
    cands = {c["geonameid"]: c for c in build.candidates(data)}
    for g in seats:
        if str(g) in data:
            cands.setdefault(g, data[str(g)])
    best = {}
    for g, item, ar, alts, coord, via in labels(sorted(cands.values(), key=lambda c: c["geonameid"])):
        c, pt = cands[g], point(coord)
        if not ar or pt is None:
            continue
        d = km((c["latitude"], c["longitude"]), pt)
        if d > 25:
            continue   # another place of that name, or a wrong GeoNames id on Wikidata
        rank = (0 if via == "id" else 1, round(d, 1), int(item.rsplit("/Q", 1)[-1]))
        if g not in best or rank < best[g][0]:
            best[g] = (rank, item.rsplit("/", 1)[-1], clean(ar), clean(alts), via, d)
    rows = ["geonameid\titem\tar\tar_aliases\tvia\tkm"] + [f"{g}\t{it}\t{ar}\t{alts}\t{via}\t{d:.1f}" for g, (_, it, ar, alts, via, d) in sorted(best.items())]
    (src / "wikidata-ar.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8")
    print(f"{len(lines) - 1} governorates, {len(best)} labelled cities", file=sys.stderr)


if __name__ == "__main__":
    main()
