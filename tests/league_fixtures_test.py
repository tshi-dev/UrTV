#!/usr/bin/env python3
"""«الدوريات كاملة» offline: the CI fixture collector (.github/tools/league_fixtures.py) with TheSportsDB mocked, the page's
file name for a league (leagueSlug, in node) against the collector's, the workflow (never on a push), and Prefs.leagueByName
(javac). Run: python3 tests/league_fixtures_test.py"""
import datetime
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
os.environ["LEAGUES_OUT"] = tempfile.mkdtemp()
spec = importlib.util.spec_from_file_location("league_fixtures", ROOT / ".github/tools/league_fixtures.py")
lf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lf)
ti = lf.ti
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok


UTC = datetime.timezone.utc
NOW = datetime.datetime(2026, 10, 23, 12, 0, tzinfo=UTC)


def ev(i, home, away, hours, league="4668", season="2026-2027", **extra):
    k = NOW + datetime.timedelta(hours=hours)
    return {"idEvent": str(i), "idLeague": league, "strSeason": season, "idHomeTeam": str(100 + i), "strHomeTeam": home,
            "idAwayTeam": str(200 + i), "strAwayTeam": away, "strTimestamp": k.strftime("%Y-%m-%dT%H:%M:%S"),
            "dateEvent": k.strftime("%Y-%m-%d"), "strStatus": "NS", "intHomeScore": None, "intAwayScore": None,
            "strHomeTeamBadge": f"https://r2.thesportsdb.com/images/media/team/badge/h{i}.png",
            "strAwayTeamBadge": f"https://r2.thesportsdb.com/images/media/team/badge/a{i}.png",
            "strThumb": "x" * 50, **extra}


# ---- pick(): the window, the league, the season, once each, in time order, only what the page uses
raw = [ev(1, "A", "B", 3), ev(1, "A", "B", 3),                      # twice
       ev(2, "C", "D", -20), ev(3, "E", "F", -40),                  # 20 h ago is kept, 40 h ago is not
       ev(4, "G", "H", 24 * 7 + 1), ev(5, "I", "J", 24 * 7 - 1),    # a day past the week is not, an hour short is
       ev(6, "Abha", "Z", 5, league="5627"),                        # Division 1, not this league
       ev(7, "K", "L", 6, season="2025-2026"),                      # last season
       {"idEvent": "8", "strHomeTeam": "M", "strAwayTeam": "N"},     # no date
       {**ev(9, "O", "P", 1), "strHomeTeam": ""}]                   # one side unnamed
got = lf.pick(raw, "4668", NOW)
check("pick: the window, this league's, this season's, once each, soonest first", [e["idEvent"] for e in got] == ["2", "1", "5"],
      [e["idEvent"] for e in got])
check("pick: only the fields the page reads", all(set(e) <= set(lf.KEEP) for e in got) and "strThumb" not in got[0], got[0])
check("pick: both clubs' badge addresses and ids are kept (the crests of the launcher's card)",
      all(e.get(k) for e in got for k in ("strHomeTeamBadge", "strAwayTeamBadge", "idHomeTeam", "idAwayTeam")), got[0])
check("pick: the venue is kept («مباريات اليوم» shows where a game is played)",
      lf.pick([ev(1, "A", "B", 3, strVenue="King Abdullah Sports City")], "4668", NOW)[0].get("strVenue") == "King Abdullah Sports City")
check("pick: an event of no league id is taken as this league's", [e["idEvent"] for e in lf.pick([{**ev(1, "A", "B", 3), "idLeague": None}], "4668", NOW)] == ["1"])
check("days(): the last 36 hours to a week ahead, 9 days", len(lf.days(NOW)) == 9 and lf.days(NOW)[0] == "2026-10-22" and lf.days(NOW)[-1] == "2026-10-30", lf.days(NOW))

# ---- collect(): a round of 9 split over 3 days that the free league calls cut short is whole from eventsday
ROUND = [ev(10 + i, f"H{i}", f"A{i}", 2 + 10 * (i // 3) + i % 3) for i in range(9)]
asked = []


def fake(path):
    asked.append(path)
    if path.startswith("eventsnextleague"):
        return {"events": ROUND[:4]}                                  # the free call stops short
    if path.startswith("eventspastleague"):
        return {"events": [ev(1, "P", "Q", -20)]}
    m = re.match(r"eventsday\.php\?d=([\d-]+)&l=(\w+)", path)
    if m and m.group(2) == "4668":
        return {"events": [e for e in ROUND if e["dateEvent"] == m.group(1)]}
    return {}


events, notes = lf.collect("Saudi-Arabian Pro League", "4668", NOW, fake)
check("collect: all 9 games of the round plus the last result, once each", len(events) == 10 and len({e["idEvent"] for e in events}) == 10, len(events))
check("collect: requests per league = 2 league calls + one per day of the window", len(asked) == 2 + 9, len(asked))
check("collect: the report says what each call answered and whether it looked capped",
      notes["next"]["count"] == 4 and notes["past"]["count"] == 1 and notes["eventsday"]["games by id"] > 0, notes)

# eventsday that does not know ids but knows names: asked by name from the third day on
asked.clear()


def by_name(path):
    asked.append(path)
    if path.startswith(("eventsnextleague", "eventspastleague")):
        return {"events": ROUND[:2]}
    m = re.match(r"eventsday\.php\?d=([\d-]+)&l=([\w-]+)", path)
    if m and m.group(2) == "Saudi-Arabian_Pro_League":
        return {"events": [e for e in ROUND if e["dateEvent"] == m.group(1)]}
    return {}


events, notes = lf.collect("Saudi-Arabian Pro League", "4668", NOW, by_name)
check("collect: eventsday answering only by league name is found and used (the first days asked again)", len(events) == 9 and notes["eventsday"]["last form"] == "name", notes)

# ---- main(): files written, an empty answer keeps the last file, one failing league does not stop the rest
out = Path(os.environ["LEAGUES_OUT"])
prev = Path(tempfile.mkdtemp())
lf.OUT, lf.PREV = out, prev
(prev / "nba.json").write_text(json.dumps({"events": [ev(50, "Lakers", "Suns", 4, league="4387")]}), encoding="utf-8")
lf.ti.league_ids = lambda names, cached=None: {"Saudi-Arabian Pro League": "4668", "NBA": "4387"}
lf.ti.load_cache = lambda: ({"leagues": {}}, {})


def main_get(path):
    if "4387" in path:
        return {}                                     # the NBA answers nothing this time
    return fake(path)


lf.ti.get = main_get
lf.collect.__defaults__ = (main_get,)
lf.datetime = type("D", (), {"datetime": type("DT", (datetime.datetime,), {"now": classmethod(lambda c, tz=None: NOW)}),
                             "timezone": datetime.timezone, "timedelta": datetime.timedelta, "date": datetime.date})
try:
    lf.main(["Saudi-Arabian Pro League", "NBA"])
except SystemExit:
    pass
sa = json.loads((out / "saudi-arabian-pro-league.json").read_text(encoding="utf-8"))
nba = json.loads((out / "nba.json").read_text(encoding="utf-8"))
check("main: a league's file holds its name, id, time and games", sa["league"] == "Saudi-Arabian Pro League" and sa["id"] == "4668"
      and sa["updated"].endswith("Z") and len(sa["events"]) == 10, {k: v for k, v in sa.items() if k != "events"})
check("main: a league that answered nothing keeps its last games, marked stale", nba.get("stale") and len(nba["events"]) == 1, nba)
check("main: a file stays small (about 300 bytes a game)", (out / "saudi-arabian-pro-league.json").stat().st_size < 6000,
      (out / "saudi-arabian-pro-league.json").stat().st_size)
check("main: the report lists each league", set(json.loads((out / "report.json").read_text(encoding="utf-8"))["leagues"]) == {"Saudi-Arabian Pro League", "NBA"})

# a run cut off by its time limit (the job is cancelled mid-league): the leagues already done are in report.json, so it is pushed
(out / "report.json").unlink()
real_collect = lf.collect
def cut_off(name, lid, now, get=main_get):
    if name == "NBA":
        raise KeyboardInterrupt   # what the runner's cancel looks like from inside
    return real_collect(name, lid, now, get)
lf.collect = cut_off
try:
    lf.main(["Saudi-Arabian Pro League", "NBA"])
except (KeyboardInterrupt, SystemExit):
    pass
lf.collect = real_collect
rep = json.loads((out / "report.json").read_text(encoding="utf-8")) if (out / "report.json").exists() else {}
check("main: cut off mid-run, report.json already holds the leagues done", set(rep.get("leagues", {})) == {"Saudi-Arabian Pro League"}, rep)

# ---- the page names a league's file as the collector does
leagues = ti.prefs_leagues()
node = shutil.which("node")
if node:
    page = (ROOT / "src/template.html").read_text(encoding="utf-8")
    line = next(l for l in page.splitlines() if l.startswith("const leagueSlug"))
    r = subprocess.run([node, "-e", line + "\nprocess.stdout.write(JSON.stringify(JSON.parse(require('fs').readFileSync(0,'utf8')).map(leagueSlug)))"],
                       input=json.dumps(leagues), capture_output=True, text=True, encoding="utf-8")
    check(f"leagueSlug (page) = slug (collector) for all {len(leagues)} leagues", r.returncode == 0 and json.loads(r.stdout) == [ti.slug(n) for n in leagues],
          r.stderr[-300:] or r.stdout)
else:
    print("SKIP leagueSlug against the page: no node here")
cache = json.loads((ROOT / ".github/tools/teams-cache.json").read_text(encoding="utf-8"))["leagues"]
check("every league of the app has its id in teams-cache.json (no search needed in CI)", all(n in cache for n in leagues), [n for n in leagues if n not in cache])

# ---- the workflow runs on a timer, by hand, or on a push to master/next that changes its own files (never a work branch)
wf = (ROOT / ".github/workflows/league-fixtures.yml").read_text(encoding="utf-8")
on = wf.split("\non:", 1)[1].split("\npermissions:", 1)[0]
push = on.split("push:", 1)[1] if "push:" in on else ""
check("workflow: a timer, by hand, or a push to master/next touching only its own files",
      "schedule:" in on and "workflow_dispatch:" in on and "pull_request" not in on
      and (not push or ("branches: [master, next]" in push
                        and set(re.findall(r"- '([^']+)'", push)) == {".github/tools/league_fixtures.py", ".github/tools/tournaments_fixtures.py", "assets/shared/tournaments.json", ".github/workflows/league-fixtures.yml"})),
      on[-400:])
check("workflow: pushes only to data-leagues", "git push -f origin data-leagues" in wf and "origin work" not in wf)
check("page reads the branch the workflow writes", "tshi-dev/UrTV/data-leagues/leagues/" in (ROOT / "src/template.html").read_text(encoding="utf-8"))

# ---- Prefs.leagueByName, the Java the settings screen and saver.sh share: English, Arabic and slug spellings
javac, java = shutil.which("javac"), shutil.which("java")
if not (javac and java):
    print("SKIP Prefs.leagueByName: no javac here")
else:
    src = (ROOT / "android/app/src/main/java/sa/urtv/screensaver/Prefs.java").read_text(encoding="utf-8")
    table = "static final String[][] LEAGUES = {" + src.split("static final String[][] LEAGUES = {")[1].split("};")[0] + "};"
    fn = src[src.index("    static String[] leagueByName"):src.index("    /** Followed leagues: a built-in one by its English name")]
    cases = [("Saudi-Arabian Pro League", "Saudi-Arabian Pro League"), ("saudi-arabian-pro-league", "Saudi-Arabian Pro League"),
             ("  SAUDI ARABIAN PRO LEAGUE ", "Saudi-Arabian Pro League"), ("دوري روشن السعودي", "Saudi-Arabian Pro League"),
             ("الدوري الإنجليزي", "English Premier League"), ("nba", "NBA"), ("دوري كرة السلة الأمريكي (NBA)", "NBA"),
             ("Premier League", "-"), ("", "-"), ("المنتخبات", "-")]
    with tempfile.TemporaryDirectory() as d:
        (Path(d) / "L.java").write_text("public class L {\n" + table + "\n" + fn + "\n  public static void main(String[] a) throws Exception {\n"
            "    java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(System.in, \"UTF-8\"));\n"
            "    java.io.PrintStream o = new java.io.PrintStream(System.out, true, \"UTF-8\"); String s;\n"
            "    while ((s = r.readLine()) != null) { String[] l = leagueByName(s); o.println(l == null ? \"-\" : l[0]); }\n  }\n}\n", encoding="utf-8")
        c = subprocess.run([javac, "-encoding", "UTF-8", "L.java"], cwd=d, capture_output=True, text=True)
        check("Prefs.leagueByName compiles", c.returncode == 0, c.stderr[-500:])
        if c.returncode == 0:
            r = subprocess.run([java, "-Dfile.encoding=UTF-8", "-cp", d, "L"], input="\n".join(q for q, _ in cases) + "\n", capture_output=True,
                               text=True, encoding="utf-8")
            got = r.stdout.split("\n")[:len(cases)]
            check("Prefs.leagueByName: English, slug, Arabic and mixed spellings; unknown names and «المنتخبات» are not leagues",
                  got == [w for _, w in cases], list(zip([q for q, _ in cases], got)))

print(f"\n{failed} failed" if failed else "\nall league tests passed")
sys.exit(1 if failed else 0)
