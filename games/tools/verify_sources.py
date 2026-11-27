#!/usr/bin/env python3
"""Checks the quiz bank against open datasets that are NOT the ones it was built from (games/REVIEW-SOURCES.md lists each source and its licence).
   python3 games/tools/verify_sources.py geo|quran games/data/quiz.json CACHE_DIR      (the sources are downloaded once into CACHE_DIR; nothing downloaded is kept in the repo)
   geo: mledoze/countries (ODbL): capital, continent, official language and currency of every country item.
   quran: Tanzil quran-data.xml (CC BY 3.0 metadata, from a mirror on GitHub): the surah order, names and verse counts."""
import json, re, sys, os, unicodedata, urllib.request, xml.etree.ElementTree as ET

SRC = {
    "countries.json": "https://raw.githubusercontent.com/mledoze/countries/master/countries.json",
    "quran-data.xml": "https://raw.githubusercontent.com/theilgaz/mahfuz/HEAD/scripts/tanzil-source/quran-data.xml",
}
def get(cache, name):
    os.makedirs(cache, exist_ok=True); p = os.path.join(cache, name)
    if not os.path.exists(p): urllib.request.urlretrieve(SRC[name], p)
    return p
def norm(s):
    s = "".join(ch for ch in unicodedata.normalize("NFKD", s) if not unicodedata.combining(ch))   # Reykjavík = Reykjavik
    return re.sub(r"[^a-z0-9]", "", s.lower())
# the same thing under two English names (the dataset's and the bank's), checked by hand once: language and currency
SAME = {("persian", "persianfarsi"), ("bangla", "bengali"), ("poundsterling", "britishpound"), ("renminbi", "chineseyuan"), ("sovereignbolivar", "venezuelanbolivarsoberano")}
def arnorm(x): return re.sub(r"\s+", " ", re.sub(r"[ً-ٰٟـ]", "", x).replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ى", "ي").replace("ة", "ه")).strip()

def quran(items, cache):
    t = ET.parse(get(cache, "quran-data.xml")).getroot(); S = {int(s.get("index")): s for s in t.find("suras")}
    bad = []; n = 0
    for x in items:
        p = x["id"].split("-")
        if p[0] != "qu" or len(p) < 3: continue
        n += 1; k = int(p[2])
        if p[1] == "next": exp = S[k + 1].get("name") if k < 114 else ""; got = x["answer"]["ar"]
        elif p[1] == "num": exp = S[k].get("name"); got = x["answer"]["ar"]
        elif p[1] == "cnt": exp = S[k].get("ayas"); got = re.sub(r"[٠-٩]", lambda m: str(ord(m.group()) - 1632), x["answer"]["ar"])
        else: continue
        if arnorm(str(got)) != arnorm(str(exp)): bad.append((x["id"], got, exp))
    return n, bad

CONT = {"Africa": "Africa", "Europe": "Europe", "Asia": "Asia", "Oceania": "Oceania", "Antarctic": "Antarctica"}
def continent(c):
    if c["region"] == "Americas": return "South America" if c.get("subregion") == "South America" else "North America"
    return CONT.get(c["region"], c["region"])
def geo(items, cache):
    cs = json.load(open(get(cache, "countries.json"), encoding="utf-8")); idx = {}
    for c in cs:
        for nm in [c["name"]["common"], c["name"]["official"]] + c.get("altSpellings", []): idx.setdefault(norm(nm), c)
    bad = []; unmatched = []; n = 0
    for x in items:
        p = x["id"].split("-")
        if p[0] != "geo": continue
        q, a = x["question"]["en"], x["answer"]["en"]; kind = p[1]
        m = {"cap": r"capital of (.+)\?", "cont": r"continent is (.+)\?", "lang": r"language of (.+)\?", "cur": r"currency of (.+)\?"}.get(kind)
        name = a if kind == "rev" else (re.search(m, q).group(1) if m and re.search(m, q) else None)
        name = re.sub(r"^the ", "", name or "", flags=re.I)
        c = idx.get(norm(name)) or idx.get(norm("the " + name)) or idx.get(norm({"Turkey": "Türkiye"}.get(name, name)))
        if not c: unmatched.append((x["id"], name)); continue
        n += 1
        if kind in ("cap", "rev"):
            cap = a if kind == "cap" else re.search(r"^(.+) is the capital", q).group(1)
            ok = any(norm(cap) == norm(k) or norm(k) in norm(cap) or norm(cap) in norm(k) for k in c["capital"]) or norm(cap) in ("ulaanbaatar",) and "ulanbator" in [norm(k) for k in c["capital"]]
            exp = c["capital"]
        elif kind == "cont": ok = a == continent(c); exp = continent(c)
        elif kind == "lang": ok = any(norm(a) == norm(v) or (norm(a), norm(v)) in SAME for v in c["languages"].values()); exp = list(c["languages"].values())
        else:
            exp = [v["name"] for v in c["currencies"].values()]
            # the dataset names a currency with its adjective («Thai baht»), the bank may use the plain word («baht»): they meet on the currency's own noun
            ok = not exp or any((norm(a), norm(e)) in SAME or norm(a) == norm(e) or norm(e).endswith(norm(a.split()[-1] if len(a.split()) > 1 else a)) or norm(a).endswith(norm(e.split()[-1])) for e in exp)
        if not ok: bad.append((x["id"], q, a, exp))
    return n, bad, unmatched

if __name__ == "__main__":
    what, path, cache = sys.argv[1:4]
    items = json.load(open(path, encoding="utf-8"))
    if what == "quran":
        n, bad = quran(items, cache); print(f"quran: {n} items checked against Tanzil, {len(bad)} differ"); [print("  ", b) for b in bad]
    else:
        n, bad, un = geo(items, cache); print(f"geo: {n} items matched to mledoze/countries, {len(bad)} differ, {len(un)} not matched"); [print("  DIFF", b) for b in bad]; [print("  ?", u) for u in un]
