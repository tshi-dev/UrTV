#!/usr/bin/env python3
"""4.3 «كل الدوريات» on the TV: a league of the catalogue is kept as "id4719|ar|en" (CatalogueLeagues), sent to the page as id4719 with its
name in #lgn=; the catalogue is read only when the picker opens (never at start or on a timer); at most 8 leagues in all; the page asks
TheSportsDB for a picked catalogue league only, in the teams' queue, and drops the copy of one no longer picked.
    python3 tests/catalogue_leagues_test.py"""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


def read(p):
    return Path(p).read_text(encoding="utf-8")


prefs, st, rc = read(APP / "Prefs.java"), read(APP / "SettingsActivity.java"), read(APP / "SaverReceiver.java")
check("at most 8 leagues in all", "static final int MAX_LEAGUES = 8;" in prefs)
check("the page gets the tokens and the names (leaguesFragment)", '+ leaguesFragment()' in prefs and '"&lgn=" + Uri.encode(names)' in prefs)
uses = [m.start() for m in re.finditer(r"CatalogueLeagues\.URL", st)]
check("the catalogue is read in one place only: openCatalogue, from the picker's «كل الدوريات…»", len(uses) == 1
      and st.rfind("private void openCatalogue()", 0, uses[0]) > st.rfind("\n    private void ", 0, uses[0]) - 1
      and 'setNeutralButton("كل الدوريات…", (d, w) -> { save.run(); openCatalogue(); })' in st)
check("nothing else in the apps reads the catalogue", not any("data-catalogue" in read(p) for p in (ROOT / "android").rglob("*.java") if p.name != "CatalogueLeagues.java")
      and "data-catalogue" not in read(ROOT / "src/template.html"))
check("a failed read falls back on the last copy, which only a whole answer replaces", 'new JSONObject(body).getJSONArray("leagues");' in st and "league-catalogue.json" in st)
check("saver.sh can follow one by id (id4719)", 'matches("id\\\\d{1,6}")' in rc)

t = read(ROOT / "src/template.html")
check("the page: #leagues= takes id4719, 8 at most; #lgn= names them", "/^id\\d{1,6}$/.test(x)).slice(0, 8)" in t and "_Q.get('lgn')" in t)
check("the page: a catalogue league is asked of TheSportsDB (next and past), after the teams, in their queue", "eventsnextleague', 'eventspastleague'" in t
      and "leagues = loadLeagues(step, teamsAsked)" in t and "teamsDone();" in t and "await ready;" in t)
check("the page: the copy of a league no longer picked goes", "if(!CONFIG.leagues.includes(k)) delete LEAGUE_DATA[k];" in t)
check("the page: a catalogue league's games are named by #lgn", "CONFIG.leagueNames[String(ev.idLeague || '')]" in t)
for f in ("dist/tv.html", "dist/tv-en.html"):
    check(f"{f} is built with it", "loadCatalogueLeague" in read(ROOT / f))

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "CatalogueLeagues.java"), str(ROOT / "tests/CatalogueLeaguesRun.java")], capture_output=True, text=True)
        check("CatalogueLeagues compiles on a plain JVM", r.returncode == 0, r.stderr[-300:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.CatalogueLeaguesRun"], capture_output=True, text=True).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            bad = [n for n, v in lines if v != "ok"]
            check(f"CatalogueLeagues: every case ({len(lines)})", len(lines) == 11 and not bad, bad or out)
sys.exit(1 if failed else 0)
