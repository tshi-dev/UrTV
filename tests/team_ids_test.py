#!/usr/bin/env python3
"""The clubs collector's offline parts (.github/tools/team_ids.py): its transliteration is the screen's translit()
(run in node from src/template.html), Wikidata labels are cleaned, no two teams keep one Arabic name, and the
monthly diff names what changed. Run: python3 tests/team_ids_test.py"""
import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("team_ids", ROOT / ".github/tools/team_ids.py")
ti = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ti)
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


# transliteration: the same as the screen's, on every English name in the list and a few hard ones
names = [c["en"] for c in json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))]
names += ["Al-Hilal", "PSG", "St. Louis City SC", "Brighton & Hove Albion", "1. FC Köln", "Schalke 04", "AC Milan",
          "Ceará", "Cercle Brugge", "Beşiktaş", "Fenerbahçe", "İstanbul Başakşehir", "Kasımpaşa", "Atlético Madrid", "Al Ain", "  Two  Spaces ", "Queens Park Rangers", "Chelsea"]
node = shutil.which("node")
if not node:
    print("SKIP translit against the screen: no node here")
else:
    page = (ROOT / "src/template.html").read_text(encoding="utf-8")
    start = page.index("const TL_WORDS")
    end = page.index("async function wikidataArabic")
    js = ("const hasArabic = s => /[؀-ۿ]/.test(s);\nconst ar = d => String(d).replace(/[0-9]/g, c => "
          "String.fromCharCode(0x660 + +c));\n" + page[start:end]
          + "\nprocess.stdout.write(JSON.stringify(JSON.parse(require('fs').readFileSync(0, 'utf8')).map(translit)));")
    r = subprocess.run([node, "-e", js], input=json.dumps(names), capture_output=True, text=True, encoding="utf-8")
    check("node runs the screen's translit()", r.returncode == 0, r.stderr[-400:])
    if r.returncode == 0:
        screen = json.loads(r.stdout)
        differ = [(n, s, ti.translit(n)) for n, s in zip(names, screen) if s != ti.translit(n)]
        check(f"translit: the screen's on all {len(names)} names", not differ, differ[:6])

# the name key: the collector's team_key() is the screen's teamKey(), accents folded, so teams-ar.json's
# "Atletico Madrid" names TheSportsDB's "Atlético Madrid"
for name, want in [("Atlético Madrid", "atletico madrid"), ("Borussia Mönchengladbach", "borussia monchengladbach"),
                   ("Beşiktaş", "besiktas"), ("Fenerbahçe", "fenerbahce"), ("İstanbul Başakşehir", "istanbul basaksehir"),
                   ("Kasımpaşa", "kasimpasa"), ("Al-Hilal SFC", "alhilal sfc"), ("Al Hilal FC", "alhilal")]:
    check(f"team_key({name!r}) = {want!r}", ti.team_key(name) == want, ti.team_key(name))
if node:
    key_js = page[page.index("const teamKey ="):page.index("\n", page.index(".replace(/^al /,'al');"))]
    r = subprocess.run([node, "-e", key_js + "\nprocess.stdout.write(JSON.stringify(JSON.parse(require('fs')"
                        ".readFileSync(0, 'utf8')).map(teamKey)));"], input=json.dumps(names), capture_output=True,
                       text=True, encoding="utf-8")
    differ = [(n, s, ti.team_key(n)) for n, s in zip(names, json.loads(r.stdout or "[]")) if s != ti.team_key(n)]
    check(f"team_key: the screen's teamKey() on all {len(names)} names", r.returncode == 0 and not differ,
          differ[:6] or r.stderr[-300:])
first, last, _ = ti.curated()
for name, want in [("Atlético Madrid", "أتلتيكو مدريد"), ("Borussia Mönchengladbach", "بوروسيا مونشنغلادباخ"),
                   ("Beşiktaş", "بشكتاش"), ("Fenerbahçe", "فنربخشة")]:
    check(f"curated name for {name}: {want}", last.get(ti.team_key(name)) == want, last.get(ti.team_key(name)))

for label, want in [("نادي العين (الإمارات)", "العين"), ("النادي الأهلي (قطر)", "الأهلي"), ("فريق الوصل", "الوصل"),
                    ("نادي ريال مدريد لكرة القدم", "ريال مدريد"), ("ريَال", "ريال"), ("گوتنبرغ", "جوتنبرغ"),
                    ("بولونيا 1909", "بولونيا"), ("بادربورن 07", "بادربورن"), ("فلم 2 تيلبورخ", "فلم ٢ تيلبورخ"),
                    ("دي.سي. يونايتد", "دي.سي. يونايتد"), ("نادى القناة", "القناة"),
                    ("Al Ain", ""), ("", ""), ("ا", ""), ("نادي (قطر)", "")]:
    check(f"clean_ar({label!r}) = {want!r}", ti.clean_ar(label) == want, ti.clean_ar(label))

for club, label, want in [("Espanyol", "RCD Espanyol B", True), ("Willem II", "Willem II", False),
                          ("Bayern Munich", "FC Bayern Munich II", True), ("Ajax", "AFC Ajax", False),
                          ("Real Madrid", "Real Madrid Castilla", True), ("Barcelona", "FC Barcelona Femení", True)]:
    side = bool(ti.SIDE.search(label) and not ti.SIDE.search(club))
    check(f"{label!r} for {club!r}: {'another side, not the club' if want else 'the club'}", side == want, side)

# names told apart: the Saudi club and the national team keep theirs
teams = {
    "1": {"en": "Al-Ain", "ar": "العين", "leagues": ["Saudi First Division League"], "arFrom": "curated"},
    "2": {"en": "Al Ain", "ar": "العين", "leagues": ["UAE Pro League"], "arFrom": "curated"},
    "3": {"en": "Qatar", "ar": "قطر", "leagues": ["National teams"], "arFrom": "curated"},
    "4": {"en": "Qatar SC", "ar": "قطر", "leagues": ["Qatar Stars League"], "arFrom": "curated"},
    "5": {"en": "Al-Arabi", "ar": "العربي", "leagues": ["Qatar Stars League"], "arFrom": "wikidata"},
    "6": {"en": "Al-Arabi Al-Saudi", "ar": "العربي", "leagues": ["Saudi First Division League"], "arFrom": "curated"},
    "7": {"en": "Al Ahly", "ar": "الأهلي المصري", "leagues": ["Egyptian Premier League"], "arFrom": "curated"},
    "8": {"en": "Al-Ahly Benghazi", "ar": "الاهلي المصري", "leagues": ["Egyptian Premier League"], "arFrom": "wikidata"},
    "9": {"en": "Al-Ahli", "ar": "الأهلي", "leagues": ["Saudi-Arabian Pro League"], "arFrom": "curated"},
}
report, problems = {"names told apart": []}, []
ti.unique_names(teams, report, problems)
got = {i: t["ar"] for i, t in teams.items()}
check("Saudi Al-Ain keeps «العين», the Emirati one gets its country", got["1"] == "العين" and got["2"] == "العين الإماراتي", got)
check("the national team keeps «قطر», the club becomes «نادي قطر»", got["3"] == "قطر" and got["4"] == "نادي قطر", got)
check("Saudi Al-Arabi keeps «العربي», Qatar's gets «العربي القطري»", got["6"] == "العربي" and got["5"] == "العربي القطري", got)
check("two names that differ only in hamza are told apart too", ti.plain_ar(got["7"]) != ti.plain_ar(got["8"]), got)
check("no names left shared", len({ti.plain_ar(a) for a in got.values()}) == len(got) and not problems, (got, problems))

# the same name in two sports: the football club keeps it, the other says its sport
sp = {"1": {"en": "Al-Ahli", "ar": "الأهلي", "sport": "Soccer", "leagues": ["Saudi-Arabian Pro League"]},
      "2": {"en": "Al-Ahli Jeddah", "ar": "الأهلي", "sport": "Basketball", "leagues": ["Saudi Basketball"]},
      "3": {"en": "Miami Heat", "ar": "ميامي هيت", "sport": "Basketball", "leagues": ["NBA"]}}
ti.unique_names(sp, {"names told apart": []}, [])
check("a basketball team named like a club says its sport", sp["1"]["ar"] == "الأهلي"
      and sp["2"]["ar"] == "الأهلي (كرة السلة)" and sp["3"]["ar"] == "ميامي هيت", {i: t["ar"] for i, t in sp.items()})
check("every NBA and NFL team is named in teams-ar.json, so Wikidata is never asked",
      all(last.get(ti.team_key(n)) for n in ("Los Angeles Lakers", "Golden State Warriors", "Kansas City Chiefs",
                                              "San Francisco 49ers", "Philadelphia 76ers")), "a name is missing")
check("«لوس أنجلوس ليكرز» and «غولدن ستيت ووريورز» as the owner spells them",
      last[ti.team_key("Los Angeles Lakers")] == "لوس أنجلوس ليكرز"
      and last[ti.team_key("Golden State Warriors")] == "غولدن ستيت ووريورز")

# every name the committed list calls ours comes back from teams-ar.json, so no run can lose it to the cache,
# Wikidata or a transliteration
listed = json.loads((ROOT / "assets/shared/teams.json").read_text(encoding="utf-8"))
again = {c["id"]: {"en": c["en"], "leagues": c["leagues"], "lookup": {"alt": c.get("alt") or []},
                   **({"ar": c["ar"]} if "National teams" in c["leagues"] else {})} for c in listed}
ti.arabic_names(again, {}, {})
ti.unique_names(again, {"names told apart": []}, [])
lost = [(c["en"], c["ar"], again[c["id"]]["ar"]) for c in listed if c["arFrom"] == "curated" and again[c["id"]]["ar"] != c["ar"]]
check(f"all {sum(c['arFrom'] == 'curated' for c in listed)} names of ours in the list come from teams-ar.json",
      not lost, lost[:6])

# an Arab club with no name of ours or Wikidata's is left without one, for a person to add; a club abroad is
# transliterated
gap = {"1": {"en": "Zzq Unknown Club", "leagues": ["Egyptian Premier League"]},
       "2": {"en": "Zzq Unknown Club", "leagues": ["Italian Serie A"]},
       "3": {"en": "Zzq Unknown Club", "leagues": ["Saudi First Division League"]}}
report = {}
ti.arabic_names(gap, {}, report)
check("no transliteration for an Arab league's club", gap["1"]["ar"] == "" and gap["1"]["arFrom"] == "missing"
      and gap["3"]["arFrom"] == "missing", gap)
check("a club abroad is transliterated, and marked so", gap["2"]["arFrom"] == "translit" and gap["2"]["ar"], gap["2"])
problems = []
ti.unique_names(gap, {"names told apart": []}, problems)
check("two clubs without a name are not a name clash", not problems, problems)

# the monthly diff
old = [{"id": "1", "en": "Al-Ain", "ar": "العين", "leagues": ["Saudi First Division League"]},
       {"id": "2", "en": "Gone FC", "ar": "غون", "leagues": ["UAE Pro League"]},
       {"id": "3", "en": "Hellas", "ar": "", "leagues": ["Italian Serie A"]}]
new = [{"id": "1", "en": "Al-Ain", "ar": "العين", "leagues": ["Saudi-Arabian Pro League"]},
       {"id": "3", "en": "Hellas Verona", "ar": "هيلاس فيرونا", "leagues": ["Italian Serie A"]},
       {"id": "4", "en": "New FC", "ar": "نيو", "leagues": ["UAE Pro League"]}]
d = ti.diff(old, new)
check("diff: added, removed, renamed, moved", [t["id"] for t in d["added"]] == ["4"] and [t["id"] for t in d["removed"]] == ["2"]
      and [t["id"] for t in d["renamed"]] == ["3"] and [t["id"] for t in d["leagues changed"]] == ["1"], d)
md = ti.diff_md(d, len(old), len(new))
check("diff.md lists them", all(s in md for s in ("New FC", "Gone FC", "Hellas -> Hellas Verona", "Saudi-Arabian Pro League")), md)


# ---- a whole run on a fake TheSportsDB and Wikidata: a full one, then an incremental one that looks up only
# the club that is new and the one renamed
import contextlib   # noqa: E402
import datetime   # noqa: E402
import io   # noqa: E402
import tempfile   # noqa: E402
import urllib.parse as up   # noqa: E402

LEAGUE_NAMES = list(ti.LEAGUES)
SEASONS = {n: ti.seasons_to_try(ti.LEAGUES[n], datetime.date.today())[:2] for n in LEAGUE_NAMES}
PINNED = {("Saudi-Arabian Pro League", 0): ("136013", "Al-Hilal"), ("Saudi-Arabian Pro League", 1): ("136022", "Al-Nassr"),
          ("Spanish La Liga", 0): ("133738", "Real Madrid"), ("English Premier League", 0): ("133604", "Arsenal")}


EVENT_LEAGUES = [{"strLeague": "Formula 1", "idLeague": "E1", "strSport": "Motorsport"},
                 {"strLeague": "UFC", "idLeague": "E2", "strSport": "Fighting"},
                 {"strLeague": "Boxing", "idLeague": "E3", "strSport": "Fighting"},
                 {"strLeague": "ATP World Tour", "idLeague": "E4", "strSport": "Tennis"},
                 {"strLeague": "WTA Tour", "idLeague": "E5", "strSport": "Tennis"},
                 {"strLeague": "Formula 2", "idLeague": "E6", "strSport": "Motorsport"}]


def fake_world(extra=None, renamed=None):
    clubs = {}
    for k, n in enumerate(LEAGUE_NAMES):
        clubs[n] = [PINNED.get((n, j), (f"9{k:02d}{j:03d}", f"Club {k} {j}")) for j in range(ti.LEAGUES[n]["min"] + 1)]
    for n, team in (extra or {}).items():
        clubs[n].append(team)
    names = {i: en for v in clubs.values() for i, en in v}
    names.update(renamed or {})
    national = {en: ("136137" if en == "Saudi Arabia" else f"8{j:03d}") for j, en in enumerate(ti.NATIONAL)}
    names.update({i: en for en, i in national.items()})
    return clubs, names, national


def fake_fetch(world, calls):
    clubs, names, national = world

    def fetch(url, data=None, headers=None, pause=0, tries=6, count="thesportsdb"):
        calls.append(url)
        ti.STATS[count] += 1   # as the real fetch() counts
        if not url.startswith(ti.API):
            return {}   # Wikidata and Wikipedia: nothing known, so names come from our lists or transliteration
        path, _, query = url[len(ti.API):].partition("?")
        q = dict(up.parse_qsl(query))
        if path == "all_leagues.php":
            return {"leagues": [{"strLeague": n, "idLeague": f"L{k}", "strSport": ti.LEAGUES[n]["sport"]}
                                for k, n in enumerate(LEAGUE_NAMES)] + EVENT_LEAGUES}
        if path == "search_all_teams.php":   # a sport's own team list: the NBA's has no preseason guests
            return {"teams": [{"idTeam": i, "strTeam": names[i]} for i, _ in clubs[q["l"]]]}
        if path == "eventsnextleague.php":
            return {"events": [{"idEvent": "1", "strEvent": "Fake Grand Prix", "dateEvent": "2030-01-01"}]
                    if q["id"] != "E5" else None}   # no upcoming WTA event
        if path == "eventspastleague.php":
            return {"events": [{"idEvent": "0", "strEvent": "Old Grand Prix"}]}
        if path == "search_all_leagues.php":
            return {"countries": None}
        if path == "eventsseason.php":
            n = LEAGUE_NAMES[int(q["id"][1:])]
            if q["s"] not in SEASONS[n]:
                return {"events": None}
            v = clubs[n]
            return {"events": [{"idHomeTeam": a[0], "strHomeTeam": names[a[0]], "idAwayTeam": b[0],
                                "strAwayTeam": names[b[0]]} for a, b in zip(v, v[1:] + v[:1])]}
        if path == "searchteams.php":
            if q["t"] == "Jordan":
                return {"teams": None}   # as TheSportsDB answers: found in its matches instead
            i = national.get(q["t"])
            return {"teams": [{"idTeam": i, "strTeam": q["t"], "strSport": "Soccer", "strGender": "Male"}] if i else None}
        if path == "searchevents.php":
            a, _, b = q["e"].replace("_", " ").partition(" vs ")
            return {"event": [{"idHomeTeam": national[a], "strHomeTeam": a, "idAwayTeam": national[b],
                               "strAwayTeam": b}]} if a in national and b in national else {"event": None}
        if path == "lookupteam.php":
            i = q["id"]
            return {"teams": [{"idTeam": i, "strTeam": names[i], "strGender": "Male"}]} if i in names else {"teams": None}
        return None   # an empty answer

    return fetch


def run_once(world, out, cache, full, *argv):
    calls = []
    ti.fetch, ti.FULL, ti.OUT, ti.CACHE = fake_fetch(world, calls), full, out, cache
    ti.STATS.update({k: 0 for k in ti.STATS})
    try:
        with contextlib.redirect_stdout(io.StringIO()):
            ti.main(list(argv))
    except SystemExit:
        pass   # a check failed (the fake Saudi clubs have no Arabic names): the files are written all the same
    report = out / "report.json"
    return calls, json.loads(report.read_text(encoding="utf-8")) if report.is_file() else {}



# a club a match names without an id: reported, and the season still counts as complete
def fake_get(path):
    if not path.startswith("eventsseason.php"):
        return {"events": None}
    season = path.rsplit("s=", 1)[1]
    names = [(str(500 + k), f"Club {k}") for k in range(20)]
    names[19] = (None, "Club Without Id") if season == ti.seasons_to_try(ti.LEAGUES["Spanish La Liga"],
                                                                         datetime.date.today())[0] else names[19]
    return {"events": [{"idHomeTeam": a[0], "strHomeTeam": a[1], "idAwayTeam": b[0], "strAwayTeam": b[1]}
                       for a, b in zip(names[::2], names[1::2])]}


def short_get(path):   # 2026-27 lists 19 of La Liga's 20: Club 19 is in last season's and in its own matches
    now, before = ti.seasons_to_try(ti.LEAGUES["Spanish La Liga"], datetime.date.today())[:2]
    if path.startswith("eventsseason.php"):
        names = [(str(500 + k), f"Club {k}") for k in range(20 if path.endswith(before) else 19)]
        names += [] if path.endswith(before) else [("518", "Club 18")]
        return {"events": [{"idHomeTeam": a[0], "strHomeTeam": a[1], "idAwayTeam": b[0], "strAwayTeam": b[1]}
                           for a, b in zip(names[::2], names[1::2])]}
    if path == "eventslast.php?id=519":
        return {"results": [{"idLeague": "4335", "strSeason": now, "idHomeTeam": "519", "strHomeTeam": "Club 19"}]}
    return {"events": None}


# early in a season the free API's first 100 games miss teams: the ids next to the found ones are looked up
asked_ids = []
def id_get(path):
    i = path.rsplit("=", 1)[1]
    asked_ids.append(i)
    return {"teams": [{"idTeam": i, "strTeam": f"Team {i}", "idLeague": "4387", "strSport": "Basketball"}]
            if i in ("134888", "134889") else None}
real_get, ti.get = ti.get, id_get
have = {str(134860 + i): f"Team {i}" for i in range(28)}
ti.fill_by_id("4387", "NBA", have)
ti.get = real_get
check("a short league is completed from the ids next to its own, and stops at the minimum", len(have) == 30
      and "134888" in have and "134889" in have, (len(have), asked_ids))

real_get, ti.get = ti.get, short_get
report, problems, teams = {"clubs per league": {}}, [], {}
with contextlib.redirect_stdout(io.StringIO()):
    ti.collect_league("Spanish La Liga", "4335", datetime.date.today(), teams, report, problems)
check("a club missing from the current season's list is found in its own matches", not problems
      and any("519" in x for x in report.get("found in their own matches", [])), (problems, report))
ti.get = real_get

real_get, ti.get = ti.get, fake_get
report, problems, teams = {"clubs per league": {}}, [], {}
with contextlib.redirect_stdout(io.StringIO()):
    ti.collect_league("Spanish La Liga", "4335", datetime.date.today(), teams, report, problems)
ti.get = real_get
league = report["clubs per league"]["Spanish La Liga"]
check("a club without an id is reported and counted", not problems and "Club Without Id" in json.dumps(league)
      and len(teams) == 20, (problems, league))

real_fetch = ti.fetch
with tempfile.TemporaryDirectory() as tmp:
    tmp = Path(tmp)
    (tmp / "one").mkdir()
    (tmp / "two").mkdir()
    world = fake_world()
    calls, report = run_once(world, tmp / "one", tmp / "none.json", full=True)
    lookups = [c for c in calls if "lookupteam.php" in c]
    every = len(json.loads((tmp / "one/teams.json").read_text(encoding="utf-8")))
    parts = len(ti.plan())
    cups = sum(1 for n in ti.LEAGUES if ti.LEAGUES[n].get("cup"))   # 4.3: a cup has no clubs of its own, so no probe
    check("full run: every team looked up (and one probe per part that has teams)", len(lookups) == every + parts - 1 - cups,
          (len(lookups), every, parts, cups))
    teams_out = {t["id"]: t for t in json.loads((tmp / "one/teams.json").read_text(encoding="utf-8"))}
    sports = {t["sport"] for t in teams_out.values()}
    check("full run: every team has its sport (football, basketball, American football)",
          sports == {"Soccer", "Basketball", "American Football"}, sports)
    check("full run: the NBA's 30 and the NFL's 32 teams, by league",
          sum("NBA" in t["leagues"] for t in teams_out.values()) == 31 and sum("NFL" in t["leagues"]
          for t in teams_out.values()) == 33, [(l, sum(l in t["leagues"] for t in teams_out.values())) for l in ("NBA", "NFL")])
    events = json.loads((tmp / "one/events.json").read_text(encoding="utf-8"))
    check("full run: events.json holds one Formula 1, UFC and boxing league and the tennis one with events",
          events == {"f1": [{"id": "E1", "name": "Formula 1"}], "ufc": [{"id": "E2", "name": "UFC"}],
                     "boxing": [{"id": "E3", "name": "Boxing"}], "tennis": [{"id": "E4", "name": "ATP World Tour"}]}, events)
    check("full run: the report keeps what a league answered, and says no Saudi basketball league was found",
          report["events"]["f1"][0]["next"][0]["strEvent"] == "Fake Grand Prix"
          and "no Saudi basketball" in str(report["events"]["saudi basketball"]), report["events"]["saudi basketball"])
    check("full run: Jordan found in its matches", any("Jordan" in x for x in report["national teams found in their matches"]),
          report["national teams not found"])
    check("full run: every league above its minimum", not [p for p in report["problems"] if "expected" in p], report["problems"])
    check("full run: the report counts the requests", report["requests"]["thesportsdb"] == len(
        [c for c in calls if c.startswith(ti.API)]) and len(report["requests"]["per part"]) == parts, report["requests"])
    new_club, renamed = ("777001", "Brand New FC"), next(iter(world[0]["Dutch Eredivisie"]))[0]
    world2 = fake_world(extra={"Dutch Eredivisie": new_club}, renamed={renamed: "Renamed Eagles"})
    calls, report = run_once(world2, tmp / "two", tmp / "one/teams-cache.json", full=False)
    lookups = sorted(up.parse_qs(up.urlparse(c).query)["id"][0] for c in calls if "lookupteam.php" in c)
    check("incremental: only the new and the renamed club looked up (after one probe)",
          lookups == sorted([new_club[0], renamed] + list(ti.PROBE)), lookups)
    check("incremental: no league or national team searched again (only the events part reads all_leagues.php)",
          len([c for c in calls if "/all_leagues.php" in c]) == 1
          and not [c for c in calls if "searchteams.php" in c or "searchevents.php" in c],
          [c for c in calls if "search" in c][:4])
    cached = json.loads((tmp / "one/teams-cache.json").read_text(encoding="utf-8"))["teams"]
    again = sum(1 for i, c in cached.items() if c["arFrom"] in ("translit", "missing") and i != renamed)
    check("incremental: Wikidata asked only about the two clubs and the names that are only transliterations",
          report["wikidata"].get("looked up before") == report["teams"] - 2 - again, (report["wikidata"], again))
    out = {t["id"]: t for t in json.loads((tmp / "two/teams.json").read_text(encoding="utf-8"))}
    check("incremental: the new club is in, the renamed one has its new name",
          new_club[0] in out and out[renamed]["en"] == "Renamed Eagles", out.get(renamed))
    check("incremental: far fewer requests", report["requests"]["thesportsdb"] < 25 + 5 * len(ti.LEAGUES), report["requests"])   # a few per league (4.3: 19 of them)
    # the workflow's way: each part on its own (one runner each), then the merge; the same list as one process
    (tmp / "split").mkdir()
    for part in ti.plan():
        run_once(world2, tmp / "split", tmp / "one/teams-cache.json", False, "collect", "--part", part["name"])
    check("split: one part file per league and the national teams",
          len(list((tmp / "split/parts").glob("*.json"))) == parts, sorted(p.name for p in (tmp / "split/parts").glob("*")))
    _, report = run_once(world2, tmp / "split", tmp / "one/teams-cache.json", False, "merge")
    check("split: the merge gives the same list as one process",
          (tmp / "split/teams.json").read_text(encoding="utf-8") == (tmp / "two/teams.json").read_text(encoding="utf-8"))
    check("split: requests counted per part", set(report["requests"]["per part"]) == {p["name"] for p in ti.plan()},
          report["requests"])
    (tmp / "split/parts/spanish-la-liga.json").unlink()
    _, report = run_once(world2, tmp / "split", tmp / "one/teams-cache.json", False, "merge")
    check("split: a part that wrote nothing is named in the problems",
          any(p.startswith("Spanish La Liga: no data") for p in report["problems"]), report["problems"][:3])
    def broken(url, **kw):
        raise RuntimeError("no answer: thesportsdb down")
    ti.fetch, ti.OUT, ti.FULL = broken, tmp / "broken", True
    code = None
    try:
        with contextlib.redirect_stdout(io.StringIO()):
            ti.main(["collect", "--part", "Qatar Stars League"])
    except SystemExit as e:
        code = e.code
    part = json.loads((tmp / "broken/parts/qatar-stars-league.json").read_text(encoding="utf-8"))
    check("collect: a crash still writes its part with the reason, and the job fails",
          code == 1 and "thesportsdb down" in part.get("error", "")
          and any(p.startswith("Qatar Stars League: the collection stopped") for p in part["report"]["problems"]),
          (code, part.get("error")))
ti.fetch = real_fetch

print("\nall clubs collector checks passed" if not failed else f"\n{failed} failed")
sys.exit(1 if failed else 0)
