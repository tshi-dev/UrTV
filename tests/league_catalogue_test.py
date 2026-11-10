#!/usr/bin/env python3
"""4.3 «كل الدوريات»: the catalogue CI builds (.github/tools/league_catalogue.py), on made-up answers: the walk resumes where it
stopped and stops when the server is busy, the content rules (no league of Israel or named after it, Western Sahara is Morocco's),
only the sports the page knows, only a competition with a season, the hand-named Arabic first. Given a file (CI, before the push),
the same rules on what is pushed.
    python3 tests/league_catalogue_test.py [catalogue/catalogue.json]"""
import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


ARABIC = re.compile(r"[؀-ۿ]")


def rules(cat, where):
    lg = cat.get("leagues") or []
    check(f"{where}: the count says how many there are", cat.get("count") == len(lg), (cat.get("count"), len(lg)))
    check(f"{where}: no league of Israel or named after it", not any(re.search(r"israel|إسرائيل", json.dumps(e, ensure_ascii=False), re.I) for e in lg))
    check(f"{where}: Western Sahara is never a country", not any("sahara" in (e.get("country") or "").lower() for e in lg))
    check(f"{where}: only the sports the page knows", all(e.get("sport") in ("Soccer", "Basketball", "American Football") for e in lg))
    check(f"{where}: every entry has a numeric id and a name; an Arabic name is Arabic", all(re.fullmatch(r"\d{1,6}", e.get("id", "")) and e.get("en")
          and (not e.get("ar") or ARABIC.search(e["ar"])) for e in lg))
    check(f"{where}: no id twice", len({e["id"] for e in lg}) == len(lg))
    bare = sorted({e.get("country") for e in lg if not ARABIC.search(e.get("country_ar") or "")})
    check(f"{where}: every country has its Arabic name (Czechia, The Netherlands, United Kingdom were English on the TV)", not bare, bare)
    tops = [e.get("top") for e in lg]
    check(f"{where}: the hand-named come first", tops == sorted(tops, reverse=True))


if len(sys.argv) > 1:
    rules(json.loads(Path(sys.argv[1]).read_text(encoding="utf-8")), sys.argv[1])
    sys.exit(1 if failed else 0)

spec = importlib.util.spec_from_file_location("league_catalogue", ROOT / ".github/tools/league_catalogue.py")
lc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lc)

keys = [k for k, _ in lc.AR]
check("about 120 competitions are hand-named in Arabic", 110 <= len(lc.AR) <= 200, len(lc.AR))
check("every hand name is Arabic", all(ARABIC.search(a) for _, a in lc.AR))
check("no hand key twice", len(set(keys)) == len(keys), [k for k in keys if keys.count(k) > 1])
check("the hand names say nothing of Israel", not any(re.search(r"israel|إسرائيل", k + a, re.I) for k, a in lc.AR))
check("no country we never list in the Arabic countries", not any(re.search(r"israel|sahara", c, re.I) for c in lc.COUNTRY_AR))

FAKE = {
    4328: {"idLeague": "4328", "strLeague": "English Premier League", "strSport": "Soccer", "strCountry": "England", "strCurrentSeason": "2026-2027"},
    4329: {"idLeague": "4329", "strLeague": "English League Championship", "strSport": "Soccer", "strCountry": "England", "strCurrentSeason": "2026-2027"},
    4400: {"idLeague": "4400", "strLeague": "Israeli Premier League", "strSport": "Soccer", "strCountry": "Israel", "strCurrentSeason": "2026-2027"},
    4401: {"idLeague": "4401", "strLeague": "Ligat ha'Al", "strSport": "Soccer", "strCountry": "Israel", "strCurrentSeason": "2026-2027"},
    4402: {"idLeague": "4402", "strLeague": "Some Cup", "strLeagueAlternate": "Israel State Cup", "strSport": "Soccer", "strCountry": "", "strCurrentSeason": "2026"},
    4403: {"idLeague": "4403", "strLeague": "Dakhla League", "strSport": "Soccer", "strCountry": "Western Sahara", "strCurrentSeason": "2026"},
    4404: {"idLeague": "4404", "strLeague": "ATP World Tour", "strSport": "Tennis", "strCountry": "Worldwide", "strCurrentSeason": "2026"},
    4405: {"idLeague": "4405", "strLeague": "Defunct League", "strSport": "Soccer", "strCountry": "Spain", "strCurrentSeason": ""},
    4406: {"idLeague": "4406", "strLeague": "Andorran 1a Divisio", "strSport": "Soccer", "strCountry": "Andorra", "strCurrentSeason": "2026-2027"},
    4407: {"idLeague": "4407", "strLeague": "AFC Asian Cup", "strSport": "Soccer", "strCountry": "Worldwide", "strCurrentSeason": "2027"},
    4387: {"idLeague": "4387", "strLeague": "NBA", "strSport": "Basketball", "strCountry": "USA", "strCurrentSeason": "2026-2027"},
}
lc.LOW, lc.MISS_TAIL = 4328, 100


def ask_all(i):
    return {"leagues": [FAKE[i]]} if i in FAKE else {"leagues": None}


state, run = lc.walk({"cursor": lc.LOW, "seen": {}, "high": lc.LOW}, budget_s=60, ask=ask_all)
check("a whole round in one run ends the run, the cursor back at the start", run["stop"] == "round" and state["cursor"] == lc.LOW and state["high"] == 4407, (run, state["cursor"], state["high"]))
cat = lc.build(state["seen"])
ids = [e["id"] for e in cat]
check("kept: the leagues with a season, of the page's sports", set(ids) == {"4328", "4329", "4403", "4406", "4407", "4387"}, ids)
check("left out: Israel's leagues, one named after it, tennis, one with no season", not ({"4400", "4401", "4402", "4404", "4405"} & set(ids)), ids)
sah = next(e for e in cat if e["id"] == "4403")
check("Western Sahara's league is listed under Morocco", sah["country"] == "Morocco" and sah["country_ar"] == "المغرب", sah)
check("hand-named first, in the list's order: the Premier League, the Championship, the AFC Asian Cup, the NBA; then by country",
      ids[:4] == ["4328", "4329", "4407", "4387"] and ids[4:] == ["4406", "4403"], ids)
epl = cat[0]
check("a hand-named one carries its Arabic, a country in Arabic and top", epl["ar"] == "الدوري الإنجليزي" and epl["country_ar"] == "إنجلترا" and epl["top"], epl)
check("one not named yet keeps English, no Arabic", next(e for e in cat if e["id"] == "4406")["ar"] == "")
rules({"count": len(cat), "leagues": cat}, "made-up catalogue")
check("an older state's banned entry is dropped at build", "4400" not in [e["id"] for e in lc.build({"4400": {"id": "4400", "en": "Israeli Premier League", "country": "Israel", "sport": "Soccer"}})])

# the walk resumes and stops on a busy server
calls = []


def busy_after(n):
    def ask(i):
        calls.append(i)
        if len(calls) > n:
            raise lc.Busy()
        return ask_all(i)
    return ask


st, run = lc.walk({"cursor": 4330, "seen": {}, "high": 4329}, budget_s=60, ask=busy_after(3))
check("a busy server ends the run where it is; the next run goes on from there", run["stop"] == "busy" and st["cursor"] == 4333 and calls[0] == 4330, (run, st["cursor"], calls))
st2, _ = lc.walk({"cursor": 4330, "seen": {"4400": None, "4328": {"id": "4328", "en": "English Premier League", "country": "England", "sport": "Soccer"}}, "high": 4407},
                 budget_s=60, ask=lambda i: None)
check("an id that did not answer keeps what was known of it", "4328" in st2["seen"])
# every country in Arabic, or the file is not pushed
check("TheSportsDB's spellings the TV showed in English have Arabic: Czechia, The Netherlands, United Kingdom",
      all(lc.COUNTRY_AR.get(c) for c in ("Czechia", "The Netherlands", "United Kingdom")))
check("the app's own country list is in the table (Bosnia and Herzegovina, Kazakhstan, Vietnam...)",
      all(ARABIC.search(lc.COUNTRY_AR.get(c, "")) for c in ("Bosnia and Herzegovina", "Kazakhstan", "Vietnam", "Albania", "Andorra", "Costa Rica")))
narnia = lc.build({"9": {"id": "9", "en": "Narnian League", "country": "Narnia", "sport": "Soccer"}})
check("a country with no Arabic name is found", lc.no_arabic(narnia) == ["Narnia"])
import tempfile
with tempfile.TemporaryDirectory() as tmp:
    out0, walk0 = lc.OUT, lc.walk
    lc.OUT = Path(tmp)
    lc.walk = lambda st: ({"cursor": 10, "seen": {"9": {"id": "9", "en": "Narnian League", "country": "Narnia", "sport": "Soccer"}}, "high": 9}, {"asked": 1, "stop": "time"})
    import contextlib, io
    with contextlib.redirect_stdout(io.StringIO()):
        rc = lc.main()
    rep = (Path(tmp) / "report.md").read_text(encoding="utf-8")
    lc.walk = lambda st: ({"cursor": 10, "seen": {"4328": {"id": "4328", "en": "English Premier League", "country": "England", "sport": "Soccer"}}, "high": 9}, {"asked": 1, "stop": "time"})
    with contextlib.redirect_stdout(io.StringIO()):
        rc2 = lc.main()
    lc.OUT, lc.walk = out0, walk0
check("the build fails (nothing pushed) while a country has no Arabic name, and the report says which", rc == 1 and "- Narnia" in rep and rc2 == 0, (rc, rc2))
miss = lc.unmatched(cat)
check("the report lists the hand names that matched nothing", "Spanish La Liga 2" in miss and "4328" not in miss)
src = (ROOT / ".github/tools/league_catalogue.py").read_text(encoding="utf-8")
check("the free key only, and a gap between requests", 'json/123/"' in src and re.search(r"^GAP = [3-9]", src, re.M))
wf = (ROOT / ".github/workflows/league-catalogue.yml").read_text(encoding="utf-8")
check("CI: weekly at most, the rules checked on the file before the push", re.search(r"cron: '\d+ \d+ \* \* \d'", wf) and "league_catalogue_test.py catalogue/catalogue.json" in wf)
sys.exit(1 if failed else 0)
