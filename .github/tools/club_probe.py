#!/usr/bin/env python3
"""4.3.1 feasibility, report only (no UI): what a club page could show from TheSportsDB with the free key the page uses (123), for Al-Hilal v
Al-Nassr and Barcelona v Real Madrid. Unreachable from the dev container, so CI runs it (club-probe.yml) and pushes the report to the
data-club-probe branch. Nothing in the apps changes.

For each club: eventslast.php (the last results: how many, the newest one's date), eventsnext.php (the coming games: how many). For each pair:
searchevents.php «A_vs_B» in both orders (how many, which seasons, the newest), and the same with the current season; and the league's season
(eventsseason.php) filtered to the pair, as the other way to a head-to-head. Staleness: the newest finished game of the club in its league's
season and in eventspastleague.php, against eventslast's newest (a user's agent saw eventslast lag by a match). Every request is counted; a
429 is waited out (team_ids.fetch) and counted too.

    python3 .github/tools/club_probe.py
"""
import datetime
import json
import sys
import urllib.parse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import team_ids as ti  # noqa: E402

OUT = Path("probe-club")
CLUBS = {"136013": "Al-Hilal", "136022": "Al-Nassr", "133739": "Barcelona", "133738": "Real Madrid"}
PAIRS = [("136013", "136022"), ("133739", "133738")]
LEAGUE = {"136013": ("4668", "Saudi-Arabian Pro League"), "136022": ("4668", "Saudi-Arabian Pro League"),
          "133739": ("4335", "Spanish La Liga"), "133738": ("4335", "Spanish La Liga")}
FINAL = {"FT", "AET", "PEN", "MATCH FINISHED"}


def ev_brief(e):
    return {k: e.get(k) for k in ("idEvent", "strEvent", "dateEvent", "strTime", "strTimestamp", "strStatus", "intHomeScore", "intAwayScore",
                                   "idLeague", "strLeague", "strSeason", "idHomeTeam", "idAwayTeam")}


def when(e):
    return e.get("strTimestamp") or ((e.get("dateEvent") or "") + "T" + (e.get("strTime") or "00:00:00"))


def finished(e):
    return (e.get("strStatus") or "").upper() in FINAL or (e.get("intHomeScore") not in (None, "") and e.get("intAwayScore") not in (None, ""))


def counted(path):
    before = dict(ti.STATS)
    try:
        got = ti.get(path)
    except Exception as e:   # noqa: BLE001 - a probe reports
        return {"_error": str(e)[:200]}, 1
    used = sum(ti.STATS.values()) - sum(before.values())
    return got, used


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rep = {"when": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"), "clubs": {}, "pairs": {}, "requests": {}}
    seasons = {}
    for lid in {v[0] for v in LEAGUE.values()}:
        lk, _ = counted(f"lookupleague.php?id={lid}")
        seasons[lid] = ((lk.get("leagues") or [{}])[0] or {}).get("strCurrentSeason") or ""
    season_events = {}
    for lid, season in seasons.items():
        got, _ = counted(f"eventsseason.php?id={lid}&s={urllib.parse.quote(season)}")
        season_events[lid] = got.get("events") or []
        past, _ = counted(f"eventspastleague.php?id={lid}")
        season_events[lid + ":past"] = past.get("events") or []
    rep["seasons"] = {lid: {"season": s, "events": len(season_events[lid]), "past_league_events": len(season_events[lid + ":past"])} for lid, s in seasons.items()}

    for cid, name in CLUBS.items():
        last, n1 = counted(f"eventslast.php?id={cid}")
        nxt, n2 = counted(f"eventsnext.php?id={cid}")
        results = last.get("results") or last.get("events") or []
        upcoming = nxt.get("events") or []
        lid = LEAGUE[cid][0]
        mine = [e for e in season_events[lid] + season_events[lid + ":past"] if cid in (str(e.get("idHomeTeam")), str(e.get("idAwayTeam"))) and finished(e)]
        newest_league = max(mine, key=when) if mine else None
        newest_last = max(results, key=when) if results else None
        rep["clubs"][name] = {
            "eventslast": {"count": len(results), "keys": sorted(last.keys()), "newest": ev_brief(newest_last) if newest_last else None,
                           "all": [ev_brief(e) for e in results], "competitions": sorted({e.get("strLeague") or "" for e in results})},
            "eventsnext": {"count": len(upcoming), "all": [ev_brief(e) for e in upcoming], "competitions": sorted({e.get("strLeague") or "" for e in upcoming})},
            "league_newest_finished": ev_brief(newest_league) if newest_league else None,
            "eventslast_behind_league": bool(newest_league and (not newest_last or when(newest_league) > when(newest_last))),
            "requests": n1 + n2,
        }

    for a, b in PAIRS:
        an, bn = CLUBS[a], CLUBS[b]
        out = {}
        for x, y in ((an, bn), (bn, an)):
            q = f"{x}_vs_{y}".replace(" ", "_")
            got, n = counted("searchevents.php?" + urllib.parse.urlencode({"e": q}))
            evs = got.get("event") or got.get("events") or []
            out[q] = {"count": len(evs), "seasons": sorted({e.get("strSeason") or "" for e in evs}), "newest": ev_brief(max(evs, key=when)) if evs else None,
                      "finished": sum(finished(e) for e in evs), "requests": n}
            season = seasons[LEAGUE[a][0]]
            got, n = counted("searchevents.php?" + urllib.parse.urlencode({"e": q, "s": season}))
            evs = got.get("event") or got.get("events") or []
            out[q + " season " + season] = {"count": len(evs), "requests": n}
        lid = LEAGUE[a][0]
        both = [e for e in season_events[lid] if {str(e.get("idHomeTeam")), str(e.get("idAwayTeam"))} == {a, b}]
        out["league season filtered"] = {"count": len(both), "events": [ev_brief(e) for e in both]}
        rep["pairs"][f"{an} v {bn}"] = out

    rep["requests"] = dict(ti.STATS)
    (OUT / "report.json").write_text(json.dumps(rep, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    lines = [f"# Club page data, free key 123 ({rep['when']})", "", "## Clubs", "", "| club | eventslast | newest result | league's newest finished | eventslast behind | eventsnext | next competitions |",
             "|---|---|---|---|---|---|---|"]
    for name, c in rep["clubs"].items():
        nl, lg = c["eventslast"]["newest"], c["league_newest_finished"]
        lines.append(f"| {name} | {c['eventslast']['count']} | {(nl or {}).get('dateEvent')} {(nl or {}).get('strEvent')} | {(lg or {}).get('dateEvent')} {(lg or {}).get('strEvent')} "
                     f"| {'YES' if c['eventslast_behind_league'] else 'no'} | {c['eventsnext']['count']} | {', '.join(c['eventsnext']['competitions'])} |")
    lines += ["", "## Head to head", ""]
    for pair, p in rep["pairs"].items():
        lines.append(f"### {pair}")
        for k, v in p.items():
            if k == "league season filtered":
                lines.append(f"- the league's season filtered to the pair: {v['count']}")
            else:
                lines.append(f"- searchevents «{k}»: {v['count']}" + (f" (seasons {', '.join(v['seasons'])}; finished {v['finished']}; newest {(v['newest'] or {}).get('dateEvent')})" if "seasons" in v else ""))
        lines.append("")
    lines += ["## Requests", "", "```", json.dumps(rep["requests"], indent=1), "```", "", "Seasons: " + json.dumps(rep["seasons"])]
    (OUT / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
