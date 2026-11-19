#!/usr/bin/env python3
"""4.5 honours, offline (.github/tools/honours.py, docs/HONOURS-STUDY.md): reading a season article's infobox (the champion, the top scorer
and his goals, Arabic and English); a league's champions from the archive, Wikidata and Wikipedia, with a disagreement left out and a count
only when the list is complete; the top scorers only when the sources agree; a failed source keeps the published files; the workflow.

    python3 tests/honours_test.py
"""
import json
import re
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / ".github/tools"))
import honours as h  # noqa: E402
h.PACE = 0   # offline: no pause between the fake answers

failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


# ---- the infobox
AR = "{{معلومات موسم دوري\n| البطل = [[نادي الهلال|الهلال]] (اللقب الـ19)\n| مجموع الأهداف = 840\n| هداف الدوري = {{رمز علم|البرتغال}} [[كريستيانو رونالدو]]<br>{{صغير|(35 هدف – قياسيّ)}}\n}}"
EN = "{{Infobox football league season\n| champions = [[Al Hilal SFC|Al-Hilal]]\n| league topscorer = [[Cristiano Ronaldo]]<br>(35 goals)\n| goals = 840\n}}"
check("ar: «هداف الدوري» read (not «مجموع الأهداف»): the player and 35 goals", h.scorer_value(h.field(AR, h.SCORER_FIELD["ar"])[1]) == {"names": [("كريستيانو رونالدو", "كريستيانو رونالدو")], "goals": 35},
      h.scorer_value(h.field(AR, h.SCORER_FIELD["ar"])[1]))
check("en: «league topscorer»: the player and 35 goals", h.scorer_value(h.field(EN, h.SCORER_FIELD["en"])[1]) == {"names": [("Cristiano Ronaldo", "Cristiano Ronaldo")], "goals": 35})
check("the champion: the link's text, ar and en", h.champion_value(h.field(AR, h.CHAMP_FIELD["ar"])[1]) == "الهلال" and h.champion_value(h.field(EN, h.CHAMP_FIELD["en"])[1]) == "Al-Hilal")
shared = "| league topscorer = {{flagicon|EGY}} [[Mohamed Salah]]<br>{{flagicon|KOR}} [[Son Heung-min]]<br>(23 goals each)"
check("a shared top scorer: both players, one goal count", h.scorer_value(h.field(shared, h.SCORER_FIELD["en"])[1]) == {"names": [("Mohamed Salah", "Mohamed Salah"), ("Son Heung-min", "Son Heung-min")], "goals": 23})
ucl = "| top_goal_scorer = {{nowrap|[[Erling Haaland]] ([[Manchester City F.C.|Manchester City]])}}<br>12 goals"
check("a club in brackets after the player is not a player (UCL's infobox)", h.scorer_value(h.field(ucl, h.SCORER_FIELD["en"])[1]) == {"names": [("Erling Haaland", "Erling Haaland")], "goals": 12})
check("two goal counts in one field: not plain, nothing said", h.scorer_value("[[A]] (20 goals)<br>[[B]] (16 goals)") is None)
check("no player linked: nothing said (an unlinked text is not trusted)", h.scorer_value("Kodjo Laba (28 goals)") is None)
check("an infobox on one line («| champions = | relegated = …»): an empty champion stays empty, never the next field",
      h.field("{{Infobox | champions = | relegated = [[Al-Hazem]] }}", h.CHAMP_FIELD["en"]) == (None, "")
      and h.champion_value("| relegated =") == "")
check("the club's Arabic label without «نادي»", h.ar_club("نادي الهلال") == "الهلال" and h.ar_club("النادي الأهلي") == "الأهلي")

# ---- season keys
check("a season over two years: the archive's key", h.season_key("2023-08-11", "2024-05-27", {"2023-2024"}) == "2023-2024")
check("AFC 2022 (April to February): the archive's «2022»", h.season_key("2022-04-15", "2023-02-26", {"2022", "2023"}) == "2022")
check("MLS (a calendar year): «2024»", h.season_key("2024-02-21", "2024-12-07", set()) == "2024")
check("an old season the archive does not keep: «1977-1978»", h.season_key("1977-09-01", "1978-05-01", {"2025-2026"}) == "1977-1978")

check("an item with no start date: its English title says the season («2008–09 …», «1977–1978 …», «1977 …»), a title that names none gives none",
      h.dates_of_title("2008–09 Süper Lig") == ("2008-08-01", "2009-05-31") and h.dates_of_title("1999–2000 Saudi Premier League") == ("1999-08-01", "2000-05-31")
      and h.dates_of_title("Saudi Premier League 1977–1978") == ("1977-08-01", "1978-05-31") and h.dates_of_title("1977 Copa del Rey") == ("1977-03-01", "1977-11-30")
      and h.dates_of_title("Süper Lig") == ("", "") and h.dates_of_title("2008–11 something") == ("", ""))
ENT = {"Q8": {"claims": {"P3450": [{"mainsnak": {"snaktype": "value", "datavalue": {"value": {"id": "Q485568"}}}, "rank": "normal"}]},
              "sitelinks": {"enwiki": {"title": "2008–09 Süper Lig"}}, "labels": {}}}
check("…and the season items read that way (Wikidata dates only a few of the Süper Lig's 69)", h.seasons_from_entities(ENT, {"Q8": "4339"})[0]["start"] == "2008-08-01")

# ---- champions
def item(q, start, end, win=(), ts=(), ar="", en=""):
    return {"q": q, "lg": "4668", "start": start, "end": end, "win": list(win), "ts": list(ts), "ar": ar, "en": en}
LAB = {"QH": {"en": "Al Hilal SFC", "ar": "الهلال"}, "QN": {"en": "Al-Nassr", "ar": "النصر"}, "QI": {"en": "Al Ittihad FC", "ar": "الاتحاد"},
       "QR": {"en": "Cristiano Ronaldo", "ar": "كريستيانو رونالدو"}}
ARCH = {"seasons": ["2026-2027", "2025-2026", "2024-2025"], "champs": [
    {"s": "2025-2026", "id": "136014", "n": "Al-Nassr", "ar": "النصر", "pts": 86, "gap": 2, "vs": "136013", "var": "الهلال"},
    {"s": "2024-2025", "id": "136015", "n": "Al Ittihad", "ar": "الاتحاد", "pts": 83, "gap": 8, "vs": "136013", "var": "الهلال"}]}
NAMES = {"136013": ("Al-Hilal", "الهلال"), "136014": ("Al-Nassr", "النصر"), "136015": ("Al Ittihad", "الاتحاد")}
items = [item("Q21", "2021-08-01", "2022-05-30", ["QH"]), item("Q22", "2022-08-01", "2023-05-30", ["QI"]), item("Q23", "2023-08-01", "2024-05-30", ["QH"]),
         item("Q24", "2024-08-01", "2025-05-30", [], en="2024–25 Saudi Pro League"), item("Q25", "2025-08-01", "2026-05-30", ["QN"])]
WP = {"en": {"2024–25 Saudi Pro League": EN.replace("Al Hilal SFC|Al-Hilal", "Al-Ittihad Club|Al-Ittihad")}, "ar": {}}
body, rep = h.champions("4668", items, ARCH, LAB, WP, NAMES)
got = [(r["s"], r.get("id"), r["ar"], r["src"]) for r in body["seasons"]]
check("champions: newest first; the archive and Wikidata agree (both), Wikipedia fills a season Wikidata has no winner of; ids and Arabic names",
      got == [("2025-2026", "136014", "النصر", "archive+wikidata"), ("2024-2025", "136015", "الاتحاد", "archive+wikipedia"),
              ("2023-2024", "136013", "الهلال", "wikidata"), ("2022-2023", "136015", "الاتحاد", "wikidata"), ("2021-2022", "136013", "الهلال", "wikidata")], got)
check("…the archive's points and margin carried on its seasons", body["seasons"][0].get("pts") == 86 and body["seasons"][0].get("gap") == 2)
check("…a season named from Wikipedia carries its article's address", body["seasons"][1].get("url") == ["https://en.wikipedia.org/wiki/2024%E2%80%9325_Saudi_Pro_League"], body["seasons"][1])
check("…every season named, no gap: complete, and each club's count", body["complete"] and body["titles"] == {"136014": 1, "136015": 2, "136013": 2} and not rep, (body.get("titles"), rep))
bad = items[:-1] + [item("Q25", "2025-08-01", "2026-05-30", ["QH"])]
body2, rep2 = h.champions("4668", bad, ARCH, LAB, WP, NAMES)
check("a disagreement (archive: Al-Nassr, Wikidata: Al-Hilal): the season left out, in the report; not complete, no count",
      "2025-2026" not in [r["s"] for r in body2["seasons"]] and rep2 and rep2[0]["why"] == "sources disagree" and not body2["complete"] and "titles" not in body2, rep2)
check("…and the file says it held that season, so the page does not put the archive's champion back", body2.get("held") == ["2025-2026"], body2.get("held"))
gap = [x for x in items if x["q"] != "Q22"]
body3, rep3 = h.champions("4668", gap, {"seasons": [], "champs": []}, LAB, WP, NAMES)
check("a season missing (no item for 2022-23): not complete, no count", not body3["complete"] and "titles" not in body3 and any(r["why"] == "a gap in the seasons" for r in rep3), rep3)
nowin = items[:3] + [item("Q24", "2024-08-01", "2025-05-30", [])]
body4, rep4 = h.champions("4668", nowin, {"seasons": [], "champs": []}, LAB, {"en": {}, "ar": {}}, NAMES)
undated = items + [item("Q00", "", "")]
body5, rep5 = h.champions("4668", undated, ARCH, LAB, WP, NAMES)
check("an item no date or title places: in the report, and the list is not complete (no count)", not body5["complete"] and "titles" not in body5
      and any(r["why"] == "a season item with no date" for r in rep5), rep5)
dup = items + [item("Q26b", "2025-08-02", "2026-05-30", ["QN"])]
body6, rep6 = h.champions("4668", dup, ARCH, LAB, WP, NAMES)
check("two items for one season: the first kept, in the report, not complete", not body6["complete"] and any(r["why"] == "two Wikidata items for one season" for r in rep6), rep6)
check("an old season no source names: not complete", not body4["complete"] and any(r["why"] == "no source names a champion" for r in rep4), rep4)

under = items + [item("Q27", "2026-08-01", "2027-05-30", [], en="2026–27 Saudi Pro League")]
WPU = {"en": dict(WP["en"], **{"2026–27 Saudi Pro League": EN}), "ar": {}}
body7, _ = h.champions("4668", under, ARCH, LAB, WPU, NAMES)
check("the season under way: its article's infobox never names a champion (run 1 named «relegated =» for 2026-27)", "2026-2027" not in [r["s"] for r in body7["seasons"]],
      [r["s"] for r in body7["seasons"]][:3])
tmpi = Path(tempfile.mkdtemp())
(tmpi / "index.json").write_text(json.dumps({"teams": {"136014": {"en": "Al-Nassr", "ar": "النصر", "leagues": ["4668"]}, "900": {"en": "Other", "ar": "آخر", "leagues": ["4328"]}}}))
check("the clubs' names from the archive's index, this league's only (urtv-data has no sports/ files yet: run 1 had no ids)",
      h.archive_names(tmpi, "4668") == {"136014": ("Al-Nassr", "النصر")})

# ---- scorers
SI = [item("Q23", "2023-08-01", "2024-05-30", ar="الدوري 2023–24", en="2023–24 Saudi Pro League"),
      item("Q24", "2024-08-01", "2025-05-30", ts=[("QR", 25)], en="2024–25 Saudi Pro League", ar="الدوري 2024–25")]
WPS = {"ar": {"الدوري 2023–24": AR, "الدوري 2024–25": AR.replace("35 هدف", "20 هدف")},
       "en": {"2023–24 Saudi Pro League": EN, "2024–25 Saudi Pro League": EN.replace("35 goals", "25 goals")}}
sb, srep = h.scorers("4668", SI, ARCH, LAB, WPS, {"Cristiano Ronaldo": "كريستيانو رونالدو"})
s23 = sb["seasons"].get("2023-2024")
check("scorers: ar and en agree (one player, 35): published, the Arabic name, the goals, both articles as its source",
      s23 and s23["top"] == [{"en": "Cristiano Ronaldo", "ar": "كريستيانو رونالدو", "goals": 35}] and len(s23["src"]) == 2 and sb["lic"] == "CC BY-SA 4.0", s23)
check("scorers: ar says 20, en and Wikidata 25: left out, in the report (UAE 2024-25's case)", "2024-2025" not in sb["seasons"] and srep and srep[0]["why"] == "scorer sources disagree", srep)
one = [item("Q26", "2025-08-01", "2026-05-30", en="2025–26 Saudi Pro League"), item("Q27", "2026-08-01", "2027-05-30", en="2026–27 Saudi Pro League")]
sb2, _ = h.scorers("4668", one, ARCH, LAB, {"en": {"2025–26 Saudi Pro League": EN.replace("35 goals", "33 goals").replace("Cristiano Ronaldo", "Julián Quiñones"),
                                                   "2026–27 Saudi Pro League": EN.replace("35 goals", "9 goals")}, "ar": {}}, {})
check("one source with a plain field: published (the Latin name when no Arabic is known)", sb2["seasons"].get("2025-2026", {}).get("top") == [{"en": "Julián Quiñones", "ar": "", "goals": 33}], sb2)
check("the season under way has no top scorer yet (an infobox's «so far» would be a week old)", "2026-2027" not in sb2["seasons"], list(sb2["seasons"]))

# ---- a run: Wikidata does not answer -> nothing written; it answers -> the files, the licence, a league with nothing keeps its last file
tmp = Path(tempfile.mkdtemp())
(tmp / "archive/sports").mkdir(parents=True)
(tmp / "archive/sports/4668.json").write_text(json.dumps(dict(ARCH, table=[{"id": k, "n": v[0], "ar": v[1]} for k, v in NAMES.items()])))
(tmp / "prev/champions").mkdir(parents=True)
(tmp / "prev/champions/4328.json").write_text('{"v":1,"id":"4328","complete":false,"seasons":[{"s":"2025-2026","en":"Arsenal"}],"updated":"old"}')
args = SimpleNamespace(archive=str(tmp / "archive"), prev=str(tmp / "prev"), out=str(tmp / "out"))
def down(url):
    raise OSError("no network")
check("Wikidata does not answer: the run stops, nothing written (the published files stay)", h.run(args, down) == 1 and not (tmp / "out").exists())
def snak(v):
    return {"mainsnak": {"snaktype": "value", "datavalue": {"value": v}}, "rank": "normal"}
ENTS = {"Q25": {"claims": {"P3450": [snak({"id": "Q255633"})], "P580": [snak({"time": "+2025-08-01T00:00:00Z"})], "P582": [snak({"time": "+2026-05-30T00:00:00Z"})],
                           "P1346": [snak({"id": "Q1001"})]}, "sitelinks": {}},
        "Q20": {"claims": {"P3450": [snak({"id": "Q255633"})], "P580": [snak({"time": "+2024-08-01T00:00:00Z"})],
                           "P1346": [{"mainsnak": {"snaktype": "somevalue"}, "rank": "normal"}]}, "sitelinks": {}},
        "Q99": {"claims": {"P3450": [snak({"id": "Q9448"})]}, "sitelinks": {}}}   # found by the search, but of another league: not this one
asked = []
def net(url):
    asked.append(url)
    if "list=search" in url:
        return json.dumps({"query": {"search": [{"title": "Q25"}, {"title": "Q20"}, {"title": "Q99"}] if "Q255633" in url else []}}).encode()
    if "wbgetentities" in url and "claims" in url:
        return json.dumps({"entities": {q: e for q, e in ENTS.items() if q in url}}).encode()
    if "wbgetentities" in url:
        return json.dumps({"entities": {"Q1001": {"labels": {"en": {"value": "Al-Nassr"}, "ar": {"value": "نادي النصر"}}}}}).encode()
    return b'{"query":{"pages":[]}}'
check("a run with answers: 0", h.run(args, net) == 0)
c = json.loads((tmp / "out/champions/4668.json").read_text())
check("…champions/4668.json: the archive's two seasons, 2025-26 confirmed by Wikidata; an «unknown value» winner is no one",
      [(r["s"], r["src"]) for r in c["seasons"]] == [("2025-2026", "archive+wikidata"), ("2024-2025", "archive")] and c["updated"], c)
check("…the archive's seasons alone are not a complete history (nothing older): no count", not c["complete"] and "titles" not in c, c)
check("…a league with nothing new keeps its last file", json.loads((tmp / "out/champions/4328.json").read_text())["seasons"][0]["en"] == "Arsenal")
lic = (tmp / "out/LICENSE.md").read_text()
check("…LICENSE.md: scorers CC BY-SA 4.0, champions CC BY 4.0 with Wikidata CC0", "CC BY-SA 4.0" in lic and "CC BY 4.0" in lic and "CC0" in lic)
check("…report.json written", "left_out" in json.loads((tmp / "out/report.json").read_text()))
check("…Wikidata through its API, never the query service (it refuses a busy client for many minutes)", not any("query.wikidata.org" in u for u in asked) and any("haswbstatement" in u for u in asked))
check("…a user agent that names the project, as Wikimedia asks", "github.com/tshi-dev/UrTV" in h.UA)

# ---- the workflow
wf = (ROOT / ".github/workflows/honours.yml").read_text(encoding="utf-8")
check("the workflow: weekly, by hand, its actions pinned to a commit", "schedule:" in wf and "workflow_dispatch" in wf
      and all(re.search(r"@[0-9a-f]{40}\b", line) for line in wf.splitlines() if "uses:" in line))
check("…reads urtv-data's archive first, data-archive else; pushes only data-honours, only when it changed",
      wf.index("urtv-data") < wf.index("origin data-archive") and "data-honours" in wf and "already the same" in wf and "git push" in wf and wf.count("git push") == 1)
sys.exit(1 if failed else 0)
