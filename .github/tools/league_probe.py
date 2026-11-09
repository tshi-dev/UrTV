#!/usr/bin/env python3
"""4.3 leagues: what TheSportsDB calls the continental competitions and the Iraqi league, and their ids, before they go into Prefs.LEAGUES
(unreachable from the dev container). Out: probe-leagues/report.json and report.md: every league whose name matches, its id, country and
sport, and for each candidate id the current season's first events (are there games, who plays them). Nothing is changed.

    python3 .github/tools/league_probe.py
"""
import json
import re
import sys
import urllib.parse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import team_ids as ti  # noqa: E402

OUT = Path("probe-leagues")
WORDS = re.compile(r"champions league|afc|asian|iraq|uefa", re.I)
GUESS = ["4480", "4719", "4720", "5144", "4914", "4877", "4790"]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rep = {"all_leagues": [], "searched": {}, "lookups": {}}
    for l in ti.get("all_leagues.php").get("leagues") or []:
        if WORDS.search(l.get("strLeague", "")) or WORDS.search(l.get("strLeagueAlternate") or ""):
            rep["all_leagues"].append(l)
    for c in ("Iraq", "Asia", "Europe", "World"):
        got = ti.get("search_all_leagues.php?" + urllib.parse.urlencode({"c": c, "s": "Soccer"}))
        rep["searched"][c] = [{k: l.get(k) for k in ("idLeague", "strLeague", "strLeagueAlternate", "strCountry", "strCurrentSeason", "intFormedYear")}
                              for l in (got.get("countries") or got.get("countrys") or got.get("leagues") or [])
                              if c == "Iraq" or WORDS.search(l.get("strLeague", "") + " " + (l.get("strLeagueAlternate") or ""))]
    ids = {str(l.get("idLeague")) for l in rep["all_leagues"]} | {str(l["idLeague"]) for v in rep["searched"].values() for l in v} | set(GUESS)
    for i in sorted(ids):
        lg = (ti.get(f"lookupleague.php?id={i}").get("leagues") or [{}])[0]
        if not lg:
            continue
        season = lg.get("strCurrentSeason") or ""
        evs = ti.get(f"eventsseason.php?id={i}&s={urllib.parse.quote(season)}").get("events") or [] if season else []
        nxt = ti.get(f"eventsnextleague.php?id={i}").get("events") or []
        rep["lookups"][i] = {"name": lg.get("strLeague"), "alt": lg.get("strLeagueAlternate"), "country": lg.get("strCountry"), "sport": lg.get("strSport"),
                             "season": season, "season_events": len(evs), "next": len(nxt),
                             "teams": sorted({e.get("strHomeTeam") for e in evs + nxt if e.get("strHomeTeam")})[:60]}
    (OUT / "report.json").write_text(json.dumps(rep, ensure_ascii=False, indent=1), encoding="utf-8")
    md = ["| id | name | alt | country | sport | season | season events | next | teams (some) |", "|---|---|---|---|---|---|---|---|---|"]
    for i, l in rep["lookups"].items():
        md.append(f"| {i} | {l['name']} | {l['alt']} | {l['country']} | {l['sport']} | {l['season']} | {l['season_events']} | {l['next']} | {', '.join(l['teams'][:12])} |")
    md += ["", "searched: " + json.dumps(rep["searched"], ensure_ascii=False)]
    (OUT / "report.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))


if __name__ == "__main__":
    main()
