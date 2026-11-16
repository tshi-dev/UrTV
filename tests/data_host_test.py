#!/usr/bin/env python3
"""4.3.3 The data files' host (docs/DATA-HOST.md): tshi-dev/urtv-data first, the old data branch of this repository second, one map in the
screensaver, the launcher and the page, and every reader of a data file going through it. Runs DataHost on a plain JVM against a local server."""
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  [{detail}]"))
    if not ok:
        fails.append(name)


def read(p):
    return Path(p).read_text(encoding="utf-8")


app, home, page = read(APP / "DataHost.java"), read(HOME / "DataHost.java"), read(ROOT / "src/template.html")
check("the launcher's DataHost is the screensaver's, the package and its own name aside",
      app.split("final class DataHost", 1)[1] == home.split("final class DataHost", 1)[1])
jmap = re.findall(r'\{"([^"]+)", "([^"]+)"\}', app)
pmap = re.findall(r"\['([^']+)', '([^']+)'\]", page.split("const DATA_MAP = ", 1)[1].split(";", 1)[0])
check("the page's DATA_MAP is the apps' MAP, in the same order", jmap == pmap and len(jmap) == 7, (jmap, pmap))
check("the same two hosts in the apps and the page",
      'OLD = "https://raw.githubusercontent.com/tshi-dev/UrTV/"' in app and 'NEW = "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/"' in app
      and "DATA_OLD = 'https://raw.githubusercontent.com/tshi-dev/UrTV/', DATA_NEW = 'https://raw.githubusercontent.com/tshi-dev/urtv-data/data/'" in page)
check("the quiz's folder comes before the other games' in the map", [m[0] for m in jmap].index("data-games/quiz/") < [m[0] for m in jmap].index("data-games/games/"))

# every reader of a data branch goes through the map; the old address stays in the code as the fallback
readers = {
    APP / "GameData.java": "DataHost.get(BASE + name + \".json\"",
    APP / "QuizBank.java": "DataHost.get(url,",   # the rev and the versioned file go through the same helper
    APP / "SettingsActivity.java": "CatalogueLeagues.URL, 15000, 12000, Bounded.JSON_MAX, 0, false)",   # DataHost.cached: the copy's ETag stays
    HOME / "MarketFile.java": "DataHost.cached(new File(c.getCacheDir(), \"net\"), url,",
}
for p, call in readers.items():
    check(f"{p.name} reads through DataHost", call in read(p))
check("no reader of a data branch calls Net.get on its own",
      not any(re.search(r"Net\.get\((BASE|URL|CatalogueLeagues\.URL|MarketData\.\w+|url), null", read(p)) for p in readers))
check("the page's league and market files go through fetchData",
      "fetchData(LEAGUE_BASE + leagueSlug(name)" in page and "fetchData(LEAGUE_BASE + 'tournaments.json'" in page and "fetchData(MARKET_URL" in page
      and not re.search(r"fetchT\((LEAGUE_BASE|MARKET_URL)", page))
check("the old addresses are kept (old data branches are the fallback)", "tshi-dev/UrTV/data-leagues/leagues/" in page and "tshi-dev/UrTV/data-markets/markets/markets.json" in page)

# the page's dataUrls in node
if shutil.which("node"):
    block = page.split("const DATA_OLD = ", 1)[1].split("// the first ok answer", 1)[0]
    js = "const DATA_OLD = " + block + """
for (const u of ['https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json', 'https://raw.githubusercontent.com/tshi-dev/UrTV/data-leagues/leagues/a.json',
                 'https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/a.jpg']) console.log(dataUrls(u).join(' '));"""
    out = subprocess.run(["node", "-e", js], capture_output=True, text=True).stdout.splitlines()
    check("page: the quiz, a league, and an address outside the map", out == [
        "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/games/quiz/quiz.json https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json",
        "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/football/leagues/a.json https://raw.githubusercontent.com/tshi-dev/UrTV/data-leagues/leagues/a.json",
        "https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/a.jpg"], out)
else:
    print("SKIP the page's run: no node")

if not shutil.which("javac"):
    print("SKIP the run: no javac")
else:
    with tempfile.TemporaryDirectory() as tmp:
        r = subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp, str(APP / "DataHost.java"), str(APP / "Net.java"), str(APP / "NetLog.java"), str(ROOT / "tests/DataHostRun.java")],
                           capture_output=True, text=True)
        check("DataHost compiles on a plain JVM", r.returncode == 0, r.stderr[-800:])
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.DataHostRun"], capture_output=True, text=True, timeout=60).stdout
            urls = [l[5:].split(" ") for l in out.splitlines() if l.startswith("URLS ")]
            N, O = "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/", "https://raw.githubusercontent.com/tshi-dev/UrTV/"
            want = [N + "football/leagues/saudi-arabian-pro-league.json", N + "football/catalogue/catalogue.json", N + "football/archive/h2h/1-2.json",
                    N + "games/quiz/quiz.json", N + "games/odd-one.json", N + "markets/markets.json", N + "stations/stations.json"]
            check("each dataset: urtv-data first, then the old address", [u[0] for u in urls[:7]] == want and all(len(u) == 2 and u[1].startswith(O + "data-") for u in urls[:7]), urls)
            check("an address outside the map is asked alone", urls[7:] == [[O + "master/photos/x.jpg"], ["https://example.com/data-games/games/x.json"]], urls[7:])
            check("get: the new address answers, the old is not asked", "NEWFIRST got /new/markets/markets.json" in out, out)
            check("get: a 404 at the new address, the old one answers", "FALLBACK got /old/data-games/games/missing.json" in out, out)
            asked = out.split("ASKED ", 1)[1].split()
            check("get: asked in that order, nothing more", asked == ["/new/markets/markets.json", "/new/games/missing.json", "/old/data-games/games/missing.json"], asked)

print(f"\n{len(fails)} failed" if fails else "\nall passed")
sys.exit(1 if fails else 0)
