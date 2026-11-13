#!/usr/bin/env python3
"""4.3.1 «البطولات الكبرى» and «اختر دورياتك»: the big tournaments' list is one data file that resolves against the catalogue (every id has its Arabic name, none twice, none a league
the person's built-in list has under another id), the switch's default (on for a new install, off for one with settings), what the page is told (tour=1, tourx=), the one-screen chooser
(search on top, the five sections in order, a tick and the Arabic name, the count in the header), no TheSportsDB request from the TV for the tournaments. LeagueGroups runs on a plain JVM.
    python3 tests/league_groups_test.py"""
import json
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


read = lambda p: Path(p).read_text(encoding="utf-8")
data = json.loads(read(ROOT / "assets/shared/tournaments.json"))
ts = data["tournaments"]
ids = [t["id"] for t in ts]
check("the list: ids are numbers, once each, every one with its Arabic and English name", all(re.fullmatch(r"\d{1,6}", t["id"]) and t["ar"].strip() and t["en"].strip() for t in ts) and len(set(ids)) == len(ids), ids)
want = {"4480": "UEFA Champions League", "4481": "UEFA Europa League", "5071": "UEFA Conference League", "4490": "UEFA Nations League", "4502": "UEFA European Championships", "4429": "FIFA World Cup",
        "4866": "AFC Asian Cup", "4719": "AFC Champions League Elite", "4720": "CAF Champions League", "4483": "Copa del Rey", "4482": "FA Cup", "4506": "Coppa Italia", "4485": "DFB-Pokal",
        "4484": "Coupe de France", "5649": "Saudi King Cup", "5650": "Saudi Super Cup", "4503": "FIFA Club World Cup", "4499": "Copa America", "5105": "FIFA Arab Cup"}
check("the set the owner asked for is in it (by TheSportsDB id and name, as data-catalogue has them)", all(any(t["id"] == i and t["en"] == n for t in ts) for i, n in want.items()), [i for i in want if i not in ids])
check("the qualifiers are in it (World Cup UEFA/AFC/CAF, Asian Cup, Africa)", all(i in ids for i in ("5518", "5513", "5514", "5521", "5520")))
check("what is not in the catalogue is said, not guessed: Gulf Cup and the Euro qualifiers 'wanted, not found', the Africa Cup finals 'try'", {w["en"] for w in data["wanted_not_found"]} == {"UEFA European Championship Qualifying", "Gulf Cup"}
      and [t["id"] for t in data["try"]] == ["4496"])
fx = read(ROOT / ".github/tools/tournaments_fixtures.py")
wf = read(ROOT / ".github/workflows/league-fixtures.yml")
check("the CI file: its own job (own timeout), after the leagues job, keeps every other file of the branch, pushes only to data-leagues", "  tournaments:" in wf and "needs: fixtures" in wf and "timeout-minutes: 60" in wf
      and "cp -r prev/leagues/. leagues/" in wf and "git push -f origin data-leagues" in wf and "origin work" not in wf and "tournaments.json prev/leagues/tournaments-report.json leagues/" in wf)
check("the tool asks TheSportsDB in CI only, three days a tournament, and the page reads one file", "eventsday.php?d={day}&l={lid}" in fx and "range(3)" in fx and "leagues/tournaments.json" in read(ROOT / "src/template.html").replace("LEAGUE_BASE + 'tournaments.json'", "leagues/tournaments.json"))

prefs, st = read(APP / "Prefs.java"), read(APP / "SettingsActivity.java")
act = read(APP / "LeaguesActivity.java")
check("the switch: on for a new install (nothing saved yet), off for one that already had settings, decided once when the settings are first opened by 4.3.1",
      'if (!sp.contains("tournaments")) sp.edit().putBoolean("tournaments", sp.getAll().isEmpty()).apply();' in prefs)
check("the page is told tour=1 and tourx= only while the switch is on; nothing when off", '(tournaments() ? "&tour=1" + (off.isEmpty() ? "" : "&tourx=" + Uri.encode(join(off, ","))) : "")' in prefs)
check("«الدوريات كاملة» opens the one-screen chooser", "LeaguesActivity.class" in st and 'android:name=".LeaguesActivity"' in read(ROOT / "android/app/src/main/AndroidManifest.xml"))
check("the chooser: the search field on top, «البطولات الكبرى» first, then السعودية، أوروبا، آسيا وأفريقيا، العالم, a tick and the Arabic name on each row, the count of the chosen in the header",
      act.index("search = new EditText") < act.index('S.t("البطولات الكبرى")') and 'S.n("السعودية"), S.n("أوروبا"), S.n("آسيا وأفريقيا"), S.n("العالم")' in act and "S.t(\"اخترت %s من %s\"" in act
      and 'text(on ? "✓" : ""' in act and "AlertDialog" not in act)
check("the chooser keeps the cap (Prefs.MAX_LEAGUES), takes the tournaments out of the rest (no duplicate) and the best-known catalogue leagues without a search, all of it with one", "Prefs.MAX_LEAGUES" in act
      and "ids.contains(r[0])" in act and "r[6].isEmpty()" in act and "searching" in act)
check("no TheSportsDB request for the tournaments from the TV: the page asks the one file, no league id of the set goes into #leagues=", "tournaments" in prefs and "thesportsdb" not in act.lower())
tpl = read(ROOT / "src/template.html")
check("the page: the file after the person's own leagues (a game both have is the league's), nothing from a file older than 36 h, the unticked left out, nothing loaded when off, the cards' rows after the teams' and the leagues' with at most 3",
      "CONFIG.leagues.concat(CONFIG.tour ? ['tournaments'] : [])" in tpl and "> 36 * 3600000" in tpl and "CONFIG.tourOff.includes(String(ev.idLeague))" in tpl and "if(CONFIG.tour) try{" in tpl
      and "(ev.tour ? 100 : 0)" in tpl and "filter(y => y.ev.tour).length < 3" in tpl)
if shutil.which("javac") and shutil.which("java"):
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "LeagueGroups.java"), str(ROOT / "tests/LeagueGroupsRun.java")], capture_output=True, text=True)
        check("LeagueGroups compiles on a plain JVM", r.returncode == 0, r.stderr[-400:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", tmp, "sa.urtv.screensaver.LeagueGroupsRun"], capture_output=True, text=True, timeout=60).stdout
            lines = [l.split("\t") for l in out.splitlines() if "\t" in l]
            for n, v in lines:
                check(n, v == "ok")
            check("every case ran", len(lines) == 4, out)
else:
    check("java is installed", False)
sys.exit(1 if failed else 0)
